
public class KeyHash<T> {
	private final T key;
	
	private final long hash;
	
	public KeyHash(T key, int bitness){
		this.key = key;
		this.hash = (long) (Math.abs(key.hashCode()) % Math.pow(2, bitness));
	}
	
	public T getKey() { return key; }
	
	public long getHash() { return hash; }
}
