/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.reassign.AthleteSnapshot;
import app.owlcms.data.agegroup.reassign.ChampionshipReassigner;
import app.owlcms.data.agegroup.reassign.LeaveReason;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.AthleteChange;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.ChampionshipChange;
import app.owlcms.data.agegroup.reassign.ReassignmentPlan.Mode;
import app.owlcms.data.agegroup.reassign.ReassignmentPreview;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.jpa.SingletonSaves;

/**
 * Championship reassignment engine (compliance item 200, phase 2). Domain code only, no Vaadin objects. Fixture as
 * in {@link ChampionshipDeleteTest}; athletes and age groups added programmatically where a case is missing.
 */
public class ChampionshipReassignmentTest {

	private static final String FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db";
	private static Path fixtureDirectory;

	@BeforeClass
	public static void setupTests() throws Exception {
		Main.injectSuppliers();
		String memoryJdbcUrl = "jdbc:h2:mem:championshipReassignmentTest-" + UUID.randomUUID()
		        + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=4";
		System.setProperty("JDBC_DATABASE_URL", memoryJdbcUrl);
		loadFixtureIntoMemoryDatabase(memoryJdbcUrl);
		JPAService.init(true, false);
		Config.initConfig();
		Championship.reset();
		Gender.initPublicGenderCodeMapString(Locale.ENGLISH);
	}

	@AfterClass
	public static void tearDownTests() throws Exception {
		try {
			Championship.reset();
		} catch (Exception e) {
			// DB may already be closed if setup failed
		}
		Competition.setCurrent(null);
		JPAService.close();
		System.clearProperty("JDBC_DATABASE_URL");
		deleteFixtureDirectory();
	}

	/** Athlete ages are year differences; pin the reference date so programmatic birth years are exact. */
	@Before
	public void competitionExists() {
		if (CompetitionRepository.findAll().isEmpty()) {
			CompetitionRepository.create(new Competition(), AuditOrigin.system("test"));
		}
		Competition current = Competition.getCurrent();
		if (current != null && !LocalDate.now().equals(current.getCompetitionDate())) {
			current.setCompetitionDate(LocalDate.now());
		}
	}

	// ---------------------------------------------------------------------------------------------
	// championship scope (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** Narrowing M35 moves a 39-year-old to M40, inside Masters; a fixture athlete elsewhere is untouched. */
	@Test
	public void mastersMoveBetweenAgeGroups() {
		Long fixtureAthlete = findFixtureAthleteId("POURAMIN", "IRI");
		assertNotNull("fixture athlete expected", fixtureAthlete);
		Long fixtureRegistration = registrationId(fixtureAthlete);
		int fixtureParticipations = participationCount(fixtureAthlete);

		Long m35 = newAgeGroup("SCM35", "ScopeM", Gender.M, 35, 39, true, 0, 73, 81, 999);
		Long m40 = newAgeGroup("SCM40", "ScopeM", Gender.M, 40, 44, true, 0, 81, 999);
		Map<Double, Long> m35Cats = categoryIdsByMax(m35);
		Map<Double, Long> m40Cats = categoryIdsByMax(m40);
		Long s1 = newAthlete("ScopeOne", Gender.M, 39, 80.0, 100);
		participate(s1, m35Cats.get(81.0), true, false);
		setRegistration(s1, m35Cats.get(81.0));

		ReassignmentPlan plan = reassignWithin("ScopeM", em -> {
			findAgeGroup(em, "SCM35", Gender.M).setMaxAge(38);
			findAgeGroup(em, "SCM40", Gender.M).setMinAge(39);
		});

		AthleteChange change = changeOf(plan, s1);
		assertNotNull("the athlete is in scope", change);
		ChampionshipChange cc = change.byChampionship().get("ScopeM");
		assertNotNull(cc);
		assertTrue("nothing kept: 39 no longer fits M35", cc.keptCategoryIds().isEmpty());
		assertTrue(cc.removedCategoryIds().contains(m35Cats.get(81.0)));
		assertEquals(List.of(m40Cats.get(81.0)),
		        cc.addedCategories().stream().map(Category::getId).toList());
		assertFalse(cc.leaves());

		Map<Long, boolean[]> flags = flagsByCategoryId(s1);
		assertFalse(flags.containsKey(m35Cats.get(81.0)));
		assertTrue(flags.containsKey(m40Cats.get(81.0)));
		assertEquals("moved participation gets the import default flags",
		        !FeatureSwitch.EXPLICIT_TEAMS.isEffective(), flags.get(m40Cats.get(81.0))[0]);
		assertFalse(flags.get(m40Cats.get(81.0))[1]);
		assertEquals(m40Cats.get(81.0), registrationId(s1));

		assertEquals("another championship is never touched", fixtureRegistration, registrationId(fixtureAthlete));
		assertEquals(fixtureParticipations, participationCount(fixtureAthlete));
	}

