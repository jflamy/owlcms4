package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import app.owlcms.access.TokenNavigation.Decision;
import app.owlcms.access.TokenNavigation.Inputs;
import app.owlcms.access.TokenNavigation.PinDecision;
import app.owlcms.data.account.RoleGrant;
import app.owlcms.data.account.UserAccount;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.displays.attemptboards.AthleteFacingDecisionBoardPage;
import app.owlcms.nui.displays.scoreboards.JuryScoreboardPage;
import app.owlcms.nui.displays.scoreboards.PublicScoreboardPage;
import app.owlcms.nui.displays.scoreboards.RankingsPage;

public class TokenNavigationTest {

	private static final String IP = "203.0.113.7";
	private static final List<String> TWO = List.of("A", "B");
	private static final List<String> ONE = List.of("Plateau A");

	private final Map<String, UserAccount> byHash = new HashMap<>();
	private final Map<Long, UserAccount> byId = new HashMap<>();
	private final AtomicInteger authenticatorCalls = new AtomicInteger();
	private long nextId = 1;
	private LoginThrottle throttle;
	private TokenAuthenticator authenticator;

	@Before
	public void setUp() {
		this.byHash.clear();
		this.byId.clear();
		this.authenticatorCalls.set(0);
		this.throttle = new LoginThrottle(() -> 1_000_000L);
		this.authenticator = new TokenAuthenticator(hash -> {
			this.authenticatorCalls.incrementAndGet();
			return this.byHash.get(hash);
		}, this.throttle);
	}

	// ---- readInputs

	@Test
	public void tokenAbsentBlankOrRepeated() {
		assertEquals(new Inputs.NoToken(null), TokenNavigation.readInputs(Map.of(), false, TWO));
		assertEquals(new Inputs.NoToken(null), TokenNavigation.readInputs(Map.of("token", List.of(" ")), false, TWO));
		assertEquals(new Inputs.Present("abc", null),
		        TokenNavigation.readInputs(Map.of("token", List.of(" abc ")), false, TWO));
		assertEquals(new Inputs.Malformed(),
		        TokenNavigation.readInputs(Map.of("token", List.of("a", "b")), false, TWO));
		assertEquals(new Inputs.Present("a", null),
		        TokenNavigation.readInputs(Map.of("token", List.of("a", " ")), false, TWO));
	}

	@Test
	public void platformIsReadOnlyWhenThePageIsPlatformBound() {
		Map<String, List<String>> withFop = Map.of("token", List.of("t"), "fop", List.of("B"));
		assertEquals(new Inputs.Present("t", "B"), TokenNavigation.readInputs(withFop, true, TWO));
		assertEquals(new Inputs.Present("t", null), TokenNavigation.readInputs(withFop, false, TWO));
		assertEquals(new Inputs.Present("t", null),
		        TokenNavigation.readInputs(Map.of("token", List.of("t"), "fop", List.of("Z")), false, TWO));
	}

	@Test
	public void platformFollowsTheSinglePlatformOrIsRequired() {
		Map<String, List<String>> noFop = Map.of("token", List.of("t"));
		assertEquals(new Inputs.Present("t", "Plateau A"), TokenNavigation.readInputs(noFop, true, ONE));
		assertEquals(new Inputs.BadPlatform(), TokenNavigation.readInputs(noFop, true, TWO));
		assertEquals(new Inputs.BadPlatform(), TokenNavigation.readInputs(
		        Map.of("token", List.of("t"), "fop", List.of("Z")), true, TWO));
		assertEquals(new Inputs.BadPlatform(), TokenNavigation.readInputs(
		        Map.of("token", List.of("t"), "fop", List.of("A", "B")), true, TWO));
		assertEquals(new Inputs.NoToken("Plateau A"), TokenNavigation.readInputs(
		        Map.of("fop", List.of("Plateau%20A")), true, ONE));
		assertEquals(new Inputs.NoToken("Plateau A"), TokenNavigation.readInputs(
		        Map.of("fop", List.of("Plateau+A")), true, ONE));
	}

	// ---- accounts mode, by page and role

	@Test
	public void streamingAndExternalOpenStreamablePagesButNotVenueOnlyOnes() {
		for (Role role : List.of(Role.STREAMING, Role.EXTERNAL)) {
			String token = issue(role.name(), new RoleGrant(role, null));
			assertAllowed(decide(PublicScoreboardPage.class, token, "A", null), role.name());
			assertAllowed(decide(RankingsPage.class, token, null, null), role.name());
			assertRefused(FORBIDDEN, "forbidden", decide(AthleteFacingDecisionBoardPage.class, token, "A", null));
			assertRefused(FORBIDDEN, "forbidden", decide(JuryScoreboardPage.class, token, "A", null));
		}
	}

