import java.util.ArrayList;


class SerialFirewall {

	
	static final int numMilliseconds = 2000;

	public static long[] run(int numSources, int numAddressesLog, int numTrainsLog, double meanTrainSize, double meanTrainsPerComm, int meanWindow, int meanCommsPerAddress, int meanWork, double configFraction, double pngFraction, double acceptingFraction) {

		@SuppressWarnings({ "unchecked" })
		StopWatch timer = new StopWatch();
		
		PacketGenerator pkt = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
		PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
		
		LamportQueue<Packet> q = new LamportQueue<Packet>(256/numSources);
		ArrayList<LamportQueue<Packet>> list = new ArrayList<LamportQueue<Packet>>();
		list.add(q);
		
		Dispatcher dispatcher = new Dispatcher(dispatcherDone, pkt, list);
		Thread dispatcherThread = new Thread(dispatcher);
		dispatcherThread.start();
		double preConfig = Math.pow(1<< numAddressesLog, 1.5);
		
		SerialPipeline workerData = new SerialPipeline(done, q);
		for (int i =0; i < preConfig; i++) {
			workerData.process(pkt.getConfigPacket());
		}
		Thread workerThread = new Thread(workerData);
		
		workerThread.start();
		timer.startTimer();
		try {
			Thread.sleep(numMilliseconds);
		} catch (InterruptedException ignore) {
			;
		}
		dispatcherDone.value = true;
		memFence.value = true; // memFence is a 'volatile' forcing a memory
		done.value = true;
		memFence.value = false; // memFence is a 'volatile' forcing a memory
		
		try { 
			dispatcherThread.join();
		} catch (InterruptedException ignore) {
			;
		}
		try { // which means that done.value is visible to the workers
			workerThread.join();
		} catch (InterruptedException ignore) {
			;
		}
		timer.stopTimer();
		final long totalCount = workerData.dataPackets;
		return new long[] { totalCount, timer.getElapsedTime() };
	}

	public static void main(String[] args) {

		final int numSources = Integer.parseInt(args[0]);
		final int numAddressesLog = 11;
		final int numTrainsLog = 12;
		final double meanTrainSize = 5.0;
		final double meanTrainsPerComm = 1.0;
		final int meanWindow = 3;
		final int meanCommsPerAddress = 3;
		final int meanWork = 3822;
		final double configFraction = 0.24;
		final double pngFraction = 0.04;
		final double acceptingFraction = 0.96;
		final int iters = 1;
		
		double throughput = 0;
		for (int i = 0; i < iters; i++) {
			long[] ans = SerialFirewall.run(numSources, numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
			throughput += (double) ans[0] / (double) ans[1];
		}

		System.out.println("\tthroughput: " + throughput / iters);

	}
}

class STMFirewall {

	
	static final int numMilliseconds = 2000;
	static final int numSources = 2;

	public static long[] run(int numAddressesLog, int numTrainsLog, double meanTrainSize, double meanTrainsPerComm, int meanWindow, int meanCommsPerAddress, int meanWork, double configFraction, double pngFraction, double acceptingFraction) {

		@SuppressWarnings({ "unchecked" })
		StopWatch timer = new StopWatch();
		
		PacketGenerator pkt = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
		PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
		
		ArrayList<LamportQueue<Packet>> list = new ArrayList<LamportQueue<Packet>>();
		STMPipeline[] workerData = new STMPipeline[numSources];
		Thread[] workerThread = new Thread[numSources];
		for(int i = 0; i < numSources; i++) {
			list.add(new LamportQueue<Packet>(256/numSources));
			workerData[i] = new STMPipeline(done, list.get(i));
			workerThread[i] = new Thread(workerData[i]);
		}
		
		Dispatcher dispatcher = new Dispatcher(dispatcherDone, pkt, list);
		Thread dispatcherThread = new Thread(dispatcher);
		dispatcherThread.start();
		preconfig(numAddressesLog, workerData[0], pkt);
		
		for(int i = 0; i < numSources; i++) {
			workerThread[i].start();
		}
		timer.startTimer();
		try {
			Thread.sleep(numMilliseconds);
		} catch (InterruptedException ignore) {
			;
		}
		dispatcherDone.value = true;
		memFence.value = true; // memFence is a 'volatile' forcing a memory
		done.value = true;
		memFence.value = false; // memFence is a 'volatile' forcing a memory
		
		try { 
			dispatcherThread.join();
		} catch (InterruptedException ignore) {
			;
		}
		long totalCount =0;
		for(int i =0; i < numSources; i++) {
			try { // which means that done.value is visible to the workers
				workerThread[i].join();
			} catch (InterruptedException ignore) {;}
			totalCount += workerData[i].dataPackets;
			
		}
		timer.stopTimer();
		return new long[] { totalCount, timer.getElapsedTime() };
	}