	/** Deactivating M45 ejects a 46-year-old from Masters with reason INACTIVE; his Open participation stays. */
	@Test
	public void deactivationLeavesWithInactiveReason() {
		Long masters45 = newAgeGroup("DCM45", "ScopeM45", Gender.M, 45, 49, true, 0, 999);
		Long open = newAgeGroup("DCOPEN", "ScopeOpen", Gender.M, 15, 999, true, 0, 999);
		Long mastersCat = categoryIdsByMax(masters45).get(999.0);
		Long openCat = categoryIdsByMax(open).get(999.0);
		Long s2 = newAthlete("ScopeTwo", Gender.M, 46, 90.0, 100);
		participate(s2, mastersCat, true, false);
		participate(s2, openCat, true, false);
		setRegistration(s2, mastersCat);

		ReassignmentPreview preview = previewWithin("ScopeM45",
		        em -> findAgeGroup(em, "DCM45", Gender.M).setActive(false));
		List<ReassignmentPreview.Entry> leavers = preview.leavers("ScopeM45");
		assertEquals(1, leavers.size());
		assertEquals(s2, leavers.get(0).athleteId());
		assertEquals(LeaveReason.INACTIVE, leavers.get(0).reason());
		assertTrue("the dry run wrote nothing",
		        flagsByCategoryId(s2).containsKey(mastersCat));

		reassignWithin("ScopeM45", null);
		Map<Long, boolean[]> flags = flagsByCategoryId(s2);
		assertFalse(flags.containsKey(mastersCat));
		assertTrue("other championships keep their participations", flags.containsKey(openCat));
		assertEquals(openCat, registrationId(s2));
	}

	/** Correcting the M30 30–35 overlap to 30–34 drops nobody from Masters: 35-year-olds keep M35. */
	@Test
	public void overlapCorrectionDropsNobody() {
		Long m30 = newAgeGroup("OM30", "OverlapM", Gender.M, 30, 35, true, 0, 999);
		Long m35 = newAgeGroup("OM35", "OverlapM", Gender.M, 35, 39, true, 0, 999);
		Long m30Cat = categoryIdsByMax(m30).get(999.0);
		Long m35Cat = categoryIdsByMax(m35).get(999.0);
		Long o1 = newAthlete("OverlapOne", Gender.M, 35, 90.0, 100);
		participate(o1, m30Cat, true, false);
		participate(o1, m35Cat, false, false);
		setRegistration(o1, m35Cat);

		ReassignmentPlan plan = reassignWithin("OverlapM",
		        em -> findAgeGroup(em, "OM30", Gender.M).setMaxAge(34));

		AthleteChange change = changeOf(plan, o1);
		assertNotNull(change);
		ChampionshipChange cc = change.byChampionship().get("OverlapM");
		assertFalse(cc.leaves());
		assertTrue(cc.removedCategoryIds().contains(m30Cat));
		assertTrue(cc.keptCategoryIds().contains(m35Cat));

		Map<Long, boolean[]> flags = flagsByCategoryId(o1);
		assertFalse(flags.containsKey(m30Cat));
		assertTrue(flags.containsKey(m35Cat));
	}

	/** Overlapping age groups of one championship: a 20-year-old in U25 also gets JR. */
	@Test
	public void athleteFittingTwoOverlappingAgeGroupsGetsBoth() {
		Long u25 = newAgeGroup("BU25", "BothU", Gender.M, 15, 25, true, 0, 999);
		Long jr = newAgeGroup("BJR", "BothU", Gender.M, 15, 20, true, 0, 999);
		Long u25Cat = categoryIdsByMax(u25).get(999.0);
		Long jrCat = categoryIdsByMax(jr).get(999.0);
		Long b1 = newAthlete("BothOne", Gender.M, 20, 90.0, 100);
		participate(b1, u25Cat, true, false);
		setRegistration(b1, u25Cat);

		reassignWithin("BothU", null);

		Map<Long, boolean[]> flags = flagsByCategoryId(b1);
		assertTrue(flags.containsKey(u25Cat));
		assertTrue("fits both overlapping age groups", flags.containsKey(jrCat));
		assertEquals("the more specific age group becomes the registration category",
		        jrCat, registrationId(b1));
	}

	// ---------------------------------------------------------------------------------------------
	// QT (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** Raising the QT above the entry total removes the athlete from the championship; dry run says QT. */
	@Test
	public void raisedQtRemovesWithQtReason() {
		Long qtAg = newAgeGroup("QRM35", "QtRaiseM", Gender.M, 35, 39, true, 0, 73, 999);
		Long cat73 = categoryIdsByMax(qtAg).get(73.0);
		SingletonSaves.inTransaction(em -> {
			categoryAtMax(em, qtAg, 999.0).setQualifyingTotal(300);
			em.flush();
			return null;
		});
		Long q1 = newAthlete("QtOne", Gender.M, 36, 70.0, 250);
		participate(q1, cat73, true, false);
		setRegistration(q1, cat73);

		ReassignmentPreview preview = previewWithin("QtRaiseM",
		        em -> categoryAtMax(em, qtAg, 73.0).setQualifyingTotal(280));
		List<ReassignmentPreview.Entry> leavers = preview.leavers("QtRaiseM");
		assertEquals(1, leavers.size());
		assertEquals(q1, leavers.get(0).athleteId());
		assertEquals(LeaveReason.QT, leavers.get(0).reason());
		assertTrue("the dry run wrote nothing", flagsByCategoryId(q1).containsKey(cat73));

		reassignWithin("QtRaiseM", null);
		assertTrue(flagsByCategoryId(q1).isEmpty());
		assertNull(registrationId(q1));
	}

