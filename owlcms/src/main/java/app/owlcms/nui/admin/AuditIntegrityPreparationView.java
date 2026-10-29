package app.owlcms.nui.admin;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.apputils.NotificationUtils;
import app.owlcms.audit.AuditActor;
import app.owlcms.audit.AuditContext;
import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditKeyFiles;
import app.owlcms.audit.AuditSigningKey;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.RestartUtils;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "admin/audit/prepare", layout = OwlcmsLayout.class)
public class AuditIntegrityPreparationView extends Composite<VerticalLayout>
		implements BeforeEnterObserver, HasDynamicTitle {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditIntegrityPreparationView.class);
	private final Paragraph status = new Paragraph();
	private final Div pendingArea = new Div();
	private final Button generate = new Button(Translator.translate("AuditIntegrity.Generate"));
	private final Button restart = new Button();
	private final boolean restartable = RestartUtils.isRestartScenario();
	private AuditIntegrity.State state;
	private boolean pendingPresent;
	private boolean busy;

	public AuditIntegrityPreparationView() {
		VerticalLayout content = getContent();
		content.add(new H2(getPageTitle()), new Paragraph(Translator.translate("AuditIntegrity.PrepareDescription")));
		this.generate.setEnabled(false);
		this.restart.setText(Translator.translate(this.restartable ? "AuditIntegrity.RestartTitle"
				: "AuditIntegrity.StopTitle"));
		this.restart.setVisible(false);
		if (!AuditIntegrity.isInitialized()) {
			this.status.setText(Translator.translate("AuditIntegrity.RestartRequired"));
			content.add(this.status);
			return;
		}
		this.state = AuditIntegrity.current();
		if (this.state.environmentKey()) {
			this.status.setText(Translator.translate("AuditIntegrity.EnvironmentOverride"));
			content.add(activeKeyArea(), this.status);
			return;
		}
		content.add(activeKeyArea(), new Paragraph(Translator.translate("AuditIntegrity.KeyFile")),
				new Pre(this.state.keyPath().toString()), this.pendingArea, this.status,
				new HorizontalLayout(this.generate, this.restart));
		this.generate.addClickListener(event -> confirmGeneration());
		this.restart.addClickListener(event -> confirmRestart(null));
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		if (this.state == null || this.state.environmentKey()) {
			return;
		}
		UI ui = attachEvent.getUI();
		Thread.ofVirtual().name("AuditKeyStatus").start(() -> {
			Optional<AuditKeyFiles.PendingKey> pending;
			try {
				pending = AuditKeyFiles.readPending(this.state.keyPath());
			} catch (IOException e) {
				logger.error("Unable to read the pending audit key", e);
				pending = Optional.of(new AuditKeyFiles.PendingKey(null, e.getMessage()));
			}
			Optional<AuditKeyFiles.PendingKey> result = pending;
			ui.access(() -> showPending(result));
		});
	}

	private Div activeKeyArea() {
		Div area = new Div();
		AuditSigningKey key = this.state.signingKey();
		area.add(new Paragraph(Translator.translate("AuditIntegrity.ActiveKey")), fingerprint(key));
		if (key.isBuiltin()) {
			area.add(new Paragraph(Translator.translate("AuditIntegrity.Builtin")));
		}
		return area;
	}

	private void showPending(Optional<AuditKeyFiles.PendingKey> pending) {
		this.pendingArea.removeAll();
		this.pendingPresent = pending.isPresent();
		pending.ifPresent(p -> {
			if (p.valid()) {
				this.pendingArea.add(new Paragraph(Translator.translate("AuditIntegrity.PendingKey")),
						fingerprint(p.key()));
			} else {
				this.pendingArea.add(new Paragraph(Translator.translate("AuditIntegrity.PendingInvalid")),
						new Pre(String.valueOf(p.error())));
			}
		});
		this.generate.setText(Translator.translate(
				this.pendingPresent ? "AuditIntegrity.Replace" : "AuditIntegrity.Generate"));
		this.generate.setEnabled(!this.busy);
		this.restart.setVisible(pending.map(AuditKeyFiles.PendingKey::valid).orElse(false));
	}

	private static Pre fingerprint(AuditSigningKey key) {
		Pre fingerprint = new Pre(key.displayFingerprint());
		fingerprint.getStyle().set("white-space", "pre-wrap");
		return fingerprint;
	}

	private void confirmGeneration() {
		ConfirmDialog confirmation = new ConfirmDialog();
		confirmation.setHeader(getPageTitle());
		confirmation.setText(Translator.translate(
				this.pendingPresent ? "AuditIntegrity.ReplaceConfirmation" : "AuditIntegrity.PrepareConfirmation"));
		confirmation.setCancelable(true);
		confirmation.setCancelText(Translator.translate("Cancel"));
		confirmation.setConfirmText(this.generate.getText());
		confirmation.addConfirmListener(event -> generateKey());
		confirmation.open();
	}

	private void generateKey() {
		if (this.busy) {
			return;
		}
		UI ui = getUI().orElseThrow();
		boolean replace = this.pendingPresent;
		String failed = Translator.translate("AuditIntegrity.PrepareFailed");
		this.busy = true;
		this.generate.setEnabled(false);
		this.restart.setVisible(false);
		this.status.setText(Translator.translate("AuditIntegrity.Generating"));
		Thread.ofVirtual().name("AuditKeyPreparation").start(() -> {
			try {
				AuditSigningKey key = AuditKeyFiles.prepare(this.state.keyPath(), replace);
				logger.info("Pending audit key prepared fingerprint={} replaced={}", key.fingerprint(), replace);
				ui.access(() -> {
					this.busy = false;
					this.status.setText("");
					showPending(Optional.of(new AuditKeyFiles.PendingKey(key, null)));
					confirmRestart(key);
				});
			} catch (IOException | RuntimeException e) {
				logger.error("Audit key preparation failed", e);
				ui.access(() -> {
					this.busy = false;
					this.status.setText(failed);
					this.generate.setEnabled(true);
				});
			}
		});
	}

	/** @param prepared the key just generated, shown in the dialog so it can be photographed; null otherwise */
	private void confirmRestart(AuditSigningKey prepared) {
		AuditActor actor = AuditActor.capture(this).atStation("ADMIN", null, false);
		String instructions = Translator.translate(this.restartable ? "AuditIntegrity.ReadyToRestart"
				: "AuditIntegrity.ReadyToStop");
		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setHeader(this.restart.getText());
		dialog.setText(prepared != null ? prepared.displayFingerprint() + "\n\n" + instructions : instructions);
		dialog.setCancelable(true);
		dialog.setCancelText(Translator.translate("AuditIntegrity.Later"));
		dialog.setConfirmText(this.restart.getText());
		dialog.addConfirmListener(event -> {
			if (this.restartable) {
				NotificationUtils.restarting(UI.getCurrent());
			}
			Thread.ofVirtual().name("AuditKeyRestart").start(
					() -> AuditContext.run(actor, "Audit key prepared",
							() -> RestartUtils.triggerRestart("Audit key prepared")));
		});
		dialog.open();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AdminView.checkLocalAccess();
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("AuditIntegrity.PrepareTitle");
	}
}
