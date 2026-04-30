/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.Comparator;
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
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.i18n.Translator;

/**
 * Team results sheet using TeamTreeItem two-level tree (teams → members).
 * Produces a printout matching the results/teamResults page.
 *
 * Template has 3 sheets: Men, Women, Mixed.
 * postProcess() removes sheets for gender groups that have no teams.
 *
 * @author jflamy
 */
public class JXLSTeamResultsSheet extends JXLSWorkbookStreamSource {

	private static final long serialVersionUID = 1L;
	private Logger logger = LoggerFactory.getLogger(JXLSTeamResultsSheet.class);

	/** Track which gender groups are present so postProcess can remove empty sheets. */
	private boolean hasMen;
	private boolean hasWomen;
	private boolean hasMixed;

	public JXLSTeamResultsSheet(UI ui) {
		super();
		this.setUi(ui);
		this.setEmptyOk(true);
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		return null;
	}

	@Override
	protected void setReportingInfo() {
		super.setReportingInfo();

		Championship championship = getChampionship();
		String ageGroupPrefix = getAgeGroupPrefix();
		Gender gender = getGender();

		// Each gender group uses its own ranking from the championship.
		// Gendered (M/W) tabs use championship.teamScoringSystem.
		// Mixed (MF) tab uses championship.mixedTeamScoringSystem.
		// These must remain segregated — no cross-tab leakage from competition-wide
		// best-athlete scoring or from one championship setting to the other.
		Ranking genderedRanking = computeTeamRanking(championship, Gender.M);
		Ranking mixedRanking = computeTeamRanking(championship, Gender.MF);

		// Ensure the per-tab scoring systems are computed for athletes — otherwise
		// Ranking.getRankingValue() returns 0 (gated by RankingConfig.shouldCompute)
		// and member.score cells render blank even when the team total is populated
		// via the team-level accumulator.
		RankingConfig.updateMustCompute();

		// Build the tree — internally calls getRankingForGender() per gender group,
		// so M/F items already carry teamScoringSystem and MF items already carry
		// mixedTeamScoringSystem. Each gender has its own list of TeamTreeItem
		// instances, so per-gender overrides cannot leak across tabs.
		TeamResultsTreeData treeData = new TeamResultsTreeData(
				ageGroupPrefix, championship, gender, genderedRanking, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = treeData.getTeamItemsByGender();

		// Sort each gender group with its own comparator.
		boolean genderedPointsBased = championship != null ? championship.computePointsBased() : true;
		boolean mixedPointsBased = championship != null ? championship.computeMixedPointsBased() : true;

		Comparator<TeamTreeItem> genderedComparator = genderedPointsBased
				? TeamTreeItem.pointComparator : TeamTreeItem.scoreComparator;
		Comparator<TeamTreeItem> mixedComparator = mixedPointsBased
				? TeamTreeItem.pointComparator : TeamTreeItem.scoreComparator;

		List<TeamTreeItem> mTeams = sortTeams(teamsByGender.getOrDefault(Gender.M, List.of()), genderedComparator);
		List<TeamTreeItem> wTeams = sortTeams(teamsByGender.getOrDefault(Gender.F, List.of()), genderedComparator);
		List<TeamTreeItem> mwTeams = sortTeams(teamsByGender.getOrDefault(Gender.MF, List.of()), mixedComparator);

		// Re-assert per-gender scoring on the items we just sorted, so each tab is
		// guaranteed to use its own championship scoring system. When points-based,
		// the score column is hidden by applyMeasureColumnVisibility(); the value
		// still flows through team/member.score for any template that references it.
		overrideScoringSystem(mTeams, genderedRanking);
		overrideScoringSystem(wTeams, genderedRanking);
		overrideScoringSystem(mwTeams, mixedRanking);

		this.hasMen = !mTeams.isEmpty();
		this.hasWomen = !wTeams.isEmpty();
		this.hasMixed = !mwTeams.isEmpty();

		// Publish counted-only legacy beans too so Team Results templates that still
		// reference mTeam/wTeam/mwTeam receive the same filtered subset as TeamTreeItem.
		getReportingBeans().put("mTeam", flattenCountedMembers(mTeams));
		getReportingBeans().put("wTeam", flattenCountedMembers(wTeams));
		getReportingBeans().put("mwTeam", flattenCountedMembers(mwTeams));

		// Per-sheet team lists.
		if (this.hasMen) {
			getReportingBeans().put("mTeamItems", mTeams);
		}
		if (this.hasWomen) {
			getReportingBeans().put("wTeamItems", wTeams);
		}
		if (this.hasMixed) {
			getReportingBeans().put("mwTeamItems", mwTeams);
		}
		// Per-sheet display control: showPoints hides the team-level points sum
		// on score-based tabs; scoringTitle labels the score column. Each tab uses
		// its own scoring system so titles stay segregated.
		String genderedTitle = Ranking.getScoringTitle(genderedRanking);
		String mixedTitle = Ranking.getScoringTitle(mixedRanking);

		getReportingBeans().put("mShowPoints", genderedPointsBased);
		getReportingBeans().put("wShowPoints", genderedPointsBased);
		getReportingBeans().put("mwShowPoints", mixedPointsBased);
		getReportingBeans().put("mScoringTitle", genderedTitle);
		getReportingBeans().put("wScoringTitle", genderedTitle);
		getReportingBeans().put("mwScoringTitle", mixedTitle);
		getReportingBeans().put("mTeamSize", computeConfiguredTeamSize(championship, ageGroupPrefix, Gender.M));
		getReportingBeans().put("wTeamSize", computeConfiguredTeamSize(championship, ageGroupPrefix, Gender.F));
		getReportingBeans().put("mwTeamSize", computeConfiguredTeamSize(championship, ageGroupPrefix, Gender.MF));

		logger.debug("team results: gendered={} (pointsBased={}), mixed={} (pointsBased={}), m={} w={} mw={}",
				genderedRanking, genderedPointsBased,
				mixedRanking, mixedPointsBased,
				mTeams.size(), wTeams.size(), mwTeams.size());
	}

	private int computeConfiguredTeamSize(Championship championship, String ageGroupPrefix, Gender gender) {
		if (championship == null) {
			return 0;
		}
		return championship.getConfiguredTeamSize(ageGroupPrefix, gender);
	}

	private List<Athlete> flattenCountedMembers(List<TeamTreeItem> teams) {
		List<Athlete> athletes = new ArrayList<>();
		for (TeamTreeItem team : teams) {
			for (TeamTreeItem member : team.getCountedTeamMembers()) {
				if (member.getAthlete() != null) {
					athletes.add(member.getAthlete());
				}
			}
		}
		return athletes;
	}

	@Override
	protected void postProcess(Workbook workbook) {
		// Remove empty sheets (iterate backwards to keep indices stable)
		// Template sheet order: 0=Men, 1=Women, 2=Mixed
		if (!this.hasMixed && workbook.getNumberOfSheets() > 2) {
			workbook.removeSheetAt(2);
		}
		if (!this.hasWomen && workbook.getNumberOfSheets() > 1) {
			workbook.removeSheetAt(1);
		}
		if (!this.hasMen && workbook.getNumberOfSheets() > 0) {
			workbook.removeSheetAt(0);
		}

		// Apply header/footer to all remaining sheets
		String c = getChampionship() != null ? getChampionship().getName() : null;
		String ag = getAgeGroupPrefix();
		Gender g = getGender();

		StringBuilder center = new StringBuilder();
		if (c != null) {
			center.append(c);
		}
		if (ag != null) {
			if (center.length() > 0) {
				center.append(" \u2013 ");
			}
			center.append(ag);
		}
		if (g != null && g != Gender.MF) {
			if (center.length() > 0) {
				center.append(" \u2013 ");
			}
			center.append(Translator.translate("Gender." + g.name()));
		}

		for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
			var sheet = workbook.getSheetAt(i);
			applyMeasureColumnVisibility(sheet);
			sheet.getHeader().setCenter(center.toString());
		}
		createStandardFooter(workbook);
	}

