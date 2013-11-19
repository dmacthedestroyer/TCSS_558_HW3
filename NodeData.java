import java.net.URL;

public class NodeData {
	private final int hashLength;
	
	private final URL url;
	
	private final long hash;
	
	public NodeData(final int hashLength, final URL url){
		this.hashLength = hashLength;
		this.url = url;
		this.hash = new KeyHash<URL>(url, hashLength).getHash();
	}
	
	public int getHashLength() { 
		return hashLength;
	}

	public long getNodeKey(){
		return hash;
	}
	
	public URL getURL(){
		return url;
	}
}
