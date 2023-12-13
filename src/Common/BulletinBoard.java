package Common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface BulletinBoard extends Remote{
    public boolean write (int index, byte[] eMessageConcat, byte[] hashedTag, Integer coin) throws RemoteException;
    public byte[] get (int index, byte[] tag) throws RemoteException;
    public List<Integer> login(String username) throws RemoteException;
    public List<Integer> getNewCoins(Integer resetCoin) throws RemoteException;
}
