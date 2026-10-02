package app.owlcms.access;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.account.RoleGrant;

/** Pure access logic: no Vaadin types, no session, unit-tested. */
public final class AccessPolicy {

	private AccessPolicy() {
	}

	/** Roles effective on a platform; a null platform means platform-independent. */
	public static Set<Role> expandedRoles(Principal principal, String platform) {
		Set<Role> result = EnumSet.noneOf(Role.class);
		if (principal == null) {
			return result;
		}
		for (RoleGrant grant : principal.grants()) {
			for (Role role : grant.getRole().expand()) {
				if (scopeMatches(grant, role, platform) && lockMatches(principal, role, platform)) {
					result.add(role);
				}
			}
		}
		return result;
	}

	public static boolean canOpen(Principal principal, PageRule rule, String targetPlatform) {
		return switch (rule.kind()) {
			case PUBLIC -> true;
			case NONE -> false;
			case AUTHENTICATED -> principal != null;
			case ROLES -> principal != null && canOpenWithRoles(principal, rule, targetPlatform);
		};
	}

	/** Platforms among {@code allPlatforms} on which the principal may operate a page needing one of the roles. */
	public static List<String> selectablePlatforms(Principal principal, Set<Role> pageRoles,
	        List<String> allPlatforms) {
		PageRule rule = PageRule.roles(pageRoles, true);
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (canOpen(principal, rule, name)) {
				result.add(name);
			}
		}
		return result;
	}

	/** Real platforms on which the principal has at least one effective platform-scopable role. */
	public static List<String> platformsWithAccess(Principal principal, List<String> allPlatforms) {
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (expandedRoles(principal, name).stream().anyMatch(Role::isPlatformScopable)) {
				result.add(name);
			}
		}
		return result;
	}

	public static boolean hasCapability(Principal principal, Capability capability, String platform) {
		return canOpen(principal, capability.asRule(), platform);
	}

	/** True for accounts that hold a lockable role and are not administrators. */
	public static boolean requiresPlatformChoice(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT || hasUnrestrictedPlatforms(principal)) {
			return false;
		}
		return principal.grants().stream().anyMatch(g -> g.getRole().expand().stream().anyMatch(Role::isLockable));
	}

	public static List<String> loginPlatformChoices(Principal principal, List<String> allPlatforms) {
		Set<String> result = new LinkedHashSet<>();
		if (principal == null) {
			return List.of();
		}
		for (RoleGrant grant : principal.grants()) {
			boolean lockable = grant.getRole().expand().stream().anyMatch(Role::isLockable);
			if (!lockable) {
				continue;
			}
			for (String name : allPlatforms) {
				if (grant.getPlatformName() == null || grant.getPlatformName().equals(name)) {
					result.add(name);
				}
			}
		}
		return new ArrayList<>(result);
	}

	public static boolean isAdmin(Principal principal) {
		return principal != null && principal.grants().stream().anyMatch(g -> g.getRole() == Role.ADMIN);
	}

	private static boolean hasUnrestrictedPlatforms(Principal principal) {
		return isAdmin(principal) || principal != null
		        && principal.grants().stream().anyMatch(grant -> grant.getRole() == Role.COMPETITION_DIRECTOR);
	}

	public static boolean isLocked(Principal principal) {
		return principal != null && principal.loginPlatform() != null && !hasUnrestrictedPlatforms(principal);
	}

	/** Platforms the session may switch to: everything unless locked. */
	public static List<String> allowedSessionPlatforms(Principal principal, List<String> allPlatforms) {
		if (!isLocked(principal)) {
			return allPlatforms;
		}
		Set<String> result = new LinkedHashSet<>();
		result.add(principal.loginPlatform());
		for (RoleGrant grant : principal.grants()) {
			for (Role role : grant.getRole().expand()) {
				if (role.isPlatformScopable() && !role.isLockable()) {
					for (String name : allPlatforms) {
						if (grant.getPlatformName() == null || grant.getPlatformName().equals(name)) {
							result.add(name);
						}
					}
				}
			}
		}
		return new ArrayList<>(result);
	}

	private static boolean canOpenWithRoles(Principal principal, PageRule rule, String targetPlatform) {
		for (RoleGrant grant : principal.grants()) {
			for (Role role : grant.getRole().expand()) {
				if (!rule.roles().contains(role)) {
					continue;
				}
				if (!rule.platformBound()) {
					return true;
				}
				if (scopeMatches(grant, role, targetPlatform) && lockMatches(principal, role, targetPlatform)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean scopeMatches(RoleGrant grant, Role role, String platform) {
		if (grant.getPlatformName() == null || !role.isPlatformScopable()) {
			return true;
		}
		return grant.getPlatformName().equals(platform);
	}

	private static boolean lockMatches(Principal principal, Role role, String platform) {
		return platform == null || !role.isLockable() || principal.loginPlatform() == null || hasUnrestrictedPlatforms(principal)
		        || principal.loginPlatform().equals(platform);
	}
}
