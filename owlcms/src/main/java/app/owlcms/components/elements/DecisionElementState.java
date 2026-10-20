package app.owlcms.components.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.eventbus.Subscribe;

import app.owlcms.fieldofplay.AdjudicationPanel;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;

public class DecisionElementState {

	public enum DisplayMode {
		RESET,
		DOWN,
		DECISION
	}

	public interface IDecisionRenderer {
		void setEnabled(UIEvent event, boolean enabled);

		void resetDecisionDisplay(UIEvent event, long generation);

		void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
				boolean singleLight, boolean announcerForced);

		void showDownSignal(UIEvent.DownSignal event, boolean silent);

		default void showJuryLights(UIEvent event, List<String> lights, long generation) {
		}

		/**
		 * Seven-person Adjudication Panel review vote. States are "empty", "voted", "white" or "red".
		 *
		 * @param refereeStates referees 1..3
		 * @param juryStates    jury members 1..4
		 */
		default void showPanelVote(UIEvent event, List<String> refereeStates, List<String> juryStates,
		        long generation) {
		}
	}

	public record Snapshot(
			DisplayMode mode,
			Boolean decision,
			Boolean ref1,
			Boolean ref2,
			Boolean ref3,
			boolean singleLight,
			boolean announcerForced,
			boolean enabled,
			boolean showsDownSignal,
			boolean liveReferee,
			boolean resetOnClockStart,
			long generation) {
	}

	public static final long MINIMUM_DOWN_SIGNAL_VISIBLE_MS = 1500L;

	private final AtomicLong decisionDisplayGeneration = new AtomicLong();
	private final AtomicLong juryDisplayGeneration = new AtomicLong();
	private boolean showJuryDecisions;
	private final IDecisionRenderer renderer;
	private boolean dontReset;
	private boolean showsDownSignal = true;
	private boolean liveReferee;
	private boolean resetOnClockStart;
	private boolean silenced;
	private FieldOfPlay fop;
	private volatile Snapshot snapshot = new Snapshot(DisplayMode.RESET, null, null, null, null, false, false, false,
			true, false, false, 0L);

	public DecisionElementState(IDecisionRenderer renderer) {
		this.renderer = renderer;
	}

	public Snapshot snapshot() {
		return this.snapshot;
	}

	public void setDontReset(boolean dontReset) {
		this.dontReset = dontReset;
	}

	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
		syncJuryDecisions();
	}

	public void setShowJuryDecisions(boolean show) {
		if (this.showJuryDecisions == show) {
			return;
		}
		this.showJuryDecisions = show;
		if (show) {
			syncJuryDecisions();
		} else {
			publishJuryLights(null, List.of());
		}
	}

	public boolean isCurrentJuryGeneration(long generation) {
		return generation == this.juryDisplayGeneration.get();
	}

	private void syncJuryDecisions() {
		if (this.showJuryDecisions) {
			publishJuryLights(null, this.fop == null ? List.of()
					: juryLights(this.fop.getJuryMemberDecision(), this.fop.getJurySize()));
		}
	}

	@Subscribe
	public void slaveJuryUpdate(UIEvent.JuryUpdate e) {
		if (this.showJuryDecisions) {
			publishJuryLights(e, juryLights(e.getJuryMemberDecision(), e.getJurySize()));
		}
	}

	private void publishJuryLights(UIEvent event, List<String> lights) {
		this.renderer.showJuryLights(event, lights, this.juryDisplayGeneration.incrementAndGet());
	}

	private List<String> juryLights(Boolean[] votes, int size) {
		if (size < 2 || size > 5) {
			return List.of();
		}
		boolean complete = votes != null && votes.length >= size;
		for (int index = 0; complete && index < size; index++) {
			complete = votes[index] != null;
		}
		List<String> lights = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			Boolean vote = votes != null && index < votes.length ? votes[index] : null;
			lights.add(complete ? (Boolean.TRUE.equals(vote) ? "white" : "red")
					: (vote == null ? "empty" : "voted"));
		}
		return List.copyOf(lights);
	}

	public void setLiveReferee(boolean liveReferee) {
		this.liveReferee = liveReferee;
		refreshProfileSnapshot();
	}

	public void setResetOnClockStart(boolean resetOnClockStart) {
		this.resetOnClockStart = resetOnClockStart;
		refreshProfileSnapshot();
	}

	public void setShowsDownSignal(boolean showsDownSignal) {
		this.showsDownSignal = showsDownSignal;
		refreshProfileSnapshot();
	}

	public void setSilenced(boolean silenced) {
		this.silenced = silenced;
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		setEnabledSnapshot(true);
		this.renderer.setEnabled(e, true);
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		JuryDeliberationEventType type = e.getDeliberationEventType();
		if (type == JuryDeliberationEventType.START_DELIBERATION || type == JuryDeliberationEventType.CHALLENGE) {
			showPanelReviewInitialVotes(e);
		}
	}

	/**
	 * Seven-person Adjudication Panel: during the review break, show the Phase A votes that led to the review until
	 * the Chair calls for votes. Does nothing when there is no such pattern.
	 *
	 * @param e the triggering event, null when syncing a newly attached display
	 */
	public void showPanelReviewInitialVotes(UIEvent e) {
		Boolean[] votes = this.fop != null ? this.fop.getPanelReviewInitialVotes() : null;
		if (votes == null) {
			return;
		}
		boolean juryComplete = true;
		for (int i = AdjudicationPanel.NB_REFEREES; i < AdjudicationPanel.PANEL_SIZE; i++) {
			juryComplete = juryComplete && votes[i] != null;
		}
		List<String> states = new ArrayList<>(AdjudicationPanel.PANEL_SIZE);
		for (int i = 0; i < AdjudicationPanel.PANEL_SIZE; i++) {
			Boolean vote = votes[i];
			boolean revealed = i < AdjudicationPanel.NB_REFEREES || juryComplete;
			states.add(vote == null ? "empty" : !revealed ? "voted" : vote ? "white" : "red");
		}
		this.renderer.showPanelVote(e, List.copyOf(states.subList(0, AdjudicationPanel.NB_REFEREES)),
		        List.copyOf(states.subList(AdjudicationPanel.NB_REFEREES, AdjudicationPanel.PANEL_SIZE)),
		        nextGeneration());
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		if (this.resetOnClockStart) {
			return;
		}
		long generation = nextGeneration();
		if (this.dontReset) {
			return;
		}
		setResetSnapshot(generation);
		this.renderer.resetDecisionDisplay(e, generation);
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		if (!this.showsDownSignal) {
			return;
		}
		setDownSnapshot();
		this.renderer.showDownSignal(e, this.silenced || isEmitSoundsOnServer());
	}

	@Subscribe
	public void slaveInitialDecision(UIEvent.InitialDecision e) {
		// InitialDecision is an early notification for consumers such as video
		// animations. The FOP decides when the decision becomes visible and emits
		// UIEvent.Decision at that time; the decision element does not render this event.
	}

	/** A new review vote: the seven cells are cleared. */
	@Subscribe
	public void slavePanelVoteCalled(UIEvent.PanelVoteCalled e) {
		if (e.getRef() != 0) {
			return;
		}
		boolean[] voted = new boolean[AdjudicationPanel.PANEL_SIZE];
		publishPanelVote(e, voted, null);
	}

	/**
	 * Who has voted, then the seven votes revealed together. The attempt board reveals the vote as soon as it is
	 * complete, before the Jury Chair gives the rule; only the scoreboards wait for the decision.
	 */
	@Subscribe
	public void slavePanelVoteUpdate(UIEvent.PanelVoteUpdate e) {
		if (!e.isOpen() && !e.isComplete()) {
			// the vote was cancelled or abandoned
			resetForJury(e);
			return;
		}
		publishPanelVote(e, e.getVoted(), e.getVotes());
	}

	/** Resume, or a decision entered by the Jury Chair: whatever the jury had voted goes away. */
	@Subscribe
	public void slaveJuryDisplayReset(UIEvent.JuryDisplayReset e) {
		resetForJury(e);
	}

	private void resetForJury(UIEvent e) {
		long generation = nextGeneration();
		setResetSnapshot(generation);
		this.renderer.resetDecisionDisplay(e, generation);
	}

	private void publishPanelVote(UIEvent e, boolean[] voted, Boolean[] votes) {
		List<String> states = new ArrayList<>(AdjudicationPanel.PANEL_SIZE);
		for (int i = 0; i < AdjudicationPanel.PANEL_SIZE; i++) {
			Boolean vote = votes != null ? votes[i] : null;
			states.add(vote != null ? (vote ? "white" : "red") : (voted[i] ? "voted" : "empty"));
		}
		this.renderer.showPanelVote(e, List.copyOf(states.subList(0, AdjudicationPanel.NB_REFEREES)),
		        List.copyOf(states.subList(AdjudicationPanel.NB_REFEREES, AdjudicationPanel.PANEL_SIZE)),
		        nextGeneration());
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		if (!this.liveReferee) {
			return;
		}
		Boolean decision = e.singleLight ? e.ref2 : computeGoodLift(e.ref1, e.ref2, e.ref3, false);
		setDecisionSnapshot(decision, e.ref1, e.ref2, e.ref3, e.singleLight, false);
		this.renderer.showDecisionLights(e, decision, e.ref1, e.ref2, e.ref3, e.singleLight, false);
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		if (!this.resetOnClockStart) {
			return;
		}
		long generation = nextGeneration();
		if (this.dontReset) {
			return;
		}
		setResetSnapshot(generation);
		this.renderer.resetDecisionDisplay(e, generation);
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		// authoritative jury state for this display cycle; JuryUpdate events refine it
		if (this.showJuryDecisions) {
			publishJuryLights(e, this.fop == null ? List.of()
					: juryLights(this.fop.getJuryMemberDecision(), this.fop.getJurySize()));
		}
		boolean announcerForced = e.getInputKind() == InputKind.ANNOUNCER_ENTRY;
		setDecisionSnapshot(e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
		this.renderer.showDecisionLights(e, e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		long generation = nextGeneration();
		if (this.resetOnClockStart && !this.dontReset) {
			setResetSnapshot(generation);
			this.renderer.resetDecisionDisplay(e, generation);
		}
		setEnabledSnapshot(true);
		this.renderer.setEnabled(e, true);
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		setEnabledSnapshot(true);
		this.renderer.setEnabled(e, true);
	}

	public boolean isCurrentGeneration(long generation) {
		return generation == this.decisionDisplayGeneration.get();
	}

	private Boolean computeGoodLift(Boolean ref1, Boolean ref2, Boolean ref3, boolean singleLight) {
		int whites = 0;
		whites += Boolean.TRUE.equals(ref1) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref2) ? 1 : 0;
		whites += Boolean.TRUE.equals(ref3) ? 1 : 0;
		return singleLight ? whites >= 1 : whites >= 2;
	}

	private boolean isEmitSoundsOnServer() {
		return this.fop != null && this.fop.isEmitSoundsOnServer();
	}

	private long nextGeneration() {
		long generation = this.decisionDisplayGeneration.incrementAndGet();
		return generation;
	}

	private void setDecisionSnapshot(Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
			boolean singleLight, boolean announcerForced) {
		this.snapshot = new Snapshot(DisplayMode.DECISION, decision, ref1, ref2, ref3, singleLight, announcerForced,
				false, this.showsDownSignal, this.liveReferee, this.resetOnClockStart,
				this.decisionDisplayGeneration.get());
	}

	private void setDownSnapshot() {
		this.snapshot = new Snapshot(DisplayMode.DOWN, null, null, null, null, isSingleRef(), false,
				this.snapshot.enabled(), this.showsDownSignal, this.liveReferee, this.resetOnClockStart,
				this.decisionDisplayGeneration.get());
	}

	private void setEnabledSnapshot(boolean enabled) {
		Snapshot current = this.snapshot;
		this.snapshot = new Snapshot(current.mode(), current.decision(), current.ref1(), current.ref2(), current.ref3(),
				current.singleLight(), current.announcerForced(), enabled, this.showsDownSignal,
				this.liveReferee, this.resetOnClockStart, this.decisionDisplayGeneration.get());
	}

	private void setResetSnapshot(long generation) {
		this.snapshot = new Snapshot(DisplayMode.RESET, null, null, null, null, isSingleRef(), false, false,
				this.showsDownSignal, this.liveReferee, this.resetOnClockStart, generation);
	}

	private void refreshProfileSnapshot() {
		Snapshot current = this.snapshot;
		this.snapshot = new Snapshot(current.mode(), current.decision(), current.ref1(), current.ref2(), current.ref3(),
				current.singleLight(), current.announcerForced(), current.enabled(), this.showsDownSignal,
				this.liveReferee, this.resetOnClockStart, current.generation());
	}

	private boolean isSingleRef() {
		return this.fop != null && this.fop.isSingleReferee();
	}
}