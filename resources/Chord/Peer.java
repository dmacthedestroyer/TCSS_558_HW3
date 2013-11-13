import java.io.*;
import java.net.*;
import java.util.*;

public class Peer {
	/**
	 * The default port number for incoming network connections.
	 */
	public static final int DEFAULT_SERVER_PORT = 5507;
	/**
	 * The size of the ID/key space in bits.
	 */
	public static final int ID_SPACE = 10;
	/**
	 * The largest valid ID/key + 1.
	 */
	public static final long ID_LIMIT = 1 << ID_SPACE;
	/**
	 * The number of entries in the finger table, 1 <= n <= ID_SPACE.
	 */
	private static final int FINGER_ENTRIES = ID_SPACE;
	/**
	 * The period between invocations of background tasks, e.g., stabilize, fix fingers.
	 */
	private static final int BACKGROUND_TASK_PERIOD = 3000;

	private PeerInformation self;
	private PeerInformation successor;
	private PeerInformation predecessor;
	private PeerInformation[] fingerTable;
	private int next;
	private ServerSocket serverSocket;
	private Thread serverThread;
	private Thread periodicThread;

	public Peer() {
		self = new PeerInformation();
		fingerTable = new PeerInformation[FINGER_ENTRIES];
	}

	public boolean createNetwork() {
		return createNetwork((long)(ID_LIMIT * Math.random()));
	}

	public boolean createNetwork(long id) {
		if (serverSocket == null)
			if (!startServer())
				return false;
		self.chordID = id;
		successor = self;
		startPeriodicThread();
		return true;
	}

	public boolean connectToNetwork(String host) {
		return connectToNetwork(host, DEFAULT_SERVER_PORT);
	}

	public boolean connectToNetwork(String host, long id) {
		return connectToNetwork(host, DEFAULT_SERVER_PORT, id);
	}

