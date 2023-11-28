package Client;

public class Message {
    private final String message;
    private final String sender;

    public Message(String m, String s){
        message = m;
        sender = s;
    }

    public String getFormat(){
        return (sender + " :" + message + "\n");
    }
}
