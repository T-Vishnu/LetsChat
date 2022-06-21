import java.io.*;
import java.util.*;
import java.net.*;
import java.text.SimpleDateFormat;

public class Server {
	private int port;
	private static int uniqueId;
	private boolean serverIsOn;
	private ArrayList<Thread_for_Client> AL;
	private SimpleDateFormat HMS;
	public Server(int p) {
		port = p;
		AL = new ArrayList<Thread_for_Client>();
		HMS = new SimpleDateFormat("HH:mm:ss");
	}
	
	public static void main(String[] args) {
		int portNumber = 1729;
		int l = args.length;
		if(l>=1) {
			try {
				portNumber = Integer.parseInt(args[0]);
			}
			catch(Exception e) {
				System.out.println("portNumber should be a number");
				return;
			}
			if(l>=2) {
				System.out.println("Invalid command-line arguments");
				return;
			}
		}
		Server server = new Server(portNumber);
		server.start();
	}
	
	public void start() {
		serverIsOn = true;
		try {
			ServerSocket serverSocket = new ServerSocket(port);
			while(serverIsOn) {
				System.out.println("["+HMS.format(new Date())+"] Server is running on port: " + port);
				Socket soc = serverSocket.accept();
				if(!serverIsOn) break;
				Thread_for_Client TC = new Thread_for_Client(soc);
				AL.add(TC);
				TC.start();
			}
			try {
				serverSocket.close();
				for(int i = 0;i<AL.size();i++) {
					Thread_for_Client TC = AL.get(i);
					try {
						TC.OIS.close();
						TC.OOS.close();
						TC.soc.close();
					}
					catch(IOException IOE) {
					}
				}
			}
			catch(Exception e) {
				System.out.println("["+HMS.format(new Date())+"]"+" Problem occured while closing the server: " + e);
			}
		}
		catch (Exception e) {
			System.out.println("["+HMS.format(new Date())+"] "+"Exception occured while creating new serverSocket: " + e);
		}
	}
	
	synchronized void remove(int ID) { //using synchronized keyword, we are allowing only a single thread to access the shared data or resources at a particular point of time i.e, Mutual exclusion.
		String loggedOutClient = new String();
		for(int i = 0;i<AL.size();i++) {
			Thread_for_Client TC = AL.get(i);
			if(TC.ID==ID) {
				loggedOutClient = TC.username;
				AL.remove(i);
				break;
			}
		}
		broadcast("* " + loggedOutClient + " left the chat. *");
	}
	
	private synchronized boolean broadcast(String message) { //using synchronized keyword, we are allowing only a single thread to access the shared data or resources at a particular point of time i.e, Mutual exclusion.
		String S[] = message.split(" ",3);
		boolean privateMsg = false;
		if(S[1].charAt(0)=='@') privateMsg = true;
		if(privateMsg) {
			String s1 = S[1].substring(1,S[1].length());
			message = "["+ HMS.format(new Date()) + "] " + "Private message from "+S[0]+" "+S[2];
			boolean present = false;
			for(int i = AL.size();--i >= 0;) {
				Thread_for_Client TC = AL.get(i);
				String s2 = TC.username;
				if(s1.equals(s2)) {
					if(!TC.allowToPrint(message)) {
						AL.remove(i);
						System.out.println("["+HMS.format(new Date())+"] " + TC.username + " is removed from the ArrayList.");
					}
					present = true;
					break;
				}
			}
			if(present != true) return false;
		}
		else {
			message = "[" + HMS.format(new Date()) + "] " + message;
			System.out.println(message);
			for(int i = AL.size(); --i >= 0;) {
				Thread_for_Client TC = AL.get(i);
				if(!TC.allowToPrint(message)) {
					AL.remove(i);
					System.out.println("["+HMS.format(new Date())+ "] "+ TC.username + " is removed from the ArrayList.");
				}
			}
		}
		return true;
	}

	class Thread_for_Client extends Thread {
		String username;
		int ID;
		Chat c;
		Socket soc;
		String date;
		ObjectInputStream OIS;
		ObjectOutputStream OOS;
		Thread_for_Client(Socket soc) {
			uniqueId = uniqueId + 1;
			ID = uniqueId;
			this.soc = soc;
			System.out.println("Thread is trying to create Object I/O Streams");
			try {
				OOS = new ObjectOutputStream(soc.getOutputStream());
				OIS  = new ObjectInputStream(soc.getInputStream());
				username = (String) OIS.readObject();
				broadcast("* " + username + " has joined the chat room. *");
			}
			catch (IOException e) {
				System.out.println("["+HMS.format(new Date())+"] Exception creating new Input/output Streams: " + e);
				return;
			}
			catch (ClassNotFoundException e) {
			}
            date = new Date().toString();
		}
		
		public void run() {
			boolean serverIsOn = true;
			while(serverIsOn) {
				try {
					c = (Chat)OIS.readObject();
				}
				catch (IOException eIO) {
					System.out.println("["+HMS.format(new Date())+"] "+username + " Exception reading Streams: " + eIO);
					break;				
				}
				catch(ClassNotFoundException eCNF) {
					break;
				}
				String message = c.message;
				switch(c.op) {
				case 'M':
					boolean B;
					try {
						B =  broadcast(username + ": " + message);
					}
					catch(Exception e) {
						message = ": Try not to send an empty line as chat.";
						B =  broadcast("Instructions from Server to " + username + message);
					}
					if(B==false){
						allowToPrint("* Sorry, No such user exists. *");
					}
					break;
				case 'L':
					System.out.println("["+HMS.format(new Date())+"] "+username + " disconnected with a LEFT message.");
					serverIsOn = false;
					break;
				case 'A':
					allowToPrint("List of the users active at [" + HMS.format(new Date()) + "]:");
					for(int i = 0; i < AL.size(); i++) {
						Thread_for_Client TC = AL.get(i);
						allowToPrint((i+1) + ". " + TC.username + " since " + TC.date);
					}
					break;
				}
			}
			remove(ID);
			close();
		}
		
		private void close() {
			try {
				if(OOS != null) OOS.close();
			}
			catch(Exception e) {}
			try {
				if(OIS != null) OIS.close();
			}
			catch(Exception e) {};
			try {
				if(soc != null) soc.close();
			}
			catch (Exception e) {}
		}

		private boolean allowToPrint(String msg) {
			if(!soc.isConnected()) {
				close();
				return false;
			}
			try {
				OOS.writeObject(msg);
			}
			catch(IOException eIO) {
				System.out.println("["+HMS.format(new Date())+"] " + "* Error sending message to " + username + " *");
			}
			return true;
		}
	}
}

class Chat implements Serializable {
	String message;
	char op;
	Chat(String m,char c) {
		message = m;
		op = c;
	}
}