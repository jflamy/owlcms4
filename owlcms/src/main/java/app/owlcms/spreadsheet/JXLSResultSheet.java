/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
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

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSResultSheet.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	private boolean resultsByCategory;

	public JXLSResultSheet() {
		this(true);
	}

	public JXLSResultSheet(boolean b) {
		this.resultsByCategory = b;
	}

	@Override
	protected List<Athlete> getSortedAthletes() {
		if (sortedAthletes != null) {
			return sortedAthletes;
		}
		final Group currentGroup = getGroup();
		Category currentCategory = getCategory();
		AgeDivision currentAgeDivision = getAgeDivision();
		String currentAgeGroupPrefix = getAgeGroupPrefix();
		List<Athlete> rankedAthletes = AthleteSorter.assignCategoryRanks(currentGroup);

		// get all the PAthletes for the current group - athletes show as many times as
		// they have participations.
		List<Athlete> pAthletes;
		if (resultsByCategory) {
			pAthletes = new ArrayList<>(rankedAthletes.size() * 2);
			for (Athlete a : rankedAthletes) {
				for (Participation p : a.getParticipations()) {
					pAthletes.add(new PAthlete(p));
				}
			}
		} else {
			pAthletes = rankedAthletes;
		}

		// @formatter:off
        List<Athlete> athletes = AthleteSorter.displayOrderCopy(pAthletes).stream()
                .filter(a -> {
                    Double bw;
                    return a.getCategory() != null && (bw = a.getBodyWeight()) != null && bw > 0.01;
                })
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
                    AgeGroup ageGroup = a.getAgeGroup();
                    AgeDivision ageDivision2 = ageGroup != null ? ageGroup.getAgeDivision() : null;
                    return (
                        currentAgeDivision != null
                            ? (ageDivision2 != null ?
                                    currentAgeDivision.equals(ageDivision2)
                                    : false)
                            : true);
                    })
                .filter(a -> {
                    AgeGroup ageGroup = a.getAgeGroup();
                    String ageGroupPrefix2 = ageGroup != null ? ageGroup.getCode() : null;
                    return (
                        currentAgeGroupPrefix != null
                            ? (ageGroupPrefix2 != null ?
                                    currentAgeGroupPrefix.equals(ageGroupPrefix2)
                                    : false)
                            : true);
                    })
                .collect(Collectors.toList());
        return athletes;
        // @formatter:on
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
	 * postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
		final Group currentCompetitionSession = getGroup();
		if (currentCompetitionSession == null
		        && !Competition.getCurrent().getProtocolTemplateFileName().contains("USAW")) {
			zapCellPair(workbook, 3, 9);
		}
	}

}
