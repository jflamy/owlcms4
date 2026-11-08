/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

/**
 * The registration side becomes restricted during a session. Registration/Supervisor/results may still offer the
 * independent Card once lifting has started in the athlete's session. Decisions use stored athletes, not the grid's
 * session filter.
 */
public final class RegistrationTabPolicy {

	/** What the page that opens the form wants for the lifting card. */
	public enum CardEntry {
		/** No Card tab (team selection and weigh-in). */
		NONE,
		/** Card after lifting starts; keep the registration/information side selected first (registration page). */
		AVAILABLE,
		/** Card after lifting starts, and selected first (supervisor, results pages). */
		CARD_FIRST
	}

	public enum Tab {
		REGISTRATION, INFORMATION, CARD
	}

	/**
	 * @param restrictedRegistration only safe information fields may replace the normal registration tabs
	 * @param cardTab      the independent Card is offered by the entry page after lifting starts
	 * @param initialTab   the tab selected when the form opens
	 */
	public record Layout(boolean restrictedRegistration, boolean cardTab, Tab initialTab) {
	}

	private RegistrationTabPolicy() {
	}

	/** A new athlete: the registration tabs alone. */
	public static Layout forNewAthlete() {
		return new Layout(false, false, Tab.REGISTRATION);
	}

	/**
	 * @param entry  what the opening page asked for
	 * @param stored the athlete as freshly loaded, not a grid row
	 */
	public static Layout choose(CardEntry entry, Athlete stored) {
		boolean restricted = RegistrationEditCheck.requiresMetadataForm(stored);
		boolean results = stored.getAttemptsDone() != null && stored.getAttemptsDone() > 0;
		boolean sessionStarted = restricted || results;
		if (entry != CardEntry.NONE && !sessionStarted && stored.getGroup() != null
		        && stored.getGroup().getId() != null) {
			sessionStarted = AthleteRepository.findAllByGroupAndWeighIn(stored.getGroup(), null).stream()
			        .anyMatch(a -> a.getAttemptsDone() != null && a.getAttemptsDone() > 0);
		}
		boolean card = entry != CardEntry.NONE && sessionStarted;
		Tab initial = card && entry == CardEntry.CARD_FIRST ? Tab.CARD
		        : restricted ? Tab.INFORMATION : Tab.REGISTRATION;
		return new Layout(restricted, card, initial);
	}
}
