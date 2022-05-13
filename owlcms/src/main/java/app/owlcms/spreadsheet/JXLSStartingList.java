/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSStartingList extends JXLSWorkbookStreamSource {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");

    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSStartingList.class);
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    public JXLSStartingList() {
        super();
        this.setExcludeNotWeighed(false);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        return AthleteSorter.registrationOrderCopy(AthleteRepository.findAll()).stream()
                .filter(a -> a.getGroup() != null)
                .map(a -> {
                    if (a.getTeam() == null) {
                        a.setTeam("-");
                    }
                    return a;
                }).collect(Collectors.toList());
    }

}
