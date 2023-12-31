package Client;

import Common.BulletinBoard;
import Common.ServerBoard;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.ByteBuffer;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Client {
    private String username;
    private final Map<String, ClientInfo> clientInfoMap;
    private final Map<String, List<Message>> messageHistory;
    private final JList<String> userList;
    private final DefaultListModel<String> userListModel;
    private final BulletinBoard bulletinBoard;
    private final ServerBoard server;
    private static MessageDigest digest;
    private final SecureRandom s;

    private JTextArea chatArea;
    private JTextField newMessageField;

    private int amountCoins;
    private int maxSendsPerSecond;
    private final ScheduledExecutorService coinRestScheduler;
    private final ScheduledExecutorService sendsPerSecondScheduler;

    public Client () throws RemoteException, NotBoundException, NoSuchAlgorithmException {
        clientInfoMap = new HashMap<>();
        digest = MessageDigest.getInstance("SHA-256");
        Registry myRegistry = LocateRegistry.getRegistry("localhost", 1234);
        bulletinBoard = (BulletinBoard) myRegistry.lookup("BulletinBoardServer");
        server = (ServerBoard) myRegistry.lookup("ServerBoard");
        messageHistory = new HashMap<>();
        s = new SecureRandom();
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        amountCoins = 500;
        maxSendsPerSecond = 3;
        coinRestScheduler = Executors.newScheduledThreadPool(1);
        coinRestScheduler.scheduleAtFixedRate(() -> {
            amountCoins = 500;
        }, 0, 1, TimeUnit.DAYS);
        sendsPerSecondScheduler = Executors.newScheduledThreadPool(1);
        sendsPerSecondScheduler.scheduleAtFixedRate(() -> {
            maxSendsPerSecond = 3;
        }, 0, 1, TimeUnit.SECONDS);
    }

    // ------------------------------------------ THE CLIENT -----------------------------------------------------------
    public static void main(String[] args) throws NotBoundException, NoSuchAlgorithmException, RemoteException {
        Client c = new Client();
        c.run();
    }

    public void run(){
        JFrame frame = this.createUI();
        try {
            this.doLogin(frame);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void send(String message, String toUser) throws RemoteException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        // calculate next index
        int nextIndex = s.nextInt(0, Integer.MAX_VALUE);
        byte[] nextIndexBytes = intToBytes(nextIndex);

        // calculate next tag
        byte[] nextTagBytes = new byte[8];
        s.nextBytes(nextTagBytes);

        // construct message
        byte[] messageBytes = message.getBytes();
        byte[] messageConcat = concatenateArrays(nextIndexBytes, nextTagBytes, messageBytes);

        // encrypt message
        ClientInfo clientInfo = clientInfoMap.get(toUser);
        byte[] eMessageConcat = encryptAES(messageConcat, clientInfo.getSendKey());

        // put in bulletinboard
        bulletinBoard.write(clientInfo.getSendIndex(), eMessageConcat, digest.digest(clientInfo.getSendTag()));

        // update clientInfo for next message
        clientInfo.setSendIndex(nextIndex);
        clientInfo.setSendTag(nextTagBytes);
        clientInfo.setSendKey(KDF(clientInfo.getSendKey()));

        // save message in history
        Message m = new Message(message, username);
        messageHistory.get(toUser).add(m);
        chatArea.append(m.getFormat());

        amountCoins--;
        maxSendsPerSecond--;
    }

    public void receive(String fromUser) throws RemoteException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        ClientInfo clientInfo = clientInfoMap.get(fromUser);
        byte[] eMessageConcat = bulletinBoard.get(clientInfo.getReceiveIndex(), clientInfo.getReceiveTag());
        if (eMessageConcat != null){
            byte[] messageConcat = decryptAES(eMessageConcat, clientInfo.getReceiveKey());
            ByteBuffer buffer = ByteBuffer.wrap(messageConcat);
            int nextIndex = buffer.getInt();
            byte[] nextTag = new byte[8];
            buffer.get(nextTag);
            byte[] message = new byte[buffer.remaining()];
            buffer.get(message);
            clientInfo.setReceiveTag(nextTag);
            clientInfo.setReceiveIndex(nextIndex);
            clientInfo.setReceiveKey(KDF(clientInfo.getReceiveKey()));
            Message m = new Message(new String(message), fromUser);
            System.out.println(m.getFormat());
            if (!userList.isSelectionEmpty() && userList.getSelectedValue().equals(fromUser)) chatArea.append(m.getFormat());
            messageHistory.get(fromUser).add(m);
            amountCoins++;
        }
    }

    // ---------------------------------------- USER INTERFACE ---------------------------------------------------------

    private JFrame createUI(){
        int width = 600;
        int height = 600;
        JFrame frame = new JFrame("Bulletin Board");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        // user list left sidebar
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, height));
        frame.add(userScrollPane, BorderLayout.WEST);

        // text area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // new message field
        newMessageField = new JTextField();
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton sendButton = new JButton("Send");
        JButton newButton = new JButton("New");
        bottomPanel.add(newMessageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        bottomPanel.add(newButton, BorderLayout.WEST);
        frame.add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    handleSendMethod();
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        newMessageField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER){
                    try {
                        handleSendMethod();
                    } catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            }
        });

        newButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try{
                    addFriend(frame);
                } catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()){
                    handleUserSelection();
                }
            }
        });

        frame.setVisible(true);
        return frame;
    }

    private void doLogin(JFrame frame) throws NoSuchAlgorithmException, RemoteException {
        username = JOptionPane.showInputDialog(frame, "Choose an username: ");
        frame.setTitle("Bulletin Board of " + username);
    }

    private void addFriend(JFrame frame) throws NoSuchAlgorithmException, RemoteException {
        String myFriendsUsername = JOptionPane.showInputDialog(frame, "What is the username of you friend?");
        ClientInfo clientInfo = server.meet(username, myFriendsUsername);
        System.out.println("initial tags: sendTag:" + new String(clientInfo.getSendTag())
                + "\n\treceiveTag: " + new String(clientInfo.getReceiveTag()));
        clientInfo.setScheduler();
        clientInfo.getScheduler().scheduleAtFixedRate(() -> {
            try {
                receive(myFriendsUsername);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, clientInfo.getWaitTime(), TimeUnit.MILLISECONDS);
        clientInfoMap.put(myFriendsUsername, clientInfo);
        Set<String> users = clientInfoMap.keySet();
        userListModel.addElement(myFriendsUsername);
        messageHistory.put(myFriendsUsername, new ArrayList<>());
    }

    private void handleSendMethod() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, RemoteException, InvalidKeyException {
        if (userList.isSelectionEmpty() || amountCoins <= 0 || maxSendsPerSecond <= 0) return;
        String outgoingMessage = newMessageField.getText();
        String toUser = userList.getSelectedValue();
        this.send(outgoingMessage, toUser);
        newMessageField.setText("");
    }

    private void handleUserSelection(){
        String user = userList.getSelectedValue();
        chatArea.setText("");
        for (Message message : messageHistory.get(user)){
            chatArea.append(message.getFormat() + " \n");
        }
    }

    // ---------------------------------------- KEYS AND STUFF ---------------------------------------------------------
//    private static SecretKey generateAESKey(int keyLength) throws NoSuchAlgorithmException {
//        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//        keyGen.init(keyLength);
//        return keyGen.generateKey();
//    }

    private static byte[] encryptAES(byte[] text, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(text);
    }

    private static byte[] decryptAES(byte[] encryptedText, SecretKey key) throws NoSuchPaddingException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedBytes = cipher.doFinal(encryptedText);
        return decryptedBytes;
    }

    private SecretKey KDF(SecretKey originalKey) throws NoSuchAlgorithmException, InvalidKeyException {
        // Use HKDF for key derivation
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(originalKey);

        byte[] newKeyBytes = hmac.doFinal();

        // Create a new SecretKey from the derived bytes
        return new SecretKeySpec(newKeyBytes, "AES");
    }

    private static byte[] intToBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        return buffer.array();
    }

    private static byte[] concatenateArrays(byte[]... arrays) {
        int totalLength = 0;
        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];
        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, destPos, array.length);
            destPos += array.length;
        }

        return result;
    }

    // ------------------------------------------- BUMP ----------------------------------------------------------------

