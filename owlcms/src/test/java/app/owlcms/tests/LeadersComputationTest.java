/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.tests;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.Main;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Leaders (bottom-of-scoreboard) computation, all four combinations of
 * MEDALISTS_AS_LEADERS on/off and 3-medal vs total-only championships.
 *
 * Fixture: two sessions.
 * <ul>
 * <li>Category M81 has athletes in session 1 (three totals, one snatch bomb-out with the best CJ,
 * one CJ bomb-out with the second-best snatch) and two athletes in session 2.</li>
 * <li>Category M73 has athletes only in session 2 (no prior results at all).</li>
 * </ul>
 * Session 2 is replayed with the identical scripted lift sequence (all weights distinct) for each
 * scenario; only the expected leader lists differ. Each checkpoint appends a scoreboard-like
 * section to target/leaders-scoreboards.html for visual validation.
 */
public class LeadersComputationTest {

	private static final StringBuilder htmlReport = new StringBuilder();
	private static int lot;
	final Logger logger = (Logger) LoggerFactory.getLogger(LeadersComputationTest.class);

	private Group session1;
	private Group session2;
	private String originalFeatureSwitches;
	private FieldOfPlay fop;
	private EventBus fopBus;

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		htmlReport.append("""
		        <!DOCTYPE html><html><head><meta charset="utf-8"><title>Leaders computation</title>
		        <style>
		        body { font-family: Arial, sans-serif; background: #202020; color: #eee; }
		        h2 { background: #001c55; color: white; padding: 0.3em 0.6em; }
		        p.context { margin: 0.2em 0 0.5em 0.6em; color: #ccc; }
		        table { border-collapse: collapse; margin: 0.5em 0 1.5em 0.6em; min-width: 46em; }
		        th, td { border: 1px solid #555; padding: 0.25em 0.8em; text-align: center; }
		        th { background: #333; }
		        td.name { text-align: left; }
		        tr.empty td { color: #888; font-style: italic; }
		        </style></head><body><h1>Leaders sent to scoreboards</h1>
		        <p class="context">Session 1 (M81): TotalGold 100/120=220, TotalSilver 95/115=210,
		        TotalBronze 90/110=200, SnatchBomb (3 missed snatches, CJ 125),
		        CJBomb (snatch 98, 3 missed CJs).<br>
		        Session 2: Challenger and Companion (M81); New73A and New73B (M73, category absent
		        from session 1).</p>
		        """);
	}

	@AfterClass
	public static void tearDownTests() throws IOException {
		htmlReport.append("</body></html>\n");
		Path out = Path.of("target", "leaders-scoreboards.html");
		Files.createDirectories(out.getParent());
		Files.writeString(out, htmlReport.toString());
		JPAService.close();
	}

	@Before
	public void setupTest() {
		TestData.insertInitialData(5, true);
		JPAService.runInTransaction((em) -> {
			this.session1 = GroupRepository.doFindByName("A", em);
			this.session2 = GroupRepository.doFindByName("B", em);
			TestData.deleteAllLifters(em);
			insertFixtureAthletes(em, this.session1);
			return null;
		});
		AthleteRepository.resetParticipations(false, true);

		this.originalFeatureSwitches = Config.getCurrent().getFeatureSwitches();

		List<Athlete> athletes = AthleteRepository.findAll();
		this.fop = MockFieldOfPlay.create(athletes, new MockCountdownTimer(), new MockCountdownTimer());
		this.fopBus = this.fop.getFopEventBus();
		OwlcmsSession.setFop(this.fop);
		this.fop.getLogger().setLevel(Level.INFO);
	}

	@After
	public void restoreConfig() {
		Config.getCurrent().setFeatureSwitches(this.originalFeatureSwitches);
	}

	// ------------------------------------------------------------------------
	// scenarios (same lift script; only mode/medal flags and expectations differ)
	// ------------------------------------------------------------------------

	@Test
	public void threeMedalMedalistsAsLeaders() {
		setMedalistsAsLeaders(true);
		setSnatchCJTotalMedals(true);
		runScenario("3-medal championship",
		        // session 1 mid-snatch: partial snatch standings
		        List.of("TotalSilver", "TotalBronze"),
		        // session 1 mid-CJ: partial totals first, then snatch medal holders without a total
		        List.of("TotalSilver", "TotalBronze", "TotalGold", "CJBomb"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze", "SnatchBomb", "CJBomb"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze", "SnatchBomb", "CJBomb", "Challenger"),
		        List.of(),
		        List.of("New73A"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze", "Challenger", "SnatchBomb", "CJBomb"),
		        List.of("TotalGold", "TotalSilver", "Challenger", "SnatchBomb", "CJBomb"),
		        // Challenger back to total rank 4 but keeps snatch bronze: stays on the board
		        List.of("TotalGold", "TotalSilver", "TotalBronze", "Challenger", "SnatchBomb", "CJBomb"));
	}

	@Test
	public void totalOnlyMedalistsAsLeaders() {
		setMedalistsAsLeaders(true);
		setSnatchCJTotalMedals(false);
		runScenario("Total-only championship",
		        // session 1 mid-snatch: no totals anywhere yet, fake snatch podium
		        List.of("TotalSilver", "TotalBronze"),
		        // session 1 mid-CJ: first totals exist, snatch standings no longer shown
		        List.of("TotalSilver", "TotalBronze"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze"),
		        List.of(),
		        List.of("New73A"),
		        List.of("TotalGold", "TotalSilver", "TotalBronze"),
		        List.of("TotalGold", "TotalSilver", "Challenger"),
		        // lift medals are not retained in total-only: Challenger is pushed off
		        List.of("TotalGold", "TotalSilver", "TotalBronze"));
	}

	@Test
	public void threeMedalPreviousSessionsOnly() {
		setMedalistsAsLeaders(false);
		setSnatchCJTotalMedals(true);
		List<String> priorMedalists = List.of("TotalGold", "TotalSilver", "TotalBronze", "SnatchBomb", "CJBomb");
		runScenario("3-medal championship",
		        // no previous session while session 1 is lifting
		        List.of(), List.of(),
		        priorMedalists, priorMedalists, List.of(), List.of(), priorMedalists,
		        // Challenger's 202 total makes TotalBronze rank 4 with no lift medal: he drops
		        // off even though the display pool is restricted to prior sessions.
		        List.of("TotalGold", "TotalSilver", "SnatchBomb", "CJBomb"),
		        // the faked 116 CJ restores TotalBronze's total bronze: he is back
		        priorMedalists);
	}

	@Test
	public void totalOnlyPreviousSessionsOnly() {
		setMedalistsAsLeaders(false);
		setSnatchCJTotalMedals(false);
		List<String> priorPodium = List.of("TotalGold", "TotalSilver", "TotalBronze");
		runScenario("Total-only championship",
		        List.of(), List.of(),
		        priorPodium, priorPodium, List.of(), List.of(), priorPodium, priorPodium, priorPodium);
	}

	/**
	 * Scripted competition (weights all distinct):
	 *
	 * <pre>
	 * session 1 snatch: SnatchBomb 84✗✗✗, TotalBronze 86✓ 88✓ 90✓,
	 *                   TotalSilver 91✓ 93✓ [CP0a] 95✓, CJBomb 96✓ 97✓ 98✓,
	 *                   TotalGold 99✓ 100✓ 101✗
	 * session 1 CJ:     TotalBronze 106✓ 108✓ 110✓, TotalSilver 111✓ 113✓ [CP0b] 115✓,
	 *                   TotalGold 116✓ 118✓, CJBomb 119✗✗✗, TotalGold 120✓,
	 *                   SnatchBomb 121✓ 123✓ 125✓
	 * session 2 snatch: Companion 79✓, Challenger 80✓, Challenger 85✓ [CP1], Companion 88✓,
	 *                   Challenger 96✓ [CP2], Companion 97✗, [CP3] New73A 100✓ [CP4], 101✓, 102✓,
	 *                   New73B 105✓, 106✓, 107✓
	 * session 2 CJ:     Challenger 103✓ (total 199, rank 4) [CP5], Companion 104✓,
	 *                   Challenger 106✓ (total 202, rank 3) [CP6],
	 *                   faked: TotalBronze prior CJ 110→116 (total 206),
	 *                   Challenger 107✓ (total 203, rank 4 again) [CP7]
	 * </pre>
	 */
	private void runScenario(String scenarioTitle, List<String> cp0a, List<String> cp0b, List<String> cp1,
	        List<String> cp2, List<String> cp3, List<String> cp4, List<String> cp5, List<String> cp6,
	        List<String> cp7) {
		startSession1();

		stepSession1ToCp0a();
		checkpoint(scenarioTitle, "CP0a — session 1 mid-snatch: partially filled board", cp0a);
		stepCp0aToCp0b();
		checkpoint(scenarioTitle, "CP0b — session 1 mid-CJ: first totals appearing", cp0b);
		stepCp0bToSession1End();

		startSession2();

		stepToCp1();
		checkpoint(scenarioTitle, "CP1 — snatch: Challenger best snatch 85, not a medalist", cp1);
		stepCp1ToCp2();
		checkpoint(scenarioTitle, "CP2 — snatch: Challenger 96 is now snatch rank 3", cp2);
		stepCp2ToCp3();
		checkpoint(scenarioTitle, "CP3 — New73A (M73) current, category has no results yet", cp3);
		stepCp3ToCp4();
		checkpoint(scenarioTitle, "CP4 — M73: New73A snatched 100, nobody in the category has a total", cp4);
		stepCp4ToCp5();
		checkpoint(scenarioTitle, "CP5 — CJ: Challenger total 199 is rank 4, not a total medalist", cp5);
		stepCp5ToCp6();
		checkpoint(scenarioTitle, "CP6 — CJ: Challenger total 202 is rank 3, now a total medalist", cp6);
		stepCp6ToCp7();
		checkpoint(scenarioTitle,
		        "CP7 — faked TotalBronze CJ 116 (total 206) reclaims total bronze; Challenger 107 makes total 203, rank 4 — keeps snatch bronze",
		        cp7);
	}

	// ------------------------------------------------------------------------
	// checkpoint-to-checkpoint steps
	// ------------------------------------------------------------------------

	private void startSession1() {
		this.fop.testBefore();
		this.fop.loadGroup(this.session1, this, true);
		this.fop.testStartLifting(this.session1, this.fop);
	}

	private void startSession2() {
		this.fop.testBefore();
		this.fop.loadGroup(this.session2, this, true);
		this.fop.testStartLifting(this.session2, this.fop);
	}

	/** Session 1 snatches: SnatchBomb bombs out, TotalBronze finishes, TotalSilver mid-way. */
	private void stepSession1ToCp0a() {
		expectCurrent("SnatchBomb", 84);
		failedLift();
		expectCurrent("SnatchBomb", 84);
		failedLift();
		expectCurrent("SnatchBomb", 84);
		failedLift();

		expectCurrent("TotalBronze", 86);
		goodLift();
		declareNext("TotalBronze", "88");
		expectCurrent("TotalBronze", 88);
		goodLift();
		declareNext("TotalBronze", "90");
		expectCurrent("TotalBronze", 90);
		goodLift();

		expectCurrent("TotalSilver", 91);
		goodLift();
		declareNext("TotalSilver", "93");
		expectCurrent("TotalSilver", 93);
		goodLift();
		declareNext("TotalSilver", "95");
	}

	/** Finish session 1 snatches, then CJ up to TotalSilver's second attempt. */
	private void stepCp0aToCp0b() {
		expectCurrent("TotalSilver", 95);
		goodLift();

		expectCurrent("CJBomb", 96);
		goodLift();
		declareNext("CJBomb", "97");
		expectCurrent("CJBomb", 97);
		goodLift();
		declareNext("CJBomb", "98");
		expectCurrent("CJBomb", 98);
		goodLift();

		expectCurrent("TotalGold", 99);
		goodLift();
		declareNext("TotalGold", "100");
		expectCurrent("TotalGold", 100);
		goodLift();
		declareNext("TotalGold", "101");
		expectCurrent("TotalGold", 101);
		failedLift();

		// ---- session 1 clean & jerk
		expectCurrent("TotalBronze", 106);
		goodLift();
		declareNext("TotalBronze", "108");
		expectCurrent("TotalBronze", 108);
		goodLift();
		declareNext("TotalBronze", "110");
		expectCurrent("TotalBronze", 110);
		goodLift();

		expectCurrent("TotalSilver", 111);
		goodLift();
		declareNext("TotalSilver", "113");
		expectCurrent("TotalSilver", 113);
		goodLift();
		declareNext("TotalSilver", "115");
	}

	/** Finish session 1: TotalSilver 115, TotalGold 120, CJBomb bombs, SnatchBomb best CJ 125. */
	private void stepCp0bToSession1End() {
		expectCurrent("TotalSilver", 115);
		goodLift();

		expectCurrent("TotalGold", 116);
		goodLift();
		declareNext("TotalGold", "118");
		expectCurrent("TotalGold", 118);
		goodLift();
		declareNext("TotalGold", "120");

		expectCurrent("CJBomb", 119);
		failedLift();
		expectCurrent("CJBomb", 119);
		failedLift();
		expectCurrent("CJBomb", 119);
		failedLift();

		expectCurrent("TotalGold", 120);
		goodLift();

		expectCurrent("SnatchBomb", 121);
		goodLift();
		declareNext("SnatchBomb", "123");
		expectCurrent("SnatchBomb", 123);
		goodLift();
		declareNext("SnatchBomb", "125");
		expectCurrent("SnatchBomb", 125);
		goodLift();
	}

	/** Opening snatches up to Challenger's second attempt at 85. */
	private void stepToCp1() {
		expectCurrent("Companion", 79);
		goodLift();
		declareNext("Companion", "88");

		expectCurrent("Challenger", 80);
		goodLift();
		declareNext("Challenger", "85");

		expectCurrent("Challenger", 85);
		goodLift();
		declareNext("Challenger", "96");
	}

	/** Companion 88, then Challenger's third snatch at 96 (snatch rank 3). */
	private void stepCp1ToCp2() {
		expectCurrent("Companion", 88);
		goodLift();
		declareNext("Companion", "97");

		expectCurrent("Challenger", 96);
		goodLift();
	}

	/** Companion misses 97; New73A (M73) becomes the current athlete. */
	private void stepCp2ToCp3() {
		expectCurrent("Companion", 97);
		failedLift();

		expectCurrent("New73A", 100);
	}

	/** New73A makes his opener at 100. */
	private void stepCp3ToCp4() {
		goodLift();
		declareNext("New73A", "101");
	}

	/** Finish the M73 snatches, then Challenger opens the CJ at 103 (total 199, rank 4). */
	private void stepCp4ToCp5() {
		expectCurrent("New73A", 101);
		goodLift();
		declareNext("New73A", "102");
		expectCurrent("New73A", 102);
		goodLift();

		expectCurrent("New73B", 105);
		goodLift();
		declareNext("New73B", "106");
		expectCurrent("New73B", 106);
		goodLift();
		declareNext("New73B", "107");
		expectCurrent("New73B", 107);
		goodLift();

		expectCurrent("Challenger", 103);
		goodLift();
		declareNext("Challenger", "106");
	}

	/** Companion CJ 104, then Challenger's second CJ at 106 (total 202, rank 3). */
	private void stepCp5ToCp6() {
		expectCurrent("Companion", 104);
		goodLift();
		declareNext("Companion", "108");

		expectCurrent("Challenger", 106);
		goodLift();
		declareNext("Challenger", "107");
	}

	/** Fake a better prior-session CJ for TotalBronze, then Challenger's last CJ at 107. */
	private void stepCp6ToCp7() {
		fakePriorCleanJerk("TotalBronze", "116");
		expectCurrent("Challenger", 107);
		goodLift();
	}

	private void checkpoint(String scenarioTitle, String description, List<String> expected) {
		snapshot(scenarioTitle, description);
		assertEquals(description, expected, leaderNames());
	}

	// ------------------------------------------------------------------------
	// fixture
	// ------------------------------------------------------------------------

	private void insertFixtureAthletes(EntityManager em, Group session1) {
		lot = 1;
		// session 1, category M81: lifted live through the FOP (openers declared here)
		Athlete gold = createAthlete(em, session1, "TotalGold", "Open_M81", 80.0);
		declare(gold, "99", "116");
		Athlete silver = createAthlete(em, session1, "TotalSilver", "Open_M81", 80.2);
		declare(silver, "91", "111");
		Athlete bronze = createAthlete(em, session1, "TotalBronze", "Open_M81", 80.4);
		declare(bronze, "86", "106");
		// will bomb all snatches, then lift the best CJ of the category
		Athlete snatchBomb = createAthlete(em, session1, "SnatchBomb", "Open_M81", 80.6);
		declare(snatchBomb, "84", "121");
		// will have the second-best snatch, then bomb all CJs
		Athlete cjBomb = createAthlete(em, session1, "CJBomb", "Open_M81", 80.8);
		declare(cjBomb, "96", "119");

		// session 2, category M81
		Athlete challenger = createAthlete(em, this.session2, "Challenger", "Open_M81", 80.9);
		declare(challenger, "80", "103");
		Athlete companion = createAthlete(em, this.session2, "Companion", "Open_M81", 80.95);
		declare(companion, "79", "104");

		// session 2, category M73: no athletes in session 1
		Athlete m73a = createAthlete(em, this.session2, "New73A", "Open_M73", 72.0);
		declare(m73a, "100", "130");
		Athlete m73b = createAthlete(em, this.session2, "New73B", "Open_M73", 72.5);
		declare(m73b, "105", "135");
	}

	private Athlete createAthlete(EntityManager em, Group group, String lastName, String categoryCode, double bw) {
		Athlete a = new Athlete();
		a.setGroup(em.contains(group) ? group : em.merge(group));
		a.setFirstName("Test");
		a.setLastName(lastName);
		a.setGender(Gender.M);
		a.setFullBirthDate(LocalDate.of(1990, 1, 1));
		a.setLotNumber(lot++);
		a.setBodyWeight(bw);
		Category cat = CategoryRepository.findByCode(categoryCode);
		a.computeCategory(cat);
		em.persist(a);
		return a;
	}

	private void declare(Athlete a, String snatch1, String cleanJerk1) {
		a.setValidation(false);
		a.setSnatch1Declaration(snatch1);
		a.setCleanJerk1Declaration(cleanJerk1);
		a.setValidation(true);
	}

	private void setMedalistsAsLeaders(boolean enabled) {
		Config.getCurrent().setFeatureSwitches(enabled ? FeatureSwitch.MEDALISTS_AS_LEADERS.getId() : "");
	}

	/** Rewrites a prior-session result in the database; the next decision recomputes medals from it. */
	private void fakePriorCleanJerk(String lastName, String weight) {
		JPAService.runInTransaction(em -> {
			Athlete a = AthleteRepository.doFindAll(em).stream()
			        .filter(x -> lastName.equals(x.getLastName()))
			        .findFirst().orElseThrow();
			a.setValidation(false);
			a.setCleanJerk1Declaration(weight);
			a.setCleanJerk1ActualLift(weight);
			a.setValidation(true);
			em.merge(a);
			return null;
		});
	}

	/** The 3-medal flag lives on the championships of the fixture categories, not on Competition. */
	private void setSnatchCJTotalMedals(boolean threeMedals) {
		for (String code : List.of("Open_M81", "Open_M73")) {
			Category cat = CategoryRepository.findByCode(code);
			Championship championship = cat.getAgeGroup().getChampionship();
			if (championship != null && championship.isSnatchCJTotalMedals() != threeMedals) {
				championship.setSnatchCJTotalMedals(threeMedals);
				ChampionshipRepository.save(championship);
			}
		}
	}

	// ------------------------------------------------------------------------
	// FOP driving (same event sequences as TwoMinutesRuleTest)
	// ------------------------------------------------------------------------

	private void expectCurrent(String lastName, int weight) {
		Athlete cur = this.fop.getCurAthlete();
		assertEquals("current athlete", lastName, cur.getLastName());
		assertEquals("requested weight for " + lastName, weight, (int) cur.getNextAttemptRequestedWeight());
	}

	private Athlete byName(String lastName) {
		return this.fop.getLiftingOrder().stream()
		        .filter(a -> lastName.equals(a.getLastName()))
		        .findFirst().orElseThrow();
	}

	private void declareNext(String lastName, String weight) {
		Athlete lifter = byName(lastName);
		JPAService.runInTransaction(em -> {
			switch (lifter.getAttemptsDone() + 1) {
				case 1 -> lifter.setSnatch1Declaration(weight);
				case 2 -> lifter.setSnatch2Declaration(weight);
				case 3 -> lifter.setSnatch3Declaration(weight);
				case 4 -> lifter.setCleanJerk1Declaration(weight);
				case 5 -> lifter.setCleanJerk2Declaration(weight);
				case 6 -> lifter.setCleanJerk3Declaration(weight);
			}
			em.merge(lifter);
			return null;
		});
		this.fopBus.post(new FOPEvent.WeightChange(this, lifter, false));
	}

	private void goodLift() {
		lift(true);
	}

	private void failedLift() {
		lift(false);
	}

	private void lift(boolean good) {
		Athlete curLifter = this.fop.getCurAthlete();
		this.fopBus.post(new FOPEvent.TimeStarted(null));
		this.fopBus.post(new FOPEvent.DownSignal(null));
		this.fopBus.post(new FOPEvent.DecisionFullUpdate(this, curLifter, good, good, good, 0L, 0L, 0L, false));
		this.fopBus.post(new FOPEvent.DecisionReset(null));
	}

	// ------------------------------------------------------------------------
	// scoreboard-like reporting
	// ------------------------------------------------------------------------

	private List<String> leaderNames() {
		List<Athlete> leaders = this.fop.getLeaders();
		if (leaders == null) {
			return List.of();
		}
		return leaders.stream().map(Athlete::getLastName).collect(Collectors.toList());
	}

	private void snapshot(String scenarioTitle, String checkpoint) {
		Athlete cur = this.fop.getCurAthlete();
		String mode = Config.getCurrent().featureSwitch(FeatureSwitch.MEDALISTS_AS_LEADERS)
		        ? "medalists as leaders (current standings, all sessions)"
		        : "previous sessions only";
		htmlReport.append("<h2>").append(escape(scenarioTitle))
		        .append(" &mdash; ").append(escape(mode)).append("</h2>\n");
		htmlReport.append("<p class=\"context\">").append(escape(checkpoint));
		if (cur != null) {
			htmlReport.append(" &mdash; current athlete: <b>").append(escape(cur.getLastName()))
			        .append("</b> (").append(escape(cur.getCategory().getComputedCode()))
			        .append(", attempts done ").append(cur.getAttemptsDone()).append(")");
		}
		htmlReport.append("</p>\n");
		htmlReport.append(
		        "<table><tr><th>#</th><th>Name</th><th>Session</th><th>Category</th><th>Best Sn</th>"
		                + "<th>Sn rank</th><th>Best CJ</th><th>CJ rank</th><th>Total</th><th>Total rank</th></tr>\n");
		List<Athlete> leaders = this.fop.getLeaders() != null ? this.fop.getLeaders() : new ArrayList<>();
		if (leaders.isEmpty()) {
			htmlReport.append("<tr class=\"empty\"><td colspan=\"10\">(no leaders shown)</td></tr>\n");
		}
		int pos = 1;
		for (Athlete a : leaders) {
			htmlReport.append("<tr><td>").append(pos++).append("</td><td class=\"name\">")
			        .append(escape(a.getLastName())).append("</td><td>")
			        .append(a.getGroup() != null ? escape(a.getGroup().getName()) : "-").append("</td><td>")
			        .append(escape(a.getCategory() != null ? a.getCategory().getComputedCode() : "-"))
			        .append("</td><td>").append(a.getBestSnatch()).append("</td><td>")
			        .append(rank(a.getSnatchRank())).append("</td><td>").append(a.getBestCleanJerk())
			        .append("</td><td>").append(rank(a.getCleanJerkRank())).append("</td><td>")
			        .append(a.getTotal()).append("</td><td>").append(rank(a.getTotalRank()))
			        .append("</td></tr>\n");
		}
		htmlReport.append("</table>\n");
	}

	private String rank(int r) {
		return r > 0 ? Integer.toString(r) : "-";
	}

	private String escape(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
