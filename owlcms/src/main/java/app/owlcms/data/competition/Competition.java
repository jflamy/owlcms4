/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.competition;

import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.athleteSort.WinningOrderComparator;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.MQTTMonitor;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.utils.DateTimeUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

/**
 * Class Competition.
 */
@Cacheable

// must be listed in app.owlcms.data.jpa.JPAService.entityClassNames()
@Entity
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "logger" })
public class Competition {

	public static final int SHORT_TEAM_LENGTH = 6;
	private static Competition competition;
	@Transient
	final static private Logger logger = (Logger) LoggerFactory.getLogger(Competition.class);
	private static final boolean SCORING_SYSTEM_ONLY = true;

	public static void debugRanks(String label, Athlete a) {
		logger./**/warn("{} {} {} {} {} {}", label, System.identityHashCode(a), a.getId(), a.getShortName(),
		        a.getTotalRank(), a.getCategory(), a.getParticipations().size());
	}

	@SuppressWarnings("unused")
	public static void dumpAthlete(String string, Athlete a) {
		logger./**/warn("{} id={} {} {} S={} C={} T={}", string, a.getId(), a.getAbbreviatedName(), System.identityHashCode(a), a.getBestSnatch(), a.getBestCleanJerk(),
				a.getTotal());
		for (Participation p : a.getParticipations()) {
			logger./**/warn("    {} S{} C{} T{} Sc{} {}", p.getCategory(), p.getSnatchRank(), p.getCleanJerkRank(), p.getTotalRank(), p.getCategoryScoreRank(),
					System.identityHashCode(p));
		}
	}

	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public static Competition getCurrent() {
		if (competition == null) {
			competition = CompetitionRepository.findAll().get(0);
		}
		return competition;
	}

	public static void setCurrent(Competition c) {
		competition = c;
	}

	public static void splitByGender(List<Athlete> athletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		for (Athlete l : athletes) {
			Gender gender = l.getGender();
			if (Gender.M == gender) {
				sortedMen.add(l);
			} else if (Gender.F == gender) {
				sortedWomen.add(l);
			} else {
				// throw new RuntimeException("gender is " + gender);
			}
		}
	}

	public static void splitPByGender(List<Athlete> athletes, List<Athlete> men, List<Athlete> women) {
		for (Athlete l : athletes) {
			Gender gender = l.getGender();
			if (Gender.M == gender) {
				men.add(l);
			} else if (Gender.F == gender) {
				women.add(l);
			} else {
				// throw new RuntimeException("gender is " + gender);
			}
		}
	}

