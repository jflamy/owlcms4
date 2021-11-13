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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSJurySheet extends JXLSWorkbookStreamSource {

    Logger logger = LoggerFactory.getLogger(JXLSJurySheet.class);

    public JXLSJurySheet(UI ui) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        return getLocalizedTemplate("/templates/jury/JurySheetTemplate", ".xls", locale);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
     * configureTransformer(net.sf.jxls.transformer.XLSTransformer )
     */
    @Override
    protected void configureTransformer(XLSTransformer transformer) {
        transformer.markAsFixedSizeCollection("athletes");
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        return AthleteSorter
                .displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(getGroup(), isExcludeNotWeighed()));
    }

}
