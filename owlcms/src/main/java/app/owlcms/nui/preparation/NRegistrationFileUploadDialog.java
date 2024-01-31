/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.InputStream;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.RegistrationFileProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class NRegistrationFileUploadDialog extends Dialog {

	
	public final static Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileUploadDialog.class);
	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl");

	static {
		jxlsLogger.setLevel(Level.ERROR);
	}

	public RegistrationFileProcessor processor = new RegistrationFileProcessor();

	public NRegistrationFileUploadDialog() {

		H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
		label.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");

		TextArea ta = new TextArea(getTranslation("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			processInput(event.getFileName(), buffer.getInputStream(), ta);
		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(getTranslation("UploadRegistrationFile"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	public int processAthletes(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		Consumer<String> errorConsumer = str -> sb.append(str);
		Runnable displayUpdater = () -> updateDisplay(ta, sb);
		return this.processor.doProcessAthletes(inputStream, dryRun, errorConsumer, displayUpdater);
	}

	public int processGroups(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		Consumer<String> errorConsumer = str -> sb.append(str);
		Runnable displayUpdater = () -> updateDisplay(ta, sb);
		return this.processor.doProcessGroups(inputStream, dryRun, errorConsumer, displayUpdater);
	}

	private void updateDisplay(TextArea ta, StringBuffer sb) {
		if (sb.length() > 0) {
			ta.setValue(sb.toString());
			ta.setVisible(true);
		}
	}
	
	public void processInput(String fileName, InputStream inputStream, TextArea ta) {
		// clear athletes to be able to clear groups
		processor.resetAthletes();
	
		// dry run to count groups
		int nbGroups = processGroups(inputStream, ta, true);
		if (nbGroups > 0) {
			// new format, reset groups from spreadsheet
			processor.resetGroups();
			processGroups(inputStream, ta, false);
		}
	
		// process athletes now that groups have been adjusted
		processAthletes(inputStream, ta, false);
		processor.adjustParticipations();
		return;
	}


}