	/** A lowered QT keeps everyone; a missing entry total fails a QT above 0 and ejects from the championship. */
	@Test
	public void loweredQtKeepsAndMissingEntryTotalLeaves() {
		Long qtAg = newAgeGroup("QLM35", "QtLowerM", Gender.M, 35, 39, true, 0, 73, 999);
		Long cat73 = categoryIdsByMax(qtAg).get(73.0);
		SingletonSaves.inTransaction(em -> {
			categoryAtMax(em, qtAg, 73.0).setQualifyingTotal(200);
			em.flush();
			return null;
		});
		Long q2 = newAthlete("QtTwo", Gender.M, 36, 70.0, 280);
		participate(q2, cat73, true, false);
		Long q3 = newAthlete("QtThree", Gender.M, 36, 70.0, null);
		participate(q3, cat73, true, false);

		ReassignmentPlan plan = reassignWithin("QtLowerM",
		        em -> categoryAtMax(em, qtAg, 73.0).setQualifyingTotal(100));

		ChampionshipChange cc = changeOf(plan, q2).byChampionship().get("QtLowerM");
		assertTrue("lowered QT keeps the athlete", cc.keptCategoryIds().contains(cat73));
		assertFalse(cc.leaves());
		ChampionshipChange cc3 = changeOf(plan, q3).byChampionship().get("QtLowerM");
		assertTrue("a missing entry total fails a QT above 0", cc3.removedCategoryIds().contains(cat73));
		// 70 kg fits only the (0, 73] category of a contiguous 73/999 set, so a QT failure leaves the championship
		assertTrue(cc3.leaves());
		assertEquals(LeaveReason.QT, cc3.leaveReason());

		assertTrue(flagsByCategoryId(q2).containsKey(cat73));
		assertTrue(flagsByCategoryId(q3).isEmpty());
	}

	// ---------------------------------------------------------------------------------------------
	// missing data (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** No birth date and no usable weight are reported as such; a presumed weight from the registration fits. */
	@Test
	public void missingDataReasons() {
		Long ag = newAgeGroup("MM35", "MissM", Gender.M, 35, 39, true, 0, 73, 999);
		Long cat73 = categoryIdsByMax(ag).get(73.0);
		Long m1 = newAthlete("MissOne", Gender.M, null, 80.0, 100);
		participate(m1, cat73, true, false);
		setRegistration(m1, cat73);
		Long m2 = newAthlete("MissTwo", Gender.M, 36, null, 100);
		participate(m2, cat73, true, false);
		// no registration: no presumed weight either
		Long m3 = newAthlete("MissThree", Gender.M, 36, null, 100);
		participate(m3, cat73, true, false);
		setRegistration(m3, cat73);

		ReassignmentPreview preview = previewWithin("MissM", null);
		List<ReassignmentPreview.Entry> leavers = preview.leavers("MissM");
		assertEquals(LeaveReason.MISSING_BIRTH_DATE,
		        leavers.stream().filter(e -> e.athleteId().equals(m1)).findFirst().orElseThrow().reason());
		assertEquals(LeaveReason.MISSING_BODY_WEIGHT,
		        leavers.stream().filter(e -> e.athleteId().equals(m2)).findFirst().orElseThrow().reason());
		assertTrue("presumed weight from the registration category fits",
		        leavers.stream().noneMatch(e -> e.athleteId().equals(m3)));

		reassignWithin("MissM", null);
		assertTrue(flagsByCategoryId(m1).isEmpty());
		assertTrue(flagsByCategoryId(m2).isEmpty());
		assertTrue(flagsByCategoryId(m3).containsKey(cat73));
	}

