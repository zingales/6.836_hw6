import java.util.HashMap;


public class Histogram {

	HashMap<Long, Integer> map;
	public Histogram() {
		map = new HashMap<Long, Integer>();
	}
	
	public void add(long val) {
		if(!map.containsKey(val)) {
			map.put(val, 0);
		}
		map.put(val, map.get(val)+1);
	}
}
