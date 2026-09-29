package app.owlcms.data.account;

import java.util.ArrayList;
import java.util.List;

/** Normalizes role grants to their all-platform form while preserving role order. */
public final class RoleGrantScope {

	private RoleGrantScope() {
	}

	public static List<RoleGrant> allPlatforms(List<RoleGrant> grants) {
		List<RoleGrant> result = new ArrayList<>();
		for (RoleGrant grant : grants) {
			if (grant.getRole() == null) {
				continue;
			}
			RoleGrant allPlatforms = new RoleGrant(grant.getRole(), null);
			if (!result.contains(allPlatforms)) {
				result.add(allPlatforms);
			}
		}
		return result;
	}

	public static boolean needsAllPlatforms(List<RoleGrant> grants) {
		return grants.stream().anyMatch(grant -> grant.getPlatformName() != null);
	}
}