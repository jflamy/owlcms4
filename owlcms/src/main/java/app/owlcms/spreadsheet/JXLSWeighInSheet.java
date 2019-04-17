/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSWeighInSheet extends JXLSWorkbookStreamSource {
	
	Logger logger = LoggerFactory.getLogger(JXLSWeighInSheet.class);

    public JXLSWeighInSheet(boolean excludeNotWeighed) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        String templateName = "/templates/weighin/WeighInSheetTemplate_" + locale.getLanguage() + ".xls";
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName);
        if (resourceAsStream == null) {
            throw new IOException("resource not found: " + templateName);} //$NON-NLS-1$
        return resourceAsStream;
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        final Group currentGroup = getGroup();
        if (currentGroup != null) {
            return AthleteSorter.displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup,isExcludeNotWeighed()));
        } else {
            return AthleteSorter.displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null,isExcludeNotWeighed()));
        }
    }

}
