/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class LocalOverrideUploadDialog extends Dialog {

	final static Logger logger = (Logger) LoggerFactory.getLogger(LocalOverrideUploadDialog.class);

	public LocalOverrideUploadDialog(ZipFileField f) {

		// H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
		// label.getStyle().set("color", "red");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, bytes) -> {
			logger.info("zip type {}", metadata.contentType());
			try {
				ResourceWalker.unzipBlobToTemp(bytes);
				// Save directly to config and reload page
				Config config = Config.getCurrent();
				config.setLocalZipBlob(bytes);
				Config.setCurrent(config);
				ResourceWalker.checkForLocalOverrideDirectory();
				this.close();
				UI.getCurrent().getPage().reload();
			} catch (Exception e) {
				String localizedMessage = e.getLocalizedMessage();
				appendErrors(ta, localizedMessage != null ? localizedMessage : e.toString());
				logger.error("{}", LoggerUtils.stackTrace(e));
			}
		}).whenStart(() -> {
			ta.clear();
			ta.setVisible(false);
		});
		
		Upload upload = new Upload(uploadHandler);
		upload.setWidth("40em");

		upload.setAcceptedFileTypes("application/zip", "application/x-zip-compressed");

		upload.setUploadButton(new Button(Translator.translate("Config.Select")));

		H3 title = new H3(Translator.translate("Config.Select"));
		VerticalLayout vl = new VerticalLayout(title, upload, ta);
		add(vl);
	}

	private void appendErrors(TextArea ta, String sb) {
		if (sb.length() > 0) {
			ta.setValue(sb.toString());
			ta.setVisible(true);
		}
	}

}
