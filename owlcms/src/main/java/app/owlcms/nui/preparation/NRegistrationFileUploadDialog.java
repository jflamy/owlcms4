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

import app.owlcms.data.category.CategoryRepository;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.IRegistrationFileProcessor;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.ORegistrationFileProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class NRegistrationFileUploadDialog extends Dialog {

	public final static Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileUploadDialog.class);
	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl");

	static {
		jxlsLogger.setLevel(Level.ERROR);
	}
	public IRegistrationFileProcessor processor;
	private boolean sbdeFormat;
	public String fileName;

	public NRegistrationFileUploadDialog(boolean sbdeFormat) {
		this.sbdeFormat = sbdeFormat;

		H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
		label.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			processor = this.sbdeFormat // (buffer.getInputStream())
			        ? new ORegistrationFileProcessor()
			        : new NRegistrationFileProcessor();
			fileName = event.getFileName();
			// try {
			// buffer.getInputStream().reset();
			processInput(buffer.getInputStream(), ta);
			// } catch (IOException e) {
			// throw new RuntimeException(e);
			// }

		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(Translator.translate("UploadRegistrationFile"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	public void processInput(InputStream inputStream, TextArea ta) {
		// clear athletes to be able to clear groups
		CategoryRepository.resetCodeMap();
		if (eraseAthletes()) {
			this.processor.resetAthletes();
		}

		// first do a dry run to count groups
		int nbGroups = processGroups(inputStream, ta, true);
		logger.info("{} groups found in file", nbGroups);
		if (nbGroups > 0) {
			if (eraseAthletes()) {
				this.processor.resetGroups();
			}

			// get the groups from the spreadsheet
			processGroups(inputStream, ta, false);
			logger.info("{} groups processed", nbGroups);
		}

		// process athletes now that groups have been adjusted
		processAthletes(inputStream, ta, false);
		this.processor.adjustParticipations();
		return;
	}

	private int processAthletes(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		Consumer<String> errorConsumer = str -> sb.append(str);
		Runnable displayUpdater = () -> updateDisplay(ta, sb);
		return this.processor.doProcessAthletes(inputStream, dryRun, errorConsumer, displayUpdater, eraseAthletes());
	}

	private boolean eraseAthletes() {
		return fileName != null && !fileName.contains("_add");
	}

	private int processGroups(InputStream inputStream, TextArea ta, boolean dryRun) {
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

}