	public boolean connectToNetwork(String host, int port) {
		try {
			return connectToNetwork(InetAddress.getByName(host), port);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean connectToNetwork(String host, int port, long id) {
		try {
			return connectToNetwork(InetAddress.getByName(host), port, id);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean connectToNetwork(InetAddress host) {
		return connectToNetwork(host, DEFAULT_SERVER_PORT);
	}

	public boolean connectToNetwork(InetAddress host, long id) {
		return connectToNetwork(host, DEFAULT_SERVER_PORT, id);
	}

	public boolean connectToNetwork(InetAddress host, int port) {
		return connectToNetwork(host, port, (long)(ID_LIMIT * Math.random()));
	}

	public boolean connectToNetwork(InetAddress host, int port, long id) {
		if (serverSocket == null)
			if (!startServer())
				return false;
		successor = null;
		PeerMessage mesg = new PeerMessage(self, id, -1);
		try {
			sendMessage(mesg, host, port);
			while (successor == null)
				Thread.sleep(1000);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		// Check for ID conflict
		if (successor.chordID == id)
			return false;
		self.chordID = id;
		startPeriodicThread();
		return true;
	}

	public void disconnectFromNetwork() {
		if (serverThread != null) {
			serverThread.interrupt();
			try {
				serverSocket.close();
			} catch (IOException e) {
			}
			serverSocket = null;
		}
		if (periodicThread != null)
			periodicThread.interrupt();
	}

	public void sendText(String text, long id) {
		handleText(new PeerMessage(id, text));
	}

	private boolean startServer() {
		return startServer(DEFAULT_SERVER_PORT);
	}

	private boolean startServer(int port) {
		while (true) {
			try {
				serverSocket = new ServerSocket(port);
			} catch (IOException e) {
				port++;
				continue;
			} catch (SecurityException e) {
				e.printStackTrace();
				return false;
			}
			break;
		}
		self.port = port;

		serverThread = new Thread() {
			{
				setDaemon(true);
			}

			public void run()  {
				while (!isInterrupted()) {
					try (Socket socket = serverSocket.accept()) {
						processConnection(socket);
					} catch (IOException e) {
					}
				}
			}
		};
		serverThread.start();

		return true;
	}

	private void processConnection(Socket socket) {
		try {
			ObjectInputStream socketIn = new ObjectInputStream(socket.getInputStream());
			PeerMessage mesg = (PeerMessage)socketIn.readObject();
			if (mesg.peer != null) {
				// First incoming remote connection will tell us our network address
				if (self.networkID == null && mesg.peer.chordID != self.chordID)
					self.networkID = socket.getLocalAddress();
				// Fill in the origin's network address if they didn't know it
				if (mesg.peer.networkID == null)
					mesg.peer.networkID = socket.getInetAddress();
			}
			switch (mesg.type) {
			case NOTIFY:
				if (predecessor == null || withinOpenInterval(mesg.peer.chordID, predecessor.chordID, self.chordID)) {
					if (mesg.peer.chordID == self.chordID)
						mesg.peer = self;
					predecessor = mesg.peer;
				}
				break;
			case FIND_SUCCESSOR:
				findSuccessor(mesg);
				break;
			case SUCCESSOR:
				if (mesg.peer.chordID == self.chordID)
					mesg.peer = self;
				if (mesg.fingerTableIndex == -1)
					successor = mesg.peer;
				else
					fingerTable[mesg.fingerTableIndex] = mesg.peer;
				break;
			case FIND_PREDECESSOR:
				PeerMessage returnMesg = new PeerMessage(PeerMessage.Type.PREDECESSOR, predecessor);
				sendMessage(returnMesg, mesg.peer);
				break;
			case PREDECESSOR:
				if (mesg.peer != null && withinOpenInterval(mesg.peer.chordID, self.chordID, successor.chordID)) {
					if (mesg.peer.chordID == self.chordID)
						mesg.peer = self;
					successor = mesg.peer;
				}
				returnMesg = new PeerMessage(PeerMessage.Type.NOTIFY, self);
				sendMessage(returnMesg, successor);
				break;
			case TEXT:
				handleText(mesg);
			}
		} catch (IOException e) {
			// We expect the deserialization to fail, throwing EOFException, when a "check predecessor" connection is made
			if (!(e instanceof EOFException) && !(e instanceof ConnectException))
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private void findSuccessor(PeerMessage mesg) {
		try {
			if (withinClosedInterval(mesg.nodeIdentifier, self.chordID, successor.chordID)) {
				PeerMessage returnMesg = new PeerMessage(mesg.fingerTableIndex, successor);
				sendMessage(returnMesg, mesg.peer);
			} else {
				PeerInformation n0 = closestPrecedingNode(mesg.nodeIdentifier);
				sendMessage(mesg, n0);
			}
		} catch (IOException e) {
		}
	}

	private PeerInformation closestPrecedingNode(long id) {
		for (int i = fingerTable.length - 1; i >= 0; i--)
			if (fingerTable[i] != null && withinOpenInterval(fingerTable[i].chordID, self.chordID, id))
				return fingerTable[i];
		return self;
	}

	private boolean withinOpenInterval(long a, long b, long c) {
		long offset = ID_LIMIT - 1 - b;
		long aPrime = (a + offset) % ID_LIMIT;
		long cPrime = (c + offset) % ID_LIMIT;
		return aPrime < cPrime;
	}

	private boolean withinClosedInterval(long a, long b, long c) {
		return b == c || withinOpenInterval(a, b, c + 1);
	}

	private void handleText(PeerMessage mesg) {
		try {
			if (mesg.nodeIdentifier == -1)
				System.out.println(mesg.text);
			else if (withinClosedInterval(mesg.nodeIdentifier, self.chordID, successor.chordID)) {
				mesg.nodeIdentifier = -1;
				sendMessage(mesg, successor);
			} else {
				PeerInformation n0 = closestPrecedingNode(mesg.nodeIdentifier);
				sendMessage(mesg, n0);
			}
		} catch (IOException e) {
		}
	}
	
	private void sendMessage(PeerMessage mesg, PeerInformation destination) throws IOException {
		sendMessage(mesg, destination.networkID, destination.port);
	}

	private void sendMessage(PeerMessage mesg, InetAddress networkID, int port) throws IOException {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(networkID, port), 10000);
			ObjectOutputStream socketOut = new ObjectOutputStream(socket.getOutputStream());
			socketOut.writeObject(mesg);
		} catch (IOException e) {
			System.out.println("Unable to connect to " + networkID + ":" + port);
			invalidatePeer(networkID, port);
			if (successor == self) {
				PeerMessage mesg2 = new PeerMessage(self, self.chordID, -1);
				findSuccessor(mesg2);
			}
			throw e;
		}
	}

	private void startPeriodicThread() {
		periodicThread = new Thread() {
			{
				setDaemon(true);
			}

			public void run()  {
				while (!isInterrupted()) {
					try {
						synchronized (this) {
							wait(BACKGROUND_TASK_PERIOD);
						}
					} catch (InterruptedException e) {
						break;
					}
					stabilize();
					fixFingers();
					checkPredecessor();
				}
			}
		};
		periodicThread.start();
	}

	private void stabilize() {
		PeerMessage mesg = new PeerMessage(PeerMessage.Type.FIND_PREDECESSOR, self);
		try {
			sendMessage(mesg, successor);
		} catch (IOException e) {
		}
	}

	private void fixFingers() {
		PeerMessage mesg = new PeerMessage(self, (self.chordID + (1 << next)) % ID_LIMIT, next);
		findSuccessor(mesg);
		next = (next + 1) % ID_SPACE;
	}

	private void checkPredecessor() {
		if (predecessor != null && predecessor.networkID != null)
			try {
				new Socket(predecessor.networkID, predecessor.port).close();
			} catch (IOException e) {
				invalidatePeer(predecessor.chordID);
			}
	}

	private void invalidatePeer(long id) {
		if (successor.chordID == id)
			successor = self;
		if (predecessor != null && predecessor.chordID == id)
			predecessor = null;
		for (int i = 0; i < fingerTable.length; i++)
			if (fingerTable[i] != null && fingerTable[i].chordID == id)
				fingerTable[i] = null;
	}

	private void invalidatePeer(InetAddress host, int port) {
		if (host == null)
			return;
		if (host.equals(successor.networkID) && successor.port == port)
			successor = self;
		if (predecessor != null && host.equals(predecessor.networkID) && predecessor.port == port)
			predecessor = null;
		for (int i = 0; i < fingerTable.length; i++)
			if (fingerTable[i] != null && host.equals(fingerTable[i].networkID) && fingerTable[i].port == port)
				fingerTable[i] = null;
	}

	private String internalState() {
		StringBuilder sb = new StringBuilder();
		sb.append("Successor: " + successor + "\n");
		sb.append("Predecessor: " + predecessor + "\n");
		sb.append("Finger table\n------------\n");
		for (int i = 0; i < fingerTable.length; i++)
			sb.append("[" + (self.chordID + (1 << i)) % ID_LIMIT + "]: " + fingerTable[i] + "\n");

		return sb.toString();
	}

	public static void main(String[] args) {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		boolean newNetwork = false;
		String input = null;

		while (true) {
			System.out.println("Create a new network or join an existing network?");
			System.out.println("  1. Create a new network");
			System.out.println("  2. Join an existing network");
			try {
				input = in.readLine();
			} catch (IOException e) {
				continue;
			}
			if (input.equals("1")) {
				newNetwork = true;
				break;
			} else if (input.equals("2"))
				break;
		}

		long id = -1;
		while (true) {
			System.out.println("Desired network ID, 0-" + (Peer.ID_LIMIT-1) + " [random]: ");
			try {
				input = in.readLine();
				if (input.length() == 0)
					break;
				id = Long.parseLong(input);
			} catch (NumberFormatException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
			if (id >= -1 && id < Peer.ID_LIMIT)
				break;
		}

		Peer peer = new Peer();
		if (newNetwork) {
			if (id == -1)
				peer.createNetwork();
			else
				peer.createNetwork(id);
		} else {
			String ip = "127.0.0.1";
			int port = Peer.DEFAULT_SERVER_PORT;
			do {
				System.out.print("Enter host to connect to [" + ip + "[:" + port + "]]: ");
				try {
					input = in.readLine();
					int index = input.indexOf(':');
					if (index >= 0) {
						port = Integer.parseInt(input.substring(index + 1));
						input = input.substring(0, index);
					}
					if (input.length() > 0)
						ip = input;
				} catch (IOException e) {
				}
			} while (id == -1 ? !peer.connectToNetwork(ip, port) : !peer.connectToNetwork(ip, port, id));
		}

		System.out.println("Enter a message in <id> <text> format, \"quit\", or \"state\".");
		while (true) {
			try {
				input = in.readLine();
				if (input.length() == 0)
					continue;
				Scanner sc = new Scanner(input);
				if (!sc.hasNextLong()) {
					input = sc.next();
					if (input.equals("quit")) {
						peer.disconnectFromNetwork();
						break;
					} else if (input.equals("state"))
						System.out.println(peer.internalState());
					else
						System.out.println("Enter a message in <id> <text> format, \"quit\", or \"state\" .");
					continue;
				}
				id = sc.nextLong();
				if (id < 0 || id >= ID_LIMIT) {
					System.out.println("Invalid ID.");
					continue;
				}
				sc.skip(sc.delimiter());
				peer.sendText(sc.nextLine(), id);
			} catch (IOException e) {
			}
		}
	}
}
