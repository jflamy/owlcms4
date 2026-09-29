package app.owlcms.data.account;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import app.owlcms.access.Role;

public class RoleGrantScopeTest {

	@Test
	public void allPlatformsCollapsesPlatformScopesByRole() {
		List<RoleGrant> grants = List.of(new RoleGrant(Role.ANNOUNCER, "A"),
		        new RoleGrant(Role.ANNOUNCER, "B"), new RoleGrant(Role.RESULTS, null),
		        new RoleGrant(Role.JURY, "C"));

		assertEquals(List.of(new RoleGrant(Role.ANNOUNCER, null), new RoleGrant(Role.RESULTS, null),
		        new RoleGrant(Role.JURY, null)), RoleGrantScope.allPlatforms(grants));
	}

	@Test
	public void allPlatformsActionIsNeededOnlyForPlatformSpecificGrants() {
		assertEquals(false, RoleGrantScope.needsAllPlatforms(List.of(new RoleGrant(Role.ANNOUNCER, null))));
		assertEquals(true, RoleGrantScope.needsAllPlatforms(List.of(new RoleGrant(Role.ANNOUNCER, "A"))));
	}
}