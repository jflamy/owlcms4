/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.UnfinishedCategories;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSWinningSheet extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSWinningSheet.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	private static final boolean ORDER_BY_CATEGORIES = false;
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}
	private boolean resultsByCategory;

	public JXLSWinningSheet() {
		this(true);
	}

	public JXLSWinningSheet(boolean b) {
		this.resultsByCategory = b;
	}

	@Override
	protected void setReportingInfo() {
		super.setReportingInfo();

		Ranking ranking = resolveBestAthleteRanking();
		if (ranking == null) {
			return;
		}

		List<Athlete> sourceAthletes = getBestAthleteSource();
		if (sourceAthletes == null || sourceAthletes.isEmpty()) {
			getReportingBeans().put("mBest", List.of());
			getReportingBeans().put("wBest", List.of());
			return;
		}

		List<Athlete> rankedAthletes = AthleteSorter.resultsOrderCopy(
				sourceAthletes.stream()
						.map(a -> a instanceof PAthlete ? ((PAthlete) a)._getAthlete() : a)
						.collect(Collectors.toMap(
								Athlete::getFullId,
								athlete -> athlete,
								(existing, replacement) -> existing,
								LinkedHashMap::new))
						.values()
						.stream()
						.toList(),
				ranking);

		getReportingBeans().put("mBest", rankedAthletes.stream().filter(a -> a.getGender() == Gender.M).toList());
		getReportingBeans().put("wBest", rankedAthletes.stream().filter(a -> a.getGender() == Gender.F).toList());
		getReportingBeans().put("bestRankingTitle", Ranking.getScoringTitle(ranking));
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		var sa = this.getSortedAthletes();
		// Championship championship = getChampionship();
		if (sa != null) {
			 logger.trace("sortedAthletes.size()={}", sa.size());
			// we are provided with an externally computed list.
			if (this.resultsByCategory) {
				if (!sa.isEmpty()) {
					logger.trace("YYYYYYYYYYYY provided athletes {}", sa.get(0).getClass().getSimpleName());
				}
				Ranking rankingOrder = Ranking.CATEGORY_SCORE;
				AthleteSorter.resultsOrder(sa, rankingOrder, ORDER_BY_CATEGORIES);
				if (!sa.isEmpty()) {
					logger.trace("ZZZZZZZZZZZZ sorted provided athletes {}", sa.get(0).getClass().getSimpleName());
				}
				this.setSortedAthletes(sa);
				return sa;
			} else {
				 logger.trace("YYYYYYYYYYYY unique athletes");
				// we need to expand all the participations before we filter down.
				List<Athlete> allParticipations = Competition.getCurrent().mapToParticipations(sa, this.resultsByCategory);

				// keep the the most specific category from the championship
				List<Athlete> uniqueAthletes = allParticipations.stream()
				        .sorted((a, b) -> {
					        int compare = ObjectUtils.compare(a.getLotNumber(), b.getLotNumber(), true);
					        if (compare != 0) {
						        return compare;
					        }
					        return Category.specificityComparator.compare(a.getCategory(), b.getCategory());
				        })
				        .filter(p -> {
					        // logger.debug("{} {}",p.getLastName(),((PAthlete)p)._getOriginalParticipation().getCategory().getAgeGroup());
					        if (getChampionship() != null && p.getAgeGroup() != null) {
						        return getChampionship().equals(p.getAgeGroup().getChampionship());
					        } else {
						        return true;
					        }
				        })
				        .collect(Collectors.toMap(
				                this::registrationCollapseKey,
				                athlete -> athlete,
				                (existing, replacement) -> existing))
				        .values()
				        .stream()
				        .collect(Collectors.toList());

				// re-sort the athletes
				sa = new ArrayList<>(uniqueAthletes);
				AthleteSorter.resultsOrder(sa, rankingOrder(), ORDER_BY_CATEGORIES);
				logger.debug("registration getSortedAthletes {}", sa.size());
				this.setSortedAthletes(sa);
				return sa;
			}
		}
		logger.debug("XXXXXXXXXXXXXXXXXXXX  no sorted athletes");
		final Group currentGroup = getGroup();
		Category currentCategory = getCategory();
		Championship currentAgeDivision = getChampionship();
		String currentAgeGroupPrefix = getAgeGroupPrefix();
		List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(currentGroup);

		// get all the PAthletes for the current group - athletes show as many times as
		// they have participations.
		List<Athlete> pAthletes = Competition.getCurrent().mapToParticipations(rankedAthletes, this.resultsByCategory);

		// unfinished categories need to be computed using all relevant athletes, including not weighed-in yet
		@SuppressWarnings("unchecked")
		UnfinishedCategories unfinishedCategories = AthleteRepository.allUnfinishedCategories();
		logger.debug("JXLSWinningSheet unfinished categories {}", unfinishedCategories.toString());

		// @formatter:off
        List<Athlete> athletes = AthleteSorter.resultsOrderCopy(pAthletes, rankingOrder(), false).stream()
                .filter(a -> {
                    Double bw;
                    return a.getCategory() != null && (bw = a.getBodyWeight()) != null && bw > 0.01;
                })
                .filter(a -> (
                        currentGroup != null
                            ? (a.getGroup() != null ?
                                    a.getGroup().equals(currentGroup)
                                    : false)
                            : true))
                .filter(a -> (
                        currentCategory != null
                            ? (a.getCategory() != null ?
                                    a.getCategory().getCode().equals(currentCategory.getCode())
                                    : false)
                            : true))
                .filter(a -> {
                    AgeGroup ageGroup = a.getAgeGroup();
                    Championship ageDivision2 = ageGroup != null ? ageGroup.getChampionship() : null;
                    return (
                        currentAgeDivision != null
                            ? (ageDivision2 != null ?
                                    currentAgeDivision.equals(ageDivision2)
                                    : false)
                            : true);
                    })
                .filter(a -> {
                    AgeGroup ageGroup = a.getAgeGroup();
                    String ageGroupPrefix2 = ageGroup != null ? ageGroup.getCode() : null;
                    return (
                        currentAgeGroupPrefix != null
                            ? (ageGroupPrefix2 != null ?
                                    currentAgeGroupPrefix.equals(ageGroupPrefix2)
                                    : false)
                            : true);
				})
				.map(a -> {
					if (a.getCategory() != null && unfinishedCategories.contains(a.getCategory())) {
						a.setCategoryFinished(false);
					} else {
						a.setCategoryFinished(true);
					}
					return a;
				})
                //.peek(a -> logger.debug("   {}",a))
                .collect(Collectors.toList());
        return athletes;
        // @formatter:on
	}

	@Override
	protected Object createRecordsBean() {
		Category category = getCategory();
		if (category == null) {
			return null;
		}

		return new LazyRecordEventList(() -> fetchCategoryRecords(category));
	}

	private List<RecordEvent> fetchCategoryRecords(Category category) {
		logger.warn("*** lazily fetching records for winning sheet category {}", category);
		List<RecordEvent> records = normalizeRecordEventsForTemplate(RecordRepository.findProvisionalRecordsForCategory(category));
		logger.warn("{} records found for winning sheet category {}", records != null ? records.size() : 0, category);
		return records;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource# postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
		String c = getChampionship() != null ? getChampionship().getName() : null;
		String ag = getAgeGroupPrefix();
		Header header = workbook.getSheetAt(0).getHeader();

		// header.setLeft(Competition.getCurrent().getCompetitionName());
		if (c != null && ag != null) {
			header.setCenter(c + "\u2013" + ag);
		} else if (c != null) {
			header.setCenter(c);
		} else if (ag != null) {
			header.setCenter(ag);
		} else {
			header.setCenter("");
		}

		createStandardFooter(workbook);
	}

	private Ranking rankingOrder() {
		return Ranking.CUSTOM;
	}

	private Object registrationCollapseKey(Athlete athlete) {
		Long athleteId = athlete.getId();
		if (athleteId != null) {
			return athleteId;
		}

		Athlete sourceAthlete = athlete instanceof PAthlete ? ((PAthlete) athlete)._getAthlete() : athlete;
		Long sourceAthleteId = sourceAthlete != null ? sourceAthlete.getId() : null;
		if (sourceAthleteId != null) {
			return sourceAthleteId;
		}

		return sourceAthlete != null ? sourceAthlete : athlete;
	}

	@SuppressWarnings("unchecked")
	private List<Athlete> getBestAthleteSource() {
		Object athletes = getReportingBeans().get("athletes");
		if (athletes instanceof List<?>) {
			return ((List<?>) athletes).stream()
					.filter(Athlete.class::isInstance)
					.map(Athlete.class::cast)
					.toList();
		}
		return computeSortedAthletes();
	}

	private Ranking resolveBestAthleteRanking() {
		if (getChampionship() != null && getChampionship().getBestAthleteScoringSystem() != null) {
			return getChampionship().getBestAthleteScoringSystem();
		}
		if (getBestLifterScoringSystem() != null) {
			return getBestLifterScoringSystem();
		}
		return Championship.of(null).getBestAthleteScoringSystem();
	}

}
