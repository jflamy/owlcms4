/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.fieldofplay.AdjudicationPanel;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.JuryRejectionReasons;
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
		this.getElement().setProperty("showDecisionSection", isDecisionSectionEnabled());
		this.getElement().setProperty("showProjectedRanks",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_PROJECTED_RANKS));
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

	/** three-medal (snatch/C&J/total) vs total-only, per the athlete's championship */
	protected boolean awardsLiftMedals(Athlete a) {
		Category athleteCategory = a.getCategory();
		AgeGroup athleteAgeGroup = athleteCategory != null ? athleteCategory.getAgeGroup() : null;
		Championship athleteChampionship = athleteAgeGroup != null ? athleteAgeGroup.getChampionship() : null;
		return athleteChampionship != null
		        ? athleteChampionship.isSnatchCJTotalMedals()
		        : Championship.of(null).isSnatchCJTotalMedals();
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
		// the decision section is off (it is always present in the DOM, just hidden).
		if (this.dsRefereeDecisions != null) {
			this.dsRefereeDecisions.setSilenced(true);
			this.dsRefereeDecisions.setFop(fop);
		}
		if (isDecisionSectionEnabled()) {
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
		FieldOfPlay fop = getFop();
		if (fop != null) {
			this.getElement().setProperty("projectedRankClockStopped", fop.getState() == FOPState.TIME_STOPPED);
		}
		if (isDecisionSectionEnabled()) {
			syncStateFromFop(fop);
		}
	}

	@Override
	public void setDecisionSection(boolean b) {
		super.setDecisionSection(b);
		refreshDecisionSection();
	}

	/** The URL parameter is read after construction and can be toggled from the display dialog. */
	private void refreshDecisionSection() {
		boolean enabled = isDecisionSectionEnabled();
		if (enabled == this.getElement().getProperty("showDecisionSection", false)) {
			return;
		}
		this.getElement().setProperty("showDecisionSection", enabled);
		if (!isAttached()) {
			// propagateFopToTimerElements and onAttach will sync
			return;
		}
		if (enabled) {
			syncStateFromFop(getFop());
		} else {
			this.decisionBlock.onStartLifting();
		}
	}

	/** A not-running clock shown for an athlete re-enables the projected rank until the next stop. */
	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		super.doUpdate(a, e);
		FieldOfPlay fop = e != null ? e.getFop() : null;
		if (fop != null && fop.getState() == FOPState.CURRENT_ATHLETE_DISPLAYED) {
			this.getElement().setProperty("projectedRankClockStopped", false);
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
		this.decisionBlock.setShowJuryVotes(
		        Config.getCurrent().featureSwitch(FeatureSwitch.JURY_DECISIONS_ON_SCOREBOARD));
		this.decisionBlock.setJurySize(fop.getJurySize());
		this.getElement().setProperty("dsShowStopwatch",
		        Config.getCurrent().featureSwitch(FeatureSwitch.DECISION_SECTION_STOPWATCH));
	}

	/**
	 * Driven by the {@code decisionSection} URL parameter; its default (public/video vs warmup) is
	 * computed by the page when the URL is read.
	 */
	private boolean isDecisionSectionEnabled() {
		return isDecisionSection();
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
			Boolean[] initialJury = initialJuryVotes(fop);
			this.decisionBlock.onDeliberationStart(type, fop.getAthleteUnderReview(), computeRefLights(fop),
			        initialJury);
			if (fop.isPanelVoteOpen() && fop.isPanelVoteComplete()) {
				// the vote is complete: the seven votes are shown, the decision follows
				Boolean[] refs = fop.getReviewRefereeDecision();
				Boolean[] jury = fop.getJuryMemberDecision();
				this.decisionBlock.onPanelVote(
				        new DecisionBlockState.RefLights(fop.getPanelVoteMajority(), refs[0], refs[1], refs[2], false,
				                false),
				        Arrays.copyOf(jury, fop.getJurySize()));
			} else if (fop.isPanelVoteOpen()) {
				this.decisionBlock.onPanelVote(null, null, panelVoted(fop));
			} else if (initialJury == null) {
				this.decisionBlock.onJuryUpdate(fop.getJuryMemberDecision(), fop.getJurySize());
			}
		} else if (fop.getState() == FOPState.DECISION_VISIBLE) {
			this.decisionBlock.onRefereeDecision(fop.getAthleteUnderReview(), computeRefLights(fop));
			this.decisionBlock.onJuryUpdate(fop.getJuryMemberDecision(), fop.getJurySize());
		} else {
			// Same rule as the StartLifting handler: a four-member jury verdict stays until the review timeout. This
			// re-sync runs on every StartLifting and SwitchGroup, so it must not undo the lingering verdict.
			this.decisionBlock.onStartLifting(fop.getState() == FOPState.CURRENT_ATHLETE_DISPLAYED
			        && fop.isSevenPersonAdjudication());
		}
	}

	/**
	 * Review break (seven-person panel, or after an automatic stop): the jury votes that led to the review, shown with the
	 * referees' decision until the vote is called (same as the attempt board). Null when there is no such pattern.
	 */
	private static Boolean[] initialJuryVotes(FieldOfPlay fop) {
		Boolean[] votes = fop != null ? fop.getPanelReviewInitialVotes() : null;
		if (votes == null || votes.length <= AdjudicationPanel.NB_REFEREES) {
			return null;
		}
		return Arrays.copyOfRange(votes, AdjudicationPanel.NB_REFEREES, votes.length);
	}

	/** who has voted in the review vote in progress (3 referees then the jury members) */
	private static boolean[] panelVoted(FieldOfPlay fop) {
		boolean[] voted = new boolean[fop.getReviewVoteSize()];
		Boolean[] referees = fop.getReviewRefereeDecision();
		Boolean[] jury = fop.getJuryMemberDecision();
		for (int i = 0; i < AdjudicationPanel.NB_REFEREES; i++) {
			voted[i] = referees != null && i < referees.length && referees[i] != null;
		}
		for (int i = 0; i < fop.getJurySize(); i++) {
			voted[AdjudicationPanel.NB_REFEREES + i] = jury != null && i < jury.length && jury[i] != null;
		}
		return voted;
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
		FieldOfPlay fop = getFop();
		String key = type == JuryDeliberationEventType.CHALLENGE ? "PublicMsg.CHALLENGE"
		        : (fop != null && fop.isPanelReview() ? "PublicMsg.AdjudicationPanelReview"
		                : "PublicMsg.JuryDeliberation");
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
	public void renderVotedRefereeLights(boolean[] voted) {
		if (this.dsRefereeDecisions == null) {
			return;
		}
		setRefereeLightsHidden(false);
		List<String> states = new ArrayList<>(voted.length);
		for (boolean v : voted) {
			states.add(v ? "voted" : "empty");
		}
		this.dsRefereeDecisions.showPanelRefereeStates(states);
	}

	@Override
	public void renderVotedJuryCircles(boolean[] voted) {
		setJuryLightsHidden(false);
		ArrayNode jsonDecisions = JsonUtils.array();
		for (int i = 0; i < voted.length; i++) {
			JsonUtils.set(jsonDecisions, i, voted[i] ? "voted" : "empty");
		}
		this.getElement().setPropertyJson("juryDecisions", jsonDecisions);
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
	public void renderJuryVerdict(boolean good, String rule) {
		String key = good ? "JuryDialog.GoodLiftLabel" : "JuryDialog.BadLiftLabel";
		String label = Translator.translate(key);
		this.getElement().setProperty("juryMessage", rule != null ? label + " \u2013 " + rule : label);
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
		if (!juryOrChallenge) {
			// READY rendering can restore the FOP's retained current athlete.
			this.getElement().setProperty("decisionSectionCurrentActive", false);
		}
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
		if (e.isReviewVote()) {
			// the review vote is shown from PanelVoteUpdate, revealed with the decision
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			this.decisionBlock.onJuryUpdate(e.getJuryMemberDecision(), e.getJurySize());
		});
	}

	/** Reset jury circles without changing the attempt-scoped projected-rank state. */
	@Subscribe
	public void slaveJuryResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isDecisionSectionEnabled()) {
				this.decisionBlock.onResetOnNewClock();
			}
		});
	}

	/**
	 * Review vote: who has voted in gray while in progress, then the votes together as soon as the last one is in. The
	 * verdict label (with the rule for a No Lift) follows when the Jury Chair confirms the decision.
	 */
	@Subscribe
	public void slavePanelVoteUpdate(UIEvent.PanelVoteUpdate e) {
		if (!isDecisionSectionEnabled() || (!e.isOpen() && !e.isComplete())) {
			return;
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			Boolean[] votes = e.getVotes();
			if (votes == null) {
				// who has voted is shown in gray, the values wait for the decision; in a jury vote (anything but the
				// seven-person panel) the referees do not vote again and their decision stays shown
				FieldOfPlay fop = e.getFop();
				DecisionBlockState.RefLights kept = null;
				if (fop != null && !fop.isPanelProcedure()) {
					Boolean[] refs = fop.getRefereeDecision();
					if (refs != null && refs.length >= AdjudicationPanel.NB_REFEREES) {
						kept = new DecisionBlockState.RefLights(AdjudicationPanel.refereeMajority(refs), refs[0],
						        refs[1], refs[2], false, false);
					}
				}
				this.decisionBlock.onPanelVote(null, null, e.getVoted(), kept);
				return;
			}
			DecisionBlockState.RefLights reviewLights = new DecisionBlockState.RefLights(e.getMajority(), votes[0],
			        votes[1], votes[2], false, false);
			this.decisionBlock.onPanelVote(reviewLights, Arrays.copyOfRange(votes, AdjudicationPanel.NB_REFEREES,
			        votes.length));
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
				this.decisionBlock.onDeliberationStart(type, athleteUnderReview, computeRefLights(fop),
				        initialJuryVotes(fop));
				break;
			case GOOD_LIFT:
			case BAD_LIFT:
				if (fop != null && fop.getCurAthlete() != null) {
					doUpdate(fop.getCurAthlete(), e);
				}
				// the technical rule relied upon for a No Lift, when the Jury Chair gave it (TCRR 7.5.7)
				String rule = JuryRejectionReasons.label(e.getReasonCode());
				this.decisionBlock.onJuryVerdict(type == JuryDeliberationEventType.GOOD_LIFT, e.isWaitForAnnouncer(),
				        rule);
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
		FieldOfPlay fop = getFop();
		// four-member jury: the verdict stays until the review timeout (or an earlier clock start, Resume, break)
		this.decisionBlock.onStartLifting(fop != null && fop.isSevenPersonAdjudication());
	}

	/** An explicit Resume (Jury Control or jury box) cleared the jury cycle. */
	@Subscribe
	public void slaveJuryDisplayReset(UIEvent.JuryDisplayReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isDecisionSectionEnabled()) {
				this.decisionBlock.onJuryDisplayReset();
			}
		});
	}

	/**
	 * Single page-level subscription for the start of an attempt clock. BaseResults does not
	 * subscribe to StartTime.
	 */
	@Subscribe
	public void slaveJuryStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (isDecisionSectionEnabled()) {
				this.decisionBlock.onStartTime();
			}
		});
	}

	/** The projected rank stays visible until the athlete clock is stopped for the first time. */
	@Subscribe
	public void slaveProjectedRankStopTime(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e,
		        () -> this.getElement().setProperty("projectedRankClockStopped", true));
	}

}