package app.owlcms.access;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Chooses a direct platform work-page landing only when it is unambiguous. */
public final class AccountLandingPolicy {

	public enum WorkPage {
		ANNOUNCER(Role.ANNOUNCER),
		MARSHAL(Role.MARSHAL),
		TIMEKEEPER(Role.TIMEKEEPER),
		TC(Role.TC),
		JURY_CONSOLE(Role.JURY),
		REFEREE_NAVIGATION(Role.JURY, Role.REFEREE),
		JURY_NAVIGATION(Role.JURY);

		private final Set<Role> roles;

		WorkPage(Role... roles) {
			this.roles = Set.of(roles);
		}

		boolean isOpenTo(Principal principal) {
			return AccessPolicy.canOpen(principal, PageRule.roles(this.roles, true), principal.loginPlatform());
		}
	}

	private AccountLandingPolicy() {
	}

	public static Optional<WorkPage> singleWorkPage(Principal principal) {
		if (!AccessPolicy.isLocked(principal)) {
			return Optional.empty();
		}
		List<WorkPage> candidates = new ArrayList<>();
		for (WorkPage page : WorkPage.values()) {
			if (page.isOpenTo(principal)) {
				candidates.add(page);
			}
		}
		return candidates.size() == 1 ? Optional.of(candidates.get(0)) : Optional.empty();
	}
}