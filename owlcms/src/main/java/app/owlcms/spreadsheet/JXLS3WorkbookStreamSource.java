/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Workbook;
import org.jxls.builder.JxlsStreaming;
import org.jxls.transform.poi.JxlsPoi;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Encapsulate a spreadsheet as a StreamSource so that it can be used as a source of data when the user clicks on a
 * link. This class converts the output stream to an input stream that the vaadin framework can consume.
 */
@SuppressWarnings("serial")
public abstract class JXLS3WorkbookStreamSource extends JXLSWorkbookStreamSource {

	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLS3WorkbookStreamSource.class);
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}
	private UI ui;
	private Consumer<String> doneCallback;

	public JXLS3WorkbookStreamSource() {
		this.ui = UI.getCurrent();
		this.setExcludeNotWeighed(true);
		init();
	}

	@Override
	public void writeStream(OutputStream stream) throws IOException {
		Locale locale = OwlcmsSession.getLocale();
		Workbook workbook = null;
		try {
			setReportingInfo();
			HashMap<String, Object> reportingInfo = getReportingBeans();
			@SuppressWarnings("unchecked")
			List<Athlete> athletes = (List<Athlete>) reportingInfo.get("athletes");
			if (athletes != null && (athletes.size() > 0 || isEmptyOk())) {
				// workbook = transformer.transformXLS(getTemplate(locale), reportingInfo);
				JxlsPoi.fill(getTemplate(locale), JxlsStreaming.STREAMING_OFF, reportingInfo, stream);
				// TODO create a workbook from the stream to enable post-processing, return the new stream
			} else {
				String noAthletes = Translator.translate("NoAthletes");
				logger./**/warn("no athletes: empty report.");
				this.ui.access(() -> {
					Notification notif = new Notification();
					notif.addThemeVariants(NotificationVariant.LUMO_ERROR);
					notif.setPosition(Position.TOP_STRETCH);
					notif.setDuration(3000);
					notif.setText(noAthletes);
					notif.open();
				});
				workbook = new HSSFWorkbook();
				workbook.createSheet().createRow(1).createCell(1).setCellValue(noAthletes);
			}
		} catch (Exception e) {
			LoggerUtils.logError(logger, e);
		}
		try {
			if (this.doneCallback != null) {
				this.doneCallback.accept(null);
			}
		} catch (Throwable e) {
			LoggerUtils.logError(logger, e);
		}
		logger.debug("wrote stream");
	}

	@Override
	protected void postProcess(Workbook workbook) {
		throw new UnsupportedOperationException("Postprocessing not implemented for jxls3.");
	}

}
