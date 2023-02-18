/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;

@SuppressWarnings("serial")
public class JXLSCards extends JXLSWorkbookStreamSource {

	private final static Logger logger = LoggerFactory.getLogger(JXLSCards.class);

	public JXLSCards() {
		super();
	}

	private void setPageBreaks(Workbook workbook, int cardsPerPage, int linesPerCard) {
		Sheet sheet = workbook.getSheetAt(0);
		int lastRowNum = sheet.getLastRowNum();
		sheet.setAutobreaks(false);
		int increment = cardsPerPage * linesPerCard + (cardsPerPage - 1);

		for (int curRowNum = increment; curRowNum < lastRowNum;) {
			logger.debug("setting break on line {}", curRowNum);
			sheet.setRowBreak(curRowNum - 1);
			curRowNum += increment;
		}
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
	 * @see
	 * org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
	 * postProcess(org.apache.poi.ss.usermodel.Workbook)
	 */
	@Override
	protected void postProcess(Workbook workbook) {
		if (Competition.getCurrent().getComputedCardsTemplateFileName().contains("IWF-")) {
			setPageBreaks(workbook, 1, 17);
		} else if (Competition.getCurrent().getComputedCardsTemplateFileName().contains("SmallCards")) {
			setPageBreaks(workbook, 2, 10);
		}
	}

}
