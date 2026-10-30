package app.owlcms.nui.admin;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import app.owlcms.audit.AuditIntegrityChecker;
import app.owlcms.audit.AuditIntegrityChecker.KeyProtection;
import app.owlcms.audit.AuditIntegrityChecker.Report;
import app.owlcms.audit.AuditIntegrityReportText;
import app.owlcms.audit.AuditIntegrityReportText.FingerprintSummary;
import app.owlcms.audit.AuditLog;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.OwlcmsLayout;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@RequiresRole(Role.ADMIN_PAGES)
@Route(value = "admin/audit/check", layout = OwlcmsLayout.class)
public class AuditIntegrityCheckView extends Composite<VerticalLayout>
		implements HasDynamicTitle {
	private static final Logger logger = (Logger) LoggerFactory.getLogger(AuditIntegrityCheckView.class);
	/** One check at a time for the whole server: reading and verifying large logs is I/O and CPU bound. */
	private static final Semaphore RUNNING = new Semaphore(1);
	private final TextField evidence = new TextField(Translator.translate("AuditCheck.Evidence"));
	private final ControlPanelTokenField token = new ControlPanelTokenField();
	private final Button check = new Button(Translator.translate("AuditCheck.Check"));
	private final Paragraph status = new Paragraph();
	private final Div result = new Div();

	public AuditIntegrityCheckView() {
		this.evidence.setValue(AuditLog.DIRECTORY.toAbsolutePath().normalize().toString());
		this.evidence.setWidthFull();
		this.check.addClickShortcut(Key.ENTER);
		this.check.addClickListener(event -> runCheck());
		this.result.setWidthFull();
		getContent().add(new H2(getPageTitle()),
				new Paragraph(Translator.translate("AuditCheck.Description")),
				new Paragraph(Translator.translate("AuditCheck.Caution")),
				this.evidence, this.token, this.check, this.status, this.result);
	}

	// runs on the request thread, like the challenge it starts with
	private void runCheck() {
		String value = this.evidence.getValue() == null ? "" : this.evidence.getValue().strip();
		if (value.isEmpty()) {
			this.status.setText(Translator.translate("AuditCheck.NoEvidence"));
			return;
		}
		// the check reads any path on the server's disk: the cp challenge comes first, at each check
		if (!this.token.passes("audit integrity check")) {
			return;
		}
		if (!RUNNING.tryAcquire()) {
			this.status.setText(Translator.translate("AuditCheck.Busy"));
			return;
		}
		UI ui = getUI().orElseThrow();
		Locale locale = OwlcmsSession.getLocale();
		Function<String, String> label = key -> Translator.translate(key, locale);
		String failedText = label.apply("AuditCheck.Failed");
		this.check.setEnabled(false);
		this.result.removeAll();
		this.status.setText(label.apply("AuditCheck.Checking"));
		Thread.ofVirtual().name("AuditIntegrityCheck").start(() -> {
			try {
				Report report = AuditIntegrityChecker.check(Path.of(value), AuditIntegrityChecker.Inventory.NONE,
						AuditLog.openFiles());
				String details = AuditIntegrityReportText.details(report, label);
				List<FingerprintSummary> fingerprints = AuditIntegrityReportText.fingerprints(report, label,
						ZoneId.systemDefault());
				ui.access(() -> {
					this.check.setEnabled(true);
					this.status.setText("");
					showReport(report, details, fingerprints, label);
				});
			} catch (IOException | InvalidPathException e) {
				logger.info("Audit integrity check could not read {}: {}", value, e.getMessage());
				ui.access(() -> {
					this.check.setEnabled(true);
					this.status.setText(failedText + " " + e.getMessage());
				});
			} catch (RuntimeException e) {
				logger.error("Audit integrity check failed for {}", value, e);
				ui.access(() -> {
					this.check.setEnabled(true);
					this.status.setText(failedText + " " + e.getMessage());
				});
			} finally {
				RUNNING.release();
			}
		});
	}

	private void showReport(Report report, String details, List<FingerprintSummary> fingerprints,
			Function<String, String> label) {
		this.result.add(dimension(label, "AuditCheck.Integrity", report.integrity().name()),
				dimension(label, "AuditCheck.Coverage", report.coverage().name()),
				dimension(label, "AuditCheck.KeyProtection", report.keyProtection().name()));
		if (report.integrity() == AuditIntegrityChecker.Integrity.FAILED) {
			this.result.add(alert(label.apply("AuditCheck.AlertFailed"), "var(--lumo-error-text-color)"));
		}
		if (report.keyProtection() == KeyProtection.BUILTIN || report.keyProtection() == KeyProtection.MIXED) {
			this.result.add(alert(label.apply("AuditCheck.Alert" + report.keyProtection().name()),
					"var(--lumo-warning-text-color, var(--lumo-error-text-color))"));
		}
		if (!details.isBlank()) {
			this.result.add(preformatted(details));
		}
		if (!fingerprints.isEmpty()) {
			this.result.add(new H3(label.apply("AuditCheck.FingerprintSummary")),
					new Paragraph(label.apply("AuditCheck.CompareFingerprint")));
			for (FingerprintSummary fingerprint : fingerprints) {
				Pre value = preformatted(fingerprint.fingerprint());
				value.getStyle().set("font-size", "var(--lumo-font-size-xl)").set("font-weight", "bold");
				this.result.add(value);
				if (fingerprint.builtinKey()) {
					this.result.add(new Paragraph(label.apply("AuditIntegrity.Builtin")));
				}
				this.result.add(preformatted(String.join("\n", fingerprint.ranges())));
			}
		}
	}

	private static Paragraph dimension(Function<String, String> label, String key, String code) {
		Span name = new Span(label.apply(key) + ": ");
		Span value = new Span(code);
		value.getStyle().set("font-weight", "bold");
		return new Paragraph(name, value, new Span(" — " + label.apply(key + "." + code)));
	}

	private static Paragraph alert(String text, String color) {
		Paragraph paragraph = new Paragraph(text);
		paragraph.getStyle().set("color", color).set("font-weight", "bold");
		return paragraph;
	}

	private static Pre preformatted(String text) {
		Pre pre = new Pre(text);
		pre.getStyle().set("white-space", "pre-wrap");
		return pre;
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("AuditIntegrity.CheckTitle");
	}
}
