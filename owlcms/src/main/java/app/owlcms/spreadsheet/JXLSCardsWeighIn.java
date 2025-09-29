/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class JXLSCardsWeighIn extends JXLSCardsDocs {

	@SuppressWarnings("unused")
	private final static Logger logger = LoggerFactory.getLogger(JXLSCardsWeighIn.class);

	public JXLSCardsWeighIn() {
	}

	@Override
	protected void postProcess(Workbook workbook) {
		if (this.getPageLength() != null) {
			setPageBreaks(workbook, 1, this.getPageLength());
		}
	}

}
