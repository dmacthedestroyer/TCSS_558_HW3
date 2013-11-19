public class Finger {

	private final long startKey;
	
	private RMINodeServer node;
	
	public Finger(final long startKey){
		this.startKey = startKey;
	}
	
	public long getStart(){
		return startKey;
	}

	public RMINodeServer getNode() {
		return node;
	}

	public void setNode(final RMINodeServer node){
		this.node = node;
	}
}
