import java.util.HashMap;
import java.util.HashSet;


class SerialPipeline implements Runnable {

	PaddedPrimitiveNonVolatile<Boolean> done;
	LamportQueue<Packet> q;
	Fingerprint fprint;
	int totalPackets;
	HashSet<Integer> png;
	HashMap<Integer, IntervalList> r;
	Histogram hist;
	public SerialPipeline( PaddedPrimitiveNonVolatile<Boolean> done , LamportQueue<Packet> q) {
		this.done = done;
		this.q = q;
		this.totalPackets = 0;
		this.fprint = new Fingerprint();
		png = new HashSet<Integer>();
		r = new HashMap<Integer, IntervalList>();
		hist = new Histogram();
	}
	@Override
	public void run() {
		Packet pkt;
		while(!done.value) {
			try {
				pkt = q.deq();
				process(pkt);
			}
			catch (EmptyException e) {
				
			}
			
		}
		
	}
	
	private void process(Packet pkt) {
		if (pkt.type == Packet.MessageType.DataPacket) {
			// ignore all packets from 
			if (png.contains(pkt.header.source)) {
				return;
			}
			if(!r.containsKey(pkt.header.dest)) {
				r.put(pkt.header.dest, new IntervalList());
			}
			if (!r.get(pkt.header.dest).valid(pkt.header.source)) {
				return;
			}
			// add to histogram
			long fingerprint = fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
			hist.add(fingerprint);
			totalPackets++;
		} else if (pkt.type == Packet.MessageType.ConfigPacket) {
			if( pkt.config.personaNonGrata ) {
				png.add(pkt.config.address);
			} else {
				png.remove(pkt.config.address);
			}
			if( pkt.config.acceptingRange) {
				// valid ranges
				r.get(pkt.config.address).add(pkt.config.addressBegin, pkt.config.addressEnd);
			} else {
				// invalid ranges
				r.get(pkt.config.address).remove(pkt.config.addressBegin, pkt.config.addressEnd);

			}
		}
	}
}
