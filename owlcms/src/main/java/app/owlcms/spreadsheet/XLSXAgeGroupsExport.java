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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import app.owlcms.data.agegroup.ChampionshipType;
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

			List<AgeGroup> ageGroups = AgeGroupRepository.findAll();
			ageGroups.sort(Comparator
			        .comparing(AgeGroup::getChampionship)
			        .thenComparing(AgeGroup::getGender).reversed()
			        .thenComparing(AgeGroup::getMaxAge));

			int rowNum = 1;
			for (AgeGroup ag : ageGroups) {
				Row curRow = sheet.createRow(rowNum);
				curRow.createCell(0).setCellValue((ag.isAlreadyGendered() ? "!" : "") + ag.getCode());
				String championshipName = effectiveChampionshipName(ag);
				if (championshipName != null && !championshipName.equalsIgnoreCase(ag.getCode())) {
					curRow.createCell(1).setCellValue(championshipName);
				}
				curRow.createCell(2).setCellValue(ag.getGender().name());
				curRow.createCell(3).setCellValue(ag.getMinAge());
				curRow.createCell(4).setCellValue(ag.getMaxAge());
				curRow.createCell(5).setCellValue(ag.isActive());
				int cellNum = 6;
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
			row.createCell(3).setCellValue(championship.isSnatchCJTotalMedals());
			setNullableRankingCell(row, 4, championship.getScoringSystem());
			setNullableRankingCell(row, 5, championship.getBestAthleteScoringSystem());
			setNullableRankingCell(row, 6, championship.getBestSnatchScoringSystem());
			setNullableRankingCell(row, 7, championship.getBestCJScoringSystem());
			setIntegerCell(row, 8, championship.getTeamPoints1st());
			setIntegerCell(row, 9, championship.getTeamPoints2nd());
			setIntegerCell(row, 10, championship.getTeamPoints3rd());
			setTeamScoringCell(row, 11, championship.isGenderedTeamsEnabled(), championship.getTeamScoringSystem());
			setIntegerCell(row, 12, normalizeTeamSize(championship.getMaxTeamSize()));
			setIntegerCell(row, 13, championship.getMaxPerCategory());
			setIntegerCell(row, 14, championship.getMensBestN());
			setIntegerCell(row, 15, championship.getWomensBestN());
			setTeamScoringCell(row, 16, championship.isMixedTeamEnabled(), championship.getMixedTeamScoringSystem());
			row.createCell(17).setCellValue(championship.isExplicitMixedTeamMembers());
			setIntegerCell(row, 18, championship.getExplicitTeamSize());
			setIntegerCell(row, 19, championship.getMixedBestN());
			setIntegerCell(row, 20, championship.getMixedMensBestN());
			setIntegerCell(row, 21, championship.getMixedWomensBestN());
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
		Set<String> explicitAgeGroupChampionshipNames = explicitAgeGroupChampionshipNames(ageGroups);
		Set<String> referencedChampionshipNames = referencedChampionshipNames(ageGroups);

		for (AgeGroup ageGroup : ageGroups) {
			String championshipName = effectiveChampionshipName(ageGroup);
			if (championshipName == null || championshipName.isBlank() || championships.containsKey(championshipName)) {
				continue;
			}
			Championship championship = new Championship(championshipName, ageGroup.getChampionshipType());
			championship.copyCompetitionSettingsFrom(template);
			applyAgeGroupScoringOverrides(championship, championshipName, ageGroups);
			championships.put(championshipName, championship);
		}

		// Export rule: a championship is omitted when reading the AgeGroups sheet alone
		// would recreate it identically. It is included iff ANY of:
		//   1. it is the competition template (always)
		//   2. its settings differ from the template (divergent)
		//   3. an age group references it under a name different from its own code
		//      (e.g. JR -> Junior); reading AgeGroups would otherwise lose the name
		//   4. it has no referencing age group at all (stored but unused; would be
		//      lost on round-trip)
		Championship templateForExport = template;
		List<Championship> exportChampionships = new ArrayList<>(championships.values().stream()
		        .filter(championship -> championship.isCompetitionTemplate()
		                || !hasSameExportedSettingsAs(championship, templateForExport)
		                || hasExplicitAgeGroupChampionshipName(championship, explicitAgeGroupChampionshipNames)
		                || isUnreferenced(championship, referencedChampionshipNames))
		        .toList());
		exportChampionships.sort(Comparator.comparing(Championship::isCompetitionTemplate).reversed()
		        .thenComparing(Championship::compareTo));
		return exportChampionships;
	}

	private boolean hasExplicitAgeGroupChampionshipName(Championship championship, Set<String> explicitAgeGroupChampionshipNames) {
		return explicitAgeGroupChampionshipNames.contains(exportKey(championship));
	}

	private boolean isUnreferenced(Championship championship, Set<String> referencedChampionshipNames) {
		return !referencedChampionshipNames.contains(exportKey(championship));
	}

	private String exportKey(Championship championship) {
		return championship.isCompetitionTemplate()
		        ? Championship.COMPETITION_TEMPLATE_NAME
		        : Championship.canonicalizeChampionshipName(championship.getName());
	}

	/**
	 * Canonical names of championships referenced by at least one age group via
	 * the age group's effective championship name (explicit name, or — if none —
	 * the age group's own code). The competition template is never returned.
	 */
	private Set<String> referencedChampionshipNames(List<AgeGroup> ageGroups) {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : ageGroups) {
			String name = effectiveChampionshipName(ageGroup);
			if (name != null && !name.isBlank()) {
				names.add(name);
			}
		}
		return names;
	}

	private Set<String> explicitAgeGroupChampionshipNames(List<AgeGroup> ageGroups) {
		Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (AgeGroup ageGroup : ageGroups) {
			String championshipName = ageGroup.getChampionshipName();
			if (championshipName == null || championshipName.isBlank()
			        || championshipName.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
				continue;
			}
			String canonicalName = Championship.canonicalizeChampionshipName(championshipName.trim());
			String selfName = Championship.canonicalizeChampionshipName(ageGroup.getCode());
			if (canonicalName != null && !canonicalName.equalsIgnoreCase(selfName)) {
				names.add(canonicalName);
			}
		}
		return names;
	}

	private String effectiveChampionshipName(AgeGroup ageGroup) {
		String championshipName = ageGroup.getChampionshipName();
		if (championshipName == null || championshipName.isBlank()
		        || championshipName.trim().equalsIgnoreCase(Championship.COMPETITION_TEMPLATE_NAME)) {
			championshipName = ageGroup.getCode();
		}
		return Championship.canonicalizeChampionshipName(championshipName != null ? championshipName.trim() : null);
	}

	private void applyAgeGroupScoringOverrides(Championship championship, String championshipName, List<AgeGroup> ageGroups) {
		Ranking bestAthleteScoringSystem = null;
		for (AgeGroup ageGroup : ageGroups) {
			String effectiveName = effectiveChampionshipName(ageGroup);
			if (effectiveName == null || !effectiveName.equalsIgnoreCase(championshipName)) {
				continue;
			}
			if (ageGroup.getScoringSystem() != null) {
				championship.setScoringSystem(ageGroup.getScoringSystem());
			} else if (ageGroup.getChampionshipType() != null && ageGroup.getChampionshipType() != ChampionshipType.U) {
				championship.setScoringSystem(ageGroup.getComputedScoringSystem());
			}
			if (ageGroup.getBestAthleteScoringSystem() != null) {
				bestAthleteScoringSystem = ageGroup.getBestAthleteScoringSystem();
			}
		}
		if (bestAthleteScoringSystem != null) {
			championship.setBestAthleteScoringSystem(bestAthleteScoringSystem);
		}
	}

	private boolean hasSameExportedSettingsAs(Championship championship, Championship template) {
		return championship != null && template != null
		        && championship.isSnatchCJTotalMedals() == template.isSnatchCJTotalMedals()
		        && Objects.equals(championship.getScoringSystem(), template.getScoringSystem())
		        && Objects.equals(championship.getBestAthleteScoringSystem(), template.getBestAthleteScoringSystem())
		        && Objects.equals(championship.getBestSnatchScoringSystem(), template.getBestSnatchScoringSystem())
		        && Objects.equals(championship.getBestCJScoringSystem(), template.getBestCJScoringSystem())
		        && Objects.equals(championship.getTeamPoints1st(), template.getTeamPoints1st())
		        && Objects.equals(championship.getTeamPoints2nd(), template.getTeamPoints2nd())
		        && Objects.equals(championship.getTeamPoints3rd(), template.getTeamPoints3rd())
		        && championship.isGenderedTeamsEnabled() == template.isGenderedTeamsEnabled()
		        && Objects.equals(championship.getTeamScoringSystem(), template.getTeamScoringSystem())
		        && Objects.equals(normalizeTeamSize(championship.getMaxTeamSize()), normalizeTeamSize(template.getMaxTeamSize()))
		        && Objects.equals(championship.getMaxPerCategory(), template.getMaxPerCategory())
		        && Objects.equals(championship.getMensBestN(), template.getMensBestN())
		        && Objects.equals(championship.getWomensBestN(), template.getWomensBestN())
		        && championship.isMixedTeamEnabled() == template.isMixedTeamEnabled()
		        && Objects.equals(championship.getMixedTeamScoringSystem(), template.getMixedTeamScoringSystem())
		        && championship.isExplicitMixedTeamMembers() == template.isExplicitMixedTeamMembers()
		        && Objects.equals(championship.getExplicitTeamSize(), template.getExplicitTeamSize())
		        && Objects.equals(championship.getMixedBestN(), template.getMixedBestN())
		        && Objects.equals(championship.getMixedMensBestN(), template.getMixedMensBestN())
		        && Objects.equals(championship.getMixedWomensBestN(), template.getMixedWomensBestN());
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

	private void setTeamScoringCell(Row row, int column, boolean enabled, Ranking ranking) {
		if (enabled) {
			row.createCell(column).setCellValue(ranking != null ? Ranking.getScoringTitle(ranking) : POINTS_SENTINEL);
		}
	}

	private void setNullableRankingCell(Row row, int column, Ranking ranking) {
		if (ranking != null) {
			row.createCell(column).setCellValue(Ranking.getScoringTitle(ranking));
		}
	}

}
