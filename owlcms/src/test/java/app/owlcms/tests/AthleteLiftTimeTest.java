package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.WinningOrderComparator;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.AthleteSessionDataReader;
import app.owlcms.data.export.v2.AthleteDTO;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.MockFieldOfPlay;
import app.owlcms.spreadsheet.PAthlete;

public class AthleteLiftTimeTest {
	private static final LocalDateTime START = LocalDateTime.of(2026, 8, 26, 12, 0);
	private static final String[] PREFIXES = {
			"snatch1", "snatch2", "snatch3", "cleanJerk1", "cleanJerk2", "cleanJerk3"
	};

	@BeforeClass
	public static void setup() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		TestData.insertInitialData(5, true);
	}

	@AfterClass
	public static void tearDown() {
		JPAService.close();
	}

	@Test
	public void propertyWritesAndRollbacksPreserveAllTimes() {
		Athlete athlete = recordedAthlete();
		List<LocalDateTime> original = times(athlete);
		for (int attempt = 1; attempt <= 6; attempt++) {
			for (String value : Arrays.asList("100", "101", "-100", "", null, "100")) {
				athlete.setActualLift(attempt, value);
				assertEquals(original, times(athlete));
			}
		}
		athlete.setSnatch2Declaration("105");
		athlete.setCleanJerk3Change1("110");
		assertEquals(original, times(athlete));
	}

	@Test
	public void liveRecordingChangesOnlySelectedTime() {
		for (int attempt = 1; attempt <= 6; attempt++) {
			Athlete athlete = recordedAthlete();
			List<LocalDateTime> expected = times(athlete);
			expected.set(attempt - 1, START.plusHours(1));
			athlete.recordLift(attempt, "-105", START.plusHours(1));
			assertEquals(expected, times(athlete));
			assertEquals(Integer.valueOf(-105), athlete.getActualLift(attempt));
		}
	}

	@Test
	public void rejectedRecordingDoesNotChangeResultOrTime() {
		Athlete athlete = recordedAthlete();
		List<LocalDateTime> original = times(athlete);
		assertThrows(IllegalArgumentException.class, () -> athlete.recordLift(7, "100", START));
		assertThrows(IllegalArgumentException.class, () -> athlete.recordLift(1, "0", START));
		assertThrows(IllegalArgumentException.class, () -> athlete.recordLift(1, "", START));
		assertThrows(IllegalArgumentException.class, () -> athlete.recordLift(1, "100", null));
		athlete.setSnatch1Declaration("100");
		athlete.setValidation(true);
		assertThrows(RuntimeException.class, () -> athlete.recordLift(1, "105", START));
		assertEquals(Integer.valueOf(100), athlete.getActualLift(1));
		assertEquals(original, times(athlete));
	}

	@Test
	public void missingTimeIsNotInventedByCorrection() {
		Athlete athlete = recordedAthlete();
		athlete.setCleanJerk2LiftTime(null);
		athlete.setActualLift(5, "-100");
		assertNull(athlete.getCleanJerk2LiftTime());
		assertEquals(START.plusMinutes(4), athlete.getCleanJerk1LiftTime());
	}

	@Test
	public void explicitClearingAndWithdrawalClearTimes() {
		Athlete athlete = recordedAthlete();
		athlete.setActualLift(6, "");
		athlete.withdraw();
		assertNull(athlete.getCleanJerk3LiftTime());
		assertEquals(START.plusMinutes(5), athlete.getCleanJerk2LiftTime());
		athlete.clearLifts();
		for (LocalDateTime time : times(athlete)) {
			assertNull(time);
		}
	}

	@Test
	public void wrappersDelegateRecordingCorrectionAndRestoration() throws Exception {
		for (boolean participationWrapper : List.of(false, true)) {
			Athlete athlete = recordedAthlete();
			Athlete wrapper = participationWrapper ? new PAthlete(athlete) : new XAthlete(athlete);
			for (int index = 0; index < PREFIXES.length; index++) {
				String suffix = Character.toUpperCase(PREFIXES[index].charAt(0)) + PREFIXES[index].substring(1);
				Athlete.class.getMethod("set" + suffix + "LiftTime", LocalDateTime.class).invoke(wrapper, START);
				Athlete.class.getMethod("set" + suffix + "ActualLift", String.class).invoke(wrapper, "-100");
				assertEquals(START, times(athlete).get(index));
				assertEquals("-100", Athlete.class.getMethod("get" + suffix + "ActualLift").invoke(wrapper));
				wrapper.recordLift(index + 1, "100", START.plusHours(1));
				wrapper.setActualLift(index + 1, "-100");
				assertEquals(START.plusHours(1), times(athlete).get(index));
			}
		}
	}

	@Test
	public void v1PropertyOrderAndNullTimesArePreserved() throws Exception {
		ObjectMapper mapper = mapper();
		Athlete source = recordedAthlete();
		source.setSnatch3LiftTime(null);
		ObjectNode json = mapper.createObjectNode();
		List<LocalDateTime> original = times(source);
		for (int index = 0; index < PREFIXES.length; index++) {
			json.set(PREFIXES[index] + "LiftTime", mapper.valueToTree(original.get(index)));
			json.put(PREFIXES[index] + "ActualLift", "100");
		}
		boolean previous = Athlete.isSkipValidationsDuringImport();
		try {
			Athlete.setSkipValidationsDuringImport(true);
			assertEquals(original, times(mapper.treeToValue(json, Athlete.class)));
		} finally {
			Athlete.setSkipValidationsDuringImport(previous);
		}
	}

	@Test
	public void v2AndCopiesPreserveTimes() {
		Athlete source = recordedAthlete();
		source.setSnatch3LiftTime(null);
		boolean previous = Athlete.isSkipValidationsDuringImport();
		try {
			Athlete.setSkipValidationsDuringImport(true);
			Athlete restored = AthleteDTO.fromAthlete(source).toAthlete(null);
			assertEquals(times(source), times(restored));
			Athlete copied = new Athlete();
			Athlete.conditionalCopy(copied, source, true, true, false);
			assertEquals(times(source), times(copied));
		} finally {
			Athlete.setSkipValidationsDuringImport(previous);
		}
	}

	@Test
	public void offlineSessionImportRestoresArraysAndNulls() throws Exception {
		Athlete target = AthleteRepository.findAll().stream()
				.filter(athlete -> athlete.getGroup() != null).findFirst().orElseThrow();
		ObjectMapper mapper = mapper();
		ObjectNode root = mapper.createObjectNode();
		ObjectNode json = root.putArray("athletes").addObject();
		json.put("id", target.getId());
		json.put("group", target.getGroup().getId());
		for (int index = 0; index < PREFIXES.length; index++) {
			json.put(PREFIXES[index] + "ActualLift", "100");
			json.set(PREFIXES[index] + "LiftTime",
					mapper.valueToTree(index == 2 ? null : START.plusMinutes(index + 1)));
		}
		AthleteSessionDataReader.importAthletes(new ByteArrayInputStream(mapper.writeValueAsBytes(root)),
				List.of(target.getGroup()));
		Athlete restored = AthleteRepository.findById(target.getId());
		for (int index = 0; index < PREFIXES.length; index++) {
			assertEquals(index == 2 ? null : START.plusMinutes(index + 1), times(restored).get(index));
			assertEquals(Integer.valueOf(100), restored.getActualLift(index + 1));
		}
	}

	@Test
	public void sessionEndTimesAreRecordedOnlyByLiveDecisions() throws Exception {
		for (int attempt : List.of(3, 6)) {
			Athlete athlete = AthleteRepository.findAll().stream()
					.filter(candidate -> candidate.getGroup() != null).findFirst().orElseThrow();
			athlete.setValidation(false);
			athlete.clearLifts();
			athlete.setSnatch1Declaration("100");
			athlete.setCleanJerk1Declaration("100");
			for (int previous = 1; previous < attempt; previous++) {
				athlete.recordLift(previous, "100", START.plusMinutes(previous));
			}
			FieldOfPlay fop = MockFieldOfPlay.create(new ArrayList<>(List.of(athlete)),
					new MockCountdownTimer(), new MockCountdownTimer());
			fop.setGroup(athlete.getGroup());
			fop.getGroup().setLastSnatchDecisionTime(null, fop.getGroup(), fop);
			fop.getGroup().setLastCJDecisionTime(null, fop.getGroup(), fop);
			Method setCurrent = FieldOfPlay.class.getDeclaredMethod("setCurAthlete", Athlete.class);
			setCurrent.setAccessible(true);
			setCurrent.invoke(fop, athlete);
			Method setOrder = FieldOfPlay.class.getDeclaredMethod("setLiftingOrder", List.class);
			setOrder.setAccessible(true);
			setOrder.invoke(fop, new ArrayList<>(List.of(athlete)));
			fop.setRefereeDecision(new Boolean[] { true, true, true });
			fop.setChallengedRecords(List.of());
			Method commit = FieldOfPlay.class.getDeclaredMethod("commitCurrentDecision");
			commit.setAccessible(true);
			commit.invoke(fop);

			LocalDateTime decisionTime = times(athlete).get(attempt - 1);
			assertTrue(decisionTime != null);
			assertEquals(attempt == 3 ? decisionTime : null, fop.getGroup().getLastSnatchDecisionTime());
			assertEquals(attempt == 6 ? decisionTime : null, fop.getGroup().getLastCJDecisionTime());
			athlete.setActualLift(attempt, "-100");
			athlete.setActualLift(attempt, "");
			athlete.setActualLift(attempt, "100");
			assertEquals(decisionTime, times(athlete).get(attempt - 1));
			assertEquals(attempt == 3 ? decisionTime : null, fop.getGroup().getLastSnatchDecisionTime());
			assertEquals(attempt == 6 ? decisionTime : null, fop.getGroup().getLastCJDecisionTime());
		}
	}

	@Test
	public void crossSessionSnatchTiebreakSurvivesResultRewrites() {
		Athlete earlier = recordedAthlete();
		Athlete later = recordedAthlete();
		earlier.setGroup(new Group("Earlier"));
		later.setGroup(new Group("Later"));
		later.recordLift(1, "100", START.plusHours(1));
		WinningOrderComparator comparator = new WinningOrderComparator(Ranking.SNATCH, true);
		assertTrue(comparator.compareSnatchResultOrder(earlier, later, true) < 0);
		for (int attempt = 1; attempt <= 6; attempt++) {
			earlier.setActualLift(attempt, "-100");
			earlier.setActualLift(attempt, "100");
		}
		assertTrue(comparator.compareSnatchResultOrder(earlier, later, true) < 0);
	}

	private static Athlete recordedAthlete() {
		Athlete athlete = new Athlete();
		athlete.setValidation(false);
		for (int attempt = 1; attempt <= 6; attempt++) {
			athlete.recordLift(attempt, "100", START.plusMinutes(attempt));
		}
		return athlete;
	}

	private static List<LocalDateTime> times(Athlete athlete) {
		return Arrays.asList(athlete.getSnatch1LiftTime(), athlete.getSnatch2LiftTime(),
				athlete.getSnatch3LiftTime(), athlete.getCleanJerk1LiftTime(),
				athlete.getCleanJerk2LiftTime(), athlete.getCleanJerk3LiftTime());
	}

	private static ObjectMapper mapper() {
		return new ObjectMapper().registerModule(new JavaTimeModule());
	}
}
