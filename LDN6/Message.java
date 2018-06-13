import java.io.Serializable;

public class Message implements Serializable{
	public String message;
	public String sender;
	public long timestamp;
}

class LoginMessage extends Message{
	public String username;
}
class PrivateMessage extends Message{
	public String receiver;	
}
class BroadcastMessage extends Message{}