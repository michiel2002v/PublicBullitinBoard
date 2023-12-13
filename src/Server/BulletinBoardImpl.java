package Server;

import java.nio.ByteBuffer;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BulletinBoardImpl extends UnicastRemoteObject implements Common.BulletinBoard {
    private final Map<String, byte[]>[] bulletinBoard;
    private final Set<Integer> currentCoins;
    private final Set<Integer> resetCoinsNow;
    private final Set<Integer> resetCoinsNext;
    private final Set<String> users;
    private final MessageDigest digest;
    private final SecureRandom secureRandom;
    private final ScheduledExecutorService scheduler;
    private final int numberCells;
    private final int numberOfCoins;

    public BulletinBoardImpl(int numberCells) throws RemoteException, NoSuchAlgorithmException {
        this.numberCells = numberCells;
        this.numberOfCoins = 20;
        this.currentCoins = new HashSet<>();
        this.resetCoinsNext = new HashSet<>();
        this.resetCoinsNow = new HashSet<>();
        this.users = new HashSet<>();
        this.bulletinBoard = new HashMap[numberCells];
        for (int i = 0; i < numberCells; i++) {
            this.bulletinBoard[i] = new HashMap<>();
        }
        digest = MessageDigest.getInstance("SHA-256");
        secureRandom = new SecureRandom();
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::resetCoins, 1, 1, TimeUnit.MINUTES);
    }

    private void resetCoins() {
        synchronized (currentCoins) {
        synchronized (resetCoinsNext) {
        synchronized (resetCoinsNow) {
            currentCoins.clear();
            resetCoinsNow.addAll(resetCoinsNext);
            resetCoinsNext.clear();
        }
        }
        }
        System.out.println("coins resetting");
    }

    @Override
    public byte[] get(int index, byte[] tag) throws RemoteException {
        byte[] hashedTag = digest.digest(tag);
        if (bulletinBoard[index% bulletinBoard.length].containsKey(new String(hashedTag))) System.out.print("");
        else {
            return null;
        }
        byte[] message = bulletinBoard[index % bulletinBoard.length].remove(new String(hashedTag));
        int randomInt = secureRandom.nextInt();

        synchronized (currentCoins) {
            while (currentCoins.contains(randomInt)) randomInt = secureRandom.nextInt();
            currentCoins.add(randomInt);
        }
        byte[] randomIntBytes = intToBytes(randomInt);
        return concatenateArrays(randomIntBytes, message);
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

    private static byte[] intToBytes(int value) {
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
        buffer.putInt(value);
        return buffer.array();
    }

    @Override
    public boolean write(int index, byte[] eMessageConcat, byte[] hashedTag, Integer coin) throws RemoteException {
        if (!currentCoins.contains(coin)) return false;
        currentCoins.remove(coin);
        bulletinBoard[index % bulletinBoard.length].put(new String(hashedTag), eMessageConcat);
        return true;
    }

    @Override
    public List<Integer> login(String username) {
        if (users.contains(username)) return null;
        users.add(username);
        List<Integer> coins = new ArrayList<>(numberOfCoins + 1);
        int randomInt;
        synchronized (currentCoins) {
            for (int i=0; i<numberOfCoins; i++) {
                randomInt = secureRandom.nextInt();
                while (currentCoins.contains(randomInt)) randomInt = secureRandom.nextInt();
                currentCoins.add(randomInt);
                coins.add(randomInt);
            }
        }
        randomInt = secureRandom.nextInt();
        synchronized (resetCoinsNext) {
            while (resetCoinsNext.contains(randomInt) || resetCoinsNow.contains(randomInt)) randomInt = secureRandom.nextInt();
            resetCoinsNext.add(randomInt);
        }
        coins.add(randomInt);
        return coins;
    }

    @Override
    public List<Integer> getNewCoins(Integer resetCoin) {
        synchronized (resetCoinsNow) {
            if (!resetCoinsNow.contains(resetCoin)) return null;
            resetCoinsNow.remove(resetCoin);
        }
        List<Integer> coins = new ArrayList<>(numberOfCoins + 1);
        int randomInt;
        synchronized (currentCoins) {
            for (int i=0; i<numberOfCoins; i++) {
                randomInt = secureRandom.nextInt();
                while (currentCoins.contains(randomInt)) randomInt = secureRandom.nextInt();
                currentCoins.add(randomInt);
                coins.add(randomInt);
            }
        }
        randomInt = secureRandom.nextInt();
        synchronized (resetCoinsNext) {
            while (resetCoinsNext.contains(randomInt) || resetCoinsNow.contains(randomInt)) randomInt = secureRandom.nextInt();
            resetCoinsNext.add(randomInt);
        }
        coins.add(randomInt);
        return coins;
    }
}
