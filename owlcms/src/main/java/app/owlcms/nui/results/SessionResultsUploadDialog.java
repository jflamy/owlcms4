/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.results;

import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class SessionResultsUploadDialog extends Dialog {

	public final static Logger logger = (Logger) LoggerFactory.getLogger(SessionResultsUploadDialog.class);
	private Consumer<MemoryBuffer> callback;

	public SessionResultsUploadDialog() {

		Html label = new Html("<div>" + Translator.translate("ImportSessions.WarningWillReplaceAll") + "</div>");
		label.getStyle().set("color", "red");
		label.getStyle().set("font-size", "large");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			getCallback().accept(buffer);
		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(Translator.translate("ImportSessions.UploadDatabaseExport"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	public Consumer<MemoryBuffer> getCallback() {
		return this.callback;
	}

	public void setCallback(Consumer<MemoryBuffer> callback) {
		this.callback = callback;
	}

}
