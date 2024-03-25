/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

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

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");

		TextArea ta = new TextArea(getTranslation("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			AgeGroupRepository.reloadDefinitions(buffer.getInputStream());
			getCallback().run();
		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(getTranslation("AgeGroups.UploadCustom"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	public Runnable getCallback() {
		return this.callback;
	}

	public void setCallback(Runnable callback) {
		this.callback = callback;
	}

}
