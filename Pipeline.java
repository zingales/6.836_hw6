import java.util.HashMap;
import java.util.HashSet;

import org.deuce.Atomic;


class SerialPipeline implements Runnable {

	PaddedPrimitiveNonVolatile<Boolean> done;
	LamportQueue<Packet> q;
	Fingerprint fprint;
	int totalPackets, dataPackets;
	HashSet<Integer> png;
	HashMap<Integer, IntervalList> r;
	Histogram hist;
	public SerialPipeline( PaddedPrimitiveNonVolatile<Boolean> done , LamportQueue<Packet> q) {
		this.done = done;
		this.q = q;
		this.totalPackets = 0;
		this.dataPackets =0;
		this.fprint = new Fingerprint();
		png = new HashSet<Integer>();
		r = new HashMap<Integer, IntervalList>();
		hist = new Histogram();
	}

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
	
	public void process(Packet pkt) {
		totalPackets++;
		if (pkt.type == Packet.MessageType.DataPacket) {
			// ignore all packets from 
			if (png.contains(pkt.header.source)) {
				return;
			}
			if(!r.containsKey(pkt.header.dest)) {
				return;
			}
			if (!r.get(pkt.header.dest).valid(pkt.header.source)) {
				return;
			}
			// add to histogram
			long fingerprint = fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
			hist.add(fingerprint);
			dataPackets++;
		} else if (pkt.type == Packet.MessageType.ConfigPacket) {
			updatePNG(pkt.config.personaNonGrata, pkt.config.address);
		
			if(!r.containsKey(pkt.config.address)) {
				r.put(pkt.config.address, new IntervalList());
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
	
	private void updatePNG(boolean add, int address) {
		if( add ) {
			png.add(address);
		} else {
			png.remove(address);
		}
	}
}

class STMPipeline implements Runnable {

	PaddedPrimitiveNonVolatile<Boolean> done;
	LamportQueue<Packet> q;
	Fingerprint fprint;
	int totalPackets, dataPackets;
	HashSet<Integer> png;
	HashMap<Integer, STMIntervalList> r;
	STMHistogram hist;
	public STMPipeline( PaddedPrimitiveNonVolatile<Boolean> done , LamportQueue<Packet> q) {
		this.done = done;
		this.q = q;
		this.totalPackets = 0;
		this.dataPackets =0;
		this.fprint = new Fingerprint();
		png = new HashSet<Integer>();
		r = new HashMap<Integer, STMIntervalList>();
		hist = new STMHistogram();
	}

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
	
	public void process(Packet pkt) {
		totalPackets++;
		if (pkt.type == Packet.MessageType.DataPacket) {
			// ignore all packets from 
			if (png.contains(pkt.header.source)) {
				return;
			}
			if(!r.containsKey(pkt.header.dest)) {
				return;
			}
			if (!r.get(pkt.header.dest).valid(pkt.header.source)) {
				return;
			}
			// add to histogram
			long fingerprint = fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
			hist.add(fingerprint);
			dataPackets++;
		} else if (pkt.type == Packet.MessageType.ConfigPacket) {
			updatePNG(pkt.config.personaNonGrata, pkt.config.address);
		
			if(!r.containsKey(pkt.config.address)) {
				r.put(pkt.config.address, new STMIntervalList());
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
	
	@Atomic
	private void updatePNG(boolean add, int address) {
		if( add ) {
			png.add(address);
		} else {
			png.remove(address);
		}
	}
}
