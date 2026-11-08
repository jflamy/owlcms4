package app.owlcms.data.athlete;

import java.util.Objects;

/**
 * The six columns owned by metadata editing. Lifting saves preserve these values without normalizing legacy rows.
 */
public record AthleteMetadata(String firstName, String lastName, String team, String federationCodes,
        boolean eligibleForIndividualRanking, EligibleForIndividualRankingStatus individualEligibilityStatus) {

	public static AthleteMetadata from(Athlete athlete) {
		return Objects.requireNonNull(athlete).metadata();
	}

	public void copyTo(Athlete athlete) {
		Objects.requireNonNull(athlete).applyMetadata(this);
	}
}
