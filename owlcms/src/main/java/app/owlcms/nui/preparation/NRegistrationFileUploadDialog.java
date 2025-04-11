/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.InputStream;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.AthleteOptions;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.SessionOptions;
import app.owlcms.spreadsheet.RCompetition;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class NRegistrationFileUploadDialog extends Dialog {

	public final static Logger logger = (Logger) LoggerFactory.getLogger(NRegistrationFileUploadDialog.class);
	final static Logger jxlsLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.reader.SimpleBlockReaderImpl");

	static {
		jxlsLogger.setLevel(Level.ERROR);
	}
	public NRegistrationFileProcessor processor;
	private boolean sbdeFormat;
	public String fileName;
	private AthleteOptions athleteOption;
	private SessionOptions sessionOption;

	public NRegistrationFileUploadDialog(boolean sbdeFormat) {
		this.sbdeFormat = sbdeFormat;

		H5 label = new H5(Translator.translate("Upload.WarningWillReplaceAll"));
		label.getStyle().set("color", "red");
		H5 sbdeLabel = new H5(Translator.translate("SBDE.AthleteOptions_WARNING"));
		sbdeLabel.getStyle().set("color", "red");

		MemoryBuffer buffer = new MemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setWidth("40em");

		Component sos = sessionOptionSelectors();
		Component aos = athleteOptionSelectors();
		
		if (!sbdeFormat) {
			athleteOption = NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES;
			sessionOption = NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS;
		}

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		upload.addSucceededListener(event -> {
			this.processor = this.sbdeFormat // (buffer.getInputStream())
			        ? new NRegistrationFileProcessor(sbdeFormat)
			        : new NRegistrationFileProcessor(sbdeFormat);
			this.fileName = event.getFileName();
			try {
				buffer.getInputStream().reset();
				processInput(buffer.getInputStream(), ta);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		});

		upload.addStartedListener(event -> {
			ta.clear();
			ta.setVisible(false);
		});

		H3 title = new H3(Translator.translate("UploadRegistrationFile"));
		VerticalLayout vl;
		if (sbdeFormat) {
			vl = new VerticalLayout(title, sbdeLabel, aos, sos, upload, ta);
		} else {
			vl = new VerticalLayout(title, label, upload, ta);
		}
		add(vl);
	}

	private Component athleteOptionSelectors() {
		RadioButtonGroup<NRegistrationFileProcessor.AthleteOptions> radioGroup = new RadioButtonGroup<>();
		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setLabel(Translator.translate("SBDE.AthleteOptions"));
		radioGroup.setItems(NRegistrationFileProcessor.AthleteOptions.values());
		radioGroup.setItemLabelGenerator(o -> Translator.translate("SBDE.AthleteOptions_"+o.name()));
		athleteOption = NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES;
		radioGroup.setValue(athleteOption);
		radioGroup.addValueChangeListener(v -> {this.athleteOption = v.getValue();});
		return radioGroup;
	}

	private Component sessionOptionSelectors() {
		RadioButtonGroup<NRegistrationFileProcessor.SessionOptions> radioGroup = new RadioButtonGroup<>();
		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setLabel(Translator.translate("SBDE.SessionOptions"));
		radioGroup.setItems(NRegistrationFileProcessor.SessionOptions.values());
		radioGroup.setItemLabelGenerator(o -> Translator.translate("SBDE.SessionOptions_"+o.name()));
		sessionOption = NRegistrationFileProcessor.SessionOptions.UPDATE_ADD_SESSIONS;
		radioGroup.setValue(sessionOption);
		radioGroup.addValueChangeListener(v -> {this.sessionOption = v.getValue();});
		return radioGroup;
	}

	public void processInput(InputStream inputStream, TextArea ta) {
		this.processor.setAthleteOptions(athleteOption);
		this.processor.setSessionOptions(sessionOption);
		
		// clear athletes to be able to clear groups
		CategoryRepository.resetCodeMap();
		if (this.processor.isDeleteAthletes()) {
			this.processor.resetAthletes();
		}

		// first do a dry run to count sessions
		if (this.processor.isIgnoreSessions()) {
			logger.info("Ignoring session updates");
			// we still need to process the existing ones
			rememberSessionCodes();
		} else {
			rememberSessionCodes();
			int nbSessions = processSessions(inputStream, ta, true);
			logger.info("{} sessions found in file", nbSessions);
			if (nbSessions > 0) {
				if (this.processor.isDeleteSessions()) {
					this.processor.resetSessions();
				}

				// get the sessions from the spreadsheet
				processSessions(inputStream, ta, false);
				logger.info("{} sessions processed", nbSessions);
			}
		}

		if (this.sbdeFormat) {
			processCompetition(inputStream, ta);
		}

		if (isProcessAthletes()) {
			// process athletes now that groups have been adjusted
			processAthletes(inputStream, ta, false);
			this.processor.adjustParticipations();
		}
	}

	private void rememberSessionCodes() {
		AthleteRepository.findAll().stream().forEach(a -> {
			RCompetition.putSessionCode(a.getId(), a.getGroup() != null ? a.getGroup().getName() : "");
		});
	}

	private boolean isProcessAthletes() {
		boolean updatesAllowed = !Config.getCurrent().featureSwitch("noAthleteUpdates");
		return updatesAllowed && this.fileName != null && !this.fileName.contains("_sessions");
	}

	private void processCompetition(InputStream inputStream, TextArea ta) {
		StringBuffer sb = new StringBuffer();
		Consumer<String> errorConsumer = str -> sb.append(str);
		Runnable displayUpdater = () -> updateDisplay(ta, sb);
		this.processor.doProcessCompetitionHeader(inputStream, errorConsumer, displayUpdater);
	}

	private int processAthletes(InputStream inputStream, TextArea ta, boolean dryRun) {
		StringBuffer sb = new StringBuffer();
		Consumer<String> errorConsumer = str -> sb.append(str);
		Runnable displayUpdater = () -> updateDisplay(ta, sb);
		if (this.fileName.contains("_add")) {
			this.processor.setAthleteOptions(NRegistrationFileProcessor.AthleteOptions.ADD_ATHLETES);
		}
		return this.processor.doProcessAthletes(inputStream, dryRun, errorConsumer, displayUpdater);
	}

	private int processSessions(InputStream inputStream, TextArea ta, boolean dryRun) {
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
