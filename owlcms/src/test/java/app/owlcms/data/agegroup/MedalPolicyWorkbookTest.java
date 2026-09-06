package app.owlcms.data.agegroup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import app.owlcms.Main;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.jpa.JPAService;

public class MedalPolicyWorkbookTest {

	@BeforeClass
	public static void setup() {
		Main.injectSuppliers();
		JPAService.init(true, true);
		Config.initConfig();
		CompetitionRepository.save(new Competition());
	}

	@AfterClass
	public static void tearDown() {
		JPAService.close();
	}

	@Test
	public void legacyWorkbookIgnoresPreviousMedalAndPointPolicies() throws Exception {
		importWorkbook(true, MedalPolicy.LIFTS_ONLY, TeamPointsPolicy.LIFTS_ONLY);
		importWorkbook(true, null, null);
		assertPolicies(MedalPolicy.ALL_THREE, TeamPointsPolicy.ALL_THREE);
		importWorkbook(false, null, null);
		assertPolicies(MedalPolicy.TOTAL_ONLY, TeamPointsPolicy.TOTAL_ONLY);
	}

	@Test
	public void explicitWorkbookPoliciesConstrainTeamPoints() throws Exception {
		for (MedalPolicy medals : MedalPolicy.values()) {
			for (TeamPointsPolicy points : TeamPointsPolicy.values()) {
				importWorkbook(false, medals, points);
				assertPolicies(medals, TeamPointsPolicy.effective(points, medals));
			}
		}
	}

	private void assertPolicies(MedalPolicy medals, TeamPointsPolicy points) {
		for (Championship championship : List.of(ChampionshipRepository.ensureCompetitionTemplate(),
		        ChampionshipRepository.findByName("Workbook"))) {
			assertNotNull(championship);
			assertEquals(medals, championship.getMedalPolicy());
			assertEquals(points, championship.getTeamPointsPolicy());
			assertEquals(Integer.valueOf(28), championship.getTeamPoints1st());
			assertEquals(Integer.valueOf(25), championship.getTeamPoints2nd());
			assertEquals(Integer.valueOf(23), championship.getTeamPoints3rd());
		}
	}

	private void importWorkbook(boolean legacyLiftMedals, MedalPolicy medals, TeamPointsPolicy points) throws Exception {
		List<String> errors = new ArrayList<>();
		AgeGroupDefinitionReader.setErrorCollector(errors::add);
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			workbook.createSheet("AgeGroups");
			Sheet sheet = workbook.createSheet("Championships");
			List<String> headers = new ArrayList<>(List.of("name", "type", "competitionTemplate",
			        "snatchCJTotalMedals", "teamPoints1st", "teamPoints2nd", "teamPoints3rd", "maxTeamSize"));
			if (medals != null) {
				headers.add("medalPolicy");
				headers.add("teamPointsPolicy");
			}
			Row header = sheet.createRow(0);
			for (int column = 0; column < headers.size(); column++) {
				header.createCell(column).setCellValue(headers.get(column));
			}
			for (int index = 1; index <= 2; index++) {
				Row row = sheet.createRow(index);
				row.createCell(0).setCellValue(index == 1 ? Championship.COMPETITION_TEMPLATE_NAME : "Workbook");
				row.createCell(1).setCellValue("U");
				row.createCell(2).setCellValue(index == 1);
				row.createCell(3).setCellValue(legacyLiftMedals);
				row.createCell(4).setCellValue(28);
				row.createCell(5).setCellValue(25);
				row.createCell(6).setCellValue(23);
				row.createCell(7).setCellValue(8);
				if (medals != null) {
					row.createCell(8).setCellValue(medals.name());
					row.createCell(9).setCellValue(points.name());
				}
			}
			workbook.write(output);
			try (Workbook imported = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
				AgeGroupDefinitionReader.createAgeGroups(imported, null, "Medal policy test");
			}
			assertTrue(errors.toString(), errors.isEmpty());
		} finally {
			AgeGroupDefinitionReader.setErrorCollector(null);
		}
	}
}