	private void applyMeasureColumnVisibility(org.apache.poi.ss.usermodel.Sheet sheet) {
		Boolean showPoints = switch (sheet.getSheetName()) {
			case "Men" -> (Boolean) getReportingBeans().get("mShowPoints");
			case "Women" -> (Boolean) getReportingBeans().get("wShowPoints");
			case "Mixed" -> (Boolean) getReportingBeans().get("mwShowPoints");
			default -> null;
		};
		if (showPoints == null) {
			return;
		}

		// E = points, G = score. Hide the unused measure column on each sheet.
		sheet.setColumnHidden(4, !showPoints.booleanValue());
		sheet.setColumnHidden(6, showPoints.booleanValue());
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

	private List<TeamTreeItem> sortTeams(List<TeamTreeItem> teams, Comparator<TeamTreeItem> comparator) {
		if (teams == null || teams.isEmpty()) {
			return List.of();
		}
		List<TeamTreeItem> sorted = new ArrayList<>(teams);
		sorted.sort(comparator);
		return sorted;
	}

	private void overrideScoringSystem(List<TeamTreeItem> teams, Ranking scoring) {
		for (TeamTreeItem team : teams) {
			team.setScoringSystem(scoring);
			for (TeamTreeItem member : team.getCountedTeamMembers()) {
				member.setScoringSystem(scoring);
			}
		}
	}
}
