/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteStreams;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.jpa.JPAService;
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

    private byte[] protocolTemplate;

    public JXLSResultSheet() {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        Competition current = Competition.getCurrent();
        protocolTemplate = current.getProtocolTemplate();
        if (protocolTemplate == null) {
            protocolTemplate = loadDefaultProtocolTemplate(locale, current);
        }
        InputStream stream = new ByteArrayInputStream(protocolTemplate);
        return stream;
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        final Group currentGroup = getGroup();
        List<Athlete> athletes;
        if (currentGroup != null) {
            athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup, true),
                    Ranking.TOTAL);
        } else {
            athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, true),
                    Ranking.TOTAL);
        }
        AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
        return athletes;
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

    private byte[] loadDefaultProtocolTemplate(Locale locale, Competition current) {
        JPAService.runInTransaction((em) -> {
            String protocolTemplateFileName = "/templates/protocol/ProtocolSheetTemplate_" + locale.getLanguage()
                    + ".xls";
            InputStream stream = this.getClass().getResourceAsStream(protocolTemplateFileName);
            try {
                protocolTemplate = ByteStreams.toByteArray(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            current.setProtocolTemplate(protocolTemplate);
            Competition merge = em.merge(current);
            Competition.setCurrent(merge);
            return merge;
        });
        return protocolTemplate;
    }

}
