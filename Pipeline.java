import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


class SerialPipeline implements Runnable {

  PaddedPrimitiveNonVolatile<Boolean> done;
  LamportQueue<Packet>                q;
  Fingerprint                         fprint;
  int                                 totalPackets, dataPackets;
  HashSet<Integer>                    png;
  HashMap<Integer, IntervalList>      r;
  Histogram                           hist;

  public SerialPipeline(PaddedPrimitiveNonVolatile<Boolean> done,
      LamportQueue<Packet> q) {
    this.done = done;
    this.q = q;
    this.totalPackets = 0;
    this.dataPackets = 0;
    this.fprint = new Fingerprint();
    png = new HashSet<Integer>();
    r = new HashMap<Integer, IntervalList>();
    hist = new Histogram();
  }

  public void run() {
    Packet pkt;
    while (!done.value) {
      pkt = q.deq();
      if (pkt == null) {
        continue;
      }
      process(pkt);
    }
  }

  public void process(Packet pkt) {
    totalPackets++;
    if (pkt.type == Packet.MessageType.DataPacket) {
      // ignore all packets from
      if (png.contains(pkt.header.source)) {
        return;
      }
      if (!r.containsKey(pkt.header.dest)) {
        return;
      }
      if (!r.get(pkt.header.dest).valid(pkt.header.source)) {
        return;
      }
      // add to histogram
      long fingerprint = fprint.getFingerprint(pkt.body.iterations,
          pkt.body.seed);
      hist.add(fingerprint);
      dataPackets++;
    } else if (pkt.type == Packet.MessageType.ConfigPacket) {
      updatePNG(pkt.config.personaNonGrata, pkt.config.address);

      if (!r.containsKey(pkt.config.address)) {
        r.put(pkt.config.address, new IntervalList());
      }
      if (pkt.config.acceptingRange) {
        // valid ranges
        r.get(pkt.config.address).add(pkt.config.addressBegin,
            pkt.config.addressEnd);
      } else {
        // invalid ranges
        r.get(pkt.config.address).remove(pkt.config.addressBegin,
            pkt.config.addressEnd);

      }
    }
  }

  private void updatePNG(boolean add, int address) {
    if (add) {
      png.add(address);
    } else {
      png.remove(address);
    }
  }
}

class STMPipeline implements Runnable {

  PaddedPrimitiveNonVolatile<Boolean> done;
  LamportQueue<Packet>                q;
  Fingerprint                         fprint;
  int                                 totalPackets, dataPackets;
  HashSet<Integer>                    png;
  HashMap<Integer, STMIntervalList>   r;
  STMHistogram                        hist;

  public STMPipeline(PaddedPrimitiveNonVolatile<Boolean> done,
      LamportQueue<Packet> q) {
    this.done = done;
    this.q = q;
    this.totalPackets = 0;
    this.dataPackets = 0;
    this.fprint = new Fingerprint();
    png = new HashSet<Integer>();
    r = new HashMap<Integer, STMIntervalList>();
    hist = new STMHistogram();
  }

  public void run() {
    // Packet pkt;
    // while(!done.value) {
    // try {
    // pkt = q.deq();
    // process(pkt);
    // }
    // catch (EmptyException e) {
    //
    // }
    //
    // }
    //
  }

  public void process(Packet pkt) {
    totalPackets++;
    if (pkt.type == Packet.MessageType.DataPacket) {
      // ignore all packets from
      if (png.contains(pkt.header.source)) {
        return;
      }
      if (!r.containsKey(pkt.header.dest)) {
        return;
      }
      if (!r.get(pkt.header.dest).valid(pkt.header.source)) {
        return;
      }
      // add to histogram
      long fingerprint = fprint.getFingerprint(pkt.body.iterations,
          pkt.body.seed);
      hist.add(fingerprint);
      dataPackets++;
    } else if (pkt.type == Packet.MessageType.ConfigPacket) {
      updatePNG(pkt.config.personaNonGrata, pkt.config.address);

      if (!r.containsKey(pkt.config.address)) {
        r.put(pkt.config.address, new STMIntervalList());
      }
      if (pkt.config.acceptingRange) {
        // valid ranges
        r.get(pkt.config.address).add(pkt.config.addressBegin,
            pkt.config.addressEnd);
      } else {
        // invalid ranges
        r.get(pkt.config.address).remove(pkt.config.addressBegin,
            pkt.config.addressEnd);

      }
    }
  }

