import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


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
//	static final int numSources = 2;

	public static long[] run(int numSources, int numAddressesLog, int numTrainsLog, double meanTrainSize, double meanTrainsPerComm, int meanWindow, int meanCommsPerAddress, int meanWork, double configFraction, double pngFraction, double acceptingFraction) {

		@SuppressWarnings({ "unchecked" })
		StopWatch timer = new StopWatch();
		
		PacketGenerator pkt = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
		pkt.numConfigPackets = pkt.addressesMask;
		PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
		
		ArrayList<LamportQueue<Packet>> list = new ArrayList<LamportQueue<Packet>>();
		ParallelPipeline[] workerData = new ParallelPipeline[numSources];
		Thread[] workerThread = new Thread[numSources];
//		create PNG
		Set<Integer> png = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
//		create histogram
		ParallelHistogram hist = new ParallelHistogram();
//		create addrs space
		ParallelIntervalList[] r;
		r = new ParallelIntervalList[1<<numAddressesLog];
		for(int i=0; i< r.length; i++) {
			r[i] = new ParallelIntervalList(numAddressesLog);
		}
		for(int i = 0; i < numSources; i++) {
			list.add(new LamportQueue<Packet>(256/numSources));
			workerData[i] = new ParallelPipeline(png, r, hist, numAddressesLog, done, list.get(i));
			workerThread[i] = new Thread(workerData[i]);
		}
		
		Dispatcher dispatcher = new Dispatcher(dispatcherDone, pkt, list);
		Thread dispatcherThread = new Thread(dispatcher);
		preconfig(numAddressesLog,workerData[0], pkt);
		
		for(int i = 0; i < numSources; i++) {
			workerThread[i].start();
		}
		timer.startTimer();
		dispatcherThread.start();
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
			totalCount += workerData[i].totalPackets;
			
		}
		timer.stopTimer();
		return new long[] { totalCount, timer.getElapsedTime() };
	}

	public static void main(String[] args) {

	  final int exp = Integer.parseInt(args[0]);
		final int numSources = Integer.parseInt(args[1]);
		final int iters = 1;
		
		//
		int numAddressesLog;
    int numTrainsLog = 12;
    double meanTrainSize = 5.0;
    double meanTrainsPerComm = 1.0;
    int meanWindow = 3;
    int meanCommsPerAddress = 3;
    int meanWork = 3822;
    double configFraction = 0.24;
    double pngFraction = 0.04;
    double acceptingFraction = 0.96;
//		exp1
		if(exp==1) {

		  numAddressesLog = 11;
	    numTrainsLog = 12;
	    meanTrainSize = 5.0;
	    meanTrainsPerComm = 1.0;
	    meanWindow = 3;
	    meanCommsPerAddress = 3;
	    meanWork = 3822;
	    configFraction = 0.24;
	    pngFraction = 0.04;
	    acceptingFraction = 0.96;
		} else if (exp==2) {
      numAddressesLog = 12;
      numTrainsLog = 10;
      meanTrainSize = 1.0;
      meanTrainsPerComm = 3.0;
      meanWindow = 3;
      meanCommsPerAddress = 1;
      meanWork = 2644;
      configFraction = 0.11;
      pngFraction = 0.09;
      acceptingFraction = 0.92;		  
		}else if (exp==3) {
      numAddressesLog = 12;
      numTrainsLog = 10;
      meanTrainSize = 4.0;
      meanTrainsPerComm = 3.0;
      meanWindow = 6;
      meanCommsPerAddress = 2;
      meanWork = 1304;
      configFraction = 0.10;
      pngFraction = 0.03;
      acceptingFraction = 0.90;
    }else if (exp==4) {
      numAddressesLog = 14;
      numTrainsLog = 10;
      meanTrainSize = 5.0;
      meanTrainsPerComm = 5.0;
      meanWindow = 6;
      meanCommsPerAddress = 2;
      meanWork = 315;
      configFraction = 0.08;
      pngFraction = 0.05;
      acceptingFraction = 0.90;
    }else if (exp==5) {
      numAddressesLog = 15;
      numTrainsLog = 14;
      meanTrainSize = 9.0;
      meanTrainsPerComm = 16.0;
      meanWindow = 7;
      meanCommsPerAddress = 10;
      meanWork = 4007;
      configFraction = 0.02;
      pngFraction = 0.10;
      acceptingFraction = 0.84;
    }else if (exp==6) {
      numAddressesLog = 15;
      numTrainsLog = 15;
      meanTrainSize = 9.0;
      meanTrainsPerComm = 10.0;
      meanWindow = 9;
      meanCommsPerAddress = 10;
      meanWork = 7125;
      configFraction = 0.01;
      pngFraction = 0.20;
      acceptingFraction = 0.77;
    }else if (exp==7) {
      numAddressesLog = 15;
      numTrainsLog = 15;
      meanTrainSize = 10.0;
      meanTrainsPerComm = 13.0;
      meanWindow = 8;
      meanCommsPerAddress = 10;
      meanWork = 5328;
      configFraction = 0.04;
      pngFraction = 0.18;
      acceptingFraction = 0.80;
    }else {
//      exp 8
      numAddressesLog = 16;
      numTrainsLog = 14;
      meanTrainSize = 15.0;
      meanTrainsPerComm = 12.0;
      meanWindow = 9;
      meanCommsPerAddress = 5;
      meanWork = 8840;
      configFraction = 0.04;
      pngFraction = 0.19;
      acceptingFraction = 0.76;
    }
	
		
		double throughput = 0;
		for (int i = 0; i < iters; i++) {
			long[] ans = ParallelFirewall.run(numSources, numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
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


