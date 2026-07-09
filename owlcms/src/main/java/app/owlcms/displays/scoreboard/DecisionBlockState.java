/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.Arrays;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.uievents.JuryDeliberationEventType;

/**
 * Single source of truth for what the bottom decision section of the results scoreboard shows.
 *
 * <p>
 * This is plain Java (no Vaadin) so it can be unit-tested against FOP event scenarios. It owns the
 * display state and drives a {@link DecisionSectionRenderer}; {@link Results} implements that
 * renderer and simply paints whatever the state machine asks for. See
 * {@code DecisionBlock_SPEC.md} in this package for the behavioral specification.
 * </p>
 *
 * <p>
 * The three states, their visibility, and the event transitions are documented in the spec. In
 * short:
 * </p>
 * <ul>
 * <li>{@code READY} — clock visible, referee lights empty, no jury circles, no label.</li>
 * <li>{@code DECISION} — athlete-under-review name, referee lights, jury circles (voted then
 * disclosed). When there is a jury the section waits (no timeout) for the jury to vote; when there
 * is no jury a review timeout bounds the display. An athlete clock start or a break wins in either
 * case and returns to ready.</li>
 * <li>{@code DELIBERATION} — athlete-under-review name, referee lights, jury circles only when the
 * second-vote-public toggle is on, deliberation/challenge label (replaced by the verdict label
 * once the jury delivers a good/no-lift verdict).</li>
 * </ul>
 */
public class DecisionBlockState {

	public enum State {
		READY,
		DECISION,
		DELIBERATION
	}

	/**
	 * Referee lights already resolved for single-referee and timing rules. {@code good} is the
	 * overall decision; {@code ref1/ref2/ref3} are the individual lights (null when not shown).
	 */
	public record RefLights(Boolean good, Boolean ref1, Boolean ref2, Boolean ref3, boolean singleRef,
	        boolean announcerForced) {
	}

	/**
	 * Rendering contract for the decision section only. Implemented by {@link Results}, which owns
	 * the {@code @Id} handles to the decision-section elements. This interface does not govern the
	 * rest of the scoreboard (grid, group info, ranks).
	 */
	public interface DecisionSectionRenderer {
		/** Ready state: decision-section clock visible, athlete-under-review name hidden. */
		void renderReadyClock();

		/** Show the athlete under review; the name replaces the clock. */
		void renderAthleteUnderReview(Athlete athlete);

		/** Draw the referee decision lights. */
		void renderRefereeLights(RefLights lights);

		/** Render the referee slot as empty (blank slots, no colors). */
		void renderEmptyRefereeLights();

		/** Jury circles: empty then voted (gray) then disclosed (white/red) once all have voted. */
		void renderJuryCircles(Boolean[] votes, int jurySize);

		/** Remove the jury circles entirely (not even empty placeholders). */
		void renderNoJuryCircles();

		/** Deliberation / challenge label. */
		void renderJuryMessage(JuryDeliberationEventType type);

		/** Jury verdict label (good / no lift), matching the attempt board wording. */
		void renderJuryVerdict(boolean good);

		/** Clear the deliberation / challenge / verdict label. */
		void renderNoJuryMessage();

		/** Start the review timeout that returns the block to ready when the jury takes no action. */
		void scheduleReviewTimeout();

		/** Cancel the review timeout. */
		void cancelReviewTimeout();
	}

	private final DecisionSectionRenderer renderer;
	private State state = State.READY;
	private boolean showBothJuryVotes;
	private int jurySize;
	private Athlete reviewedAthlete;
	private RefLights refLights;
	private Boolean[] juryVotes;
	private JuryDeliberationEventType deliberationType;
	private Boolean juryVerdictGood;

	public DecisionBlockState(DecisionSectionRenderer renderer) {
		this.renderer = renderer;
	}

	public State getState() {
		return this.state;
	}

	public void setShowBothJuryVotes(boolean showBothJuryVotes) {
		this.showBothJuryVotes = showBothJuryVotes;
	}

	public void setJurySize(int jurySize) {
		this.jurySize = jurySize;
	}

	// ------------------------------------------------------------------------
	// Events (called by Results' @Subscribe handlers, or directly by tests)
	// ------------------------------------------------------------------------

	/**
	 * The referee decision has become visible for the current athlete (all referees voted and the
	 * down signal has been shown per the immediate/delayed rules). Ignored during a deliberation:
	 * the reviewed decision is controlled by the deliberation itself.
	 */
	public void onRefereeDecision(Athlete athlete, RefLights lights) {
		if (this.state == State.DELIBERATION) {
			return;
		}
		this.reviewedAthlete = athlete;
		this.refLights = lights;
		this.state = State.DECISION;
		render();
	}

	/**
	 * A jury member vote arrived. Circles update in place (they do not restart the review timeout).
	 * Ignored in READY, and ignored during deliberation unless the second vote is public.
	 */
	public void onJuryUpdate(Boolean[] votes, int jurySize) {
		this.juryVotes = copyOf(votes);
		this.jurySize = jurySize;
		switch (this.state) {
			case DECISION:
				this.renderer.renderJuryCircles(this.juryVotes, this.jurySize);
				break;
			case DELIBERATION:
				if (this.showBothJuryVotes) {
					this.renderer.renderJuryCircles(this.juryVotes, this.jurySize);
				}
				break;
			case READY:
			default:
				break;
		}
	}

	/** The review timeout expired: keeping the previous athlete visible would confuse the audience. */
	public void onReviewTimeout() {
		if (this.state == State.DECISION) {
			toReady();
		}
	}

