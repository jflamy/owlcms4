/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.template.Id;

import tools.jackson.databind.node.ArrayNode;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionBlockDecisionElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.components.elements.StopwatchTimerElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.JsonUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

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

public class Results extends BaseResults implements DecisionBlockState.DecisionSectionRenderer {
	
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
	private DecisionBlockDecisionElement dsRefereeDecisions; // WebComponent, injected by Vaadin
	private static final long REVIEW_TIMEOUT_MS = 20_000L;
	private static final ScheduledExecutorService REVIEW_TIMEOUT_SCHEDULER = Executors
	        .newSingleThreadScheduledExecutor(r -> {
		        Thread t = new Thread(r, "decision-review-timeout");
		        t.setDaemon(true);
		        return t;
	        });
	private final DecisionBlockState decisionBlock = new DecisionBlockState(this);
	private ScheduledFuture<?> reviewTimeoutFuture;
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
		// Always give the decision-block element a FOP so it initialises cleanly even when
		// DECISION_SECTION is off (it is always present in the DOM, just hidden).
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setSilenced(true);
			this.dsRefereeDecisions.setFop(fop);
		}
		if (Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION)) {
			syncStateFromFop(fop);
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
			syncStateFromFop(getFop());
		}
	}

	// ------------------------------------------------------------------------
	// Decision section: a single state machine (DecisionBlockState) drives all four
	// visual pieces (clock/athlete name, referee lights, jury circles, jury label).
	// This page implements DecisionSectionRenderer and only paints what it is told.
	// See DecisionBlock_SPEC.md in this package.
	// ------------------------------------------------------------------------

	/**
	 * Configure the state machine from the current FOP (jury size, second-vote visibility) and
	 * push the stopwatch feature switch to the element.
	 */
	private void configureDecisionBlock(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}
		this.decisionBlock.setShowBothJuryVotes(
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_SHOW_BOTH_JURY_VOTES));
		this.decisionBlock.setJurySize(fop.getJurySize());
		this.getElement().setProperty("dsShowStopwatch",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_STOPWATCH));
	}

	private boolean isDecisionSectionEnabled() {
		return Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION);
	}

	/**
	 * Derive the decision-section state from the current FOP on (re)attach or FOP change. Page
	 * load can miss the events that would have driven the state machine, so we reconstruct it.
	 */
	private void syncStateFromFop(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setSilenced(true);
			this.dsRefereeDecisions.setFop(fop);
		}
		configureDecisionBlock(fop);
		BreakType breakType = fop.getBreakType();
		boolean juryOrChallenge = breakType == BreakType.JURY || breakType == BreakType.CHALLENGE;
		if (fop.getState() == FOPState.BREAK && juryOrChallenge) {
			JuryDeliberationEventType type = breakType == BreakType.CHALLENGE
			        ? JuryDeliberationEventType.CHALLENGE
			        : JuryDeliberationEventType.START_DELIBERATION;
			this.decisionBlock.onDeliberationStart(type, fop.getAthleteUnderReview(), computeRefLights(fop));
			this.decisionBlock.onJuryUpdate(fop.getJuryMemberDecision(), fop.getJurySize());
		} else if (fop.getState() == FOPState.DECISION_VISIBLE) {
			this.decisionBlock.onRefereeDecision(fop.getAthleteUnderReview(), computeRefLights(fop));
			this.decisionBlock.onJuryUpdate(fop.getJuryMemberDecision(), fop.getJurySize());
		} else {
			this.decisionBlock.onStartLifting();
		}
	}

	private void setJuryLightsHidden(boolean hidden) {
		this.getElement().setProperty("decisionSectionHideJuryLights", hidden);
	}

	private void setRefereeLightsHidden(boolean hidden) {
		this.getElement().setProperty("decisionSectionHideRefereeLights", hidden);
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

	/**
	 * Paint the jury circles. Until every juror has voted, individual votes are hidden (shown as a
	 * filled "voted" circle) so they do not influence the others; once all have voted the actual
	 * white/red decisions are revealed.
	 */
	private void pushJuryCircles(Boolean[] votes, int n) {
		boolean allVoted = n > 0;
		for (int i = 0; i < n; i++) {
			if (votes == null || votes[i] == null) {
				allVoted = false;
				break;
			}
		}
		ArrayNode jsonDecisions = JsonUtils.array();
		for (int i = 0; i < n; i++) {
			Boolean v = (votes != null) ? votes[i] : null;
			if (allVoted) {
				JsonUtils.set(jsonDecisions, i, v ? "white" : "red");
			} else {
				JsonUtils.set(jsonDecisions, i, v != null ? "voted" : "empty");
			}
		}
		this.getElement().setPropertyJson("juryDecisions", jsonDecisions);
	}

	/**
	 * Resolve the current referee decision into ready-to-render lights, applying the
	 * single-referee and announcer rules. Returns {@code null} when no decision is present.
	 */
	private DecisionBlockState.RefLights computeRefLights(FieldOfPlay fop) {
		if (fop == null) {
			return null;
		}
		Boolean[] curRefDecisions = fop.getRefereeDecision();
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
			return null;
		}
		InputKind inputKind = fop.getCurrentInputKind();
		boolean singleRef = inputKind == InputKind.ANNOUNCER_ENTRY || inputKind == InputKind.SOLO_INPUT;
		Boolean ref1 = singleRef ? null : curRefDecisions[0];
		Boolean ref2 = curRefDecisions[1];
		Boolean ref3 = singleRef ? null : curRefDecisions[2];
		Boolean goodLift = fop.getGoodLift() != null
		        ? fop.getGoodLift()
		        : (singleRef ? Boolean.TRUE.equals(ref2)
		                : ((Boolean.TRUE.equals(ref1) ? 1 : 0)
		                        + (Boolean.TRUE.equals(ref2) ? 1 : 0)
		                        + (Boolean.TRUE.equals(ref3) ? 1 : 0)) >= 2);
		boolean announcerForced = inputKind == InputKind.ANNOUNCER_ENTRY;
		return new DecisionBlockState.RefLights(goodLift, ref1, ref2, ref3, singleRef, announcerForced);
	}

	// ------------------------------------------------------------------------
	// DecisionSectionRenderer implementation (paint only; no decisions here)
	// ------------------------------------------------------------------------

	@Override
	public void renderReadyClock() {
		clearDecisionSectionDecisionAthlete();
	}

	@Override
	public void renderAthleteUnderReview(Athlete athlete) {
		setDecisionSectionDecisionAthlete(athlete);
	}

	@Override
	public void renderRefereeLights(DecisionBlockState.RefLights lights) {
		if (this.dsRefereeDecisions == null || lights == null) {
			return;
		}
		FieldOfPlay fop = getFop();
		Athlete athlete = fop != null ? fop.getAthleteUnderReview() : null;
		InputKind inputKind = lights.announcerForced() ? InputKind.ANNOUNCER_ENTRY : null;
		setRefereeLightsHidden(false);
		// origin must NOT be this page: the element ignores events whose origin is its
		// parent (self-origin filter in uiAccessIgnoreIfSelfOrigin).
		this.dsRefereeDecisions.slaveShowDecision(new UIEvent.Decision(
		        athlete, lights.good(), lights.ref1(), lights.ref2(), lights.ref3(),
		        this.dsRefereeDecisions, fop, lights.singleRef(), TimingPolicy.IMMEDIATE, inputKind));
	}

	@Override
	public void renderEmptyRefereeLights() {
		setRefereeLightsHidden(true);
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.doReset();
		}
	}

	@Override
	public void renderJuryCircles(Boolean[] votes, int jurySize) {
		setJuryLightsHidden(false);
		pushJuryCircles(votes, jurySize);
	}

	@Override
	public void renderNoJuryCircles() {
		setJuryLightsHidden(true);
		this.getElement().setPropertyJson("juryDecisions", JsonUtils.array());
	}

	@Override
	public void renderJuryMessage(JuryDeliberationEventType type) {
		setJuryMessage(type);
	}

	@Override
	public void renderJuryVerdict(boolean good) {
		String key = good ? "JuryDialog.GoodLiftLabel" : "JuryDialog.BadLiftLabel";
		this.getElement().setProperty("juryMessage", Translator.translate(key));
		if (this.decisionSectionStopwatch != null) {
			this.decisionSectionStopwatch.clearCountUp();
		}
	}

	@Override
	public void renderNoJuryMessage() {
		clearJuryMessage();
	}

	@Override
	public void scheduleReviewTimeout() {
		cancelReviewTimeout();
		UI ui = getUI().orElse(null);
		if (ui == null) {
			return;
		}
		this.reviewTimeoutFuture = REVIEW_TIMEOUT_SCHEDULER.schedule(() -> {
			try {
				ui.access(() -> this.decisionBlock.onReviewTimeout());
			} catch (Exception ignored) {
				// UI detached: nothing to update.
			}
		}, REVIEW_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	@Override
	public void cancelReviewTimeout() {
		if (this.reviewTimeoutFuture != null) {
			this.reviewTimeoutFuture.cancel(false);
			this.reviewTimeoutFuture = null;
		}
	}

	// ------------------------------------------------------------------------
	// FOP event forwarding to the state machine
	// ------------------------------------------------------------------------

	@Override
	protected void afterSlaveDecision(UIEvent.Decision e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		boolean announcerForced = e.getInputKind() == InputKind.ANNOUNCER_ENTRY;
		DecisionBlockState.RefLights lights = new DecisionBlockState.RefLights(
		        e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
		this.decisionBlock.onRefereeDecision(e.getAthlete(), lights);
	}

	@Override
	protected void afterSlaveDecisionReset(UIEvent.DecisionReset e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		this.decisionBlock.onDecisionReset();
	}

	@Override
	protected void afterSlaveStartBreak(UIEvent.BreakStarted e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		boolean juryOrChallenge = e.getBreakType() == BreakType.JURY || e.getBreakType() == BreakType.CHALLENGE;
		this.decisionBlock.onBreakStarted(juryOrChallenge);
	}

	/**
	 * Single page-level subscription for jury member decisions. BaseResults does not subscribe to
	 * JuryUpdate, so this is the only handler for that event type on the page.
	 */
	@Subscribe
	public void slaveJuryUpdate(UIEvent.JuryUpdate e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			this.decisionBlock.onJuryUpdate(e.getJuryMemberDecision(), e.getJurySize());
		});
	}

	/** Reset jury circles on the same new-clock event used by the jury panel. */
	@Subscribe
	public void slaveJuryResetOnNewClock(UIEvent.ResetOnNewClock e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			this.decisionBlock.onResetOnNewClock();
		});
	}

	@Override
	protected void afterSlaveJuryNotification(UIEvent.JuryNotification e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		JuryDeliberationEventType type = e.getDeliberationEventType();
		if (type == null) {
			return;
		}
		FieldOfPlay fop = getFop();
		switch (type) {
			case START_DELIBERATION:
			case CHALLENGE:
				Athlete athleteUnderReview = e.getAthlete() != null ? e.getAthlete()
				        : (fop != null ? fop.getAthleteUnderReview() : null);
				this.decisionBlock.onDeliberationStart(type, athleteUnderReview, computeRefLights(fop));
				break;
			case GOOD_LIFT:
			case BAD_LIFT:
				if (fop != null && fop.getCurAthlete() != null) {
					doUpdate(fop.getCurAthlete(), e);
				}
				this.decisionBlock.onJuryVerdict(type == JuryDeliberationEventType.GOOD_LIFT, e.isWaitForAnnouncer());
				break;
			case END_JURY_BREAK:
			case END_CHALLENGE:
				this.decisionBlock.onEndBreak();
				break;
			default:
				break;
		}
	}

	@Override
	protected void afterSlaveStartLifting(UIEvent.StartLifting e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		this.decisionBlock.onStartLifting();
	}

	/**
	 * Single page-level subscription for the start of an attempt clock. BaseResults does not
	 * subscribe to StartTime.
	 */
	@Subscribe
	public void slaveJuryStartTime(UIEvent.StartTime e) {
		if (!isDecisionSectionEnabled()) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			this.decisionBlock.onStartTime();
		});
	}

}