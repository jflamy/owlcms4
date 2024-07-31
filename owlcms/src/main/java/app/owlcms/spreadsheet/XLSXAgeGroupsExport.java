package app.owlcms.spreadsheet;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXAgeGroupsExport extends XLSXWorkbookStreamSource {

	Logger logger = (Logger) LoggerFactory.getLogger(XLSXAgeGroupsExport.class);

	@Override
	@SuppressWarnings("unchecked")
	protected void writeStream(OutputStream stream) {
		Workbook workbook = null;
		try {
			workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet();
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("code");
			header.createCell(1).setCellValue("championship");
			header.createCell(2).setCellValue("championshipType");
			header.createCell(3).setCellValue("gender");
			header.createCell(4).setCellValue("from");
			header.createCell(5).setCellValue("to");
			header.createCell(6).setCellValue("active");
			header.createCell(7).setCellValue("agegroupscoring");

			List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
			ageGroups.sort(Comparator
			        .comparing(AgeGroup::getChampionship)
			        .thenComparing(AgeGroup::getGender).reversed()
			        .thenComparing(AgeGroup::getMaxAge));

			int rowNum = 1;
			for (AgeGroup ag : ageGroups) {
				Row curRow = sheet.createRow(rowNum);
				curRow.createCell(0).setCellValue(ag.getCode());
				curRow.createCell(1).setCellValue(ag.getChampionship().getName());
				curRow.createCell(2).setCellValue(ag.getChampionshipType().name());
				curRow.createCell(3).setCellValue(ag.getGender().name());
				curRow.createCell(4).setCellValue(ag.getMinAge());
				curRow.createCell(5).setCellValue(ag.getMaxAge());
				curRow.createCell(6).setCellValue(ag.isActive());
				Ranking scoringSystem = ag.getScoringSystem();
				curRow.createCell(7).setCellValue(scoringSystem == Ranking.TOTAL ? "" : scoringSystem.name());

				int cellNum = 8;
				for (Category cat : ag.getCategories()) {
					Double maximumWeight = cat.getMaximumWeight();
					int val = (int) (maximumWeight + 0.5);
					int qt = cat.getQualifyingTotal();
					curRow.createCell(cellNum).setCellValue(val + (qt > 0 ? (" " + qt) : ""));
					cellNum++;
				}
				rowNum++;
			}
			workbook.write(stream);
			if (this.doneCallback != null) {
				this.doneCallback.accept(null);
			}
			stream.close();
		} catch (Exception e) {
			LoggerUtils.logError(this.logger, e);
		}
	}

}
