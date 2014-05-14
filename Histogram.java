import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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


class ParallelHistogram {

	Map<Long, AtomicInteger> map;
	public ParallelHistogram() {
		map = new ConcurrentHashMap<Long, AtomicInteger>();
	}
	
	public void add(long val) {
		if(!map.containsKey(val)) {
			map.put(val, new AtomicInteger(1));
			return;
		}
		map.get(val).getAndIncrement();
	}
}