package Common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface BulletinBoard extends Remote{
    public void write (int index, byte[] eMessageConcat, byte[] hashedTag) throws RemoteException;
    public byte[] get (int index, byte[] tag) throws RemoteException;
}
