import java.rmi.Remote;
import java.rmi.RemoteException;


public interface Logger extends Remote {
	public void log(String log) throws RemoteException;
}
