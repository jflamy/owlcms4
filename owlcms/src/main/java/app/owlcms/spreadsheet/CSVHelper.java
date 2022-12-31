/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.CellProcessorAdaptor;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.IsIncludedIn;
import org.supercsv.cellprocessor.constraint.StrRegEx;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import org.supercsv.util.CsvContext;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;

/**
 * Read registration data in CSV format. The file is expected to contain a header line, as illustrated:
 *
 * <pre>
 * lastName,firstName,gender,club,fullBirthDate,category,group,qualifyingTotal
 * Lamy,Jean-François,M,C-I,1961-12-02,m69,H1,140
 * </pre>
 *
 * (the date is expected in ISO international format YYYY-MM-dd : 4 digit year - month - day). If you are forcing
 * owlcms.useBirthYear=true then your header and format should be as follows.
 *
 * <pre>
 * lastName,firstName,gender,club,yearOfBirth,category,group,qualifyingTotal
 * Lamy,Jean-François,M,C-I,1961,m69,H1,140
 * </pre>
 *
 *
 *
 *
 *
 * registrationCategory and competitionSession must be valid entries in the database.
 *
 * @author Jean-François Lamy
 *
 */
public class CSVHelper {
    @SuppressWarnings("unused")
    private class AsCategory extends CellProcessorAdaptor {

        public AsCategory() {
            super();
        }

        public AsCategory(CellProcessor next) {
            super(next);
        }

        @Override
        public <T> T execute(Object value, CsvContext context) {
            final Category result = CategoryRepository.findByName((String) value);
            return next.execute(result, context);
        }
    }

    @SuppressWarnings("unused")
    private class AsGroup extends CellProcessorAdaptor {

        public AsGroup() {
            super();
        }

        public AsGroup(CellProcessor next) {
            super(next);
        }

        @Override
        public <T> T execute(Object value, CsvContext context) {
            final Group result = GroupRepository.findByName((String) value);
            return next.execute(result, context);
        }

    }

    final private Logger logger = LoggerFactory.getLogger(CSVHelper.class);

    private CellProcessor[] processors;

    public CSVHelper() {
        initProcessors();
    }

    public synchronized List<Athlete> getAllAthletes(InputStream is, Session session) {
        LinkedList<Athlete> allAthletes = new LinkedList<>();

        CsvBeanReader cbr = new CsvBeanReader(new InputStreamReader(is), CsvPreference.EXCEL_PREFERENCE);
        try {
            final String[] header = cbr.getHeader(true);
            Athlete athlete;
            while ((athlete = cbr.read(Athlete.class, header, processors)) != null) {
                logger.debug("adding {}", athlete.toString());
                allAthletes.add(athlete);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                cbr.close();
            } catch (Exception e) {
                // ignored
            }
        }
        return allAthletes;
    }

    public List<Athlete> getAthletes(boolean excludeNotWeighed) {
        // do nothing
        return null;
    }

    /*
     * (non-Javadoc)
     */
    public List<Athlete> getGroupAthletes(InputStream is, Group aGroup, Session session) {
        List<Athlete> groupAthletes = new ArrayList<>();
        for (Athlete curAthlete : getAllAthletes(is, session)) {
            if (aGroup.equals(curAthlete.getGroup())) {
                groupAthletes.add(curAthlete);
            }
        }
        return groupAthletes;
    }

    public void readHeader(InputStream is, Session session) {
        // do nothing
    }

    /**
     * Configure the cell validators and value converters.
     *
     * @param hbnSessionManager to access current database.
     */
    private void initProcessors() {

        List<Group> sessionList = GroupRepository.findAll();
        Set<Object> sessionNameSet = new HashSet<>();
        for (Group s : sessionList) {
            sessionNameSet.add(s.getName());
        }
        List<Category> categoryList = CategoryRepository.findActive();
        Set<Object> categoryNameSet = new HashSet<>();
        for (Category c : categoryList) {
            categoryNameSet.add(c.getName());
        }

        CellProcessor dateparser = new ParseDate("yyyy-MM-dd");
        if (Competition.getCurrent().isUseBirthYear()) {
            dateparser = new StrRegEx("(19|20)[0-9][0-9]", new ParseInt());
        }
        processors = new CellProcessor[] { null, // last name, as is.
                null, // first name, as is.
                new StrRegEx("[mfMF]"), // gender
                null, // club, as is.
                dateparser, // birth date or birth year
                new Optional(new IsIncludedIn(categoryNameSet, new AsCategory())), // registrationCategory
                new IsIncludedIn(sessionNameSet, new AsGroup()), // sessionName
                new Optional(new ParseInt()), // registration total
        };
    }

}
