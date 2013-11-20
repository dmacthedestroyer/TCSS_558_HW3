import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.net.InetSocketAddress;

import org.junit.Before;
import org.junit.Test;


public class FingerTableTest {

	FingerTable table;
	RMINodeServer nodeserver;
	
	@Before
	public void setUp() throws Exception {
		InetSocketAddress address = new InetSocketAddress(0);
		nodeserver = new RMINode(2, address);
		table = new FingerTable(nodeserver);
	}

	@Test
	public void testGetSuccessor() {
		Finger successor = table.getSuccessor();
		assertNotEquals(successor, null);
	}

	@Test
	public void testGetRandomFinger() {
		Finger randomFinger = table.getRandomFinger();
		assertEquals(null, randomFinger.getNode());
	}

}
