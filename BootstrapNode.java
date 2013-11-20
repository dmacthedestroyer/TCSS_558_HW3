import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Bootstraps an node and registers it with the RMI registry, given a name and
 * the port where the registry is located.
 * 
 * @author Jesse Carrigan
 */
public class BootstrapNode {

	/**
	 * Main method.
	 * 
	 * @param args
	 *            String arguments; expects (1) the port of the registry (2) the hostname (3) the port
	 *            this node will use (4) the name of the node.
	 * 
	 * @throws AlreadyBoundException 
	 * @throws IOException 
	 */
	public static void main(String[] args) throws AlreadyBoundException, IOException {
		if (args.length != 3) {
			Log.err("Usage: BootstrapNode <registryAddress> <registryPort> <m>");
		} else {
			String registryHostname = args[0];
			int registryPort = Integer.parseInt(args[1]);
			int m = Integer.parseInt(args[2]);

			Registry fakeDNS = LocateRegistry.getRegistry(registryHostname, registryPort);
			RMINode node = new RMINode(m, generateInetSocketAddress());
			fakeDNS.rebind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
			node.join(null);
			
			Log.out("seeded new chord network with node id " + node.getNodeKey());
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
