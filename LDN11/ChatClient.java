import java.io.*;
import java.net.*;
import java.util.Scanner;
import javax.net.ssl.*;
import java.security.*;

public class ChatClient extends Thread {
	protected int serverPort = 1234;
	private String username;
	private boolean loggedIn = false;

	public static void main(String[] args) throws Exception {
		System.out.println("Izberite uporabnika: 0, 1 ali 2");
		Scanner scanner = new Scanner(System.in);
		int user = scanner.nextInt();

		String[] kljuci = {"alfa", "beta", "gama"};
		
		new ChatClient(kljuci[user]);
	}

	public ChatClient(final String kljuc) throws Exception {
		SSLSocket socket = null;
		ObjectInputStream in = null;
		ObjectOutputStream out = null;

		// connect to the chat server
		try {
			System.out.println("[system] connecting to chat server ...");
			//socket = new Socket("localhost", serverPort); // create socket connection
			
			String passphrase = "password";

			// preberi datoteko s streznikovim certifikatom
			KeyStore serverKeyStore = KeyStore.getInstance("JKS");
			serverKeyStore.load(new FileInputStream("keys/server.public"), passphrase.toCharArray());

			// preberi datoteko s svojim certifikatom in tajnim kljucem
			KeyStore clientKeyStore = KeyStore.getInstance("JKS");
			clientKeyStore.load(new FileInputStream("keys/"+kljuc+".private"), passphrase.toCharArray());

			// vzpostavi SSL kontekst (komu zaupamo, kaksni so moji tajni kljuci in certifikati)
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(serverKeyStore);
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(clientKeyStore, passphrase.toCharArray());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

			// kreiramo socket
			SSLSocketFactory sf = sslContext.getSocketFactory();
			socket = (SSLSocket) sf.createSocket("localhost", serverPort);
			socket.setEnabledCipherSuites(new String[] { "TLS_RSA_WITH_AES_128_CBC_SHA" }); // dovoljeni nacin kriptiranja (CipherSuite)
			socket.startHandshake(); // eksplicitno sprozi SSL Handshake

			out = new ObjectOutputStream(socket.getOutputStream()); // create output stream for sending messages
			in = new ObjectInputStream(socket.getInputStream()); // create input stream for listening for incoming
																	// messages
			System.out.println("[system] connected");

			ChatClientMessageReceiver message_receiver = new ChatClientMessageReceiver(in); // create a separate thread
																							// for listening to messages
																							// from the chat server
			message_receiver.start(); // run the new thread
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		// read from STDIN and send messages to the chat server
		BufferedReader std_in = new BufferedReader(new InputStreamReader(System.in));
		String userInput;
		while ((userInput = std_in.readLine()) != null) { // read a line from the console
			Scanner s = new Scanner(userInput);

			Message m = null;

			if (s.hasNext()) {
				String command = s.next();
				switch (command) {
				case "/login":
					m = new LoginMessage();
					if (s.hasNext()) {
						String u = s.next();
						((LoginMessage) m).username = u;
						if (loggedIn) { // Don't send anything
							System.out.println("[system] You are already logged in.");
							continue;
						} else {
							username = u;
							loggedIn = true;
						}

					} else {
						System.out.println("[system] Invalid command format. Please use /login <username>.");
						continue;
					}
					break;
					
					
				case "/msg":
					m = new PrivateMessage(); {
					String r = null;
					String msg = null;
					if (s.hasNext()) {
						r = s.next();
					} else {
						System.out.println("[system] Invalid command format. Please use /msg <receiver> <text>.");
						continue;
					}
					if (s.hasNext()) {
						s.useDelimiter("\n");
						msg = s.next().trim();
						s.useDelimiter(" ");
					} else {
						System.out.println("[system] Invalid command format. Please use /msg <receiver> <text>.");
						continue;
					}
					((PrivateMessage)m).receiver = r;
					m.message = msg;

				}
					break;
				default:
					m = new BroadcastMessage();
					((BroadcastMessage) m).message = userInput;
					break;
				}
			}
			if (m == null)
				continue;

			m.timestamp = System.currentTimeMillis();
			m.sender = username;
			this.sendMessage(m, out); // send the message to the chat server
		}

		// cleanup
		out.close();
		in.close();
		std_in.close();
		socket.close();
	}

	private void sendMessage(Message message, ObjectOutputStream out) {
		try {
			out.writeObject(message);
			out.flush(); // ensure the message has been sent
		} catch (IOException e) {
			System.err.println("[system] could not send message");
			e.printStackTrace(System.err);
		}
	}
}

// wait for messages from the chat server and print the out
class ChatClientMessageReceiver extends Thread {
	private ObjectInputStream in;

	public ChatClientMessageReceiver(ObjectInputStream in) {
		this.in = in;
	}

	public void run() {
		try {
			Object m = null;
			while ((m = this.in.readObject()) != null) { // read new message
				if (m instanceof Message) {
					Message message = (Message) m;
					System.out.println("[RKchat] " + message.message); // print the message to the console
				}
			}
		} catch (Exception e) {
			System.err.println("[system] could not read message");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}
}
