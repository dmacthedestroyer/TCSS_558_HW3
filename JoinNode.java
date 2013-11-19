import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class JoinNode {

	public static void main(String[] args) throws IOException, NotBoundException {
		if (args.length != 3) {
			Log.err("Usage: BootstrapNode <registryAddress> <registryPort> <nodeKey>");
			return;
		} 
		
		String registryHostname = args[0];
		int registryPort = Integer.parseInt(args[1]);
		String nodeKey = args[2];

		Registry fakeDNS = LocateRegistry.getRegistry(registryHostname, registryPort);
		RMINodeServer fromNetwork = (RMINodeServer)fakeDNS.lookup(nodeKey);
		RMINode node = new RMINode(fromNetwork, generateInetSocketAddress());
		fakeDNS.rebind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
		Log.out("Bound new node to id " + node.getNodeKey());
	}
	
	private static InetSocketAddress generateInetSocketAddress() throws IOException{
		InetAddress localhost = InetAddress.getLocalHost();
		int port;
		try (ServerSocket incrediblyInefficientMeansOfAcquiringAPortNumber = new ServerSocket(0)){
			port = incrediblyInefficientMeansOfAcquiringAPortNumber.getLocalPort();
			incrediblyInefficientMeansOfAcquiringAPortNumber.close();
		}
		Log.out("port: " + port);
		return new InetSocketAddress(localhost, port);
	}
}
