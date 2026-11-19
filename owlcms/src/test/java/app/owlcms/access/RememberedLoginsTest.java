package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.access.RememberedBrowser.Kind;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.access.RememberedLoginRepository;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.tests.TestStartup;

public class RememberedLoginsTest {

	private static final AuditOrigin ORIGIN = AuditOrigin.system("remembered login test");
	private static final ZoneId ZONE = ZoneId.of("America/Toronto");
	private static final Instant NOW = ZonedDateTime.of(2026, 11, 18, 13, 26, 0, 0, ZONE).toInstant();
	private static final String PIN = "stored:pbkdf2-sha256$600000$abc$def";
	private static int accountNumber;

	@BeforeClass
	public static void setUpDatabase() {
		TestStartup.init();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void closeDatabase() {
		JPAService.close();
	}

	@Before
	public void clearRememberedLogins() {
		RememberedLoginRepository.deleteAll();
		Config.getCurrent().setRememberAccountLogins(true);
	}

	@Test
	public void aPinLoginIsValidAndUpdatesLastUse() {
		RememberedLogins.Issued issued = issue(Kind.OFFICIALS, PIN, null);
		RememberedLogins.Lookup lookup = lookup(issued.cookieValue(), Kind.OFFICIALS, PIN, NOW.plusSeconds(600));

		assertNotNull(lookup.found());
		assertFalse(lookup.deleteCookie());
		assertEquals(NOW.plusSeconds(600), lookup.found().login().getLastUsedAt());
		assertEquals(NOW.plusSeconds(86400 - 13 * 3600 - 26 * 60), lookup.found().login().getExpiresAt()
		        .atZone(ZONE).withZoneSameInstant(ZONE).toInstant());
	}

	@Test
	public void wrongAndMalformedCookiesAreRefused() {
		RememberedLogins.Issued issued = issue(Kind.DISPLAY, PIN, null);
		String wrongValidator = wrongValidator(issued.cookieValue());

		RememberedLogins.Lookup wrong = lookup(wrongValidator, Kind.DISPLAY, PIN, NOW.plusSeconds(1));
		assertNull(wrong.found());
		assertTrue(wrong.deleteCookie());
		assertNull(RememberedLoginRepository.find(issued.login().getSelector()));

		assertNull(lookup("garbage", Kind.DISPLAY, PIN, NOW.plusSeconds(1)).found());
		assertTrue(lookup("v1.a.b", Kind.DISPLAY, PIN, NOW.plusSeconds(1)).deleteCookie());
		assertFalse(lookup(null, Kind.DISPLAY, PIN, NOW.plusSeconds(1)).deleteCookie());
	}

	@Test
	public void expiryAndCredentialChangesDeleteTheRow() {
		RememberedLogins.Issued expired = issue(Kind.OFFICIALS, PIN, null);
		RememberedLogins.Lookup late = lookup(expired.cookieValue(), Kind.OFFICIALS, PIN,
		        expired.login().getExpiresAt());
		assertNull(late.found());
		assertTrue(late.deleteCookie());
		assertNull(RememberedLoginRepository.find(expired.login().getSelector()));

		RememberedLogins.Issued changed = issue(Kind.OFFICIALS, PIN, null);
		RememberedLogins.Lookup wrongPin = lookup(changed.cookieValue(), Kind.OFFICIALS,
		        "stored:pbkdf2-sha256$600000$abc$other", NOW.plusSeconds(1));
		assertNull(wrongPin.found());
		assertNull(RememberedLoginRepository.find(changed.login().getSelector()));
	}

	@Test
	public void anAccountLoginRequiresTheSamePasswordAndPolicy() {
		UserAccount account = saveAccount("results");
		RememberedLogins.Issued issued = issue(Kind.ACCOUNT, account.getPasswordHash(), account);

		RememberedLogins.Lookup valid = lookup(issued.cookieValue(), Kind.ACCOUNT, null, NOW.plusSeconds(1));
		assertNotNull(valid.found());
		assertEquals(account.getId(), valid.found().account().getId());

		Config.getCurrent().setRememberAccountLogins(false);
		RememberedLogins.Lookup disabled = lookup(issued.cookieValue(), Kind.ACCOUNT, null, NOW.plusSeconds(2));
		assertNull(disabled.found());
		assertFalse("the row stays while remembering is disabled", disabled.deleteCookie());
		assertNotNull(RememberedLoginRepository.find(issued.login().getSelector()));

		Config.getCurrent().setRememberAccountLogins(true);
		UserAccount changed = account.copy();
		changed.setPasswordHash("pbkdf2-sha256$600000$xyz$new");
		UserAccountRepository.save(changed, ORIGIN);
		RememberedLogins.Lookup changedPassword = lookup(issued.cookieValue(), Kind.ACCOUNT, null,
		        NOW.plusSeconds(3));
		assertNull(changedPassword.found());
		assertNull(RememberedLoginRepository.find(issued.login().getSelector()));
	}

	@Test
	public void ineligibleAccountsDoNotKeepRows() {
		UserAccount account = saveAccount("disabled");
		RememberedLogins.Issued issued = issue(Kind.ACCOUNT, account.getPasswordHash(), account);
		UserAccount disabled = account.copy();
		disabled.setEnabled(false);
		UserAccountRepository.save(disabled, ORIGIN);

		RememberedLogins.Lookup lookup = lookup(issued.cookieValue(), Kind.ACCOUNT, null, NOW.plusSeconds(1));
		assertNull(lookup.found());
		assertNull(RememberedLoginRepository.find(issued.login().getSelector()));
	}

	@Test
	public void expiryAlwaysUsesAMidnightAcrossDaylightSavingTime() {
		ZoneId montreal = ZoneId.of("America/Montreal");
		Instant beforeDst = ZonedDateTime.of(2026, 3, 7, 13, 0, 0, 0, montreal).toInstant();
		assertEquals(ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, montreal).toInstant(),
		        RememberedLogins.expiry(beforeDst, montreal, 1));

		Instant between = ZonedDateTime.of(2026, 3, 20, 13, 0, 0, 0, montreal).toInstant();
		Instant expected = ZonedDateTime.of(2026, 4, 10, 0, 0, 0, 0, montreal).toInstant();
		assertEquals(expected, RememberedLogins.expiry(between, montreal, 21));
	}