	public static void splitPTeamMembersByGender(List<Athlete> athletes, List<Athlete> men, List<Athlete> women) {
		for (Athlete l : athletes) {
			if (!l.isTeamMember()) {
				continue;
			}
			Gender gender = l.getGender();
			if (Gender.M == gender) {
				men.add(l);
			} else if (Gender.F == gender) {
				women.add(l);
			} else {
				// throw new RuntimeException("gender is " + gender);
			}
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;
	private String ageGroupsFileName;
	/**
	 * announcer sees decisions as they are made by referee.
	 */
	@Column(columnDefinition = "boolean default true")
	private boolean announcerLiveDecisions = true;
	private String cardsTemplateFileName;
	private String athleteCredentialsTemplateFileName;
	private String toCredentialsTemplateFileName;
	private String coachCredentialsTemplateFileName;
	private String competitionCity;
	private LocalDate competitionDate = null;
	private LocalDate competitionEndDate = null;
	private String competitionName;
	private String competitionOrganizer;
	private String competitionSite;
	/**
	 * enable overriding total for kids categories with bonus points
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean customScore = false;
	private boolean enforce20kgRule;
	private String federation;
	private String federationAddress;
	private String federationEMail = "";
	private String federationWebSite;
	private String finalPackageTemplateFileName;
	/**
	 * In a mixed group, call all female lifters then all male lifters
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean genderOrder;
	/* in a round-robin competition, use lot number instead of ascending weight */
	@Column(columnDefinition = "boolean default false")
	private boolean fixedOrder;
	private String juryTemplateFileName;
	private boolean masters;
	/**
	 * Add W75 and W80+ masters categories
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean mastersGenderEquality = false;
	@Transient
	@JsonIgnore
	private HashMap<Group, TreeMap<String, List<Athlete>>> medalsByGroup;
	private String medalScheduleTemplateFileName;
	private String medalsTemplateFileName;
	/* this is really "keep best n results", backward compatibility with database exports */
	@Column(name = "mensTeamSize", columnDefinition = "integer default 8")
	@JsonProperty("mensTeamSize")
	private Integer mensBestN = 8;
	@Column(columnDefinition = "integer default 8")
	private Integer maxTeamSize = 8;
	@Column(columnDefinition = "integer default 2")
	private Integer maxPerCategory = 2;
	private String protocolTemplateFileName;
	private String resultsTemplateFileName;
	private String introductionTemplateFileName;
	@Transient
	@JsonIgnore
	private boolean rankingsInvalid = true;
	@Column(name = "refdelay", columnDefinition = "integer default 1500")
	private int refereeWakeUpDelay = 1500;
	@Transient
	private HashMap<String, Object> reportingBeans = new HashMap<>();
	/**
	 * All first lifts, then all second lifts, then all third lifts, etc. Can be combined with genderOrder as well.
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean roundRobinOrder;
	@Column(columnDefinition = "boolean default false")
	private boolean snatchCJTotalMedals = false;
	// private String startingWeightsSheetTemplateFileName;
	private String weighInFormTemplateFileName;
	private String emptyProtocolTemplateFileName;
	private String startListTemplateFileName;
	private String scheduleTemplateFileName;
	/**
	 * Do not require month and day for birth.
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean useBirthYear = false;
	/**
	 * Do not require month and day for birth.
	 */
	@Column(columnDefinition = "boolean default true")
	private boolean automaticCJBreak = true;
	/**
	 * Idiosyncratic rule in Québec federation computes best lifter using Sinclair at body weight boundary.
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean useCategorySinclair = false;
	@Column(columnDefinition = "integer default 2024")
	private int sinclairYear = 2024;
	/**
	 * For traditional competitions that have lower body weight comes out first. Tie breaker for identical Sinclair.
	 */
	@Column(columnDefinition = "boolean default false")
	private boolean useOldBodyWeightTieBreak = false;
	/**
	 * Obsolete. We no longer infer categories.
	 */
	@Column(columnDefinition = "boolean default false")
	@Deprecated
	private boolean useRegistrationCategory = false;
	/* this is really "keep best n results", backward compatibility with database exports */
	@Column(name = "womensTeamSize", columnDefinition = "integer default 8")
	@JsonProperty("womensTeamSize")
	private Integer womensBestN = 8;
	@Column(columnDefinition = "boolean default false")
	private boolean sinclairMeet;
	@Column(columnDefinition = "integer default 3")
	private Integer jurySize = 3;
	@Column(columnDefinition = "integer default 6")
	private Integer longerBreakMax = 6;
	@Column(columnDefinition = "integer default 10")
	private Integer longerBreakDuration = 10;
	@Column(columnDefinition = "integer default 9")
	private Integer shorterBreakMin = 9;
	@Column(columnDefinition = "integer default 10")
	private Integer shorterBreakDuration = 10;
	@Transient
	@JsonIgnore
	private boolean simulation;
	private String categoriesListTemplateFileName;
	private String bodyWeightListTemplateFileName;
	private String officialsListTemplateFileName;
	private String teamsListTemplateFileName;
	private String recordOrder;
	private Ranking scoringSystem;
	@Column(columnDefinition = "boolean default false")
	private boolean displayScores = false;
	@Column(columnDefinition = "boolean default false")
	private boolean displayScoreRanks;
	private String checkInTemplateFileName;
	@Column(columnDefinition = "boolean default false")
	private boolean displayByAgeGroup;
	@Column(columnDefinition = "boolean default true")
	private boolean announcerControlledJuryDecision = true;
	private String currentRecordsTemplateFileName;
	public String getAthleteCredentialsTemplateFileName() {
		return athleteCredentialsTemplateFileName;
	}

	public void setAthleteCredentialsTemplateFileName(String athleteCredentialsTemplateFileName) {
		this.athleteCredentialsTemplateFileName = athleteCredentialsTemplateFileName;
	}

	public String getToCredentialsTemplateFileName() {
		return toCredentialsTemplateFileName;
	}

	public void setToCredentialsTemplateFileName(String toCredentialsTemplateFileName) {
		this.toCredentialsTemplateFileName = toCredentialsTemplateFileName;
	}

	public String getCoachCredentialsTemplateFileName() {
		return coachCredentialsTemplateFileName;
	}

	public void setCoachCredentialsTemplateFileName(String coachCredentialsTemplateFileName) {
		this.coachCredentialsTemplateFileName = coachCredentialsTemplateFileName;
	}
	@Column(columnDefinition = "boolean default false")
	private boolean masters20kg = false;
	private String technicalOfficialsTemplateFileName;
	@Column(columnDefinition = "boolean default true")
	private boolean imwa = true;
	@Column(columnDefinition = "boolean default true")
	private Boolean deduct250g = true;
	@Column(columnDefinition = "boolean default false")
	private boolean manualStartNumbers = false;

	public Competition() {
		this.medalsByGroup = new HashMap<>();
	}

	/**
	 * @param g a group
	 * @return for each category represented in group g where all athletes have lifted, the medals
	 */
	public TreeMap<String, List<Athlete>> computeMedals(Group g) {
	List<Athlete> rankedAthletes = AthleteRepository.findAthletesForGlobalRanking(g, false);
	// Trace the IDs of the ranked athletes
	logger.trace("computeMedals: rankedAthletes IDs: {}", rankedAthletes == null ? null : rankedAthletes.stream().map(a -> a.getId()).toList());
	var medals = computeMedals(g, rankedAthletes);
	//logger.debug("*** ranked athletes for group {} {}", g, rankedAthletes.size());// rankedAthletes.stream().map(a -> a.getLastName()).toList());
	return medals;
	}

	/**
	 * @param g
	 * @param rankedAthletes athletes participating in the group, plus athletes in the same category that have yet to compete
	 * @return for each category, medal-winnning athletes in snatch, clean & jerk and total.
	 */
	public TreeMap<String, List<Athlete>> computeMedals(Group g, List<Athlete> rankedAthletes) {
		if (g == null) {
			return new TreeMap<>();
		}
		if (this.medalsByGroup == null) {
			logger.error("no initialization !?");
			this.medalsByGroup = new HashMap<>();
		}
		if (rankedAthletes == null || rankedAthletes.size() == 0) {
			TreeMap<String, List<Athlete>> medalsPerCategory = new TreeMap<>();
			this.medalsByGroup.put(g, medalsPerCategory);
			return medalsPerCategory;
		}

		TreeMap<String, List<Athlete>> groupMedalsByCategory = computeMedalsByCategory(rankedAthletes);
		this.medalsByGroup.put(g, groupMedalsByCategory);
		return groupMedalsByCategory;
	}

	public TreeMap<String, List<Athlete>> computeMedalsByCategory(List<Athlete> rankedAthletes) {
		// logger.debug("computeMedalsByCategory athletes {}\n{}", rankedAthletes.size(), LoggerUtils.stackTrace());
		var before = System.currentTimeMillis();

		// extract all categories
		Set<Category> medalCategories = rankedAthletes.stream()
		        .map(a -> a.getEligibleCategories())
		        .flatMap(Collection::stream)
		        .collect(Collectors.toSet());
		// logger.debug("medal categories {}", medalCategories);

		TreeMap<String, List<Athlete>> medalsByCategory = new TreeMap<>();

		// iterate over the remaining categories
		for (Category category : medalCategories) {
			List<Athlete> currentCategoryPAthletes = new ArrayList<>();
			for (Athlete a : rankedAthletes) {
				// fetch the participation that matches the category
				Stream<Participation> filter = a.getParticipations().stream()
				        .filter(p -> p.getCategory().sameAs(category))
				// .peek(p -> logger./**/warn("====== a {} {} {} p {}", a.getLastName(), a.getClass().getSimpleName(), a.getCategory(), p.getCategory()));
				;
				Optional<Participation> matchingParticipation = filter.findFirst();
				// get a PAthlete wrapper: this wrapped athlete has a single participation, to the category under consideration
				if (matchingParticipation.isPresent()) {
					PAthlete e = new PAthlete(matchingParticipation.get());
					currentCategoryPAthletes.add(e);
					//logger.debug("[CATEGORY] Processing athlete {} in category {} (scoring system: {})", e.getAbbreviatedName(), category.getCode(),
					//        category.getAgeGroup().getComputedScoringSystem());
				}
			}

			// logger.debug("*** category {} members {}", category, currentCategoryPAthletes.stream().map(a2 -> a2.getAbbreviatedName()).toList());

			/*
			 * when the last parameter "save" is true, AthleteSorter.updateEligibleCategoryRanks fetches the full athlete with all its participations from the
			 * database and updates the participation that matches the category being ranked. We want the medalists to be the PAthlete wrapper that contains
			 * just the participation for the medal category. For sorting to work, we need to pass the PAthlete wrapper so the values for the current category
			 * are the only ones available.
			 *
			 * All variables with a P are PAthletes with a single participation to the current category.
			 *
			 */
			List<Athlete> snatchPLeaders = null;
			List<Athlete> cjPLeaders = null;
			List<Athlete> updatedAthletes = null;

			snatchPLeaders = AthleteSorter.resultsOrderCopy(currentCategoryPAthletes, Ranking.SNATCH);
			updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(snatchPLeaders, Ranking.SNATCH, category);

			cjPLeaders = AthleteSorter.resultsOrderCopy(updatedAthletes, Ranking.CLEANJERK);
			updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(cjPLeaders, Ranking.CLEANJERK, category);

			List<Athlete> pMedalists;

			if (category.getAgeGroup() != null && category.getAgeGroup().getComputedScoringSystem() == Ranking.TOTAL) {
				//logger.debug("[TOTAL] Updating TOTAL and CATEGORY_SCORE ranks for category {}", category.getCode());
				List<Athlete> totalPLeaders = AthleteSorter.resultsOrderCopy(updatedAthletes, Ranking.TOTAL)
				        .stream()
				        .filter(a -> a.getTotal() > 0 && a.isEligibleForIndividualRanking())
				        .collect(Collectors.toList());
				List<Athlete> notPFinished = AthleteSorter.resultsOrderCopy(updatedAthletes, Ranking.TOTAL)
				        .stream().filter(a -> a.isEligibleForIndividualRanking() && a.getActuallyAttemptedLifts() < 6)
				        .collect(Collectors.toList());

				WinningOrderComparator comparator = new WinningOrderComparator(Ranking.TOTAL, true);
				var mSet = new TreeSet<>(comparator);
				mSet.addAll(totalPLeaders);
				mSet.addAll(cjPLeaders); // in case of bomb-out
				mSet.addAll(snatchPLeaders); // in case of bomb-out
				mSet.addAll(notPFinished); // for interim results
				pMedalists = new ArrayList<>(mSet);

				updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(new ArrayList<>(pMedalists), Ranking.TOTAL, category);
				//logger.debug("[TOTAL] After updateEligibleCategoryRanks (TOTAL), athletes: {}",
				//        updatedAthletes.stream().map(Athlete::getAbbreviatedName).toList());
				// update CATEGORY_SCORE rankings same as TOTAL.
				updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(new ArrayList<>(updatedAthletes), Ranking.CATEGORY_SCORE, category);
				//logger.debug("[CATEGORY_SCORE] After updateEligibleCategoryRanks (CATEGORY_SCORE), athletes: {}",
				//        updatedAthletes.stream().map(a -> a.getAbbreviatedName() + ": rank=" + a.getCategoryScoreRank()).toList());

				// for (Athlete a : updatedAthletes) {
				// 	dumpAthlete(category.getCode(), a);
				// }

				List<Athlete> updatedPAthletes = getPAthletes(category, updatedAthletes, false);
				medalsByCategory.put(category.getCode(), updatedPAthletes);
			} else {
				//logger.debug("[CATEGORY_SCORE] Updating CATEGORY_SCORE and TOTAL ranks for category {}", category.getCode());
				List<Athlete> scorePLeaders = AthleteSorter.resultsOrderCopy(currentCategoryPAthletes, Ranking.CATEGORY_SCORE)
				        .stream().filter(a -> a.getTotal() > 0 && a.isEligibleForIndividualRanking())
				        .collect(Collectors.toList());
				List<Athlete> notPFinished = AthleteSorter.resultsOrderCopy(currentCategoryPAthletes, Ranking.CATEGORY_SCORE)
				        .stream().filter(a -> a.isEligibleForIndividualRanking() && a.getActuallyAttemptedLifts() < 6)
				        .collect(Collectors.toList());
				WinningOrderComparator comparator = new WinningOrderComparator(Ranking.CATEGORY_SCORE, true);

				var mSet = new TreeSet<>(comparator);
				mSet.addAll(scorePLeaders);
				mSet.addAll(notPFinished);
				pMedalists = new ArrayList<>(mSet);
				pMedalists.sort(new WinningOrderComparator(Ranking.TOTAL, true));
				updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(new ArrayList<>(pMedalists), Ranking.TOTAL, category);
				//logger.debug("[CATEGORY_SCORE] After updateEligibleCategoryRanks (TOTAL), athletes: {}",
				//        updatedAthletes.stream().map(Athlete::getAbbreviatedName).toList());
				updatedAthletes.sort(comparator);
				updatedAthletes = AthleteSorter.updateEligibleCategoryRanks(new ArrayList<>(updatedAthletes), Ranking.CATEGORY_SCORE, category);
				//logger.debug("[CATEGORY_SCORE] After updateEligibleCategoryRanks (CATEGORY_SCORE), athletes: {}",
				//        updatedAthletes.stream().map(a -> a.getAbbreviatedName() + ": rank=" + a.getCategoryScoreRank()).toList());
				List<Athlete> updatedPAthletes = getPAthletes(category, updatedAthletes, false);

				// for (Athlete a : updatedAthletes) {
				// 	dumpAthlete(category.getCode(), a);
				// }

				medalsByCategory.put(category.getCode(), updatedPAthletes);
			}

			// logger./**/warn("medalists for {}", category);
			getPAthletes(category, medalsByCategory.get(category.getCode()), false);
		}
		logger.info("computeMedalsByCategory nbAthletes={} time={}ms", rankedAthletes.size(), System.currentTimeMillis() - before);
		saveAthletes(rankedAthletes);
		return medalsByCategory;
	}

	/*
	 * Used by ResultsMedals
	 *
	 * @param category
	 *
	 * @return
	 */
	public List<Athlete> computeMedalsForCategory(Category category) {
		// brute force - reuse what works
		List<Athlete> rankedAthletes = AthleteRepository.findAthletesForGlobalRanking(null, false);
		List<Athlete> treeSet = computeMedalsByCategory(rankedAthletes).get(category.getCode());
		// logger.trace("computeMedalsForCategory {}",treeSet);
		return treeSet;
	}

	synchronized public HashMap<String, Object> computeReportingInfo() {
		List<Athlete> athletes = AgeGroupRepository.allWeighedInPAthletesForAgeGroupAgeDivision(null, null);
		doComputeReportingInfo(true, athletes, (String) null, null);
		return this.reportingBeans;
	}

	synchronized public HashMap<String, Object> computeReportingInfo(String ageGroupPrefix, Championship ad) {
		// this is where we will look for the athletes that have not lifted yet when computing medals
		List<Athlete> allPAthletes = AgeGroupRepository.allPAthletesForAgeGroupAgeDivision(ageGroupPrefix, ad);
		// remove people not in a session, they withdrew, possibly after weighing in.
		allPAthletes = allPAthletes.stream().filter(a -> a.getGroup() != null).collect(Collectors.toList());

		// these are all the weighed in athletes that will be potentially earning a a medal.
		// we filter the big list so we don't hit the database again.
		List<Athlete> weighedInAthletes = allPAthletes.stream()
		        .filter(a -> a.getBodyWeight() != null && a.getBodyWeight() > 0.1).collect(Collectors.toList());

		doComputeReportingInfo(true, weighedInAthletes, ageGroupPrefix, ad);
		AthleteSorter.resultsOrder(allPAthletes, Ranking.TOTAL, false);
		this.reportingBeans.put("allPAthletes", allPAthletes);

		return this.reportingBeans;
	}

	public void doGlobalRankings(List<Athlete> athletes, Boolean scoringSystemOnly) {
		// long beforeDedup = System.currentTimeMillis();
		TreeSet<Athlete> noDup = new TreeSet<>(Comparator.comparing(Athlete::getFullId));
		for (Athlete pAthlete : athletes) {
			Athlete athlete;
			if (pAthlete instanceof PAthlete) {
				athlete = ((PAthlete) pAthlete)._getAthlete();
				noDup.add(athlete);
			} else {
				noDup.add(pAthlete);
			}
		}
		ArrayList<Athlete> nodupAthletes = new ArrayList<>(noDup);
		// long afterDedup = System.currentTimeMillis();
		// logger.trace("------------------------- dedup {}ms {}", afterDedup - beforeDedup, LoggerUtils.whereFrom(5));

		if (scoringSystemOnly) {
			// long beforeReporting = System.currentTimeMillis();
			doReporting(nodupAthletes, getScoringSystem(), true);
			// long afterReporting = System.currentTimeMillis();
			// logger.trace("------------------------- scoringSystem reporting {}ms", afterReporting - beforeReporting);
		} else {
			// long beforeReporting = System.currentTimeMillis();
			doReporting(nodupAthletes, Ranking.BW_SINCLAIR, true);
			doReporting(nodupAthletes, Ranking.SMM, true);
			doReporting(nodupAthletes, Ranking.QPOINTS, true);
			doReporting(nodupAthletes, Ranking.QAGE, true); // Q-masters
			doReporting(nodupAthletes, Ranking.CAT_SINCLAIR, true);
			doReporting(nodupAthletes, Ranking.CAT_QPOINTS, true);
			doReporting(nodupAthletes, Ranking.GAMX, true);
			doReporting(nodupAthletes, Ranking.AGEFACTORS, true); // Q-youth
			// long afterReporting = System.currentTimeMillis();
			// logger.trace("------------------------- full reporting {}ms", afterReporting - beforeReporting);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if ((obj == null) || (getClass() != obj.getClass())) {
			return false;
		}
		Competition other = (Competition) obj;
		return this.id != null && this.id.equals(other.getId());
	}

	public String getAgeGroupsFileName() {
		return this.ageGroupsFileName;
	}

	public String getBodyWeightListTemplateFileName() {
		return this.bodyWeightListTemplateFileName;
	}

	/**
	 * @return the cardsTemplateFileName
	 */
	public String getCardsTemplateFileName() {
		return this.cardsTemplateFileName;
	}

	public String getCategoriesListTemplateFileName() {
		return this.categoriesListTemplateFileName;
	}

	/**
	 * @return the startListTemplateFileName
	 */
	public String getCheckInTemplateFileName() {
		return this.checkInTemplateFileName;
	}

	/**
	 * Gets the competition city.
	 *
	 * @return the competition city
	 */
	public String getCompetitionCity() {
		return this.competitionCity;
	}

	/**
	 * Gets the competition date.
	 *
	 * @return the competition date
	 */
	public LocalDate getCompetitionDate() {
		return this.competitionDate;
	}

	@Transient
	@JsonIgnore
	public Date getCompetitionDateAsDate() {
		return DateTimeUtils.dateFromLocalDate(this.competitionDate);
	}

	/**
	 * Gets the competition name.
	 *
	 * @return the competition name
	 */
	public String getCompetitionName() {
		return this.competitionName;
	}

	/**
	 * Gets the competition organizer.
	 *
	 * @return the competition organizer
	 */
	public String getCompetitionOrganizer() {
		return this.competitionOrganizer;
	}

	/**
	 * Gets the competition site.
	 *
	 * @return the competition site
	 */
	public String getCompetitionSite() {
		return this.competitionSite;
	}

	@Transient
	@JsonIgnore
	public String getComputedCardsTemplateFileName() {
		// logger.trace("getComputedCardsTemplateFileName {}",cardsTemplateFileName);
		if (this.cardsTemplateFileName == null) {
			return "IWF-A4.xls";
		}
		return this.cardsTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public String getComputedCategoriesListTemplateFileName() {
		if (this.categoriesListTemplateFileName == null) {
			return "Categories-A4.xls";
		}
		return this.categoriesListTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public String getComputedCurrentRecordsTemplateFileName() {
		if (this.currentRecordsTemplateFileName == null) {
			return "currentRecords.xlsx";
		}
		return this.currentRecordsTemplateFileName;
	}

	/**
	 * Gets the result template file name.
	 *
	 * @return the result template file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public String getComputedFinalPackageTemplateFileName() {
		if (this.finalPackageTemplateFileName == null) {
			return "Total-A4.xls";
		} else {
			return this.finalPackageTemplateFileName;
		}
	}

	@Transient
	@JsonIgnore
	public String getComputedJuryTemplateFileName() {
		if (this.juryTemplateFileName == null) {
			return "JurySheetTemplate.xls";
		}
		return this.juryTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public String getComputedMedalScheduleTemplateFileName() {
		if (this.medalScheduleTemplateFileName == null) {
			return "MedalSchedule-A4.xls";
		}
		return this.medalScheduleTemplateFileName;
	}

	/**
	 * Gets the protocol file name.
	 *
	 * @return the protocol file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Transient
	@JsonIgnore
	public String getComputedMedalsTemplateFileName() {
		if (getMedalsTemplateFileName() == null) {
			return "Medals-A4.xls";
		} else {
			return getMedalsTemplateFileName();
		}
	}

	@Transient
	@JsonIgnore
	public String getComputedOfficialsListTemplateFileName() {
		if (this.officialsListTemplateFileName == null) {
			return "Officials-A4.xls";
		}
		return this.officialsListTemplateFileName;
	}

	/**
	 * Gets the protocol file name.
	 *
	 * @return the protocol file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Transient
	@JsonIgnore
	public String getComputedProtocolTemplateFileName() {
		if (getProtocolTemplateFileName() == null) {
			return "Protocol-A4.xls";
		} else {
			return getProtocolTemplateFileName();
		}
	}

	/**
	 * Gets the protocol file name.
	 *
	 * @return the protocol file name
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	@Transient
	@JsonIgnore
	public String getComputedResultsTemplateFileName() {
		if (getResultsTemplateFileName() == null) {
			return "Results-A4.xls";
		} else {
			return getResultsTemplateFileName();
		}
	}

	@Transient
	@JsonIgnore
	public String getComputedStartListTemplateFileName() {
		if (this.startListTemplateFileName == null) {
			return "StartSheet-A4.xls";
		}
		return this.startListTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public String getComputedTeamsListTemplateFileName() {
		if (this.teamsListTemplateFileName == null) {
			return "Teams-A4.xls";
		}
		return this.teamsListTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public String getComputedTechnicalOfficialsTemplateFileName() {
		if (this.technicalOfficialsTemplateFileName == null) {
			return "toAssignments.xlsx";
		}
		return this.technicalOfficialsTemplateFileName;
	}

	public String getCurrentRecordsTemplateFileName() {
		return this.currentRecordsTemplateFileName;
	}

	public boolean getDisplayByAgeGroup() {
		return this.isDisplayByAgeGroup();
	}

	public String getEmptyProtocolTemplateFileName() {
		return this.emptyProtocolTemplateFileName;
	}

	/**
	 * Gets the federation.
	 *
	 * @return the federation
	 */
	public String getFederation() {
		return this.federation;
	}

	/**
	 * Gets the federation address.
	 *
	 * @return the federation address
	 */
	public String getFederationAddress() {
		return this.federationAddress;
	}

	/**
	 * Gets the federation E mail.
	 *
	 * @return the federation E mail
	 */
	public String getFederationEMail() {
		return this.federationEMail;
	}

	/**
	 * Gets the federation web site.
	 *
	 * @return the federation web site
	 */
	public String getFederationWebSite() {
		return this.federationWebSite;
	}

	/**
	 * @return the finalPackageTemplateFileName
	 */
	public String getFinalPackageTemplateFileName() {
		return this.finalPackageTemplateFileName;
	}

	@Transient
	@JsonIgnore
	synchronized public List<Athlete> getGlobalScoreRanking(Gender gender) {
		return getListOrElseRecompute(
		        gender == Gender.F ? getScoringSystem().getWReportingName() : getScoringSystem().getMReportingName());
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public Long getId() {
		return this.id;
	}

	public String getIntroductionTemplateFileName() {
		return this.introductionTemplateFileName;
	}

	/**
	 * Gets the invited if born before.
	 *
	 * @return the invited if born before
	 */
	public Integer getInvitedIfBornBefore() {
		return 0;
	}

	public Integer getJurySize() {
		if (this.jurySize == null || this.jurySize < 3) {
			return 3;
		}
		return this.jurySize;
	}

	/**
	 * @return the juryTemplateFileName
	 */
	public String getJuryTemplateFileName() {
		return this.juryTemplateFileName;
	}

	@SuppressWarnings("unchecked")
	@Transient
	@JsonIgnore
	synchronized public List<Athlete> getListOrElseRecompute(String listName) {
		// logger.trace("getting list {}",listName);
		List<Athlete> athletes = (List<Athlete>) this.reportingBeans.get(listName);
		if (isRankingsInvalid() || athletes == null) {
			setRankingsInvalid(true);
			while (isRankingsInvalid()) { // could be made invalid again while we compute
				setRankingsInvalid(false);
				// recompute because an athlete has been saved (new weight requested, good/bad
				// lift, etc.)
				computeReportingInfo();
				athletes = (List<Athlete>) this.reportingBeans.get(listName);
				if (athletes == null) {
					String error = MessageFormat.format("list {0} not found", listName);
					logger./**/warn(error);
					athletes = Collections.emptyList();
				}
			}
			// logger.trace("recomputed {} size {} from {}", listName, athletes != null ? athletes.size() : null);
		} else {
			// logger.trace("found {} size {} from {}", listName, athletes != null ? athletes.size() : null);
		}
		return athletes;
	}

	// @Transient
	// @JsonIgnore
	public String getLocalizedCompetitionDate() {
		String shortPattern = null;
		try {
			Locale locale = OwlcmsSession.getLocale();
			shortPattern = DateTimeUtils.localizedShortDatePattern(locale);
			DateTimeFormatter shortStyleFormatter = DateTimeFormatter.ofPattern(shortPattern, locale);
			if (this.competitionDate == null) {
				return "";
			}
			String str = this.competitionDate.format(shortStyleFormatter);
			return str;
		} catch (Exception a) {
			// null or unparseable
			logger.error("cannot format {}: {} {}", this.competitionDate, a, shortPattern);
			LoggerUtils.logError(logger, a);
			return "";
		}
	}

	public Integer getLongerBreakDuration() {
		return this.longerBreakDuration;
	}

	public Integer getLongerBreakMax() {
		return this.longerBreakMax;
	}

	/**
	 * Gets the masters.C
	 *
	 * @return the masters
	 */
	public boolean getMasters() {
		return isMasters();
	}

	public Integer getMaxPerCategory() {
		return this.maxPerCategory != null && this.maxPerCategory > 0 ? this.maxPerCategory : 2;
	}

	public Integer getMaxTeamSize() {
		return this.maxTeamSize;
	}

	public TreeMap<String, List<Athlete>> getMedals(Group g, boolean onlyFinished) {
		TreeMap<String, List<Athlete>> medals;
		if (this.medalsByGroup == null || (medals = this.medalsByGroup.get(g)) == null || g == null) {
			medals = computeMedals(g);
		}
		final TreeMap<String, List<Athlete>> m = new TreeMap<>(medals);
		// logger.debug("medals categories keyset {}", medals.keySet());
		if (onlyFinished) {
			List<String> toRemove = medals.keySet().stream()
			        .filter(k -> {
				        List<Athlete> athletes = m.get(k);
				        if (athletes.isEmpty()) {
					        return true; // remove from list.
				        }
				        // logger.trace("athletes {} {}", k, athletes);

				        // category includes an athlete that has not finished, mark it as "to be
				        // removed". Athletes out of competition don't count as not being done.
				        boolean anyMatch = athletes.stream().anyMatch(a -> (!a.isDone(g) && a.isEligibleForIndividualRanking()));

				        // logger.trace("category {} has finished {}", k, !anyMatch);
				        // return those that have not finished
				        return anyMatch;
			        })
			        .collect(Collectors.toList());
			logger.info("notFinished {}", toRemove);
			for (String notFinished : toRemove) {
				m.remove(notFinished);
			}
		}
		return m;

	}

	@Transient
	@JsonIgnore
	public Integer getMenBestNElseDefault() {
		return this.mensBestN != null ? this.mensBestN : this.maxTeamSize;
	}

	public Integer getMensBestN() {
		return this.mensBestN;
	}

	public String getOfficialsListTemplateFileName() {
		return this.officialsListTemplateFileName;
	}

	/**
	 * @return the protocolTemplateFileName
	 */
	public String getProtocolTemplateFileName() {
		return this.protocolTemplateFileName;
	}

	public String getRecordOrder() {
		return this.recordOrder;
	}

	public int getRefereeWakeUpDelay() {
		return this.refereeWakeUpDelay;
	}

	@Transient
	@JsonIgnore
	public HashMap<String, Object> getReportingBeans() {
		return this.reportingBeans;
	}

	public String getResultsTemplateFileName() {
		return this.resultsTemplateFileName;
	}

	public String getScheduleTemplateFileName() {
		return this.scheduleTemplateFileName;
	}

	public Ranking getScoringSystem() {
		if (this.scoringSystem == null) {
			return Ranking.BW_SINCLAIR;
		}
		return this.scoringSystem;
	}

	public Integer getShorterBreakDuration() {
		return this.shorterBreakDuration;
	}

	public Integer getShorterBreakMin() {
		return this.shorterBreakMin;
	}

	public int getSinclairYear() {
		return this.sinclairYear;
	}

	/**
	 * @return the startListTemplateFileName
	 */
	public String getStartListTemplateFileName() {
		return this.startListTemplateFileName;
	}

	public String getTeamsListTemplateFileName() {
		return this.teamsListTemplateFileName;
	}

	public String getTechnicalOfficialsTemplateFileName() {
		return this.technicalOfficialsTemplateFileName;
	}

	public String getTranslatedScoringSystemName() {
		String translate = Translator.translateOrElseNull("Ranking." + getScoringSystem(), OwlcmsSession.getLocale());
		return translate != null ? translate : Translator.translate("Score");
	}

	public String getWeighInFormTemplateFileName() {
		return this.weighInFormTemplateFileName;
	}

	@Transient
	@JsonIgnore
	public Integer getWomenBestNElseDefault() {
		return this.womensBestN != null ? this.womensBestN : this.maxTeamSize;
	}

	public Integer getWomensBestN() {
		return this.womensBestN;
	}

	@Override
	public int hashCode() {
		return 31;
	}

	public boolean isAnnouncerControlledJuryDecision() {
		return this.announcerControlledJuryDecision;
	}

	public boolean isAnnouncerLiveDecisions() {
		return this.announcerLiveDecisions;
	}

	public boolean isAutomaticCJBreak() {
		return this.automaticCJBreak;
	}

	@Transient
	@JsonIgnore
	public boolean isByAgeGroup() {
		return this.isDisplayByAgeGroup() || this.isMasters();
	}

	public boolean isCustomScore() {
		return this.customScore;
	}

	public boolean isDisplayByAgeGroup() {
		return this.displayByAgeGroup;
	}

	public boolean isDisplayScoreRanks() {
		return this.displayScoreRanks || Config.getCurrent().featureSwitch("displayBestScoreRank");
	}

	public boolean isDisplayScores() {
		return this.displayScores || Config.getCurrent().featureSwitch("displayBestScore");
	}

	public boolean isManualStartNumbers() {
		return this.manualStartNumbers || Config.getCurrent().featureSwitch("manualStartNumbers");
	}

	public void setManualStartNumbers(boolean manualStartNumbers) {
		this.manualStartNumbers = manualStartNumbers;
	}

	/**
	 * Checks if is enforce 20 kg rule.
	 *
	 * @return true, if is enforce 20 kg rule
	 */
	public boolean isEnforce20kgRule() {
		return this.enforce20kgRule;
	}

	public boolean isFixedOrder() {
		if (StartupUtils.getBooleanParam("fixedOrder")) {
			setFixedOrder(true);
		}
		return this.fixedOrder;
	}

	public boolean isGenderInclusive() {
		return Config.getCurrent().featureSwitch("genderInclusive");
	}

	public boolean isGenderOrder() {
		if (StartupUtils.getBooleanParam("genderOrder")) {
			setGenderOrder(true);
		}
		return this.genderOrder;
	}

	// public boolean isSimulation() {
	// return this.simulation;
	// }

	/**
	 * Checks if is masters.
	 *
	 * @return true, if is masters
	 */
	public boolean isMasters() {
		return this.masters;
	}

	public boolean isMastersGenderEquality() {
		return this.mastersGenderEquality;
	}

	synchronized public boolean isRankingsInvalid() {
		return this.rankingsInvalid;
	}

	public boolean isRoundRobinOrder() {
		return this.roundRobinOrder;
	}

	public boolean isSinclair() {
		return this.sinclairMeet || Config.getCurrent().featureSwitch("SinclairMeet");
	}

	public boolean isSnatchCJTotalMedals() {
		return this.snatchCJTotalMedals;
	}

	/**
	 * Checks if is use birth year.
	 *
	 * @return the useBirthYear
	 */
	public boolean isUseBirthYear() {
		return this.useBirthYear;
	}

	/**
	 * Checks if is use category sinclair.
	 *
	 * @return true, use category sinclair
	 */
	public boolean isUseCategorySinclair() {
		return this.useCategorySinclair;
	}

	/**
	 * Checks if is use old body weight tie break.
	 *
	 * @return true, if is use old body weight tie break
	 */
	public boolean isUseOldBodyWeightTieBreak() {
		return this.useOldBodyWeightTieBreak;
	}

	/**
	 * Checks if is use registration category.
	 *
	 * @return true, if is use registration category
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public boolean isUseRegistrationCategory() {
		return false;
	}

	public List<Athlete> mapToParticipations(List<Athlete> rankedAthletes, boolean resultsByCategory) {
		List<Athlete> pAthletes;
		if (resultsByCategory) {
			pAthletes = new ArrayList<>(rankedAthletes.size() * 2);
			for (Athlete a : rankedAthletes) {
				Athlete pa = a;
				if (a instanceof PAthlete) {
					pa = ((PAthlete) a)._getAthlete();
				}
				for (Participation p : pa.getParticipations()) {
					PAthlete e = new PAthlete(p);
					// logger.debug("pa {} participation {} paCat {}", pa.getFullName(), p.getCategory().getCode(), e.getCategory().getCode());
					pAthletes.add(e);
				}
			}
		} else {
			// we sometimes get pAthletes and but here we need the wrapped athlete.
			pAthletes = rankedAthletes.stream()
			        .peek(r -> {
				        // logger.debug("{} {}", r.getShortName(), r.getClass().getSimpleName());
			        })
			        .map(r -> r instanceof PAthlete ? r : new PAthlete(r))
			        .collect(Collectors.toList());
		}
		return pAthletes;
	}

	public void scoringSystemRankings(EntityManager em) {
		// long beforeFindAll = System.currentTimeMillis();
		List<Athlete> athletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, null, true, null);
		// long afterFindAll = System.currentTimeMillis();
		// logger.trace("------------------------- scoringSystemRankings doFindAllByGroupAndWeighIn {}ms", afterFindAll - beforeFindAll);
		doGlobalRankings(athletes, SCORING_SYSTEM_ONLY);
	}

	public void setAgeGroupsFileName(String localizedName) {
		this.ageGroupsFileName = localizedName;
	}

	public void setAnnouncerControlledJuryDecision(boolean announcerControlledJuryDecision) {
		this.announcerControlledJuryDecision = announcerControlledJuryDecision;
	}

	public void setAnnouncerLiveDecisions(boolean announcerLiveDecisions) {
		this.announcerLiveDecisions = announcerLiveDecisions;
	}

	public void setAutomaticCJBreak(boolean automaticCJBreak) {
		this.automaticCJBreak = automaticCJBreak;
	}

	public void setBodyWeightListTemplateFileName(String bodyweightListTemplateFileName) {
		this.bodyWeightListTemplateFileName = bodyweightListTemplateFileName;
	}

	/**
	 * @param cardsTemplateFileName the cardsTemplateFileName to set
	 */
	public void setCardsTemplateFileName(String cardsFileName) {
		this.cardsTemplateFileName = cardsFileName;
	}

	public void setCategoriesListTemplateFileName(String categoriesListTemplateFileName) {
		this.categoriesListTemplateFileName = categoriesListTemplateFileName;
	}

	public void setCheckInTemplateFileName(String startingListFileName) {
		this.checkInTemplateFileName = startingListFileName;
	}

	/**
	 * Sets the competition city.
	 *
	 * @param competitionCity the new competition city
	 */
	public void setCompetitionCity(String competitionCity) {
		this.competitionCity = competitionCity;
	}

	/**
	 * Sets the competition date.
	 *
	 * @param localDate the new competition date
	 */
	public void setCompetitionDate(LocalDate localDate) {
		this.competitionDate = localDate;
	}

	public void setCompetitionDateAsDate(Date ignored) {
	}

	/**
	 * Sets the competition name.
	 *
	 * @param competitionName the new competition name
	 */
	public void setCompetitionName(String competitionName) {
		this.competitionName = competitionName;
	}

	/**
	 * Sets the competition organizer.
	 *
	 * @param competitionOrganizer the new competition organizer
	 */
	public void setCompetitionOrganizer(String competitionOrganizer) {
		this.competitionOrganizer = competitionOrganizer;
	}

	/**
	 * Sets the competition site.
	 *
	 * @param competitionSite the new competition site
	 */
	public void setCompetitionSite(String competitionSite) {
		this.competitionSite = competitionSite;
	}

	public void setCurrentRecordsTemplateFileName(String currentRecordsTemplateFileName) {
		this.currentRecordsTemplateFileName = currentRecordsTemplateFileName;
	}

	public void setCustomScore(boolean customScore) {
		this.customScore = customScore;
	}

	public void setDisplayByAgeGroup(boolean displayByAgeGroup) {
		this.displayByAgeGroup = displayByAgeGroup;
	}

	public void setDisplayScoreRanks(boolean displayScoreRanks) {
		this.displayScoreRanks = displayScoreRanks;
	}

	public void setDisplayScores(boolean score) {
		this.displayScores = score;
	}

	public void setEmptyProtocolTemplateFileName(String emptyProtocolFileName) {
		this.emptyProtocolTemplateFileName = emptyProtocolFileName;
	}

	public void setEnforce20kgRule(boolean enforce20kgRule) {
		this.enforce20kgRule = enforce20kgRule;
	}

	/**
	 * Sets the federation.
	 *
	 * @param federation the new federation
	 */
	public void setFederation(String federation) {
		this.federation = federation;
	}

	/**
	 * Sets the federation address.
	 *
	 * @param federationAddress the new federation address
	 */
	public void setFederationAddress(String federationAddress) {
		this.federationAddress = federationAddress;
	}

	/**
	 * Sets the federation E mail.
	 *
	 * @param federationEMail the new federation E mail
	 */
	public void setFederationEMail(String federationEMail) {
		this.federationEMail = federationEMail;
	}

	/**
	 * Sets the federation web site.
	 *
	 * @param federationWebSite the new federation web site
	 */
	public void setFederationWebSite(String federationWebSite) {
		this.federationWebSite = federationWebSite;
	}

	/**
	 * Sets the result template file name.
	 *
	 * @param finalPackageTemplateFileName the new result template file name
	 */
	public void setFinalPackageTemplateFileName(String finalPackageTemplateFileName) {
		this.finalPackageTemplateFileName = finalPackageTemplateFileName;
	}

	public void setFixedOrder(boolean fixedOrder) {
		this.fixedOrder = fixedOrder;
	}

	public void setGenderOrder(boolean genderOrder) {
		this.genderOrder = genderOrder;
	}

	public void setIntroductionTemplateFileName(String introductionTemplateFileName) {
		this.introductionTemplateFileName = introductionTemplateFileName;
	}

	/**
	 * Sets the invited if born before.
	 *
	 * @param invitedIfBornBefore the new invited if born before
	 */
	public void setInvitedIfBornBefore(Integer invitedIfBornBefore) {
	}

	public void setJurySize(Integer jurySize) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop != null) {
			MQTTMonitor mqttMonitor = fop.getMqttMonitor();
			if (mqttMonitor != null) {
				mqttMonitor.publishMqttConfig();
			}
		}
		this.jurySize = jurySize;
	}

	public void setJuryTemplateFileName(String juryTemplateFileName) {
		this.juryTemplateFileName = juryTemplateFileName;
	}

	public void setLocalizedCompetitionDate(String ignored) {
	}

	public void setLongerBreakDuration(Integer longerBreakDuration) {
		this.longerBreakDuration = longerBreakDuration;
	}

	public void setLongerBreakMax(Integer longerBreakMax) {
		this.longerBreakMax = longerBreakMax;
	}

	public void setMasters(boolean masters) {
		this.masters = masters;
	}

	public void setMastersGenderEquality(boolean mastersGenderEquality) {
		this.mastersGenderEquality = mastersGenderEquality;
	}

	public void setMaxPerCategory(Integer maxPerCategory) {
		this.maxPerCategory = maxPerCategory;
	}

	public void setMaxTeamSize(Integer maxTeamSize) {
		this.maxTeamSize = maxTeamSize;
	}

	public void setMedalScheduleTemplateFileName(String medalScheduleTemplateFileName) {
		this.medalScheduleTemplateFileName = medalScheduleTemplateFileName;
	}

	public void setMedalsTemplateFileName(String medalsTemplateFileName) {
		this.medalsTemplateFileName = medalsTemplateFileName;
	}

	public void setMensBestN(Integer mensTeamSize) {
		this.mensBestN = mensTeamSize;
	}

	public void setOfficialsListTemplateFileName(String officialsListTemplateFileName) {
		this.officialsListTemplateFileName = officialsListTemplateFileName;
	}

	/**
	 * Sets the protocol file name.
	 *
	 * @param protocolTemplateFileName the new protocol file name
	 */
	public void setProtocolTemplateFileName(String protocolFileName) {
		this.protocolTemplateFileName = protocolFileName;
	}

	synchronized public void setRankingsInvalid(boolean invalid) {
		this.rankingsInvalid = invalid;
	}

	public void setRecordOrder(String recordOrder) {
		this.recordOrder = recordOrder;
	}

	public void setRefereeWakeUpDelay(int refereeWakeUpDelay) {
		this.refereeWakeUpDelay = refereeWakeUpDelay;
	}

	public void setResultsTemplateFileName(String resultsTemplateFileName) {
		this.resultsTemplateFileName = resultsTemplateFileName;
	}

	public void setRoundRobinOrder(boolean roundRobinOrder) {
		this.roundRobinOrder = roundRobinOrder;
	}

	public void setScheduleTemplateFileName(String scheduleTemplateFileName) {
		this.scheduleTemplateFileName = scheduleTemplateFileName;
	}

	public void setScoringSystem(Ranking scoringSystem) {
		if (!Ranking.scoringSystems().contains(scoringSystem)) {
			//throw new IllegalArgumentException(scoringSystem + " is not a scoring system");
			logger.error("{} is not a scoring system", scoringSystem);
			return;
		}
		this.scoringSystem = scoringSystem;
	}

	public void setShorterBreakDuration(Integer shorterBreakDuration) {
		this.shorterBreakDuration = shorterBreakDuration;
	}

	public void setShorterBreakMin(Integer shorterBreakMin) {
		this.shorterBreakMin = shorterBreakMin;
	}

	public void setSimulation(boolean b) {
		this.simulation = b;
	}

	public void setSinclair(boolean b) {
		this.sinclairMeet = b;
	}

	public void setSinclairYear(int sinclairYear) {
		this.sinclairYear = sinclairYear;
	}

	public void setSnatchCJTotalMedals(boolean snatchCJTotalMedals) {
		this.snatchCJTotalMedals = snatchCJTotalMedals;
	}

	public void setStartListTemplateFileName(String startingListFileName) {
		this.startListTemplateFileName = startingListFileName;
	}

	public void setTeamsListTemplateFileName(String teamsListTemplateFileName) {
		this.teamsListTemplateFileName = teamsListTemplateFileName;
	}

	public void setTechnicalOfficialsTemplateFileName(String technicalOfficialsTemplateFileName) {
		this.technicalOfficialsTemplateFileName = technicalOfficialsTemplateFileName;
	}

	/**
	 * Sets the use birth year.
	 *
	 * @param b the new use birth year
	 */
	public void setUseBirthYear(boolean b) {
		this.useBirthYear = b;
	}

	/**
	 * Sets the use registration category. No longer used. We always use the category. Only kept for backward compatibility.
	 *
	 * @param useRegistrationCategory the useRegistrationCategory to set
	 */
	@Deprecated
	@Transient
	@JsonIgnore
	public void setUseRegistrationCategory(boolean useRegistrationCategory) {
		this.useRegistrationCategory = false;
	}

	public void setWeighInFormTemplateFileName(String weighInFormTemplateFileName) {
		this.weighInFormTemplateFileName = weighInFormTemplateFileName;
	}

	public void setWomensBestN(Integer womensTeamSize) {
		this.womensBestN = womensTeamSize;
	}

	@Override
	public String toString() {
		return "Competition [id=" + this.id + ", competitionName=" + this.competitionName + ", competitionDate="
		        + this.competitionDate
		        + ", competitionOrganizer=" + this.competitionOrganizer + ", competitionSite=" + this.competitionSite
		        + ", competitionCity=" + this.competitionCity + ", federation=" + this.federation
		        + ", federationAddress="
		        + this.federationAddress + ", federationEMail=" + this.federationEMail + ", federationWebSite="
		        + this.federationWebSite + ", protocolTemplateFileName=" + getProtocolTemplateFileName()
		        + ", finalPackageTemplateFileName=" + this.finalPackageTemplateFileName
		        + ", ageGroupsFileName=" + this.ageGroupsFileName + ", enforce20kgRule="
		        + this.enforce20kgRule + ", masters=" + this.masters + ", mensTeamSize=" + this.mensBestN
		        + ", womensTeamSize="
		        + this.womensBestN + ", customScore=" + this.customScore + ", mastersGenderEquality="
		        + this.mastersGenderEquality
		        + ", useBirthYear=" + isUseBirthYear() + ", useCategorySinclair=" + this.useCategorySinclair
		        + ", useOldBodyWeightTieBreak=" + this.useOldBodyWeightTieBreak + ", useRegistrationCategory="
		        + this.useRegistrationCategory + ", reportingBeans=" + this.reportingBeans + "]";
	}

	private void addToReportingBean(String string, List<Athlete> sorted) {
		List<Athlete> athletes = getOrCreateBean(string);
		athletes.addAll(sorted);
	}

	private void categoryRankings(List<Athlete> athletes) {
		this.reportingBeans.clear();

		this.reportingBeans.put("competition", Competition.getCurrent());
		this.reportingBeans.put("groups", GroupRepository.findAll().stream().sorted((a, b) -> {
			int compare = ObjectUtils.compare(a.getWeighInTime(), b.getWeighInTime(), true);
			if (compare != 0) {
				return compare;
			}
			return compare = ObjectUtils.compare(a.getPlatform(), b.getPlatform(), true);
		}).collect(Collectors.toList()));
		this.reportingBeans.put("t", Translator.getMap());

		doReporting(athletes, Ranking.SNATCH, false);
		doReporting(athletes, Ranking.CLEANJERK, false);
		doMixedReporting(athletes, Ranking.TOTAL, false);
		doReporting(athletes, Ranking.CUSTOM, false);
		// you can have two robi (one for junior, one for senior)
		doMixedReporting(athletes, Ranking.ROBI, false);
	}

	private void clearTeamReportingBeans(String suffix) {
		suffix = suffix != null ? suffix : "";
		getOrCreateBean("mCombined" + suffix).clear();
		getOrCreateBean("wCombined" + suffix).clear();
		getOrCreateBean("mwCombined" + suffix).clear();
		getOrCreateBean("mTeam" + suffix).clear();
		getOrCreateBean("wTeam" + suffix).clear();
		getOrCreateBean("mwTeam" + suffix).clear();
		getOrCreateBean("mCustom" + suffix).clear();
		getOrCreateBean("wCustom" + suffix).clear();
		getOrCreateBean("mwCustom" + suffix).clear();
		getOrCreateBean("mTeamBest" + suffix).clear();
		getOrCreateBean("wTeamBest" + suffix).clear();
	}

	private void doComputeReportingInfo(boolean full, List<Athlete> athletes, String ageGroupPrefix,
	        Championship ad) {

		// reporting does many database queries. fork a low-priority thread.
		// logger.trace("doComputeReportingInfo {}",LoggerUtils.whereFrom());
		runInThread(() -> {
			if (athletes.isEmpty()) {
				// prevent outputting silliness.
				logger./**/warn("no athletes");
				this.reportingBeans.clear();
				return;
			}

			try {
				// the ranks within a category are stored in the database and
				// not recomputed
				categoryRankings(athletes);

				// splitResultsByGroups(athletes);
				if (full) {
					this.reportingBeans.put("athletes", athletes);
					// logger.trace("championship={} ageGroupPrefix={}", ad, ageGroupPrefix);
					if (ad != null && (ageGroupPrefix == null || ageGroupPrefix.isBlank())) {
						// iterate over all age groups present in championship ad
						teamRankingsForAgeDivision(ad);
					} else {
						teamRankings(athletes, ageGroupPrefix);
					}
				}

				doGlobalRankings(athletes, false);
			} catch (Throwable t) {
				t.printStackTrace();
				throw t;
			}
			// globalRankings();
		}, Thread.MIN_PRIORITY);
	}

	private void doMixedReporting(List<Athlete> athletes, Ranking ranking, boolean overall) {
		List<Athlete> sortedAthletes;
		List<Athlete> sortedMen;
		List<Athlete> sortedWomen;
		String mBeanName;
		String wBeanName;
		String mwBeanName;
		sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, ranking);
		if (overall) {
			AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, ranking);
		}
		sortedMen = new ArrayList<>(sortedAthletes.size());
		sortedWomen = new ArrayList<>(sortedAthletes.size());
		splitByGender(sortedAthletes, sortedMen, sortedWomen);
		mBeanName = ranking.getMReportingName();
		wBeanName = ranking.getWReportingName();
		mwBeanName = ranking.getMWReportingName();
		this.reportingBeans.put(mBeanName, sortedMen);
		this.reportingBeans.put(wBeanName, sortedWomen);
		this.reportingBeans.put(mwBeanName, sortedAthletes);
		// logger.trace("{} {}", mBeanName, sortedMen);
		// logger.trace("{} {}", wBeanName, sortedWomen);
		// logger.trace("{} {}", mwBeanName, sortedAthletes);
	}

	private void doReporting(List<Athlete> athletes, Ranking ranking, boolean overall) {
		List<Athlete> sortedAthletes;
		List<Athlete> sortedMen;
		List<Athlete> sortedWomen;
		String mBeanName;
		String wBeanName;
		sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, ranking);
		if (overall) {
			AthleteSorter.assignOverallRanksAndPoints(sortedAthletes, ranking);
		}
		sortedMen = new ArrayList<>(sortedAthletes.size());
		sortedWomen = new ArrayList<>(sortedAthletes.size());
		splitByGender(sortedAthletes, sortedMen, sortedWomen);
		mBeanName = ranking.getMReportingName();
		wBeanName = ranking.getWReportingName();
		this.reportingBeans.put(mBeanName, sortedMen);
		this.reportingBeans.put(wBeanName, sortedWomen);
		// logger.trace("{} {}", mBeanName, sortedMen);
		// logger.trace("{} {}", wBeanName, sortedWomen);
		// additional entry in the map so we can have a simple book with
		// just the global score.
		this.reportingBeans.put("mBest", AthleteSorter.resultsOrderCopy(sortedMen, Competition.getCurrent().getScoringSystem()));
		this.reportingBeans.put("wBest", AthleteSorter.resultsOrderCopy(sortedWomen, Competition.getCurrent().getScoringSystem()));
	}

