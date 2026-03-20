/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamResultsTreeData extends TreeData<TeamTreeItem> {

	Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);
	private boolean debug = false;
	private List<Group> doneGroups = null;
	private Gender genderFilterValue;
	private final Logger logger = (Logger) LoggerFactory.getLogger(TeamResultsTreeData.class);
	private HashMap<String, Object> reportingBeans;
	private Ranking ranking;
	private Championship championship;

	public TeamResultsTreeData(String ageGroupPrefix, Championship ageDivision, Gender gender, Ranking ranking,
	        boolean includeNotDone) {
		this.genderFilterValue = gender;
		this.setRanking(ranking);
		init(ageGroupPrefix, ageDivision, includeNotDone);
	}

	public Ranking getRanking() {
		return this.ranking;
	}

	public Map<Gender, List<TeamTreeItem>> getTeamItemsByGender() {
		return this.teamsByGender;
	}

	public void setRanking(Ranking ranking) {
		this.ranking = ranking;
	}

	private void buildTeamItemTree(
	        HashMap<String, Object> reportingBeans2,
	        String ageGroupPrefix,
	        Championship ageDivision,
	        boolean includeNotDone) {
		this.doneGroups = null; // force recompute.
		if (ageDivision == null) {
			return;
		}
		for (Gender gender : Gender.mfmfValues()) {
			if (this.genderFilterValue != null && gender != this.genderFilterValue) {
				continue;
			}

			List<TeamTreeItem> curGenderTeams = getTeamItemsByGender().get(gender);
			if (curGenderTeams == null) {
				curGenderTeams = new ArrayList<>();
				getTeamItemsByGender().put(gender, curGenderTeams);
				//this.logger.debug("created list for gender {}: {}", gender, getTeamItemsByGender().get(gender));
			}
			TeamTreeItem curTeamItem = null;
			curTeamItem = doTeamGender(ageGroupPrefix, ageDivision, includeNotDone, gender, curGenderTeams, curTeamItem);
		}

		dumpTeams();
	}

	public TeamTreeItem doTeamGender(String ageGroupPrefix, Championship ageDivision, boolean includeNotDone, Gender gender, List<TeamTreeItem> curGenderTeams,
	        TeamTreeItem curTeamItem) {
		String key = computeGenderKey(gender) + "Team"
		        + (ageGroupPrefix != null ? ageGroupPrefix : ageDivision.getName());
		Ranking rankingForGender = getRankingForGender(gender);
		this.logger.debug("looking for {} in {}", key, this.reportingBeans.keySet());

		@SuppressWarnings("unchecked")
		List<Athlete> athletes = (List<Athlete>) this.reportingBeans.get(key);
		if (athletes == null) {
			return null;
		}
		athletes = athletes.stream()
		        // .peek(a -> {
		        // logger.debug("{} {} {} {}",a.getShortName(), ((PAthlete)
		        // a)._getOriginalParticipation().getTeamMember(), a.getClass().getSimpleName(), ((PAthlete)
		        // a).getCategory());
		        // })
		        // .filter(a -> a.isTeamMember())
		        .collect(Collectors.toList());
		if (gender == Gender.MF && ageDivision != null && ageDivision.isMixed()) {
			AthleteSorter.teamPointsOrderMixed(athletes, rankingForGender);
		} else {
			AthleteSorter.teamPointsOrder(athletes, rankingForGender);
		}

		String prevTeamName = null;
		if (athletes != null) {
			boolean combinedTotal = ageDivision != null ? ageDivision.isSnatchCJTotalMedals()
			        : Competition.getCurrent().isSnatchCJTotalMedals();
			// count points for each team
			for (Athlete a : athletes) {
				// check if competition is a "best n results" team comp.
				// if the competition is "top n", we can have "top 4 men" + "top 2 women", so we
				// want the athlete's
				// gender.
				Integer maxCount = gender == Gender.MF && ageDivision != null && ageDivision.isMixed()
				        ? getTopNTeamSize(Gender.MF)
				        : getTopNTeamSize(a.getGender());
				String curTeamName = a.getTeam();
				// logger.debug("a={} curTeam = {}",a, a.getTeam());
				curTeamItem = findCurTeamItem(getTeamItemsByGender(), gender, curGenderTeams, prevTeamName,
				        curTeamItem,
				        curTeamName != null ? curTeamName : "-", rankingForGender);
				boolean groupIsDone = groupIsDone(a);
				Integer curPoints = gender == Gender.MF && ageDivision != null && ageDivision.isMixed()
				        ? (combinedTotal ? a.getRawCombinedPoints() : a.getRawTotalPoints())
				        : (combinedTotal ? a.getCombinedPoints() : a.getTotalPoints());
				double curSinclair = a.getSinclairForDelta();
				double curCatSinclair = a.getCategorySinclairForDelta();
				double curSmf = a.getSmhfForDelta();
				double curRobi = a.getRobi();
				double curGamx = a.getGamx();
				double curCatGamx = a.getCategoryGAMXForDelta();
				double curQPoints = a.getQPoints();
				double curCatQPoints = a.getCategoryQPointsForDelta();
				double curQMasters = a.getQMasters();

				Team curTeam = curTeamItem.getTeam();

				boolean b = curTeam.getCounted() < maxCount;
				boolean c = curPoints != null && curPoints > 0;

				// if (debug) {
				// logger.debug("---- Athlete {} {} {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints,
				// curTeam.getCounted(), groupIsDone, b, c);
				// }
				boolean contributes = gender == Gender.MF && ageDivision != null && ageDivision.isMixed()
				        ? a.isMixedTeamMember()
				        : a.isTeamMember();
				if (contributes) {
					if ((includeNotDone || groupIsDone) && b && c) {
						curTeam.setPoints(curTeam.getPoints() + Math.round(curPoints));
					}
					if (b) {
						curTeam.setSinclairScore(curTeam.getSinclairScore() + curSinclair);
						curTeam.setCatSinclairScore(curTeam.getCatSinclairScore() + curCatSinclair);
						curTeam.setSmfScore(curTeam.getSmfScore() + curSmf);
						curTeam.setCounted(curTeam.getCounted() + 1);
						curTeam.setRobi(curTeam.getRobi() + curRobi);
						curTeam.setGamx(curTeam.getGamx() + curGamx);
						curTeam.setCatGamxScore(curTeam.getCatGamxScore() + curCatGamx);
						curTeam.setQPoints(curTeam.getQPoints() + curQPoints);
						curTeam.setCatQPointsScore(curTeam.getCatQPointsScore() + curCatQPoints);
						curTeam.setQMasters(curTeam.getQMasters() + curQMasters);
					}
				}
				curTeamItem.addTreeItemChild(a, groupIsDone);
				List<TeamTreeItem> members = curTeamItem.getTeamMembers();
				members.get(members.size() - 1).setScoringSystem(rankingForGender);
				curTeam.setSize(curTeam.getSize() + 1);
				prevTeamName = curTeamName;
			}
		}
		return curTeamItem;
	}

	private String computeGenderKey(Gender gender) {
		String genderKey;
		switch (gender) {
			case F:
				genderKey = "w";
				break;
			case M:
				genderKey = "m";
				break;
			case MF:
				genderKey = "mw";
				break;
			default:
				throw new IllegalArgumentException();
		}
		return genderKey;
	}

	private void dumpTeams() {
		for (Gender g : Gender.values()) {
			List<TeamTreeItem> teamItems = getTeamItemsByGender().get(g);
			if (teamItems == null) {
				continue;
			}
			for (TeamTreeItem item : teamItems) {
				this.logger.debug("team: {} {} {} {} {}", item.getName(), item.getGender(), item.getPoints(),
				        item.getSinclairScore(), item.getCounted());
				List<TeamTreeItem> teamMembers = item.getTeamMembers();
				for (TeamTreeItem t : teamMembers) {
					this.logger.debug("    {} {} {}", t.getName(), t.getPoints(), t.getSinclairScore());
				}
			}
		}
	}

	private TeamTreeItem findCurTeamItem(Map<Gender, List<TeamTreeItem>> teamItemsByGender, Gender gender,
	        List<TeamTreeItem> curGenderTeams, String prevTeamName, TeamTreeItem curTeamItem, String curTeamName,
	        Ranking rankingForGender) {
		if (curTeamItem == null || prevTeamName == null || !curTeamName.contentEquals(prevTeamName)) {
			// maybe we have seen the team already (if mixed)
			TeamTreeItem found = null;
			for (TeamTreeItem ct : curGenderTeams) {
				if (ct.getName() != null && ct.getName().contentEquals(curTeamName)) {
					found = ct;
					break;
				}
			}
			if (found != null) {
				curTeamItem = found;
			} else {
				curTeamItem = new TeamTreeItem(curTeamName, gender, null, false);
				curTeamItem.setScoringSystem(rankingForGender);
				curTeamItem.getTeam().setSize(AthleteRepository.countTeamMembers(curTeamName, gender));
				teamItemsByGender.get(gender).add(curTeamItem);
			}
		}
		return curTeamItem;
	}

	private Ranking getRankingForGender(Gender gender) {
		if (this.championship != null) {
			if (gender == Gender.MF) {
				return this.championship.getMixedTeamScoringSystem() != null
				        ? this.championship.getMixedTeamScoringSystem()
				        : Ranking.TOTAL;
			}
			return this.championship.getTeamScoringSystem() != null
			        ? this.championship.getTeamScoringSystem()
			        : Ranking.TOTAL;
		}
		return this.ranking != null ? this.ranking : Ranking.TOTAL;
	}

	private Integer getTopNTeamSize(Gender gender) {
		Integer maxCount = null;
		if (this.championship != null) {
			switch (gender) {
				case M:
					maxCount = this.championship.getMensBestN();
					break;
				case F:
					maxCount = this.championship.getWomensBestN();
					break;
				case MF:
					maxCount = this.championship.getMixedBestN();
					break;
				case I:
					return 0;
				default:
					break;
			}
		} else {
			Competition comp = Competition.getCurrent();
			switch (gender) {
				case M:
					maxCount = comp.getMensBestN();
					break;
				case F:
					maxCount = comp.getWomensBestN();
					break;
				case MF:
					maxCount = comp.getMixedBestNElseDefault();
					break;
				case I:
					return 0;
				default:
					break;
			}
		}
		return maxCount != null ? maxCount : Integer.MAX_VALUE;
	}

	private boolean groupIsDone(Athlete a) {
		if (this.doneGroups == null) {
			this.doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
		}
		return this.doneGroups.contains(a.getGroup());
	}

	private void init(String ageGroupPrefix, Championship ageDivision, boolean includeNotDone) {
		if (this.debug) {
			this.logger.setLevel(Level.DEBUG);
		}
		this.championship = ageDivision;
		// logger.debug("init tree {} {}", ageGroupPrefix, ageDivision);
		this.reportingBeans = Competition.getCurrent().computeReportingInfo(ageGroupPrefix, ageDivision);
		buildTeamItemTree(this.reportingBeans, ageGroupPrefix, ageDivision, includeNotDone);
		if (this.debug) {
			dumpTeams();
		}

		for (Gender g : Gender.values()) {
			List<TeamTreeItem> teams = getTeamItemsByGender().get(g);
			if (teams != null) {
				addItems(teams, TeamTreeItem::getSortedTeamMembers);
			}
		}
	}

}
