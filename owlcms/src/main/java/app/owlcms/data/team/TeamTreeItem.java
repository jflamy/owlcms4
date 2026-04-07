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
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.NativeLabel;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
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
			return isDone() ? Translator.translate("Done") : "";
		} else {
			return getTeam().getCounted() + "/" + getTeam().getSize();
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
			pts = isDone() ? (this.combinedPoints ? getCombinedPoints() : getTotalPoints()) : null;
		}
		return pts;
	}

	public Double getScore() {
		return (this.team != null ? this.team.getScore() : Ranking.getRankingValue(this.athlete, this.scoringSystem));
	}

	public void setScoringSystem(Ranking scoringSystem) {
		this.scoringSystem = scoringSystem;
		if (this.team != null) {
			this.team.setScoringSystem(scoringSystem);
		}
	}

	public Double getSinclairScore() {
		return (this.team != null ? this.team.getSinclairScore() : this.athlete.getSinclairForDelta());
	}
	
	public Double getQPointsScore() {
		return (this.team != null ? this.team.getQPoints() : this.athlete.getQPointsForDelta());
	}
	
	public Double getQMastersScore() {
		return (this.team != null ? this.team.getQMasters() : this.athlete.getQMastersForDelta());
	}

	public Double getCatGamxScore() {
		return (this.team != null ? this.team.getCatGamxScore() : this.athlete.getCategoryGAMXForDelta());
	}

	public Double getCatQPointsMetric() {
		return (this.team != null ? this.team.getCatQPointsScore() : this.athlete.getCategoryQPointsForDelta());
	}

	public Double getCatSinclairMetric() {
		return (this.team != null ? this.team.getCatSinclairScore() : this.athlete.getCategorySinclairForDelta());
	}

	public Double getGamxScore() {
		return (this.team != null ? this.team.getGamx() : this.athlete.getGamx());
	}

	public int getRawCombinedPoints() {
		return (this.athlete != null ? this.athlete.getRawCombinedPoints() : 0);
	}

	public int getRawTotalPoints() {
		return (this.athlete != null ? this.athlete.getRawTotalPoints() : 0);
	}

	public Double getRobiScore() {
		return (this.team != null ? this.team.getRobi() : this.athlete.getRobi());
	}



	public long getSize() {
		return this.team != null ? this.team.getSize() : 0;
	}

	public Double getSmfScore() {
		return (this.team != null ? this.team.getSmfScore() : this.athlete.getSmhfForDelta());
	}

	public Integer getSnatchPoints() {
		return this.athlete.getSnatchPoints();
	}

	public List<TeamTreeItem> getSortedTeamMembers() {
		if (getTeamMembers() == null) {
			return Collections.emptyList();
		}
		getTeamMembers().sort(Comparator.comparing(TeamTreeItem::getPoints, (a, b) -> ObjectUtils.compare(a, b, true)));
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
