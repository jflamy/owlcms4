/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import app.owlcms.audit.AthleteDiff;
import app.owlcms.audit.AthleteDiff.Snapshot;

/**
 * Detects that saving an athlete card would overwrite changes saved by someone else (another card, or the field of
 * play) since the card was opened.
 *
 * The card writes back every field of the copy it loaded when opened, so any change made in between would be silently
 * lost. The card keeps one instance per opening and checks and saves under its platform's monitor.
 */
public final class ConcurrentEditCheck {

	/**
	 * Fields the card writes back from its copy, i.e. those transferred by
	 * {@link Athlete#conditionalCopy(Athlete, Athlete, boolean, boolean, boolean)}. Automatic progressions are left out
	 * since they are computed from the previous lift.
	 */
	public static final Set<String> FIELDS = cardFields();

	private final Long athleteId;
	private Snapshot baseline;

	private ConcurrentEditCheck(Long athleteId, Snapshot baseline) {
		this.athleteId = athleteId;
		this.baseline = baseline;
	}

	/**
	 * @param fromDb the athlete as just loaded from the database when the card is opened
	 * @return a check, or null if the athlete is not yet persisted
	 */
	public static ConcurrentEditCheck open(Athlete fromDb) {
		if (fromDb == null || fromDb.getId() == null) {
			return null;
		}
		return new ConcurrentEditCheck(fromDb.getId(), snapshot(fromDb));
	}

	/**
	 * Values of the athlete that the card can overwrite.
	 */
	public static Snapshot snapshot(Athlete athlete) {
		Map<String, Object> values = new LinkedHashMap<>(AthleteDiff.snapshot(athlete).values());
		if (athlete != null) {
			values.put("coach", athlete.getCoach());
			values.put("custom1", athlete.getCustom1());
			values.put("custom2", athlete.getCustom2());
			values.put("federationCodes", athlete.getFederationCodes());
			values.put("customScore", athlete.getCustomScore());
		}
		values.keySet().retainAll(FIELDS);
		return new Snapshot(values);
	}

	/**
	 * A field that was changed elsewhere since the card was opened and that the card would overwrite with a different
	 * value.
	 */
	public record Conflict(String field, Object whenOpened, Object current, Object inForm) {
	}

	/**
	 * Fields whose stored value changed since the card was opened and whose card value differs from the stored one.
	 * Fields where the user has re-entered the stored value are not conflicts.
	 */
	public static List<Conflict> conflicts(Snapshot whenOpened, Snapshot current, Snapshot inForm) {
		List<Conflict> conflicts = new ArrayList<>();
		for (Map.Entry<String, Object> entry : current.values().entrySet()) {
			String field = entry.getKey();
			Object stored = entry.getValue();
			Object opened = whenOpened.values().get(field);
			Object form = inForm.values().get(field);
			if (!Objects.equals(opened, stored) && !Objects.equals(form, stored)) {
				conflicts.add(new Conflict(field, opened, stored, form));
			}
		}
		return conflicts;
	}

	/**
	 * An unsuccessful attempt retains the exact stored snapshot to which Save anyway may consent.
	 */
	public record SaveAttempt(Snapshot current, List<Conflict> conflicts, boolean saved) {
	}

	/**
	 * The save callback contains persistence/FOP work only, and returns after the database commit.
	 * No UI work or user confirmation may run under the monitor.
	 */
	public SaveAttempt trySave(Object monitor, Athlete formValues, Snapshot accepted, BooleanSupplier active,
	        Runnable save) {
		Snapshot reference = accepted != null ? accepted : this.baseline;
		SaveAttempt result = trySave(monitor, reference, snapshot(formValues), () -> {
			Athlete current = AthleteRepository.findById(this.athleteId);
			if (current == null) {
				throw new IllegalStateException("Athlete no longer exists: " + this.athleteId);
			}
			return snapshot(current);
		}, active, save);
		if (result.saved()) {
			this.baseline = snapshot(formValues);
		} else if (accepted != null && !result.conflicts().isEmpty()) {
			Map<String, Conflict> displayed = new LinkedHashMap<>();
			for (Conflict conflict : conflicts(this.baseline, result.current(), snapshot(formValues))) {
				displayed.put(conflict.field(), conflict);
			}
			// Also retain newly conflicting values that reverted to their opening value after confirmation.
			for (Conflict conflict : result.conflicts()) {
				displayed.putIfAbsent(conflict.field(), new Conflict(conflict.field(),
				        this.baseline.values().get(conflict.field()), conflict.current(), conflict.inForm()));
			}
			return new SaveAttempt(result.current(), List.copyOf(displayed.values()), false);
		}
		return result;
	}

	static SaveAttempt trySave(Object monitor, Snapshot reference, Snapshot formValues, Supplier<Snapshot> load,
	        BooleanSupplier active, Runnable save) {
		Objects.requireNonNull(monitor);
		synchronized (monitor) {
			if (!active.getAsBoolean()) {
				return new SaveAttempt(null, List.of(), false);
			}
			Snapshot current = load.get();
			List<Conflict> conflicts = conflicts(reference, current, formValues);
			if (!conflicts.isEmpty()) {
				return new SaveAttempt(current, List.copyOf(conflicts), false);
			}
			if (!active.getAsBoolean()) {
				return new SaveAttempt(current, List.of(), false);
			}
			save.run();
			return new SaveAttempt(current, List.of(), true);
		}
	}

	private static Set<String> cardFields() {
		Set<String> fields = new HashSet<>(Set.of("lastName", "firstName", "birthDate", "birthYear", "session",
		        "category", "startNumber", "lotNumber", "entryTotal", "bodyWeight", "forcedAsCurrent", "coach",
		        "custom1", "custom2", "federationCodes", "customScore"));
		for (String attempt : List.of("snatch1", "snatch2", "snatch3", "cleanJerk1", "cleanJerk2", "cleanJerk3")) {
			for (String part : List.of("declaration", "change1", "change2", "actualLift")) {
				fields.add(attempt + "." + part);
			}
		}
		return Set.copyOf(fields);
	}
}
