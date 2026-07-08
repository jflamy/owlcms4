package app.owlcms.components.elements;

import java.util.concurrent.atomic.AtomicLong;

import com.google.common.eventbus.Subscribe;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.InputKind;
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
		boolean announcerForced = e.getInputKind() == InputKind.ANNOUNCER_ENTRY;
		setDecisionSnapshot(e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
		this.renderer.showDecisionLights(e, e.decision, e.ref1, e.ref2, e.ref3, e.isSingleLight(), announcerForced);
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		nextGeneration();
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