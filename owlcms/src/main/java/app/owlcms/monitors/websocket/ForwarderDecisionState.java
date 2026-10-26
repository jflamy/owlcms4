/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.monitors.websocket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import app.owlcms.components.elements.DecisionElementState;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;

/**
 * Decision information sent to the tracker over the WebSocket (compliance/150-TRACKER_JURY_VOTES.md).
 *
 * <p>
 * Runs the attempt board's {@link DecisionElementState} with the jury votes always shown (full facts: the tracker
 * plugins decide what to show) and keeps what it renders as the {@code decisionState} object. Also builds the
 * {@code adjudicationDecision} object of a jury decision. The forwarder calls the {@code on...} methods from its own
 * event handlers (one per {@code @Subscribe} method of {@link DecisionElementState}), so the state is up to date
 * before each message is built.
 * </p>
 */
public class ForwarderDecisionState implements DecisionElementState.IDecisionRenderer {

	public static final String EMPTY = "empty";
	public static final String VOTED = "voted";
	public static final String WHITE = "white";
	public static final String RED = "red";

	/** Monotonic across forwarders and OWLCMS restarts: seeded from the clock, incremented on every change. */
	private static final AtomicLong SEQUENCE = new AtomicLong(System.currentTimeMillis());

	private final DecisionElementState state;
	private FieldOfPlay fop;
	private long sequence = SEQUENCE.incrementAndGet();
	private boolean visible;
	private boolean down;
	private boolean singleRef;
	private List<String> refs = emptyCells(3);
	private List<String> jury = List.of();
	private Long reviewStartMillis;
	/** review announced by the jury notification (sent before the FOP break starts); null when none */
	private String reviewType;

	public ForwarderDecisionState() {
		this.state = new DecisionElementState(this);
		this.state.setLiveReferee(false);
		this.state.setResetOnClockStart(false);
		this.state.setShowsDownSignal(true);
		this.state.setDontReset(false);
		this.state.setSilenced(true);
		this.state.setShowJuryDecisions(true);
	}

	public synchronized void setFop(FieldOfPlay fop) {
		this.fop = fop;
		this.state.setFop(fop);
		resync();
	}

	/**
	 * Bring the state up to date with the FOP, as an attempt board does when it is attached (connection, session
	 * switch): during a review break, the votes that led to the review.
	 */
	public synchronized void resync() {
		if (this.fop != null && this.fop.getState() == FOPState.BREAK) {
			this.state.showPanelReviewInitialVotes(null);
		}
	}

	// ------------------------------------------------------------------------
	// Events, one per @Subscribe method of DecisionElementState
	// ------------------------------------------------------------------------

	public synchronized void onDecision(UIEvent.Decision e) {
		this.state.slaveShowDecision(e);
	}

	public synchronized void onInitialDecision(UIEvent.InitialDecision e) {
		this.state.slaveInitialDecision(e);
	}

	public synchronized void onDownSignal(UIEvent.DownSignal e) {
		this.state.slaveDownSignal(e);
	}

	public synchronized void onDecisionReset(UIEvent.DecisionReset e) {
		this.state.slaveDecisionReset(e);
	}

	public synchronized void onRefereeUpdate(UIEvent.RefereeUpdate e) {
		this.state.slaveRefereeUpdate(e);
	}

	public synchronized void onResetOnNewClock(UIEvent.ResetOnNewClock e) {
		this.state.slaveResetOnNewClock(e);
	}

	public synchronized void onBreakStarted(UIEvent.BreakStarted e) {
		this.state.slaveBreakStart(e);
	}

	public synchronized void onStartTime(UIEvent.StartTime e) {
		this.state.slaveStartTimer(e);
	}

	public synchronized void onStopTime(UIEvent.StopTime e) {
		this.state.slaveStopTimer(e);
	}

	public synchronized void onJuryNotification(UIEvent.JuryNotification e) {
		JuryDeliberationEventType type = e.getDeliberationEventType();
		if (type == JuryDeliberationEventType.START_DELIBERATION || type == JuryDeliberationEventType.CHALLENGE) {
			this.reviewStartMillis = System.currentTimeMillis();
			this.reviewType = type == JuryDeliberationEventType.CHALLENGE ? "challenge" : "deliberation";
		}
		this.state.slaveJuryNotification(e);
	}