	// ---------------------------------------------------------------------------------------------
	// team flags (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** Unchanged participations keep all four flag combinations, the same row (ranks), and mixed under policy. */
	@Test
	public void keptParticipationsKeepFlagsAndRanks() {
		newChampionshipRow("FlagsM", true);
		Long ag = newAgeGroup("FM35", "FlagsM", Gender.M, 35, 39, true, 0, 81, 999);
		Long cat81 = categoryIdsByMax(ag).get(81.0);
		boolean[][] combos = { { false, false }, { true, false }, { false, true }, { true, true } };
		Long[] athletes = new Long[combos.length];
		for (int i = 0; i < combos.length; i++) {
			athletes[i] = newAthlete("Flag" + i, Gender.M, 36, 80.0, 100);
			participate(athletes[i], cat81, combos[i][0], combos[i][1]);
			setRegistration(athletes[i], cat81);
		}
		setSnatchRank(athletes[0], cat81, 3);

		reassignWithin("FlagsM", null);

		for (int i = 0; i < combos.length; i++) {
			boolean[] flags = flagsByCategoryId(athletes[i]).get(cat81);
			assertNotNull("participation kept", flags);
			assertEquals("team flag kept", combos[i][0], flags[0]);
			assertEquals("mixed flag kept while the championship allows it", combos[i][1], flags[1]);
		}
		assertEquals("same participation row: ranks survive", 3, snatchRank(athletes[0], cat81).intValue());
	}

	/** Turning mixed teams off clears the mixed flag on kept participations; the team flag is unchanged. */
	@Test
	public void mixedFlagClearedWhenChampionshipDisablesMixedTeams() {
		newChampionshipRow("FlagsOffM", true);
		Long ag = newAgeGroup("FOM35", "FlagsOffM", Gender.M, 35, 39, true, 0, 81, 999);
		Long cat81 = categoryIdsByMax(ag).get(81.0);
		Long f5 = newAthlete("FlagFive", Gender.M, 36, 80.0, 100);
		participate(f5, cat81, true, true);
		setRegistration(f5, cat81);

		reassignWithin("FlagsOffM", em -> {
			em.createQuery("select c from Championship c where lower(c.name) = lower(:name)", Championship.class)
			        .setParameter("name", "FlagsOffM").getResultList()
			        .forEach(c -> c.setExplicitMixedTeamMembers(false));
		});

		boolean[] flags = flagsByCategoryId(f5).get(cat81);
		assertNotNull(flags);
		assertTrue("team flag kept", flags[0]);
		assertFalse("mixed flag cleared", flags[1]);
	}

