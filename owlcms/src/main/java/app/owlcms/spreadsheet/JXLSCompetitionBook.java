/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.AthleteSorter.Ranking;
import app.owlcms.i18n.Messages;
import app.owlcms.init.OwlcmsSession;
import net.sf.jxls.transformer.XLSTransformer;

/**
 * Result sheet, with team rankings
 *
 * @author jflamy
 *
 */
public class JXLSCompetitionBook extends JXLSWorkbookStreamSource {

    private static final long serialVersionUID = 1L;
    // final private static int TEAMSHEET_FIRST_ROW = 5;

    @SuppressWarnings("unused")
    private Logger logger = LoggerFactory.getLogger(JXLSCompetitionBook.class);

    public JXLSCompetitionBook() {
        // by default, we exclude athletes who did not weigh in.
        super();
    }

    public JXLSCompetitionBook(boolean excludeNotWeighed) {
        super();
    }

    @Override
    public InputStream getTemplate(Locale locale) throws IOException {
    	String packageTemplateFileName = "/templates/competitionBook/CompetitionBook_Total_" + locale.getLanguage() + ".xls";
    	InputStream stream = this.getClass().getResourceAsStream(packageTemplateFileName);
        // can't happen unless system is misconfigured.
        if (stream == null) throw new IOException("resource not found: " + packageTemplateFileName); //$NON-NLS-1$
        else return stream;
    }