	/** A jury deliberation or challenge started. Shows the athlete under review and clears circles. */
	public void onDeliberationStart(JuryDeliberationEventType type, Athlete athleteUnderReview, RefLights reviewedLights) {
		this.deliberationType = type;
		this.reviewedAthlete = athleteUnderReview;
		this.refLights = reviewedLights;
		this.juryVotes = null;
		this.juryVerdictGood = null;
		this.state = State.DELIBERATION;
		render();
	}

	/**
	 * A jury verdict (good/bad lift) was posted. The block stays in deliberation but now shows the
	 * verdict label (matching the attempt board wording) instead of the deliberation label.
	 * <p>
	 * To stay in step with the attempt board, we only paint the verdict once it is actually shown
	 * there: the first notification (a jury button press awaiting the announcer,
	 * {@code waitForAnnouncer == true}) is ignored, exactly as the attempt board ignores it; we paint
	 * the verdict on the confirming notification ({@code waitForAnnouncer == false}), which the
	 * announcer's button press — or the automatic outcome — produces.
	 * <p>
	 * It returns to ready only when the announcer resumes competition ({@link #onStartLifting()}); in
	 * the automatic mode the FOP posts start-lifting on its own, which returns the block to ready
	 * through the same path.
	 */
	public void onJuryVerdict(boolean good, boolean waitForAnnouncer) {
		if (this.state != State.DELIBERATION) {
			return;
		}
		if (waitForAnnouncer) {
			// The attempt board has not shown the verdict yet (it is waiting for the announcer);
			// keep showing the deliberation label until the confirming notification arrives.
			return;
		}
		this.juryVerdictGood = good;
		render();
	}

	/** End of the jury break / challenge. We still wait for start-lifting before returning to ready. */
	public void onEndBreak() {
		// Intentionally no state change: wait for start-lifting.
	}

	/** The announcer resumed competition (or the automatic mode resumed it). Return to ready. */
	public void onStartLifting() {
		toReady();
	}

	/**
	 * The athlete clock started (the announcer announced the next athlete and started time). This
	 * wins over the review window: the decision section returns to ready-for-next-athlete. (A break
	 * timer starting has the same effect through {@link #onBreakStarted(boolean)}.)
	 */
	public void onStartTime() {
		if (this.state != State.READY) {
			toReady();
		}
	}

	/** A new clock was declared for the next athlete/attempt. Ignored: only a clock <em>start</em> wins. */
	public void onResetOnNewClock() {
		// Intentionally no state change: announcing the next athlete does not close the section;
		// only starting the clock (onStartTime) or a break does.
	}

	/**
	 * The FOP cleared its (short) referee-decision display window. Ignored: the decision section
	 * lingers longer than the FOP flash so the jury has time to review and vote. The section
	 * returns to ready via the review timeout, a new referee decision, a jury deliberation, a
	 * non-jury break, or the announcer resuming from a break.
	 */
	public void onDecisionReset() {
		// Intentionally no state change: the review window owns when we return to ready.
	}

	/**
	 * A break started. Jury/challenge breaks are driven by {@link #onDeliberationStart} and ignored
	 * here; any other break returns the decision section to its ready (clock) presentation.
	 */
	public void onBreakStarted(boolean juryOrChallenge) {
		if (juryOrChallenge) {
			return;
		}
		toReady();
	}

	// ------------------------------------------------------------------------
	// Rendering
	// ------------------------------------------------------------------------

	private void toReady() {
		this.state = State.READY;
		this.reviewedAthlete = null;
		this.refLights = null;
		this.juryVotes = null;
		this.deliberationType = null;
		this.juryVerdictGood = null;
		render();
	}

	/** Fully paint the decision section from the current state. Single rendering authority. */
	private void render() {
		switch (this.state) {
			case READY:
				this.renderer.cancelReviewTimeout();
				this.renderer.renderReadyClock();
				this.renderer.renderEmptyRefereeLights();
				this.renderer.renderNoJuryCircles();
				this.renderer.renderNoJuryMessage();
				break;
			case DECISION:
				this.renderer.renderAthleteUnderReview(this.reviewedAthlete);
				renderRefereeLightsOrEmpty();
				if (this.jurySize > 0) {
					// A jury must vote, so we wait: never start a review timeout. The section is
					// closed by the announcer starting the next clock, a break, a deliberation, or
					// start-lifting.
					this.renderer.renderJuryCircles(this.juryVotes, this.jurySize);
				} else {
					// No jury: nobody has to vote, so bound the display with the review timeout.
					this.renderer.renderNoJuryCircles();
					this.renderer.scheduleReviewTimeout();
				}
				this.renderer.renderNoJuryMessage();
				break;
			case DELIBERATION:
				this.renderer.cancelReviewTimeout();
				this.renderer.renderAthleteUnderReview(this.reviewedAthlete);
				renderRefereeLightsOrEmpty();
				if (this.showBothJuryVotes) {
					this.renderer.renderJuryCircles(this.juryVotes, this.jurySize);
				} else {
					this.renderer.renderNoJuryCircles();
				}
				if (this.juryVerdictGood != null) {
					this.renderer.renderJuryVerdict(this.juryVerdictGood);
				} else {
					this.renderer.renderJuryMessage(this.deliberationType);
				}
				break;
			default:
				break;
		}
	}

	private void renderRefereeLightsOrEmpty() {
		if (this.refLights != null) {
			this.renderer.renderRefereeLights(this.refLights);
		} else {
			this.renderer.renderEmptyRefereeLights();
		}
	}

	private static Boolean[] copyOf(Boolean[] votes) {
		return votes == null ? null : Arrays.copyOf(votes, votes.length);
	}
}
