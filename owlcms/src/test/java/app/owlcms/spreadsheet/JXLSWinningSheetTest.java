package app.owlcms.spreadsheet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class JXLSWinningSheetTest {

	@Test
	public void recognizesIwfFlatFileTemplateName() {
		assertTrue(JXLSWinningSheet.isIwfFlatFileTemplate("_IWF_FlatFile.xlsx"));
		assertTrue(JXLSWinningSheet.isIwfFlatFileTemplate("IWF Flat File.xlsx"));
		assertTrue(JXLSWinningSheet.isIwfFlatFileTemplate("custom-iwf-flat-file.xlsx"));
	}

	@Test
	public void ignoresOtherResultsTemplates() {
		assertFalse(JXLSWinningSheet.isIwfFlatFileTemplate(null));
		assertFalse(JXLSWinningSheet.isIwfFlatFileTemplate("_FlatFile.xlsx"));
		assertFalse(JXLSWinningSheet.isIwfFlatFileTemplate("Protocol_All_IWF-A4.xlsx"));
	}

}
