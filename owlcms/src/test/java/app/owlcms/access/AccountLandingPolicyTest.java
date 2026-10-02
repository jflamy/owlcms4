package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import app.owlcms.access.AccountLandingPolicy.WorkPage;
import app.owlcms.access.Principal.AuthSource;
import app.owlcms.data.account.RoleGrant;

public class AccountLandingPolicyTest {

	private static Principal account(RoleGrant... grants) {
		return new Principal(AuthSource.ACCOUNT, 1L, "user", List.of(grants), "A");
	}

	@Test
	public void singleOperatorGoesDirectlyToItsWorkPage() {
		assertEquals(WorkPage.ANNOUNCER,
		        AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.ANNOUNCER, "A"))).orElseThrow());
		assertEquals(WorkPage.REFEREE_NAVIGATION,
		        AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.REFEREE, "A"))).orElseThrow());
	}

	@Test
	public void everyGrantableRoleHasItsOwnDefault() {
		Map<Role, WorkPage> defaults = Map.ofEntries(
		        Map.entry(Role.ADMIN, WorkPage.HOME),
		        Map.entry(Role.COMPETITION_DIRECTOR, WorkPage.COMPETITION_DIRECTOR),
		        Map.entry(Role.PREPARATION, WorkPage.PREPARATION),
		        Map.entry(Role.REGISTRATION, WorkPage.REGISTRATION),
		        Map.entry(Role.WEIGHIN, WorkPage.WEIGHIN),
		        Map.entry(Role.RESULTS, WorkPage.RESULTS),
		        Map.entry(Role.PLATFORM, WorkPage.LIFTING),
		        Map.entry(Role.ANNOUNCER, WorkPage.ANNOUNCER),
		        Map.entry(Role.MARSHAL, WorkPage.MARSHAL),
		        Map.entry(Role.TIMEKEEPER, WorkPage.TIMEKEEPER),
		        Map.entry(Role.TC, WorkPage.TC),
		        Map.entry(Role.JURY, WorkPage.JURY_CONSOLE),
		        Map.entry(Role.REFEREE, WorkPage.REFEREE_NAVIGATION),
		        Map.entry(Role.DISPLAYS, WorkPage.DISPLAY_NAVIGATION));
		for (Role role : Role.values()) {
			if (role.isGrantable()) {
				Principal principal = new Principal(AuthSource.ACCOUNT, 1L, "user",
				        List.of(new RoleGrant(role, null)), null);
				assertEquals(role.name(), defaults.get(role), AccountLandingPolicy.singleWorkPage(principal).orElseThrow());
			}
		}
	}

	@Test
	public void multipleExplicitRolesLandAtHomeRatherThanChoosingInheritedPermissions() {
		assertTrue(AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.ANNOUNCER, "A"),
		        new RoleGrant(Role.TIMEKEEPER, "A"))).isEmpty());
		assertTrue(AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.ADMIN, null),
		        new RoleGrant(Role.COMPETITION_DIRECTOR, null))).isEmpty());
		assertEquals(WorkPage.LIFTING,
		        AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.PLATFORM, "A"))).orElseThrow());
		assertEquals(WorkPage.COMPETITION_DIRECTOR,
		        AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.COMPETITION_DIRECTOR, null))).orElseThrow());
	}

	@Test
	public void repeatedRoleGrantsDoNotMakeTheDefaultAmbiguous() {
		assertEquals(WorkPage.ANNOUNCER, AccountLandingPolicy.singleWorkPage(account(
		        new RoleGrant(Role.ANNOUNCER, "A"), new RoleGrant(Role.ANNOUNCER, "B"))).orElseThrow());
	}

	@Test
	public void missingAccountsAndPinPrincipalsHaveNoAccountLanding() {
		assertTrue(AccountLandingPolicy.singleWorkPage(null).isEmpty());
		assertTrue(AccountLandingPolicy.singleWorkPage(account()).isEmpty());
		assertTrue(AccountLandingPolicy.singleWorkPage(Principal.admin(AuthSource.OFFICIALS_PIN)).isEmpty());
	}
}