	@Test
	public void theNewestPinBrowsersStayWithinTheCap() {
		String firstSelector = null;
		for (int i = 0; i <= RememberedLogins.MAX_PIN_BROWSERS; i++) {
			RememberedLogins.Issued issued = RememberedLogins.issue(Kind.OFFICIALS, null, null, PIN,
			        NOW.plusSeconds(i), ZONE, "127.0.0.1", "test");
			if (i == 0) {
				firstSelector = issued.login().getSelector();
			}
		}
		assertNull(RememberedLoginRepository.find(firstSelector));
		long count = JPAService.runInTransaction(
		        em -> em.createQuery("select count(r) from RememberedLogin r", Long.class).getSingleResult());
		assertEquals(RememberedLogins.MAX_PIN_BROWSERS, count);
	}

	@Test
	public void forgettingWithTheWrongValidatorDeletesNothing() {
		RememberedLogins.Issued issued = issue(Kind.OFFICIALS, PIN, null);
		assertFalse(RememberedLogins.forget(wrongValidator(issued.cookieValue()), Kind.OFFICIALS));
		assertNotNull(RememberedLoginRepository.find(issued.login().getSelector()));
	}

	@Test
	public void forgettingAndReissuingKeepsOnlyTheNewRow() {
		RememberedLogins.Issued first = issue(Kind.OFFICIALS, PIN, null);
		assertTrue(RememberedLogins.forget(first.cookieValue(), Kind.OFFICIALS));
		RememberedLogins.Issued second = issue(Kind.OFFICIALS, PIN, null);

		assertNull(RememberedLoginRepository.find(first.login().getSelector()));
		assertNotNull(RememberedLoginRepository.find(second.login().getSelector()));
	}

	@Test
	public void forgetAllRemovesEveryKind() {
		RememberedLogins.Issued officials = issue(Kind.OFFICIALS, PIN, null);
		RememberedLogins.Issued display = issue(Kind.DISPLAY, PIN, null);
		UserAccount account = saveAccount("forgotten");
		RememberedLogins.Issued accountLogin = issue(Kind.ACCOUNT, account.getPasswordHash(), account);

		assertEquals(3, RememberedLogins.forgetAll(ORIGIN));
		assertNull(RememberedLoginRepository.find(officials.login().getSelector()));
		assertNull(RememberedLoginRepository.find(display.login().getSelector()));
		assertNull(RememberedLoginRepository.find(accountLogin.login().getSelector()));
	}

	@Test
	public void onlyEnabledNonAdministratorAccountsWithAPasswordAreRememberable() {
		assertTrue(RememberedAccount.isRememberable(account("marshal", new RoleGrant(Role.MARSHAL, "A"))));
		assertFalse(RememberedAccount.isRememberable(account("boss", new RoleGrant(Role.ADMIN, null))));
		assertFalse("the built-in admin is excluded even if its grants have not been repaired yet",
		        RememberedAccount.isRememberable(account(UserAccount.BUILT_IN_ADMIN)));
		UserAccount mustChange = account("results", new RoleGrant(Role.RESULTS, null));
		mustChange.setMustChangePassword(true);
		assertFalse(RememberedAccount.isRememberable(mustChange));
		UserAccount noPassword = account("results", new RoleGrant(Role.RESULTS, null));
		noPassword.setPasswordHash(null);
		assertFalse(RememberedAccount.isRememberable(noPassword));
		assertFalse(RememberedAccount.isRememberable(null));
	}

	private static RememberedLogins.Issued issue(Kind kind, String basis, UserAccount account) {
		return RememberedLogins.issue(kind, account, null, basis, NOW, ZONE, "127.0.0.1", "test agent");
	}

	private static RememberedLogins.Lookup lookup(String cookie, Kind kind, String basis, Instant now) {
		return RememberedLogins.lookup(cookie, kind, basis, Config.getCurrent(), now, "127.0.0.1", "test agent");
	}

	private static UserAccount saveAccount(String prefix) {
		UserAccount saved = account(prefix + ++accountNumber, new RoleGrant(Role.RESULTS, null));
		return UserAccountRepository.save(saved, ORIGIN);
	}

	private static UserAccount account(String username, RoleGrant... grants) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setPasswordHash("pbkdf2-sha256$600000$abc$def");
		account.setGrants(List.of(grants));
		return account;
	}

	private static String wrongValidator(String cookie) {
		int dot = cookie.lastIndexOf('.');
		String validator = cookie.substring(dot + 1);
		return cookie.substring(0, dot + 1) + (validator.startsWith("A") ? "B" : "A") + validator.substring(1);
	}
}
