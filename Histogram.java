import java.util.HashMap;

import org.deuce.Atomic;


public class Histogram {

	HashMap<Long, Integer> map;
	public Histogram() {
		map = new HashMap<Long, Integer>();
	}
	
	@Atomic
	public void add(long val) {
		if(!map.containsKey(val)) {
			map.put(val, 0);
		}
		map.put(val, map.get(val)+1);
	}
}


class STMHistogram {

	HashMap<Long, Integer> map;
	public STMHistogram() {
		map = new HashMap<Long, Integer>();
	}
	
	@Atomic
	public void add(long val) {
		if(!map.containsKey(val)) {
			map.put(val, 0);
		}
		map.put(val, map.get(val)+1);
	}
}