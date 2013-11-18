import java.net.URL;

public class NodeData {
	private final int hashBitness;
	
	private final URL url;
	
	private final long hash;
	
	public NodeData(final int hashBitness, final URL url){
		this.hashBitness = hashBitness;
		this.url = url;
		this.hash = new KeyHash<URL>(url, hashBitness).getHash();
	}
	
	public int getHashBitness() { 
		return hashBitness;
	}

	public long getNodeKey(){
		return hash;
	}
	
	public URL getURL(){
		return url;
	}
}
