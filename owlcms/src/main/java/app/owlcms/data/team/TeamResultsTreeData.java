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
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.spreadsheet.PAthlete;
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
	private Set<Long> championshipCategoryIds;

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
			if (this.genderFilterValue != null && this.genderFilterValue != Gender.MF && gender != this.genderFilterValue) {
				continue;
			}

			List<TeamTreeItem> curGenderTeams = getOrCreateGenderTeams(gender);
			doTeamGender(ageGroupPrefix, ageDivision, includeNotDone, gender, curGenderTeams, null);
		}

		if (this.genderFilterValue == null || this.genderFilterValue == Gender.MF) {
			List<TeamTreeItem> mixedTeams = getOrCreateGenderTeams(Gender.MF);
			doTeamGender(ageGroupPrefix, ageDivision, includeNotDone, Gender.MF, mixedTeams, null);
			if (this.genderFilterValue == Gender.MF) {
				getTeamItemsByGender().remove(Gender.M);
				getTeamItemsByGender().remove(Gender.F);
			}
		}

		dumpTeams();
	}

	public TeamTreeItem doTeamGender(String ageGroupPrefix, Championship ageDivision, boolean includeNotDone, Gender gender, List<TeamTreeItem> curGenderTeams,
	        TeamTreeItem curTeamItem) {
		boolean mixedTeamCompetition = gender == Gender.MF;
		boolean explicitMixedTeamCompetition = mixedTeamCompetition && ageDivision != null
		        && ageDivision.isExplicitMixedTeamMembers();
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
		if (mixedTeamCompetition) {
			AthleteSorter.teamPointsOrderMixed(athletes, rankingForGender);
		} else {
			AthleteSorter.teamPointsOrder(athletes, rankingForGender);
		}
		Map<String, Integer> mixedCountedByTeam = mixedTeamCompetition ? new HashMap<>() : null;
		Map<String, Map<Gender, Integer>> mixedCountedByTeamAndGender = mixedTeamCompetition ? new HashMap<>() : null;

		String prevTeamName = null;
		if (athletes != null) {
			Championship effectiveChampionship = ageDivision != null ? ageDivision : Championship.of(null);
			boolean combinedTotal = effectiveChampionship.isSnatchCJTotalMedals();
			boolean pointsBasedTeamCompetition = mixedTeamCompetition
			        ? effectiveChampionship.computeMixedPointsBased()
			        : effectiveChampionship.computePointsBased();
			// count points for each team
			for (Athlete a : athletes) {
				String curTeamName = a.getTeam();
				String normalizedTeamName = curTeamName != null ? curTeamName : "-";
				// logger.debug("a={} curTeam = {}",a, a.getTeam());
				curTeamItem = findCurTeamItem(getTeamItemsByGender(), gender, curGenderTeams, prevTeamName,
				        curTeamItem,
				        normalizedTeamName, rankingForGender);
				boolean groupIsDone = groupIsDone(a);
				Team curTeam = curTeamItem.getTeam();

				// if (debug) {
				// logger.debug("---- Athlete {} {} {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints,
				// curTeam.getCounted(), groupIsDone, b, c);
				// }
				boolean contributes = mixedTeamCompetition
				        ? isMixedTeamMember(a, explicitMixedTeamCompetition)
				        : isTeamMember(a);
				if (contributes) {
					TeamTreeItem member = curTeamItem.addTreeItemChild(a, groupIsDone);
					member.setScoringSystem(rankingForGender);
					Integer memberPoints = explicitMixedTeamCompetition
					        ? (combinedTotal ? member.getRawCombinedPoints() : member.getRawTotalPoints())
					        : (combinedTotal ? member.getCombinedPoints() : member.getTotalPoints());
					int counted = mixedTeamCompetition
					        ? getMixedCounted(normalizedTeamName, a.getGender(), mixedCountedByTeam,
					                mixedCountedByTeamAndGender)
					        : curTeam.getCounted();
					Integer maxCount = mixedTeamCompetition ? getMixedTopNTeamSize(a.getGender()) : getTopNTeamSize(a.getGender());
					boolean b = counted < maxCount;
					boolean c = memberPoints != null && memberPoints > 0;
					boolean countedForTeam = b && groupIsDone && (!pointsBasedTeamCompetition || c);
					if (groupIsDone && b && c) {
						curTeam.setPoints(curTeam.getPoints() + Math.round(memberPoints));
					}
					member.setCountedForTeam(countedForTeam);
					if (countedForTeam) {
						curTeam.setSinclairScore(curTeam.getSinclairScore() + member.getSinclairScore());
						curTeam.setCatSinclairScore(curTeam.getCatSinclairScore() + member.getCatSinclairMetric());
						curTeam.setSmfScore(curTeam.getSmfScore() + member.getSmfScore());
						curTeam.setCounted(curTeam.getCounted() + 1);
						curTeam.setRobi(curTeam.getRobi() + member.getRobiScore());
						curTeam.setGamx(curTeam.getGamx() + member.getGamxScore());
						curTeam.setCatGamxScore(curTeam.getCatGamxScore() + member.getCatGamxScore());
						curTeam.setQPoints(curTeam.getQPoints() + member.getQPointsScore());
						curTeam.setCatQPointsScore(curTeam.getCatQPointsScore() + member.getCatQPointsMetric());
						curTeam.setQMasters(curTeam.getQMasters() + member.getQMastersScore());
						if (mixedTeamCompetition) {
							incrementMixedCounted(normalizedTeamName, a.getGender(), mixedCountedByTeam,
							        mixedCountedByTeamAndGender);
						}
					}
				}
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
				curTeamItem = new TeamTreeItem(curTeamName, gender, null, false, this.championship);
				curTeamItem.setScoringSystem(rankingForGender);
				curTeamItem.getTeam().setSize(AthleteRepository.countTeamMembers(curTeamName, gender));
				teamItemsByGender.get(gender).add(curTeamItem);
			}
		}
		return curTeamItem;
	}

	private List<TeamTreeItem> getOrCreateGenderTeams(Gender gender) {
		List<TeamTreeItem> curGenderTeams = getTeamItemsByGender().get(gender);
		if (curGenderTeams == null) {
			curGenderTeams = new ArrayList<>();
			getTeamItemsByGender().put(gender, curGenderTeams);
		}
		return curGenderTeams;
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
		return maxCount != null && maxCount > 0 ? maxCount : Integer.MAX_VALUE;
	}

	private Integer getMixedTopNTeamSize(Gender athleteGender) {
		if (this.championship != null) {
			Integer overallMixedBestN = positiveCap(this.championship.getMixedBestN());
			if (overallMixedBestN != null) {
				return overallMixedBestN;
			}
			switch (athleteGender) {
				case M:
					return positiveCapOrUnlimited(this.championship.getMixedMensBestN());
				case F:
					return positiveCapOrUnlimited(this.championship.getMixedWomensBestN());
				case I:
					return 0;
				case MF:
				default:
					return Integer.MAX_VALUE;
			}
		}

		Competition comp = Competition.getCurrent();
		switch (athleteGender) {
			case M:
				return positiveCapOrUnlimited(comp.getMensBestN());
			case F:
				return positiveCapOrUnlimited(comp.getWomensBestN());
			case I:
				return 0;
			case MF:
			default:
				return positiveCapOrUnlimited(comp.getMixedBestNElseDefault());
		}
	}

	private boolean groupIsDone(Athlete a) {
		if (this.doneGroups == null) {
			this.doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
		}
		return this.doneGroups.contains(a.getGroup());
	}

	private int getMixedCounted(String teamName, Gender athleteGender, Map<String, Integer> mixedCountedByTeam,
	        Map<String, Map<Gender, Integer>> mixedCountedByTeamAndGender) {
		if (positiveCap(this.championship != null ? this.championship.getMixedBestN() : null) != null) {
			return mixedCountedByTeam.getOrDefault(teamName, 0);
		}
		Map<Gender, Integer> countsByGender = mixedCountedByTeamAndGender.get(teamName);
		if (countsByGender == null) {
			return 0;
		}
		return countsByGender.getOrDefault(athleteGender, 0);
	}

	private void incrementMixedCounted(String teamName, Gender athleteGender, Map<String, Integer> mixedCountedByTeam,
	        Map<String, Map<Gender, Integer>> mixedCountedByTeamAndGender) {
		mixedCountedByTeam.merge(teamName, 1, Integer::sum);
		mixedCountedByTeamAndGender.computeIfAbsent(teamName, ignored -> new EnumMap<>(Gender.class))
		        .merge(athleteGender, 1, Integer::sum);
	}

	private boolean isMixedTeamMember(Athlete athlete, boolean explicitMixedTeamCompetition) {
		return explicitMixedTeamCompetition ? isExplicitMixedTeamMember(athlete) : isTeamMember(athlete);
	}

	private Integer positiveCap(Integer cap) {
		return cap != null && cap > 0 ? cap : null;
	}

	private Integer positiveCapOrUnlimited(Integer cap) {
		return positiveCap(cap) != null ? cap : Integer.MAX_VALUE;
	}

	private void init(String ageGroupPrefix, Championship ageDivision, boolean includeNotDone) {
		if (this.debug) {
			this.logger.setLevel(Level.DEBUG);
		}
		this.championship = ageDivision;
		this.championshipCategoryIds = computeChampionshipCategoryIds(ageDivision);
		// logger.debug("init tree {} {}", ageGroupPrefix, ageDivision);
		this.reportingBeans = Competition.getCurrent().computeReportingInfo(ageGroupPrefix, ageDivision);
		buildTeamItemTree(this.reportingBeans, ageGroupPrefix, ageDivision, includeNotDone);
		if (this.debug) {
			dumpTeams();
		}

		for (Gender g : Gender.values()) {
			List<TeamTreeItem> teams = getTeamItemsByGender().get(g);
			if (teams != null) {
				addItems(teams, TeamTreeItem::getCountedTeamMembers);
			}
		}
	}

	private Set<Long> computeChampionshipCategoryIds(Championship championship) {
		if (championship == null) {
			return Set.of();
		}
		return AgeGroupRepository.findFiltered(null, null, championship, null, true, -1, -1).stream()
		        .map(AgeGroup::getCategories)
		        .flatMap(List::stream)
		        .map(Category::getId)
		        .filter(id -> id != null)
		        .collect(Collectors.toSet());
	}

	private boolean isExplicitMixedTeamMember(Athlete athlete) {
		if (athlete == null) {
			return false;
		}
		if (athlete instanceof PAthlete pAthlete) {
			return pAthlete.isMixedTeamMember();
		}
		if (this.championshipCategoryIds == null || this.championshipCategoryIds.isEmpty()) {
			return false;
		}
		return athlete.getParticipations().stream()
		        .anyMatch(p -> p.getCategory() != null
		                && p.getCategory().getId() != null
		                && this.championshipCategoryIds.contains(p.getCategory().getId())
		                && p.getMixedTeamMember());
	}

	private boolean isTeamMember(Athlete athlete) {
		if (athlete == null) {
			return false;
		}
		if (athlete instanceof PAthlete pAthlete) {
			return pAthlete.isTeamMember();
		}
		if (this.championshipCategoryIds == null || this.championshipCategoryIds.isEmpty()) {
			return athlete.isTeamMember();
		}
		return athlete.getParticipations().stream()
		        .anyMatch(p -> p.getCategory() != null
		                && p.getCategory().getId() != null
		                && this.championshipCategoryIds.contains(p.getCategory().getId())
		                && p.getTeamMember());
	}

}
