import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;


public interface RMINodeClient extends Remote {
	public Serializable get(String key) throws RemoteException;
	
	public void put(String key, Serializable value) throws RemoteException;
	
	public void delete(String key) throws RemoteException;
}
