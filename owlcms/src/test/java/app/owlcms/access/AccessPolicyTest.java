package app.owlcms.access;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import app.owlcms.access.Principal.AuthSource;
import app.owlcms.data.account.RoleGrant;

public class AccessPolicyTest {

	private static final List<String> PLATFORMS = List.of("A", "B", "C");

	private static Principal account(String loginPlatform, RoleGrant... grants) {
		return new Principal(AuthSource.ACCOUNT, 1L, "user", List.of(grants), loginPlatform);
	}

	private static PageRule page(boolean platformBound, Role... roles) {
		Set<Role> set = EnumSet.noneOf(Role.class);
		set.addAll(List.of(roles));
		return PageRule.roles(set, platformBound);
	}

	@Test
	public void compositeRolesExpandToBaseRoles() {
		assertTrue(Role.ADMIN.expand().contains(Role.ADMIN_PAGES));
		assertTrue(Role.ADMIN.expand().containsAll(EnumSet.of(Role.PREPARATION, Role.DISPLAYS, Role.REFEREE)));
		assertEquals(EnumSet.of(Role.RESULTS, Role.DISPLAYS), Role.RESULTS.expand());
		assertEquals(EnumSet.of(Role.ANNOUNCER, Role.MARSHAL, Role.TIMEKEEPER, Role.TC, Role.DISPLAYS),
		        Role.PLATFORM.expand());
		assertEquals(EnumSet.of(Role.JURY), Role.JURY.expand());
	}

	@Test
	public void pagesOnlyReferToBaseRoles() {
		for (Role role : Role.baseRoles()) {
			assertTrue(role + " must be a base role", role.isBase());
		}
		assertFalse(Role.baseRoles().contains(Role.ADMIN));
		assertFalse(Role.baseRoles().contains(Role.PLATFORM));
	}

	@Test
	public void pinModePrincipalsOpenWhatTheyDidBefore() {
		Principal officials = Principal.admin(AuthSource.OFFICIALS_PIN);
		Principal displays = Principal.displays();
		PageRule officialsPage = page(false, Role.PREPARATION);
		PageRule adminPage = page(false, Role.ADMIN_PAGES);
		PageRule boardPage = page(true, Role.DISPLAYS);
		PageRule juryBoard = page(true, Role.DISPLAYS, Role.JURY);

		assertTrue(AccessPolicy.canOpen(officials, officialsPage, null));
		assertTrue(AccessPolicy.canOpen(officials, adminPage, null));
		assertTrue(AccessPolicy.canOpen(officials, boardPage, "A"));
		assertTrue(AccessPolicy.canOpen(displays, boardPage, "A"));
		assertTrue(AccessPolicy.canOpen(displays, juryBoard, "B"));
		assertFalse(AccessPolicy.canOpen(displays, officialsPage, null));
		assertFalse(AccessPolicy.canOpen(displays, adminPage, null));
	}

	@Test
	public void recordsOnlyDisplayPagesPreserveLegacyDestinations() {
		assertEquals(RecordsOnlyPolicy.DisplayDestination.EDIT_RECORDS,
		        RecordsOnlyPolicy.displayDestination(true));
		assertEquals(RecordsOnlyPolicy.DisplayDestination.PUBLIC_RECORDS,
		        RecordsOnlyPolicy.displayDestination(false));
	}

	@Test
	public void publicAuthenticatedAndUnannotatedPages() {
		Principal displays = Principal.displays();
		PageRule publicPage = new PageRule(PageRule.Kind.PUBLIC, Set.of(), false, null);
		PageRule authenticated = new PageRule(PageRule.Kind.AUTHENTICATED, Set.of(), false, null);
		PageRule unannotated = new PageRule(PageRule.Kind.NONE, Set.of(), false, null);

		assertTrue(AccessPolicy.canOpen(null, publicPage, null));
		assertFalse(AccessPolicy.canOpen(null, authenticated, null));
		assertTrue(AccessPolicy.canOpen(displays, authenticated, null));
		assertFalse(AccessPolicy.canOpen(Principal.admin(AuthSource.BACKDOOR), unannotated, null));
	}

	@Test
	public void scopeLimitsPlatformBoundPagesOnly() {
		Principal announcerA = account(null, new RoleGrant(Role.ANNOUNCER, "A"));
		PageRule announcerPage = page(true, Role.ANNOUNCER);
		PageRule navigation = page(false, Role.ANNOUNCER, Role.MARSHAL);

		assertTrue(AccessPolicy.canOpen(announcerA, announcerPage, "A"));
		assertFalse(AccessPolicy.canOpen(announcerA, announcerPage, "B"));
		assertTrue(AccessPolicy.canOpen(announcerA, navigation, null));
	}

