/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

import app.owlcms.fieldofplay.FieldOfPlay;

/**
 * The label that the displays show during a jury or challenge break. A review started automatically is a Review (the
 * seven-person Adjudication Panel, or a jury with the automatic stop option); one started by the Chair keeps the name
 * of its button (Jury Deliberation or Challenge). Once the Chair calls the vote, the label becomes the Vote of the
 * panel or of the jury; a vote that opens by itself when the Chair starts the review does not change the label.
 */
public final class ReviewLabels {

	public static final String CHALLENGE = "PublicMsg.CHALLENGE";
	public static final String JURY_DELIBERATION = "PublicMsg.JuryDeliberation";
	public static final String PANEL_REVIEW = "PublicMsg.AdjudicationPanelReview";
	public static final String PANEL_VOTE = "PublicMsg.AdjudicationPanelVote";
	public static final String JURY_REVIEW = "PublicMsg.JuryReview";
	public static final String JURY_VOTE = "PublicMsg.JuryVote";

	private ReviewLabels() {
	}

	/**
	 * @param breakType JURY or CHALLENGE; other break types are not reviews
	 * @param fop       the platform under review; null gives the label of a review started by the Chair
	 * @return the translation key of the label, null when the break is not a review
	 */
	public static String key(BreakType breakType, FieldOfPlay fop) {
		if (breakType != BreakType.JURY && breakType != BreakType.CHALLENGE) {
			return null;
		}
		if (fop != null && fop.isReviewVoteCalled()) {
			return fop.isPanelProcedure() ? PANEL_VOTE : JURY_VOTE;
		}
		if (breakType == BreakType.CHALLENGE) {
			return CHALLENGE;
		}
		if (fop != null && fop.isPanelReview()) {
			return PANEL_REVIEW;
		}
		if (fop != null && fop.isJuryReview()) {
			return JURY_REVIEW;
		}
		return JURY_DELIBERATION;
	}
}
