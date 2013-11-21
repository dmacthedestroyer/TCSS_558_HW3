import java.rmi.RemoteException;


public class RemoteLogger implements Logger {

	public RemoteLogger() {
		super();
	}
	
	@Override
	public void log(String log) throws RemoteException {
		Log.out(log);
	}

}
