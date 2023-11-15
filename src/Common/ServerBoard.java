package Common;

import Client.Client;

import java.rmi.RemoteException;
import java.util.Map;

public interface ServerBoard {
    public Map<String, Client> getClients() throws RemoteException;
    public boolean addUser(String username, Client c) throws RemoteException;
}