	@Test
	public void displaysOpensEveryOutputPage() {
		String token = issue("displays", new RoleGrant(Role.DISPLAYS, null));
		for (Class<?> page : List.of(PublicScoreboardPage.class, RankingsPage.class,
		        AthleteFacingDecisionBoardPage.class, JuryScoreboardPage.class)) {
			assertAllowed(decide(page, token, "A", null), page.getSimpleName());
		}
	}

	@Test
	public void scopedDisplaysIsRefusedOnOtherPlatforms() {
		String token = issue("scoped", new RoleGrant(Role.DISPLAYS, "A"));
		assertAllowed(decide(AthleteFacingDecisionBoardPage.class, token, "A", null), "A");
		assertRefused(FORBIDDEN, "forbidden", decide(AthleteFacingDecisionBoardPage.class, token, "B", null));
	}

	@Test
	public void juryOpensOnlyThePagesListingJury() {
		String token = issue("jury", new RoleGrant(Role.JURY, null));
		assertAllowed(decide(JuryScoreboardPage.class, token, "A", null), "jury scoreboard");
		assertRefused(FORBIDDEN, "forbidden", decide(PublicScoreboardPage.class, token, "A", null));
	}

	@Test
	public void supervisorReachesDisplayPagesThroughDisplays() {
		String token = issue("supervisor", new RoleGrant(Role.COMPETITION_SUPERVISOR, null));
		assertAllowed(decide(PublicScoreboardPage.class, token, "A", null), "supervisor");
		assertAllowed(decide(JuryScoreboardPage.class, token, "B", null), "supervisor");
	}

	@Test
	public void resultsOpensNoDisplayPage() {
		String token = issue("results", new RoleGrant(Role.RESULTS, null));
		assertRefused(FORBIDDEN, "forbidden", decide(PublicScoreboardPage.class, token, "A", null));
		assertRefused(FORBIDDEN, "forbidden", decide(RankingsPage.class, token, null, null));
		assertRefused(FORBIDDEN, "forbidden", decide(JuryScoreboardPage.class, token, "A", null));
		assertRefused(FORBIDDEN, "forbidden", decide(AthleteFacingDecisionBoardPage.class, token, "A", null));
	}

	@Test
	public void everyTokenIsRefusedOnALauncherWithoutCallingTheAuthenticator() {
		for (Role role : List.of(Role.STREAMING, Role.EXTERNAL, Role.DISPLAYS, Role.COMPETITION_SUPERVISOR,
		        Role.ADMIN)) {
			String token = issue(role.name(), new RoleGrant(role, null));
			assertRefused(FORBIDDEN, "not-token-page", decide(DisplayNavigationContent.class, token, "A", null));
		}
		assertEquals(0, this.authenticatorCalls.get());
	}

	// ---- accounts mode, token handling

	@Test
	public void noTokenAndNoBindingIsNotATokenNavigation() {
		assertTrue(decide(PublicScoreboardPage.class, null, "A", null) instanceof Decision.NotToken);
		assertTrue(decide(DisplayNavigationContent.class, null, null, null) instanceof Decision.NotToken);
		assertEquals(0, this.authenticatorCalls.get());
	}

	@Test
	public void unknownTokenIsUnauthorizedAndRepeatedFailuresAreThrottled() {
		for (int i = 0; i < LoginThrottle.FREE_FAILURES; i++) {
			assertRefused(UNAUTHORIZED, "invalid", decide(PublicScoreboardPage.class, "bad-token-00" + i, "A", null));
		}
		String good = issue("streaming", new RoleGrant(Role.STREAMING, null));
		assertRefused(TOO_MANY, "throttled", decide(PublicScoreboardPage.class, good, "A", null));
	}

