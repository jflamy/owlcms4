/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSResultSheet extends JXLSWorkbookStreamSource {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSResultSheet.class);
    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

//    private byte[] protocolTemplate;

    public JXLSResultSheet(UI ui) {
        super(ui);
    }

    @Override
    public InputStream getTemplate(Locale locale) throws Exception {
        Competition current = Competition.getCurrent();
        logger.trace("current={}", current);
        String protocolTemplateFileName = current.getProtocolFileName();
        logger.trace("protocolTemplateFileName={}", protocolTemplateFileName);

        int stripIndex = protocolTemplateFileName.indexOf("_");
        if (stripIndex > 0) {
            protocolTemplateFileName = protocolTemplateFileName.substring(0, stripIndex);
        }

        stripIndex = protocolTemplateFileName.indexOf(".xls");
        if (stripIndex > 0) {
            protocolTemplateFileName = protocolTemplateFileName.substring(0, stripIndex);
        }

        return getLocalizedTemplate("/templates/protocol/" + protocolTemplateFileName, ".xls", locale);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        final Group currentGroup =  getGroup();
        List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(currentGroup);

        if (currentGroup != null) {
            List<Athlete> currentGroupAthletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
                    .filter(a -> a.getGroup() != null ? a.getGroup().equals(currentGroup) : false)
                    .collect(Collectors.toList());
            return currentGroupAthletes;
        } else {
            return rankedAthletes;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
     * postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        final Group currentCompetitionSession = getGroup();
        if (currentCompetitionSession == null) {
            zapCellPair(workbook, 3, 9);
        }
    }

}
