import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.rmi.AlreadyBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Random;


public class TwoNodeTest {

	public static void main(String[] args) throws AlreadyBoundException, IOException {
        if (args.length != 3) {
            Log.err("Usage: RMIController <port> <m> <node count>");
        } else {
            int port = Integer.parseInt(args[0]);
			int m = Integer.parseInt(args[1]);
			int nodeCount = Integer.parseInt(args[2]);
			
			if(nodeCount > Math.pow(2, m))
				throw new IllegalArgumentException("node count (" + nodeCount + ") cannot exceed max number of nodes (" + Math.pow(2, m) + ")");
			
            Registry registry = LocateRegistry.createRegistry(port);
            RemoteLogger fodder = new RemoteLogger();
            registry.bind("__fakeThing", UnicastRemoteObject.exportObject(fodder, 0));
            Log.out("Initialized registry on port " + port);
            
            ArrayList<RMINodeServer> nodes = new ArrayList<RMINodeServer>();
            Random random = new Random();

			RMINode seed = new RMINode(m, generateInetSocketAddress());
			registry.bind("" + seed.getNodeKey(), UnicastRemoteObject.exportObject(seed, 0));
			seed.join(null);
			nodes.add(seed);
			
			for(int i=1; i<nodeCount; i++){
				RMINode nodeI = new RMINode(seed.getHashLength(), generateInetSocketAddress());
				registry.bind("" + nodeI.getNodeKey(), UnicastRemoteObject.exportObject(nodeI, 0));
				
				nodeI.join(nodes.get(random.nextInt(nodes.size())));
				nodes.add(nodeI);
//				Log.out("Bound new node to id " + nodeI.getNodeKey());
			}
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
