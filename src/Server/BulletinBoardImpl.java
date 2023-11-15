package Server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class BulletinBoardImpl extends UnicastRemoteObject implements Common.BulletinBoard {
    private static Map<byte[], byte[]>[] bulletinBoard;
    private MessageDigest digest;
    private int numberCells;

    public BulletinBoardImpl(int numberCells) throws RemoteException, NoSuchAlgorithmException {
        this.numberCells = numberCells;
        bulletinBoard = new HashMap[numberCells];
        for (int i = 0; i < numberCells; i++) {
            bulletinBoard[i] = new HashMap<>();
        }
        digest = MessageDigest.getInstance("SHA-256");
    }

    @Override
    public byte[] get(int index, byte[] tag) throws RemoteException {
        byte[] hashedTag = digest.digest(tag);
        return bulletinBoard[index].get(hashedTag);
    }

    @Override
    public void write(int index, byte[] eMessageConcat, byte[] hashedTag) throws RemoteException {
        bulletinBoard[index].put(hashedTag, eMessageConcat);
    }

    @Override
    public int getSize(){
        return numberCells;
    }
}
