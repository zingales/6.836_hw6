import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;




public class IntervalList {
	private final static  boolean BEGIN = true;
	private final static boolean END = false;
	ConcurrentSkipListMap<Integer, Boolean> list;
	public IntervalList() {
		list = new ConcurrentSkipListMap<Integer, Boolean>();
	}

	public boolean valid(int source) {
		Entry<Integer, Boolean> floor, ciel;
		floor = list.floorEntry(source);
		ciel = list.ceilingEntry(source);
		if(list.get(source) || (floor != null &&  floor.getValue() == BEGIN)) {
			if (ciel == null || ciel.getValue() != END ) {
				System.out.println("Things are broken, our skip list is not what we expected");
			}
			return true;
		}
		return false;
	}

	public void add(int addressBegin, int addressEnd) {
		// to make the list inclusive
		addressEnd--;
		Entry<Integer, Boolean> mb_left, mb_right;
		mb_left = list.floorEntry(addressBegin);
		if(mb_left !=null && mb_left.getValue() == BEGIN) {
			
		} else if(mb_left.getKey() == addressBegin) {
			list.remove(addressBegin);
		} else {
			list.put(addressBegin, BEGIN);
			mb_right = list.ceilingEntry(addressBegin);
			if( mb_right.getKey() < addressEnd) {
				list.remove(mb_right.getKey());
			}
		}
		//TODO finish
		
	}

	public void remove(int addressBegin, int addressEnd) {
		// to make the list inclusive. 
		addressEnd--;
		
	}

	private class Node {
		int value;
		boolean isEnd;
		public Node(int value, boolean isEnd) {
			this.value = value;
			this.isEnd = isEnd;
		}
	}
	
}
