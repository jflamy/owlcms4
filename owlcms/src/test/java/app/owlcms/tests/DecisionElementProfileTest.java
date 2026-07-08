package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.eventbus.EventBus;

import app.owlcms.components.elements.DecisionElementState;
import app.owlcms.components.elements.DecisionElementState.DisplayMode;
import app.owlcms.components.elements.DecisionElementState.IDecisionRenderer;
import app.owlcms.fieldofplay.InputKind;
import app.owlcms.fieldofplay.TimingPolicy;
import app.owlcms.uievents.UIEvent;

public class DecisionElementProfileTest {

	@Test
	public void defaultProfileRendersDownAndDecisionOnly() {
		// @formatter:off
		// Expected renderer behavior:
		// ResetOnNewClock is ignored
		// RefereeUpdate is ignored
		// InitialDecision is ignored
		// DownSignal renders DOWN
		// Decision renders final decision
		// DecisionReset renders reset
		// @formatter:on
		ProfileHarness profile = new ProfileHarness();

		profile.post(resetOnNewClock());
		assertEquals(0, profile.renderer.resetCount);

		profile.post(refereeUpdate(true, false, null));
		assertEquals(0, profile.renderer.decisionCount);

		profile.post(initialDecision(true));
		assertEquals(0, profile.renderer.decisionCount);

		profile.post(downSignal());
		assertEquals(1, profile.renderer.downCount);
		assertEquals(DisplayMode.DOWN, profile.state.snapshot().mode());

		profile.post(decision(true));
		assertEquals(1, profile.renderer.decisionCount);
		assertEquals(Boolean.TRUE, profile.renderer.lastDecision);
		assertEquals(DisplayMode.DECISION, profile.state.snapshot().mode());

		profile.post(decisionReset());
		assertEquals(1, profile.renderer.resetCount);
		assertEquals(DisplayMode.RESET, profile.state.snapshot().mode());
	}

	@Test
	public void liveProfileRendersRefereeUpdatesAndResetsOnNewClock() {
		// @formatter:off
		// Expected renderer behavior:
		// DownSignal is ignored
		// ResetOnNewClock renders reset
		// RefereeUpdate renders live referee state
		// InitialDecision is ignored
		// Decision renders final decision
		// @formatter:on
		ProfileHarness profile = new ProfileHarness();
		profile.state.setShowsDownSignal(false);
		profile.state.setLiveReferee(true);
		profile.state.setResetOnClockStart(true);

		profile.post(downSignal());
		assertEquals(0, profile.renderer.downCount);

		profile.post(resetOnNewClock());
		assertEquals(1, profile.renderer.resetCount);
		assertEquals(1, profile.renderer.resetOnNewClockCount);
		assertEquals(DisplayMode.RESET, profile.state.snapshot().mode());

		profile.post(refereeUpdate(true, false, null));
		assertEquals(1, profile.renderer.decisionCount);
		assertEquals(Boolean.FALSE, profile.renderer.lastDecision);
		assertEquals(Boolean.TRUE, profile.renderer.lastRef1);
		assertEquals(Boolean.FALSE, profile.renderer.lastRef2);

		profile.post(initialDecision(true));
		assertEquals(1, profile.renderer.decisionCount);

		profile.post(decision(true));
		assertEquals(2, profile.renderer.decisionCount);
		assertEquals(Boolean.TRUE, profile.renderer.lastDecision);
	}

	@Test
	public void decisionSectionProfileRendersDecisionOnlyAndResetsOnNewClock() {
		// @formatter:off
		// Expected renderer behavior:
		// DownSignal is ignored
		// RefereeUpdate is ignored
		// ResetOnNewClock clears the display for the next clock
		// InitialDecision is ignored
		// Decision renders final decision
		// DecisionReset is ignored; the decision section waits for ResetOnNewClock
		// @formatter:on
		ProfileHarness profile = new ProfileHarness();
		profile.state.setShowsDownSignal(false);
		profile.state.setLiveReferee(false);
		profile.state.setResetOnClockStart(true);

		profile.post(downSignal());
		assertEquals(0, profile.renderer.downCount);

		profile.post(refereeUpdate(true, false, null));
		assertEquals(0, profile.renderer.decisionCount);

		profile.post(resetOnNewClock());
		assertEquals(1, profile.renderer.resetCount);
		assertEquals(1, profile.renderer.resetOnNewClockCount);
		assertEquals(0, profile.renderer.decisionResetCount);

		profile.post(initialDecision(true));
		assertEquals(0, profile.renderer.decisionCount);

		profile.post(decision(false));
		assertEquals(1, profile.renderer.decisionCount);
		assertEquals(Boolean.FALSE, profile.renderer.lastDecision);

		profile.post(decisionReset());
		assertEquals(1, profile.renderer.resetCount);
		assertEquals(0, profile.renderer.decisionResetCount);
	}

	private static UIEvent.Decision decision(boolean goodLift) {
		return new UIEvent.Decision(null, goodLift, true, true, true, DecisionElementProfileTest.class, null, false,
				TimingPolicy.IMMEDIATE, InputKind.THREE_REFEREE_INPUT);
	}

	private static UIEvent.DecisionReset decisionReset() {
		return new UIEvent.DecisionReset(null, DecisionElementProfileTest.class, null);
	}

	private static UIEvent.DownSignal downSignal() {
		return new UIEvent.DownSignal(DecisionElementProfileTest.class, null);
	}

	private static UIEvent.InitialDecision initialDecision(boolean goodLift) {
		return new UIEvent.InitialDecision(null, goodLift, true, true, true, DecisionElementProfileTest.class, null,
				false, TimingPolicy.IMMEDIATE, InputKind.THREE_REFEREE_INPUT);
	}

	private static UIEvent.RefereeUpdate refereeUpdate(Boolean ref1, Boolean ref2, Boolean ref3) {
		return new UIEvent.RefereeUpdate(null, ref1, ref2, ref3, 100L, 200L, 300L, DecisionElementProfileTest.class,
				false, null);
	}

	private static UIEvent.ResetOnNewClock resetOnNewClock() {
		return new UIEvent.ResetOnNewClock(null, DecisionElementProfileTest.class, null);
	}

	private static final class ProfileHarness {
		private final EventBus eventBus = new EventBus();
		private final RecordingRenderer renderer = new RecordingRenderer();
		private final DecisionElementState state = new DecisionElementState(this.renderer);

		private ProfileHarness() {
			this.eventBus.register(this.state);
		}

		private void post(UIEvent event) {
			this.eventBus.post(event);
		}
	}

	private static final class RecordingRenderer implements IDecisionRenderer {
		private int decisionCount;
		private int decisionResetCount;
		private int downCount;
		private int resetCount;
		private int resetOnNewClockCount;
		private Boolean lastDecision;
		private Boolean lastRef1;
		private Boolean lastRef2;

		@Override
		public void resetDecisionDisplay(UIEvent event, long generation) {
			this.resetCount++;
			if (event instanceof UIEvent.DecisionReset) {
				this.decisionResetCount++;
			} else if (event instanceof UIEvent.ResetOnNewClock) {
				this.resetOnNewClockCount++;
			}
		}

		@Override
		public void setEnabled(UIEvent event, boolean enabled) {
		}

		@Override
		public void showDecisionLights(UIEvent event, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3,
				boolean singleLight, boolean announcerForced) {
			this.decisionCount++;
			this.lastDecision = decision;
			this.lastRef1 = ref1;
			this.lastRef2 = ref2;
		}

		@Override
		public void showDownSignal(UIEvent.DownSignal event, boolean silent) {
			this.downCount++;
		}
	}
}