	/**
	 * Compute a team-ranking for the specified PAthletes.
	 *
	 * PAthletes have a single participation, which is the one that will be used for ranking. Caller is responsible for putting several age groups together
	 * (e.g. for Masters), or using a single age group (e.g. SR)
	 *
	 * Reporting beans are modified. Caller must clear them beforehand if needed.
	 *
	 * @param athletes
	 * @param singleAgeGroup true if not called in a loop, can compute team stats.
	 * @param ageGroupPrefix
	 */
	private void doTeamRankings(List<Athlete> athletes, String suffix, boolean singleAgeGroup) {
		// team-oriented rankings. These rankings put all the athletes from the same
		// team together, sorted according to their points, so the top n can be kept if
		// needed.
		// substitutes are not included -- they should be marked as
		// !isEligibleForTeamRanking
		suffix = suffix != null ? suffix : "";

		List<Athlete> sortedAthletes;
		List<Athlete> sortedMen = new ArrayList<>();
		List<Athlete> sortedWomen = new ArrayList<>();
		splitPTeamMembersByGender(athletes, sortedMen, sortedWomen);
		athletes = new ArrayList<>();
		athletes.addAll(sortedMen);
		athletes.addAll(sortedWomen);

		suffix = suffix != null ? suffix : "";
		sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.TOTAL);
		sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.TOTAL);
		sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.TOTAL);
		addToReportingBean("mTeam" + suffix, sortedMen);
		addToReportingBean("wTeam" + suffix, sortedWomen);
		addToReportingBean("mwTeam" + suffix, sortedAthletes);
		if (singleAgeGroup) {
			reportTeams(sortedAthletes, sortedMen, sortedWomen);
		}

		sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.SNATCH_CJ_TOTAL);
		sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.SNATCH_CJ_TOTAL);
		sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.SNATCH_CJ_TOTAL);
		addToReportingBean("mCombined" + suffix, sortedMen);
		addToReportingBean("wCombined" + suffix, sortedWomen);
		addToReportingBean("mwCombined" + suffix, sortedAthletes);
		if (singleAgeGroup) {
			reportCombined(sortedAthletes, sortedMen, sortedWomen);
		}

		// this is per age group ranking
		sortedAthletes = AthleteSorter.teamPointsOrderCopy(athletes, Ranking.CUSTOM);
		sortedMen = AthleteSorter.teamPointsOrderCopy(sortedMen, Ranking.CUSTOM);
		sortedWomen = AthleteSorter.teamPointsOrderCopy(sortedWomen, Ranking.CUSTOM);
		addToReportingBean("mCustom" + suffix, sortedMen);
		addToReportingBean("wCustom" + suffix, sortedWomen);
		addToReportingBean("mwCustom" + suffix, sortedAthletes);
		if (singleAgeGroup) {
			reportCustom(sortedAthletes, sortedMen, sortedWomen);
		}

		AthleteSorter.teamPointsOrder(sortedMen, Competition.getCurrent().getScoringSystem());
		AthleteSorter.teamPointsOrder(sortedWomen, Competition.getCurrent().getScoringSystem());
		addToReportingBean("mTeamBest" + suffix, sortedMen);
		// logger.debug("mteamBest {} {}",
		// sortedMen.stream()
		// .filter(a -> a.getTeam().equals("Category 5 Athletics"))
		// .map(a -> a.getAbbreviatedName() + " " + a.getBestLifterScore() + " " +a.getBestLifterRank())
		// .collect(Collectors.joining("\n")));
		addToReportingBean("wTeamBest" + suffix, sortedWomen);
	}

	private String getMedalsTemplateFileName() {
		return this.medalsTemplateFileName;
	}

	@SuppressWarnings("unchecked")
	private List<Athlete> getOrCreateBean(String string) {
		List<Athlete> list = (List<Athlete>) this.reportingBeans.get(string);
		if (list == null) {
			list = new ArrayList<>();
			this.reportingBeans.put(string, list);
		}
		return list;
	}

	private List<Athlete> getPAthletes(Category category, List<Athlete> medalists, boolean debug) {
		// logger.trace("getPathletes {} ({})", category, LoggerUtils.whereFrom());
		List<Athlete> nMedalists = new ArrayList<>();
		for (Athlete med : medalists) {
			// need the right participation with the right category.
			Optional<Participation> part = med.getParticipations().stream().filter(p -> p.getCategory().sameAs(category)).findFirst();
			if (part.isPresent()) {
				var particip = part.get();
				if (debug) {
					logger./**/debug("    {}\tS{} C{} T{} Sc{} {} {}", med.getAbbreviatedName(), particip.getSnatchRank(),
					        particip.getCleanJerkRank(), particip.getTotalRank(), particip.getCategoryScoreRank(), System.identityHashCode(particip),
					        System.identityHashCode(particip.getAthlete()));
				}
				nMedalists.add(new PAthlete(particip));
			}
		}
		return nMedalists;
	}

	private void reportCombined(List<Athlete> sortedAthletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		getOrCreateBean("mCombined");
		this.reportingBeans.put("mCombined", sortedMen);
		getOrCreateBean("wCombined");
		this.reportingBeans.put("wCombined", sortedWomen);
		getOrCreateBean("mwCombined");
		this.reportingBeans.put("mwCombined", sortedAthletes);
	}

	private void reportCustom(List<Athlete> sortedAthletes, List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		// these are the per-age-group values
		getOrCreateBean("mCustom");
		this.reportingBeans.put("mCustom", sortedMen);
		getOrCreateBean("wCustom");
		this.reportingBeans.put("wCustom", sortedWomen);
		getOrCreateBean("mwCustom");
		this.reportingBeans.put("mwCustom", sortedAthletes);
	}

	private void reportQAge(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		getOrCreateBean("mQAge");
		this.reportingBeans.put("mQAge", sortedMen);
		getOrCreateBean("wQAge");
		this.reportingBeans.put("wQAge", sortedWomen);
	}

	private void reportQPoints(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		getOrCreateBean("mQPoints");
		this.reportingBeans.put("mQPoints", sortedMen);
		getOrCreateBean("wQPoints");
		this.reportingBeans.put("wQPoints", sortedWomen);
	}

	private void reportSinclair(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		getOrCreateBean("mSinclair");
		this.reportingBeans.put("mSinclair", sortedMen);
		getOrCreateBean("wSinclair");
		this.reportingBeans.put("wSinclair", sortedWomen);
	}

	private void reportSMF(List<Athlete> sortedMen, List<Athlete> sortedWomen) {
		getOrCreateBean("mSMF");
		this.reportingBeans.put("mSMF", sortedMen);
		getOrCreateBean("wSMF");
		this.reportingBeans.put("wSMF", sortedWomen);
	}

	private void reportTeams(List<Athlete> sortedAthletes, List<Athlete> sortedMen,
	        List<Athlete> sortedWomen) {
		// only needed once
		this.reportingBeans.put("nbMen", sortedMen.size());
		this.reportingBeans.put("nbWomen", sortedWomen.size());
		this.reportingBeans.put("nbAthletes", sortedAthletes.size());
		// logger.trace("sortedMen {} sortedWomen {} sortedCombined {}", sortedMen.size(), sortedWomen.size(), sortedAthletes.size());

		// extract club lists
		TreeSet<String> teams = new TreeSet<>();
		for (Athlete curAthlete : sortedAthletes) {
			if (curAthlete.getTeam() != null) {
				teams.add(curAthlete.getTeam());
			}
		}

		getOrCreateBean("mTeam");
		this.reportingBeans.put("mTeam", sortedMen);
		getOrCreateBean("wTeam");
		this.reportingBeans.put("wTeam", sortedWomen);
		getOrCreateBean("mwTeam");
		this.reportingBeans.put("mwTeam", sortedAthletes);

		this.reportingBeans.put("clubs", teams);
		this.reportingBeans.put("nbClubs", teams.size());
		if (sortedMen.size() > 0) {
			this.reportingBeans.put("mClubs", teams);
		} else {
			this.reportingBeans.put("mClubs", new ArrayList<>());
		}
		if (sortedWomen.size() > 0) {
			this.reportingBeans.put("wClubs", teams);
		} else {
			this.reportingBeans.put("wClubs", new ArrayList<>());
		}
	}

	private void runInThread(Runnable runnable, int priority) {
		Thread t = new Thread(runnable);
		AtomicReference<Throwable> errorReference = new AtomicReference<>();
		t.setUncaughtExceptionHandler((th, ex) -> {
			errorReference.set(ex);
		});
		t.setPriority(priority);
		try {
			t.start();
			t.join();
			Throwable throwable = errorReference.get();
			if (throwable != null) {
				throw new RuntimeException(throwable);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unused")
	private void saveAthletes(List<Athlete> updatedAthletes) {
		var msBefore = System.currentTimeMillis();
		JPAService.runInTransaction(em -> {
			for (Athlete a : updatedAthletes) {
				Athlete oldAthlete = em.find(Athlete.class, a.getId());
				Athlete newAthlete = em.merge(a);
				// dumpAthlete("updated", a);
				// dumpAthlete("old", oldAthlete);
				// dumpAthlete("merged", newAthlete);
			}
			return null;
		});
		logger.info("computeMedalsByCategory saving {}ms", System.currentTimeMillis() - msBefore);
	}

	@SuppressWarnings({ "unchecked", "unused" })
	private void splitResultsByGroups(List<PAthlete> athletes) {
		// create one list per competition group
		for (Group g : GroupRepository.findAll()) {
			String name = g.getName();
			if (name != null) {
				this.reportingBeans.remove(name);
				this.reportingBeans.put(name, new ArrayList<>());
			}
		}

		AthleteSorter.displayOrder(athletes);
		for (Athlete a : athletes) {
			Group group = a.getGroup();
			if (group != null && group.getName() != null) {
				List<Athlete> list = (List<Athlete>) this.reportingBeans.get(group.getName());
				// logger.trace("adding {} to {}", a.getShortName(), group.getName());
				list.add(a);
			}
		}
		logger.debug("updated reporting data");
	}

	private void teamRankings(List<Athlete> athletes, String ageGroupPrefix) {
		clearTeamReportingBeans(ageGroupPrefix);
		doTeamRankings(athletes, ageGroupPrefix, true);
	}

	/**
	 * Iterate over all age prefixes in the age division and accumulate results.
	 *
	 * @param athletes
	 * @param ageGroupPrefix
	 */
	private void teamRankingsForAgeDivision(Championship ad) {
		if (ad == null) {
			return;
		}
		List<String> agePrefixes = AgeGroupRepository.findActiveAndUsedAgeGroupNames(ad);

		String adName = ad.getName();
		adName = adName != null ? adName : "";
		for (String curAGPrefix : agePrefixes) {
			List<Athlete> athletes = AgeGroupRepository.allPAthletesForAgeGroup(curAGPrefix);
			doTeamRankings(athletes, adName, false);
		}

		List<Athlete> sortedAthletes;
		List<Athlete> sortedMen;
		List<Athlete> sortedWomen;

		sortedMen = getOrCreateBean("mTeam" + adName);
		sortedWomen = getOrCreateBean("wTeam" + adName);
		sortedAthletes = getOrCreateBean("mwTeam" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.TOTAL);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.TOTAL);
		AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.TOTAL);

		reportTeams(sortedAthletes, sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mCombined" + adName);
		sortedWomen = getOrCreateBean("wCombined" + adName);
		sortedAthletes = getOrCreateBean("mwCombined" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.SNATCH_CJ_TOTAL);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.SNATCH_CJ_TOTAL);
		AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.SNATCH_CJ_TOTAL);

		reportCombined(sortedAthletes, sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mCustom" + adName);
		sortedWomen = getOrCreateBean("wCustom" + adName);
		sortedAthletes = getOrCreateBean("mwCustom" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.CUSTOM);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.CUSTOM);
		AthleteSorter.teamPointsOrder(sortedAthletes, Ranking.CUSTOM);

		reportCustom(sortedAthletes, sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mTeamSinclair" + adName);
		sortedWomen = getOrCreateBean("wTeamSinclair" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.BW_SINCLAIR);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.BW_SINCLAIR);

		reportSinclair(sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mTeamSMF" + adName);
		sortedWomen = getOrCreateBean("wTeamSMF" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.SMM);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.SMM);

		reportSMF(sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mTeamQPoints" + adName);
		sortedWomen = getOrCreateBean("wTeamQPoints" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.QPOINTS);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.QPOINTS);

		reportQPoints(sortedMen, sortedWomen);

		sortedMen = getOrCreateBean("mTeamQAge" + adName);
		sortedWomen = getOrCreateBean("wTeamQAge" + adName);
		AthleteSorter.teamPointsOrder(sortedMen, Ranking.QAGE);
		AthleteSorter.teamPointsOrder(sortedWomen, Ranking.QAGE);

		reportQAge(sortedMen, sortedWomen);
	}

	public boolean isMasters20kg() {
		return !imwa || Config.getCurrent().featureSwitch("masters20kg");
	}

	// public void setMasters20kg(boolean masters20kg) {
	// this.masters20kg = masters20kg;
	// }

	public LocalDate getCompetitionEndDate() {
		return competitionEndDate;
	}

	public void setCompetitionEndDate(LocalDate competitionEndDate) {
		this.competitionEndDate = competitionEndDate;
	}

	public boolean isImwa() {
		return this.imwa;
	}

	public void setImwa(boolean imwa) {
		this.imwa = imwa;
	}

	public List<Category> computeReferenceCategories(Gender g) {
		// logger.debug("all {}", AgeGroupRepository.findAll().stream()
		// .map(ag -> ag.getCode())
		// .collect(Collectors.joining(", ")));

		var ag = AgeGroupRepository.findFiltered("SR", g, null, null, false, 0, 0);
		List<Category> allCategories;
		if (ag.size() > 0) {
			allCategories = ag.get(0).getAllCategories();
		} else {
			allCategories = null;
		}
		return allCategories;
	}

	public Boolean getDeduct250g() {
		return this.deduct250g;
	}

	public void setDeduct250g(Boolean deduct250g) {
		this.deduct250g = deduct250g;
	}

	public static void recomputeAllAthleteRanks() {
		JPAService.runInTransaction(em -> {
		    // assign ranks to all categories, recompute global
		    List<Athlete> l = AthleteRepository.findAllByGroupAndWeighIn(null, true);
	
		    getCurrent().computeMedalsByCategory(l);
		    getCurrent().doGlobalRankings(l, true);
		    for (Athlete a : l) {
		        em.merge(a);
		    }
		    em.flush();
		    return null;
		});
	}

}
