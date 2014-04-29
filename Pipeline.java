
class PipelineIntro implements Runnable {

	PaddedPrimitiveNonVolatile<Boolean> done;
	LamportQueue<Packet> q;
	Fingerprint fprint;
	int totalPackets;
	public PipelineIntro( PaddedPrimitiveNonVolatile<Boolean> done , LamportQueue<Packet> q) {
		this.done = done;
		this.q = q;
		this.totalPackets = 0;
		this.fprint = new Fingerprint();
	}
	@Override
	public void run() {
		while(!done.value) {
			try {
				Packet pkt = q.deq();
				if (pkt.type == Packet.MessageType.DataPacket) {
					fprint.getFingerprint(pkt.body.iterations, pkt.body.seed);
				} else {
					// its a config packet
				}
				totalPackets++;
			} catch (EmptyException e) {
				
			}
			
		}
		
	}
}
