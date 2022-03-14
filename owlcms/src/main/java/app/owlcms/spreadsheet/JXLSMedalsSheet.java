/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
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

    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSMedalsSheet.class);
    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    public JXLSMedalsSheet() {
        super();
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        if (sortedAthletes != null) {
            return sortedAthletes;
        }

        Group group = getGroup();
        TreeMap<Category, TreeSet<Athlete>> medals = Competition.getCurrent().getMedals(group);
        sortedAthletes = new ArrayList<>();
        for (Entry<Category, TreeSet<Athlete>> medalCat : medals.entrySet()) {
            TreeSet<Athlete> medalists = medalCat.getValue();
            if (medalists != null && !medalists.isEmpty()) {
                for (Athlete p : medalists) {
                    //logger.trace("Competition.getCurrent().isSnatchCJTotalMedals() {}",Competition.getCurrent().isSnatchCJTotalMedals());
                    if (Competition.getCurrent().isSnatchCJTotalMedals()) {
                        sortedAthletes
                                .add(new MAthlete((PAthlete) p, Ranking.SNATCH, p.getSnatchRank(), p.getBestSnatch()));
                        sortedAthletes.add(new MAthlete((PAthlete) p, Ranking.CLEANJERK, p.getCleanJerkRank(),
                                p.getBestCleanJerk()));
                    }
                    sortedAthletes.add(new MAthlete((PAthlete) p, Ranking.TOTAL, p.getTotalRank(), p.getTotal()));
                }
            }
        }

        MAthlete[] array = sortedAthletes.toArray(new MAthlete[0]);
        Arrays.sort(array, new MAthlete.MedalComparator());
        sortedAthletes = Arrays.asList(array).stream().filter(m -> m.getLiftRank() >= 1 && m.getLiftRank() <= 3)
                .collect(Collectors.toList());
        return sortedAthletes;
        // @formatter:on
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
     * postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
    }

}
