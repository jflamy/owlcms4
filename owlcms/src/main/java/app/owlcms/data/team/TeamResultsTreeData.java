/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
		for (Gender gender : Gender.mfValues()) {
			if (this.genderFilterValue != null && gender != this.genderFilterValue) {
				continue;
			}

			List<TeamTreeItem> curGenderTeams = getTeamItemsByGender().get(gender);
			if (curGenderTeams == null) {
				curGenderTeams = new ArrayList<>();
				getTeamItemsByGender().put(gender, curGenderTeams);
				this.logger.debug("created list for gender {}: {}", gender, getTeamItemsByGender().get(gender));
			}

			TeamTreeItem curTeamItem = null;
			String key = computeGenderKey(gender) + "Team"
			        + (ageGroupPrefix != null ? ageGroupPrefix : ageDivision.getName());
			this.logger.debug("looking for {} in {}", key, this.reportingBeans.keySet());

			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) this.reportingBeans.get(key);
			if (athletes == null) {
				return;
			}
			athletes = athletes.stream()
			        // .peek(a -> {
			        // logger.debug("{} {} {} {}",a.getShortName(), ((PAthlete)
			        // a)._getOriginalParticipation().getTeamMember(), a.getClass().getSimpleName(), ((PAthlete)
			        // a).getCategory());
			        // })
			        // .filter(a -> a.isTeamMember())
			        .collect(Collectors.toList());
			AthleteSorter.teamPointsOrder(athletes, this.ranking);

			String prevTeamName = null;
			if (athletes != null) {
				boolean combinedTotal = Competition.getCurrent().isSnatchCJTotalMedals();
				// count points for each team
				for (Athlete a : athletes) {
					// check if competition is a "best n results" team comp.
					// if the competition is "top n", we can have "top 4 men" + "top 2 women", so we
					// want the athlete's
					// gender.
					Integer maxCount = getTopNTeamSize(a.getGender());
					String curTeamName = a.getTeam();
					// logger.debug("a={} curTeam = {}",a, a.getTeam());
					curTeamItem = findCurTeamItem(getTeamItemsByGender(), gender, curGenderTeams, prevTeamName,
					        curTeamItem,
					        curTeamName != null ? curTeamName : "-");
					boolean groupIsDone = groupIsDone(a);
					Integer curPoints = combinedTotal ? a.getCombinedPoints() : a.getTotalPoints();
					double curSinclair = a.getSinclairForDelta();
					double curSmf = a.getSmhfForDelta();
					double curRobi = a.getRobi();
					double curGamx = a.getGamx();

					Team curTeam = curTeamItem.getTeam();

					boolean b = curTeam.getCounted() < maxCount;
					boolean c = curPoints != null && curPoints > 0;

					// if (debug) {
					// logger.debug("---- Athlete {} {} {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints,
					// curTeam.getCounted(), groupIsDone, b, c);
					// }
					if (a.isTeamMember()) {
						if ((includeNotDone || groupIsDone) && b && c) {
							curTeam.setPoints(curTeam.getPoints() + Math.round(curPoints));
						}
						if (b) {
							curTeam.setSinclairScore(curTeam.getSinclairScore() + curSinclair);
							curTeam.setSmfScore(curTeam.getSmfScore() + curSmf);
							curTeam.setCounted(curTeam.getCounted() + 1);
							curTeam.setRobi(curTeam.getRobi() + curRobi);
							curTeam.setGamx(curTeam.getGamx() + curGamx);
						}
					}
					curTeamItem.addTreeItemChild(a, groupIsDone);
					curTeam.setSize(curTeam.getSize() + 1);
					prevTeamName = curTeamName;
				}
			}
		}

		dumpTeams();
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
			default:
				genderKey = "mw";
				break;
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
	        List<TeamTreeItem> curGenderTeams, String prevTeamName, TeamTreeItem curTeamItem, String curTeamName) {
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
				curTeamItem.getTeam().setSize(AthleteRepository.countTeamMembers(curTeamName, gender));
				teamItemsByGender.get(gender).add(curTeamItem);
			}
		}
		return curTeamItem;
	}

	private Integer getTopNTeamSize(Gender gender) {
		Integer maxCount = null;
		Competition comp = Competition.getCurrent();

		switch (gender) {
			case M:
				maxCount = comp.getMensBestN() != null ? comp.getMensBestN() : Integer.MAX_VALUE;
				break;
			case F:
				maxCount = comp.getWomensBestN() != null ? comp.getWomensBestN() : Integer.MAX_VALUE;
				break;
			case I:
				return 0;
			default:
				break;
		}
		return maxCount;
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
