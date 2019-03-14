/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSStartingList extends JXLSWorkbookStreamSource {

    public JXLSStartingList() {
        super(false);
    }

    public JXLSStartingList(boolean excludeNotWeighed) {
        super(excludeNotWeighed);
    }

    Logger logger = LoggerFactory.getLogger(JXLSStartingList.class);

    @Override
    protected void init() {
        super.init();

        Competition competition = Competition.getCurrent();
        getReportingBeans().put("competition", competition);
    }

    @Override
    public InputStream getTemplate() throws IOException {
        String templateName = "/StartSheetTemplate_" + UI.getCurrent().getLocale().getLanguage() + ".xls";
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName);
        if (resourceAsStream == null) {
            throw new IOException("resource not found: " + templateName);} //$NON-NLS-1$
        return resourceAsStream;
    }

    @Override
    protected void getSortedAthletes() {
        this.athletes = AthleteSorter.registrationOrderCopy(AthleteRepository.findAll());
    }

}
