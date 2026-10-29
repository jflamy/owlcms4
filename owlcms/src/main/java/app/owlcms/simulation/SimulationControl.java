package app.owlcms.simulation;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/** Starts and stops the competition simulation on its own thread. */
public final class SimulationControl {

	public enum Result {
		STARTED, ALREADY_RUNNING, STOPPED, NOT_RUNNING
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(SimulationControl.class);
	private static Thread simulationThread;

	private SimulationControl() {
	}

	public static boolean isRunning() {
		return CompetitionSimulator.isRunning();
	}

	/**
	 * @param skipDone               skip sessions already done (defer weigh-in, keep existing results)
	 * @param skipBefore             name of the first session to simulate, or null
	 * @param platforms              comma-separated platform names, or null for all
	 * @param randomDeclarationJumps random +2/+3 kg declarations and marshal changes while the clock runs
	 */
	public static synchronized Result start(boolean skipDone, String skipBefore, String platforms,
	        boolean randomDeclarationJumps) {
		if (CompetitionSimulator.isRunning()) {
			return Result.ALREADY_RUNNING;
		}
		simulationThread = new Thread(() -> {
			try {
				new CompetitionSimulator(skipDone, skipBefore, platforms, randomDeclarationJumps).runSimulation();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger./**/warn("simulation thread interrupted");
			} catch (Throwable t) {
				logger.error("{}", LoggerUtils.stackTrace(t));
			}
		}, "competition-simulation");
		simulationThread.setDaemon(true);
		simulationThread.start();
		logger.info("simulation start requested skipDone={} skipBefore={} platforms={} randomDeclarationJumps={}",
		        skipDone, skipBefore, platforms, randomDeclarationJumps);
		return Result.STARTED;
	}

	public static synchronized Result stop() {
		boolean wasRunning = CompetitionSimulator.isRunning();
		CompetitionSimulator.stopSimulation();
		if (simulationThread != null) {
			simulationThread.interrupt();
			simulationThread = null;
		}
		logger.info("simulation stop requested (was running: {})", wasRunning);
		return wasRunning ? Result.STOPPED : Result.NOT_RUNNING;
	}
}
