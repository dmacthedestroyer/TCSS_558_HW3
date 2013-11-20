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
	
	private void initialize(int hashLength, InetSocketAddress url) throws RemoteException{
		this.hashLength = hashLength;
		this.nodeKey = new KeyHash<InetSocketAddress>(url, hashLength).getHash();
		fingerTable = new FingerTable(this);		
	}
	
	/**
	 * Creates the first node in a Chord network.
	 * @param hashLength the logarithm of the total number of nodes in the network (to base 2)
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(int hashLength, InetSocketAddress url) throws RemoteException {
		initialize(hashLength, url);
		
		//this is currently the only node in the network, so set all fingers and predecessor to self
		for(Finger f: fingerTable)
			f.setNode(this);
		predecessor = this;
	}
	
	/**
	 * Creates a new node to join the network that fromNetwork exists in.
	 * @param fromNetwork an arbitrary node in the network
	 * @param url this node's URL, and where other nodes may reach it
	 */
	public RMINode(RMINodeServer fromNetwork, InetSocketAddress url) throws RemoteException {
		initialize(fromNetwork.getHashLength(), url);

		fromNetwork.findSuccessor(getNodeKey()).checkPredecessor(this);
		for(Finger f: fingerTable)
			f.setNode(fromNetwork.findSuccessor(f.getStart()));
	}
	
	private boolean isWithinInterval(boolean leftInclusive, long left, long x, long right, boolean rightInclusive) {
		//if left and right are the same, there is only one value in the set.  X must equal this, and the interval must be inclusive on both sides
		if(left == right)
			return left == x && leftInclusive && rightInclusive;
		//if the x equals left or right, it must be inclusive on that side
		boolean isBoundedOk = (leftInclusive || left != x) && (rightInclusive || right != x);
		
		//if left > right, then we're in a situation where the interval spans the end and beginning of the ring.  The logic is a bit different there
		if(left > right)
			return isBoundedOk && (left <= x || x <= right);
		
		return isBoundedOk && left <= x && x <= right;
	}
	
	private boolean isInRange(long key) {
		if(predecessor != null)
			try {
				return isWithinInterval(false, predecessor.getNodeKey(), key, getNodeKey(), true);
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
				if(f.getNode() != null && isWithinInterval(false, getNodeKey(), f.getNode().getNodeKey(), key, false))
					return f.getNode().findPredecessor(key);
				}
			catch (RemoteException e) {
				fixFinger(f);
			}
		
		return this;
	}

	
	@Override
	public void checkPredecessor(RMINodeServer potentialPredecessor) throws RemoteException {
		if(predecessor == null || isWithinInterval(false, predecessor.getNodeKey(), potentialPredecessor.getNodeKey(), getNodeKey(), false))
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
				if(isWithinInterval(false, getNodeKey(), successor_predecessor.getNodeKey(), successor.getNodeKey(), false))
					fingerTable.getSuccessor().setNode(successor_predecessor);
				fingerTable.getSuccessor().getNode().checkPredecessor(this);
			} catch (RemoteException e) {
				fixFinger(fingerTable.getSuccessor());
				return;
			}
		}
	}
}
