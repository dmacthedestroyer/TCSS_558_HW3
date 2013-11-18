public class Finger {

	private final long startKey;
	
	private NodeData nodeData;
	
	public Finger(final long startKey){
		this.startKey = startKey;
	}
	
	public long getStart(){
		return startKey;
	}

	public NodeData getNodeData() {
		return nodeData;
	}

	public void setNodeData(final NodeData nodeData){
		this.nodeData = nodeData;
	}
}