	@Test
	public void lockableRolesFollowLoginPlatformButDisplaysDoNot() {
		Principal platformAll = account("A", new RoleGrant(Role.PLATFORM, null));

		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Role.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(platformAll, page(true, Role.ANNOUNCER), "B"));
		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Role.DISPLAYS), "B"));
	}

	@Test
	public void adminIsNeverLockedAndNeverAsksForAPlatform() {
		Principal admin = account("A", new RoleGrant(Role.ADMIN, null));
		Principal unlockedAdmin = account(null, new RoleGrant(Role.ADMIN, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(admin));
		assertFalse(AccessPolicy.isLocked(admin));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(admin, PLATFORMS));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(true, Role.ANNOUNCER), "C"));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(false, Role.ADMIN_PAGES), null));
		assertTrue(AccessPolicy.canOpen(admin, page(true, Role.ANNOUNCER), "C"));
	}

	@Test
	public void resultsAccountOpensResultsAndAnyPlatformScoreboardWithoutPlatformChoice() {
		Principal results = account(null, new RoleGrant(Role.RESULTS, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(results));
		assertTrue(AccessPolicy.canOpen(results, page(false, Role.RESULTS), null));
		assertTrue(AccessPolicy.canOpen(results, page(false, Role.DISPLAYS), null));
		assertTrue(AccessPolicy.canOpen(results, page(true, Role.DISPLAYS), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(true, Role.ANNOUNCER), "B"));
	}

	@Test
	public void platformChoiceIsOfferedAmongLockableGrants() {
		Principal scoped = account(null, new RoleGrant(Role.ANNOUNCER, "B"), new RoleGrant(Role.DISPLAYS, null));
		Principal everywhere = account(null, new RoleGrant(Role.TIMEKEEPER, null));
		Principal displaysOnly = account(null, new RoleGrant(Role.DISPLAYS, null));

		assertTrue(AccessPolicy.requiresPlatformChoice(scoped));
		assertEquals(List.of("B"), AccessPolicy.loginPlatformChoices(scoped, PLATFORMS));
		assertEquals(PLATFORMS, AccessPolicy.loginPlatformChoices(everywhere, PLATFORMS));
		assertFalse(AccessPolicy.requiresPlatformChoice(displaysOnly));
		assertEquals(List.of(), AccessPolicy.loginPlatformChoices(displaysOnly, PLATFORMS));
	}

	@Test
	public void grantsForUnknownPlatformsDoNotOpenAnything() {
		Principal announcerZ = account(null, new RoleGrant(Role.ANNOUNCER, "Z"));

		assertEquals(List.of(), AccessPolicy.selectablePlatforms(announcerZ, EnumSet.of(Role.ANNOUNCER), PLATFORMS));
		assertEquals(List.of(), AccessPolicy.loginPlatformChoices(announcerZ, PLATFORMS));
	}

	@Test
	public void loginAuditPlatformsFollowEffectivePlatformAccess() {
		Principal scoped = account(null, new RoleGrant(Role.ANNOUNCER, "B"));
		Principal results = account(null, new RoleGrant(Role.RESULTS, null));
		Principal preparation = account(null, new RoleGrant(Role.PREPARATION, null));
		Principal locked = account("A", new RoleGrant(Role.ANNOUNCER, null));

		assertEquals(List.of("B"), AccessPolicy.platformsWithAccess(scoped, PLATFORMS));
		assertEquals(PLATFORMS, AccessPolicy.platformsWithAccess(results, PLATFORMS));
		assertEquals(List.of(), AccessPolicy.platformsWithAccess(preparation, PLATFORMS));
		assertEquals(List.of("A"), AccessPolicy.platformsWithAccess(locked, PLATFORMS));
	}

	@Test
	public void sessionPlatformsAreRestrictedWhenLocked() {
		Principal locked = account("A", new RoleGrant(Role.PLATFORM, null));
		Principal lockedScopedDisplays = account("A", new RoleGrant(Role.ANNOUNCER, "A"), new RoleGrant(Role.DISPLAYS, "B"));
		Principal lockedScopedOperator = account("A", new RoleGrant(Role.ANNOUNCER, "A"));

		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(locked, PLATFORMS));
		assertEquals(List.of("A", "B"), AccessPolicy.allowedSessionPlatforms(lockedScopedDisplays, PLATFORMS));
		assertEquals(List.of("A"), AccessPolicy.allowedSessionPlatforms(lockedScopedOperator, PLATFORMS));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(Principal.displays(), PLATFORMS));
	}

	@Test
	public void breakManagementCapability() {
		Principal timekeeperA = account("A", new RoleGrant(Role.TIMEKEEPER, "A"));
		Principal marshalA = account("A", new RoleGrant(Role.MARSHAL, "A"));
		Principal tcA = account("A", new RoleGrant(Role.TC, "A"));
		Principal juryA = account("A", new RoleGrant(Role.JURY, "A"));

		assertTrue(AccessPolicy.hasCapability(timekeeperA, Capability.BREAK_MANAGEMENT, "A"));
		assertTrue(AccessPolicy.hasCapability(marshalA, Capability.BREAK_MANAGEMENT, "A"));
		assertFalse(AccessPolicy.hasCapability(timekeeperA, Capability.BREAK_MANAGEMENT, "B"));
		assertFalse(AccessPolicy.hasCapability(tcA, Capability.BREAK_MANAGEMENT, "A"));
		assertFalse(AccessPolicy.hasCapability(juryA, Capability.BREAK_MANAGEMENT, "A"));
	}
}
