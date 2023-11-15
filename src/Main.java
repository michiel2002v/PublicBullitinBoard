//import Client.Client;
//import Server.Server;
//
//import java.rmi.NotBoundException;
//import java.rmi.RemoteException;
//import java.security.NoSuchAlgorithmException;
//
//public class Main {
//    public static void main(String[] args) throws NotBoundException, RemoteException, NoSuchAlgorithmException, InterruptedException {
//        Server s = new Server();
//        s.start();
//        Thread.sleep(1000);
//        int aantalClients = 2;
//        for(int i = 0; i < aantalClients; i++){
//            Client c = new Client("client" + i, s);
//            c.start();
//            Thread.sleep(1000);
//        }
//    }
//}