import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

public class FingerTable implements Iterable<Finger> {

	/**
	 * Random number generator for use in the getRandomFinger method
	 */
	private static Random random = new Random();

	private List<Finger> fingers;

	public FingerTable(RMINodeServer forNode) {
		fingers = new ArrayList<Finger>();
		for (int i = 0; i < forNode.getHashLength(); i++)
			fingers.add(new Finger((long) (forNode.getNodeKey() + Math.pow(2, i))));
	}

	public Finger getSuccessor() {
		if (fingers.size() == 0)
			return null;
		return fingers.get(0);
	}

	public Finger getRandomFinger() {
		return fingers.get(random.nextInt(fingers.size()));
	}

	public Iterable<Finger> reverse() {
		return new ReverseIterator<Finger>(fingers);
	}

	@Override
	public Iterator<Finger> iterator() {
		return fingers.iterator();
	}

	/**
	 * An iterator for lists that allows the programmer to traverse the list in
	 * reverse order.
	 * 
	 * @param <T>
	 */
	class ReverseIterator<T> implements Iterable<T> {
		private ListIterator<T> listIterator;

		public ReverseIterator(List<T> wrappedList) {
			this.listIterator = wrappedList.listIterator(wrappedList.size());
		}

		public Iterator<T> iterator() {
			return new Iterator<T>() {

				public boolean hasNext() {
					return listIterator.hasPrevious();
				}

				public T next() {
					return listIterator.previous();
				}

				public void remove() {
					listIterator.remove();
				}
			};
		}
	}
}
