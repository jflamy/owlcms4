/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package app.owlcms.spreadsheet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSResultSheet extends JXLSWorkbookStreamSource {

    Logger logger = LoggerFactory.getLogger(JXLSResultSheet.class);

    private Competition competition;

    public JXLSResultSheet() {
        super(true);
    }

    public JXLSResultSheet(boolean excludeNotWeighed) {
        super(excludeNotWeighed);
    }


    @Override
    protected void init() {
        //System.err.println("JXLSResultSheet init");
        super.init();
        competition = Competition.getCurrent();
        getReportingBeans().put("competition", competition);
        getReportingBeans().put("session", getCurrentCompetitionSession());
        //System.err.println("masters = "+getReportingBeans().get("masters"));
    }



	@Override
    public InputStream getTemplate() throws IOException {
        String protocolTemplateFileName = competition.getProtocolFileName();
        if (protocolTemplateFileName != null) {
            File templateFile = new File(protocolTemplateFileName);
            if (templateFile.exists()) {
                FileInputStream resourceAsStream = new FileInputStream(templateFile);
                return resourceAsStream;
            }
            // can't happen unless system is misconfigured.
            throw new IOException("resource not found: " + protocolTemplateFileName); //$NON-NLS-1$
        } else {
            throw new RuntimeException("Protocol sheet template not defined.");
        }
    }

    @Override
    protected void getSortedAthletes() {
        final Group currentGroup = getCurrentCompetitionSession();
        if (currentGroup != null) {
            // AthleteContainer is used to ensure filtering to current group
            this.athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup,isExcludeNotWeighed()),Ranking.TOTAL);
        } else {
            this.athletes = AthleteSorter.resultsOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null,isExcludeNotWeighed()),Ranking.TOTAL);
        }
        AthleteSorter.assignCategoryRanks(athletes, Ranking.TOTAL);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        final Group currentCompetitionSession = getCurrentCompetitionSession();
        if (currentCompetitionSession == null) {
            zapCellPair(workbook, 3, 9);
        }
    }

}
