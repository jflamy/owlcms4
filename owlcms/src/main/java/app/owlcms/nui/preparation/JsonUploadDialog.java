/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.preparation;

import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.access.ImportsNotAllowedException;
import app.owlcms.access.ImportsNotAllowedException.Reason;
import app.owlcms.access.Principal;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditOrigin;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.streams.UploadHandler;

import app.owlcms.data.export.FormatDetector;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.RestartUtils;
import ch.qos.logback.classic.Logger;

/**
 * Replaces the database with an uploaded export. The uploaded file is held until the import is confirmed; OWLCMS then
 * imports it and restarts, because the running server cannot be used on the replaced data.
 */
@SuppressWarnings("serial")
public class JsonUploadDialog extends Dialog {

	final static Logger logger = (Logger) LoggerFactory.getLogger(JsonUploadDialog.class);
	/** leaves time for the restart notification to reach the browser */
	private static final long RESTART_NOTICE_MS = 2000L;
	private final UI ui;
	private final Locale capturedLocale;
	private final AuditOrigin importOrigin;
	private final Principal importer;
	private final String notPermitted;
	private final String notAllowed;
	private final Upload upload;
	private final TextArea errors;
	private final Button cancelButton;
	private final Button confirmButton;
	private String pendingFileName;
	private byte[] pendingData;

	public JsonUploadDialog(Principal importer, UI ui) {
		this.ui = ui;
		// Capture from session—safe here because the constructor runs on the UI thread.
		// OwlcmsSession.getLocale() correctly resolves fr_CA, ru_CA, etc.
		this.capturedLocale = OwlcmsSession.getLocale();
		this.importOrigin = AuditOrigin.fromUi("Import Database");
		this.importer = importer;
		this.notPermitted = Translator.translate("Imports.NotPermitted");
		this.notAllowed = Translator.translate("Imports.NotAllowed");

		H5 label = new H5(Translator.translate("ImportJsonR.RestartWarning"));
		label.getStyle().set("color", "red");

		this.errors = new TextArea(Translator.translate("Errors"));
		this.errors.setHeight("20ex");
		this.errors.setWidth("80em");
		this.errors.setVisible(false);

		// the upload callbacks are not on the UI thread
		UploadHandler uploadHandler = UploadHandler.inMemory((metadata, data) -> {
			String fileName = metadata.fileName();
			updateUi(() -> fileReceived(fileName, data));
		}).whenStart(() -> updateUi(this::uploadStarted));

		this.upload = new Upload(uploadHandler);
		this.upload.setWidth("40em");
		this.upload.setAcceptedMimeTypes("application/json");
		this.upload.setMaxFiles(1);
		this.upload.addFileRemovedListener(e -> clearPending());

		H3 title = new H3(Translator.translate("ExportDatabase.UploadJson"));
		VerticalLayout vl = new VerticalLayout(title, label, this.upload, this.errors);
		add(vl);

		this.cancelButton = new Button(Translator.translate("Cancel"), e -> close());
		this.confirmButton = new Button(Translator.translate("ImportJson.ImportAndRestart"), e -> importConfirmed());
		this.confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		this.confirmButton.setEnabled(false);
		getFooter().add(this.cancelButton, this.confirmButton);
	}

	private void uploadStarted() {
		clearPending();
		this.errors.clear();
		this.errors.setVisible(false);
	}

	private void fileReceived(String fileName, byte[] data) {
		this.pendingFileName = fileName;
		this.pendingData = data;
		this.confirmButton.setEnabled(true);
	}

	private void clearPending() {
		this.pendingFileName = null;
		this.pendingData = null;
		this.confirmButton.setEnabled(false);
	}

	/** On the UI thread. The import itself runs in the background. */
	private void importConfirmed() {
		String fileName = this.pendingFileName;
		byte[] data = this.pendingData;
		if (data == null) {
			return;
		}
		clearPending();
		setImporting(true);
		Notification importing = new Notification(Translator.translate("ImportJson.Importing"));
		importing.setPosition(Position.BOTTOM_START);
		importing.setDuration(0);
		importing.open();
		Thread.ofVirtual().name("DatabaseImport").start(() -> importAndRestart(fileName, data, importing));
	}

	private void setImporting(boolean importing) {
		this.upload.setEnabled(!importing);
		this.cancelButton.setEnabled(!importing);
		setCloseOnEsc(!importing);
		setCloseOnOutsideClick(!importing);
	}

	/** Background thread. Once the database has been replaced, the restart takes place whatever happens to the page. */
	private void importAndRestart(String fileName, byte[] data, Notification importing) {
		try {
			// detects the format, checks that imports are still allowed, and audits the import
			FormatDetector.importData(data, fileName, this.importer, this.importOrigin);
		} catch (ImportsNotAllowedException e) {
			String message = e.getReason() == Reason.NO_PERMISSION ? this.notPermitted : this.notAllowed;
			updateUi(() -> importFailed(importing, message));
			return;
		} catch (Throwable e) {
			logger.error("Error processing uploaded JSON file", e);
			String message = LoggerUtils.exceptionMessage(e) + "\n" + LoggerUtils.stackTrace(e);
			updateUi(() -> importFailed(importing, message));
			return;
		}
		String restarting = Translator.translate("Platform.Restarting", this.capturedLocale);
		updateUi(() -> {
			importing.close();
			close();
			NotificationUtils.centeredStatus(restarting);
		});
		try {
			Thread.sleep(RESTART_NOTICE_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		AuditContext.run(this.importOrigin.actor(), this.importOrigin.cause(),
		        () -> RestartUtils.triggerRestart("JSON import completed"));
	}

	private void importFailed(Notification importing, String message) {
		importing.close();
		this.errors.setValue(message);
		this.errors.setVisible(true);
		this.upload.clearFileList();
		setImporting(false);
	}

	/** The page may have been closed since the dialog was opened; the caller carries on. */
	private void updateUi(Command command) {
		try {
			this.ui.access(command);
		} catch (UIDetachedException e) {
			logger.info("Import Database page no longer open: {}", LoggerUtils.exceptionMessage(e));
		}
	}

}
