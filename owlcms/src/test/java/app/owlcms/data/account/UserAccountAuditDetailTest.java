package app.owlcms.data.account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import app.owlcms.access.Role;

public class UserAccountAuditDetailTest {

	private static final String HASH = "pbkdf2$secret-hash-value";

	@Test
	public void creationListsPasswordAndEveryGrant() {
		UserAccount created = account("speaker-jf", HASH, new RoleGrant(Role.JURY, "A"),
		        new RoleGrant(Role.ANNOUNCER, "A"));
		assertEquals("account=speaker-jf,changes=password,granted=\"ANNOUNCER@A,JURY@A\"",
		        UserAccount.auditDetail(null, created));
	}

	@Test
	public void creationWithoutPasswordNamesOnlyTheGrants() {
		UserAccount created = account("results", null, new RoleGrant(Role.RESULTS, null));
		assertEquals("account=results,granted=RESULTS", UserAccount.auditDetail(null, created));
	}

	@Test
	public void deletionRevokesEveryGrant() {
		UserAccount deleted = account("referee-WHITE", null, new RoleGrant(Role.REFEREE, "WHITE"));
		assertEquals("account=referee-WHITE,revoked=REFEREE@WHITE", UserAccount.auditDetail(deleted, null));
	}

	@Test
	public void unchangedAccountHasNoDetail() {
		UserAccount before = account("announcer", HASH, new RoleGrant(Role.ANNOUNCER, "A"));
		assertNull(UserAccount.auditDetail(before, before.copy()));
	}

	@Test
	public void grantChangesListTheRolesGivenAndTakenAway() {
		UserAccount before = account("speaker-jf", HASH, new RoleGrant(Role.ANNOUNCER, "A"));
		UserAccount after = before.copy();
		after.getGrants().clear();
		after.getGrants().add(new RoleGrant(Role.JURY, "A"));
		assertEquals("account=speaker-jf,changes=grants,granted=JURY@A,revoked=ANNOUNCER@A",
		        UserAccount.auditDetail(before, after));
	}

	@Test
	public void renameKeepsThePreviousUsername() {
		UserAccount before = account("announcer", null, new RoleGrant(Role.ANNOUNCER, "A"));
		UserAccount after = before.copy();
		after.setUsername("announcer-A");
		assertEquals("account=announcer-A,previous=announcer,changes=username",
		        UserAccount.auditDetail(before, after));
	}

	@Test
	public void passwordAndMustChangeFlagsAreNamedWithoutValues() {
		UserAccount before = account("speaker-jf", HASH, new RoleGrant(Role.ANNOUNCER, "A"));
		UserAccount reset = before.copy();
		reset.setPasswordHash("pbkdf2$another-hash");
		reset.setMustChangePassword(true);
		String detail = UserAccount.auditDetail(before, reset);
		assertEquals("account=speaker-jf,changes=\"password,must-change-set\"", detail);
		assertFalse(detail.contains("hash"));

		UserAccount cleared = reset.copy();
		cleared.setPasswordHash(null);
		cleared.setMustChangePassword(false);
		assertEquals("account=speaker-jf,changes=\"password-removed,must-change-cleared\"",
		        UserAccount.auditDetail(reset, cleared));
	}

	@Test
	public void disablingIsRecorded() {
		UserAccount before = account("tc", null, new RoleGrant(Role.TC, "A"));
		UserAccount after = before.copy();
		after.setEnabled(false);
		assertEquals("account=tc,changes=disabled", UserAccount.auditDetail(before, after));
	}

	@Test
	public void tokenChangesNameTheTokenIdentifierNeverTheToken() {
		UserAccount before = account("broadcaster-1", null, new RoleGrant(Role.EXTERNAL, null));
		UserAccount generated = before.copy();
		generated.replaceToken(new AccountToken("K7Q2XM", "c0ffee-hash", null));
		assertEquals("account=broadcaster-1,changes=token-generated,tokenIdAdded=K7Q2XM",
		        UserAccount.auditDetail(before, generated));

		UserAccount replaced = generated.copy();
		replaced.replaceToken(new AccountToken("P3RT8W", "other-hash", null));
		String detail = UserAccount.auditDetail(generated, replaced);
		assertEquals("account=broadcaster-1,changes=\"token-generated,token-revoked\",tokenIdAdded=P3RT8W,"
		        + "tokenIdRevoked=K7Q2XM", detail);
		assertFalse(detail.contains("hash"));

		UserAccount revoked = replaced.copy();
		revoked.replaceToken(null);
		assertEquals("account=broadcaster-1,changes=token-revoked,tokenIdRevoked=P3RT8W",
		        UserAccount.auditDetail(replaced, revoked));
	}

	@Test
	public void unchangedTokenHasNoDetail() {
		UserAccount before = account("broadcaster-1", null, new RoleGrant(Role.EXTERNAL, null));
		before.replaceToken(new AccountToken("K7Q2XM", "c0ffee-hash", null));
		assertNull(UserAccount.auditDetail(before, before.copy()));
	}

	private static UserAccount account(String username, String hash, RoleGrant... grants) {
		UserAccount account = new UserAccount();
		account.setUsername(username);
		account.setPasswordHash(hash);
		for (RoleGrant grant : grants) {
			account.getGrants().add(grant);
		}
		return account;
	}
}
