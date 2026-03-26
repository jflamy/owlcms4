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
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.NRegistrationFileProcessor;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.AthleteOptions;
import app.owlcms.spreadsheet.NRegistrationFileProcessor.SessionOptions;
import app.owlcms.spreadsheet.RCompetition;
import org.apache.maven.artifact.versioning.ComparableVersion;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class NRegistrationFileUploadDialog extends Dialog {
	private static final Set<String> ACCEPTED_SPREADSHEET_EXTENSIONS = Set.of(".xls", ".xlsx");
	private static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
	private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	private static final String REGISTRATION_REPLACE_WARNING_KEY = "Upload.RegistrationWarningWillReplaceAll";
	private static final String UNSUPPORTED_REGISTRATION_UPLOAD_MESSAGE = "Only XLSX and XLS formats are supported";
	private static final String UNSUPPORTED_REGISTRATION_UPLOAD_MESSAGE_KEY = "Upload.UnsupportedSpreadsheetFormat";

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
	private Locale capturedLocale;
	private boolean isRestartScenario;

	public NRegistrationFileUploadDialog(boolean sbdeFormat) {
		this.sbdeFormat = sbdeFormat;
		this.isRestartScenario = checkIfRestartScenario();
		// Capture locale now while still on UI thread - will be used in upload callback
		this.capturedLocale = OwlcmsSession.getLocale();

		H5 label = new H5(Translator.translate(REGISTRATION_REPLACE_WARNING_KEY));
		label.getStyle().set("color", "red");
		H5 sbdeLabel = new H5(Translator.translate("SBDE.AthleteOptions_WARNING"));
		sbdeLabel.getStyle().set("color", "red");
		H5 restartWarning = new H5(Translator.translate("SBDE.RestartWarning"));
		restartWarning.getStyle().set("color", "red");

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
			        ? new NRegistrationFileProcessor(sbdeFormat, this.capturedLocale)
			        : new NRegistrationFileProcessor(sbdeFormat, this.capturedLocale);
			this.fileName = metadata.fileName();
			if (!isAcceptedSpreadsheetUpload(metadata.fileName(), metadata.contentType())) {
				logger./**/warn("Rejected registration upload fileName={} contentType={}", metadata.fileName(), metadata.contentType());
				appendErrors(ta, getUnsupportedRegistrationUploadMessage());
				return;
			}
			
			// Check if this is a sessions-only file by looking at A2 of first sheet
			boolean isSessionsOnly = false;
			try (ByteArrayInputStream checkStream = new ByteArrayInputStream(data)) {
				isSessionsOnly = isSessionsOnlyFile(checkStream);
			} catch (Exception e) {
				logger./**/warn("Could not determine file type, assuming full registration file", e);
			}
			
			try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
				processInput(inputStream, ta, isSessionsOnly);
				openRestartConfirmation();
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
		upload.setAcceptedFileTypes(XLS_CONTENT_TYPE, XLSX_CONTENT_TYPE, ".xls", ".xlsx");
		upload.addFileRejectedListener(event -> appendErrors(ta, getUnsupportedRegistrationUploadMessage()));

		H3 title = new H3(Translator.translate("UploadRegistrationFile"));
		VerticalLayout vl;
		if (sbdeFormat) {
			vl = new VerticalLayout(title, sbdeLabel, restartWarning, aos, sos, upload, ta);
		} else {
			vl = new VerticalLayout(title, label, restartWarning, upload, ta);
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
		processInput(inputStream, ta, false);
	}

	public void processInput(InputStream inputStream, TextArea ta, boolean isSessionsOnly) {
		this.processor.setAthleteOptions(athleteOption);
		this.processor.setSessionOptions(sessionOption);

		// clear athletes to be able to clear sessions
		CategoryRepository.resetCodeMap();
		if (this.processor.isDeleteAthletes()) {
			// compute how many will be deleted; log the info but do not display a translated message in the UI
			int priorCount = AthleteRepository.findAll().size();
			this.processor.resetAthletes();
			String deletedMsg = "Existing athletes were deleted before processing: " + priorCount;
			logger.debug(deletedMsg);
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
			logger.debug(msg);
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
			logger.debug(msg);
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

		// First pass: parse sessions to determine valid groups for athlete assignment.
		// The valid groups depend on the session option:
		// - IGNORE_SESSIONS: only database groups are valid (spreadsheet sessions ignored)
		// - DELETE_SESSIONS: only spreadsheet groups are valid (database will be cleared)
		// - UPDATE_ADD_SESSIONS: database + spreadsheet groups (spreadsheet overrides)
		rememberSessionCodes();
		int nbSessionsFound = this.processor.doProcessGroups(inputStream, true, s -> {
			// discard validation pass messages; we only want the count
		}, noopUpdater);
		// show the session count only in logs (keep DataProcessed.* translations for UI counts)
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

		if (this.sbdeFormat && !isSessionsOnly) {
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

	private boolean isSessionsOnlyFile(InputStream inputStream) {
		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			Sheet firstSheet = workbook.getSheetAt(0);
			Row row = firstSheet.getRow(1); // A2 is row index 1
			if (row == null) {
				return false;
			}
			Cell cell = row.getCell(0); // Column A
			if (cell == null) {
				return false;
			}
			if (cell.getCellType() != CellType.STRING) {
				return false;
			}
			String cellValue = cell.getStringCellValue();
			if (cellValue == null || cellValue.trim().isEmpty()) {
				return false;
			}
			
			// Check if A2 matches "Group" or "Session" canonical key
			String trimmed = cellValue.trim();
			try {
				String tGroupCur = Translator.translate("Group");
				String tGroupEng = Translator.translateExplicitLocale("Group", Locale.ENGLISH);
				String tSessionCur = Translator.translate("Session");
				String tSessionEng = Translator.translateExplicitLocale("Session", Locale.ENGLISH);
				
				if ((tGroupCur != null && trimmed.equalsIgnoreCase(tGroupCur)) ||
					(tGroupEng != null && trimmed.equalsIgnoreCase(tGroupEng)) ||
					(tSessionCur != null && trimmed.equalsIgnoreCase(tSessionCur)) ||
					(tSessionEng != null && trimmed.equalsIgnoreCase(tSessionEng))) {
					return true;
				}
			} catch (Exception ex) {
				// ignore translation errors
			}
		} catch (Exception e) {
			logger./**/warn("Could not check if file is sessions-only", e);
		}
		return false;
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
			// logger.debug(sb.toString() + " " + LoggerUtils.stackTrace());
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

	private void appendErrors(TextArea ta, String message) {
		if (message == null || message.isBlank()) {
			return;
		}
		StringBuffer sb = new StringBuffer();
		sb.append(message).append('\n');
		updateDisplay(ta, sb);
	}

	private String getUnsupportedRegistrationUploadMessage() {
		String translated = Translator.translateOrElseNull(UNSUPPORTED_REGISTRATION_UPLOAD_MESSAGE_KEY, capturedLocale);
		return translated != null && !translated.isBlank() ? translated : UNSUPPORTED_REGISTRATION_UPLOAD_MESSAGE;
	}

	private boolean isAcceptedSpreadsheetUpload(String uploadedFileName, String contentType) {
		String normalizedFileName = uploadedFileName == null ? "" : uploadedFileName.toLowerCase(Locale.ROOT);
		boolean acceptedExtension = ACCEPTED_SPREADSHEET_EXTENSIONS.stream().anyMatch(normalizedFileName::endsWith);
		if (!acceptedExtension) {
			return false;
		}
		if (contentType == null || contentType.isBlank()) {
			return true;
		}
		return XLS_CONTENT_TYPE.equalsIgnoreCase(contentType) || XLSX_CONTENT_TYPE.equalsIgnoreCase(contentType);
	}

	private void openRestartConfirmation() {
		UI ui = this.getUI().orElse(UI.getCurrent());

		String titleKey = isRestartScenario ? "ImportR.Success" : "Import.Success";
		String controlPanelKey = isRestartScenario ? "ImportR.ControlPanelRestart" : "Import.ControlPanelRestart";
		String localKey = isRestartScenario ? "ImportR.ControlPanelRestart" : "Import.LocalRestart";
		String cloudKey = isRestartScenario ? "ImportR.CloudRestart" : "Import.CloudRestart";
		String confirmKey = isRestartScenario ? "ImportR.DoIt" : "Import.DoIt";

		String owlcmsLauncher = System.getenv("OWLCMS_CONTROLPANEL");
		String preamble = Translator.translate("SBDE.RestartWarning");
		String message;
		if (owlcmsLauncher != null) {
			message = preamble + " " + Translator.translate(controlPanelKey);
		} else if (JPAService.isLocalDb()) {
			message = preamble + " " + Translator.translate(localKey);
		} else {
			message = preamble + " " + Translator.translate(cloudKey);
		}

		new ConfirmationDialog(
		        Translator.translate(titleKey),
		        message,
		        Translator.translate(confirmKey),
		        null,
		        () -> {
			        NRegistrationFileUploadDialog.this.close();
			        if (ui != null) {
				        ui.push();
			        }
			        try {
				        Thread.sleep(2000);
			        } catch (InterruptedException e) {
				        Thread.currentThread().interrupt();
			        }
			        FormatDetector.checkAndRestartIfNeeded();
		        }
		).open();
		if (ui != null) {
			ui.push();
		}
	}

	private boolean checkIfRestartScenario() {
		String controlPanelVersion = System.getenv("OWLCMS_CONTROLPANEL");
		if (controlPanelVersion == null || controlPanelVersion.trim().isEmpty()) {
			return false;
		}

		try {
			ComparableVersion currentVersion = new ComparableVersion(controlPanelVersion);
			ComparableVersion minVersion = new ComparableVersion("3.1.0-alpha00");
			return currentVersion.compareTo(minVersion) >= 0;
		} catch (Exception e) {
			logger.error("Error checking control panel version: {}", e.getMessage());
			return false;
		}
	}

}