    @Override
    protected void setReportingInfo() {
    	super.setReportingInfo();
        HashMap<String, Object> reportingBeans = getReportingBeans();

        List<Athlete> athletes = AthleteRepository.findAllByGroupAndWeighIn(null,true);
        if (athletes.isEmpty()) {
            // prevent outputting silliness.
            throw new RuntimeException("No athletes."); //$NON-NLS-1$
        }
        // extract club lists
        TreeSet<String> clubs = new TreeSet<String>();
        for (Athlete curAthlete : athletes) {
            clubs.add(curAthlete.getClub());
        }
        reportingBeans.put("clubs", clubs);

        List<Athlete> sortedAthletes;
        List<Athlete> sortedMen = null;
        List<Athlete> sortedWomen = null;

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SNATCH);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.SNATCH);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSn", sortedMen);
        reportingBeans.put("wSn", sortedWomen);

        // only needed once
        reportingBeans.put("nbMen", sortedMen.size());
        reportingBeans.put("nbWomen", sortedWomen.size());
        reportingBeans.put("nbAthletes", sortedAthletes.size());
        reportingBeans.put("nbClubs", clubs.size());
        if (sortedMen.size() > 0) {
            reportingBeans.put("mClubs", clubs);
        } else {
            reportingBeans.put("mClubs", new ArrayList<String>());
        }
        if (sortedWomen.size() > 0) {
            reportingBeans.put("wClubs", clubs);
        } else {
            reportingBeans.put("wClubs", new ArrayList<String>());
        }

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CLEANJERK);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CLEANJERK);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCJ", sortedMen);
        reportingBeans.put("wCJ", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.TOTAL);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.TOTAL);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mTot", sortedMen);
        reportingBeans.put("wTot", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.SINCLAIR);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.SINCLAIR);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mSinclair", sortedMen);
        reportingBeans.put("wSinclair", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.ROBI);
        AthleteSorter.assignSinclairRanksAndPoints(sortedAthletes, Ranking.ROBI);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mRobi", sortedMen);
        reportingBeans.put("wRobi", sortedWomen);

        sortedAthletes = AthleteSorter.resultsOrderCopy(athletes, Ranking.CUSTOM);
        AthleteSorter.assignCategoryRanks(sortedAthletes, Ranking.CUSTOM);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCus", sortedMen);
        reportingBeans.put("wCus", sortedWomen);

        // team-oriented rankings. These put all the athletes from the same team together,
        // sorted from best to worst, so that the top "n" can be given points
        sortedAthletes = AthleteSorter.teamRankingOrderCopy(athletes, Ranking.CUSTOM);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCustom", sortedMen);
        reportingBeans.put("wCustom", sortedWomen);

        sortedAthletes = AthleteSorter.teamRankingOrderCopy(athletes, Ranking.COMBINED);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mCombined", sortedMen);
        reportingBeans.put("wCombined", sortedWomen);
        reportingBeans.put("mwCombined", sortedAthletes);

        AthleteSorter.teamRankingOrder(sortedAthletes, Ranking.TOTAL);
        sortedMen = new ArrayList<Athlete>(sortedAthletes.size());
        sortedWomen = new ArrayList<Athlete>(sortedAthletes.size());
        splitByGender(sortedAthletes, sortedMen, sortedWomen);
        reportingBeans.put("mTeam", sortedMen);
        reportingBeans.put("wTeam", sortedWomen);
        reportingBeans.put("mwTeam", sortedAthletes);
    }

    @Override
    protected void configureTransformer(XLSTransformer transformer) {
        super.configureTransformer(transformer);
        transformer.markAsFixedSizeCollection("clubs");
        transformer.markAsFixedSizeCollection("mTeam");
        transformer.markAsFixedSizeCollection("wTeam");
        transformer.markAsFixedSizeCollection("mwTeam");
        transformer.markAsFixedSizeCollection("mCombined");
        transformer.markAsFixedSizeCollection("wCombined");
        transformer.markAsFixedSizeCollection("mCustom");
        transformer.markAsFixedSizeCollection("wCustom");
    }

    /*
     * team result sheets need columns hidden, print area fixed
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        super.postProcess(workbook);
        @SuppressWarnings("unchecked")
        int nbClubs = ((Set<String>) getReportingBeans().get("clubs")).size();

        setTeamSheetPrintArea(workbook, "MT", nbClubs);
        setTeamSheetPrintArea(workbook, "WT", nbClubs);
        setTeamSheetPrintArea(workbook, "MWT", nbClubs);

        setTeamSheetPrintArea(workbook, "MXT", nbClubs);
        setTeamSheetPrintArea(workbook, "WXT", nbClubs);

        setTeamSheetPrintArea(workbook, "MCT", nbClubs);
        setTeamSheetPrintArea(workbook, "WCT", nbClubs);
        setTeamSheetPrintArea(workbook, "MWCT", nbClubs);

        translateSheets(workbook);
        workbook.setForceFormulaRecalculation(true);

    }

    private void setTeamSheetPrintArea(Workbook workbook, String sheetName, int nbClubs) {
        // int sheetIndex = workbook.getSheetIndex(sheetName);
        // if (sheetIndex >= 0) {
        // workbook.setPrintArea(sheetIndex, 0, 4, TEAMSHEET_FIRST_ROW, TEAMSHEET_FIRST_ROW+nbClubs);
        // }
    }

    private void translateSheets(Workbook workbook) {
        int nbSheets = workbook.getNumberOfSheets();
        for (int sheetIndex = 0; sheetIndex < nbSheets; sheetIndex++) {
            Sheet curSheet = workbook.getSheetAt(sheetIndex);
            String sheetName = curSheet.getSheetName();
            workbook.setSheetName(sheetIndex, Messages.getString("CompetitionBook." + sheetName, OwlcmsSession.getLocale()));

            String leftHeader = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_LeftHeader",
                    OwlcmsSession.getLocale());
            if (leftHeader != null)
                curSheet.getHeader().setLeft(leftHeader);
            String centerHeader = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_CenterHeader",
                    OwlcmsSession.getLocale());
            if (centerHeader != null)
                curSheet.getHeader().setCenter(centerHeader);
            String rightHeader = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_RightHeader",
                    OwlcmsSession.getLocale());
            if (rightHeader != null)
                curSheet.getHeader().setRight(rightHeader);

            String leftFooter = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_LeftFooter",
                    OwlcmsSession.getLocale());
            if (leftFooter != null)
                curSheet.getFooter().setLeft(leftFooter);
            String centerFooter = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_CenterFooter",
                    OwlcmsSession.getLocale());
            if (centerFooter != null)
                curSheet.getFooter().setCenter(centerFooter);
            String rightFooter = Messages.getStringNullIfMissing("CompetitionBook." + sheetName + "_RightFooter",
                    OwlcmsSession.getLocale());
            if (rightFooter != null)
                curSheet.getFooter().setRight(rightFooter);
        }
    }

    public static void splitByGender(List<Athlete> sortedAthletes,
            List<Athlete> sortedMen, List<Athlete> sortedWomen) {
        for (Athlete l : sortedAthletes) {
            Gender gender = l.getGender();
            if (Gender.M == gender) {
                sortedMen.add(l);
            } else if (Gender.F == gender) {
                sortedWomen.add(l);
            } else {
            	throw new RuntimeException("gender is "+gender);
            }
        }
    }

	@Override
	protected List<Athlete> getSortedAthletes() {
		// not used (setReportingInfo does all the work)
		return null;
	}
}
