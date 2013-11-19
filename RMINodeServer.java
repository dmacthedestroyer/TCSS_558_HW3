import java.rmi.RemoteException;

public interface RMINodeServer extends RMINodeClient {
	/**
	 * Finds the successor node for the given key
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public RMINodeServer findSuccessor(long key) throws RemoteException;
	
	/**
	 * Finds the predecessor node for the given key
	 * @param key
	 * @return
	 * @throws RemoteException
	 */
	public RMINodeServer findPredecessor(long key) throws RemoteException;
	
	/**
	 * Check whether the specified node should be your predecessor
	 * @param potentialPredecessor
	 * @return
	 * @throws RemoteException
	 */
	public RMINodeServer checkPredecessor(NodeData potentialPredecessor) throws RemoteException;
}
