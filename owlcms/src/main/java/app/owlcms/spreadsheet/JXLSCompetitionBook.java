/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.competition.Competition;
import app.owlcms.i18n.Translator;
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
	private Championship ageDivision;
	private String ageGroupPrefix;
	@SuppressWarnings("unused")
	private Logger logger = LoggerFactory.getLogger(JXLSCompetitionBook.class);
	private boolean isIncludeUnfinished;

	public JXLSCompetitionBook(boolean excludeNotWeighed, UI ui) {
	}

	public JXLSCompetitionBook(UI ui) {
	}

	/**
	 * @return the ageDivision
	 */
	@Override
	public Championship getChampionship() {
		return this.ageDivision;
	}

	@Override
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		// not used (setReportingInfo does all the work)
		return null;
	}

	/**
	 * @param ageDivision the ageDivision to set
	 */
	@Override
	public void setChampionship(Championship ageDivision) {
		// logger.debug("set ad {} \\n{}",ageDivision,LoggerUtils.stackTrace());
		this.ageDivision = ageDivision;
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
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
		transformer.markAsFixedSizeCollection("mwCombined");
		transformer.markAsFixedSizeCollection("mCustom");
		transformer.markAsFixedSizeCollection("wCustom");
		transformer.markAsFixedSizeCollection("mwCustom");
	}

	/*
	 * team result sheets need columns hidden, print area fixed
	 *
	 * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
	 * postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
		super.postProcess(workbook);
		translateSheets(workbook);
		workbook.setForceFormulaRecalculation(true);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected void setReportingInfo() {
		Competition competition = Competition.getCurrent();
		competition.computeReportingInfo(getAgeGroupPrefix(), getChampionship());

		super.setReportingInfo();
		Object records = super.getReportingBeans().get("records");
		HashMap<String, Object> reportingBeans = competition.getReportingBeans();

		// remove athletes from incomplete categories
		if (!isIncludeUnfinished()) {
			for (String k : reportingBeans.keySet()) {
				logger.debug("bean {}",k);
				Object bean = reportingBeans.get(k);
				if (bean instanceof List && ((List) bean).size() > 0 && ((List) bean).get(0) instanceof Athlete) {
					logger.debug("cleaning up {}", k);
					List<Athlete> bean2 = (List<Athlete>) bean;
					Set<String> unfinishedCategories = AthleteRepository.unfinishedCategories(bean2);
					bean2 = bean2.stream().filter(a -> !unfinishedCategories.contains(a.getCategoryCode())).toList();
					reportingBeans.put(k, bean2);
				}
			}
		}

		reportingBeans.put("records", records);

		Ranking overallScoringSystem = this.getBestLifterScoringSystem();
		// make available to the Athlete class in this Thread (and subThreads).
		JXLSWorkbookStreamSource.setBestLifterRankingTL(overallScoringSystem);
		logger.debug("setBestLifterRankingTL {} {}",overallScoringSystem, overallScoringSystem.getMReportingName());
		reportingBeans.put("bestRankingTitle",Ranking.getScoringTitle(overallScoringSystem));
		reportingBeans.put("mBest", reportingBeans.get(overallScoringSystem.getMReportingName()));
		reportingBeans.put("wBest", reportingBeans.get(overallScoringSystem.getWReportingName()));
		setReportingBeans(reportingBeans);
	}

	/**
	 * jxls does not translate sheet names and header/footers.
	 *
	 * @param workbook
	 */
	private void translateSheets(Workbook workbook) {
		logger.debug("translating sheets {}", OwlcmsSession.getLocale());
		int nbSheets = workbook.getNumberOfSheets();
		for (int sheetIndex = 0; sheetIndex < nbSheets; sheetIndex++) {
			Sheet curSheet = workbook.getSheetAt(sheetIndex);
			String sheetName = curSheet.getSheetName();
			String translatedSheetName = Translator.translateOrElseNull("CompetitionBook." + sheetName,
			        OwlcmsSession.getLocale());
			workbook.setSheetName(sheetIndex, translatedSheetName != null ? translatedSheetName : sheetName);

			String leftHeader = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_LeftHeader",
			        OwlcmsSession.getLocale());
			if (leftHeader == null) {
				curSheet.getHeader().setLeft(Competition.getCurrent().getCompetitionName());
			} else {
				curSheet.getHeader().setLeft(leftHeader != null ? leftHeader : "");
			}
			
			String centerHeader = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_CenterHeader",
			        OwlcmsSession.getLocale());
			if (centerHeader == null) {
				String c = getChampionship() != null ? getChampionship().getName() : "";
				String ag = getAgeGroupPrefix();
				curSheet.getHeader().setCenter(c != null && ag != null ? c + "\u2013" + ag : (c != null ? c : ag));
			} else {
				curSheet.getHeader().setCenter(centerHeader != null ? centerHeader : "");
			}

			String rightHeader = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_RightHeader",
			        OwlcmsSession.getLocale());
			if (rightHeader == null && translatedSheetName != null) {
				curSheet.getHeader().setRight(translatedSheetName);
			} else  {
				curSheet.getHeader().setRight(rightHeader != null ? rightHeader: "");
			}

			createStandardFooter(workbook);
			String leftFooter = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_LeftFooter",
			        OwlcmsSession.getLocale());
			if (leftFooter != null) {
				curSheet.getFooter().setLeft("leftFooter");
			}
			String centerFooter = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_CenterFooter",
			        OwlcmsSession.getLocale());
			if (centerFooter != null) {
				curSheet.getFooter().setCenter(centerFooter);
			}
			String rightFooter = Translator.translateOrElseNull("CompetitionBook." + sheetName + "_RightFooter",
			        OwlcmsSession.getLocale());
			if (rightFooter != null) {
				curSheet.getFooter().setRight(rightFooter);
			}
		}
	}

	public boolean isIncludeUnfinished() {
		return isIncludeUnfinished;
	}

	public void setIncludeUnfinished(boolean isIncludeUnifinished) {
		this.isIncludeUnfinished = isIncludeUnifinished;
	}

}
