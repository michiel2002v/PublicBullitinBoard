package Common;

import Client.Client;
import Client.ClientInfo;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public interface ServerBoard extends Remote {
    ClientInfo meet(String myUsername, String otherUsername) throws RemoteException, NoSuchAlgorithmException;
    /*
    request user info --> store voor de andere user
        1. kijk of er al info is
        2. maak/return info
     */
}
