package app.owlcms.access;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.account.RoleGrant;

/**
 * Pure access logic: no Vaadin types, no session, unit-tested. The one exception is
 * {@link Permission#isAvailable()}, which may read the configuration.
 */
public final class AccessPolicy {

	private AccessPolicy() {
	}

	/** Access rights effective on a platform; a null platform means platform-independent. */
	public static Set<Access> effectiveAccess(Principal principal, String platform) {
		Set<Access> result = EnumSet.noneOf(Access.class);
		if (principal == null) {
			return result;
		}
		for (RoleGrant grant : principal.grants()) {
			for (Access access : grant.getRole().accesses()) {
				if (scopeMatches(grant, access, platform) && lockMatches(principal, grant, access, platform)) {
					result.add(access);
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
			case ACCESS -> principal != null && canOpenWithAccess(principal, rule, targetPlatform);
		};
	}

	/**
	 * Platforms among {@code allPlatforms} on which the principal may operate a page needing one of the access rights.
	 */
	public static List<String> selectablePlatforms(Principal principal, Set<Access> pageAccess,
	        List<String> allPlatforms) {
		PageRule rule = PageRule.accesses(pageAccess, true);
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (canOpen(principal, rule, name)) {
				result.add(name);
			}
		}
		return result;
	}

	/** Real platforms on which the principal has at least one effective platform-scopable access right. */
	public static List<String> platformsWithAccess(Principal principal, List<String> allPlatforms) {
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (effectiveAccess(principal, name).stream().anyMatch(Access::isPlatformScopable)
			        && hasPlatformOperatingGrant(principal, name)) {
				result.add(name);
			}
		}
		return result;
	}

	/** Also false when the permission is not available in the configuration ({@link Permission#isAvailable()}). */
	public static boolean hasPermission(Principal principal, Permission permission, String platform) {
		return permission.isAvailable() && canOpen(principal, permission.asRule(), platform);
	}

	/** True for accounts that hold a lockable role and do not cover all platforms. */
	public static boolean requiresPlatformChoice(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT || hasUnrestrictedPlatforms(principal)) {
			return false;
		}
		return principal.grants().stream().anyMatch(g -> g.getRole().isLockable());
	}

	/**
	 * True when the principal is locked to the login platform and holds a lockable role that gives one of the page's
	 * access rights: the platform selector of such a page is read-only.
	 */
	public static boolean isLockedFor(Principal principal, Set<Access> pageAccess) {
		return isLocked(principal) && principal.grants().stream()
		        .anyMatch(g -> g.getRole().isLockable()
		                && g.getRole().accesses().stream().anyMatch(pageAccess::contains));
	}

	public static List<String> loginPlatformChoices(Principal principal, List<String> allPlatforms) {
		Set<String> result = new LinkedHashSet<>();
		if (principal == null) {
			return List.of();
		}
		for (RoleGrant grant : principal.grants()) {
			if (!grant.getRole().isLockable()) {
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

	private static boolean hasUnrestrictedPlatforms(Principal principal) {
		return principal != null && principal.grants().stream().anyMatch(grant -> grant.getRole().coversAllPlatforms());
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
			if (grant.getRole().isPlatformScopable() && !grant.getRole().isLockable()) {
				for (String name : allPlatforms) {
					if (grant.getPlatformName() == null || grant.getPlatformName().equals(name)) {
						result.add(name);
					}
				}
			}
		}
		return new ArrayList<>(result);
	}

	private static boolean canOpenWithAccess(Principal principal, PageRule rule, String targetPlatform) {
		for (RoleGrant grant : principal.grants()) {
			for (Access access : grant.getRole().accesses()) {
				if (!rule.accesses().contains(access)) {
					continue;
				}
				if (!rule.platformBound()) {
					return true;
				}
				if (scopeMatches(grant, access, targetPlatform)
				        && lockMatches(principal, grant, access, targetPlatform)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean scopeMatches(RoleGrant grant, Access access, String platform) {
		if (grant.getPlatformName() == null || !access.isPlatformScopable()) {
			return true;
		}
		return grant.getPlatformName().equals(platform);
	}

	/**
	 * Platform scope and login-platform locks come from operator roles (and the all-platform roles). A global role
	 * that merely carries a scopable access right, such as Preparation with athlete card editing, is neither scoped nor
	 * locked, and does not make the account choose a platform.
	 */
	private static boolean operatesOnPlatforms(Role role) {
		return role.isPlatformScopable() || role.coversAllPlatforms();
	}

	private static boolean hasPlatformOperatingGrant(Principal principal, String platform) {
		for (RoleGrant grant : principal.grants()) {
			if (operatesOnPlatforms(grant.getRole())) {
				for (Access access : grant.getRole().accesses()) {
					if (access.isPlatformScopable() && scopeMatches(grant, access, platform)
					        && lockMatches(principal, grant, access, platform)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean lockMatches(Principal principal, RoleGrant grant, Access access, String platform) {
		return platform == null || !grant.getRole().isLockable()
		        || principal.loginPlatform() == null || hasUnrestrictedPlatforms(principal)
		        || principal.loginPlatform().equals(platform);
	}
}
