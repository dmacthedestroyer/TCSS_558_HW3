import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;


public class JoinNode {

	public static void main(String[] args) throws IOException, NotBoundException {
		if (args.length != 4) {
			Log.err("Usage: JoinNode <registryAddress> <registryPort> <nodeKeyFromNetwork> <newNodeKey>");
			return;
		}
		
		String registryHostname = args[0];
		int registryPort = Integer.parseInt(args[1]);
		String nodeKey = args[2];
		long newNodeKey = Long.parseLong(args[3]);

		Registry fakeDNS = LocateRegistry.getRegistry(registryHostname, registryPort);
		RMINodeServer fromNetwork = (RMINodeServer)fakeDNS.lookup(nodeKey);
		
		RMINode node = new RMINode(fromNetwork.getHashLength(), newNodeKey);
		fakeDNS.rebind("" + node.getNodeKey(), UnicastRemoteObject.exportObject(node, 0));
		
		node.join(fromNetwork);
		Log.out("Bound new node to id " + node.getNodeKey());
	}
}
