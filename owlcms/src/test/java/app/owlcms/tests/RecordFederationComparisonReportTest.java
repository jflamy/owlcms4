package app.owlcms.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.nui.admin.RecordFederationComparisonReport.BlankEligibilityAthlete;
import app.owlcms.nui.admin.RecordFederationComparisonReport;
import app.owlcms.nui.admin.RecordFederationComparisonReport.FederationParticipationSummary;
import app.owlcms.nui.admin.RecordFederationComparisonReport.ReportData;

public class RecordFederationComparisonReportTest {

	@BeforeClass
	public static void setupTests() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
	}

	@AfterClass
	public static void tearDownTests() {
		JPAService.close();
	}

	@Test
	public void buildSummaryFlagsFederationsWithoutMatches() {
		RecordEvent usRecord1 = new RecordEvent();
		usRecord1.setRecordFederation("US");
		RecordEvent usRecord2 = new RecordEvent();
		usRecord2.setRecordFederation("US");
		RecordEvent caRecord = new RecordEvent();
		caRecord.setRecordFederation("CA");

		Athlete usAthlete = new Athlete();
		usAthlete.setFederationCodes("US");
		Athlete mxAthlete = new Athlete();
		mxAthlete.setFederationCodes("MX");

		List<FederationParticipationSummary> summary = RecordFederationComparisonReport.buildSummary(
			        List.of(usRecord1, usRecord2, caRecord),
			        List.of(usAthlete, mxAthlete));

		Map<String, FederationParticipationSummary> byFederation = summary.stream()
			        .collect(Collectors.toMap(FederationParticipationSummary::getFederation, Function.identity()));

		assertEquals(3, summary.size());
		assertEquals(2, byFederation.get("US").getLoadedRecordsCount());
		assertEquals(1, byFederation.get("US").getAthleteCount());
		assertFalse(byFederation.get("US").isMissingAthletes());
		assertFalse(byFederation.get("US").isMissingLoadedRecords());

		assertEquals(1, byFederation.get("CA").getLoadedRecordsCount());
		assertEquals(0, byFederation.get("CA").getAthleteCount());
		assertTrue(byFederation.get("CA").isMissingAthletes());
		assertFalse(byFederation.get("CA").isMissingLoadedRecords());

		assertEquals(0, byFederation.get("MX").getLoadedRecordsCount());
		assertEquals(1, byFederation.get("MX").getAthleteCount());
		assertFalse(byFederation.get("MX").isMissingAthletes());
		assertTrue(byFederation.get("MX").isMissingLoadedRecords());
	}

	@Test
	public void blankFederationCodesCountAsAllLoadedFederations() {
		RecordEvent usRecord = new RecordEvent();
		usRecord.setRecordFederation("US");
		RecordEvent caRecord = new RecordEvent();
		caRecord.setRecordFederation("CA");

		Athlete athleteWithNoFederationCodes = new Athlete();

		List<FederationParticipationSummary> summary = RecordFederationComparisonReport.buildSummary(
			        List.of(usRecord, caRecord),
			        List.of(athleteWithNoFederationCodes));

		Map<String, FederationParticipationSummary> byFederation = summary.stream()
			        .collect(Collectors.toMap(FederationParticipationSummary::getFederation, Function.identity()));

		assertEquals(1, byFederation.get("US").getAthleteCount());
		assertEquals(1, byFederation.get("CA").getAthleteCount());
	}

	@Test
	public void blankFederationAthletesAppearInHtmlSectionWithDelimitedEligibilityData() {
		RecordEvent usRecord = new RecordEvent();
		usRecord.setRecordFederation("US");

		Athlete nullCodesAthlete = new Athlete();
		nullCodesAthlete.setLastName("Null");
		nullCodesAthlete.setFirstName("Case");

		Athlete spacesCodesAthlete = new Athlete();
		spacesCodesAthlete.setLastName("Spaces");
		spacesCodesAthlete.setFirstName("Case");
		spacesCodesAthlete.setFederationCodes("   ");

		ReportData report = RecordFederationComparisonReport.buildReport(
		        List.of(usRecord),
		        List.of(nullCodesAthlete, spacesCodesAthlete));

		List<BlankEligibilityAthlete> blankEligibilityAthletes = report.getBlankEligibilityAthletes();
		assertEquals(2, blankEligibilityAthletes.size());
		assertEquals("null", blankEligibilityAthletes.get(0).getDisplayEligibilityData());
		assertEquals("   ", blankEligibilityAthletes.get(1).getDisplayEligibilityData());

		String html = RecordFederationComparisonReport.buildHtmlContent(report);
		assertTrue(html.contains("Athletes With Blank Or Malformed Eligibility Data"));
		assertTrue(html.contains(blankEligibilityAthletes.get(0).getAthleteName()));
		assertTrue(html.contains(blankEligibilityAthletes.get(1).getAthleteName()));
		assertTrue(html.contains(blankEligibilityAthletes.get(0).getIssueDescription()));
		assertTrue(html.contains("&laquo;null&raquo;"));
		assertTrue(html.contains("&laquo;   &raquo;"));
	}

	@Test
	public void malformedFederationCodesCausingEmptyFederationAppearInHtmlSection() {
		RecordEvent usRecord = new RecordEvent();
		usRecord.setRecordFederation("US");

		Athlete malformedAthlete = new Athlete();
		malformedAthlete.setLastName("Malformed");
		malformedAthlete.setFirstName("Case");
		malformedAthlete.setFederationCodes("US, ,CA");

		ReportData report = RecordFederationComparisonReport.buildReport(List.of(usRecord), List.of(malformedAthlete));

		List<BlankEligibilityAthlete> flaggedEligibilityAthletes = report.getBlankEligibilityAthletes();
		assertEquals(1, flaggedEligibilityAthletes.size());
		assertEquals("US, ,CA", flaggedEligibilityAthletes.get(0).getDisplayEligibilityData());
		assertTrue(flaggedEligibilityAthletes.get(0).getIssueDescription().contains("empty federation row"));

		Map<String, FederationParticipationSummary> byFederation = report.getSummaries().stream()
		        .collect(Collectors.toMap(FederationParticipationSummary::getFederation, Function.identity()));
		assertEquals(1, byFederation.get("").getAthleteCount());

		String html = RecordFederationComparisonReport.buildHtmlContent(report);
		assertTrue(html.contains("Athletes With Blank Or Malformed Eligibility Data"));
		assertTrue(html.contains(flaggedEligibilityAthletes.get(0).getAthleteName()));
		assertTrue(html.contains(flaggedEligibilityAthletes.get(0).getIssueDescription()));
		assertTrue(html.contains("&laquo;US, ,CA&raquo;"));
	}
}
