package Client;

public class Message {
    private String message;
    private String sender;

    public Message(String m, String s){
        message = m;
        sender = s;
    }

    public String getFormat(){
        return (sender + " :" + message);
    }
}
