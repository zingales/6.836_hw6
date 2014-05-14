import java.util.ArrayList;

class Dispatcher implements Runnable {

	PaddedPrimitiveNonVolatile<Boolean> done;
	final PacketGenerator pkt;
	final Fingerprint residue = new Fingerprint();
	long fingerprint = 0;
	long totalPackets = 0;
	final int numSources = 1;
	ArrayList<LamportQueue<Packet>> qArray;

	public Dispatcher(PaddedPrimitiveNonVolatile<Boolean> done, PacketGenerator pkt, ArrayList<LamportQueue<Packet>> qArray) {

		this.done = done;
		this.pkt = pkt;
		this.qArray = qArray;
	}

	public void run() {
		Packet tmp;
		// guaranteed to be zero
		tmp = pkt.getPacket();
		int i = 0;
		while (!done.value) {
			try {
				qArray.get(i).enq(tmp);
				tmp = pkt.getPacket();
			} catch (FullException e) {
				;
			}
			i = (i+1) % qArray.size();
		}
	}
}

//class Dispatcher implements Runnable {
//
//	PaddedPrimitiveNonVolatile<Boolean> done;
//	final PacketGenerator pkt;
//	final Fingerprint residue = new Fingerprint();
//	long fingerprint = 0;
//	long totalPackets = 0;
//	final int numSources;
//	ArrayList<LamportQueue<Packet>> qArray;
//
//	public Dispatcher(PaddedPrimitiveNonVolatile<Boolean> done,
//			PacketGenerator pkt, boolean uniformBool, int numSources,
//			ArrayList<LamportQueue<Packet>> qArray) {
//
//		this.done = done;
//		this.pkt = pkt;
//		this.numSources = numSources;
//		this.qArray = qArray;
//	}
//
//	public void run() {
//		Packet tmp;
//		// guaranteed to be zero
//		// int[] dispatched = new int[numSources];
//		// TODO Do all the Queues need to be fair?
//		Boolean enq;
//		while (!done.value) {
//			for (int i = 0; i < numSources; i++) {
//				enq = true;
//				while (enq) {
//					tmp = pkt.getPacket();
//					try {
//
//						// enqueue tmp in the ith Lamport queue
//						qArray.get(i).enq(tmp);
//						// dispatched[i]++;
//
//					} catch (FullException e) {
//						enq = false;
//					}
//
//				}
//			}
//		}
//	}
//}