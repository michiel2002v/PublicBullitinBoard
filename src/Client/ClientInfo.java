package Client;

import javax.crypto.SecretKey;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientInfo {
    private SecretKey sendKey;
    private SecretKey receiveKey;
    private int sendIndex;
    private int receiveIndex;
    private byte[] sendTag;
    private byte[] receiveTag;
    private int waitTime;
    private ScheduledExecutorService scheduler;
    private String username;

    public ClientInfo(String username){
        scheduler = Executors.newScheduledThreadPool(1);
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

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
}
