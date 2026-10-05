package app.owlcms.data.athleteSort;

import app.owlcms.data.athlete.Gender;
import app.owlcms.spreadsheet.PAthlete;

/** Assigns ranks to an already sorted population, counting each athlete once. */
final class BestAthleteRankSetter {
	private Gender previousGender;
	private Long previousAthleteId;
	private int nextRank;
	private int previousRank;

	void assign(PAthlete athlete, boolean eligible, double score) {
		if (athlete.getGender() != previousGender) {
			nextRank = 0;
			previousAthleteId = null;
		}
		int rank = athlete.getId().equals(previousAthleteId) ? previousRank
		        : !eligible ? -1 : score > 0 ? ++nextRank : 0;
		athlete.setBestAthleteRank(rank);
		previousGender = athlete.getGender();
		previousAthleteId = athlete.getId();
		previousRank = rank;
	}
}
