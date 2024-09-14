/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.poi.ss.usermodel.Header;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.group.Group;
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
	public List<Athlete> getSortedAthletes() {
		if (this.sortedAthletes != null) {
			// logger.debug("YYYYYYYYYYYY sorted athletes {}",LoggerUtils.whereFrom());
			// we are provided with an externally computed list.
			if (this.resultsByCategory) {
				// we to complete all the athletes with their participations, before filtering.
				// logger.debug("YYYYYYYYYYYY category athletes");
				this.sortedAthletes = mapToParticipations(this.sortedAthletes);

				// if there are age group-specific scoring systems, this can be different than the total
				// usual ordering.
				// logger.debug("YYYYYYYYYYYY ranking order {}", rankingOrder());
				logger.debug("eligible getSortedAthletes {}", this.sortedAthletes.size());
				AthleteSorter.resultsOrder(this.sortedAthletes, rankingOrder(), false);
				// logger.debug("YYYYYYYYYYYY eligible getSortedAthletes {}", this.sortedAthletes.size());
				return this.sortedAthletes;
			} else {
				// logger.debug("YYYYYYYYYYYY unique athletes");
				// we need to expand all the participations before we filter down.
				List<Athlete> allParticipations = mapToParticipations(this.sortedAthletes);

				// keep the the most specific category from the championship
				List<Athlete> uniqueAthletes = allParticipations.stream()
				        .sorted((a, b) -> {
					        int compare = ObjectUtils.compare(a.getLotNumber(), b.getLotNumber(), true);
					        if (compare != 0)
						        return compare;
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
				                Athlete::getLotNumber,
				                athlete -> athlete,
				                (existing, replacement) -> existing))
				        .values()
				        .stream()
				        .collect(Collectors.toList());

				// re-sort the athletes
				this.sortedAthletes = new ArrayList<>(uniqueAthletes);
				AthleteSorter.resultsOrder(this.sortedAthletes, rankingOrder(), false);
				logger.debug("registration getSortedAthletes {}", this.sortedAthletes.size());
				return this.sortedAthletes;
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
		List<Athlete> pAthletes = mapToParticipations(rankedAthletes);

		// unfinished categories need to be computed using all relevant athletes, including not weighed-in yet
		@SuppressWarnings("unchecked")
		Set<String> unfinishedCategories = AthleteRepository.allUnfinishedCategories();
		logger.debug("JXLSWinningSheet unfinished categories {}", unfinishedCategories);

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
					if (a.getCategory() != null && unfinishedCategories.contains(a.getCategory().getCode())) {
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

	private Ranking rankingOrder() {
		return Ranking.CUSTOM;
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

	public List<Athlete> mapToParticipations(List<Athlete> rankedAthletes) {
		List<Athlete> pAthletes;
		if (this.resultsByCategory) {
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

}