	private static void preconfig(int numAddressesLog, STMPipeline stmPipeline, PacketGenerator pkt) {
		double preConfig = Math.pow(1<< numAddressesLog, 1.5);
		
		for (int i =0; i < preConfig; i++) {
			stmPipeline.process(pkt.getConfigPacket());
		}
		
	}

	public static void main(String[] args) {

		final int numAddressesLog = 11;
		final int numTrainsLog = 12;
		final double meanTrainSize = 5.0;
		final double meanTrainsPerComm = 1.0;
		final int meanWindow = 3;
		final int meanCommsPerAddress = 3;
		final int meanWork = 3822;
		final double configFraction = 0.24;
		final double pngFraction = 0.04;
		final double acceptingFraction = 0.96;
		final int iters = 1;
		
		double throughput = 0;
		for (int i = 0; i < iters; i++) {
			long[] ans = STMFirewall.run(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
			throughput += (double) ans[0] / (double) ans[1];
		}

		System.out.println("\tthroughput: " + throughput / iters);

	}
}

class ParallelFirewall {

	
	static final int numMilliseconds = 2000;
	static final int numSources = 2;

	public static long[] run(int numAddressesLog, int numTrainsLog, double meanTrainSize, double meanTrainsPerComm, int meanWindow, int meanCommsPerAddress, int meanWork, double configFraction, double pngFraction, double acceptingFraction) {

		@SuppressWarnings({ "unchecked" })
		StopWatch timer = new StopWatch();
		
		PacketGenerator pkt = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
		pkt.numConfigPackets = pkt.addressMask
		PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
		
		ArrayList<LamportQueue<Packet>> list = new ArrayList<LamportQueue<Packet>>();
		ParallelPipeline[] workerData = new ParallelPipeline[numSources];
		Thread[] workerThread = new Thread[numSources];
		for(int i = 0; i < numSources; i++) {
			list.add(new LamportQueue<Packet>(256/numSources));
			workerData[i] = new ParallelPipeline(numAddressesLog, done, list.get(i));
			workerThread[i] = new Thread(workerData[i]);
		}
		
		Dispatcher dispatcher = new Dispatcher(dispatcherDone, pkt, list);
		Thread dispatcherThread = new Thread(dispatcher);
		dispatcherThread.start();
		preconfig(numAddressesLog,workerData[0], pkt);
//		double preConfig = Math.pow(1<< numAddressesLog, 1.5);
//		
//		for (int i =0; i < preConfig; i++) {
//			workerData[0].process(pkt.getConfigPacket());
//		}
		
		for(int i = 0; i < numSources; i++) {
			workerThread[i].start();
		}
		timer.startTimer();
		try {
			Thread.sleep(numMilliseconds);
		} catch (InterruptedException ignore) {
			;
		}
		dispatcherDone.value = true;
		memFence.value = true; // memFence is a 'volatile' forcing a memory
		done.value = true;
		memFence.value = false; // memFence is a 'volatile' forcing a memory
		
		try { 
			dispatcherThread.join();
		} catch (InterruptedException ignore) {
			;
		}
		long totalCount =0;
		for(int i =0; i < numSources; i++) {
			try { // which means that done.value is visible to the workers
				workerThread[i].join();
			} catch (InterruptedException ignore) {;}
			totalCount += workerData[i].dataPackets;
			
		}
		timer.stopTimer();
		return new long[] { totalCount, timer.getElapsedTime() };
	}

	public static void main(String[] args) {

//		exp1
		final int numAddressesLog = 11;
		final int numTrainsLog = 12;
		final double meanTrainSize = 5.0;
		final double meanTrainsPerComm = 1.0;
		final int meanWindow = 3;
		final int meanCommsPerAddress = 3;
		final int meanWork = 3822;
		final double configFraction = 0.24;
		final double pngFraction = 0.04;
		final double acceptingFraction = 0.96;
		final int iters = 1;
		
//		exp5
//		final int numAddressesLog = 15;
//		final int numTrainsLog = 14;
//		final double meanTrainSize = 9.0;
//		final double meanTrainsPerComm = 16.0;
//		final int meanWindow = 7;
//		final int meanCommsPerAddress = 10;
//		final int meanWork = 4007;
//		final double configFraction = 0.02;
//		final double pngFraction = 0.10;
//		final double acceptingFraction = 0.84;
//		final int iters = 1;
		
		double throughput = 0;
		for (int i = 0; i < iters; i++) {
			long[] ans = ParallelFirewall.run(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
			throughput += (double) ans[0] / (double) ans[1];
		}

		System.out.println("\tthroughput: " + throughput / iters);

	}
	
//	mostly useful in profiling. 
	private static void preconfig(int numAddressesLog, ParallelPipeline stmPipeline, PacketGenerator pkt) {
		double preConfig = Math.pow(1<< numAddressesLog, 1.5);
		
		for (int i =0; i < preConfig; i++) {
			stmPipeline.process(pkt.getConfigPacket());
		}
		
	}
}


