/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
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

    private AgeDivision ageDivision;
    private String ageGroupPrefix;

    public JXLSResultSheet() {
        super();
    }

    @Override
    protected List<Athlete> getSortedAthletes() {
        final Group currentGroup = getGroup();
        Category currentCategory = getCategory();
        AgeDivision currentAgeDivision = getAgeDivision();
        String currentAgeGroupPrefix = getAgeGroupPrefix();
        List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(currentGroup);

        List<Athlete> athletes = AthleteSorter.displayOrderCopy(rankedAthletes).stream()
                .filter(a -> (
                        currentGroup != null 
                            ? (a.getGroup() != null ?
                                    a.getGroup().equals(currentGroup) 
                                    : false)
                            : true))
                .filter(a -> (
                        currentCategory != null
                            ? (a.getCategory() != null ?
                                    a.getCategory().getCode().equals(currentCategory.getCode())
                                    : false)
                            : true))
                .filter(a -> {
                    AgeDivision ageDivision2 = a.getAgeGroup().getAgeDivision();
                    return (
                        currentAgeDivision != null 
                            ? (ageDivision2 != null ?
                                    currentAgeDivision.equals(ageDivision2) 
                                    : false)
                            : true);
                    })
                .filter(a -> {
                    String ageGroupPrefix2 = a.getAgeGroup().getCode();
                    return (
                        currentAgeGroupPrefix != null 
                            ? (ageGroupPrefix2 != null ?
                                    currentAgeGroupPrefix.equals(ageGroupPrefix2) 
                                    : false)
                            : true);
                    })
                .collect(Collectors.toList());
        return athletes;

    }

    public String getAgeGroupPrefix() {
        return ageGroupPrefix;
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

    public void setAgeDivision(AgeDivision ageDivision) {
        this.ageDivision = ageDivision;
    }

    /**
     * @return the ageDivision
     */
    private AgeDivision getAgeDivision() {
        return ageDivision;
    }

    /**
     * @param ageGroupPrefix the ageGroupPrefix to set
     */
    public void setAgeGroupPrefix(String ageGroupPrefix) {
        this.ageGroupPrefix = ageGroupPrefix;
    }

}
