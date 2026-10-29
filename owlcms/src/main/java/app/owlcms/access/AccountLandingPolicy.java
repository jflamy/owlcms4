package app.owlcms.access;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Chooses a default destination from explicit role grants, not inherited permissions. */
public final class AccountLandingPolicy {

	public enum WorkPage {
		HOME, COMPETITION_SUPERVISOR, PREPARATION, REGISTRATION, WEIGHIN, RESULTS,
		LIFTING, ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY_CONTROL,
		REFEREE_NAVIGATION, JURY_NAVIGATION, DISPLAY_NAVIGATION
	}

	private AccountLandingPolicy() {
	}

	public static Optional<WorkPage> singleWorkPage(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT) {
			return Optional.empty();
		}
		Set<Role> explicitRoles = principal.grants().stream().map(grant -> grant.getRole()).collect(Collectors.toSet());
		if (explicitRoles.size() != 1) {
			return Optional.empty();
		}
		return Optional.of(defaultWorkPage(explicitRoles.iterator().next()));
	}

	private static WorkPage defaultWorkPage(Role role) {
		return switch (role) {
			case ADMIN, ADMIN_PAGES, EXTERNAL, STREAMING, COMPETITION_DIRECTOR -> WorkPage.HOME;
			case COMPETITION_SUPERVISOR -> WorkPage.COMPETITION_SUPERVISOR;
			case PREPARATION -> WorkPage.PREPARATION;
			case REGISTRATION -> WorkPage.REGISTRATION;
			case WEIGHIN -> WorkPage.WEIGHIN;
			case RESULTS -> WorkPage.RESULTS;
			case PLATFORM -> WorkPage.LIFTING;
			case ANNOUNCER -> WorkPage.ANNOUNCER;
			case MARSHAL -> WorkPage.MARSHAL;
			case TIMEKEEPER -> WorkPage.TIMEKEEPER;
			case TC -> WorkPage.TC;
			case JURY -> WorkPage.JURY_CONTROL;
			case REFEREE -> WorkPage.REFEREE_NAVIGATION;
			case DISPLAYS -> WorkPage.DISPLAY_NAVIGATION;
		};
	}
}