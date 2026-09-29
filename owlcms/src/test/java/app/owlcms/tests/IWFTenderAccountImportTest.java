package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.access.AccessPolicy;
import app.owlcms.access.AccountAuthenticator;
import app.owlcms.access.LoginThrottle;
import app.owlcms.access.PageRule;
import app.owlcms.access.Principal;
import app.owlcms.access.Role;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.PlatformRepository;

public class IWFTenderAccountImportTest {

	private static final List<String> PLATFORMS = List.of("A", "B");

	@BeforeClass
	public static void setUpDatabase() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Test
	public void fixtureImportsAccountsAndEnforcesTheirPrivileges() {
		UserAccount sentinel = new UserAccount();
		sentinel.setUsername("local-only");
		UserAccountRepository.save(sentinel);

		try (InputStream fixture = IWFTenderAccountImportTest.class
		        .getResourceAsStream("/testDatabases/IWFTender.json")) {
			assertTrue("IWFTender fixture is present", fixture != null);
			new CompetitionData().restore(fixture);
		} catch (Exception e) {
			throw new AssertionError("Could not import IWFTender fixture", e);
		}

		assertEquals(PLATFORMS, PlatformRepository.findAll().stream().map(platform -> platform.getName()).toList());
		assertEquals(21, UserAccountRepository.findAll().size());
		assertNull(UserAccountRepository.findByUsername("local-only"));

		for (Role role : List.of(Role.ADMIN, Role.PREPARATION, Role.REGISTRATION, Role.WEIGHIN, Role.RESULTS)) {
			assertGrantAndPassword(role.name().toLowerCase(), role, null);
		}
		for (Role role : List.of(Role.PLATFORM, Role.ANNOUNCER, Role.MARSHAL, Role.TIMEKEEPER, Role.TC, Role.JURY,
		        Role.REFEREE, Role.DISPLAYS)) {
			for (String platform : PLATFORMS) {
				assertGrantAndPassword(role.name().toLowerCase() + platform.toLowerCase(), role, platform);
			}
		}

		Principal admin = login("admin");
		assertTrue(AccessPolicy.canOpen(admin, page(Role.ADMIN_PAGES, false), null));
		assertTrue(AccessPolicy.canOpen(admin, page(Role.ANNOUNCER, true), "B"));

		Principal results = login("results");
		assertTrue(AccessPolicy.canOpen(results, page(Role.RESULTS, false), null));
		assertTrue(AccessPolicy.canOpen(results, page(Role.DISPLAYS, true), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(Role.ANNOUNCER, true), "B"));

		for (Role role : List.of(Role.ANNOUNCER, Role.MARSHAL, Role.TIMEKEEPER, Role.TC, Role.JURY, Role.REFEREE,
		        Role.DISPLAYS)) {
			Principal account = login(role.name().toLowerCase() + "a");
			assertTrue(AccessPolicy.canOpen(account, page(role, true), "A"));
			assertFalse(AccessPolicy.canOpen(account, page(role, true), "B"));
			assertEquals(role != Role.DISPLAYS, AccessPolicy.requiresPlatformChoice(account));
		}

		Principal platformA = login("platforma");
		assertTrue(AccessPolicy.canOpen(platformA, page(Role.ANNOUNCER, true), "A"));
		assertTrue(AccessPolicy.canOpen(platformA, page(Role.TIMEKEEPER, true), "A"));
		assertTrue(AccessPolicy.canOpen(platformA, page(Role.DISPLAYS, true), "A"));
		assertFalse(AccessPolicy.canOpen(platformA, page(Role.ANNOUNCER, true), "B"));
	}

	private static void assertGrantAndPassword(String username, Role role, String platform) {
		Principal principal = login(username);
		assertEquals(List.of(new RoleGrant(role, platform)), principal.grants());
	}

	private static Principal login(String username) {
		AccountAuthenticator authenticator = new AccountAuthenticator(UserAccountRepository::findByUsername,
		        new LoginThrottle(System::currentTimeMillis), ip -> true);
		AccountAuthenticator.Result result = authenticator.authenticate(username, username, "127.0.0.1");
		assertTrue(username + " must authenticate", result instanceof AccountAuthenticator.Result.Success);
		return Principal.of(((AccountAuthenticator.Result.Success) result).account(), null);
	}

	private static PageRule page(Role role, boolean platformBound) {
		return PageRule.roles(Set.of(role), platformBound);
	}
}