	@Test
	public void badPlatformsAndRepeatedTokensAreRefusedBeforeAuthentication() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		this.authenticatorCalls.set(0);
		assertRefused(FORBIDDEN, "platform", decide(PublicScoreboardPage.class, token, "Z", null));
		assertRefused(FORBIDDEN, "platform", decide(PublicScoreboardPage.class, token, null, null));
		Decision twoTokens = TokenNavigation.decideAccounts(PageRule.of(PublicScoreboardPage.class),
		        Map.of("token", List.of(token, token), "fop", List.of("A")), TWO, null, IP, this.authenticator,
		        this.byId::get);
		assertRefused(UNAUTHORIZED, "malformed", twoTokens);
		assertEquals(0, this.authenticatorCalls.get());
	}

	@Test
	public void aSinglePlatformNeedsNoFop() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		Decision decision = TokenNavigation.decideAccounts(PageRule.of(PublicScoreboardPage.class),
		        Map.of("token", List.of(token)), ONE, null, IP, this.authenticator, this.byId::get);
		assertAllowed(decision, "single platform");
	}

	@Test
	public void allowedTokenBindsTheTabWithoutKeepingTheToken() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		Decision.Allow allow = (Decision.Allow) decide(PublicScoreboardPage.class, token, "A", null);
		assertTrue(allow.newlyBound());
		assertEquals("streaming", allow.tab().principal().username());
		assertEquals(Principal.AuthSource.TOKEN, allow.tab().principal().source());
		assertFalse(allow.tab().toString().contains(token));
	}

	// ---- accounts mode, existing binding

	@Test
	public void aBoundTabIsRevalidatedWhenTheUrlHasNoToken() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		TokenTab tab = ((Decision.Allow) decide(PublicScoreboardPage.class, token, "A", null)).tab();
		this.authenticatorCalls.set(0);

		Decision.Allow again = (Decision.Allow) decide(RankingsPage.class, null, null, tab);
		assertFalse(again.newlyBound());
		assertEquals(0, this.authenticatorCalls.get());
		assertRefused(FORBIDDEN, "not-token-page", decide(DisplayNavigationContent.class, null, null, tab));
	}

	@Test
	public void revokedReplacedDisabledAndDeletedAccountsAreUnauthorized() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		TokenTab tab = ((Decision.Allow) decide(PublicScoreboardPage.class, token, "A", null)).tab();
		UserAccount account = this.byId.get(tab.accountId());

		account.replaceToken(null);
		assertRefused(UNAUTHORIZED, "revoked", decide(PublicScoreboardPage.class, null, "A", tab));

		account.replaceToken(AccountTokens.generate().record());
		assertRefused(UNAUTHORIZED, "revoked", decide(PublicScoreboardPage.class, null, "A", tab));

		String second = issue("second", new RoleGrant(Role.STREAMING, null));
		TokenTab secondTab = ((Decision.Allow) decide(PublicScoreboardPage.class, second, "A", null)).tab();
		this.byId.get(secondTab.accountId()).setEnabled(false);
		assertRefused(UNAUTHORIZED, "revoked", decide(PublicScoreboardPage.class, null, "A", secondTab));

		this.byId.remove(secondTab.accountId());
		assertRefused(UNAUTHORIZED, "revoked", decide(PublicScoreboardPage.class, null, "A", secondTab));
	}

	@Test
	public void aRoleRemovedFromTheOwnerIsForbidden() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		TokenTab tab = ((Decision.Allow) decide(PublicScoreboardPage.class, token, "A", null)).tab();
		UserAccount account = this.byId.get(tab.accountId());
		account.getGrants().clear();
		account.getGrants().add(new RoleGrant(Role.JURY, null));
		Decision.Refuse refuse = (Decision.Refuse) decide(PublicScoreboardPage.class, null, "A", tab);
		assertEquals(FORBIDDEN, refuse.status());
		assertEquals("streaming", refuse.username());
	}

	@Test
	public void aTokenInTheUrlReplacesTheBinding() {
		String first = issue("first", new RoleGrant(Role.STREAMING, null));
		String second = issue("second", new RoleGrant(Role.STREAMING, null));
		TokenTab tab = ((Decision.Allow) decide(PublicScoreboardPage.class, first, "A", null)).tab();
		Decision.Allow replaced = (Decision.Allow) decide(PublicScoreboardPage.class, second, "A", tab);
		assertTrue(replaced.newlyBound());
		assertEquals("second", replaced.tab().principal().username());
		assertRefused(UNAUTHORIZED, "invalid", decide(PublicScoreboardPage.class, "not-a-token1", "A", tab));
	}

	@Test
	public void aTokenOnALauncherIsRefusedEvenWhenTheTabIsBound() {
		String token = issue("streaming", new RoleGrant(Role.STREAMING, null));
		TokenTab tab = ((Decision.Allow) decide(PublicScoreboardPage.class, token, "A", null)).tab();
		Decision.Refuse refuse = (Decision.Refuse) decide(DisplayNavigationContent.class, token, "A", tab);
		assertEquals("not-token-page", refuse.reason());
		assertNull(refuse.username());
	}

	// ---- PIN mode

	@Test
	public void pinDecisionIsNotApplicableWithoutAPinATokenOrATokenPage() {
		PageRule output = PageRule.of(PublicScoreboardPage.class);
		Map<String, List<String>> withPin = Map.of("token", List.of("1234"));
		assertTrue(pin(output, withPin, false, true, "1234") instanceof PinDecision.NotApplicable);
		assertTrue(pin(output, Map.of(), true, true, "1234") instanceof PinDecision.NotApplicable);
		assertTrue(pin(output, Map.of("token", List.of(" ")), true, true, "1234") instanceof PinDecision.NotApplicable);
		assertTrue(pin(PageRule.of(DisplayNavigationContent.class), withPin, true, true, "1234")
		        instanceof PinDecision.NotApplicable);
	}

	@Test
	public void pinDecisionAcceptsTheDisplayPinOnOutputPagesIncludingVenueOnlyOnes() {
		Map<String, List<String>> withPin = Map.of("token", List.of(" 1234 "));
		for (Class<?> page : List.of(PublicScoreboardPage.class, AthleteFacingDecisionBoardPage.class,
		        JuryScoreboardPage.class)) {
			assertTrue(page.getSimpleName(),
			        pin(PageRule.of(page), withPin, true, true, "1234") instanceof PinDecision.Accept);
		}
	}

	@Test
	public void pinDecisionRefusesWrongRepeatedAndUnlistedAndThrottles() {
		PageRule output = PageRule.of(PublicScoreboardPage.class);
		assertRefusedPin(UNAUTHORIZED, "invalid", pin(output, Map.of("token", List.of("0000")), true, true, "1234"));
		assertRefusedPin(UNAUTHORIZED, "malformed",
		        pin(output, Map.of("token", List.of("1234", "1234")), true, true, "1234"));
		assertRefusedPin(FORBIDDEN, "display-ip", pin(output, Map.of("token", List.of("1234")), true, false, "1234"));
		for (int i = 1; i < LoginThrottle.FREE_FAILURES; i++) {
			pin(output, Map.of("token", List.of("bad" + i)), true, true, "1234");
		}
		assertRefusedPin(TOO_MANY, "throttled", pin(output, Map.of("token", List.of("1234")), true, true, "1234"));
	}

	// ---- helpers

	private static final int UNAUTHORIZED = TokenNavigation.UNAUTHORIZED;
	private static final int FORBIDDEN = TokenNavigation.FORBIDDEN;
	private static final int TOO_MANY = TokenNavigation.TOO_MANY_REQUESTS;

	private Decision decide(Class<?> page, String token, String fop, TokenTab existing) {
		Map<String, List<String>> query = new HashMap<>();
		if (token != null) {
			query.put("token", List.of(token));
		}
		if (fop != null) {
			query.put("fop", List.of(fop));
		}
		return TokenNavigation.decideAccounts(PageRule.of(page), query, TWO, existing, IP, this.authenticator,
		        this.byId::get);
	}

	private PinDecision pin(PageRule rule, Map<String, List<String>> query, boolean pinExpected, boolean ipAllowed,
	        String displayPin) {
		return TokenNavigation.decidePin(rule, query, pinExpected, ipAllowed, IP, displayPin::equals, this.throttle);
	}

	private String issue(String username, RoleGrant... grants) {
		AccountTokens.Generated generated = AccountTokens.generate();
		UserAccount account = new UserAccount();
		account.setId(this.nextId++);
		account.setUsername(username);
		account.getGrants().addAll(List.of(grants));
		account.replaceToken(generated.record());
		this.byHash.put(generated.record().getTokenHash(), account);
		this.byId.put(account.getId(), account);
		return generated.value();
	}

	private static void assertAllowed(Decision decision, String message) {
		assertTrue(message + " expected Allow but was " + decision, decision instanceof Decision.Allow);
	}

	private static void assertRefused(int status, String reason, Decision decision) {
		assertTrue("expected Refuse but was " + decision, decision instanceof Decision.Refuse);
		Decision.Refuse refuse = (Decision.Refuse) decision;
		assertEquals(reason, refuse.reason());
		assertEquals(status, refuse.status());
	}

	private static void assertRefusedPin(int status, String reason, PinDecision decision) {
		assertTrue("expected Refuse but was " + decision, decision instanceof PinDecision.Refuse);
		PinDecision.Refuse refuse = (PinDecision.Refuse) decision;
		assertEquals(reason, refuse.reason());
		assertEquals(status, refuse.status());
	}
}
