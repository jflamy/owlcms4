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

	private static PageRule page(boolean platformBound, Access... accesses) {
		Set<Access> set = EnumSet.noneOf(Access.class);
		set.addAll(List.of(accesses));
		return PageRule.accesses(set, platformBound);
	}

	@Test
	public void compositeRolesExpandToBaseRoles() {
		assertTrue(Role.ADMIN.accesses().contains(Access.ADMINISTRATOR));
		assertTrue(Role.ADMIN.accesses().containsAll(EnumSet.of(Access.PREPARATION, Access.DISPLAYS, Access.REFEREE)));
		assertEquals(EnumSet.of(Access.RESULTS), Role.RESULTS.accesses());
		assertTrue("the retired platform bundle gives nothing", Role.PLATFORM.accesses().isEmpty());
		assertTrue(Role.PLATFORM.isObsolete());
		assertEquals(Role.grantableRoles().size() + 1, Role.values().length);
		assertEquals(EnumSet.of(Access.ANNOUNCER, Access.SPEAKER, Access.MARSHAL, Access.TIMEKEEPER,
		        Access.TC, Access.DISPLAYS),
		        Role.ANNOUNCER.accesses());
		assertEquals(EnumSet.of(Access.SPEAKER), Role.SPEAKER.accesses());
		assertEquals(EnumSet.of(Access.JURY), Role.JURY.accesses());
		assertEquals(EnumSet.of(Access.RESULTS, Access.STREAMING), Role.EXTERNAL.accesses());
		assertEquals(EnumSet.of(Access.STREAMING), Role.STREAMING.accesses());
		assertTrue(Role.ADMIN.accesses().contains(Access.STREAMING));
		assertTrue(Role.COMPETITION_SUPERVISOR.accesses().contains(Access.STREAMING));
		assertEquals(EnumSet.of(Access.PREPARATION, Access.RESULTS, Access.DISPLAYS),
		        Role.COMPETITION_DIRECTOR.accesses());
	}

	@Test
	public void athleteEditingPermissionsDoNotGrantNavigation() {
		for (Role role : List.of(Role.PREPARATION, Role.REGISTRATION, Role.WEIGHIN,
		        Role.COMPETITION_DIRECTOR, Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			Principal user = account(null, new RoleGrant(role, null));
			assertTrue(role.name(), AccessPolicy.hasPermission(user, Permission.METADATA_EDITING, null));
		}
		for (Role role : List.of(Role.ANNOUNCER, Role.MARSHAL,
		        Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.ATHLETE_CARD_EDITING, "A"));
		}
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.TC, Role.TIMEKEEPER, Role.REFEREE, Role.JURY,
		        Role.SPEAKER)) {
			Principal user = account(null, new RoleGrant(role, null));
			assertFalse(role.name(), AccessPolicy.hasPermission(user, Permission.METADATA_EDITING, "A"));
			assertFalse(role.name(), AccessPolicy.hasPermission(user, Permission.ATHLETE_CARD_EDITING, "A"));
		}
		for (Role role : List.of(Role.ANNOUNCER, Role.TIMEKEEPER, Role.MARSHAL, Role.COMPETITION_SUPERVISOR,
		        Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.ATHLETE_TIMER, "A"));
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.BREAK_TIMER, "A"));
		}
		for (Role role : List.of(Role.SPEAKER, Role.TC, Role.JURY, Role.REFEREE, Role.DISPLAYS, Role.RESULTS)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.ATHLETE_TIMER, "A"));
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.BREAK_TIMER, "A"));
		}
		for (Role role : List.of(Role.ANNOUNCER, Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.DECISION_BUTTONS, "A"));
		}
		for (Role role : List.of(Role.SPEAKER, Role.TIMEKEEPER, Role.MARSHAL, Role.TC, Role.JURY, Role.REFEREE)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.DECISION_BUTTONS, "A"));
		}
		for (Role role : List.of(Role.ANNOUNCER, Role.PREPARATION, Role.COMPETITION_DIRECTOR,
		        Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name() + " opens the card with results editing on",
			        AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			                Permission.ATHLETE_RESULTS_EDITING, "A"));
		}
		for (Role role : List.of(Role.MARSHAL, Role.SPEAKER, Role.TIMEKEEPER, Role.REGISTRATION, Role.WEIGHIN)) {
			assertFalse(role.name() + " does not start with results editing on",
			        AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			                Permission.ATHLETE_RESULTS_EDITING, "A"));
		}
		for (Role role : List.of(Role.PREPARATION, Role.COMPETITION_DIRECTOR)) {
			assertTrue(role.name() + " edits cards through the preparation access",
			        AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			                Permission.ATHLETE_CARD_EDITING, "A"));
		}
		for (Role role : List.of(Role.REGISTRATION, Role.WEIGHIN)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.ATHLETE_CARD_EDITING, "A"));
		}
		Principal preparation = account(null, new RoleGrant(Role.PREPARATION, null));
		assertTrue(AccessPolicy.canOpen(preparation, page(false, Access.RESULTS), null));
		assertTrue(AccessPolicy.canOpen(preparation, page(false, Access.REGISTRATION), null));
		assertFalse(AccessPolicy.canOpen(preparation, page(false, Access.WEIGHIN), null));
		assertFalse(AccessPolicy.requiresPlatformChoice(preparation));
		assertTrue(AccessPolicy.loginPlatformChoices(preparation, PLATFORMS).isEmpty());
	}

	@Test
	public void cardEditingRespectsOperatorScopeButPreparationAndSupervisorRemainGlobal() {
		Principal marshal = account("A", new RoleGrant(Role.MARSHAL, null), new RoleGrant(Role.RESULTS, null));
		assertTrue(AccessPolicy.hasPermission(marshal, Permission.ATHLETE_CARD_EDITING, "A"));
		assertFalse(AccessPolicy.hasPermission(marshal, Permission.ATHLETE_CARD_EDITING, "B"));
		Principal scoped = account(null, new RoleGrant(Role.MARSHAL, "A"));
		assertFalse(AccessPolicy.hasPermission(scoped, Permission.ATHLETE_CARD_EDITING, "B"));
		for (Role role : List.of(Role.COMPETITION_SUPERVISOR, Role.PREPARATION)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.ATHLETE_CARD_EDITING, "B"));
		}
		Principal token = new Principal(AuthSource.TOKEN, 1L, "token",
		        List.of(new RoleGrant(Role.MARSHAL, "A")), null);
		assertTrue(AccessPolicy.hasPermission(token, Permission.ATHLETE_CARD_EDITING, "A"));
		assertFalse(AccessPolicy.hasPermission(token, Permission.ATHLETE_CARD_EDITING, "B"));
	}

	@Test
	public void adminReachesEveryAccess() {
		assertEquals(EnumSet.allOf(Access.class), Role.ADMIN.accesses());
	}


	@Test
	public void pinModePrincipalsOpenWhatTheyDidBefore() {
		Principal officials = Principal.admin(AuthSource.OFFICIALS_PIN);
		Principal displays = Principal.displays();
		PageRule officialsPage = page(false, Access.PREPARATION);
		PageRule adminPage = page(false, Access.ADMINISTRATOR);
		PageRule boardPage = page(true, Access.DISPLAYS);
		PageRule juryBoard = page(true, Access.DISPLAYS, Access.JURY);

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
		PageRule announcerPage = page(true, Access.ANNOUNCER);
		PageRule navigation = page(false, Access.ANNOUNCER, Access.MARSHAL);

		assertTrue(AccessPolicy.canOpen(announcerA, announcerPage, "A"));
		assertFalse(AccessPolicy.canOpen(announcerA, announcerPage, "B"));
		assertTrue(AccessPolicy.canOpen(announcerA, navigation, null));
	}

	@Test
	public void lockableRolesFollowLoginPlatformButDisplaysDoNot() {
		Principal platformAll = account("A", new RoleGrant(Role.ANNOUNCER, null), new RoleGrant(Role.DISPLAYS, null));

		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Access.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(platformAll, page(true, Access.ANNOUNCER), "B"));
		assertTrue(AccessPolicy.canOpen(platformAll, page(true, Access.DISPLAYS), "B"));
	}

	@Test
	public void theLockBelongsToTheRoleNotToItsAccesses() {
		assertEquals(EnumSet.of(Role.ANNOUNCER, Role.SPEAKER, Role.MARSHAL, Role.TIMEKEEPER, Role.TC, Role.JURY,
		        Role.REFEREE),
		        EnumSet.copyOf(java.util.Arrays.stream(Role.values()).filter(Role::isLockable).toList()));
		for (Role role : Role.values()) {
			assertTrue(role.name() + " is lockable only if scopable", !role.isLockable() || role.isPlatformScopable());
		}
		Principal announcer = account("A", new RoleGrant(Role.ANNOUNCER, null));
		assertTrue(AccessPolicy.isLockedFor(announcer, EnumSet.of(Access.ANNOUNCER)));
		assertTrue("all access rights of a lockable role are locked together",
		        AccessPolicy.isLockedFor(announcer, EnumSet.of(Access.DISPLAYS)));
		Principal speaker = account("A", new RoleGrant(Role.SPEAKER, null));
		assertTrue(AccessPolicy.isLockedFor(speaker, EnumSet.of(Access.SPEAKER)));
		assertFalse("a page the lockable roles do not give is not locked",
		        AccessPolicy.isLockedFor(speaker, EnumSet.of(Access.DISPLAYS)));
		Principal displaysToo = account("A", new RoleGrant(Role.SPEAKER, null), new RoleGrant(Role.DISPLAYS, null));
		assertFalse(AccessPolicy.isLockedFor(displaysToo, EnumSet.of(Access.DISPLAYS)));
		assertFalse(AccessPolicy.isLockedFor(account("A", new RoleGrant(Role.COMPETITION_SUPERVISOR, null)),
		        EnumSet.of(Access.ANNOUNCER)));
		assertFalse("global roles are never locked",
		        AccessPolicy.isLockedFor(account("A", new RoleGrant(Role.PREPARATION, null)),
		                EnumSet.of(Access.PREPARATION)));
	}

	@Test
	public void adminIsNeverLockedAndNeverAsksForAPlatform() {
		Principal admin = account("A", new RoleGrant(Role.ADMIN, null));
		Principal unlockedAdmin = account(null, new RoleGrant(Role.ADMIN, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(admin));
		assertFalse(AccessPolicy.isLocked(admin));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(admin, PLATFORMS));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(true, Access.ANNOUNCER), "C"));
		assertTrue(AccessPolicy.canOpen(unlockedAdmin, page(false, Access.ADMINISTRATOR), null));
		assertTrue(AccessPolicy.canOpen(admin, page(true, Access.ANNOUNCER), "C"));
	}

	@Test
	public void competitionDirectorAccessIsNotGrantedToOrdinaryOfficials() {
		PageRule directorPage = page(true, Access.SUPERVISOR);
		Principal director = account("A", new RoleGrant(Role.COMPETITION_SUPERVISOR, null));
		Principal admin = account(null, new RoleGrant(Role.ADMIN, null));
		Principal announcer = account("A", new RoleGrant(Role.ANNOUNCER, "A"));
		Principal platform = account("A", new RoleGrant(Role.MARSHAL, null));

		assertTrue(AccessPolicy.canOpen(director, directorPage, "B"));
		assertTrue(AccessPolicy.canOpen(admin, directorPage, "A"));
		assertTrue(AccessPolicy.canOpen(Principal.admin(AuthSource.OFFICIALS_PIN), directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(announcer, directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(platform, directorPage, "A"));
		assertFalse(AccessPolicy.canOpen(Principal.displays(), directorPage, "A"));
		assertFalse(AccessPolicy.requiresPlatformChoice(director));
		assertFalse(AccessPolicy.isLocked(director));
		assertEquals(PLATFORMS, AccessPolicy.allowedSessionPlatforms(director, PLATFORMS));
		assertTrue(AccessPolicy.canOpen(director, page(true, Access.ANNOUNCER), "C"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Access.ADMINISTRATOR), null));
	}

	@Test
	public void competitionDirectorHasPreparationAndResultsOnly() {
		Principal director = account(null, new RoleGrant(Role.COMPETITION_DIRECTOR, null));

		assertTrue(AccessPolicy.canOpen(director, page(false, Access.PREPARATION), null));
		assertTrue(AccessPolicy.canOpen(director, page(false, Access.RESULTS), null));
		assertTrue(AccessPolicy.canOpen(director, page(true, Access.DISPLAYS), "B"));
		assertFalse(AccessPolicy.canOpen(director, page(true, Access.SUPERVISOR), "A"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Access.REGISTRATION), null));
		assertFalse(AccessPolicy.canOpen(director, page(false, Access.WEIGHIN), null));
		assertFalse(AccessPolicy.canOpen(director, page(true, Access.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(director, page(false, Access.ADMINISTRATOR), null));
	}

	@Test
	public void resultsAccountOpensResultsOnlyWithoutPlatformChoice() {
		Principal results = account(null, new RoleGrant(Role.RESULTS, null));

		assertFalse(AccessPolicy.requiresPlatformChoice(results));
		assertTrue(AccessPolicy.canOpen(results, page(false, Access.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(results, page(false, Access.DISPLAYS, Access.STREAMING), null));
		assertFalse(AccessPolicy.canOpen(results, page(true, Access.DISPLAYS, Access.STREAMING), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(true, Access.DISPLAYS), "B"));
		assertFalse(AccessPolicy.canOpen(results, page(true, Access.ANNOUNCER), "B"));
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

		assertEquals(List.of(), AccessPolicy.selectablePlatforms(announcerZ, EnumSet.of(Access.ANNOUNCER), PLATFORMS));
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
		Principal locked = account("A", new RoleGrant(Role.ANNOUNCER, null), new RoleGrant(Role.DISPLAYS, null));
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
		PageRule board = page(true, Access.DISPLAYS, Access.STREAMING);

		assertTrue(AccessPolicy.canOpen(external, board, "A"));
		assertTrue(AccessPolicy.canOpen(external, board, "B"));
		assertTrue(AccessPolicy.canOpen(external, page(false, Access.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(external, page(true, Access.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(external, page(true, Access.DISPLAYS, Access.JURY), "A"));
		assertFalse(AccessPolicy.canOpen(external, page(true, Access.DISPLAYS), "A"));
		assertTrue(AccessPolicy.canOpen(external, page(true, Access.DISPLAYS, Access.JURY, Access.STREAMING), "A"));
		assertEquals(PLATFORMS, AccessPolicy.selectablePlatforms(external,
		        EnumSet.of(Access.DISPLAYS, Access.STREAMING), PLATFORMS));
	}

	@Test
	public void streamingOpensStreamingPagesOnEveryPlatformButNothingElse() {
		Principal streaming = account(null, new RoleGrant(Role.STREAMING, null));
		PageRule board = page(true, Access.DISPLAYS, Access.STREAMING);

		assertTrue(AccessPolicy.canOpen(streaming, board, "A"));
		assertTrue(AccessPolicy.canOpen(streaming, board, "B"));
		assertTrue(AccessPolicy.canOpen(streaming, page(true, Access.DISPLAYS, Access.JURY, Access.STREAMING), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(false, Access.RESULTS), null));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Access.ANNOUNCER), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Access.DISPLAYS), "A"));
		assertFalse(AccessPolicy.canOpen(streaming, page(true, Access.DISPLAYS, Access.JURY), "A"));
		assertEquals(PLATFORMS, AccessPolicy.selectablePlatforms(streaming,
		        EnumSet.of(Access.DISPLAYS, Access.STREAMING), PLATFORMS));
	}

	@Test
	public void externalAndStreamingAreReadOnlyAndNeverAskForAPlatform() {
		for (Role role : List.of(Role.EXTERNAL, Role.STREAMING)) {
			Principal principal = account(null, new RoleGrant(role, null));

			assertFalse(role + " needs no platform choice", AccessPolicy.requiresPlatformChoice(principal));
			assertFalse(role + " has no display input",
			        AccessPolicy.hasPermission(principal, Permission.DISPLAY_INPUT, "A"));
		}
	}

	@Test
	public void displayInputIsReservedToDisplaysHolders() {
		for (Role role : List.of(Role.DISPLAYS, Role.ANNOUNCER, Role.ADMIN)) {
			assertTrue(role + " keeps display input", AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.DISPLAY_INPUT, "A"));
		}
		for (Role role : List.of(Role.RESULTS, Role.STREAMING, Role.EXTERNAL, Role.SPEAKER)) {
			assertFalse(role + " has no display input", AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.DISPLAY_INPUT, "A"));
		}
		assertFalse(AccessPolicy.hasPermission(null, Permission.DISPLAY_INPUT, "A"));
	}

	@Test
	public void externalWithScopedDisplaysHasInputOnlyOnThatPlatform() {
		Principal mixed = account(null, new RoleGrant(Role.EXTERNAL, null), new RoleGrant(Role.DISPLAYS, "A"));

		assertTrue(AccessPolicy.hasPermission(mixed, Permission.DISPLAY_INPUT, "A"));
		assertFalse(AccessPolicy.hasPermission(mixed, Permission.DISPLAY_INPUT, "B"));
	}

	@Test
	public void databaseExportsNeedResults() {
		for (Role role : List.of(Role.RESULTS, Role.PREPARATION, Role.COMPETITION_DIRECTOR,
		        Role.COMPETITION_SUPERVISOR, Role.EXTERNAL, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.EXPORT_DATA, null));
		}
		for (Role role : List.of(Role.REGISTRATION, Role.WEIGHIN, Role.DISPLAYS)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.EXPORT_DATA, null));
		}
	}

	@Test
	public void checkpointsNeedTheCompetitionSupervisor() {
		for (Role role : List.of(Role.COMPETITION_SUPERVISOR, Role.ADMIN)) {
			assertTrue(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.CHECKPOINT, null));
		}
		for (Role role : List.of(Role.RESULTS, Role.EXTERNAL, Role.COMPETITION_DIRECTOR, Role.MARSHAL)) {
			assertFalse(role.name(), AccessPolicy.hasPermission(account(null, new RoleGrant(role, null)),
			        Permission.CHECKPOINT, null));
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
