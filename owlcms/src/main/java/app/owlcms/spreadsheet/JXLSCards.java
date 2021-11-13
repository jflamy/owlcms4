/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;

@SuppressWarnings("serial")
public class JXLSCards extends JXLSWorkbookStreamSource {

    /**
     * Number of rows in a card
     */
    final static int CARD_SIZE = 10;
    /**
     * Number of cards per page
     */
    final static int CARDS_PER_PAGE = 2;

    @SuppressWarnings("unused")
    private final static Logger logger = LoggerFactory.getLogger(JXLSCards.class);

    public JXLSCards(boolean excludeNotWeighed, UI ui) {
        super(ui);
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        return getLocalizedTemplate("/templates/cards/CardTemplate", ".xls", locale);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        if (getGroup() != null) {
            List<Athlete> registrationOrderCopy = AthleteSorter
                    .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(getGroup(), null));
            return registrationOrderCopy;
        } else {
            List<Athlete> registrationOrderCopy = AthleteSorter
                    .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, null));
            return registrationOrderCopy;
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
        setPageBreaks(workbook);
    }

    private void setPageBreaks(Workbook workbook) {
        Sheet sheet = workbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();
        sheet.setAutobreaks(false);
        int increment = CARDS_PER_PAGE * CARD_SIZE + (CARDS_PER_PAGE - 1);

        for (int curRowNum = increment; curRowNum < lastRowNum;) {
            sheet.setRowBreak(curRowNum - 1);
            curRowNum += increment;
        }
    }

}