	/** A move to another body-weight category gets the import default flags, never the old mixed flag. */
	@Test
	public void movedParticipationGetsImportDefaultFlags() {
		newChampionshipRow("MoveM", true);
		Long ag = newAgeGroup("VM35", "MoveM", Gender.M, 35, 39, true, 0, 73, 81, 89, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long v1 = newAthlete("MoveOne", Gender.M, 36, 82.0, 100);
		participate(v1, cats.get(89.0), false, true);
		setRegistration(v1, cats.get(89.0));

		// add 85; the caller supplies final definitions with recomputed lower bounds (step 5)
		reassignWithin("MoveM", em -> {
			AgeGroup stored = em.find(AgeGroup.class, ag);
			stored.addCategory(new Category(81.0, 85.0, Gender.M, true, 0, 0, 0, stored, 0));
			categoryAtMax(em, ag, 89.0).setMinimumWeight(85.0);
		});

		Map<Long, boolean[]> flags = flagsByCategoryId(v1);
		assertFalse("old participation removed", flags.containsKey(cats.get(89.0)));
		Long cat85 = categoryIdsByMax(ag).entrySet().stream()
		        .filter(e -> e.getKey() == 85.0).map(Map.Entry::getValue).findFirst().orElseThrow();
		boolean[] moved = flags.get(cat85);
		assertNotNull("moved to the new category", moved);
		assertEquals(!FeatureSwitch.EXPLICIT_TEAMS.isEffective(), moved[0]);
		assertFalse("a moved or new participation is never mixed", moved[1]);
		assertEquals("a new participation row has no ranks", 0, snatchRank(v1, cat85).intValue());
		assertEquals(cat85, registrationId(v1));
	}

	// ---------------------------------------------------------------------------------------------
	// registration category (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** Specificity ties use the final category count: deleting a category can move the registration elsewhere. */
	@Test
	public void registrationPreferenceUsesFinalCategoryCounts() {
		Long zulu = newAgeGroup("ZU20", "Zulu", Gender.M, 15, 20, true, 0, 60, 70, 999);
		Long alpha = newAgeGroup("AL20", "Alpha", Gender.M, 15, 20, true, 0, 65, 999);
		Map<Double, Long> zuluCats = categoryIdsByMax(zulu);
		Map<Double, Long> alphaCats = categoryIdsByMax(alpha);
		Long p1 = newAthlete("PrefOne", Gender.M, 18, 63.0, 100);
		participate(p1, zuluCats.get(70.0), true, false);
		participate(p1, alphaCats.get(65.0), true, false);
		setRegistration(p1, zuluCats.get(70.0));

		reassignWithin("Zulu", null);
		assertEquals("same span, more categories: Zulu stays preferred", zuluCats.get(70.0), registrationId(p1));

		// delete Zulu 70: the caller recomputes the lower limits (999 now starts at 60) and excludes the row;
		// the athlete moves to Zulu's unlimited category, and the final-count tie-break prefers Alpha
		ReassignmentPlan plan = reassignWithin("Zulu",
		        em -> categoryAtMax(em, zulu, 999.0).setMinimumWeight(60.0),
		        Set.of(zuluCats.get(70.0)));
		ChampionshipChange cc = changeOf(plan, p1).byChampionship().get("Zulu");
		assertTrue(cc.removedCategoryIds().contains(zuluCats.get(70.0)));
		assertTrue(cc.addedCategories().stream().anyMatch(c -> c.getId().equals(zuluCats.get(999.0))));

		assertEquals(alphaCats.get(65.0), registrationId(p1));
		assertTrue(flagsByCategoryId(p1).containsKey(zuluCats.get(999.0)));

		// no reference to the deleted category remains, so its row can be removed cleanly (step 7)
		assertEquals("no participation points to the deleted category", 0,
		        categoryParticipationCount(zuluCats.get(70.0)));
		SingletonSaves.inTransaction(em -> {
			AgeGroup stored = em.find(AgeGroup.class, zulu);
			stored.removeCategory(em.find(Category.class, zuluCats.get(70.0)));
			em.flush();
			return null;
		});
	}

	// ---------------------------------------------------------------------------------------------
	// no recruiting (spec §10)
	// ---------------------------------------------------------------------------------------------

	/** WITHIN never adds anyone to a championship; "Recompute all eligibilities" does, and the dry run says so. */
	@Test
	public void withinNeverRecruitsButRecomputeAllAdds() {
		Long jrLeague = newAgeGroup("JLJR", "JRLeague", Gender.M, 15, 20, true, 0, 999);
		Long gamesQ = newAgeGroup("GQ", "GamesQ", Gender.M, 15, 999, true, 0, 999);
		Long jrCat = categoryIdsByMax(jrLeague).get(999.0);
		Long gqCat = categoryIdsByMax(gamesQ).get(999.0);
		Long n1 = newAthlete("NoRecruit", Gender.M, 18, 80.0, 100);
		participate(n1, jrCat, true, false);
		setRegistration(n1, jrCat);

		List<String> both = List.of("JRLeague", "GamesQ");
		SingletonSaves.inTransaction(em -> {
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em, both);
			ReassignmentPlan plan = ChampionshipReassigner.plan(em, snapshots,
			        targetsFor(em, both, Set.of()), reasonOnlyFor(em, both),
			        ChampionshipReassigner.resolveMixedTeams(em, both), Mode.WITHIN);
			ChampionshipReassigner.apply(em, plan);
			return plan;
		});
		assertFalse("reassign within championships never adds", flagsByCategoryId(n1).containsKey(gqCat));

		ReassignmentPlan allPlan = reassignAll(both);
		AthleteChange change = changeOf(allPlan, n1);
		assertNotNull(change);
		ChampionshipChange cc = change.byChampionship().get("GamesQ");
		assertNotNull(cc);
		assertTrue(cc.joins());
		ReassignmentPreview preview = ChampionshipReassigner.preview(allPlan);
		assertTrue(preview.joiners("GamesQ").stream().anyMatch(e -> e.athleteId().equals(n1)));
		assertTrue(flagsByCategoryId(n1).containsKey(gqCat));
	}

	/** Activating the only age group of a championship adds nobody: nobody is in that championship. */
	@Test
	public void activatingOnlyAgeGroupAddsNobody() {
		Long nc = newAgeGroup("NC", "NewChamp", Gender.M, 30, 40, false, 0, 999);
		Long ncCat = categoryIdsByMax(nc).get(999.0);

		ReassignmentPlan plan = reassignWithin("NewChamp",
		        em -> findAgeGroup(em, "NC", Gender.M).setActive(true));

		assertTrue(plan.changes().isEmpty());
		assertEquals(0, categoryParticipationCount(ncCat));
	}

	// ---------------------------------------------------------------------------------------------
	// dry run, deleted categories, cold cache (plan 2.4)
	// ---------------------------------------------------------------------------------------------

	/** An athlete linked only to a deleted category stays in scope; the frozen presumed weight is used. */
	@Test
	public void athleteLinkedOnlyToDeletedCategoryUsesFrozenPresumedWeight() {
		Long ag = newAgeGroup("DM35", "DelM", Gender.M, 35, 39, true, 0, 73, 85, 999);
		Map<Double, Long> cats = categoryIdsByMax(ag);
		Long d1 = newAthlete("DelOne", Gender.M, 36, null, 100);
		participate(d1, cats.get(73.0), true, false);
		setRegistration(d1, cats.get(73.0));
		// no body weight: the presumed weight is the registration category's limit, 73

		ReassignmentPlan plan = reassignWithin("DelM", em -> {
			// the registration repair happens before categories are removed; the snapshot already froze 73
			em.find(Athlete.class, d1).setCategory(null);
			// the caller recomputes the lower limits of the final categories (step 5): 85 now starts at 0
			categoryAtMax(em, ag, 85.0).setMinimumWeight(0.0);
			categoryAtMax(em, ag, 999.0).setMinimumWeight(85.0);
		}, Set.of(cats.get(73.0)));

		AthleteChange change = changeOf(plan, d1);
		assertNotNull("linked only to a deleted category, still in scope", change);
		ChampionshipChange cc = change.byChampionship().get("DelM");
		assertTrue(cc.removedCategoryIds().contains(cats.get(73.0)));
		assertTrue("the frozen presumed weight (73) still fits 85",
		        cc.addedCategories().stream().anyMatch(c -> c.getId().equals(cats.get(85.0))));
		assertFalse("a live read would see no weight and report MISSING_BODY_WEIGHT", cc.leaves());
	}

