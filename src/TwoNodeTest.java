import java.io.IOException;
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
			
			long keyspace = (long)Math.pow(2, m); 
			if(nodeCount > keyspace)
				throw new IllegalArgumentException("node count (" + nodeCount + ") cannot exceed max number of nodes (" + keyspace + ")");
			
			Log.out(String.format("port:%s m:%s node count:%s", port, m, nodeCount));
			
			RandomNumberSet randoms = new RandomNumberSet(keyspace);
			
            Registry registry = LocateRegistry.createRegistry(port);
            RemoteLogger fodder = new RemoteLogger();
            registry.bind("__fakeThing", UnicastRemoteObject.exportObject(fodder, 0));
            Log.out("Initialized registry on port " + port);
            
            ArrayList<RMINodeServer> nodes = new ArrayList<RMINodeServer>();
            Random random = new Random();

			RMINode seed = new RMINode(m, randoms.next());
			registry.bind("" + seed.getNodeKey(), UnicastRemoteObject.exportObject(seed, 0));
			seed.join(null);
			nodes.add(seed);
			
			for(int i=1; i<nodeCount; i++){
				RMINode nodeI = new RMINode(seed.getHashLength(), randoms.next());
				registry.bind("" + nodeI.getNodeKey(), UnicastRemoteObject.exportObject(nodeI, 0));
				
				nodeI.join(nodes.get(random.nextInt(nodes.size())));
				nodes.add(nodeI);
			}
        }
	}
}
