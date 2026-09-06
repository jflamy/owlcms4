package app.owlcms.data.agegroup;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum MedalPolicy {

	ALL_THREE,
	TOTAL_ONLY,
	LIFTS_ONLY;

	@JsonCreator
	public static MedalPolicy fromValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static MedalPolicy effective(MedalPolicy configured, boolean legacyLiftMedals) {
		return configured != null ? configured : legacyLiftMedals ? ALL_THREE : TOTAL_ONLY;
	}

	public boolean includesSnatchAndCleanJerk() {
		return this != TOTAL_ONLY;
	}

	public boolean includesTotal() {
		return this != LIFTS_ONLY;
	}

	public String labelKey() {
		return "Championship.TeamPointsPolicy." + name();
	}

	public boolean isMedalist(int snatchRank, int cleanJerkRank, int overallRank) {
		return (includesSnatchAndCleanJerk() && (isMedalRank(snatchRank) || isMedalRank(cleanJerkRank)))
		        || (includesTotal() && isMedalRank(overallRank));
	}

	private boolean isMedalRank(int rank) {
		return rank >= 1 && rank <= 3;
	}
}