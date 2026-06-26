/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate the flow of a competition on a field of play.
 *
 * The actions of technical officials are simulated: the the events that the user interface would send (FOPEvents) are posted The state automaton in the
 * FieldOfPlay triggers the user interface updates as required. It is therefore possible to create as many real browser windows as required to observe the
 * updates taking place.
 *
 * @author Jean-François Lamy
 *
 */
public class FOPSimulator implements SafeEventBusRegistration {

	private static final boolean USE_MQTT_TIMER = true;
	static private Random r = new Random(0);
	private FieldOfPlay fop;
	private boolean groupDone;
	private List<Group> groups;
	private final boolean skipDone;
	private final boolean randomDeclarationJumps;
	private CompetitionSimulator simulator;

	// private EventBus fopEventBus;
	final private Logger logger = (Logger) LoggerFactory.getLogger(FOPSimulator.class);
	private Object origin;
	private EventBus uiEventBus;
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("Simulation-" + this.logger.getName());
	private final List<Thread> workers = Collections.synchronizedList(new ArrayList<>());
	private volatile boolean stopped;

	public FOPSimulator(FieldOfPlay f, List<Group> groups) {
		this(f, groups, false, false);
	}

	public FOPSimulator(FieldOfPlay f, List<Group> groups, boolean skipDone) {
		this(f, groups, skipDone, false);
	}

	public FOPSimulator(FieldOfPlay f, List<Group> groups, boolean skipDone, boolean randomDeclarationJumps) {
		this.fop = f;
		this.groups = groups;
		this.skipDone = skipDone;
		this.randomDeclarationJumps = randomDeclarationJumps;
	}

	public void setCompetitionSimulator(CompetitionSimulator simulator) {
		this.simulator = simulator;
	}

