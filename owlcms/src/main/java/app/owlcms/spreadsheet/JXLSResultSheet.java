/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
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

    public JXLSResultSheet() {
        super();
    }

	@Override
    public InputStream getTemplate(Locale locale) throws IOException {
        String protocolTemplateFileName = Competition.getCurrent().getProtocolFileName();
        
        protocolTemplateFileName = "/templates/protocol/ProtocolSheetTemplate_" + locale.getLanguage() + ".xls";
//        if (protocolTemplateFileName != null) {
//            File templateFile = new File(protocolTemplateFileName);
//            if (templateFile.exists()) {
//                FileInputStream resourceAsStream = new FileInputStream(templateFile);
//                return resourceAsStream;
//            }
        	InputStream stream = this.getClass().getResourceAsStream(protocolTemplateFileName);
            // can't happen unless system is misconfigured.
            if (stream == null) throw new IOException("resource not found: " + protocolTemplateFileName); //$NON-NLS-1$
            else return stream;
//        } 
//        else {
//            throw new RuntimeException("Protocol sheet template not defined.");
//        }
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        final Group currentGroup = getGroup();
        List<Athlete> athletes;
		if (currentGroup != null) {
            athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup,true),Ranking.TOTAL);
        } else {
            athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null,true),Ranking.TOTAL);
        }
        AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
        return athletes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        final Group currentCompetitionSession = getGroup();
        if (currentCompetitionSession == null) {
            zapCellPair(workbook, 3, 9);
        }
    }

}
