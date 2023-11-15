package Client;

import Common.BulletinBoard;
import Common.ServerBoard;
import Server.Server;

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
import java.util.concurrent.TimeUnit;

public class Client {
    private String username;
    private Map<String, ClientInfo> clientInfoMap;
    private Map<String, List<Message>> messageHistory;
    private JList<String> userList;
    private BulletinBoard bulletinBoard;
    private ServerBoard server;
    private static MessageDigest digest;
    private SecureRandom s;

    private JTextArea chatArea;
    private JTextField newMessageField;

    public Client (String username) throws RemoteException, NotBoundException, NoSuchAlgorithmException {
        this.username = username;
        this.clientInfoMap = new HashMap<>();
        digest = MessageDigest.getInstance("SHA-256");
        Registry myRegistry = LocateRegistry.getRegistry("localhost", 1234);
        bulletinBoard = (BulletinBoard) myRegistry.lookup("BulletinBoardServer");
        server = (ServerBoard) myRegistry.lookup("ServerBoard");
        s = new SecureRandom();
    }

    // ------------------------------------------ THE CLIENT -----------------------------------------------------------
    public static void main(String[] args) throws NotBoundException, NoSuchAlgorithmException, RemoteException {
        Client c = new Client("null");
        c.run();
    }

    public void run(){
        // TODO: wat zullen we hier doen
        JFrame frame = this.createUI();
        try {
            this.doLogin(frame);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void send(String message, String toUser) throws RemoteException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        int nextIndex = s.nextInt()% bulletinBoard.getSize();
        byte[] nextIndexBytes = intToBytes(nextIndex);
        byte[] nextTagBytes = new byte[8];
        s.nextBytes(nextTagBytes);
        byte[] messageBytes = message.getBytes();
        byte[] messageConcat = concatenateArrays(nextIndexBytes, nextTagBytes, messageBytes);
        ClientInfo clientInfo = clientInfoMap.get(toUser);
        byte[] eMessageConcat = encryptAES(messageConcat, clientInfo.getSendKey());
        bulletinBoard.write(clientInfo.getSendIndex(), eMessageConcat, digest.digest(nextTagBytes));
        clientInfo.setSendIndex(nextIndex);
        clientInfo.setSendTag(nextTagBytes);
        clientInfo.setSendKey(KDF(clientInfo.getSendKey()));
        Message m = new Message(message, toUser);
        messageHistory.get(toUser).add(m);
        chatArea.append(m.getFormat());
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
            chatArea.append(m.getFormat());
            messageHistory.get(fromUser).add(m);
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
        JScrollPane userScrollPane = new JScrollPane();
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
        bottomPanel.add(newMessageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
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

        userList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()){
                    handleUserSelection();
                }
            }
        });

        return frame;
    }

    private void doLogin(JFrame frame) throws NoSuchAlgorithmException, RemoteException {
        username = JOptionPane.showInputDialog(frame, "Choose an username: ");
        while (!server.addUser(username, this)){
            username = JOptionPane.showInputDialog(frame, "Choose an other username: ");
        }
        bump();
    }

    private void handleSendMethod() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, RemoteException, InvalidKeyException {
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

    private static SecretKey generateAESKey(int keyLength) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keyLength);
        return keyGen.generateKey();
    }

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

        byte[] salt = new byte[50];
        s.nextBytes(salt);

        byte[] newKeyBytes = hmac.doFinal(salt);

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

    private void receiveState(String username, SecretKey toMeKey, SecretKey toThemKey,
                              int toMeIndex, int toThemIndex, byte[] toMeTag, byte[] toThemTag){
        ClientInfo c = new ClientInfo(username);
        c.setSendKey(toThemKey);
        c.setReceiveKey(toMeKey);
        c.setReceiveIndex(toMeIndex);
        c.setReceiveTag(toMeTag);
        c.setSendTag(toThemTag);
        c.setSendIndex(toThemIndex);
        clientInfoMap.put(username, c);
        Set<String> users = clientInfoMap.keySet();
        String[] userArray = users.toArray(new String[users.size()]);
        userList = new JList<>(userArray);
    }

    private void bump() throws NoSuchAlgorithmException, RemoteException{
        Map<String, Client> otherClients = server.getClients();
        for (Client otherClient : otherClients.values()){
            SecretKey toMeKey = generateAESKey(256);
            SecretKey toThemKey = generateAESKey(256);
            int toMeIndex = s.nextInt() % bulletinBoard.getSize();
            int toThemIndex = s.nextInt() % bulletinBoard.getSize();
            byte[] toMeTag = new byte[8];
            byte[] toThemTag = new byte[8];
            s.nextBytes(toMeTag);
            s.nextBytes(toThemTag);
            otherClient.receiveState(username, toThemKey, toMeKey, toThemIndex, toMeIndex, toThemTag, toMeTag);

            ClientInfo c = new ClientInfo(otherClient.getUsername());
            c.setSendKey(toThemKey);
            c.setReceiveKey(toMeKey);
            c.setReceiveIndex(toMeIndex);
            c.setReceiveTag(toMeTag);
            c.setSendTag(toThemTag);
            c.setSendIndex(toThemIndex);
            c.setWaitTime(400);
            c.getScheduler().scheduleAtFixedRate(() -> {
                try {
                    receive(c.getUsername());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, 0, c.getWaitTime(), TimeUnit.MILLISECONDS);
            clientInfoMap.put(otherClient.getUsername(), c);
        }
        Set<String> users = clientInfoMap.keySet();
        String[] userArray = users.toArray(new String[users.size()]);
        userList = new JList<>(userArray);
    }

    public String getUsername(){
        return username;
    }

}