	public synchronized void onJuryUpdate(UIEvent.JuryUpdate e) {
		this.state.slaveJuryUpdate(e);
	}

	public synchronized void onPanelVoteCalled(UIEvent.PanelVoteCalled e) {
		this.state.slavePanelVoteCalled(e);
	}

	public synchronized void onPanelVoteUpdate(UIEvent.PanelVoteUpdate e) {
		this.state.slavePanelVoteUpdate(e);
	}

	public synchronized void onJuryDisplayReset(UIEvent.JuryDisplayReset e) {
		this.state.slaveJuryDisplayReset(e);
	}

	/** Lifting resumed: the review and the attempt-board decision display are over. */
	public synchronized void onStartLifting() {
		this.reviewStartMillis = null;
		this.reviewType = null;
		// AbstractAttemptBoard hides its decision area on StartLifting. Mirror it before the forwarder sends the update,
		// otherwise the tracker keeps decisionState.visible=true and hides the next athlete's running clock.
		resetDecisionDisplay(null, this.sequence);
	}

	// ------------------------------------------------------------------------
	// Renderer: keep what the attempt board would show
	// ------------------------------------------------------------------------

	@Override
	public void setEnabled(UIEvent event, boolean enabled) {
		// only relevant to boards that accept referee input
	}

	@Override
	public synchronized void resetDecisionDisplay(UIEvent event, long generation) {
		this.visible = false;
		this.down = false;
		this.singleRef = this.fop != null && this.fop.isSingleReferee();
		this.refs = emptyCells(3);
		this.jury = emptyCells(this.jury.size());
		changed();
	}

