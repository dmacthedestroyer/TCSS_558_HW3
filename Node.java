import java.net.URL;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class Node {
	/**
	 * AKA 'm', from the paper
	 */
	private int hashBitness;
	
	private NodeData nodeData;

	private FingerTable fingerTable;

	public Node(Node fromNetwork, URL url) {
		this.nodeData =  new NodeData(fromNetwork.nodeData.getHashBitness(), url);
		this.fingerTable = new FingerTable(this.nodeData);
		join(fromNetwork);
	}

	/**
	 * TODO: I'm not sure how the best way to handle joins should be
	 * @param fromNetwork
	 */
	public void join(Node fromNetwork){
		fingerTable.getSuccessor().setNodeData(fromNetwork.findSuccessor(nodeData.getNodeKey()));
	}
	
	/**
	 * Make a network call to find the successor of the given node for the given key
	 * @param ofNode
	 * @param forKey
	 * @return
	 */
	private NodeData findSuccessor(NodeData ofNode, long forKey){
		if(ofNode.getNodeKey() == nodeData.getNodeKey())
			return fingerTable.getSuccessor().getNodeData();
		
		throw new NotImplementedException();
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

		throw new NotImplementedException();
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
