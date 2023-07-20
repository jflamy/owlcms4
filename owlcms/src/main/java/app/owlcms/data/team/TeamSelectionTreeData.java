/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamSelectionTreeData extends TreeData<TeamTreeItem> {

	Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);

	private boolean debug = false;

	private final Logger logger = (Logger) LoggerFactory.getLogger(TeamSelectionTreeData.class);

	private Ranking ranking;

	private Map<String, List<TeamTreeItem>> teamsByName = new TreeMap<>();
	private Set<TeamTreeItem> teams = new TreeSet<TeamTreeItem>((a, b) -> {
		if (a.getAthlete() == null && b.getAthlete() == null) {
			int compare = 0;
			compare = a.getGender().compareTo(b.getGender());
			if (compare != 0)
				return compare;
			compare = a.getTeam().getName().compareTo(b.getTeam().getName());
			return compare;
		} else {
			int compare = 0;
			compare = a.getAthlete().getCategory()
			        .compareTo(b.getAthlete().getCategory());
			if (compare != 0)
				return compare;
			compare = a.getAthlete().getFullName().compareTo(b.getAthlete().getFullName());
			return compare;
		}
	});

	public TeamSelectionTreeData(String ageGroupPrefix, AgeDivision ageDivision, Gender gender, Ranking ranking,
	        boolean includeNotDone) {
		this.setRanking(ranking);
		init(ageGroupPrefix, ageDivision, includeNotDone);
	}

	public Ranking getRanking() {
		return ranking;
	}

	public Map<Gender, List<TeamTreeItem>> getTeamItemsByGender() {
		return teamsByGender;
	}

	public void setRanking(Ranking ranking) {
		this.ranking = ranking;
	}

	private void buildTeamItemTree(
	        Collection<Participation> participations,
	        String ageGroupPrefix,
	        AgeDivision ageDivision,
	        boolean includeNotDone) {

		logger.warn("buildTeamItemTree {}", LoggerUtils.stackTrace());
//		if (ageDivision == null) {
//			return;
//		}
		for (Gender gender : Gender.mfValues()) {
			Collection<Participation> gParticipations;
			TeamTreeItem curTeamItem = null;
			if (participations == null) {
				return;
			} else {
				gParticipations = participations.stream()
				        .filter(a -> a.getAthlete().getGender() == gender)
				        .sorted((p1,p2) -> {
				        	int compare = 0;
				        	compare = ObjectUtils.compare(p1.getAthlete().getTeam(),p2.getAthlete().getTeam());
				        	if (compare != 0) return compare;
				        	compare = ObjectUtils.compare(p1.getTeamMember(),p2.getTeamMember());
				        	if (compare != 0) return -compare;
				        	compare = p1.getCategory().compareTo(p2.getCategory());
				        	return compare;
				        })
				        .collect(Collectors.toList());
				logger.warn("participations size {}", participations.size());
			}

			if (gParticipations.size() > 0) {
				for (Participation p : gParticipations) {
					Athlete a = p.getAthlete();
					String curTeamName = p.getAthlete().getTeam();
//					logger.warn("a={} curTeam = {}", a, a.getTeam());
					curTeamItem = findCurTeamItem(
					        gender,
					        curTeamName != null ? curTeamName : "-");
					if (!teams.contains(curTeamItem)) {
						this.addRootItems(curTeamItem);
						teams.add(curTeamItem);
					}

					Team curTeam = curTeamItem.getTeam();
					logger.warn("---- Athlete {} {} {}", curTeamItem.getTeam().getName(), a, a.getGender());

					if (p.getTeamMember()) {
					}

					Group group = p.getAthlete().getGroup();
					curTeamItem.addTreeItemChild(this, new PAthlete(p), group != null ? group.isDone() : false);
					logger.warn("curTeamItem {} {}", curTeamItem.getTeam().getName(),
					        curTeamItem.getTeamMembers().size());
					curTeam.setSize(curTeam.getSize() + 1);
				}
			}
		}
		dumpTeams();
	}

	private void dumpTeams() {
		for (Gender g : Gender.values()) {
			List<TeamTreeItem> teamItems = getTeamItemsByGender().get(g);
			if (teamItems == null) {
				continue;
			}
			for (TeamTreeItem item : teamItems) {
				logger.debug("team: {} {} {} {} {}", item.getName(), item.getGender(), item.getPoints(),
				        item.getSinclairScore(), item.getCounted());
				List<TeamTreeItem> teamMembers = item.getTeamMembers();
				for (TeamTreeItem t : teamMembers) {
					logger.debug("    {} {} {}", t.getName(), t.getPoints(), t.getSinclairScore());
				}
			}
		}
	}

	private TeamTreeItem findCurTeamItem(Gender gender, String curTeamName) {
		TeamTreeItem curTeamItem;
		// maybe we have seen the team already (if mixed)
		TeamTreeItem found = null;
		for (TeamTreeItem ct : teamsByName.get(curTeamName)) {
			if (ct.getGender() != null && ct.getGender() == gender) {
				found = ct;
				break;
			}
		}
		if (found != null) {
			logger.warn("found {} {} {}",curTeamName, gender, found.getGender());
			curTeamItem = found;
		} else {
			curTeamItem = new TeamTreeItem(curTeamName, gender, null, false);
			curTeamItem.getTeam().setSize(AthleteRepository.countTeamMembers(curTeamName, gender));
			teamsByName.get(curTeamName).add(curTeamItem);
		}
		return curTeamItem;
	}

	private void init(String ageGroupPrefix, AgeDivision ageDivision, boolean includeNotDone) {
		if (debug) {
			logger.setLevel(Level.DEBUG);
		}
		// logger.debug("init tree {} {}", ageGroupPrefix, ageDivision);
		Collection<Participation> athletes = findAll(ageGroupPrefix, ageDivision, null, null, null);
		buildTeamItemTree(athletes, ageGroupPrefix, ageDivision, includeNotDone);
		if (debug) {
			dumpTeams();
		}

		for (Gender g : Gender.values()) {
			List<TeamTreeItem> teams = getTeamItemsByGender().get(g);
			if (teams != null) {
				addItems(teams, TeamTreeItem::getSortedTeamMembers);
			}
		}
	}

	public Collection<Participation> findAll(String ageGroupPrefix, AgeDivision ageDivision, Gender genderFilterValue,
	        Category catFilterValue, String teamFilterValue) {
		if (ageGroupPrefix == null && ageDivision == null) {
			return new ArrayList<>();
		}

		List<Participation> participations = AgeGroupRepository.allParticipationsForAgeGroupAgeDivision(ageGroupPrefix,
		        ageDivision);

		Set<String> teamNames = new TreeSet<>();
		Stream<Participation> stream = participations.stream()
		        .filter(p -> {
			        String catCode = catFilterValue != null ? catFilterValue.getComputedCode() : null;
			        String athleteCatCode = p.getCategory().getComputedCode();

			        String athleteTeamName = p.getAthlete().getTeam();
			        if (athleteTeamName == null || athleteTeamName.isBlank()) {
				        return false;
			        }
			        teamNames.add(athleteTeamName);

			        Gender athleteGender = p.getAthlete().getGender();
			        List<TeamTreeItem> teams = teamsByName.get(athleteTeamName);
			        TeamTreeItem genderTeam = null;
			        if (teams != null) {
				        for (TeamTreeItem t : teams) {
					        if (t.getGender() == athleteGender) {
						        genderTeam = t;
					        }
				        }
			        } else {
				        teams = new ArrayList<>();
			        }
			        if (genderTeam == null) {
				        genderTeam = new TeamTreeItem(athleteTeamName, athleteGender, null, true);
				        teams.add(genderTeam);
				        teamsByName.put(athleteTeamName, teams);
			        }

			        boolean catOk = (catFilterValue == null || athleteCatCode.contentEquals(catCode))
			                && (genderFilterValue == null || genderFilterValue == athleteGender)
			                && (teamFilterValue == null || teamFilterValue.contentEquals(athleteTeamName));
			        // logger.debug("{} {} {} {} {} {}", catFilterValue, genderFilterValue,
			        // teamFilterValue, catOk, p.getAthlete().getShortName(), athleteTeamName);
			        return catOk;
		        })
		        .sorted((a, b) -> {
			        int compare = 0;

			        String teamA = a.getAthlete().getTeam();
			        String teamB = b.getAthlete().getTeam();
			        compare = teamA.compareTo(teamB);
			        if (compare != 0) {
				        return compare;
			        }

			        Boolean memberA = a.getTeamMember();
			        Boolean memberB = b.getTeamMember();
			        compare = -(memberA.compareTo(memberB));
			        if (compare != 0) {
				        return compare;
			        }

			        Category catA = a.getCategory();
			        Category catB = b.getCategory();
			        compare = catA.compareTo(catB);
			        if (compare != 0) {
				        return compare;
			        }

			        String nameA = a.getAthlete().getFullName();
			        String nameB = b.getAthlete().getFullName();
			        compare = nameA.compareTo(nameB);
			        if (compare != 0) {
				        return compare;
			        }

			        return 0;
		        })
		// .peek(p -> logger.warn("findAll {}", p.long_dump()))
		;
		return stream.collect(Collectors.toList());
	}

}
