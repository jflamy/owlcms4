package app.owlcms.data.agegroup;

import java.util.Locale;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TeamPointsPolicy {

	ALL_THREE,
	TOTAL_ONLY,
	LIFTS_ONLY;

	@JsonCreator
	public static TeamPointsPolicy fromValue(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
	}

	public static TeamPointsPolicy effective(TeamPointsPolicy configured, boolean threeMedals) {
		return effective(configured, MedalPolicy.effective(null, threeMedals));
	}

	public static TeamPointsPolicy effective(TeamPointsPolicy configured, MedalPolicy medals) {
		if (medals == MedalPolicy.TOTAL_ONLY) {
			return TOTAL_ONLY;
		}
		if (medals == MedalPolicy.LIFTS_ONLY) {
			return configured != null ? configured : LIFTS_ONLY;
		}
		return configured != null ? configured : ALL_THREE;
	}

	public static List<TeamPointsPolicy> allowedFor(MedalPolicy medals) {
		return switch (medals) {
		case TOTAL_ONLY -> List.of(TOTAL_ONLY);
		case LIFTS_ONLY -> List.of(values());
		case ALL_THREE -> List.of(values());
		};
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
}