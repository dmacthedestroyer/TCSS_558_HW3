import java.io.Serializable;
import java.net.URL;
import java.rmi.RemoteException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RMINode implements RMINodeServer {

	/**
	 * Creates the first node in a Chord network.
	 * @param hashLength the logarithm of the total number of nodes in the network (to base 2)
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(int hashLength, URL url) {
		if(url == null)
			throw new NullPointerException("'url' must not be null");
		
		throw new NotImplementedException();
	}
	
	/**
	 * Creates a new node to join the network that fromNetwork exists in.
	 * @param fromNetwork an arbitrary node in the network
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(RMINode fromNetwork, URL url) {
		if(fromNetwork == null)
			throw new NullPointerException("'fromNetwork' must not be null");
		if(url == null)
			throw new NullPointerException("'url' must not be null");
		
		throw new NotImplementedException();
	}
	
	@Override
	public Serializable get(String key) throws RemoteException {
		throw new NotImplementedException();
	}

	@Override
	public void put(String key, Serializable value) throws RemoteException {
		throw new NotImplementedException();
	}

	@Override
	public void delete(String key) throws RemoteException {
		throw new NotImplementedException();
	}

	@Override
	public RMINodeServer findSuccessor(long key) throws RemoteException {
		throw new NotImplementedException();
	}

	@Override
	public RMINodeServer findPredecessor(long key) throws RemoteException {
		throw new NotImplementedException();
	}

	@Override
	public RMINodeServer checkPredecessor(NodeData potentialPredecessor) throws RemoteException {
		throw new NotImplementedException();
	}
}
