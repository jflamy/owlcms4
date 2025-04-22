/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.export.CompetitionData;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JsonUploadDialog extends Dialog {

	final static Logger logger = (Logger) LoggerFactory.getLogger(JsonUploadDialog.class);
	private UI ui;

	public JsonUploadDialog(UI ui) {
		this.ui = ui;

		H5 label = new H5(Translator.translate("ImportJson.RestartWarning"));
		label.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");
		upload.setAcceptedFileTypes("application/json");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			try {
				processInput(event.getFileName(), buffer.getInputStream(), ta);
				
				ConfirmDialog dialog = new ConfirmDialog();
				dialog.setHeader(Translator.translate("Import.Success"));
				String owlcmsLauncher = System.getenv("OWLCMS_LAUNCHER");
				String preamble = Translator.translate("Import.Warning");
				if (owlcmsLauncher != null) {
					dialog.setText(new Html("<div>"+preamble+Translator.translate("Import.ControlPanelRestart")+"</div>"));
				} else if (JPAService.isLocalDb()){
					dialog.setText(new Html("<div>"+preamble+Translator.translate("Import.LocalRestart")+"</div>"));
				} else {
					dialog.setText(new Html("<div>"+preamble+Translator.translate("Import.CloudRestart")+"</div>"));
				}
				dialog.setConfirmText(Translator.translate("OK"));
				dialog.addConfirmListener(ev -> {
					dialog.close();
					this.ui.getPage().reload();
				});
				dialog.open();
				this.ui.push();

			} catch (Throwable e) {
				e.printStackTrace();
				ta.setValue(LoggerUtils./**/stackTrace(e));
			}
		});

		upload.addStartedListener(event -> {
			logger.debug("started");
			ta.clear();
			ta.setVisible(false);
		});

		upload.addFailedListener(event -> {
			logger.error("failed upload {}", event.getReason());
		});

		upload.addFileRejectedListener(event -> {
			logger.error("rejected {}" + event.getErrorMessage());
		});

		H3 title = new H3(Translator.translate("ExportDatabase.UploadJson"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	@SuppressWarnings("unused")
	private void processInput(String fileName, InputStream inputStream, TextArea ta)
	        throws StreamReadException, DatabindException, IOException {
		try {
			new CompetitionData().restore(inputStream);
		} catch (Throwable e1) {
			ta.setValue(LoggerUtils.exceptionMessage(e1));
		}
	}

}
