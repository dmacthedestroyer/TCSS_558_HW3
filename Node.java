import java.net.URL;

public abstract class Node {
	private NodeData nodeData;

	private FingerTable fingerTable;

	/**
	 * Creates the first node in a Chord network.
	 * @param hashLength the logarithm of the total number of nodes in the network (to base 2)
	 * @param url this node's URL, and where other nodes may reach it
	 */
	protected Node(int hashLength, URL url) {
		if(url == null)
			throw new NullPointerException("'url' must not be null");
		
		this.nodeData = new NodeData(hashLength, url);
		
		if(lookupNode(this.nodeData) != null)
			throw new IllegalArgumentException("There is already a node registered in this network for the url provided at " + url);

		this.fingerTable = new FingerTable(this.nodeData);
		for(Finger f : fingerTable)
			f.setNodeData(this.nodeData);
	}
	
	/**
	 * Creates a new node to join the network that fromNetwork exists in.
	 * @param fromNetwork an arbitrary node in the network
	 * @param url this node's URL, and where other nodes may reach it
	 */
	protected Node(Node fromNetwork, URL url) {
		if(fromNetwork == null)
			throw new NullPointerException("'fromNetwork' must not be null");
		if(url == null)
			throw new NullPointerException("'url' must not be null");
		
		this.nodeData =  new NodeData(fromNetwork.nodeData.getHashLength(), url);
		if(lookupNode(this.nodeData) != null)
			throw new IllegalArgumentException("There is already a node registered in this network for the url provided at " + url);

		this.fingerTable = new FingerTable(this.nodeData);
		for(Finger f: fingerTable)
			f.setNodeData(fromNetwork.findSuccessor(f.getStart()));
		fingerTable.getSuccessor().setNodeData(fromNetwork.findSuccessor(nodeData.getNodeKey()));
	}

	/**
	 * Returns a Node object from the network with the specified identity information, or null if no Node exists
	 * @param node
	 * @return
	 */
	public abstract Node lookupNode(NodeData node);
		
	/**
	 * Make a network call to find the successor of the given node for the given key
	 * @param ofNode
	 * @param forKey
	 * @return
	 */
	private NodeData findSuccessor(NodeData ofNode, long forKey){
		if(ofNode.getNodeKey() == nodeData.getNodeKey())
			return fingerTable.getSuccessor().getNodeData();
		
		return lookupNode(ofNode).findSuccessor(forKey);
	}
	
	/**
	 * Make a network call to find the closes preceding finger of the given node for the given key
	 * @param ofNode
	 * @param forKey
	 * @return
	 */
	public NodeData findClosestPrecedingFinger(NodeData ofNode, long forKey){
		if(ofNode.getNodeKey() == nodeData.getNodeKey())
			return findClosestPrecedingFinger(forKey);

		return lookupNode(ofNode).findClosestPrecedingFinger(forKey);
	}

	public NodeData findSuccessor(long key){
		return findSuccessor(findPredecessor(key), key);
	}
	
	public NodeData findPredecessor(long key) {
		NodeData currentPredecessor = nodeData;
		while(!(currentPredecessor.getNodeKey() < key && key <= findSuccessor(currentPredecessor, key).getNodeKey()))
			currentPredecessor = findClosestPrecedingFinger(currentPredecessor, key);
		return currentPredecessor;
	}
	
	private NodeData findClosestPrecedingFinger(long key){
		for(Finger f : fingerTable.reverse()){
			if(nodeData.getNodeKey() <  f.getNodeData().getNodeKey() && f.getNodeData().getNodeKey() < key)
				return f.getNodeData();
		}
		return nodeData;
	}
	
	private void stabilize() {
		 long x = findPredecessor(fingerTable.getSuccessor().getNodeData().getNodeKey()).getNodeKey();
		 fingerTable.getSuccessor().setNodeData(findSuccessor(x));
	}
	
	private void fixFinger(){
		Finger randomFinger = fingerTable.getRandomFinger();
		randomFinger.setNodeData(findSuccessor(randomFinger.getStart()));
	}
}
