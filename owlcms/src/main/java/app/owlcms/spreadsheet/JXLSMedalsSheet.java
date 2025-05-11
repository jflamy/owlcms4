/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSMedalsSheet extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSMedalsSheet.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	public JXLSMedalsSheet() {
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		// logger.debug("%%% getSortedAthletes() {}",sortedAthletes.stream().map(a->a.getAbbreviatedName()).toList());
		if (this.sortedAthletes != null) {
			return this.sortedAthletes;
		}

		Group group = getGroup();
		TreeMap<String, List<Athlete>> medals = Competition.getCurrent().getMedals(group, true);
		this.sortedAthletes = new ArrayList<>();
		for (Entry<String, List<Athlete>> medalCat : medals.entrySet()) {
			List<Athlete> medalists = medalCat.getValue();
			// logger.debug("medalCat {} {}", medalCat.getKey(), medalCat.getValue().stream().map(a -> a.getAbbreviatedName()).toList());
			if (medalists != null && !medalists.isEmpty()) {
				for (Athlete p : medalists) {
					if (!p.getAgeGroup().getMedals()) {
						continue;
					}
					// logger.trace("Competition.getCurrent().isSnatchCJTotalMedals()
					// {}",Competition.getCurrent().isSnatchCJTotalMedals());
					if (Competition.getCurrent().isSnatchCJTotalMedals()) {
						if (p.getSnatchRank() <= 3) {
							this.sortedAthletes
							        .add(new MAthlete((PAthlete) p, Ranking.SNATCH, p.getSnatchRank(),
							                (double) p.getBestSnatch()));
						}
						if (p.getCleanJerkRank() <= 3) {
							this.sortedAthletes.add(new MAthlete((PAthlete) p, Ranking.CLEANJERK, p.getCleanJerkRank(),
							        (double) p.getBestCleanJerk()));
						}
					}

					if (p.getComputedScoringSystem() == Ranking.TOTAL && p.getTotalRank() <= 3) {
						// logger.debug("+++ adding total {}", p);
						this.sortedAthletes
						        .add(new MAthlete((PAthlete) p, Ranking.TOTAL, p.getTotalRank(), (double) p.getTotal()));
					} else if (p.getCategoryScoreRank() <= 3) {
						// logger.debug("+++ adding score {}", p);
						this.sortedAthletes
						        .add(new MAthlete((PAthlete) p, Ranking.CATEGORY_SCORE, p.getCategoryScoreRank(), (p.getCategoryScore())));
					}
				}
			}
		}

		MAthlete[] array = this.sortedAthletes.toArray(new MAthlete[0]);
		Arrays.sort(array, new MAthlete.MedalComparator());
		this.sortedAthletes = Arrays.asList(array).stream()
		        // .peek(m -> logger.debug("{} {} {} {}", m.getCategory(), m.getAbbreviatedName(), m.getRankingText(), m.getLiftRank()))
		        .filter(m -> m.getLiftRank() >= 1 && m.getLiftRank() <= 3)
		        .collect(Collectors.toList());
		return this.sortedAthletes;
		// @formatter:on
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource# postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
	}

}
