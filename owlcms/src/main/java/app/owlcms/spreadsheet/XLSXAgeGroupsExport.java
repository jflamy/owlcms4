/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.ChampionshipRepository;
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
			ChampionshipRepository.normalizeDefaultTypes();
			ChampionshipRepository.normalizeCompetitionDefaultFlags();
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
			writeChampionshipsSheet(workbook.createSheet(CHAMPIONSHIPS_SHEET_NAME), ageGroups);
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

	private void writeChampionshipsSheet(Sheet sheet, List<AgeGroup> ageGroups) {
		String[] headers = {
		        "name",
		        "type",
		        "competitionTemplate",
		        "useCompetitionDefaults",
		        "snatchCJTotalMedals",
		        "scoringSystem",
		        "bestAthleteScoringSystem",
		        "bestSnatchScoringSystem",
		        "bestCJScoringSystem",
		        "teamPoints1st",
		        "teamPoints2nd",
		        "teamPoints3rd",
		        "teamScoringSystem",
		        "maxTeamSize",
		        "maxPerCategory",
		        "mensBestN",
		        "womensBestN",
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

		List<Championship> championships = findChampionshipsForExport(ageGroups);
		int rowNum = 1;
		for (Championship championship : championships) {
			Row row = sheet.createRow(rowNum++);
			row.createCell(0).setCellValue(championship.isCompetitionTemplate() ? Championship.COMPETITION_TEMPLATE_NAME : championship.getName());
			row.createCell(1).setCellValue(championship.getType().name());
			row.createCell(2).setCellValue(championship.isCompetitionTemplate());
			row.createCell(3).setCellValue(championship.usesCompetitionDefaults());
			row.createCell(4).setCellValue(championship.isSnatchCJTotalMedals());
			setRankingCell(row, 5, championship.getScoringSystem());
			setRankingCell(row, 6, championship.getBestAthleteScoringSystem());
			setRankingCell(row, 7, championship.getBestSnatchScoringSystem());
			setRankingCell(row, 8, championship.getBestCJScoringSystem());
			setIntegerCell(row, 9, championship.getTeamPoints1st());
			setIntegerCell(row, 10, championship.getTeamPoints2nd());
			setIntegerCell(row, 11, championship.getTeamPoints3rd());
			setRankingCell(row, 12, championship.getTeamScoringSystem());
			setIntegerCell(row, 13, normalizeTeamSize(championship.getMaxTeamSize()));
			setIntegerCell(row, 14, championship.getMaxPerCategory());
			setIntegerCell(row, 15, championship.getMensBestN());
			setIntegerCell(row, 16, championship.getWomensBestN());
			row.createCell(17).setCellValue(championship.isMixedTeamEnabled());
			setRankingCell(row, 18, championship.getMixedTeamScoringSystem());
			row.createCell(19).setCellValue(championship.isExplicitMixedTeamMembers());
			setIntegerCell(row, 20, championship.getExplicitTeamSize());
			setIntegerCell(row, 21, championship.getMixedBestN());
			setIntegerCell(row, 22, championship.getMixedMensBestN());
			setIntegerCell(row, 23, championship.getMixedWomensBestN());
		}
	}

	private List<Championship> findChampionshipsForExport(List<AgeGroup> ageGroups) {
		Map<String, Championship> championships = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		Championship template = null;
		for (Championship championship : Championship.findAllIncludingTemplate()) {
			String key = championship.isCompetitionTemplate()
			        ? Championship.COMPETITION_TEMPLATE_NAME
			        : Championship.canonicalizeChampionshipName(championship.getName());
			championships.put(key, championship);
			if (championship.isCompetitionTemplate()) {
				template = championship;
			}
		}
		if (template == null) {
			template = ChampionshipRepository.ensureCompetitionTemplate();
			championships.put(Championship.COMPETITION_TEMPLATE_NAME, template);
		}

		for (AgeGroup ageGroup : ageGroups) {
			String championshipName = Championship.canonicalizeChampionshipName(ageGroup.computeChampionshipName());
			if (championshipName == null || championshipName.isBlank() || championships.containsKey(championshipName)) {
				continue;
			}
			Championship championship = new Championship(championshipName, ageGroup.getChampionshipType());
			championship.copyCompetitionSettingsFrom(template);
			championship.setUseCompetitionDefaults(true);
			championships.put(championshipName, championship);
		}

		List<Championship> exportChampionships = new ArrayList<>(championships.values());
		exportChampionships.sort(Comparator.comparing(Championship::isCompetitionTemplate).reversed()
		        .thenComparing(Championship::compareTo));
		return exportChampionships;
	}

	private static final int LEGACY_UNBOUNDED_TEAM_SIZE = 50;
	private static final int UNBOUNDED_TEAM_SIZE = 999;

	private static Integer normalizeTeamSize(Integer size) {
		return (size != null && size == LEGACY_UNBOUNDED_TEAM_SIZE) ? UNBOUNDED_TEAM_SIZE : size;
	}

	private void setIntegerCell(Row row, int column, Integer value) {
		if (value != null) {
			row.createCell(column).setCellValue(value);
		}
	}

	private static final String POINTS_SENTINEL = "POINTS";

	private void setRankingCell(Row row, int column, Ranking ranking) {
		row.createCell(column).setCellValue(ranking != null ? ranking.getReportingName() : POINTS_SENTINEL);
	}

}
