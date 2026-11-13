/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.NativeLabel;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.RankingConfig;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

/**
 * A TeamTreeItem is either a team, or a person inside a team. A Vaadin tree contains only one type of node.
 *
 * @author JF
 *
 */
public class TeamTreeItem {

	public static Comparator<TeamTreeItem> pointComparator = ((a, b) -> {
		int compare = 0;
		compare = ObjectUtils.compare(a.getGender(), b.getGender(), true);
		if (compare != 0) {
			return compare;
		}
		// bigger is better
		compare = -ObjectUtils.compare(a.getPoints(), b.getPoints(), true);
		return compare;
	});
	public static Comparator<TeamTreeItem> scoreComparator = ((a, b) -> {
		int compare = 0;
		compare = ObjectUtils.compare(a.getGender(), b.getGender(), true);
		if (compare != 0) {
			return compare;
		}
		// bigger is better
		compare = -ObjectUtils.compare(a.getScore(), b.getScore(), true);
		return compare;
	});
	@SuppressWarnings("unused")
	private final static Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeItem.class);
	private Athlete athlete;
	private boolean done;
	private TeamTreeItem parent;
	private Team team;
	private List<TeamTreeItem> teamMembers;
	private boolean combinedPoints;
	private NativeLabel membershipLabel;
	private NativeLabel mixedMembershipLabel;
	private boolean warning;
	private Ranking scoringSystem;
	private boolean countedForTeam;
	private boolean scoresWithoutTotal;

	public TeamTreeItem(String curTeamName, Gender gender, Athlete teamMember, boolean done) {
		this(curTeamName, gender, teamMember, done, Championship.of(null));
	}

	public TeamTreeItem(String curTeamName, Gender gender, Athlete teamMember, boolean done, Championship championship) {
		Championship effectiveChampionship = championship != null ? championship : Championship.of(null);
		this.scoringSystem = effectiveChampionship.getScoringSystem();
		this.athlete = teamMember;
		this.setDone(done);
		if (this.athlete == null) {
			// this node is a team
			this.setTeam(new Team(curTeamName, gender, this.scoringSystem));
			this.setTeamMembers(new ArrayList<>());
		}
		this.combinedPoints = effectiveChampionship.isSnatchCJTotalMedals();
	}

	public TeamTreeItem addTreeItemChild(Athlete a, boolean done) {
		List<TeamTreeItem> members = getTeamMembers();
		TeamTreeItem existing = members.stream()
		        .filter(m -> m.getAthlete().getId().equals(a.getId()))
		        .findFirst()
		        .orElse(null);
		if (existing != null) {
			return existing;
		}
		Championship championship = a.getAgeGroup() != null ? a.getAgeGroup().getChampionship() : Championship.of(null);
		TeamTreeItem child = new TeamTreeItem(null, a.getGender(), a, done, championship);
		child.setParent(this);
		getTeamMembers().add(child);
		return child;
	}

	public void addTreeItemChild(TeamSelectionTreeData teamSelectionTreeData, Athlete a, boolean done) {
		Championship championship = a.getAgeGroup() != null ? a.getAgeGroup().getChampionship() : Championship.of(null);
		TeamTreeItem child = new TeamTreeItem(null, a.getGender(), a, done, championship);
		child.setParent(this);
		getTeamMembers().add(child);
		teamSelectionTreeData.addItem(this, child);
	}

	public String formatName() {
		if (this.athlete == null) {
			return Translator.translate("TeamResults.TeamNameFormat", getTeam().getName(), getTeam().getGender());
		} else {
			return this.athlete.getFullName();
		}
	}

	public String formatProgress() {
		if (this.athlete != null) {
			return isLiftingDone() ? Translator.translate("Done") : "";
		} else {
			return getTeam().getCounted() + "/" + getMemberCount();
		}
	}

	public String formatScore() {
		Integer pts = getPoints();
		return (pts == null ? "" : Integer.toString(pts));
	}

	public Athlete getAthlete() {
		return this.athlete;
	}

	public String getCategory() {
		return this.team == null ? this.athlete.getCategory().getNameWithAgeGroup() : "";
	}

	public Integer getCleanJerkPoints() {
		return this.athlete.getCleanJerkPoints();
	}

	public Integer getCombinedPoints() {
		return this.athlete.getCombinedPoints();
	}

	public Integer getCounted() {
		return this.team != null ? this.team.getCounted() : null;
	}

	/**
	 * For member nodes: whether this athlete was included in the team's topN scoring.
	 */
	public boolean isCountedForTeam() {
		return this.countedForTeam;
	}

	public void setCountedForTeam(boolean countedForTeam) {
		this.countedForTeam = countedForTeam;
	}

	public Integer getCustomPoints() {
		return this.athlete.getCustomPoints();
	}

	public Gender getGender() {
		return this.team != null ? this.team.getGender() : this.athlete.getGender();
	}

	public NativeLabel getMembershipLabel() {
		return this.membershipLabel;
	}

	public NativeLabel getMixedMembershipLabel() {
		return this.mixedMembershipLabel;
	}

	public String getName() {
		if (this.athlete == null) {
			return getTeam().getName();
		} else {
			return this.athlete.getFullName();
		}
	}

	public TeamTreeItem getParent() {
		return this.parent;
	}

	public Integer getPoints() {
		Integer pts;
		if (this.athlete == null) {
			pts = getTeam().getPoints();
		} else {
			pts = isDone() ? getConfiguredPoints() : null;
		}
		return pts;
	}

	public Integer getRank() {
		return this.team != null ? this.team.getRank() : null;
	}

	public Double getScore() {
		if (this.team != null) {
			return this.team.getScore();
		}
		if (this.scoringSystem == null) {
			return 0D;
		}
		switch (this.scoringSystem) {
			case BW_SINCLAIR:
				return getSinclairScore();
			case CAT_SINCLAIR:
				return getCatSinclairMetric();
			case CAT_QPOINTS:
				return getCatQPointsMetric();
			case CAT_GAMX:
				return getCatGamxScore();
			case QPOINTS:
				return getQPointsScore();
			case QAGE:
				return getQMastersScore();
			case SMM:
				return getSmfScore();
			case GAMX:
				return getGamxScore();
			default:
				return Ranking.getRankingValue(this.athlete, this.scoringSystem);
		}
	}

	/**
	 * @param scoresWithoutTotal true if an athlete without a total keeps the score computed from their best snatch
	 *                           and best clean and jerk; false if that score is 0
	 */
	public void setScoresWithoutTotal(boolean scoresWithoutTotal) {
		this.scoresWithoutTotal = scoresWithoutTotal;
	}

	private Double memberScore(Supplier<Double> bestLiftsScore) {
		if (!this.scoresWithoutTotal && hasNoTotal()) {
			return 0.0D;
		}
		Double value = bestLiftsScore.get();
		return value != null ? value : 0.0D;
	}

	private boolean hasNoTotal() {
		Integer total = this.athlete.getTotal();
		boolean finished = isDone() || this.athlete.isDone();
		return finished && (total == null || total <= 0);
	}

	public void setScoringSystem(Ranking scoringSystem) {
		this.scoringSystem = scoringSystem;
		if (this.team != null) {
			this.team.setScoringSystem(scoringSystem);
		}
	}

	public Double getSinclairScore() {
		return (this.team != null ? this.team.getSinclairScore() : memberScore(this.athlete::getSinclairForDelta));
	}

	public Double getQPointsScore() {
		return (this.team != null ? this.team.getQPoints() : memberScore(this.athlete::getQPointsForDelta));
	}

	public Double getQMastersScore() {
		return (this.team != null ? this.team.getQMasters() : memberScore(this.athlete::getQMastersForDelta));
	}

	public Double getCatGamxScore() {
		return (this.team != null ? this.team.getCatGamxScore() : memberScore(this.athlete::getCategoryGAMXForDelta));
	}

	public Double getCatQPointsMetric() {
		return (this.team != null ? this.team.getCatQPointsScore()
		        : memberScore(this.athlete::getCategoryQPointsForDelta));
	}

	public Double getCatSinclairMetric() {
		return (this.team != null ? this.team.getCatSinclairScore()
		        : memberScore(this.athlete::getCategorySinclairForDelta));
	}

	/** Team GAMX contribution, in the GAMX variant that the member is scored with (regular GAMX by default). */
	public Double getGamxScore() {
		if (this.team != null) {
			return this.team.getGamx();
		}
		Ranking gamxRanking = this.scoringSystem != null && this.scoringSystem != Ranking.CAT_GAMX
		        && RankingConfig.getGamxRankings().contains(this.scoringSystem) ? this.scoringSystem : Ranking.GAMX;
		return memberScore(() -> Ranking.getRankingValueForDelta(this.athlete, gamxRanking));
	}

	public int getRawCombinedPoints() {
		return (this.athlete != null ? this.athlete.getRawCombinedPoints() : 0);
	}

	public int getRawTotalPoints() {
		return (this.athlete != null ? this.athlete.getRawTotalPoints() : 0);
	}

	public Integer getConfiguredPoints() {
		return this.combinedPoints ? getCombinedPoints() : getTotalPoints();
	}

	public int getConfiguredRawPoints() {
		return this.combinedPoints ? getRawCombinedPoints() : getRawTotalPoints();
	}

	public int getTotalOnlyPoints() {
		return (this.team != null ? this.team.getTotalOnlyPoints() : getRawTotalPoints());
	}

	public Double getRobiScore() {
		return (this.team != null ? this.team.getRobi() : this.athlete.getRobi());
	}



	/**
	 * @return the number of team members who lifted (denominator of the counted/lifted status)
	 */
	public int getMemberCount() {
		return getLiftedTeamMembers().size();
	}

	public long getSize() {
		return this.team != null ? this.team.getSize() : 0;
	}

	public Double getSmfScore() {
		return (this.team != null ? this.team.getSmfScore() : memberScore(this.athlete::getSmhfForDelta));
	}

	public Integer getSnatchPoints() {
		return this.athlete.getSnatchPoints();
	}

	public List<TeamTreeItem> getSortedTeamMembers() {
		if (getTeamMembers() == null) {
			return Collections.emptyList();
		}
		if (this.scoringSystem == Ranking.TOTAL || this.scoringSystem == Ranking.SNATCH_CJ_TOTAL) {
			getTeamMembers().sort(Comparator
			        .comparing(TeamTreeItem::getPoints, Comparator.nullsLast(Integer::compareTo))
			        .reversed());
		} else {
			getTeamMembers().sort(Comparator
			        .comparing(TeamTreeItem::getScore, Comparator.nullsLast(Double::compareTo))
			        .reversed());
		}
		return getTeamMembers();
	}

	/**
	 * @return only team members that were included in the topN scoring, sorted by points.
	 *         When no topN limit is configured, all members are counted and this returns the same as getSortedTeamMembers().
	 */
	public List<TeamTreeItem> getCountedTeamMembers() {
		return getSortedTeamMembers().stream()
		        .filter(TeamTreeItem::isCountedForTeam)
		        .collect(Collectors.toList());
	}

	/**
	 * Team members who lifted (took at least one attempt), whether or not they made a total or have a zero score or
	 * points. Counted members come first, then by decreasing points or score, missing values last.
	 */
	public List<TeamTreeItem> getLiftedTeamMembers() {
		Comparator<TeamTreeItem> byValue = (this.scoringSystem == Ranking.TOTAL
		        || this.scoringSystem == Ranking.SNATCH_CJ_TOTAL)
		                ? Comparator.comparing(TeamTreeItem::getPoints,
		                        Comparator.nullsLast(Comparator.<Integer>reverseOrder()))
		                : Comparator.comparing(TeamTreeItem::getScore,
		                        Comparator.nullsLast(Comparator.<Double>reverseOrder()));
		return getTeamMembers().stream()
		        .filter(TeamTreeItem::isLifted)
		        .sorted(Comparator.comparing(TeamTreeItem::isCountedForTeam).reversed().thenComparing(byValue))
		        .collect(Collectors.toList());
	}

	public Team getTeam() {
		return this.team;
	}

	public List<TeamTreeItem> getTeamMembers() {
		if (this.teamMembers == null) {
			return Collections.emptyList();
		}
		return this.teamMembers;
	}

	public String getTeamName() {
		return this.athlete.getTeam();
	}

	public boolean isDone() {
		return this.done;
	}

	/**
	 * @return true if the athlete has taken at least one attempt (misses and declared zeros included)
	 */
	public boolean isLifted() {
		return this.athlete != null && this.athlete.getAttemptsDone() > 0;
	}

	/**
	 * @return true if the athlete has taken all six attempts (misses and declared zeros included)
	 */
	public boolean isLiftingDone() {
		return this.athlete != null && this.athlete.getAttemptsDone() > 5;
	}

	public Integer getTotalPoints() {
		return (this.athlete != null ? this.athlete.getTotalPoints() : null);
	}

	public Boolean isTeamMember() {
		return (this.athlete != null ? this.athlete.isTeamMember() : null);
	}

	public Boolean isMixedTeamMember() {
		return (this.athlete != null ? this.athlete.isMixedTeamMember() : null);
	}

	public boolean isWarning() {
		return this.warning;
	}

	public void setMembershipLabel(NativeLabel label) {
		this.membershipLabel = label;
	}

	public void setMixedMembershipLabel(NativeLabel label) {
		this.mixedMembershipLabel = label;
	}

	public void setParent(TeamTreeItem parent) {
		this.parent = parent;
	}

	public void setRank(Integer rank) {
		if (this.team != null) {
			this.team.setRank(rank);
		}
	}

	public void setTeamMember(boolean b) {
		if (this.athlete != null) {
			this.athlete.setTeamMember(b);
		}
	}

	public void setMixedTeamMember(boolean b) {
		if (this.athlete != null) {
			this.athlete.setMixedTeamMember(b);
		}
	}

	public void setTeamMembers(List<TeamTreeItem> teamMembers) {
		this.teamMembers = teamMembers;
	}

	public void setWarning(boolean contains) {
		this.warning = contains;
	}

	private void setDone(boolean done) {
		this.done = done;
	}

	private void setTeam(Team team) {
		this.team = team;
	}

}
