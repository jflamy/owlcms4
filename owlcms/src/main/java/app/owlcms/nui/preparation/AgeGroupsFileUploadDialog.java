/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.function.Consumer;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class AgeGroupsFileUploadDialog extends Dialog {

	public final static Logger logger = (Logger) LoggerFactory.getLogger(AgeGroupsFileUploadDialog.class);
	private Runnable callback;

	public AgeGroupsFileUploadDialog() {

		Html label = new Html("<div>" + Translator.translate("AgeGroups.WarningWillReplaceAll") + "</div>");
		label.getStyle().set("color", "red");
		label.getStyle().set("font-size", "large");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, data) -> {
			// Process the uploaded file data
			try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
				StringBuffer errorsSb = new StringBuffer();
				Consumer<String> errorCollector = str -> {
					if (str != null) {
						String s = str.replaceAll("[\r\n]+$", "");
						errorsSb.append(s).append('\n');
					}
				};
				AgeGroupRepository.reloadDefinitions(inputStream, errorCollector);
				updateDisplay(ta, errorsSb);
				getCallback().run();
			} catch (Exception e) {
				logger.error("Error processing uploaded age groups file", e);
				// You might want to show an error notification here
			}
		}).whenStart(() -> {
			// Clear and hide the error area when upload starts
			ta.clear();
			ta.setVisible(false);
		});
		
		Upload upload = new Upload(uploadHandler);
		upload.setWidth("40em");

		H3 title = new H3(Translator.translate("AgeGroups.UploadCustom"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	private void updateDisplay(TextArea ta, StringBuffer sb) {
		if (sb.length() > 0) {
			String existing = ta.getValue();
			String newText = sb.toString().strip();
			if (existing == null || existing.isEmpty()) {
				ta.setValue(newText);
			} else {
				ta.setValue(existing + System.lineSeparator() + newText);
			}
			ta.setVisible(true);
		}
	}

	public Runnable getCallback() {
		return this.callback;
	}

	public void setCallback(Runnable callback) {
		this.callback = callback;
	}

}
