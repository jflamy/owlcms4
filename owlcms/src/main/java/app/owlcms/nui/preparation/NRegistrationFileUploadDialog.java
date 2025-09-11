/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
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
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.AthleteOptions;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.SessionOptions;
import app.owlcms.spreadsheet.RCompetition;
import app.owlcms.utils.LoggerUtils;
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

	// Keep the exported-Excel translation in the master file, but in the interactive UI we
	// show a simple English warning text (non-translated) and log the canonical warning if needed.
	H5 label = new H5("Warning: this will replace all existing data.");
		label.getStyle().set("color", "red");
		H5 sbdeLabel = new H5(Translator.translate("SBDE.AthleteOptions_WARNING"));
		sbdeLabel.getStyle().set("color", "red");

		Component sos = sessionOptionSelectors();
		Component aos = athleteOptionSelectors();

		if (!sbdeFormat) {
			athleteOption = NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES;
			sessionOption = NRegistrationFileProcessor.SessionOptions.DELETE_SESSIONS;
		}

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, data) -> {
			// Process the uploaded file data
			this.processor = this.sbdeFormat
			        ? new NRegistrationFileProcessor(sbdeFormat)
			        : new NRegistrationFileProcessor(sbdeFormat);
			this.fileName = metadata.fileName();
			try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
				processInput(inputStream, ta);
			} catch (Exception e) {
				logger.error("Error processing uploaded registration file", e);
				throw new RuntimeException(e);
			}
		}).whenStart(() -> {
			// Clear and hide the error area when upload starts (equivalent to addStartedListener)
			ta.clear();
			ta.setVisible(false);
		});
		
		Upload upload = new Upload(uploadHandler);
		upload.setWidth("40em");

		H3 title = new H3(Translator.translate("UploadRegistrationFile"));
		VerticalLayout vl;
		if (sbdeFormat) {
			vl = new VerticalLayout(title, sbdeLabel, aos, sos, upload, ta);
		} else {
			vl = new VerticalLayout(title, label, upload, ta);
		}
		add(vl);
	}

	RadioButtonGroup<NRegistrationFileProcessor.AthleteOptions> radioGroup = new RadioButtonGroup<>();

	private Component athleteOptionSelectors() {
		radioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		radioGroup.setLabel(Translator.translate("SBDE.AthleteOptions"));
		radioGroup.setItems(NRegistrationFileProcessor.AthleteOptions.values());
		radioGroup.setItemLabelGenerator(o -> Translator.translate("SBDE.AthleteOptions_" + o.name()));
		athleteOption = NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES;
		radioGroup.setValue(athleteOption);
		radioGroup.addValueChangeListener(v -> {
			this.athleteOption = v.getValue();
			if (this.athleteOption != NRegistrationFileProcessor.AthleteOptions.DELETE_ATHLETES) {
				sessionOption = NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS;
				sessionRadioGroup.setValue(sessionOption);			
			}
		});
		return radioGroup;
	}

	private RadioButtonGroup<NRegistrationFileProcessor.SessionOptions> sessionRadioGroup = new RadioButtonGroup<>();

	private Component sessionOptionSelectors() {
		sessionRadioGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		sessionRadioGroup.setLabel(Translator.translate("SBDE.SessionOptions"));
		sessionRadioGroup.setItems(NRegistrationFileProcessor.SessionOptions.values());
		sessionRadioGroup.setItemLabelGenerator(o -> Translator.translate("SBDE.SessionOptions_" + o.name()));
		sessionOption = NRegistrationFileProcessor.SessionOptions.DELETE_SESSIONS;
		sessionRadioGroup.setValue(sessionOption);
		sessionRadioGroup.addValueChangeListener(v -> {
			this.sessionOption = v.getValue();
		});
		return sessionRadioGroup;
	}

	public void processInput(InputStream inputStream, TextArea ta) {
		this.processor.setAthleteOptions(athleteOption);
		this.processor.setSessionOptions(sessionOption);

		// clear athletes to be able to clear sessions
		CategoryRepository.resetCodeMap();
		if (this.processor.isDeleteAthletes()) {
			// compute how many will be deleted; log the info but do not display a translated message in the UI
			int priorCount = AthleteRepository.findAll().size();
			this.processor.resetAthletes();
			String deletedMsg = "Existing athletes were deleted before processing: " + priorCount;
			logger.info(deletedMsg);
			// do not call updateDisplay for this non-count Upload.* message (logging only)
		}

		// Surface a short confirmation message in the UI about how athletes will be handled
		{
			String msg;
			if (this.athleteOption == NRegistrationFileProcessor.AthleteOptions.IGNORE_ATHLETES) {
				msg = "Athlete updates will be ignored.";
			} else if (this.processor.isDeleteAthletes()) {
				msg = "Existing athletes were deleted before processing.";
			} else if (this.athleteOption == NRegistrationFileProcessor.AthleteOptions.ADD_ATHLETES) {
				msg = "Athletes will be added only (no updates).";
			} else {
				msg = "Athletes will be updated or added.";
			}
			// logging only for option messages; do not surface these messages in the UI
			logger.info(msg);
		}

		// Surface a short confirmation message in the UI about how sessions will be handled
		{
			String msg;
			if (this.sessionOption == NRegistrationFileProcessor.SessionOptions.IGNORE_SESSIONS) {
				msg = "Session updates will be ignored.";
			} else if (this.processor.isDeleteSessions()) {
				msg = "Existing sessions were deleted before processing.";
			} else {
				msg = "Sessions will be updated or added.";
			}
			// logging only for session option messages; do not surface these messages in the UI
			logger.info(msg);
		}

		// Collect errors into a buffer and avoid interleaving counts/options with errors.
		StringBuffer errorsSb = new StringBuffer();
		Consumer<String> errorCollector = str -> {
			if (str != null) {
				// strip any trailing CR/LF sequences and append exactly one LF
				String s = str.replaceAll("[\r\n]+$", "");
				errorsSb.append(s).append('\n');
			}
		};
		Runnable noopUpdater = () -> {
			// intentionally empty: we'll show counts/options immediately and errors at the end
		};

		// first do a dry run to count sessions (always run dry-run to report count)
		rememberSessionCodes();
		int nbSessionsFound = this.processor.doProcessGroups(inputStream, true, s -> {
			// discard dry-run messages; we only want the count
		}, noopUpdater);
	// show the dry-run session count only in logs (keep DataProcessed.* translations for UI counts)
	logger.info(MessageFormat.format("{0} sessions identified.", Integer.valueOf(nbSessionsFound)));

		int nbSessionsProcessed = 0;
		if (nbSessionsFound > 0 && !this.processor.isIgnoreSessions()) {
			if (this.processor.isDeleteSessions()) {
				this.processor.resetSessions();
				logger.info("cleared existing sessions");
			}
			// perform actual session processing but collect errors instead of showing them immediately
			nbSessionsProcessed = this.processor.doProcessGroups(inputStream, false, errorCollector, noopUpdater);
			// Processor will have added a processed summary to the collector; remove it so we can display counts separately
			try {
				String processedTpl = Translator.translate("Upload.DataProcessed.Sessions");
				String processedMsg = MessageFormat.format(processedTpl, Integer.valueOf(nbSessionsProcessed));
				int idx = errorsSb.indexOf(processedMsg);
				if (idx >= 0) {
					errorsSb.delete(idx, idx + processedMsg.length());
				}
			} catch (Exception ex) {
				// ignore translation removal failure
			}
			// show processed sessions count
			{
				String template = Translator.translate("Upload.DataProcessed.Sessions");
				String msg = MessageFormat.format(template, nbSessionsProcessed);
				StringBuffer sbForDisplay = new StringBuffer();
				sbForDisplay.append(msg).append("\n");
				updateDisplay(ta, sbForDisplay);
			}
		} else if (this.processor.isIgnoreSessions()) {
			// indicate sessions were ignored (already displayed earlier as option)
		}

		if (this.sbdeFormat) {
			processCompetition(inputStream, ta);
		}

		if (isProcessAthletes()) {
			// process athletes now that groups have been adjusted
			int nbAthletesProcessed = this.processor.doProcessAthletes(inputStream, false, errorCollector, noopUpdater);
			this.processor.adjustParticipations();
			// Processor may have added a processed summary to the collector; remove it so we can display counts separately
			try {
				String processedTpl = Translator.translate("Upload.DataProcessed.Athletes");
				String processedMsg = MessageFormat.format(processedTpl, Integer.valueOf(nbAthletesProcessed));
				int idx = errorsSb.indexOf(processedMsg);
				if (idx >= 0) {
					errorsSb.delete(idx, idx + processedMsg.length());
				}
			} catch (Exception ex) {
				// ignore translation removal failure
			}
			// show processed athletes count
			String template = Translator.translate("Upload.DataProcessed.Athletes");
			String msg = MessageFormat.format(template, nbAthletesProcessed);
			StringBuffer sbForDisplay = new StringBuffer();
			sbForDisplay.append(msg).append("\n");
			updateDisplay(ta, sbForDisplay);
		}

		// Finally, append any collected errors at the end so counts/options remain separate
		if (errorsSb.length() > 0) {
			updateDisplay(ta, errorsSb);
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

	private void updateDisplay(TextArea ta, StringBuffer sb) {
		if (sb.length() > 0) {
			String existing = ta.getValue();
			// Log a warning with origin information (full stack trace) so we can trace who added the UI trace
			logger.warn(sb.toString() + "  " + LoggerUtils.stackTrace());
			String newText = sb.toString();
			// Strip trailing/leading whitespace to avoid double blank lines when appending
			newText = newText.strip();
			if (existing == null || existing.isEmpty()) {
				ta.setValue(newText);
			} else {
				ta.setValue(existing + System.lineSeparator() + newText);
			}
			ta.setVisible(true);
		}
	}

}
