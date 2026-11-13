package app.owlcms.spreadsheet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import app.owlcms.data.athleteSort.Ranking;

public class XLSXAgeGroupsExportTest {

	@Test
	public void scoringCellsUseModernReportingNames() throws Exception {
		try (Workbook workbook = new XSSFWorkbook()) {
			Row row = workbook.createSheet("Championships").createRow(0);

			XLSXAgeGroupsExport.setNullableRankingCell(row, 0, Ranking.CAT_SINCLAIR);
			XLSXAgeGroupsExport.setNullableRankingCell(row, 1, Ranking.QAGE);
			XLSXAgeGroupsExport.setNullableRankingCell(row, 2, Ranking.GAMX_M);
			XLSXAgeGroupsExport.setNullableRankingCell(row, 3, Ranking.SMM);
			XLSXAgeGroupsExport.setNullableRankingCell(row, 4, Ranking.AGEFACTORS);
			XLSXAgeGroupsExport.setNullableRankingCell(row, 5, null);

			assertEquals("CatSinclair", row.getCell(0).getStringCellValue());
			assertEquals("QMasters", row.getCell(1).getStringCellValue());
			assertEquals("GAMX-M", row.getCell(2).getStringCellValue());
			assertEquals("SMHF", row.getCell(3).getStringCellValue());
			assertEquals("QYouth", row.getCell(4).getStringCellValue());
			assertNull(row.getCell(5));
		}
	}

	@Test
	public void teamScoringCellsUseModernNamesAndPreservePointsAndDisabledStates() throws Exception {
		try (Workbook workbook = new XSSFWorkbook()) {
			Row row = workbook.createSheet("Championships").createRow(0);

			XLSXAgeGroupsExport.setTeamScoringCell(row, 0, true, Ranking.CAT_SINCLAIR);
			XLSXAgeGroupsExport.setTeamScoringCell(row, 1, true, null);
			XLSXAgeGroupsExport.setTeamScoringCell(row, 2, false, Ranking.CAT_SINCLAIR);
			XLSXAgeGroupsExport.setTeamScoringCell(row, 3, false, null);

			assertEquals("CatSinclair", row.getCell(0).getStringCellValue());
			assertEquals("POINTS", row.getCell(1).getStringCellValue());
			assertNull(row.getCell(2));
			assertNull(row.getCell(3));
		}
	}

	@Test
	public void exportedScoringNamesAreAcceptedByTheImportLookup() throws Exception {
		try (Workbook workbook = new XSSFWorkbook()) {
			Row row = workbook.createSheet("Championships").createRow(0);
			for (Ranking ranking : Ranking.values()) {
				XLSXAgeGroupsExport.setNullableRankingCell(row, 0, ranking);
				String exported = row.getCell(0).getStringCellValue();
				assertEquals(ranking, Ranking.rankingByReportingName.get(exported.toLowerCase(Locale.ROOT)));
			}
		}
	}
}
