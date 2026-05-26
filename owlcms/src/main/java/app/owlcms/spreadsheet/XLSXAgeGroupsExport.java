/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.i18n.Translator;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class XLSXAgeGroupsExport extends XLSXWorkbookStreamSource {

	private static final String AGE_GROUPS_SHEET_NAME = "AgeGroups";
	private static final String CHAMPIONSHIPS_SHEET_NAME = "Championships";

	public XLSXAgeGroupsExport(UI ui) {
		super(ui);
	}

	Logger logger = (Logger) LoggerFactory.getLogger(XLSXAgeGroupsExport.class);

	@Override
	@SuppressWarnings("unchecked")
	protected void writeStream(OutputStream stream) {
		Workbook workbook = null;
		try {
			workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet(AGE_GROUPS_SHEET_NAME);
			Row header = sheet.createRow(0);
			header.createCell(0).setCellValue("code");
			header.createCell(1).setCellValue("championship");
			header.createCell(2).setCellValue("gender");
			header.createCell(3).setCellValue("from");
			header.createCell(4).setCellValue("to");
			header.createCell(5).setCellValue("active");
			header.createCell(6).setCellValue("agegroupscoring");
			header.createCell(7).setCellValue("agegroupbestathlete");

			List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
			ageGroups.sort(Comparator
			        .comparing(AgeGroup::getChampionship)
			        .thenComparing(AgeGroup::getGender).reversed()
			        .thenComparing(AgeGroup::getMaxAge));

			int rowNum = 1;
			for (AgeGroup ag : ageGroups) {
				Row curRow = sheet.createRow(rowNum);
				curRow.createCell(0).setCellValue((ag.isAlreadyGendered() ? "!" : "") + ag.getCode());
				String championshipName = ag.getChampionshipName();
				if (championshipName != null && !championshipName.equalsIgnoreCase(ag.getCode())) {
					curRow.createCell(1).setCellValue(championshipName);
				}
				curRow.createCell(2).setCellValue(ag.getGender().name());
				curRow.createCell(3).setCellValue(ag.getMinAge());
				curRow.createCell(4).setCellValue(ag.getMaxAge());
				curRow.createCell(5).setCellValue(ag.isActive());
				Ranking scoringSystem = ag.getComputedScoringSystem();
				curRow.createCell(6).setCellValue(scoringSystem == Ranking.TOTAL ? "" : scoringSystem.getReportingName());
				Ranking bestScoringSystem = ag.getBestAthleteScoringSystem();
				curRow.createCell(7).setCellValue(bestScoringSystem != null ? bestScoringSystem.getReportingName() : "");

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
			writeChampionshipsSheet(workbook.createSheet(CHAMPIONSHIPS_SHEET_NAME));
			workbook.write(stream);
			if (this.doneCallback != null) {
				this.doneCallback.accept(null);
			}
			stream.close();
		} catch (Exception e) {
			LoggerUtils.logError(this.logger, e);
		}
	}

	public Optional<Exception> preCheck() {
		try {
			List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
			if (ageGroups == null || ageGroups.isEmpty()) {
				return Optional.of(new StopProcessingException(Translator.translate("export.noAgeGroups"), null));
			}
			return Optional.empty();
		} catch (Exception e) {
			return Optional.of(e);
		}
	}

	@Override
	public Optional<Exception> prepare() {
		return preCheck();
	}

	private void writeChampionshipsSheet(Sheet sheet) {
		String[] headers = {
		        "name",
		        "type",
		        "useCompetitionDefaults",
		        "scoringSystem",
		        "bestAthleteScoringSystem",
		        "bestSnatchScoringSystem",
		        "bestCJScoringSystem",
		        "snatchCJTotalMedals",
		        "teamPoints1st",
		        "teamPoints2nd",
		        "teamPoints3rd",
		        "mensBestN",
		        "womensBestN",
		        "teamScoringSystem",
		        "maxTeamSize",
		        "maxPerCategory",
		        "mixedTeamEnabled",
		        "mixedTeamScoringSystem",
		        "explicitMixedTeamMembers",
		        "explicitTeamSize",
		        "mixedBestN",
		        "mixedMensBestN",
		        "mixedWomensBestN"
		};
		Row header = sheet.createRow(0);
		for (int i = 0; i < headers.length; i++) {
			header.createCell(i).setCellValue(headers[i]);
		}

		int rowNum = 1;
		for (Championship championship : Championship.findAll()) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(championship.getName());
			row.createCell(1).setCellValue(championship.getType().name());
			row.createCell(2).setCellValue(championship.usesCompetitionDefaults());
			setRankingCell(row, 3, championship.getScoringSystem());
			setRankingCell(row, 4, championship.getBestAthleteScoringSystem());
			setRankingCell(row, 5, championship.getBestSnatchScoringSystem());
			setRankingCell(row, 6, championship.getBestCJScoringSystem());
			row.createCell(7).setCellValue(championship.isSnatchCJTotalMedals());
			setIntegerCell(row, 8, championship.getTeamPoints1st());
			setIntegerCell(row, 9, championship.getTeamPoints2nd());
			setIntegerCell(row, 10, championship.getTeamPoints3rd());
			setIntegerCell(row, 11, championship.getMensBestN());
			setIntegerCell(row, 12, championship.getWomensBestN());
			setRankingCell(row, 13, championship.getTeamScoringSystem());
			setIntegerCell(row, 14, championship.getMaxTeamSize());
			setIntegerCell(row, 15, championship.getMaxPerCategory());
			row.createCell(16).setCellValue(championship.isMixedTeamEnabled());
			setRankingCell(row, 17, championship.getMixedTeamScoringSystem());
			row.createCell(18).setCellValue(championship.isExplicitMixedTeamMembers());
			setIntegerCell(row, 19, championship.getExplicitTeamSize());
			setIntegerCell(row, 20, championship.getMixedBestN());
			setIntegerCell(row, 21, championship.getMixedMensBestN());
			setIntegerCell(row, 22, championship.getMixedWomensBestN());
		}
	}

	private void setIntegerCell(Row row, int column, Integer value) {
		if (value != null) {
			row.createCell(column).setCellValue(value);
		}
	}

	private void setRankingCell(Row row, int column, Ranking ranking) {
		if (ranking != null) {
			row.createCell(column).setCellValue(ranking.getReportingName());
		}
	}

}
