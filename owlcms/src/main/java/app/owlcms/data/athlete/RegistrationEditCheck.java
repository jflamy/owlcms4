/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import app.owlcms.audit.AthleteDiff;
import app.owlcms.audit.AthleteDiff.Snapshot;
import app.owlcms.data.category.Participation;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * Registration merges the whole athlete, so even a change to hidden data makes the open form unsafe to save.
 */
public final class RegistrationEditCheck {
	private final Long athleteId;
	private final Snapshot baseline;

	private RegistrationEditCheck(Athlete athlete) {
		this.athleteId = athlete.getId();
		this.baseline = snapshot(athlete);
	}

	public static RegistrationEditCheck open(Athlete athlete) {
		return new RegistrationEditCheck(Objects.requireNonNull(athlete));
	}

	public Athlete loadCurrent() {
		Athlete current = AthleteRepository.findById(this.athleteId);
		if (current == null) {
			throw new IllegalStateException("Athlete no longer exists: " + this.athleteId);
		}
		return current;
	}

	public boolean isStale(Athlete current) {
		return !this.baseline.equals(snapshot(current));
	}

	public record SaveResult(FieldOfPlay lifting, boolean stale, boolean saved) {
	}

	public SaveResult trySave(Athlete formValues, BooleanSupplier active, Runnable save) {
		if (!active.getAsBoolean()) {
			return new SaveResult(null, false, false);
		}
		Athlete current = loadCurrent();
		FieldOfPlay lifting = OwlcmsFactory.getFOPSessionInProgress(formValues.getGroup(), current.getGroup());
		if (lifting != null) {
			return new SaveResult(lifting, false, false);
		}
		if (isStale(current)) {
			return new SaveResult(null, true, false);
		}
		if (!active.getAsBoolean()) {
			return new SaveResult(null, false, false);
		}
		save.run();
		return new SaveResult(null, false, true);
	}

	public static Snapshot snapshot(Athlete athlete) {
		Map<String, Object> values = new LinkedHashMap<>(AthleteDiff.snapshot(athlete).values());
		values.keySet().removeIf(key -> key.endsWith(".automatic") || key.equals("withdrawnFromSnatch")
		        || key.equals("withdrawnFromCleanJerk") || key.equals("eligibleCategories"));
		values.putAll(ConcurrentEditCheck.snapshot(athlete).values());
		values.put("session", athlete.getGroup() != null ? athlete.getGroup().getId() : null);
		values.put("category", athlete.getCategory() != null ? athlete.getCategory().getId() : null);
		values.put("isoBirthDate", athlete.getIsoBirthDate());
		values.put("scaleWeight", athlete.getScaleWeight());
		values.put("presumedBodyWeight", athlete.getPresumedBodyWeight());
		values.put("qualifyingTotal", athlete.getQualifyingTotal());
		values.put("subCategory", athlete.getSubCategory());
		values.put("individualEligibilityStatus", athlete.getIndividualEligibilityStatus());
		values.put("personalBestSnatch", athlete.getPersonalBestSnatch());
		values.put("personalBestCleanJerk", athlete.getPersonalBestCleanJerk());
		values.put("personalBestTotal", athlete.getPersonalBestTotal());
		values.put("participations", athlete.getParticipations().stream()
		        .map(ParticipationValues::new).sorted((a, b) -> a.categoryId().compareTo(b.categoryId())).toList());
		addAttempt(values, "snatch1", athlete.getSnatch1Decisions(), athlete.getSnatch1JuryDeliberation(),
		        athlete.getSnatch1Challenge(), athlete.getSnatch1LiftTime());
		addAttempt(values, "snatch2", athlete.getSnatch2Decisions(), athlete.getSnatch2JuryDeliberation(),
		        athlete.getSnatch2Challenge(), athlete.getSnatch2LiftTime());
		addAttempt(values, "snatch3", athlete.getSnatch3Decisions(), athlete.getSnatch3JuryDeliberation(),
		        athlete.getSnatch3Challenge(), athlete.getSnatch3LiftTime());
		addAttempt(values, "cleanJerk1", athlete.getCleanJerk1Decisions(), athlete.getCleanJerk1JuryDeliberation(),
		        athlete.getCleanJerk1Challenge(), athlete.getCleanJerk1LiftTime());
		addAttempt(values, "cleanJerk2", athlete.getCleanJerk2Decisions(), athlete.getCleanJerk2JuryDeliberation(),
		        athlete.getCleanJerk2Challenge(), athlete.getCleanJerk2LiftTime());
		addAttempt(values, "cleanJerk3", athlete.getCleanJerk3Decisions(), athlete.getCleanJerk3JuryDeliberation(),
		        athlete.getCleanJerk3Challenge(), athlete.getCleanJerk3LiftTime());
		return new Snapshot(values);
	}

	private static void addAttempt(Map<String, Object> values, String attempt, List<Boolean> decisions,
	        Boolean jury, Boolean challenge, LocalDateTime time) {
		values.put(attempt + ".decisions", decisions != null ? new ArrayList<>(decisions) : null);
		values.put(attempt + ".jury", jury);
		values.put(attempt + ".challenge", challenge);
		values.put(attempt + ".time", time);
	}

	private record ParticipationValues(Long categoryId, boolean teamMember, boolean mixedTeamMember) {
		ParticipationValues(Participation participation) {
			this(participation.getCategory().getId(), participation.isTeamMember(), participation.isMixedTeamMember());
		}
	}
}
