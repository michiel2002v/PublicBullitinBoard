package Client;

import javax.crypto.SecretKey;
import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ClientInfo implements Serializable{
    private static final long serialVersionUID = 1L;

    private SecretKey sendKey;
    private SecretKey receiveKey;
    private int sendIndex;
    private int receiveIndex;
    private byte[] sendTag;
    private byte[] receiveTag;
    private int waitTime;
    private transient ScheduledExecutorService scheduler;
    private String username;

    public ClientInfo(String username){
        this.username = username;
    }

    public String getUsername(){
        return username;
    }

    public SecretKey getReceiveKey() {
        return receiveKey;
    }

    public void setReceiveKey(SecretKey receiveKey) {
        this.receiveKey = receiveKey;
    }

    public int getSendIndex() {
        return sendIndex;
    }

    public void setSendIndex(int sendIndex) {
        this.sendIndex = sendIndex;
    }

    public int getReceiveIndex() {
        return receiveIndex;
    }

    public void setReceiveIndex(int receiveIndex) {
        this.receiveIndex = receiveIndex;
    }

    public byte[] getSendTag() {
        return sendTag;
    }

    public void setSendTag(byte[] sendTag) {
        this.sendTag = sendTag;
    }

    public byte[] getReceiveTag() {
        return receiveTag;
    }

    public void setReceiveTag(byte[] receiveTag) {
        this.receiveTag = receiveTag;
    }

    public SecretKey getSendKey() {
        return sendKey;
    }

    public void setSendKey(SecretKey sendKey) {
        this.sendKey = sendKey;
    }

    public ScheduledExecutorService getScheduler(){
        return scheduler;
    }

    public void setScheduler(){
        scheduler = Executors.newScheduledThreadPool(1);
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
}
