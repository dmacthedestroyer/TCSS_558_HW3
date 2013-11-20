import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class TwoNodeTest {

	public static void main(String[] args) throws AlreadyBoundException, IOException {
        if (args.length != 2) {
            Log.err("Usage: RMIController <port> <m>");
        } else {
            int port = Integer.parseInt(args[0]);
            Registry registry = LocateRegistry.createRegistry(port);
            RemoteLogger fodder = new RemoteLogger();
            registry.bind("__fakeThing", UnicastRemoteObject.exportObject(fodder, 0));
            Log.out("Initialized registry on port " + port);
            
			int m = Integer.parseInt(args[1]);

			RMINode node = new RMINode(m, generateInetSocketAddress());
			registry.bind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
			node.join(null);
			
			Log.out("seeded new chord network with node id " + node.getNodeKey());

			RMINode node2 = new RMINode(node.getHashLength(), generateInetSocketAddress());
			registry.bind("" + node2.getNodeKey(), UnicastRemoteObject.exportObject(node2, 0));
			
			node2.join(node);
			Log.out("Bound new node to id " + node2.getNodeKey());
        }
	}

	private static InetSocketAddress generateInetSocketAddress() throws IOException{
		InetAddress localhost = InetAddress.getLocalHost();
		int port;
		try (ServerSocket incrediblyInefficientMeansOfAcquiringAPortNumber = new ServerSocket(0)){
			port = incrediblyInefficientMeansOfAcquiringAPortNumber.getLocalPort();
			incrediblyInefficientMeansOfAcquiringAPortNumber.close();
		}
		return new InetSocketAddress(localhost, port);
	}

}
