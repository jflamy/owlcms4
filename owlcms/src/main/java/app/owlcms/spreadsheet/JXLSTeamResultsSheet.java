/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.i18n.Translator;

/**
 * Team results sheet using TeamTreeItem two-level tree (teams → members).
 * Produces a printout matching the results/teamResults page.
 *
 * The JXLS3 template iterates over team items, and for each team iterates
 * over its sorted members using {@link TeamTreeItem#getSortedTeamMembers()}.
 *
 * @author jflamy
 */
public class JXLSTeamResultsSheet extends JXLSWorkbookStreamSource {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(JXLSTeamResultsSheet.class);

	public JXLSTeamResultsSheet(UI ui) {
		super();
		this.setUi(ui);
		this.setEmptyOk(true);
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		// Not used — team tree items are built directly in setReportingInfo()
		return null;
	}

	@Override
	protected void setReportingInfo() {
		super.setReportingInfo();

		Championship championship = getChampionship();
		String ageGroupPrefix = getAgeGroupPrefix();
		Gender gender = getGender();

		// Determine the ranking system from the championship
		Ranking ranking = computeTeamRanking(championship, gender);

		// Build the team tree — same logic as TeamResultsContent.findAll()
		// TeamResultsTreeData respects explicit mixed team membership from the championship
		// and the gender filter value to build the correct team structure.
		TeamResultsTreeData treeData = new TeamResultsTreeData(
				ageGroupPrefix, championship, gender, ranking, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = treeData.getTeamItemsByGender();

		// Collect teams in display order: M, F, then MF
		// Only include non-empty gender groups to avoid empty sections in the template
		List<TeamTreeItem> mTeams = sortTeamsByPoints(teamsByGender.getOrDefault(Gender.M, List.of()));
		List<TeamTreeItem> wTeams = sortTeamsByPoints(teamsByGender.getOrDefault(Gender.F, List.of()));
		List<TeamTreeItem> mwTeams = sortTeamsByPoints(teamsByGender.getOrDefault(Gender.MF, List.of()));

		// Only put non-empty lists into beans so the template skips empty gender sections
		if (!mTeams.isEmpty()) {
			getReportingBeans().put("mTeamItems", mTeams);
		}
		if (!wTeams.isEmpty()) {
			getReportingBeans().put("wTeamItems", wTeams);
		}
		if (!mwTeams.isEmpty()) {
			getReportingBeans().put("mwTeamItems", mwTeams);
		}

		// All teams combined (includes whichever genders are present)
		List<TeamTreeItem> allTeams = new ArrayList<>();
		allTeams.addAll(mTeams);
		allTeams.addAll(wTeams);
		allTeams.addAll(mwTeams);
		if (!allTeams.isEmpty()) {
			getReportingBeans().put("teamItems", allTeams);
		}

		// Scoring system title for the template header
		String scoringTitle = ranking != null
				? Ranking.getScoringTitle(ranking)
				: Translator.translate("Score");
		getReportingBeans().put("scoringTitle", scoringTitle);

		logger.debug("team results beans: mTeamItems={} wTeamItems={} mwTeamItems={}",
				mTeams.size(), wTeams.size(), mwTeams.size());
	}

	@Override
	protected void postProcess(Workbook workbook) {
		String c = getChampionship() != null ? getChampionship().getName() : null;
		String ag = getAgeGroupPrefix();
		Gender g = getGender();

		var header = workbook.getSheetAt(0).getHeader();
		StringBuilder center = new StringBuilder();
		if (c != null) {
			center.append(c);
		}
		if (ag != null) {
			if (center.length() > 0) {
				center.append("\u2013");
			}
			center.append(ag);
		}
		if (g != null && g != Gender.MF) {
			if (center.length() > 0) {
				center.append(" \u2013 ");
			}
			center.append(Translator.translate("Gender." + g.name()));
		}
		header.setCenter(center.toString());
		createStandardFooter(workbook);
	}

	private Ranking computeTeamRanking(Championship championship, Gender gender) {
		if (gender == Gender.MF && championship != null) {
			return championship.getMixedTeamScoringSystem() != null
					? championship.getMixedTeamScoringSystem()
					: Ranking.TOTAL;
		}
		if (championship != null) {
			return championship.getTeamScoringSystem() != null
					? championship.getTeamScoringSystem()
					: Ranking.TOTAL;
		}
		return Ranking.TOTAL;
	}

	private List<TeamTreeItem> sortTeamsByPoints(List<TeamTreeItem> teams) {
		if (teams == null || teams.isEmpty()) {
			return List.of();
		}
		List<TeamTreeItem> sorted = new ArrayList<>(teams);
		sorted.sort(TeamTreeItem.pointComparator);
		return sorted;
	}
}
