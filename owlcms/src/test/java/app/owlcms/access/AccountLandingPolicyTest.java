package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
	public void ambiguousAndUnlockedAccountsLandAtHome() {
		assertTrue(AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.PLATFORM, "A"))).isEmpty());
		assertTrue(AccountLandingPolicy.singleWorkPage(account(new RoleGrant(Role.JURY, "A"))).isEmpty());
		Principal admin = new Principal(AuthSource.ACCOUNT, 1L, "admin", List.of(new RoleGrant(Role.ADMIN, null)), null);
		assertTrue(AccountLandingPolicy.singleWorkPage(admin).isEmpty());
	}
}