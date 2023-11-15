package Server;

import Client.Client;
import Common.ServerBoard;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ServerBoardImpl extends UnicastRemoteObject implements ServerBoard {
    private static Map<String, Client> users;

    public ServerBoardImpl() throws RemoteException {
        users = new HashMap<>();
    }

    public Map<String, Client> getClients() throws RemoteException{
        return users;
    }

    public boolean addUser(String username, Client c) throws RemoteException{
        if (users.containsKey(username)) return false;
        users.put(username, c);
        return true;
    }
}
