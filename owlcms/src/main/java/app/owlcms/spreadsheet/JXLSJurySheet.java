/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.util.List;

import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSJurySheet extends JXLSWorkbookStreamSource {

	Logger logger = LoggerFactory.getLogger(JXLSJurySheet.class);
	// private int nbAthletes;

	public JXLSJurySheet() {
	}

	@Override
	public List<Athlete> computeSortedAthletes() {
		List<Athlete> athletes = AthleteSorter
		        .displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(getGroup(), isExcludeNotWeighed()));
		// this.nbAthletes = athletes.size();
		return athletes;
	}


	protected void setPageBreaks(Workbook workbook, int line) {
		Sheet sheet = workbook.getSheetAt(0);

		sheet.setFitToPage(true);
		PrintSetup printSetup = sheet.getPrintSetup();
		printSetup.setFitWidth((short) 1);
		printSetup.setFitHeight((short) 0);

		sheet.setAutobreaks(false);
		sheet.setRowBreak(line - 1);
	}

}
