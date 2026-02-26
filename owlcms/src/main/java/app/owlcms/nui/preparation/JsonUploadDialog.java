/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;

import app.owlcms.components.ConfirmationDialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.export.FormatDetector;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;
import org.apache.maven.artifact.versioning.ComparableVersion;

@SuppressWarnings("serial")
public class JsonUploadDialog extends Dialog {

	final static Logger logger = (Logger) LoggerFactory.getLogger(JsonUploadDialog.class);
	private UI ui;
	private boolean isRestartScenario;
	private final Locale capturedLocale;
	private final ConfirmationDialog restartConfirmationDialog;

	public JsonUploadDialog(UI ui) {
		this.ui = ui;
		this.isRestartScenario = checkIfRestartScenario();
		// Capture from session—safe here because the constructor runs on the UI thread.
		// OwlcmsSession.getLocale() correctly resolves fr_CA, ru_CA, etc.
		this.capturedLocale = OwlcmsSession.getLocale();
		this.restartConfirmationDialog = createRestartConfirmationDialog();

		H5 label = new H5(Translator.translate(isRestartScenario ? "ImportJsonR.RestartWarning" : "ImportJson.RestartWarning"));
		label.getStyle().set("color", "red");

		TextArea ta = new TextArea(Translator.translate("Errors"));
		ta.setHeight("20ex");
		ta.setWidth("80em");
		ta.setVisible(false);

		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, data) -> {
			try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
				processInput(metadata.fileName(), inputStream, ta);
				openRestartConfirmation();

			} catch (Throwable e) {
				logger.error("Error processing uploaded JSON file", e);
				if (ui != null) {
					ui.access(() -> {
						ta.setValue(LoggerUtils.stackTrace(e));
						ta.setVisible(true);
					});
					ui.push();
				} else {
					ta.setValue(LoggerUtils.stackTrace(e));
					ta.setVisible(true);
				}
			}
		}).whenStart(() -> {
			logger.debug("started");
			ta.clear();
			ta.setVisible(false);
		});
		
		Upload upload = new Upload(uploadHandler);
		upload.setWidth("40em");
		upload.setAcceptedFileTypes("application/json");

		H3 title = new H3(Translator.translate("ExportDatabase.UploadJson"));
		VerticalLayout vl = new VerticalLayout(title, label, upload, ta);
		add(vl);
	}

	private ConfirmationDialog createRestartConfirmationDialog() {
		String titleKey = isRestartScenario ? "ImportR.Success" : "Import.Success";
		String warningKey = isRestartScenario ? "ImportR.Warning" : "Import.Warning";
		String controlPanelKey = isRestartScenario ? "ImportR.ControlPanelRestart" : "Import.ControlPanelRestart";
		String localKey = isRestartScenario ? "ImportR.ControlPanelRestart" : "Import.LocalRestart";
		String cloudKey = isRestartScenario ? "ImportR.CloudRestart" : "Import.CloudRestart";
		String confirmKey = isRestartScenario ? "ImportR.DoIt" : "Import.DoIt";
		String restartLabel = translateButtonLabel(confirmKey, "Restart OWLCMS");
		String cancelLabel = translateButtonLabel("Cancel", "Cancel");

		String preamble = translatePinned(warningKey);
		String message;
		if (System.getenv("OWLCMS_CONTROLPANEL") != null) {
			message = preamble + translatePinned(controlPanelKey);
		} else if (JPAService.isLocalDb()) {
			message = preamble + translatePinned(localKey);
		} else {
			message = preamble + translatePinned(cloudKey);
		}

		return new ConfirmationDialog(
		        translatePinned(titleKey),
		        message,
		        restartLabel,
		        cancelLabel,
		        null,
		        () -> {
			        JsonUploadDialog.this.close();
			        if (ui != null) {
				        ui.push();
			        }
			        try {
				        Thread.sleep(2000);
			        } catch (InterruptedException e) {
				        Thread.currentThread().interrupt();
			        }
			        FormatDetector.checkAndRestartIfNeeded();
			        if (ui != null) {
				        ui.getPage().reload();
			        }
		        });
	}

	private String translateButtonLabel(String key, String hardcodedFallback) {
		String value = Translator.translateNoOverrideOrElseNull(key, capturedLocale);
		if (value == null || value.isBlank()) {
			logger.warn("{} Missing button translation key='{}' locale='{}'. Using hardcoded fallback='{}'.\n{}",
			        LoggerUtils.whereFrom(), key, capturedLocale, hardcodedFallback, LoggerUtils.stackTrace());
			return hardcodedFallback;
		}
		return value;
	}

	private String translatePinned(String key) {
		String value = Translator.translateExplicitLocale(key, capturedLocale);
		if (value != null && value.startsWith("!")) {
			logger.warn("{} translatePinned primary miss key='{}' locale='{}' value='{}'\n{}",
			        LoggerUtils.whereFrom(), key, capturedLocale, value, LoggerUtils.stackTrace());
			String fallback = Translator.translateExplicitLocale(key, Locale.ENGLISH);
			if (fallback != null && fallback.startsWith("!")) {
				logger.warn("{} translatePinned fallback miss key='{}' fallbackLocale='{}' value='{}'\n{}",
				        LoggerUtils.whereFrom(), key, Locale.ENGLISH, fallback, LoggerUtils.stackTrace());
			}
			return fallback;
		}
		return value;
	}

	private void openRestartConfirmation() {
		if (ui != null) {
			ui.access(() -> {
				restartConfirmationDialog.open();
				ui.push();
			});
		} else {
			restartConfirmationDialog.open();
		}
	}

	@SuppressWarnings("unused")
	private void processInput(String fileName, InputStream inputStream, TextArea ta)
	        throws StreamReadException, DatabindException, IOException {
		try {
			// Use FormatDetector for automatic V1/V2 format detection
			FormatDetector.importData(inputStream);
		} catch (Throwable e1) {
			if (ui != null) {
				ui.access(() -> {
					ta.setValue(LoggerUtils.exceptionMessage(e1));
					ta.setVisible(true);
				});
				ui.push();
			} else {
				ta.setValue(LoggerUtils.exceptionMessage(e1));
				ta.setVisible(true);
			}
			throw new IOException("Import failed: " + e1.getMessage(), e1);
		}
	}

	/**
	 * Check if we're in a restart scenario (OWLCMS_CONTROLPANEL >= 3.1.0)
	 */
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
