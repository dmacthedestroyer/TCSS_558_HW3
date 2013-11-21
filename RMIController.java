import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * The RMIController provides the RMI registry service for the Chord network.
 * 
 * @author Jesse Carrigan
 * 
 */
public class RMIController implements Remote {
	/**
	 * Main method for the RMI controller; creates the registry.
	 * 
	 * @param args
	 * @throws RemoteException
	 * @throws AlreadyBoundException
	 */
	public static void main(String[] args) throws RemoteException, AlreadyBoundException {
		if (args.length != 1) {
			Log.err("Usage: RMIController <port>");
		} else {
			int port = Integer.parseInt(args[0]);
			Registry registry = LocateRegistry.createRegistry(port);
			RMIController emptyObjectToAddToTheRMIRegistryBecauseItWillShutDownIfNothingIsInIt = new RMIController();
			registry.bind("QWIJIBO", UnicastRemoteObject.exportObject(emptyObjectToAddToTheRMIRegistryBecauseItWillShutDownIfNothingIsInIt, 0));
			Log.out("Initialized registry on port " + port);
		}
	}
}
