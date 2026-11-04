package app.owlcms.audit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.category.Category;

/**
 * Age group definition changes and the athlete reassignments they caused, one sealed block per committed action.
 * Values are codes and numbers, never translated text.
 */
public final class AgeGroupAudit {
	private static final String COMPETITION = "competition";

	private AgeGroupAudit() {
	}

	/** Plain values of an age group; take it inside the transaction, compare after commit. */
	public static Map<String, Object> snapshot(AgeGroup ageGroup) {
		Map<String, Object> values = new LinkedHashMap<>();
		values.put("code", ageGroup.getCode());
		values.put("gender", ageGroup.getGender() != null ? ageGroup.getGender().name() : null);
		values.put("championship", ageGroup.getChampionshipName());
		values.put("minAge", ageGroup.getMinAge());
		values.put("maxAge", ageGroup.getMaxAge());
		values.put("active", ageGroup.isActive());
		values.put("medals", ageGroup.getMedals());
		values.put("alreadyGendered", ageGroup.isAlreadyGendered());
		List<Category> categories = ageGroup.getCategories();
		values.put("categories", categories.stream().map(Category::getCode).collect(Collectors.joining(" ")));
		values.put("qualifyingTotals", categories.stream().filter(c -> c.getQualifyingTotal() > 0)
				.map(c -> c.getCode() + "=" + c.getQualifyingTotal()).collect(Collectors.joining(" ")));
		return values;
	}

	public static void added(AuditOrigin origin, Map<String, Object> after) {
		write(origin, List.of(entry(origin, "agegroup.add").detail(kvs(after)).build()), List.of());
	}

	/** @param decision APPLY or LATER for a change that reassigns athletes, else null */
	public static void changed(AuditOrigin origin, Map<String, Object> before, Map<String, Object> after,
			String decision, List<AthleteAudit.Changed> athletes) {
		write(origin, changes(origin, before, after, decision), athletes);
	}

	public static void deleted(AuditOrigin origin, Map<String, Object> before, List<AthleteAudit.Changed> athletes) {
		write(origin, List.of(entry(origin, "agegroup.delete").detail(kvs(before)).build()), athletes);
	}

	/** Several age groups moved to another championship in one transaction. */
	public static void moved(AuditOrigin origin, List<Map<String, Object>> before, List<Map<String, Object>> after) {
		List<AuditEntry> entries = new ArrayList<>();
		for (int i = 0; i < before.size(); i++) {
			entries.addAll(changes(origin, before.get(i), after.get(i), null));
		}
		write(origin, entries, List.of());
	}

	/** @param action agegroup.reassign-within or agegroup.recompute-all */
	public static void reassigned(AuditOrigin origin, String action, List<AthleteAudit.Changed> athletes) {
		write(origin, List.of(entry(origin, action).detail(AuditFormat.kv("athletes", athletes.size())).build()),
				athletes);
	}

	public static void championshipRenamed(AuditOrigin origin, String oldName, String newName,
			List<String> ageGroups) {
		write(origin, List.of(entry(origin, "championship.rename").field("name").oldValue(oldName).newValue(newName)
				.detail(AuditFormat.kv("ageGroups", String.join(" ", ageGroups))).build()), List.of());
	}

	/** All age groups replaced from a definitions file, and the athletes whose categories changed as a result. */
	public static void definitionsLoaded(AuditOrigin origin, String source, long ageGroups,
			List<AthleteAudit.Changed> athletes) {
		write(origin, List.of(entry(origin, "agegroup.definitions-load").detail(AuditFormat.kvs("source", source,
				"ageGroups", ageGroups, "athletes", athletes.size())).build()), athletes);
	}

	private static List<AuditEntry> changes(AuditOrigin origin, Map<String, Object> before,
			Map<String, Object> after, String decision) {
		String identity = AuditFormat.kvs("ageGroup", before.get("code"), "gender", before.get("gender"),
				"championship", before.get("championship"));
		String detail = decision == null ? identity : identity + "," + AuditFormat.kv("decision", decision);
		List<AuditEntry> entries = new ArrayList<>();
		for (Map.Entry<String, Object> e : after.entrySet()) {
			Object oldValue = before.get(e.getKey());
			if (!Objects.equals(oldValue, e.getValue())) {
				entries.add(entry(origin, "agegroup.change").field(e.getKey()).oldValue(oldValue)
						.newValue(e.getValue()).detail(detail).build());
			}
		}
		return entries;
	}

	private static AuditEntry.Builder entry(AuditOrigin origin, String action) {
		return AuditEntry.builder(COMPETITION, action).actor(origin.actor()).cause(origin.cause());
	}

	private static String kvs(Map<String, Object> values) {
		return values.entrySet().stream().map(e -> AuditFormat.kv(e.getKey(), e.getValue()))
				.collect(Collectors.joining(","));
	}

	/** One sealed block per platform stream: the age group entries and competition athletes, then each platform. */
	private static void write(AuditOrigin origin, List<AuditEntry> ageGroupEntries,
			List<AthleteAudit.Changed> athletes) {
		Map<String, List<AuditEntry>> byPlatform = new LinkedHashMap<>();
		byPlatform.put(COMPETITION, new ArrayList<>(ageGroupEntries));
		for (AthleteAudit.Changed changed : athletes) {
			List<AuditEntry> entries = byPlatform.computeIfAbsent(changed.platform(), k -> new ArrayList<>());
			for (AthleteDiff.Change change : changed.changes()) {
				entries.add(AuditEntry.builder(changed.platform(), "athlete.change").actor(origin.actor())
						.cause(origin.cause()).athlete(changed.athlete()).field(change.field())
						.oldValue(change.oldValue()).newValue(change.newValue()).build());
			}
		}
		for (List<AuditEntry> entries : byPlatform.values()) {
			AuditLog.writeBatch(entries, true);
		}
	}
}
