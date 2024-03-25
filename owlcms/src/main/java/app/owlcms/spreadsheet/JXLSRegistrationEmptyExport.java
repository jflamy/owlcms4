/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Return empty list with the current information in the database for groups and platforms, but no athletes
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSRegistrationEmptyExport extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSRegistrationEmptyExport.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	/**
	 * @param ui
	 */
	public JXLSRegistrationEmptyExport(UI ui) {
		try {
			// needed to set the file extension in the source so the download button works.
			getTemplate(OwlcmsSession.getLocale());
		} catch (IOException e) {
		}
	}

	@Override
	public List<Athlete> getSortedAthletes() {
		// no athletes - create an empty so the reporting works
		return List.of();
	}

	@Override
	public InputStream getTemplate(Locale locale) throws IOException {
		return getLocalizedTemplate("/templates/registration/Registration", ".xls", locale);
	}

	@Override
	public boolean isEmptyOk() {
		return true;
	}

	@Override
	protected void postProcess(Workbook workbook) {
		Sheet sheet = workbook.getSheetAt(0);
		Drawing<?> drawing = sheet.createDrawingPatriarch();
		ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 3, 9, 9);
		CreationHelper richTextFactory = workbook.getCreationHelper();
		RichTextString instructions = richTextFactory.createRichTextString(
		        Translator.translate("EmptyInstructions.1") + "\n" +
		                Translator.translate("EmptyInstructions.2") + "\n" +
		                Translator.translate("EmptyInstructions.3") + "\n" +
		                Translator.translate("EmptyInstructions.4"));
		Comment comment = drawing.createCellComment(anchor);
		comment.setString(instructions);
		comment.setVisible(true);

	}

}