//    private void receiveState(String username, SecretKey toMeKey, SecretKey toThemKey,
//                              int toMeIndex, int toThemIndex, byte[] toMeTag, byte[] toThemTag){
//        ClientInfo c = new ClientInfo(username);
//        c.setSendKey(toThemKey);
//        c.setReceiveKey(toMeKey);
//        c.setReceiveIndex(toMeIndex);
//        c.setReceiveTag(toMeTag);
//        c.setSendTag(toThemTag);
//        c.setSendIndex(toThemIndex);
//        clientInfoMap.put(username, c);
//        Set<String> users = clientInfoMap.keySet();
//        String[] userArray = users.toArray(new String[users.size()]);
//        userList = new JList<>(userArray);
//    }

//    private void bump() throws NoSuchAlgorithmException, RemoteException{
//        Map<String, Client> otherClients = server.getClients();
//        for (Client otherClient : otherClients.values()){
//            SecretKey toMeKey = generateAESKey(256);
//            SecretKey toThemKey = generateAESKey(256);
//            int toMeIndex = s.nextInt();
//            int toThemIndex = s.nextInt();
//            byte[] toMeTag = new byte[8];
//            byte[] toThemTag = new byte[8];
//            s.nextBytes(toMeTag);
//            s.nextBytes(toThemTag);
//            otherClient.receiveState(username, toThemKey, toMeKey, toThemIndex, toMeIndex, toThemTag, toMeTag);
//
//            ClientInfo c = new ClientInfo(otherClient.getUsername());
//            c.setSendKey(toThemKey);
//            c.setReceiveKey(toMeKey);
//            c.setReceiveIndex(toMeIndex);
//            c.setReceiveTag(toMeTag);
//            c.setSendTag(toThemTag);
//            c.setSendIndex(toThemIndex);
//            c.setWaitTime(400);
//            c.getScheduler().scheduleAtFixedRate(() -> {
//                try {
//                    receive(c.getUsername());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }, 0, c.getWaitTime(), TimeUnit.MILLISECONDS);
//            clientInfoMap.put(otherClient.getUsername(), c);
//        }
//        Set<String> users = clientInfoMap.keySet();
//        String[] userArray = users.toArray(new String[users.size()]);
//        userList = new JList<>(userArray);
//    }
}
