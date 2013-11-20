import java.io.Serializable;
import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class RMINode implements RMINodeServer {

	private long nodeKey;
	
	private int hashLength;
	
	private FingerTable fingerTable;
	
	private RMINodeServer predecessor;
	
	private final ScheduledExecutorService periodicTask = Executors.newScheduledThreadPool(1);
	
	public String internalState(){
		String s = "m:" + getHashLength() + "\t n:" + getNodeKey() + "\tp:";
		try{
			s += predecessor.getNodeKey();
		}
		catch(Throwable t) {
			s += "<none>";
		}
		return s + "\tf: " + fingerTable.toString();
	}
	
	public RMINode(int hashLength, InetSocketAddress url) throws RemoteException {
		this.hashLength = hashLength;
		this.nodeKey = new KeyHash<InetSocketAddress>(url, hashLength).getHash();
		fingerTable = new FingerTable(this);
	}

	public void join(RMINodeServer fromNetwork) throws RemoteException {
		if(fromNetwork != null) {
			RMINodeServer successor = fromNetwork.findSuccessor(getNodeKey());
			if(successor.getNodeKey() == getNodeKey())
				throw new IllegalArgumentException("A node with this key already exists in the network");
			
			fingerTable.getSuccessor().setNode(successor);
			successor.checkPredecessor(this);
			for(Finger f: fingerTable)
				f.setNode(fromNetwork.findSuccessor(f.getStart()));
		}
		else {
			//this is currently the only node in the network, so set all fingers and predecessor to self
			for(Finger f: fingerTable)
				f.setNode(this);
			predecessor = this;
		}

		periodicTask.scheduleAtFixedRate(new Runnable() {
			
			@Override
			public void run() {
				try{
					Log.out(internalState());
					stabilize();
					fixFinger(fingerTable.getRandomFinger());
				} catch(Throwable t){
					Log.err("error running periodic task: " + t.getClass());
				}
			}
		}, 5, 10, TimeUnit.SECONDS);
	}
	
	private boolean isWithinInterval(boolean leftInclusive, long left, long x, long right, boolean rightInclusive) {
		//if the bounds have been violated, it's not within the interval
		if((!leftInclusive && left == x) || (!rightInclusive && right == x))
			return false;

		//if left and right are the same, the entire key space is contained in the set.  And since we know the bounds are correct, we know that x is in the interval
		if(left == right)
			return true;

		//if left > right, then we're in a situation where the interval spans the end and beginning of the ring.  The logic is a bit different there
		if(left > right)
			return left <= x || x <= right;
		
		return left <= x && x <= right;
	}
	
	private boolean isInRange(long key) throws RemoteException {
		if(predecessor == null)
			predecessor = findPredecessor(getNodeKey());
		
		try {
			//special case: if we're the only node in the network, then we're our own predecessor.  That means we own the whole god-damned thing
			if(predecessor == null || predecessor.getNodeKey() == getNodeKey())
				return true;
			
			return isWithinInterval(false, predecessor.getNodeKey(), key, getNodeKey(), true);
		}
		catch (RemoteException e){
			predecessor = null; //if we got a remote exception, then the predecessor is no longer online
			throw e;
		}
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
		//if the key belongs to our interval, then we're the successor
		if(isInRange(key))
			return this;
		
		RMINodeServer predecessor = findPredecessor(key);
		if(predecessor.getNodeKey() == getNodeKey())
			return predecessor;

		return predecessor.findSuccessor(key);
	}
	
	@Override
	public RMINodeServer findPredecessor(long key) throws RemoteException {
		//if the key belongs to the interval of our successor, then we're the predecessor
		if(isWithinInterval(false, getNodeKey(), key, fingerTable.getSuccessor().getNode().getNodeKey(), true))
			return this;
		
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
