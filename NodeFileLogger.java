import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Provides a logging mechanism for nodes to write to a dedicated log file.
 * 
 * @author Jesse Carrigan
 *
 */
public class NodeFileLogger {
	
	/**
	 * The key to use in the path.
	 */
	private long key;
	
	/**
	 * The logfile path.
	 */
	private Path logfile;
	
	/**
	 * The date formatter to be used, as used in Log.
	 */
	private static final SimpleDateFormat DATE = 
	  new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS", Locale.US);
	  
	/**
	 * The separator between the date and the message, as used in Log.
	 */
	  private static final String SEPARATOR = ": ";
	  
	/**
	 * Creates a NodeFileLogger object.
	 * 
	 * @param nodeKey 
	 */
	public NodeFileLogger(long nodeKey) {
		key = nodeKey;
		logfile = createLogFile(key);
	}
	
	/**
	 * Log output to the log file.
	 * 
	 * @param message The output to log.
	 */
	public void logOutput(String message) {
		// Open the file with the following options
		try (OutputStream output = 
				Files.newOutputStream(logfile, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			String infoMessage = DATE + SEPARATOR + message + "\n";
			output.write(message.getBytes());
		} catch (IOException e) {
			Log.err(e.getMessage());
		}
	}
	
	/**
	 * Creates a log file using a key as part of the path.
	 * 
	 * @param key The key to use in the pathname.
	 * @return A path to the file.
	 */
	public final Path createLogFile(long key) {
		Path logfile = Paths.get(System.getProperty("user.home"), "node" + key);
		return logfile;
	}
	
}
