import java.util.ArrayList;


class SerialFirewall {

	
	static final int numMilliseconds = 2000;
	static final int numSources = 1;

	public static long[] run(int numAddressesLog, int numTrainsLog, double meanTrainSize, double meanTrainsPerComm, int meanWindow, int meanCommsPerAddress, int meanWork, double configFraction, double pngFraction, double acceptingFraction) {

		@SuppressWarnings({ "unchecked" })
		StopWatch timer = new StopWatch();
		
		PacketGenerator pkt = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
		PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitiveNonVolatile<Boolean> dispatcherDone = new PaddedPrimitiveNonVolatile<Boolean>(false);
		PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);
		
		LamportQueue<Packet> q = new LamportQueue<Packet>(256/numSources);
		ArrayList<LamportQueue<Packet>> list = new ArrayList<LamportQueue<Packet>>();
		list.add(q);
		
		SerialDispatcher dispatcher = new SerialDispatcher(dispatcherDone, pkt, list);
		Thread dispatcherThread = new Thread(dispatcher);
		dispatcherThread.start();
		
		SerialPipeline workerData = new SerialPipeline(done, q);
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
		final long totalCount = workerData.totalPackets;
		return new long[] { totalCount, timer.getElapsedTime() };
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
			long[] ans = SerialFirewall.run(numAddressesLog, numTrainsLog, meanTrainSize, meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFraction, pngFraction, acceptingFraction);
			throughput += (double) ans[0] / (double) ans[1];
		}

		System.out.println("\tthroughput: " + throughput / iters);

	}
}

