/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.template.Id;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.components.elements.StopwatchTimerElement;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;

/**
 * Class Results
 *
 * Show results scoreboard for a session, including records and leaders
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class Results extends BaseResults {
	
	protected final Logger logger = (Logger) LoggerFactory.getLogger(Results.class);

	@Id("breakTimer")
	private BreakTimerElement breakTimer; // WebComponent, injected by Vaadin
	@Id("decisions")
	private DecisionElement decisions; // WebComponent, injected by Vaadin
	@Id("timer")
	private AthleteTimerElement timer; // WebComponent, injected by Vaadin
	@Id("decisionSectionBreakTimer")
	private BreakTimerElement decisionSectionBreakTimer; // WebComponent, injected by Vaadin
	@Id("decisionSectionTimer")
	private AthleteTimerElement decisionSectionTimer; // WebComponent, injected by Vaadin
	@Id("decisionSectionStopwatch")
	private StopwatchTimerElement decisionSectionStopwatch; // WebComponent, injected by Vaadin
	@Id("decisionSectionReferee")
	private JuryDisplayDecisionElement dsRefereeDecisions; // WebComponent, injected by Vaadin
	private final Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

	public Results() {
		this.uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
		this.getElement().setProperty("scoreboardType", this.getClass().getSimpleName());
		this.getElement().setProperty("showDecisionSection",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION));
		this.getElement().setProperty("showProjectedRanks",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DISPLAY_PROJECTED_RANKS));
		this.getElement().setProperty("showScoreboardTimers",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DISPLAY_SCOREBOARD_TIMERS));
		overrideColors(this.getElement());
	}

	public BreakTimerElement getBreakTimer() {
		return breakTimer;
	}

	public void setBreakTimer(BreakTimerElement breakTimer) {
		this.breakTimer = breakTimer;
	}

	public DecisionElement getDecisions() {
		return decisions;
	}

	public void setDecisions(DecisionElement decisions) {
		this.decisions = decisions;
	}

	public AthleteTimerElement getTimer() {
		return timer;
	}

	public void setTimer(AthleteTimerElement timer) {
		this.timer = timer;
	}
	
	@Override
	public void setSilenced(boolean silent) {
		super.setSilenced(silent);
		this.getTimer().setSilenced(silent);
		this.getBreakTimer().setSilenced(silent);
		if (this.decisionSectionTimer != null) {
			this.decisionSectionTimer.setSilenced(true);
		}
		if (this.decisionSectionBreakTimer != null) {
			this.decisionSectionBreakTimer.setSilenced(true);
		}
		if (this.decisionSectionStopwatch != null) {
			this.decisionSectionStopwatch.setSilenced(true);
		}
	}
	
	@Override
	public void setDownSilenced(boolean silent) {
		super.setDownSilenced(silent);
		this.getDecisions().setSilenced(silent);
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setSilenced(true);
		}
	}

	@Override
	protected void propagateFopToTimerElements(FieldOfPlay fop) {
		if (this.breakTimer != null) {
			this.breakTimer.setFop(fop);
		}
		if (this.timer != null) {
			this.timer.setFop(fop);
		}
		if (this.decisions != null) {
			this.decisions.setFop(fop);
		}
		if (this.decisionSectionBreakTimer != null) {
			this.decisionSectionBreakTimer.setSilenced(true);
			this.decisionSectionBreakTimer.setFop(fop);
		}
		if (this.decisionSectionTimer != null) {
			this.decisionSectionTimer.setSilenced(true);
			this.decisionSectionTimer.setFop(fop);
		}
		if (this.decisionSectionStopwatch != null) {
			this.decisionSectionStopwatch.setSilenced(true);
			this.decisionSectionStopwatch.setFop(fop);
		}
		// Always give the jury display element a FOP so it initialises cleanly even when
		// DECISION_SECTION is off (it is always present in the DOM, just hidden).
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setSilenced(true);
			this.dsRefereeDecisions.setFop(fop);
		}
		if (Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION)) {
			syncDecisionSection(fop);
		}
	}

	/**
	 * The decision-section sync done in {@link #propagateFopToTimerElements(FieldOfPlay)} runs
	 * before BaseResults registers this page on the FOP UI event bus, so clearing events fired
	 * during page load (resume, new clock) could be missed, leaving a stale snapshot. Re-sync
	 * once the bus registration is in place.
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		if (Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION)) {
			syncDecisionSection(getFop());
		}
	}

	/**
	 * Sync the bottom decision-section state. The referee lights
	 * ({@link JuryDisplayDecisionElement}) are self-subscribing once given the FOP.
	 * The jury lights are simple circles rendered directly in Results.js, driven by the single
	 * {@link #slaveJuryUpdate(UIEvent.JuryUpdate)} subscription on this page.
	 */
	private void syncDecisionSection(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setFinalOnly(Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_REF_FINAL_ONLY));
			this.dsRefereeDecisions.setSilenced(true);
			this.dsRefereeDecisions.setFop(fop);
			syncLiveRefereeDecisions(fop);
		}
		// Live jury member decisions: show empty circles, then reflect current votes.
		this.getElement().setProperty("dsShowStopwatch",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_STOPWATCH));
		initJuryDecisions();
		pushJuryDecisions();
		syncJuryMessage(fop);
	}

	private int getNbJurors() {
		return Competition.getCurrent().getJurySize();
	}

	/** Show one empty placeholder circle per juror. */
	private void initJuryDecisions() {
		JsonArray decisions = Json.createArray();
		for (int i = 0; i < getNbJurors(); i++) {
			decisions.set(i, "empty");
		}
		this.getElement().setPropertyJson("juryDecisions", decisions);
		clearJuryMessage();
	}

	private void clearJuryMessage() {
		this.getElement().setProperty("juryMessage", "");
		if (this.decisionSectionStopwatch != null) {
			this.decisionSectionStopwatch.clearCountUp();
		}
	}

	private void setJuryMessage(JuryDeliberationEventType type) {
		String key = type == JuryDeliberationEventType.CHALLENGE ? "PublicMsg.CHALLENGE" : "PublicMsg.JuryDeliberation";
		this.getElement().setProperty("juryMessage", Translator.translate(key));
		if (this.decisionSectionStopwatch != null
		        && Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_STOPWATCH)) {
			this.decisionSectionStopwatch.startCountUp();
		}
	}

	private void syncJuryMessage(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}
		BreakType breakType = fop.getBreakType();
		if (breakType == BreakType.JURY) {
			setJuryMessage(JuryDeliberationEventType.START_DELIBERATION);
		} else if (breakType == BreakType.CHALLENGE) {
			setJuryMessage(JuryDeliberationEventType.CHALLENGE);
		}
	}

	/**
	 * Reflect the current jury votes, including the initial votes cast while lifting is
	 * still in progress (before any deliberation break starts). Until every juror has
	 * voted, individual votes are hidden (shown as a filled "voted" circle) so they do
	 * not influence the others; once all have voted the actual white/red decisions are
	 * revealed. Stale votes are cleared by the explicit reset events (new clock, start
	 * lifting, end of deliberation/challenge, announced verdict).
	 */
	private void pushJuryDecisions() {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			return;
		}
		Boolean[] votes = fop.getJuryMemberDecision();
		int n = getNbJurors();
		boolean allVoted = true;
		for (int i = 0; i < n; i++) {
			if (votes == null || votes[i] == null) {
				allVoted = false;
				break;
			}
		}
		JsonArray decisions = Json.createArray();
		for (int i = 0; i < n; i++) {
			Boolean v = (votes != null) ? votes[i] : null;
			if (allVoted) {
				decisions.set(i, v ? "white" : "red");
			} else {
				decisions.set(i, v != null ? "voted" : "empty");
			}
		}
		this.getElement().setPropertyJson("juryDecisions", decisions);
	}

	/**
	 * Single page-level subscription for jury member decisions. BaseResults does not
	 * subscribe to JuryUpdate, so this is the only handler for that event type on the page.
	 */
	@Subscribe
	public void slaveJuryUpdate(UIEvent.JuryUpdate e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			// First-vote-only (default): once a jury deliberation/challenge is in progress,
			// freeze the circles on the initial vote and ignore the re-vote. When showing
			// both votes, let the deliberation vote update the circles live.
			FieldOfPlay fop = getFop();
			boolean inDeliberation = fop != null && (fop.getBreakType() == BreakType.JURY
			        || fop.getBreakType() == BreakType.CHALLENGE);
			boolean showBothVotes = Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_SHOW_BOTH_JURY_VOTES);
			if (inDeliberation && !showBothVotes) {
				return;
			}
			pushJuryDecisions();
		});
	}

	/** Reset jury circles on the same new-clock event used by the jury panel. */
	@Subscribe
	public void slaveJuryResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			initJuryDecisions();
		});
	}

	/** Keep the inherited notification behavior, and mirror jury-panel vote resets. */
	@Override
	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		super.slaveJuryNotification(e);
		JuryDeliberationEventType type = e.getDeliberationEventType();
		if (type == JuryDeliberationEventType.START_DELIBERATION || type == JuryDeliberationEventType.CHALLENGE) {
			// Default is first-vote-only: keep the jury lights from the initial vote and do
			// not clear them when a new deliberation/challenge starts (they stay visible
			// until the END_* events restore the athlete-centric view). When showing both
			// the initial and final votes, clear at the start so each deliberation's votes
			// are shown fresh.
			boolean showBothVotes = Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_SHOW_BOTH_JURY_VOTES);
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				if (showBothVotes) {
					initJuryDecisions();
				}
				setJuryMessage(type);
			});
		} else if (type == JuryDeliberationEventType.GOOD_LIFT || type == JuryDeliberationEventType.BAD_LIFT) {
			// The deliberation is only over once the speaker announces. While the jury
			// decision is waiting for the announcer, everything stays visible.
			if (!e.isWaitForAnnouncer()) {
				UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
					// Announced: clear message, stopwatch, jury circles and referee lights right away.
					initJuryDecisions();
					if (this.dsRefereeDecisions != null) {
						this.dsRefereeDecisions.doReset();
					}
				});
			}
		} else if (type == JuryDeliberationEventType.END_JURY_BREAK || type == JuryDeliberationEventType.END_CHALLENGE) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				initJuryDecisions();
			});
		}
	}

	/**
	 * Clear the bottom decision section when the competition resumes (announcer ends a
	 * jury/challenge break, or the jury decision display period ends: both paths post
	 * StartLifting). The referee lights element ignores DecisionReset on purpose (jury
	 * console behavior) and does not listen to StartLifting, so we reset it here through
	 * the already-registered BaseResults subscription.
	 */
	@Override
	public void slaveStartLifting(UIEvent.StartLifting e) {
		super.slaveStartLifting(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			initJuryDecisions();
			if (this.dsRefereeDecisions != null) {
				this.dsRefereeDecisions.doReset();
			}
		});
	}

	/**
	 * Single page-level subscription for the start of an attempt clock; clears the jury
	 * circles back to empty for the new attempt. BaseResults does not subscribe to StartTime.
	 */
	@Subscribe
	public void slaveJuryStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			initJuryDecisions();
		});
	}

	private void syncLiveRefereeDecisions(FieldOfPlay fop) {
		if (fop == null || this.dsRefereeDecisions == null) {
			return;
		}
		Boolean[] curRefDecisions = fop.getRefereeDecision();
		Long[] curRefTimes = fop.getRefereeTime();
		boolean hasDecision = false;
		if (curRefDecisions != null) {
			for (Boolean d : curRefDecisions) {
				if (d != null) {
					hasDecision = true;
					break;
				}
			}
		}
		if (!hasDecision) {
			return;
		}
		InputKind inputKind = fop.getCurrentInputKind();
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		// origin must NOT be this page: the element ignores events whose origin is its
		// parent (self-origin filter in uiAccessIgnoreIfSelfOrigin).
		if (singleRef) {
			this.dsRefereeDecisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(
			        fop.getAthleteUnderReview(), null, curRefDecisions[1], null, null,
			        curRefTimes[1], null, this.dsRefereeDecisions, true, fop));
		} else {
			this.dsRefereeDecisions.slaveRefereeUpdate(new UIEvent.RefereeUpdate(
			        fop.getAthleteUnderReview(), curRefDecisions[0], curRefDecisions[1], curRefDecisions[2],
			        curRefTimes[0], curRefTimes[1], curRefTimes[2], this.dsRefereeDecisions, false, fop));
		}
	}

}