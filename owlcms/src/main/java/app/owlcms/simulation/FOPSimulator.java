/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.simulation;

import java.util.List;
import java.util.Random;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 *
 * Simulate the flow of a competition on a field of play.
 *
 * The actions of technical officials are simulated: the the events that the user interface would send (FOPEvents) are
 * posted The state automaton in the FieldOfPlay triggers the user interface updates as required. It is therefore
 * possible to create as many real browser windows as required to observe the updates taking place.
 *
 * @author Jean-François Lamy
 *
 */
public class FOPSimulator {

	private static final boolean USE_MQTT_TIMER = true;
	static private Random r = new Random(0);
	private FieldOfPlay fop;
	private boolean groupDone;
	private List<Group> groups;

	// private EventBus fopEventBus;
	final private Logger logger = (Logger) LoggerFactory.getLogger(FOPSimulator.class);
	private Object origin;
	private EventBus uiEventBus;
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("Simulation-" + this.logger.getName());

	public FOPSimulator(FieldOfPlay f, List<Group> groups) {
		this.fop = f;
		this.groups = groups;
	}

	public void go() throws InterruptedException {
		this.uiEventBus = this.fop.getUiEventBus();
		this.uiEventBus.register(this);
		this.setOrigin(this);

		this.logger.info("simulating fop {}", this.fop.getName());
		startNextGroup(this.groups);
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) throws InterruptedException {
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		new Thread(() -> {
			if (this.groupDone) {
				if (this.groups.size() > 0) {
					this.groups.remove(0);
					startNextGroup(this.groups);
				}
			} else {
				doNextAthleteWithDeclaration(e);
			}
		}).start();
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// nothing to do, wait for decision reset
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) throws InterruptedException {
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		// note that the group is done.
		this.groupDone = false; // WAS true
		new Thread(() -> {
			this.logger.info("########## group {} done", e.getGroup());
			if (this.groups.size() > 0) {
				if (this.groups.get(0).getName().contentEquals(e.getGroup().getName())) {
					this.groups.remove(0);
				}
				startNextGroup(this.groups);
			}
		}).start();
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
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		new Thread(() -> doNextAthlete(e)).start();
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		// nothing to do
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) throws InterruptedException {
		this.uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin());
		new Thread(() -> doSwitchGroup(e)).start();
	}

	public void unregister() {
		this.logger.debug("unregister simulator {}", this.fop.getName());
		this.uiEventBus.unregister(this);
	}

	protected void doEmpty() {
	}

	@SuppressWarnings("unused")
	protected void doLift(Athlete a) {
		if (a == null) {
			doEmpty();
			return;
		} else if (a.getAttemptsDone() >= 6) {
			// do nothing. wait on decision reset for last athlete.
			// doDone(fop.getGroup());
			return;
		}

		MQTTMonitor mm = this.fop.getMqttMonitor();
		// do a lift in group g: start timer
		if (USE_MQTT_TIMER && mm != null) {
			try {
				mm.publishStartAthleteTimer();
			} catch (MqttException e) {
				LoggerUtils.logError(this.logger, e);
			}

		} else {
			this.fop.fopEventPost(new FOPEvent.TimeStarted(this));
		}

		// wait for clock to run down a bit
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
		}

		// stop time and get decisions
		if (USE_MQTT_TIMER && mm != null) {
			try {
				mm.publishStopAthleteTimer();
			} catch (MqttException e) {
				LoggerUtils.logError(this.logger, e);
			}
		} else {
			this.fop.fopEventPost(new FOPEvent.TimeStopped(this));
		}

		// wait for clock to run down a bit
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 0, goodLift(r)));
		this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 1, goodLift(r)));
		this.fop.fopEventPost(new FOPEvent.DecisionUpdate(this, 2, goodLift(r)));
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
		List<Athlete> order = this.fop.getLiftingOrder();
		Athlete athlete = order.size() > 0 ? order.get(0) : null;

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
		}
		doLift(athlete);
	}

	private void doNextAthleteWithDeclaration(UIEvent e) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
		}

		List<Athlete> order = this.fop.getLiftingOrder();
		Athlete athlete = order.size() > 0 ? order.get(0) : null;
		if (athlete == null) {
			return;
		}

		String declaration = athlete.getCurrentDeclaration();
		String automatic = athlete.getCurrentAutomatic();
		if (declaration == null || declaration.isBlank()) {
			// do a fake declaration at the automatic progression to force a recompute (for
			// perfomance testing)
			if (automatic != null && !automatic.isBlank()) {
				try {
					int autoAsInt = Integer.parseInt(automatic);
					doDeclaration(athlete, Integer.toString(autoAsInt + 1));
					this.fop.fopEventPost(new FOPEvent.WeightChange(this, athlete, false));
				} catch (NumberFormatException e1) {
					// ignore
				}
			}
		}

		// recompute lifting order based on exception
		order = this.fop.getLiftingOrder();
		athlete = order.size() > 0 ? order.get(0) : null;
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

	private void setOrigin(Object origin) {
		this.origin = origin;
	}

	private boolean startNextGroup(List<Group> curGs) {
		if (curGs != null && curGs.size() > 0) {
			Group g = curGs.get(0);
			this.logger.info("########## waiting to start group {} of {}", g, curGs);
			try {
				Thread.sleep(6000);
			} catch (InterruptedException e) {
			}
			this.logger.info("{}########## switching to group {} of {}", FieldOfPlay.getLoggingName(this.fop), g, curGs);
			this.fop.fopEventPost(new FOPEvent.SwitchGroup(g, this));
			this.logger.info("{}########## starting group {}", FieldOfPlay.getLoggingName(this.fop), g);
			this.groupDone = false;
			this.fop.fopEventPost(new FOPEvent.StartLifting(this));

			return true;
		} else {
			return false;
		}
	}

}