	public void go() throws InterruptedException {
		// explicitly use the generic subscriber overload (not a Vaadin Component)
		this.uiEventBus = uiEventBusRegisterNoUI((Object) this, this.fop);
		this.setOrigin(this);

		this.logger.info("simulating fop {}", this.fop.getName());
		if (!startNextGroup(this.groups)) {
			CompetitionSimulator.simulatorCompleted(this);
		}
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) throws InterruptedException {
		if (!isActive()) {
			return;
		}
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		startWorker(() -> {
			if (this.groupDone) {
				if (this.groups.size() > 0) {
					this.groups.remove(0);
					if (!startNextGroup(this.groups)) {
						CompetitionSimulator.simulatorCompleted(this);
					}
				}
			} else {
				doNextAthleteWithDeclaration(e);
			}
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// nothing to do, wait for decision reset
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) throws InterruptedException {
		if (!isActive()) {
			return;
		}
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// note that the group is done.
		this.groupDone = false; // WAS true
		startWorker(() -> {
			this.logger.info("########## group {} done", e.getGroup());
			if (this.groups.size() > 0) {
				if (this.groups.get(0).getName().contentEquals(e.getGroup().getName())) {
					this.groups.remove(0);
				}
				if (!startNextGroup(this.groups)) {
					CompetitionSimulator.simulatorCompleted(this);
				}
			} else {
				CompetitionSimulator.simulatorCompleted(this);
			}
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		// nothing to do
	}

	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		// nothing to do
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		// nothing to do
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) throws InterruptedException {
		if (!isActive()) {
			return;
		}
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		startWorker(() -> doNextAthlete(e));
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		// nothing to do
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) throws InterruptedException {
		if (!isActive()) {
			return;
		}
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		startWorker(() -> doSwitchGroup(e));
	}

	public void unregister() {
		this.logger.debug("unregister simulator {}", this.fop != null ? this.fop.getName() : null);
		if (this.uiEventBus != null) {
			this.uiEventBus.unregister(this);
		}
	}

	public void stop() {
		this.stopped = true;
		unregister();
		synchronized (this.workers) {
			for (Thread t : this.workers) {
				t.interrupt();
			}
			this.workers.clear();
		}
	}

	protected void doEmpty() {
	}

	@SuppressWarnings("unused")
	protected void doLift(Athlete a) {
		if (!isActive()) {
			return;
		}
		if (a == null) {
			doEmpty();
			return;
		} else if (a.getAttemptsDone() >= 6) {
			// do nothing. wait on decision reset for last athlete.
			// doDone(fop.getGroup());
			return;
		}
		if (skipLiftDuringBreak(a, "before timer start")) {
			return;
		}

		MQTTMonitor mm = this.fop.getMqttMonitor();
		// do a lift in group g: start timer
		if (USE_MQTT_TIMER && mm != null) {
			try {
				mm.simulateStartAthleteTimer();
			} catch (MqttException | RuntimeException e) {
				LoggerUtils.logError(this.logger, e);
			}

		} else {
			this.fop.fopEventPost(new FOPEvent.TimeStarted(this));
		}

		// wait for clock to run down a bit
		if (!sleepQuietly(2000)) {
			return;
		}

		if (skipLiftDuringBreak(a, "before timer stop")) {
			return;
		}

		// stop time and get decisions
		if (USE_MQTT_TIMER && mm != null) {
			try {
				mm.simulateStopAthleteTimer();
			} catch (MqttException | RuntimeException e) {
				LoggerUtils.logError(this.logger, e);
			}
		} else {
			this.fop.fopEventPost(new FOPEvent.TimeStopped(this));
		}

		// wait for clock to run down a bit
		if (!sleepQuietly(1000)) {
			return;
		}

		if (skipLiftDuringBreak(a, "before referee decisions")) {
			return;
		}

		// stop time and get decisions
		if (USE_MQTT_TIMER && mm != null) {
			try {
				mm.publishRefDecision(0, goodLift(r));
				mm.publishRefDecision(1, goodLift(r));
				mm.publishRefDecision(2, goodLift(r));
			} catch (MqttException | RuntimeException e) {
				LoggerUtils.logError(this.logger, e);
			}
		} else {
			this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
			this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
			this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
		}

	}

	Object getOrigin() {
		return this.origin;
	}

	private void doDeclaration(Athlete athlete, String automatic) {
		final String weight = automatic;
		int liftNo = athlete.getAttemptsDone() + 1;
		switch (liftNo) {
			case 1:
				athlete.setSnatch1Declaration(weight);
				break;
			case 2:
				athlete.setSnatch2Declaration(weight);
				break;
			case 3:
				athlete.setSnatch3Declaration(weight);
				break;
			case 4:
				athlete.setCleanJerk1Declaration(weight);
				break;
			case 5:
				athlete.setCleanJerk2Declaration(weight);
				break;
			case 6:
				athlete.setCleanJerk3Declaration(weight);
				break;
		}
	}

	private void doNextAthlete(UIEvent e) {
		if (!isActive()) {
			return;
		}
		List<Athlete> order = this.fop.getLiftingOrder();
		Athlete athlete = order.size() > 0 ? order.get(0) : null;

		if (!sleepQuietly(1000)) {
			return;
		}
		if (skipLiftDuringBreak(athlete, "after next-athlete delay")) {
			return;
		}
		doLift(athlete);
	}

	private void doNextAthleteWithDeclaration(UIEvent e) {
		if (!isActive()) {
			return;
		}
		if (!sleepQuietly(2000)) {
			return;
		}
		if (skipLiftDuringBreak(null, "after declaration delay")) {
			return;
		}

		List<Athlete> order = this.fop.getLiftingOrder();
		Athlete athlete = order.size() > 0 ? order.get(0) : null;
		if (athlete == null) {
			return;
		}

		String declaration = athlete.getCurrentDeclaration();
		String automatic = athlete.getCurrentAutomatic();
		if ((declaration == null || declaration.isBlank()) && shouldDeclareRandomJump()) {
			if (automatic != null && !automatic.isBlank()) {
				try {
					int autoAsInt = Integer.parseInt(automatic);
					doDeclaration(athlete, Integer.toString(autoAsInt + declarationIncrement()));
					this.fop.fopEventPost(new FOPEvent.WeightChange(this, athlete, false));
				} catch (NumberFormatException e1) {
					// ignore
				}
			}
		}

		// recompute lifting order based on exception
		order = this.fop.getLiftingOrder();
		athlete = order.size() > 0 ? order.get(0) : null;
		if (skipLiftDuringBreak(athlete, "before declared lift")) {
			return;
		}
		doLift(athlete);
	}

	private void doSwitchGroup(UIEvent.SwitchGroup e) {
		switch (this.fop.getState()) {
			case INACTIVE:
				doEmpty();
				break;
			case BREAK:
				if (e.getGroup() == null) {
					doEmpty();
				} else {
					// doBreak();
				}
				break;
			default:
				// doLift(fop.getCurAthlete());
		}
	}

	private boolean goodLift(Random r) {
		return r.nextFloat() < 0.7;
	}

	private boolean shouldDeclareRandomJump() {
		return this.randomDeclarationJumps && r.nextFloat() < 0.25F;
	}

	private int declarationIncrement() {
		return r.nextBoolean() ? 2 : 3;
	}

	private void setOrigin(Object origin) {
		this.origin = origin;
	}

	private boolean startNextGroup(List<Group> curGs) {
		if (!isActive()) {
			return false;
		}
		if (curGs != null && curGs.size() > 0) {
			Group g = curGs.get(0);
			if (this.skipDone && this.simulator != null) {
				this.simulator.prepareSkipDoneGroup(g);
			}
			this.logger.info("########## waiting to start group {} of {}", g, curGs);
			if (!sleepQuietly(6000)) {
				return false;
			}
			this.logger.info("{}########## switching to group {} of {}", FieldOfPlay.getLoggingName(this.fop), g, curGs);
			this.fop.fopEventPost(new FOPEvent.SwitchGroup(g, this));
			
			// Assign start numbers to athletes in the group for simulation
			List<Athlete> athletes = g.getAthletes();
			if (athletes != null && !athletes.isEmpty()) {
				this.logger.debug("{}########## About to assign start numbers. Athletes in group: {}", 
					FieldOfPlay.getLoggingName(this.fop), athletes.size());
				for (Athlete a : athletes) {
					this.logger.debug("{}########## Athlete: {} {} - bodyWeight: {} startNumber: {}", 
						FieldOfPlay.getLoggingName(this.fop), 
						a.getLastName(), a.getFirstName(), a.getBodyWeight(), a.getStartNumber());
				}
				AthleteSorter.testAssignStartNumbers(athletes);
				this.logger.info("{}########## assigned start numbers for group {}", FieldOfPlay.getLoggingName(this.fop), g);
				for (Athlete a : athletes) {
					this.logger.debug("{}########## After assignment - Athlete: {} {} - bodyWeight: {} startNumber: {}", 
						FieldOfPlay.getLoggingName(this.fop), 
						a.getLastName(), a.getFirstName(), a.getBodyWeight(), a.getStartNumber());
				}
			}
			
			this.logger.info("{}########## starting group {}", FieldOfPlay.getLoggingName(this.fop), g);
			this.groupDone = false;
			this.fop.fopEventPost(new FOPEvent.StartLifting(this));

			return true;
		} else {
			return false;
		}
	}

	private boolean isActive() {
		return !this.stopped && CompetitionSimulator.isRunning();
	}

	private boolean isInBreak() {
		return isActive() && this.fop != null && this.fop.getState() == app.owlcms.fieldofplay.FOPState.BREAK;
	}

	private boolean skipLiftDuringBreak(Athlete athlete, String phase) {
		if (!isInBreak()) {
			return false;
		}
		this.logger.info("{}simulation in break {}; skipping lift phase={} athlete={}",
		        FieldOfPlay.getLoggingName(this.fop),
		        this.fop.getBreakType(),
		        phase,
		        athlete != null ? athlete.getFullName() : "(no athlete)");
		return true;
	}

	private boolean sleepQuietly(long millis) {
		if (!isActive()) {
			return false;
		}
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
		return isActive();
	}

	private void startWorker(Runnable task) {
		if (!isActive()) {
			return;
		}
		Thread thread = new Thread(() -> {
			try {
				task.run();
			} finally {
				this.workers.remove(Thread.currentThread());
			}
		});
		this.workers.add(thread);
		thread.start();
	}

}
