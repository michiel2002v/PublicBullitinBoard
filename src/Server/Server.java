package Server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Server extends Thread{

    private void startServer(int numberCells){
        try {
            Registry registry = LocateRegistry.createRegistry(1234);
            registry.rebind("BulletinBoardServer", new BulletinBoardImpl(numberCells));
            registry.rebind("ServerBoard", new ServerBoardImpl());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void run(){
        int numberCells = 8;
        this.startServer(numberCells);
    }

    public static void main(String[] args){
        Server server = new Server();
        server.startServer(8);
    }
}
