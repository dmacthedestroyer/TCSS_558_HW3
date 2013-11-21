import java.rmi.RemoteException;

/** 
 * The RMINodeServer exposes methods that a node can use to interact with other nodes via RMI.
 */
public interface RMINodeServer extends RMINodeClient {

	public int getHashLength() throws RemoteException;
	
	public long getNodeKey() throws RemoteException;
	
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
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException;
}
