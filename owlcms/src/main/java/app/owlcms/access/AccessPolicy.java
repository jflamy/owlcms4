package app.owlcms.access;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import app.owlcms.data.account.RoleGrant;

/**
 * Pure access logic: no Vaadin types, no session, unit-tested. The one exception is
 * {@link Capability#isAvailable()}, which may read the configuration.
 */
public final class AccessPolicy {

	private AccessPolicy() {
	}

	/** permissions effective on a platform; a null platform means platform-independent. */
	public static Set<Permission> effectivePermissions(Principal principal, String platform) {
		Set<Permission> result = EnumSet.noneOf(Permission.class);
		if (principal == null) {
			return result;
		}
		for (RoleGrant grant : principal.grants()) {
			for (Permission permission : grant.getRole().permissions()) {
				if (scopeMatches(grant, permission, platform) && lockMatches(principal, grant, permission, platform)) {
					result.add(permission);
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
			case PERMISSIONS -> principal != null && canOpenWithPermissions(principal, rule, targetPlatform);
		};
	}

	/**
	 * Platforms among {@code allPlatforms} on which the principal may operate a page needing one of the permissions.
	 */
	public static List<String> selectablePlatforms(Principal principal, Set<Permission> pagePermissions,
	        List<String> allPlatforms) {
		PageRule rule = PageRule.permissions(pagePermissions, true);
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (canOpen(principal, rule, name)) {
				result.add(name);
			}
		}
		return result;
	}

	/** Real platforms on which the principal has at least one effective platform-scopable permission. */
	public static List<String> platformsWithAccess(Principal principal, List<String> allPlatforms) {
		List<String> result = new ArrayList<>();
		for (String name : allPlatforms) {
			if (effectivePermissions(principal, name).stream().anyMatch(Permission::isPlatformScopable)
			        && hasPlatformOperatingGrant(principal, name)) {
				result.add(name);
			}
		}
		return result;
	}

	/** Also false when the capability is not available in the current configuration ({@link Capability#isAvailable()}). */
	public static boolean hasCapability(Principal principal, Capability capability, String platform) {
		return capability.isAvailable() && canOpen(principal, capability.asRule(), platform);
	}

	/** True for accounts that hold a lockable permission and are not administrators. */
	public static boolean requiresPlatformChoice(Principal principal) {
		if (principal == null || principal.source() != Principal.AuthSource.ACCOUNT || hasUnrestrictedPlatforms(principal)) {
			return false;
		}
		return principal.grants().stream()
		        .filter(g -> operatesOnPlatforms(g.getRole()))
		        .anyMatch(g -> g.getRole().permissions().stream().anyMatch(Permission::isLockable));
	}

	public static List<String> loginPlatformChoices(Principal principal, List<String> allPlatforms) {
		Set<String> result = new LinkedHashSet<>();
		if (principal == null) {
			return List.of();
		}
		for (RoleGrant grant : principal.grants()) {
			boolean lockable = grant.getRole().permissions().stream().anyMatch(Permission::isLockable);
			if (!lockable || !operatesOnPlatforms(grant.getRole())) {
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
			for (Permission permission : grant.getRole().permissions()) {
				if (permission.isPlatformScopable() && !permission.isLockable()) {
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

	private static boolean canOpenWithPermissions(Principal principal, PageRule rule, String targetPlatform) {
		for (RoleGrant grant : principal.grants()) {
			for (Permission permission : grant.getRole().permissions()) {
				if (!rule.permissions().contains(permission)) {
					continue;
				}
				if (!rule.platformBound()) {
					return true;
				}
				if (scopeMatches(grant, permission, targetPlatform)
				        && lockMatches(principal, grant, permission, targetPlatform)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean scopeMatches(RoleGrant grant, Permission permission, String platform) {
		if (grant.getPlatformName() == null || !permission.isPlatformScopable()) {
			return true;
		}
		return grant.getPlatformName().equals(platform);
	}

	/**
	 * Platform scope and login-platform locks come from operator roles (and the all-platform roles). A global role
	 * that merely carries a scopable permission, such as Preparation with athlete card editing, is neither scoped nor
	 * locked, and does not make the account choose a platform.
	 */
	private static boolean operatesOnPlatforms(Role role) {
		return role.isPlatformScopable() || role.coversAllPlatforms();
	}

	private static boolean hasPlatformOperatingGrant(Principal principal, String platform) {
		for (RoleGrant grant : principal.grants()) {
			if (operatesOnPlatforms(grant.getRole())) {
				for (Permission permission : grant.getRole().permissions()) {
					if (permission.isPlatformScopable() && scopeMatches(grant, permission, platform)
					        && lockMatches(principal, grant, permission, platform)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean lockMatches(Principal principal, RoleGrant grant, Permission permission, String platform) {
		return platform == null || !permission.isLockable() || !operatesOnPlatforms(grant.getRole())
		        || principal.loginPlatform() == null || hasUnrestrictedPlatforms(principal)
		        || principal.loginPlatform().equals(platform);
	}
}
