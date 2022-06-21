import java.io.*;
import java.util.*;
import java.net.*;
public class Client  {
	private String username;
	private String server;
	private int port;
	private Socket socket;
	private ObjectInputStream OIS;
	private ObjectOutputStream OOS;
	Client(String u, String s,int p) {
		username = u;
		server = s;
		port = p;
	}
	
	public static void main(String[] args) {
		int portNumber = 1729;
		String serverAddress = "localhost";
		String userName = new String();
		int l=args.length;
		if(l>=1) {
			try {
				portNumber = Integer.parseInt(args[0]);
			}
			catch(NumberFormatException eN) {
				System.out.println("portNumber should be a number");
				return;
			}
			if(l>1) {
				System.out.println("Invalid command-line arguments");
				return;
			}
		}
		Scanner sc = new Scanner(System.in);
		System.out.print("Enter username: ");
		userName = sc.nextLine();
		Client clnt = new Client(userName,serverAddress,portNumber);
		if(!clnt.start()) {
			sc.close();
			return;
		}
		System.out.println("Welcome to the chat, here are a set of instructions to make you familiar with our application:\n1. Type ACTIVE to see the current active clients.\n2. Type your message directly to broadcast it to all active clients.\n3. Type @username<space>yourmessage to send private message to a client.\n4. Type LEAVE to exit from the Chat.");
		System.out.println();
		while(true) {
			String msg = sc.nextLine();
			if(msg.equals("LEAVE")) {
				clnt.writeToServer(new Chat("",'L'));
				break;
			}
			else if(msg.equals("ACTIVE")) {
				clnt.writeToServer(new Chat("",'A'));				
			}
			else {
				clnt.writeToServer(new Chat(msg,'M'));
			}
		}
		sc.close();
		clnt.disconnect();	
	}
	
	public boolean start() {
		try {
			socket = new Socket(server,port);
		} 
		catch(Exception e) {
			System.out.println("Problem while connecting to the Server: " + e);
			return false;
		}
		System.out.println("\nYou are now connected to the Server " + socket.getInetAddress() + ":" + socket.getPort()+"\n");
		try {
			OIS = new ObjectInputStream(socket.getInputStream());
			OOS = new ObjectOutputStream(socket.getOutputStream());
		}
		catch (IOException IOE) {
			System.out.println("Problem occured while creating new I/O streams: " + IOE);
			return false;
		}
		try {
			new getFromServer().start();
			OOS.writeObject(username);
		}
		catch (IOException IOE) {
			System.out.println("Problem occured while logging in: " + IOE);
			disconnect();
			return false;
		}
		return true;
	}
	
	private void disconnect() {
		try { 
			if(OIS != null) OIS.close();
		}
		catch(Exception e) {}
		try {
			if(OOS != null) OOS.close();
		}
		catch(Exception e) {}
        try{
			if(socket != null) socket.close();
		}
		catch(Exception e) {}
	}
	
	void writeToServer(Chat c) {
		try {
			OOS.writeObject(c);
		}
		catch(IOException IOE) {
			System.out.println("Problem while writing to Server: " + IOE);
		}
	}

	class getFromServer extends Thread {
		public void run() {
			while(true) {
				try {
					String msg = (String) OIS.readObject();
					System.out.println(msg);
				}
				catch(Exception IOE) {
					System.out.println("* Server has closed the connection: " + IOE +" *");
					break;
				}
			}
		}
	}
}