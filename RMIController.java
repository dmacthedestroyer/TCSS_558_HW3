import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * The RMIController provides the RMI registry service for the Chord network.
 * @author Jesse Carrigan
 *
 */
public class RMIController {

	/**
	 * Main method for the RMI controller; creates the registry.
	 * @param args
	 */
	public static void main(String[] args) {
        if (args.length != 2) {
            Log.err("Usage: RMIController <port> <registryName>");
        } else {
            try {
                int port = Integer.parseInt(args[0]);
                String registryName = args[1];
                Registry registry = LocateRegistry.createRegistry(port);
                Log.out("Initialized registry " + registryName + " on port " + port);
            } catch (Exception e) {
                Log.err("RMIController exception:");
                Log.err(e.toString());
            }
        }
	}
	
}
