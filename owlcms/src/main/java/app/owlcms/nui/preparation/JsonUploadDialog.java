/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.export.CompetitionData;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JsonUploadDialog extends Dialog {

	final static Logger logger = (Logger) LoggerFactory.getLogger(JsonUploadDialog.class);
	private UI ui;

	public JsonUploadDialog(UI ui) {
		this.ui = ui;

		H5 label = new H5(Translator.translate("ExportDatabase.WarningWillReplaceAll"));
		label.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");
		upload.setAcceptedFileTypes("application/json");

		TextArea ta = new TextArea(getTranslation("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			try {
				System.err.println("success");
				processInput(event.getFileName(), buffer.getInputStream(), ta);
			} catch (Throwable e) {
				ta.setValue(LoggerUtils./**/stackTrace(e));
			}
		});

		upload.addStartedListener(event -> {
			logger.warn("started");
			ta.clear();
			ta.setVisible(false);
		});
		
		upload.addFailedListener(event -> {
			logger.warn("failed upload {}",event.getReason());
		});
		
		upload.addFileRejectedListener(event -> {
			logger.warn("rejected {}"+event.getErrorMessage());
		});

		H3 title = new H3(getTranslation("ExportDatabase.UploadJson"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	@SuppressWarnings("unused")
	private void processInput(String fileName, InputStream inputStream, TextArea ta)
	        throws StreamReadException, DatabindException, IOException {
		try {
			new CompetitionData().restore(inputStream);
			ui.getPage().reload();
		} catch (Throwable e1) {
			ta.setValue(LoggerUtils.exceptionMessage(e1));
		}
	}

}
