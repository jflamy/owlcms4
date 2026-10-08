package app.owlcms.nui.admin;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
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
	private final Pre fingerprint = new Pre();
	private final Button prepare = new Button(Translator.translate("AuditIntegrity.Generate"));
	private boolean preparing;

	public AuditIntegrityPreparationView() {
		VerticalLayout content = getContent();
		content.add(new H2(getPageTitle()), new Paragraph(Translator.translate("AuditIntegrity.PrepareDescription")),
				this.status, this.fingerprint,
				this.prepare);
		this.fingerprint.getStyle().set("white-space", "pre-wrap");
		if (!AuditIntegrity.isInitialized()) {
			this.status.setText(Translator.translate("AuditIntegrity.RestartRequired"));
			this.prepare.setEnabled(false);
			return;
		}
		AuditIntegrity.State state = AuditIntegrity.current();
		if (state.environmentKey()) {
			this.status.setText(Translator.translate("AuditIntegrity.EnvironmentOverride"));
			this.prepare.setEnabled(false);
		} else {
			content.add(new Paragraph(state.keyPath().toString()));
			this.prepare.addClickListener(event -> confirmPreparation());
		}
	}

	private void confirmPreparation() {
		ConfirmDialog confirmation = new ConfirmDialog();
		confirmation.setHeader(getPageTitle());
		confirmation.setText(Translator.translate("AuditIntegrity.PrepareConfirmation"));
		confirmation.setCancelable(true);
		confirmation.setCancelText(Translator.translate("Cancel"));
		confirmation.setConfirmText(Translator.translate("AuditIntegrity.Generate"));
		confirmation.addConfirmListener(event -> prepareKey());
		confirmation.open();
	}

	private void prepareKey() {
		if (this.preparing) {
			return;
		}
		UI ui = getUI().orElseThrow();
		AuditActor actor = AuditActor.capture(this).atStation("ADMIN", null, false);
		AuditIntegrity.State state = AuditIntegrity.current();
		String failed = Translator.translate("AuditIntegrity.PrepareFailed");
		String ready = Translator.translate("AuditIntegrity.ReadyToRestart");
		String restartTitle = Translator.translate("AuditIntegrity.RestartTitle");
		String restartLabel = Translator.translate("AuditIntegrity.RestartTitle");
		this.preparing = true;
		this.prepare.setEnabled(false);
		this.status.setText(Translator.translate("AuditIntegrity.Generating"));
		Thread.ofVirtual().name("AuditKeyPreparation").start(() -> {
			try {
				AuditSigningKey key = AuditKeyFiles.prepare(state.keyPath());
				ui.access(() -> {
					this.fingerprint.setText(key.displayFingerprint());
					this.status.setText(ready);
					ConfirmDialog restart = new ConfirmDialog();
					restart.setHeader(restartTitle);
					restart.setText(key.displayFingerprint() + "\n\n" + ready);
					restart.setConfirmText(restartLabel);
					restart.addConfirmListener(event -> Thread.ofVirtual().name("AuditKeyRestart").start(
							() -> AuditContext.run(actor, "Audit key prepared",
									() -> RestartUtils.triggerRestart("Audit key prepared"))));
					restart.open();
				});
			} catch (IOException | RuntimeException e) {
				logger.error("Audit key preparation failed", e);
				ui.access(() -> {
					this.status.setText(failed);
					this.preparing = false;
					this.prepare.setEnabled(true);
				});
			}
		});
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