	/** No championship row and a cold cache: the engine works and bootstraps nothing. */
	@Test
	public void coldCacheAndMissingChampionshipRowDoesNotBootstrap() {
		Championship.reset();
		Long ag = newAgeGroup("GH35", "GhostChamp", Gender.M, 35, 39, true, 0, 999);
		Long cat = categoryIdsByMax(ag).get(999.0);
		Long g1 = newAthlete("GhostOne", Gender.M, 36, 80.0, 100);
		participate(g1, cat, true, false);
		setRegistration(g1, cat);
		long before = championshipRowCount();

		ReassignmentPlan plan = reassignWithin("GhostChamp", null);

		ChampionshipChange cc = changeOf(plan, g1).byChampionship().get("GhostChamp");
		assertTrue(cc.keptCategoryIds().contains(cat));
		assertEquals("no championship row materialized", before, championshipRowCount());
		assertNull(SingletonSaves.inTransaction(em -> em
		        .createQuery("select c from Championship c where lower(c.name) = 'ghostchamp'", Championship.class)
		        .getResultList().stream().findFirst().orElse(null)));
		assertTrue(flagsByCategoryId(g1).containsKey(cat));
	}

	/** The dry run writes nothing and the preview is immutable data. */
	@Test
	public void previewWritesNothingAndIsImmutable() {
		Long ag = newAgeGroup("PVM35", "PrevM", Gender.M, 35, 39, true, 0, 73, 999);
		Long cat73 = categoryIdsByMax(ag).get(73.0);
		SingletonSaves.inTransaction(em -> {
			categoryAtMax(em, ag, 73.0).setQualifyingTotal(280);
			categoryAtMax(em, ag, 999.0).setQualifyingTotal(300);
			em.flush();
			return null;
		});
		Long p1 = newAthlete("PrevOne", Gender.M, 36, 70.0, 250);
		participate(p1, cat73, true, false);
		int partsBefore = participationCount(p1);

		ReassignmentPreview preview = previewWithin("PrevM", null);

		assertEquals(partsBefore, participationCount(p1));
		assertTrue(flagsByCategoryId(p1).containsKey(cat73));
		List<ReassignmentPreview.Entry> leavers = preview.leavers("PrevM");
		assertEquals(1, leavers.size());
		assertEquals(LeaveReason.QT, leavers.get(0).reason());
		try {
			preview.entries().add(leavers.get(0));
			throw new AssertionError("preview entries must be immutable");
		} catch (UnsupportedOperationException expected) {
			// immutable by contract
		}
	}


	// ---------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------

	private static Long newAgeGroup(String code, String championship, Gender gender, int minAge, int maxAge,
	        boolean active, int qualifyingTotal, double... upperLimits) {
		return SingletonSaves.inTransaction(em -> {
			AgeGroup ageGroup = new AgeGroup(code, active, minAge, maxAge, gender, "U", 0);
			ageGroup.setChampionshipName(championship);
			double lower = 0.0;
			for (double upper : upperLimits) {
				ageGroup.addCategory(new Category(lower, upper, gender, active, 0, 0, 0, ageGroup, qualifyingTotal));
				lower = upper;
			}
			em.persist(ageGroup);
			em.flush();
			return ageGroup.getId();
		});
	}

	private static Long newAthlete(String lastName, Gender gender, Integer age, Double bodyWeight, Integer entryTotal) {
		return SingletonSaves.inTransaction(em -> {
			Athlete athlete = new Athlete();
			athlete.setLastName(lastName);
			athlete.setFirstName("T");
			athlete.setGender(gender);
			if (age != null) {
				athlete.setYearOfBirth(LocalDate.now().getYear() - age);
			}
			if (bodyWeight != null) {
				athlete.setBodyWeight(bodyWeight);
			}
			if (entryTotal != null) {
				athlete.setQualifyingTotal(entryTotal);
			}
			em.persist(athlete);
			em.flush();
			return athlete.getId();
		});
	}

	private static void newChampionshipRow(String name, boolean explicitMixed) {
		SingletonSaves.inTransaction(em -> {
			Championship championship = new Championship();
			championship.setName(name);
			championship.setExplicitMixedTeamMembers(explicitMixed);
			em.persist(championship);
			em.flush();
			return null;
		});
	}

