import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
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
	 *            String arguments; expects the name of the node and the port of
	 *            the registry, respectively.
	 * 
	 * @throws MalformedURLException
	 * @throws RemoteException
	 */
	public static void main(String[] args) throws MalformedURLException,
			RemoteException {
		if (args.length != 4) {
			Log.err("Usage: BootstrapNode <registryPort> <nodeHostname> <nodePort> <nodeName>");
		} else {
			int registryPort = Integer.parseInt(args[0]);
			String nodeHostname = args[1];
			int nodePort = Integer.parseInt(args[2]);
			String nodeName = args[3];

			RMINode node = new RMINode(4, createURL(nodePort, nodeHostname));
			RMINodeServer stub = (RMINodeServer) UnicastRemoteObject
					.exportObject(node, 0);
			Registry fakeDNS = LocateRegistry.getRegistry(registryPort);
			fakeDNS.rebind(nodeName, stub);
		}
	}

	/**
	 * Creates a URL given a port and hostname.
	 * 
	 * @param port
	 *            The port.
	 * @param host
	 *            The host name.
	 * @return A URL object.
	 */
	protected static URL createURL(int port, String host) {
		URL url = null;
		try {
			url = new URL("http", host, port, null);
		} catch (MalformedURLException e) {
			Log.err(e.getMessage());
		}
		return url;
	}

}
