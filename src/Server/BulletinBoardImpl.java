package Server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class BulletinBoardImpl extends UnicastRemoteObject implements Common.BulletinBoard {
    private final Map<String, byte[]>[] bulletinBoard;
    private final MessageDigest digest;
    private final int numberCells;

    public BulletinBoardImpl(int numberCells) throws RemoteException, NoSuchAlgorithmException {
        this.numberCells = numberCells;
        this.bulletinBoard = new HashMap[numberCells];
        for (int i = 0; i < numberCells; i++) {
            this.bulletinBoard[i] = new HashMap<>();
        }
        digest = MessageDigest.getInstance("SHA-256");
    }

    @Override
    public byte[] get(int index, byte[] tag) throws RemoteException {
        byte[] hashedTag = digest.digest(tag);
        System.out.println("GET index: " + index % bulletinBoard.length + " hashedTag: " + new String(hashedTag)+ " tag: " + new String(tag));
        if (bulletinBoard[index% bulletinBoard.length].containsKey(new String(hashedTag))) System.out.println("we found a message!");
        return bulletinBoard[index % bulletinBoard.length].remove(new String(hashedTag));
    }

    @Override
    public void write(int index, byte[] eMessageConcat, byte[] hashedTag) throws RemoteException {
        System.out.println("WRITE index: " + index%bulletinBoard.length + " hashedTag: " + new String(hashedTag));
        bulletinBoard[index % bulletinBoard.length].put(new String(hashedTag), eMessageConcat);
    }
}