	private static void participate(Long athleteId, Long categoryId, boolean team, boolean mixed) {
		SingletonSaves.inTransaction(em -> {
			Athlete athlete = em.find(Athlete.class, athleteId);
			Category category = em.find(Category.class, categoryId);
			athlete.addEligibleCategory(category, team, mixed);
			em.flush();
			return null;
		});
	}

	private static void setRegistration(Long athleteId, Long categoryId) {
		SingletonSaves.inTransaction(em -> {
			Athlete athlete = em.find(Athlete.class, athleteId);
			athlete.setCategory(categoryId != null ? em.find(Category.class, categoryId) : null);
			em.flush();
			return null;
		});
	}

	private static void setSnatchRank(Long athleteId, Long categoryId, int rank) {
		SingletonSaves.inTransaction(em -> {
			Athlete athlete = em.find(Athlete.class, athleteId);
			for (Participation p : athlete.getParticipations()) {
				if (p.getCategory() != null && p.getCategory().getId().equals(categoryId)) {
					p.setSnatchRank(rank);
				}
			}
			em.flush();
			return null;
		});
	}

	private static AgeGroup findAgeGroup(EntityManager em, String code, Gender gender) {
		return em.createQuery("select ag from AgeGroup ag where ag.code = :code and ag.gender = :gender",
		        AgeGroup.class)
		        .setParameter("code", code)
		        .setParameter("gender", gender)
		        .getResultList().stream().findFirst().orElse(null);
	}

	private static Category categoryAtMax(EntityManager em, Long ageGroupId, double maxWeight) {
		for (Category c : em.find(AgeGroup.class, ageGroupId).getAllCategories()) {
			if (c.getMaximumWeight() != null && c.getMaximumWeight() == maxWeight) {
				return c;
			}
		}
		return null;
	}

	private static Map<Double, Long> categoryIdsByMax(Long ageGroupId) {
		return SingletonSaves.inTransaction(em -> {
			Map<Double, Long> result = new LinkedHashMap<>();
			for (Category c : em.find(AgeGroup.class, ageGroupId).getAllCategories()) {
				result.put(c.getMaximumWeight(), c.getId());
			}
			return result;
		});
	}

	/** Final target categories: the championship's active age groups, minus any marked for deletion. */
	private static Map<String, List<Category>> targetsFor(EntityManager em, List<String> championships,
	        Set<Long> excludedCategoryIds) {
		Map<String, List<Category>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (String name : championships) {
			result.put(name, em.createQuery(
			        "select c from Category c join c.ageGroup ag"
			                + " where lower(ag.championshipName) = lower(:name) and ag.active = true"
			                + " order by ag.minAge, c.maximumWeight",
			        Category.class)
			        .setParameter("name", name)
			        .getResultList().stream()
			        .filter(c -> !excludedCategoryIds.contains(c.getId()))
			        .collect(Collectors.toList()));
		}
		return result;
	}

	/** Reason-only candidates: categories of the championship's inactive age groups. */
	private static Map<String, List<Category>> reasonOnlyFor(EntityManager em, List<String> championships) {
		Map<String, List<Category>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (String name : championships) {
			result.put(name, em.createQuery(
			        "select c from Category c join c.ageGroup ag"
			                + " where lower(ag.championshipName) = lower(:name) and ag.active = false"
			                + " order by ag.minAge, c.maximumWeight",
			        Category.class)
			        .setParameter("name", name)
			        .getResultList());
		}
		return result;
	}

