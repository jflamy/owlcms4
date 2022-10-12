/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.records.RecordEvent;
import app.owlcms.data.records.RecordRepository;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSExportRecords extends JXLSWorkbookStreamSource {

    Logger logger = (Logger) LoggerFactory.getLogger(JXLSExportRecords.class);

    public JXLSExportRecords(Group group, boolean excludeNotWeighed, UI ui) {
        super();
    }

    public JXLSExportRecords(UI ui) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
        return getLocalizedTemplate("/templates/records/exportRecords", ".xls", locale);
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        HashMap<String, Object> reportingBeans = getReportingBeans();

        List<Athlete> athletes = AthleteSorter
                .registrationOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null, isExcludeNotWeighed()));
        if (athletes.isEmpty()) {
            // prevent outputting silliness.
            throw new RuntimeException("");
        } else {
            logger.debug("{} athletes", athletes.size());
        }

        List<RecordEvent> records = RecordRepository.findFiltered(null, null, null, null, true);
        records.sort(
                Comparator.comparing(RecordEvent::getRecordFederation)
                        .thenComparing(RecordEvent::getRecordName)
                        .thenComparing(RecordEvent::getAgeGrp)
                        .thenComparing(RecordEvent::getAgeGrpLower)
                        .thenComparing(RecordEvent::getAgeGrpUpper)
                        .thenComparing(RecordEvent::getBwCatUpper)
                        .thenComparing((r) -> r.getRecordLift().ordinal())
        );
        reportingBeans.put("records", records);
        return athletes;
    }

}
