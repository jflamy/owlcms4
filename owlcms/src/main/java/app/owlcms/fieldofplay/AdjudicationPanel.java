/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

/**
 * Tally of the seven-person Adjudication Panel (IWF TCRR 2026, 7.5.7): three referees and four jury members.
 *
 * Pure logic, no UI. A vote is {@code TRUE} for a good lift (white), {@code FALSE} for a no lift (red), and
 * {@code null} when the official has not voted.
 *
 * <b>No early result</b>: the majority is only reported when all seven votes are present, so that nothing can be
 * inferred about the votes still to come. {@link #isJuryComplete()} tells when the jury part is complete (the
 * referees' initial decision is already known when the fourth juror votes).
 */
public final class AdjudicationPanel {

	public static final int NB_REFEREES = 3;
	public static final int NB_JURORS = 4;
	public static final int PANEL_SIZE = NB_REFEREES + NB_JURORS;
	private static final int MAJORITY = PANEL_SIZE / 2 + 1;

	private final Boolean[] referees = new Boolean[NB_REFEREES];
	private final Boolean[] jury = new Boolean[NB_JURORS];

	/**
	 * @param refereeVotes the referee votes; only the first 3 are used, may be null
	 * @param juryVotes    the jury votes; only the first 4 are used (the FOP array may be larger), may be null
	 */
	public AdjudicationPanel(Boolean[] refereeVotes, Boolean[] juryVotes) {
		if (refereeVotes != null) {
			System.arraycopy(refereeVotes, 0, this.referees, 0, Math.min(NB_REFEREES, refereeVotes.length));
		}
		if (juryVotes != null) {
			System.arraycopy(juryVotes, 0, this.jury, 0, Math.min(NB_JURORS, juryVotes.length));
		}
	}

	/**
	 * Majority of the three referee votes (the initial decision).
	 *
	 * @return TRUE or FALSE when two votes agree, null otherwise
	 */
	public static Boolean refereeMajority(Boolean[] refereeVotes) {
		if (refereeVotes == null) {
			return null;
		}
		int nbRed = 0;
		int nbWhite = 0;
		for (int i = 0; i < Math.min(NB_REFEREES, refereeVotes.length); i++) {
			if (Boolean.TRUE.equals(refereeVotes[i])) {
				nbWhite++;
			} else if (Boolean.FALSE.equals(refereeVotes[i])) {
				nbRed++;
			}
		}
		if (nbWhite >= 2) {
			return Boolean.TRUE;
		}
		if (nbRed >= 2) {
			return Boolean.FALSE;
		}
		return null;
	}

	/**
	 * @return true if the panel majority is known and opposite to the referee majority.
	 */
	public boolean differsFrom(Boolean refereeMajority) {
		Boolean panelMajority = majority();
		return panelMajority != null && refereeMajority != null && !panelMajority.equals(refereeMajority);
	}

	/** @return true if all seven votes are present */
	public boolean isComplete() {
		return votesReceived() == PANEL_SIZE;
	}

	/** @return true if all four jury votes are present */
	public boolean isJuryComplete() {
		return count(this.jury, null) == 0;
	}

	/**
	 * @return TRUE (good lift) or FALSE (no lift) when all seven votes are present, null otherwise. With seven voters
	 *         there cannot be a tie. A 2-2 jury split always leaves the referee majority as the panel majority.
	 */
	public Boolean majority() {
		if (!isComplete()) {
			return null;
		}
		return whites() >= MAJORITY;
	}

	public int reds() {
		return count(this.referees, Boolean.FALSE) + count(this.jury, Boolean.FALSE);
	}

	public int votesReceived() {
		return PANEL_SIZE - count(this.referees, null) - count(this.jury, null);
	}

	public int whites() {
		return count(this.referees, Boolean.TRUE) + count(this.jury, Boolean.TRUE);
	}

	private static int count(Boolean[] votes, Boolean value) {
		int n = 0;
		for (Boolean vote : votes) {
			if (value == null ? vote == null : value.equals(vote)) {
				n++;
			}
		}
		return n;
	}
}
