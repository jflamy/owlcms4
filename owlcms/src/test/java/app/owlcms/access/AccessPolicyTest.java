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

	private static PageRule page(boolean platformBound, Permission... permissions) {
		Set<Permission> set = EnumSet.noneOf(Permission.class);
		set.addAll(List.of(permissions));
		return PageRule.permissions(set, platformBound);
	}

	@Test
	public void compositeRolesExpandToBaseRoles() {
		assertTrue(Role.ADMIN.permissions().contains(Permission.ADMIN_PAGES));
		assertTrue(Role.ADMIN.permissions().containsAll(EnumSet.of(Permission.PREPARATION, Permission.DISPLAYS, Permission.REFEREE)));
		assertEquals(EnumSet.of(Permission.RESULTS), Role.RESULTS.permissions());
		assertEquals(EnumSet.of(Permission.ANNOUNCER, Permission.MARSHAL, Permission.TIMEKEEPER, Permission.TC, Permission.DISPLAYS,
		        Permission.ATHLETE_CARD_EDITING),
		        Role.PLATFORM.permissions());
		assertEquals(EnumSet.of(Permission.JURY), Role.JURY.permissions());
		assertEquals(EnumSet.of(Permission.RESULTS, Permission.STREAMING), Role.EXTERNAL.permissions());
		assertEquals(EnumSet.of(Permission.STREAMING), Role.STREAMING.permissions());
		assertTrue(Role.ADMIN.permissions().contains(Permission.STREAMING));
		assertTrue(Role.COMPETITION_SUPERVISOR.permissions().contains(Permission.STREAMING));
		assertEquals(EnumSet.of(Permission.PREPARATION, Permission.RESULTS, Permission.DISPLAYS,
		        Permission.METADATA_EDITING), Role.COMPETITION_DIRECTOR.permissions());
	}

	@Test
	public void athleteEditingCapabilitiesDoNotGrantNavigation() {
		for (Role role : List.of(Role.PREPARATION, Role.REGISTRATION, Role.WEIGHIN,
		        Role.COMPETITION_DIRECTOR, Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			Principal user = account(null, new RoleGrant(role, null));
			assertTrue(role.name(), AccessPolicy.hasCapability(user, Capability.METADATA_EDITING, null));
		}
		for (Role role : List.of(Role.PREPARATION, Role.ANNOUNCER, Role.MARSHAL, Role.PLATFORM,
		        Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.ATHLETE_CARD_EDITING, "A"));
		}
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.TC, Role.TIMEKEEPER, Role.REFEREE, Role.JURY)) {
			Principal user = account(null, new RoleGrant(role, null));
			assertFalse(role.name(), AccessPolicy.hasCapability(user, Capability.METADATA_EDITING, "A"));
			assertFalse(role.name(), AccessPolicy.hasCapability(user, Capability.ATHLETE_CARD_EDITING, "A"));
		}
		assertFalse("metadata only: no Card writes",
		        AccessPolicy.hasCapability(account(null, new RoleGrant(Role.COMPETITION_DIRECTOR, null)),
		                Capability.ATHLETE_CARD_EDITING, "A"));
		for (Role role : List.of(Role.REGISTRATION, Role.WEIGHIN)) {
			assertFalse(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.ATHLETE_CARD_EDITING, "A"));
		}
		Principal preparation = account(null, new RoleGrant(Role.PREPARATION, null));
		assertFalse(AccessPolicy.canOpen(preparation, page(false, Permission.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(preparation, page(false, Permission.REGISTRATION), null));
		assertFalse(AccessPolicy.requiresPlatformChoice(preparation));
		assertTrue(AccessPolicy.loginPlatformChoices(preparation, PLATFORMS).isEmpty());
	}

	@Test
	public void cardEditingRespectsOperatorScopeButPreparationRemainsGlobal() {
		Principal marshal = account("A", new RoleGrant(Role.MARSHAL, null), new RoleGrant(Role.RESULTS, null));
		assertTrue(AccessPolicy.hasCapability(marshal, Capability.ATHLETE_CARD_EDITING, "A"));
		assertFalse(AccessPolicy.hasCapability(marshal, Capability.ATHLETE_CARD_EDITING, "B"));
		Principal scoped = account(null, new RoleGrant(Role.MARSHAL, "A"));
		assertFalse(AccessPolicy.hasCapability(scoped, Capability.ATHLETE_CARD_EDITING, "B"));
		assertTrue(AccessPolicy.hasCapability(account(null, new RoleGrant(Role.PREPARATION, null)),
		        Capability.ATHLETE_CARD_EDITING, "B"));
		Principal token = new Principal(AuthSource.TOKEN, 1L, "token",
		        List.of(new RoleGrant(Role.MARSHAL, "A")), null);
		assertTrue(AccessPolicy.hasCapability(token, Capability.ATHLETE_CARD_EDITING, "A"));
		assertFalse(AccessPolicy.hasCapability(token, Capability.ATHLETE_CARD_EDITING, "B"));
	}

	@Test
	public void adminReachesEveryPermission() {
		assertEquals(EnumSet.allOf(Permission.class), Role.ADMIN.permissions());
	}


	@Test
	public void pinModePrincipalsOpenWhatTheyDidBefore() {
		Principal officials = Principal.admin(AuthSource.OFFICIALS_PIN);
		Principal displays = Principal.displays();
		PageRule officialsPage = page(false, Permission.PREPARATION);
		PageRule adminPage = page(false, Permission.ADMIN_PAGES);
		PageRule boardPage = page(true, Permission.DISPLAYS);
		PageRule juryBoard = page(true, Permission.DISPLAYS, Permission.JURY);

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
		assertFalse(AccessPolicy.canOpen(Principal.admin(AuthSource.NO_PIN), unannotated, null));
	}

	@Test
	public void scopeLimitsPlatformBoundPagesOnly() {
		Principal announcerA = account(null, new RoleGrant(Role.ANNOUNCER, "A"));
		PageRule announcerPage = page(true, Permission.ANNOUNCER);
		PageRule navigation = page(false, Permission.ANNOUNCER, Permission.MARSHAL);

		assertTrue(AccessPolicy.canOpen(announcerA, announcerPage, "A"));
		assertFalse(AccessPolicy.canOpen(announcerA, announcerPage, "B"));
		assertTrue(AccessPolicy.canOpen(announcerA, navigation, null));
	}

	@Test
	public void lockableRolesFollowLoginPlatformButDisplaysDoNot() {
		Principal platformAll = account("A", new RoleGrant(Role.PLATFORM, null));

		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Permission.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(platformAll, page(true, Permission.ANNOUNCER), "B"));
		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Permission.DISPLAYS), "B"));
	}

	@Test
	public void adminIsNeverLockedAndNeverAsksForAPlatform() {
		Principal admin = account("A", new RoleGrant(Role.ADMIN, null));
		Principal unlockedAdmin = account(null, new RoleGrant(Role.ADMIN, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(admin));
		assertFalse(AccessPolicy.isLocked(admin));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(admin, PLATFORMS));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(true, Permission.ANNOUNCER), "C"));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(false, Permission.ADMIN_PAGES), null));
		assertTrue(AccessPolicy.canOpen(admin, page(true, Permission.ANNOUNCER), "C"));
	}

	@Test
	public void competitionDirectorAccessIsNotGrantedToOrdinaryOfficials() {
		PageRule directorPage = page(true, Permission.COMPETITION_SUPERVISOR);
		Principal director = account("A", new RoleGrant(Role.COMPETITION_SUPERVISOR, null));
		Principal admin = account(null, new RoleGrant(Role.ADMIN, null));
		Principal announcer = account("A", new RoleGrant(Role.ANNOUNCER, "A"));
		Principal platform = account("A", new RoleGrant(Role.PLATFORM, null));

		assertTrue(AccessPolicy.canOpen(director, directorPage, "B"));
		assertTrue(AccessPolicy.canOpen(admin, directorPage, "A"));
		assertTrue(AccessPolicy.canOpen(Principal.admin(AuthSource.OFFICIALS_PIN), directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(announcer, directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(platform, directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(Principal.displays(), directorPage, "A"));
		assertFalse(AccessPolicy.requiresPlatformChoice(director));
		assertFalse(AccessPolicy.isLocked(director));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(director, PLATFORMS));
		assertTrue(AccessPolicy.canOpen(director, page(true, Permission.ANNOUNCER), "C"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Permission.ADMIN_PAGES), null));
	}

	@Test
	public void competitionDirectorHasPreparationAndResultsOnly() {
		Principal director = account(null, new RoleGrant(Role.COMPETITION_DIRECTOR, null));

		assertTrue(AccessPolicy.canOpen(director, page(false, Permission.PREPARATION), null));
		assertTrue(AccessPolicy.canOpen(director, page(false, Permission.RESULTS), null));
		assertTrue(AccessPolicy.canOpen(director, page(true, Permission.DISPLAYS), "B"));
		assertFalse(AccessPolicy.canOpen(director, page(true, Permission.COMPETITION_SUPERVISOR), "A"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Permission.REGISTRATION), null));
		assertFalse(AccessPolicy.canOpen(director, page(false, Permission.WEIGHIN), null));
		assertFalse(AccessPolicy.canOpen(director, page(true, Permission.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Permission.ADMIN_PAGES), null));
	}

	@Test
	public void resultsAccountOpensResultsOnlyWithoutPlatformChoice() {
		Principal results = account(null, new RoleGrant(Role.RESULTS, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(results));
		assertTrue(AccessPolicy.canOpen(results, page(false, Permission.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(results, page(false, Permission.DISPLAYS, Permission.STREAMING), null));
		assertFalse(AccessPolicy.canOpen(results, page(true, Permission.DISPLAYS, Permission.STREAMING), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(true, Permission.DISPLAYS), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(true, Permission.ANNOUNCER), "B"));
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

		assertEquals(List.of(), AccessPolicy.selectablePlatforms(announcerZ, EnumSet.of(Permission.ANNOUNCER), PLATFORMS));
		assertEquals(List.of(), AccessPolicy.loginPlatformChoices(announcerZ, PLATFORMS));
	}

	@Test
	public void loginAuditPlatformsFollowEffectivePlatformAccess() {
		Principal scoped = account(null, new RoleGrant(Role.ANNOUNCER, "B"));
		Principal results = account(null, new RoleGrant(Role.RESULTS, null));
		Principal preparation = account(null, new RoleGrant(Role.PREPARATION, null));
		Principal locked = account("A", new RoleGrant(Role.ANNOUNCER, null));

		assertEquals(List.of("B"), AccessPolicy.platformsWithAccess(scoped, PLATFORMS));
		assertEquals(List.of(), AccessPolicy.platformsWithAccess(results, PLATFORMS));
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
	public void externalOpensStreamingAndResultsPagesOnEveryPlatformButNothingElse() {
		Principal external = account(null, new RoleGrant(Role.EXTERNAL, null));
		PageRule board = page(true, Permission.DISPLAYS, Permission.STREAMING);

		assertTrue(AccessPolicy.canOpen(external, board, "A"));
		assertTrue(AccessPolicy.canOpen(external, board, "B"));
		assertTrue(AccessPolicy.canOpen(external, page(false, Permission.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(external, page(true, Permission.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(external, page(true, Permission.DISPLAYS, Permission.JURY), "A"));
		assertFalse(AccessPolicy.canOpen(external, page(true, Permission.DISPLAYS), "A"));
		assertTrue(AccessPolicy.canOpen(external, page(true, Permission.DISPLAYS, Permission.JURY, Permission.STREAMING), "A"));
		assertEquals(PLATFORMS, AccessPolicy.selectablePlatforms(external,
		        EnumSet.of(Permission.DISPLAYS, Permission.STREAMING), PLATFORMS));
	}

	@Test
	public void streamingOpensStreamingPagesOnEveryPlatformButNothingElse() {
		Principal streaming = account(null, new RoleGrant(Role.STREAMING, null));
		PageRule board = page(true, Permission.DISPLAYS, Permission.STREAMING);

		assertTrue(AccessPolicy.canOpen(streaming, board, "A"));
		assertTrue(AccessPolicy.canOpen(streaming, board, "B"));
		assertTrue(AccessPolicy.canOpen(streaming, page(true, Permission.DISPLAYS, Permission.JURY, Permission.STREAMING), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(false, Permission.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Permission.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Permission.DISPLAYS), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Permission.DISPLAYS, Permission.JURY), "A"));
		assertEquals(PLATFORMS, AccessPolicy.selectablePlatforms(streaming,
		        EnumSet.of(Permission.DISPLAYS, Permission.STREAMING), PLATFORMS));
	}

	@Test
	public void externalAndStreamingAreReadOnlyAndNeverAskForAPlatform() {
		for (Role role : List.of(Role.EXTERNAL, Role.STREAMING)) {
			Principal principal = account(null, new RoleGrant(role, null));

			assertFalse(role + " needs no platform choice", AccessPolicy.requiresPlatformChoice(principal));
			assertFalse(role + " has no display input", AccessPolicy.hasCapability(principal, Capability.DISPLAY_INPUT, "A"));
		}
	}

	@Test
	public void displayInputIsReservedToDisplaysHolders() {
		for (Role role : List.of(Role.DISPLAYS, Role.PLATFORM, Role.ADMIN)) {
			assertTrue(role + " keeps display input",
			        AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)), Capability.DISPLAY_INPUT, "A"));
		}
		for (Role role : List.of(Role.RESULTS, Role.STREAMING, Role.EXTERNAL)) {
			assertFalse(role + " has no display input",
			        AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)), Capability.DISPLAY_INPUT, "A"));
		}
		assertFalse(AccessPolicy.hasCapability(null, Capability.DISPLAY_INPUT, "A"));
	}

	@Test
	public void externalWithScopedDisplaysHasInputOnlyOnThatPlatform() {
		Principal mixed = account(null, new RoleGrant(Role.EXTERNAL, null), new RoleGrant(Role.DISPLAYS, "A"));

		assertTrue(AccessPolicy.hasCapability(mixed, Capability.DISPLAY_INPUT, "A"));
		assertFalse(AccessPolicy.hasCapability(mixed, Capability.DISPLAY_INPUT, "B"));
	}

	@Test
	public void databaseExportsNeedResults() {
		for (Role role : List.of(Role.RESULTS, Role.COMPETITION_DIRECTOR, Role.COMPETITION_SUPERVISOR, Role.EXTERNAL,
		        Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.EXPORT_DATA, null));
		}
		for (Role role : List.of(Role.PREPARATION, Role.REGISTRATION, Role.WEIGHIN, Role.PLATFORM, Role.DISPLAYS)) {
			assertFalse(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.EXPORT_DATA, null));
		}
	}

	@Test
	public void checkpointsNeedTheCompetitionSupervisor() {
		for (Role role : List.of(Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.CHECKPOINT, null));
		}
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.COMPETITION_DIRECTOR, Role.PLATFORM)) {
			assertFalse(role.name(), AccessPolicy.hasCapability(account(null, new RoleGrant(role, null)),
			        Capability.CHECKPOINT, null));
		}
	}

	@Test
	public void onlyAdminAndCompetitionSupervisorCoverAllPlatforms() {
		for (Role role : Role.values()) {
			assertEquals(role.name(), role == Role.ADMIN || role == Role.COMPETITION_SUPERVISOR,
			        role.coversAllPlatforms());
		}
	}
}
