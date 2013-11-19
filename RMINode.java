import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.rmi.RemoteException;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RMINode implements RMINodeServer {

	private long nodeKey;
	
	private int hashLength;
	
	private FingerTable fingerTable;
	
	private RMINodeServer predecessor;
	
	/**
	 * Creates the first node in a Chord network.
	 * @param hashLength the logarithm of the total number of nodes in the network (to base 2)
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(int hashLength, InetSocketAddress url) throws RemoteException {
		if(url == null)
			throw new NullPointerException("'url' must not be null");
		
		this.hashLength = hashLength;
		this.nodeKey = new KeyHash<InetSocketAddress>(url, hashLength).getHash();
		fingerTable = new FingerTable(this);
		for(Finger f: fingerTable)
			f.setNode(this);
	}
	
	/**
	 * Creates a new node to join the network that fromNetwork exists in.
	 * @param fromNetwork an arbitrary node in the network
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(RMINodeServer fromNetwork, InetSocketAddress url) throws RemoteException {
		if(fromNetwork == null)
			throw new NullPointerException("'fromNetwork' must not be null");
		if(url == null)
			throw new NullPointerException("'url' must not be null");

		hashLength = fromNetwork.getHashLength();
		nodeKey = new KeyHash<InetSocketAddress>(url, hashLength).getHash();
		fingerTable = new FingerTable(this);
		for(Finger f: fingerTable)
			fixFinger(f);
	}
	
	private boolean isInRange(long key) {
		if(predecessor != null)
			try {
				return predecessor.getNodeKey() < key && key <= getNodeKey();
			}
			catch (RemoteException e){
				predecessor = null; //if we got a remote exception, then the predecessor is no longer online
			}
		
		return key == getNodeKey();
	}
	
	@Override
	public long getNodeKey() { return nodeKey; }
	
	@Override
	public int getHashLength() { return hashLength; }

	@Override
	public Serializable get(String key) throws RemoteException {
		long hash = new KeyHash<String>(key, getHashLength()).getHash();
		if(isInRange(hash))
			throw new NotImplementedException();

		return findSuccessor(hash).get(key);
	}

	@Override
	public void put(String key, Serializable value) throws RemoteException {
		long hash = new KeyHash<String>(key, getHashLength()).getHash();
		if(isInRange(hash))
			throw new NotImplementedException();
		
		findSuccessor(hash).put(key, value);
	}

	@Override
	public void delete(String key) throws RemoteException {
		long hash = new KeyHash<String>(key, getHashLength()).getHash();
		if(isInRange(hash))
			throw new NotImplementedException();

		findSuccessor(hash).delete(key);
	}

	@Override
	public RMINodeServer findSuccessor(long key) throws RemoteException {
		if(isInRange(key))
			return this;
		
		return findPredecessor(key).findSuccessor(key);
	}

	@Override
	public RMINodeServer findPredecessor(long key) throws RemoteException {
		for(Finger f : fingerTable.reverse())
			try {
				if(f.getNode() != null && getNodeKey() < f.getNode().getNodeKey() && f.getNode().getNodeKey() < key)
					return f.getNode().findPredecessor(key);
				}
			catch (RemoteException e) {
				fixFinger(f);
			}
		
		return this;
	}

	@Override
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException {
		if(predecessor == null || (predecessor.getNodeKey() < potentialPredecessor.getNodeKey() && potentialPredecessor.getNodeKey() < getNodeKey()))
			this.predecessor = potentialPredecessor;
		//TODO: update range, reassign values, etc
	}

	private void fixFinger(Finger finger) {
		try {
			finger.setNode(findSuccessor(finger.getStart()));
		} catch (RemoteException e) {
			finger.setNode(null);
		}
	}
	
	private void stabilize() {
		RMINodeServer successor = fingerTable.getSuccessor().getNode();
		if(successor != null) {
			try {
				RMINodeServer successor_predecessor = successor.findPredecessor(successor.getNodeKey());
				if(getNodeKey() < successor_predecessor.getNodeKey() && successor_predecessor.getNodeKey() < successor.getNodeKey())
					fingerTable.getSuccessor().setNode(successor_predecessor);
				fingerTable.getSuccessor().getNode().checkPredecessor(this);
			} catch (RemoteException e) {
				fixFinger(fingerTable.getSuccessor());
				return;
			}
		}
	}
}
