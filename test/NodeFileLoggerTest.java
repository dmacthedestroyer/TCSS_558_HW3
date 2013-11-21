import org.junit.Before;
import org.junit.Test;

public class NodeFileLoggerTest {

	NodeFileLogger logger;
	
	@Before
	public void setUp() throws Exception {
		logger = new NodeFileLogger(0);
	}
	
	@Test
	public void testLogOutput() {
		logger.logOutput("Test log");
	}

}
