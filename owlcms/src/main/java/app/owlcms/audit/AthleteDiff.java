package app.owlcms.audit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Collectors;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Participation;

public final class AthleteDiff {
	private AthleteDiff() {
	}

	public static Snapshot snapshot(Athlete athlete) {
		Map<String, Object> values = new LinkedHashMap<>();
		if (athlete == null) {
			return new Snapshot(values);
		}
		values.put("lastName", athlete.getLastName());
		values.put("firstName", athlete.getFirstName());
		values.put("team", athlete.getTeam());
		values.put("gender", athlete.getGender());
		values.put("birthDate", athlete.getFullBirthDate());
		values.put("birthYear", athlete.getYearOfBirth());
		values.put("membership", athlete.getMembership());
		values.put("session", athlete.getGroup() != null ? athlete.getGroup().getName() : null);
		values.put("category", athlete.getCategory() != null ? athlete.getCategory().getCode() : null);
		values.put("eligibleCategories", athlete.getEligibleCategories() == null ? null
				: new TreeSet<>(athlete.getEligibleCategories().stream().map(category -> category.getCode()).toList()));
		values.put("participations", participations(athlete.getParticipations()));
		values.put("eligibleForIndividualRanking", athlete.isEligibleForIndividualRanking());
		values.put("eligibleForTeamRanking", athlete.isEligibleForTeamRanking());
		values.put("startNumber", athlete.getStartNumber());
		values.put("lotNumber", athlete.getLotNumber());
		values.put("entryTotal", athlete.getEntryTotal());
		values.put("bodyWeight", athlete.getBodyWeight());
		values.put("withdrawnFromSnatch", athlete.withdrawnFromSnatch());
		values.put("withdrawnFromCleanJerk", athlete.withdrawnFromCJ());
		values.put("forcedAsCurrent", athlete.isForcedAsCurrent());
		addAttempt(values, "snatch1", athlete.getSnatch1AutomaticProgression(), athlete.getSnatch1Declaration(),
				athlete.getSnatch1Change1(), athlete.getSnatch1Change2(), athlete.getSnatch1ActualLift());
		addAttempt(values, "snatch2", athlete.getSnatch2AutomaticProgression(), athlete.getSnatch2Declaration(),
				athlete.getSnatch2Change1(), athlete.getSnatch2Change2(), athlete.getSnatch2ActualLift());
		addAttempt(values, "snatch3", athlete.getSnatch3AutomaticProgression(), athlete.getSnatch3Declaration(),
				athlete.getSnatch3Change1(), athlete.getSnatch3Change2(), athlete.getSnatch3ActualLift());
		addAttempt(values, "cleanJerk1", athlete.getCleanJerk1AutomaticProgression(), athlete.getCleanJerk1Declaration(),
				athlete.getCleanJerk1Change1(), athlete.getCleanJerk1Change2(), athlete.getCleanJerk1ActualLift());
		addAttempt(values, "cleanJerk2", athlete.getCleanJerk2AutomaticProgression(), athlete.getCleanJerk2Declaration(),
				athlete.getCleanJerk2Change1(), athlete.getCleanJerk2Change2(), athlete.getCleanJerk2ActualLift());
		addAttempt(values, "cleanJerk3", athlete.getCleanJerk3AutomaticProgression(), athlete.getCleanJerk3Declaration(),
				athlete.getCleanJerk3Change1(), athlete.getCleanJerk3Change2(), athlete.getCleanJerk3ActualLift());
		return new Snapshot(values);
	}

	public static List<Change> diff(Snapshot before, Snapshot after) {
		List<Change> changes = new ArrayList<>();
		for (Map.Entry<String, Object> entry : after.values.entrySet()) {
			Object oldValue = before.values.get(entry.getKey());
			if (!Objects.equals(oldValue, entry.getValue())) {
				changes.add(new Change(entry.getKey(), oldValue, entry.getValue(), attempt(entry.getKey())));
			}
		}
		return changes;
	}

	private static void addAttempt(Map<String, Object> values, String prefix, Object automatic, Object declaration,
			Object change1, Object change2, Object actualLift) {
		values.put(prefix + ".automatic", automatic);
		values.put(prefix + ".declaration", declaration);
		values.put(prefix + ".change1", change1);
		values.put(prefix + ".change2", change2);
		values.put(prefix + ".actualLift", actualLift);
	}

	private static String participations(List<Participation> participations) {
		if (participations == null) {
			return null;
		}
		return participations.stream()
				.map(participation -> participation.getCategory().getCode() + ":team=" + participation.isTeamMember()
						+ ":mixed=" + participation.isMixedTeamMember())
				.sorted().collect(Collectors.joining(","));
	}

	private static String attempt(String field) {
		if (field.startsWith("snatch")) {
			return "SN" + field.charAt(6);
		}
		if (field.startsWith("cleanJerk")) {
			return "CJ" + field.charAt(9);
		}
		return null;
	}

	public record Snapshot(Map<String, Object> values) {
	}

	public record Change(String field, Object oldValue, Object newValue, String attempt) {
	}
}