  // @Atomic
  private void updatePNG(boolean add, int address) {
    if (add) {
      png.add(address);
    } else {
      png.remove(address);
    }
  }
}

class ParallelPipeline implements Runnable {

  PaddedPrimitiveNonVolatile<Boolean> done;
  LamportQueue<Packet>                q, fq;
  // Fingerprint fprint;
  FingerPrinter                       fp;
  int                                 totalPackets, dataPackets;
  Set<Integer>                        png;
  // Map<Integer, ParallelIntervalList> r;
  ParallelIntervalList[]              r;
  ParallelHistogram                   hist;
  Thread t;

  public ParallelPipeline(Set<Integer> png, ParallelIntervalList[] r,
      ParallelHistogram hist, int numAddressesLog,
      PaddedPrimitiveNonVolatile<Boolean> done, LamportQueue<Packet> q) {
    this.done = done;
    this.q = q;
    this.totalPackets = 0;
    this.dataPackets = 0;
    this.fq = new LamportQueue<Packet>(5);
    t = new Thread(new FingerPrinter(new Fingerprint(), fq, hist, done));
    t.start();
    this.r = r;
    this.hist = hist;
    this.png = png;
  }

  public void run() {
    Packet pkt;
    while (!done.value) {
      pkt = q.deq();
      if (pkt == null) {
        continue;
      }
      process(pkt);
    }
    try {
      t.join();
    } catch (InterruptedException e) {;}

  }

  public void process(Packet pkt) {
    totalPackets++;
    if (pkt.type == Packet.MessageType.DataPacket) {
      // ignore all packets from
      if (png.contains(pkt.header.source)) {
        return;
      }
      if (!r[pkt.header.dest].valid(pkt.header.source)) {
        return;
      }
      // add to histogram
      while (!fq.enq(pkt))
        ;
      // long fingerprint = fprint.getFingerprint(pkt.body.iterations,
      // pkt.body.seed);
      // hist.add(fingerprint);
      dataPackets++;
    } else if (pkt.type == Packet.MessageType.ConfigPacket) {
      updatePNG(pkt.config.personaNonGrata, pkt.config.address);

      // if(!r.containsKey(pkt.config.address)) {
      // r.put(pkt.config.address, new ParallelIntervalList(addrLog));
      // }
      if (pkt.config.acceptingRange) {
        // valid ranges
        r[pkt.config.address].add(pkt.config.addressBegin,
            pkt.config.addressEnd);
      } else {
        // invalid ranges
        r[pkt.config.address].remove(pkt.config.addressBegin,
            pkt.config.addressEnd);

      }
    }
  }

  private void updatePNG(boolean add, int address) {
    if (add) {
      png.add(address);
    } else {
      png.remove(address);
    }
  }
}

class FingerPrinter implements Runnable {

	ParallelHistogram hist;
	LamportQueue<Packet> q;
	PaddedPrimitiveNonVolatile<Boolean> done;
	Fingerprint fprint;
	public FingerPrinter(Fingerprint fprint, LamportQueue<Packet> q, ParallelHistogram hist, PaddedPrimitiveNonVolatile<Boolean> done) {
		this.q = q;
		this.hist = hist;
		this.done = done;
		this.fprint = fprint;
	}

	public void run() {
//		System.out.println("Starting Fingerprinter");
		Packet pkt = null;
		while(!done.value) {
			pkt = q.deq();
			if (pkt == null) continue;
			long fingerprint = fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
			hist.add(fingerprint);
		}
		while(true) {
			pkt = q.deq();
			if (pkt == null) break;
			long fingerprint = fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
			hist.add(fingerprint);
		}


	}
	
}