	@Override
	public synchronized void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2,
	        Boolean ref3, boolean singleLight, boolean announcerForced) {
		Boolean displayedRef2 = singleLight ? (ref2 != null ? ref2 : decision) : ref2;
		this.visible = true;
		this.down = false;
		this.singleRef = singleLight;
		this.refs = singleLight
		        ? List.of(EMPTY, cell(displayedRef2), EMPTY)
		        : List.of(cell(ref1), cell(displayedRef2), cell(ref3));
		changed();
	}

	@Override
	public synchronized void showDownSignal(UIEvent.DownSignal event, boolean silent) {
		this.visible = true;
		this.down = true;
		this.singleRef = this.fop != null && this.fop.isSingleReferee();
		this.refs = emptyCells(3);
		changed();
	}

	@Override
	public synchronized void showJuryLights(UIEvent event, List<String> lights, long generation) {
		this.jury = List.copyOf(lights);
		changed();
	}

	@Override
	public synchronized void showPanelVote(UIEvent event, List<String> refereeStates, List<String> juryStates,
	        long generation) {
		this.visible = true;
		this.down = false;
		this.singleRef = false;
		this.refs = List.copyOf(refereeStates);
		this.jury = List.copyOf(juryStates);
		changed();
	}

	// ------------------------------------------------------------------------
	// Payloads
	// ------------------------------------------------------------------------

	/**
	 * @return the {@code decisionState} object
	 */
	public synchronized Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("sequence", this.sequence);
		Athlete athlete = this.fop == null ? null
		        : (this.fop.getAthleteUnderReview() != null ? this.fop.getAthleteUnderReview() : this.fop.getCurAthlete());
		map.put("athleteKey", athlete != null ? athlete.getKey() : null);
		map.put("visible", this.visible);
		map.put("down", this.down);
		map.put("singleRef", this.singleRef);
		map.put("refs", this.refs);
		map.put("jury", this.jury);
		String review = this.reviewType != null ? this.reviewType : review(this.fop);
		map.put("review", review);
		map.put("panelReview", this.fop != null && this.fop.isPanelReview());
		map.put("reviewVoteOpen", this.fop != null && this.fop.isPanelVoteOpen());
		if (!"none".equals(review) && this.fop != null) {
			if (this.reviewStartMillis != null) {
				map.put("reviewStartMillis", this.reviewStartMillis);
			}
			Athlete underReview = this.fop.getAthleteUnderReview();
			map.put("originalRefereeDecisions", underReview != null
			        ? originalRefereeDecisions(underReview)
			        : refereeList(this.fop.getRefereeDecision()));
		}
		return map;
	}

	/**
	 * @return the {@code adjudicationDecision} object for a jury Good Lift / No Lift notification, null for other
	 *         notifications
	 */
	public static Map<String, Object> adjudicationDecision(UIEvent.JuryNotification e, FieldOfPlay fop) {
		JuryDeliberationEventType type = e.getDeliberationEventType();
		if (type != JuryDeliberationEventType.GOOD_LIFT && type != JuryDeliberationEventType.BAD_LIFT) {
			return null;
		}
		boolean good = type == JuryDeliberationEventType.GOOD_LIFT;
		Athlete athlete = e.getAthlete();
		int liftNo = athlete != null ? athlete.getAttemptsDone() : 0;

		Map<String, Object> map = new LinkedHashMap<>();
		map.put("decision", good);
		map.put("reversal", e.getReversal());
		map.put("ruleCode", good ? null : e.getReasonCode());
		map.put("athleteKey", athlete != null ? athlete.getKey() : null);
		map.put("liftTypeKey", liftNo >= 1 && liftNo <= 6 ? (liftNo <= 3 ? "Snatch" : "Clean_and_Jerk") : null);
		map.put("attemptNumber", liftNo >= 1 && liftNo <= 6 ? (liftNo - 1) % 3 + 1 : null);
		map.put("originalRefereeDecisions", athlete != null ? originalRefereeDecisions(athlete) : refereeList(null));
		// without the seven-person panel the referees do not vote again; the FOP then holds a copy of their
		// original decision in the review array, which must not be reported as review votes
		map.put("refereeVotes", fop != null && fop.isPanelProcedure()
		        ? refereeList(e.getReviewRefereeVotes())
		        : List.of());
		int jurySize = fop != null ? fop.getJurySize() : 0;
		Boolean[] juryVotes = e.getReviewJuryVotes() != null ? e.getReviewJuryVotes()
		        : (fop != null ? fop.getJuryMemberDecision() : null);
		map.put("juryVotes", juryList(juryVotes, jurySize));
		map.put("newRecord", e.getNewRecord());
		map.put("recordKind", fop == null || fop.getLastChallengedRecords() == null
		        || fop.getLastChallengedRecords().isEmpty() ? "none" : (e.getNewRecord() ? "new" : "denied"));
		map.put("waitForAnnouncer", e.isWaitForAnnouncer());
		map.put("chairEntered", e.isChairEntered());
		map.put("speakerEntered", e.isSpeakerEntered());
		return map;
	}

	// ------------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------------

	private void changed() {
		this.sequence = SEQUENCE.incrementAndGet();
	}

	private static String review(FieldOfPlay fop) {
		if (fop == null || fop.getState() != FOPState.BREAK) {
			return "none";
		}
		BreakType breakType = fop.getBreakType();
		if (breakType == BreakType.JURY) {
			return "deliberation";
		}
		if (breakType == BreakType.CHALLENGE) {
			return "challenge";
		}
		return "none";
	}

	private static List<Boolean> originalRefereeDecisions(Athlete athlete) {
		int liftNo = athlete.getAttemptsDone();
		List<Boolean> decisions = liftNo >= 1 && liftNo <= 6 ? athlete.getDecisions(liftNo) : null;
		return refereeList(decisions != null ? decisions.toArray(new Boolean[0]) : null);
	}

	private static List<Boolean> refereeList(Boolean[] decisions) {
		List<Boolean> list = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			list.add(decisions != null && i < decisions.length ? decisions[i] : null);
		}
		return Collections.unmodifiableList(list);
	}

	private static List<Boolean> juryList(Boolean[] votes, int jurySize) {
		if (jurySize <= 0) {
			return List.of();
		}
		Boolean[] copy = votes != null ? Arrays.copyOf(votes, jurySize) : new Boolean[jurySize];
		return Collections.unmodifiableList(Arrays.asList(copy));
	}

	private static List<String> emptyCells(int size) {
		return Collections.nCopies(size, EMPTY);
	}

	private static String cell(Boolean vote) {
		return vote == null ? EMPTY : (vote ? WHITE : RED);
	}
}
