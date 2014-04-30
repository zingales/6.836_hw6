import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;




public class IntervalList {
	private final static  boolean BEGIN = true;
	private final static boolean END = false;
	ConcurrentSkipListMap<Integer, Node> list;
	public IntervalList() {
		list = new ConcurrentSkipListMap<Integer, Node>();
	}

	public boolean valid(int source) {
		Entry<Integer, Node> e = list.floorEntry(source);
		if (e != null) {
			Node n = e.getValue();
			if (source < n.end) {
				return true;
			}
		}
		return false;
	}

	public void add(int addressBegin, int addressEnd) {
		Node n = new Node(addressBegin, addressEnd);
		// check if this node is encapsulated
		Entry<Integer, Node> e = list.floorEntry(n.start);
		if (e != null) {
			Node n2 = e.getValue();
			if (n2.start <= n.start && n2.end >= n.end){
				return;
			}
		}
		
		mergeLeft(n);
		mergeRight(n);
		list.put(addressBegin, n);
	}


	private void mergeLeft(Node n) {
		Entry<Integer, Node> e = list.floorEntry(n.start);
		if (e != null) {
			Node n2 = e.getValue();
			if(n2.start >= n.start){
				list.remove(n2.start);
				n.start = Math.min(n2.start, n.start);
				mergeLeft(n);
			}
		}
		
	}
	
	private void mergeRight(Node n) {
		Entry<Integer, Node> e = list.ceilingEntry(n.start);
		if (e!=null) {
			Node n2 = e.getValue();
			if(n2.start <= n.end) {
				list.remove(n2.start);
				n.end = Math.max(n2.end, n.end);
				mergeRight(n);
			}
		}
	}

	public void remove(int addressBegin, int addressEnd) {
		removeContained(addressBegin, addressEnd);
		
	}	
	
	private void removeContained(int start, int end) {
		Entry<Integer, Node> e = list.ceilingEntry(start);
		if( e!= null) {
			Node n = e.getValue();
		}
	}

	private class Node {
		int start, end;
		public Node(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}
}
