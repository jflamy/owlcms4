/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsFactory;

/**
 * Evaluates whether a session is ready to be introduced or started, based on current weighed-in athletes rather than
 * stale completion markers or in-memory platform state. Clearing a final attempt can reopen a completed session.
 * Pure non-UI logic so it can be unit tested directly.
 */
public final class SessionReadiness {

	public enum Status {
		NO_SESSION,
		DONE,
		NOT_READY,
		READY
	}

	public record Evaluation(Status status, Group group, boolean needsReload) {
		public boolean isReady() {
			return this.status == Status.READY;
		}
	}

	/** Why a session proposed in the selection dialog can no longer be chosen. */
	public enum Unavailability {
		NONE,
		SELECTED_ELSEWHERE,
		FINISHED,
		NOT_READY
	}

	/**
	 * @param holder the platform holding the session when {@code reason} is {@code SELECTED_ELSEWHERE}, else null
	 */
	public record Availability(Unavailability reason, FieldOfPlay holder) {
		public boolean isAvailable() {
			return this.reason == Unavailability.NONE;
		}
	}

	private SessionReadiness() {
	}

	public static Evaluation evaluate(FieldOfPlay fop) {
		if (fop == null) {
			return new Evaluation(Status.NO_SESSION, null, false);
		}
		Group group = fop.getGroup();
		if (group == null) {
			return new Evaluation(Status.NO_SESSION, null, false);
		}
		List<Athlete> weighedIn = loadWeighedInAthletes(group);
		return evaluate(group, weighedIn, fop.getCurAthlete());
	}

	public static Evaluation evaluate(Group group) {
		if (group == null) {
			return new Evaluation(Status.NO_SESSION, null, false);
		}
		return evaluate(group, loadWeighedInAthletes(group), null);
	}

	public static Evaluation evaluate(Group group, List<Athlete> weighedInAthletes, Athlete loadedCurAthlete) {
		if (group == null) {
			return new Evaluation(Status.NO_SESSION, null, false);
		}
		List<Athlete> eligible = weighedInAthletes == null ? List.of() : weighedInAthletes.stream()
		        .filter(Objects::nonNull)
		        .filter(a -> a.getCategory() != null)
		        .filter(a -> a.getBodyWeight() != null && a.getBodyWeight() > 0.1)
		        .toList();
		if (eligible.isEmpty()) {
			return new Evaluation(Status.NOT_READY, group, false);
		}
		boolean allDone = eligible.stream().allMatch(Athlete::isDone);
		if (allDone) {
			return new Evaluation(Status.DONE, group, false);
		}
		boolean staleCurAthlete = loadedCurAthlete == null || loadedCurAthlete.getAttemptsDone() >= 6;
		// a session under way is not vetted further: blank declarations (e.g. C&J weights still being entered
		// during the C&J break) must not block the break, and reloading would bring back the same order
		boolean liftingStarted = eligible.stream().anyMatch(a -> a.getAttemptsDone() > 0);
		if (liftingStarted) {
			return new Evaluation(Status.READY, group, staleCurAthlete);
		}
		boolean anyReady = eligible.stream().anyMatch(SessionReadiness::hasReadyLift);
		if (!anyReady) {
			return new Evaluation(Status.NOT_READY, group, false);
		}
		boolean needsReload = staleCurAthlete || !hasPositiveNextWeight(loadedCurAthlete);
		return new Evaluation(Status.READY, group, needsReload);
	}

	public static boolean hasReadyLift(Athlete athlete) {
		if (athlete == null || athlete.isDone()) {
			return false;
		}
		if (athlete.declaresSnatchWithdrawal() && athlete.declaresCleanJerkWithdrawal()) {
			return false;
		}
		if (athlete.declaresSnatchWithdrawal()) {
			Integer cj1 = athlete.getRequestedWeightForAttempt(4);
			return cj1 != null && cj1 >= 2;
		}
		return hasPositiveNextWeight(athlete);
	}

	public static List<Group> findReadySessions(FieldOfPlay fop) {
		List<Group> ready = new ArrayList<>();
		for (Group candidate : GroupRepository.findAll()) {
			if (availabilityFor(fop, candidate).isAvailable()) {
				ready.add(candidate);
			}
		}
		String currentPlatformName = fop != null ? fop.getName() : null;
		return orderCandidates(ready, currentPlatformName);
	}

	/**
	 * Whether {@code fop} can switch to {@code candidate}, and if not, why. Checked when building the selection list
	 * and again on confirmation, since the session may have been selected elsewhere, finished, or lost its ready
	 * athletes while the dialog was open.
	 */
	public static Availability availabilityFor(FieldOfPlay fop, Group candidate) {
		if (candidate == null) {
			return new Availability(Unavailability.NOT_READY, null);
		}
		FieldOfPlay activeFop = OwlcmsFactory.getFOPWithGroup(candidate);
		if (activeFop != null && activeFop != fop) {
			return new Availability(Unavailability.SELECTED_ELSEWHERE, activeFop);
		}
		return switch (evaluate(candidate).status()) {
			case READY -> new Availability(Unavailability.NONE, null);
			case DONE -> new Availability(Unavailability.FINISHED, null);
			default -> new Availability(Unavailability.NOT_READY, null);
		};
	}

	public static List<Group> orderCandidates(List<Group> candidates, String currentPlatformName) {
		return SessionSelectionOrder.order(candidates, currentPlatformName, g -> false);
	}

	public static Comparator<Group> candidateComparator(String currentPlatformName) {
		return SessionSelectionOrder.candidateComparator(currentPlatformName);
	}

	public static boolean isOnPlatform(Group group, String platformName) {
		return SessionSelectionOrder.isOnPlatform(group, platformName);
	}

	private static boolean hasPositiveNextWeight(Athlete athlete) {
		Integer nextWeight = athlete.getNextAttemptRequestedWeight();
		return nextWeight != null && nextWeight > 0;
	}

	private static List<Athlete> loadWeighedInAthletes(Group group) {
		return AthleteRepository.findAllByGroupAndWeighIn(group, true).stream()
		        .filter(a -> a.getCategory() != null)
		        .toList();
	}
}