	/** The production save shape: snapshot first, then the definition change, then plan and apply, one transaction. */
	private static ReassignmentPlan reassignWithin(String championship, Consumer<EntityManager> edit,
	        Set<Long> excludedCategoryIds) {
		return SingletonSaves.inTransaction(em -> {
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em, List.of(championship));
			if (edit != null) {
				edit.accept(em);
			}
			List<String> names = List.of(championship);
			ReassignmentPlan plan = ChampionshipReassigner.plan(em, snapshots,
			        targetsFor(em, names, excludedCategoryIds), reasonOnlyFor(em, names),
			        ChampionshipReassigner.resolveMixedTeams(em, names), Mode.WITHIN);
			ChampionshipReassigner.apply(em, plan);
			return plan;
		});
	}

	private static ReassignmentPlan reassignWithin(String championship, Consumer<EntityManager> edit) {
		return reassignWithin(championship, edit, Set.of());
	}

	private static ReassignmentPlan reassignAll(List<String> championships) {
		return SingletonSaves.inTransaction(em -> {
			ReassignmentPlan plan = ChampionshipReassigner.plan(em, AthleteSnapshot.all(em),
			        targetsFor(em, championships, Set.of()), reasonOnlyFor(em, championships),
			        ChampionshipReassigner.resolveMixedTeams(em, championships), Mode.ALL);
			ChampionshipReassigner.apply(em, plan);
			return plan;
		});
	}

	/** Dry run only: plan and preview, never apply. Definition edits made here commit, as in the advisory flow. */
	private static ReassignmentPreview previewWithin(String championship, Consumer<EntityManager> edit) {
		return SingletonSaves.inTransaction(em -> {
			Map<Long, AthleteSnapshot> snapshots = AthleteSnapshot.forChampionships(em, List.of(championship));
			if (edit != null) {
				edit.accept(em);
			}
			List<String> names = List.of(championship);
			ReassignmentPlan plan = ChampionshipReassigner.plan(em, snapshots,
			        targetsFor(em, names, Set.of()), reasonOnlyFor(em, names),
			        ChampionshipReassigner.resolveMixedTeams(em, names), Mode.WITHIN);
			return ChampionshipReassigner.preview(plan);
		});
	}

	private static Map<Long, boolean[]> flagsByCategoryId(Long athleteId) {
		return SingletonSaves.inTransaction(em -> {
			Map<Long, boolean[]> result = new LinkedHashMap<>();
			for (Participation p : em.find(Athlete.class, athleteId).getParticipations()) {
				result.put(p.getCategory().getId(), new boolean[] { p.getTeamMember(), p.getMixedTeamMember() });
			}
			return result;
		});
	}

	private static Integer snatchRank(Long athleteId, Long categoryId) {
		return SingletonSaves.inTransaction(em -> {
			for (Participation p : em.find(Athlete.class, athleteId).getParticipations()) {
				if (p.getCategory() != null && p.getCategory().getId().equals(categoryId)) {
					return p.getSnatchRank();
				}
			}
			return null;
		});
	}

	private static Long registrationId(Long athleteId) {
		return SingletonSaves.inTransaction(em -> {
			Category registration = em.find(Athlete.class, athleteId).getCategory();
			return registration != null ? registration.getId() : null;
		});
	}

	private static int participationCount(Long athleteId) {
		return SingletonSaves.inTransaction(em -> em.find(Athlete.class, athleteId).getParticipations().size());
	}

	private static int categoryParticipationCount(Long categoryId) {
		return SingletonSaves.inTransaction(em -> em.find(Category.class, categoryId).getParticipations().size());
	}

	private static Long findFixtureAthleteId(String lastName, String team) {
		return SingletonSaves.inTransaction(em -> em
		        .createQuery("select a from Athlete a", Athlete.class).getResultList().stream()
		        .filter(a -> lastName.equalsIgnoreCase(a.getLastName()) && team.equalsIgnoreCase(a.getTeam()))
		        .map(Athlete::getId).findFirst().orElse(null));
	}

	private static long championshipRowCount() {
		return SingletonSaves.inTransaction(em -> em
		        .createQuery("select count(c) from Championship c", Long.class).getSingleResult());
	}

	private static AthleteChange changeOf(ReassignmentPlan plan, Long athleteId) {
		return plan.changes().stream().filter(c -> c.athleteId().equals(athleteId)).findFirst().orElse(null);
	}

	private static void loadFixtureIntoMemoryDatabase(String memoryJdbcUrl) throws IOException, SQLException {
		fixtureDirectory = Files.createTempDirectory("championship-reassignment-test-db-");
		Path copiedDatabase = fixtureDirectory.resolve("mixedTestsJRSR.mv.db");
		Path scriptFile = fixtureDirectory.resolve("mixedTestsJRSR.sql");

		try (InputStream fixtureStream = ChampionshipReassignmentTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
			assertNotNull("Fixture database not found: " + FIXTURE_RESOURCE, fixtureStream);
			Files.copy(fixtureStream, copiedDatabase, StandardCopyOption.REPLACE_EXISTING);
		}

		String sourceBase = copiedDatabase.toAbsolutePath().toString().replaceAll("\\.mv\\.db$", "");
		String sourceUrl = "jdbc:h2:file:" + sourceBase + ";DB_CLOSE_DELAY=-1;TRACE_LEVEL_FILE=0";
		String escapedScriptFile = scriptFile.toAbsolutePath().toString().replace("\\", "\\\\").replace("'", "''");

		try (Connection source = DriverManager.getConnection(sourceUrl, "sa", "");
		        Statement sourceStatement = source.createStatement()) {
			sourceStatement.execute("SCRIPT TO '" + escapedScriptFile + "'");
		}
		try (Connection target = DriverManager.getConnection(memoryJdbcUrl, "sa", "");
		        Statement targetStatement = target.createStatement()) {
			targetStatement.execute("RUNSCRIPT FROM '" + escapedScriptFile + "'");
		}
	}

	private static void deleteFixtureDirectory() throws IOException {
		if (fixtureDirectory == null || !Files.exists(fixtureDirectory)) {
			return;
		}
		try (var paths = Files.walk(fixtureDirectory)) {
			for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}
}
