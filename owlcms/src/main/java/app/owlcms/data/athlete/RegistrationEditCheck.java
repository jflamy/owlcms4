/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import app.owlcms.audit.AthleteDiff;
import app.owlcms.audit.AthleteDiff.Snapshot;
import app.owlcms.data.category.Participation;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;

/**
 * Registration owns its form fields, not the Card's results or adjudication evidence.
 */
public final class RegistrationEditCheck {
	private final Long athleteId;
	private Snapshot baseline;
	private String snatchDeclaration;
	private String cleanJerkDeclaration;

	private RegistrationEditCheck(Athlete athlete) {
		this.athleteId = athlete.getId();
		rebase(athlete);
	}

	public static RegistrationEditCheck open(Athlete athlete) {
		return new RegistrationEditCheck(Objects.requireNonNull(athlete));
	}

	/** Use the stored athlete's session, not the grid's session filter or a retained row copy. */
	public static boolean requiresMetadataForm(Athlete stored) {
		return OwlcmsFactory.getFOPSessionInProgress(stored.getGroup()) != null;
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

	public record SaveResult(FieldOfPlay lifting, boolean stale, boolean saved, Athlete athlete) {
	}

	public SaveResult trySave(Athlete formValues, BooleanSupplier active, Function<Athlete, Athlete> save) {
		if (!active.getAsBoolean()) {
			return new SaveResult(null, false, false, null);
		}
		Athlete current = loadCurrent();
		FieldOfPlay fop = OwlcmsFactory.getFOPWithGroup(current.getGroup());
		synchronized (fop != null ? fop : ConcurrentEditCheck.class) {
			current = loadCurrent();
			if (OwlcmsFactory.getFOPWithGroup(current.getGroup()) != fop) {
				throw new IllegalStateException("Platform selection changed while saving athlete " + this.athleteId);
			}
			FieldOfPlay lifting = OwlcmsFactory.getFOPSessionInProgress(formValues.getGroup(), current.getGroup());
			if (lifting != null) {
				return new SaveResult(lifting, false, false, null);
			}
			if (isStale(current)
			        || conflicts(this.snatchDeclaration, current.getSnatch1Declaration(), formValues.getSnatch1Declaration())
			        || conflicts(this.cleanJerkDeclaration, current.getCleanJerk1Declaration(), formValues.getCleanJerk1Declaration())) {
				return new SaveResult(null, true, false, null);
			}
			if (!active.getAsBoolean()) {
				return new SaveResult(null, false, false, null);
			}
			AthleteRegistrationValues.copyTo(formValues, current);
			current.setValidation(false);
			if (!Objects.equals(this.snatchDeclaration, formValues.getSnatch1Declaration())) {
				current.setSnatch1Declaration(formValues.getSnatch1Declaration());
			}
			if (!Objects.equals(this.cleanJerkDeclaration, formValues.getCleanJerk1Declaration())) {
				current.setCleanJerk1Declaration(formValues.getCleanJerk1Declaration());
			}
			current.setValidation(formValues.isValidation());
			Athlete saved = Objects.requireNonNull(save.apply(current), "Registration save result");
			rebase(saved);
			return new SaveResult(null, false, true, saved);
		}
	}

	private void rebase(Athlete saved) {
		this.baseline = snapshot(saved);
		this.snatchDeclaration = saved.getSnatch1Declaration();
		this.cleanJerkDeclaration = saved.getCleanJerk1Declaration();
	}

	private static boolean conflicts(String opened, String stored, String form) {
		return !Objects.equals(opened, form) && !Objects.equals(opened, stored) && !Objects.equals(stored, form);
	}

	public static Snapshot snapshot(Athlete athlete) {
		Map<String, Object> values = new LinkedHashMap<>(AthleteDiff.snapshot(athlete).values());
		values.keySet().removeIf(key -> key.contains(".") || key.equals("withdrawnFromSnatch")
		        || key.equals("withdrawnFromCleanJerk") || key.equals("eligibleCategories")
		        || key.equals("forcedAsCurrent") || key.equals("customScore"));
		values.put("coach", athlete.getCoach());
		values.put("custom1", athlete.getCustom1());
		values.put("custom2", athlete.getCustom2());
		values.put("session", athlete.getGroup() != null ? athlete.getGroup().getId() : null);
		values.put("category", athlete.getCategory() != null ? athlete.getCategory().getId() : null);
		values.put("isoBirthDate", athlete.getIsoBirthDate());
		values.put("scaleWeight", athlete.getScaleWeight());
		values.put("presumedBodyWeight", athlete.getPresumedBodyWeight());
		values.put("qualifyingTotal", athlete.getQualifyingTotal());
		values.put("subCategory", athlete.getSubCategory());
		values.put("individualEligibilityStatus", athlete.getIndividualEligibilityStatus());
		// the registration form writes all six metadata values, so it is stale when any of them (or the stamp) moved
		values.put("metadata", AthleteMetadata.from(athlete));
		values.put("metadataUpdatedAt", athlete.getMetadataUpdatedAt());
		values.put("personalBestSnatch", athlete.getPersonalBestSnatch());
		values.put("personalBestCleanJerk", athlete.getPersonalBestCleanJerk());
		values.put("personalBestTotal", athlete.getPersonalBestTotal());
		values.put("participations", athlete.getParticipations().stream()
		        .map(ParticipationValues::new).sorted((a, b) -> a.categoryId().compareTo(b.categoryId())).toList());
		return new Snapshot(values);
	}

	private record ParticipationValues(Long categoryId, boolean teamMember, boolean mixedTeamMember) {
		ParticipationValues(Participation participation) {
			this(participation.getCategory().getId(), participation.isTeamMember(), participation.isMixedTeamMember());
		}
	}
}
