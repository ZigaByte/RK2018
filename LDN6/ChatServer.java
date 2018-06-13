import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class ChatServer {

	protected int serverPort = 1234;
	protected List<ChatServerConnector> clients = new ArrayList<ChatServerConnector>(); // list of clients

	public static void main(String[] args) throws Exception {
		new ChatServer();
	}

	public ChatServer() {
		ServerSocket serverSocket = null;

		// create socket
		try {
			serverSocket = new ServerSocket(this.serverPort); // create the ServerSocket
		} catch (Exception e) {
			System.err.println("[system] could not create socket on port " + this.serverPort);
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// start listening for new connections
		System.out.println("[system] listening ...");
		try {
			while (true) {
				Socket newClientSocket = serverSocket.accept(); // wait for a new client connection

				ChatServerConnector conn = new ChatServerConnector(this, newClientSocket); // create a new thread for
																							// communication with the
																							// new client
				synchronized (this) {
					clients.add(conn); // add client to the list of clients
				}
				conn.start(); // run the new thread
			}
		} catch (Exception e) {
			System.err.println("[error] Accept failed.");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// close socket
		System.out.println("[system] closing server socket ...");
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	// send a message to all clients connected to the server
	public void sendToAllClients(String message) throws Exception {
		Message m = new Message();
		m.message = message;

		Iterator<ChatServerConnector> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			ChatServerConnector client = (ChatServerConnector) i.next(); // get the socket for communicating with this
																			// client
			client.sendMessage(m);
		}
	}

	public void removeClient(Socket socket) {
		synchronized (this) {
			clients.remove(socket);
		}
	}

	public boolean sendToClientByUsername(String receiver, String message) throws Exception {
		Message m = new Message();
		m.message = message;

		Iterator<ChatServerConnector> i = clients.iterator();
		while (i.hasNext()) { // iterate through the client list
			ChatServerConnector client = (ChatServerConnector) i.next();
			if (client.getUsername().trim().equals(receiver.trim())) {
				client.sendMessage(m);
				return true;
			}
		}
		return false;
	}
}

class ChatServerConnector extends Thread {
	private ChatServer server;
	private Socket socket;
	private ObjectOutputStream outData;

	private boolean loggedIn = false;
	private String username;

	public ChatServerConnector(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;

		try {
			outData = new ObjectOutputStream(socket.getOutputStream());
			outData.flush();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void sendMessage(Message m) {
		try {
			outData.writeObject(m);
		} catch (Exception e) {
			System.err.println("[system] could not send message to a client");
			e.printStackTrace(System.err);
		}
	}

	public void run() {
		System.out.println(
				"[system] connected with " + this.socket.getInetAddress().getHostName() + ":" + this.socket.getPort());

		ObjectInputStream in;
		try {
			in = new ObjectInputStream(this.socket.getInputStream()); // create input stream for listening for incoming
																		// messages
		} catch (IOException e) {
			System.err.println("[system] could not open input stream!");
			e.printStackTrace(System.err);
			this.server.removeClient(socket);
			return;
		}

		while (true) { // infinite loop in which this thread waits for incoming messages and processes
						// them
			String msg_received = "";
			String msg_send = "";
			boolean sendToAll = false;

			try {
				Object m = in.readObject();
				if (m instanceof LoginMessage && !loggedIn) {
					loggedIn = true;
					username = ((LoginMessage) m).username;
					msg_received = username + " has just logged in.";
					msg_send = "Server: " + msg_received;
					sendToAll = true;

				} else if (m instanceof BroadcastMessage && loggedIn) {
					msg_received = ((BroadcastMessage) m).message;

					msg_send = username + ": " + msg_received;
					sendToAll = true;

				} else if (m instanceof BroadcastMessage && !loggedIn) {
					Message newM = new Message();
					newM.message = "Server: You must login before sending messages. Use /login <username>.";
					sendMessage(newM);

				} else if (m instanceof PrivateMessage && loggedIn) {
					msg_received = ((PrivateMessage) m).sender + " -> " + ((PrivateMessage) m).receiver + ": "
							+ ((PrivateMessage) m).message;

					String message = "[" + ((PrivateMessage) m).sender + "] " + ((PrivateMessage) m).message;
					try {
						boolean success = this.server.sendToClientByUsername(((PrivateMessage) m).receiver, message);
						if (!success) {
							Message newM = new Message();
							newM.message = ((PrivateMessage) m).receiver + " is currently not available.";
							sendMessage(newM);
							System.out.println("[system] User " + ((PrivateMessage) m).receiver  +  " not found.");
						}
					} catch (Exception e) {
						System.err.println(
								"[system] there was a problem while sending the message to a client by username");
						e.printStackTrace(System.err);
						continue;
					}

				} else if (m instanceof PrivateMessage && !loggedIn) {
					Message newM = new Message();
					newM.message = "Server: You must login before sending messages. Use /login <username>.";
					sendMessage(newM);
				}

				if (m instanceof Message) {
					long yourmilliseconds = ((Message) m).timestamp;
					SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm");
					Date resultdate = new Date(yourmilliseconds);
					msg_received = sdf.format(resultdate) + " - " + msg_received;
					msg_send = sdf.format(resultdate) + " - " + msg_send;
				}

			} catch (Exception e) {
				System.err.println("[system] there was a problem while reading message client on port "
						+ this.socket.getPort() + ", removing client");
				e.printStackTrace(System.err);
				this.server.removeClient(this.socket);
				return;
			}

			if (msg_received.length() == 0)
				continue;
			System.out.println("[RKchat] [" + this.socket.getPort() + "] : " + msg_received); // print the incoming

			if (msg_send.length() != 0 && sendToAll) {
				try {
					this.server.sendToAllClients(msg_send); // send message to all clients
				} catch (Exception e) {
					System.err.println("[system] there was a problem while sending the message to all clients");
					e.printStackTrace(System.err);
					continue;
				}
			}
		}
	}

	public String getUsername() {
		if (username == null)
			return "";
		return username;
	}
}
