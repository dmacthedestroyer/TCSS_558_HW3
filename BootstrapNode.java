import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.rmi.AlreadyBoundException;
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
	 * @throws AlreadyBoundException 
	 * @throws UnknownHostException 
	 */
	public static void main(String[] args) throws MalformedURLException, RemoteException, AlreadyBoundException, UnknownHostException {
		if (args.length != 2) {
			Log.err("Usage: BootstrapNode <registryAddress> <registryPort>");
		} else {
			String registryHostname = args[0];
			int registryPort = Integer.parseInt(args[1]);

			RMINode node = new RMINode(4, InetAddress.getLocalHost());
			Registry fakeDNS = LocateRegistry.getRegistry(registryHostname, registryPort);
			fakeDNS.rebind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
		}
	}
}
