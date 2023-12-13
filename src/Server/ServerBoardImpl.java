package Server;

import Client.ClientInfo;
import Common.ServerBoard;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

public class ServerBoardImpl extends UnicastRemoteObject implements ServerBoard {
    private final Map<String, ClientInfo> meetingPoint;
    private final SecureRandom s;

    public ServerBoardImpl() throws RemoteException {
        meetingPoint = new HashMap<>();
        s = new SecureRandom();
    }
    private static SecretKey generateAESKey(int keyLength) throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keyLength);
        return keyGen.generateKey();
    }

    public ClientInfo meet(String myUsername, String otherUsername) throws NoSuchAlgorithmException {
        if(myUsername.equals(otherUsername)){
            return null;
        }
        if (meetingPoint.containsKey(myUsername+otherUsername)){
            return meetingPoint.remove(myUsername+otherUsername);
        }
        SecretKey toMeKey = generateAESKey(256);
        SecretKey toYouKey = generateAESKey(256);
        int toMeIndex = s.nextInt(0, Integer.MAX_VALUE);
        int toYouIndex = s.nextInt(0, Integer.MAX_VALUE);
        byte[] toMeTag = new byte[8];
        byte[] toYouTag = new byte[8];
        s.nextBytes(toMeTag);
        s.nextBytes(toYouTag);

        ClientInfo c1 = new ClientInfo(otherUsername);
        c1.setSendKey(toYouKey);
        c1.setReceiveKey(toMeKey);
        c1.setReceiveIndex(toMeIndex);
        c1.setReceiveTag(toMeTag);
        c1.setSendTag(toYouTag);
        c1.setSendIndex(toYouIndex);
        c1.setWaitTime(500);

        ClientInfo c2 = new ClientInfo(myUsername);
        c2.setSendKey(toMeKey);
        c2.setReceiveKey(toYouKey);
        c2.setReceiveIndex(toYouIndex);
        c2.setReceiveTag(toYouTag);
        c2.setSendTag(toMeTag);
        c2.setSendIndex(toMeIndex);
        c2.setWaitTime(500);
        meetingPoint.put(otherUsername+myUsername, c2);
        return c1;
    }

}
