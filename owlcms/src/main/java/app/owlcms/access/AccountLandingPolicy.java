package app.owlcms.access;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Chooses a default destination from explicit role grants, not inherited access. */
public final class AccountLandingPolicy {

	public enum WorkPage {
		HOME, COMPETITION_SUPERVISOR, PREPARATION, REGISTRATION, WEIGHIN, RESULTS,
		LIFTING, ANNOUNCER, SPEAKER, MARSHAL, TIMEKEEPER, TC, JURY_CONTROL,
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
			case Role.ADMIN, Role.EXTERNAL, Role.STREAMING, Role.COMPETITION_DIRECTOR, Role.PLATFORM -> WorkPage.HOME;
			case Role.COMPETITION_SUPERVISOR -> WorkPage.COMPETITION_SUPERVISOR;
			case Role.PREPARATION -> WorkPage.PREPARATION;
			case Role.REGISTRATION -> WorkPage.REGISTRATION;
			case Role.WEIGHIN -> WorkPage.WEIGHIN;
			case Role.RESULTS -> WorkPage.RESULTS;
			case Role.ANNOUNCER -> WorkPage.ANNOUNCER;
			case Role.SPEAKER -> WorkPage.SPEAKER;
			case Role.MARSHAL -> WorkPage.MARSHAL;
			case Role.TIMEKEEPER -> WorkPage.TIMEKEEPER;
			case Role.TC -> WorkPage.TC;
			case Role.JURY -> WorkPage.JURY_CONTROL;
			case Role.REFEREE -> WorkPage.REFEREE_NAVIGATION;
			case Role.DISPLAYS -> WorkPage.DISPLAY_NAVIGATION;
		};
	}
}