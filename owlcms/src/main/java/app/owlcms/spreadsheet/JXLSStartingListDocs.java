/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSStartingListDocs extends JXLSWorkbookStreamSource {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");

    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSStartingListDocs.class);
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

    public JXLSStartingListDocs() {
        super();
        this.setExcludeNotWeighed(false);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        List<Athlete> registrationOrderCopy = AthleteSorter.registrationOrderCopy(sortedAthletes);
        return registrationOrderCopy;
    }
}
