package app.owlcms.nui.admin;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.markdown.Markdown;
import com.vaadin.flow.component.notification.Notification;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditKeyFiles;
import app.owlcms.audit.AuditSigningKey;
import app.owlcms.components.ConfirmationDialog;
import app.owlcms.i18n.Translator;
import app.owlcms.utils.RestartUtils;
import ch.qos.logback.classic.Logger;

/**
 * Audit key initialization, opened from the admin page, in two steps: (A) confirmation with the cp challenge, then
 * generation of a pending key; (B) the new fingerprint, to be photographed, and a restart to activate it.
 */
final class AuditKeyDialog {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditKeyDialog.class);

	private AuditKeyDialog() {
	}

	/**
	 * @param ui    the admin page UI, captured on the UI thread
	 * @param actor the operator, captured on the UI thread, for the restart audit entry
	 */
	static void open(UI ui, AuditActor actor) {
		if (!AuditIntegrity.isInitialized()) {
			NotificationUtils.errorNotification(Translator.translate("AuditIntegrity.RestartRequired"));
			return;
		}
		AuditIntegrity.State state = AuditIntegrity.current();
		if (state.environmentKey()) {
			NotificationUtils.errorNotification(Translator.translate("AuditIntegrity.EnvironmentOverride"));
			return;
		}
		// the pending key file is read off the UI thread
		Thread.ofVirtual().name("AuditKeyStatus").start(() -> {
			Optional<AuditKeyFiles.PendingKey> pending;
			try {
				pending = AuditKeyFiles.readPending(state.keyPath());
			} catch (IOException e) {
				logger.error("Unable to read the pending audit key", e);
				pending = Optional.of(new AuditKeyFiles.PendingKey(null, e.getMessage()));
			}
			Optional<AuditKeyFiles.PendingKey> result = pending;
			ui.access(() -> showGenerationDialog(ui, actor, state, result));
		});
	}

	/** Step A: confirmation with the cp challenge. No fingerprint here: the new key does not exist yet. */
	private static void showGenerationDialog(UI ui, AuditActor actor, AuditIntegrity.State state,
	        Optional<AuditKeyFiles.PendingKey> pending) {
		boolean replace = pending.isPresent();
		String generateLabel = Translator.translate(replace ? "AuditIntegrity.Replace" : "AuditIntegrity.Generate");
		ControlPanelTokenField token = new ControlPanelTokenField();

		ConfirmationDialog dialog = new ConfirmationDialog(Translator.translate("AuditIntegrity.PrepareTitle"), null,
		        generateLabel, null, null, () -> generate(ui, actor, state, replace));
		Markdown explanation = new Markdown(Translator.translate("AuditIntegrity.PrepareConfirmation"));
		explanation.setWidth("550px");
		dialog.addBody(explanation);
		dialog.addBody(token);
		dialog.setConfirmCheck(() -> token.passes("audit key generation"));
		dialog.open();
	}

	private static Pre fingerprint(AuditSigningKey key) {
		Pre fingerprint = new Pre(key.displayFingerprint());
		fingerprint.getStyle().set("white-space", "pre-wrap");
		return fingerprint;
	}

	private static void generate(UI ui, AuditActor actor, AuditIntegrity.State state, boolean replace) {
		String failed = Translator.translate("AuditIntegrity.PrepareFailed");
		Thread.ofVirtual().name("AuditKeyPreparation").start(() -> {
			try {
				AuditSigningKey key = AuditKeyFiles.prepare(state.keyPath(), replace);
				logger.info("Pending audit key prepared fingerprint={} replaced={}", key.fingerprint(), replace);
				ui.access(() -> confirmRestart(ui, actor, state, key));
			} catch (IOException | RuntimeException e) {
				logger.error("Audit key preparation failed", e);
				ui.access(() -> NotificationUtils.errorNotification(failed));
			}
		});
	}

	/**
	 * Step B: the new fingerprint, to be photographed, then a restart (or stop) of OWLCMS to activate it. Cancelling
	 * deletes the pending key, so the current key stays in force.
	 */
	private static void confirmRestart(UI ui, AuditActor actor, AuditIntegrity.State state, AuditSigningKey prepared) {
		boolean restartable = RestartUtils.isRestartScenario();
		String title = Translator.translate(restartable ? "AuditIntegrity.RestartTitle" : "AuditIntegrity.StopTitle");
		ConfirmationDialog dialog = new ConfirmationDialog(title, Translator.translate("AuditIntegrity.PendingKey"),
		        title, Translator.translate("AuditIntegrity.CancelAndDeletePending"), null, () -> {
			        if (restartable) {
				        NotificationUtils.restarting(ui);
			        }
			        Thread.ofVirtual().name("AuditKeyRestart").start(() -> AuditContext.run(actor, "Audit key prepared",
			                () -> RestartUtils.triggerRestart("Audit key prepared")));
		        });
		dialog.addBody(fingerprint(prepared));
		dialog.addBody(new Paragraph(Translator.translate(
		        restartable ? "AuditIntegrity.ReadyToRestart" : "AuditIntegrity.ReadyToStop")));
		dialog.setCancelAction(() -> deletePending(ui, state, prepared));
		dialog.open();
	}

	private static void deletePending(UI ui, AuditIntegrity.State state, AuditSigningKey prepared) {
		String deleted = Translator.translate("AuditIntegrity.PendingDeleted");
		String failed = Translator.translate("AuditIntegrity.PendingDeleteFailed");
		Thread.ofVirtual().name("AuditKeyWithdrawal").start(() -> {
			try {
				AuditKeyFiles.deletePending(state.keyPath());
				logger.info("Pending audit key deleted fingerprint={}", prepared.fingerprint());
				ui.access(() -> Notification.show(deleted));
			} catch (IOException | RuntimeException e) {
				logger.error("Unable to delete the pending audit key", e);
				ui.access(() -> NotificationUtils.errorNotification(failed));
			}
		});
	}
}
