package app.owlcms.access;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.LoggerFactory;

import app.owlcms.audit.AuditIntegrity;
import app.owlcms.audit.AuditKeyFiles;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import ch.qos.logback.classic.Logger;

/**
 * IWF compliance is a check, not a mode: when it is on, OWLCMS lists the requirements that are not met, on the
 * Administration page, the home page and the Competition Supervisor page. Nothing is enforced.
 */
public final class IwfComplianceCheck {

	public enum Requirement {
		/** Every user logs in with an account. */
		USER_ACCOUNTS("IwfCheck.UserAccounts"),
		/** The audit trail is signed with a real key (a key file, or the key secret in the cloud), not the built-in one. */
		AUDIT_KEY("IwfCheck.AuditKey", "IwfCheck.AfterRestart"),
		/** OWLCMS was started by the control panel (or, in the cloud, with the token secret). */
		CONTROL_PANEL_TOKEN("IwfCheck.ControlPanelToken"),
		/** The TCRR rules are applied ({@code tcrrCompliance} feature switch). */
		TCRR_RULES("IwfCheck.TcrrRules", "IwfCheck.TcrrAfterRestart"),
		/**
		 * Every database export offered is signed: the {@code noV1Export} feature switch withdraws the unsigned V1
		 * export and turns on V2, which is always signed.
		 */
		SIGNED_EXPORTS("IwfCheck.SignedExports");

		private final String translationKey;
		private final String afterRestartKey;

		Requirement(String translationKey) {
			this(translationKey, null);
		}

		Requirement(String translationKey, String afterRestartKey) {
			this.translationKey = translationKey;
			this.afterRestartKey = afterRestartKey;
		}

		public String getTranslationKey() {
			return this.translationKey;
		}

		/** @return the explanation shown with {@link Status#AFTER_RESTART}, null if the status does not apply */
		public String getAfterRestartKey() {
			return this.afterRestartKey;
		}
	}

	public enum Status {
		MET,
		NOT_MET,
		/** The change is made (a new audit key, the TCRR switch) but only takes effect after the next restart. */
		AFTER_RESTART
	}

	public record Item(Requirement requirement, Status status) {
		public boolean isMet() {
			return this.status == Status.MET;
		}
	}

	private static final Logger logger = (Logger) LoggerFactory.getLogger(IwfComplianceCheck.class);

	private IwfComplianceCheck() {
	}

	/** @return every requirement with its status, whether the check is on or not */
	public static List<Item> evaluate() {
		AuditIntegrity.State state = AuditIntegrity.isInitialized() ? AuditIntegrity.current() : null;
		boolean realKey = state != null && !state.signingKey().isBuiltin();
		return evaluate(Config.getCurrent().isAccountsMode(), realKey, pendingKey(state),
		        SameMachineProof.configuredToken() != null, FeatureSwitch.TCRR_COMPLIANCE.isEffective(),
		        Config.getCurrent().requestedFeatureSwitch(FeatureSwitch.TCRR_COMPLIANCE),
		        FeatureSwitch.NO_V1_EXPORT.isEffective());
	}

	private static boolean pendingKey(AuditIntegrity.State state) {
		Path keyPath = state != null ? state.keyPath() : null;
		if (keyPath == null) {
			return false;
		}
		try {
			return AuditKeyFiles.exists(AuditKeyFiles.pendingPath(keyPath));
		} catch (IOException e) {
			logger./**/warn("cannot check for a pending audit key: {}", e.getMessage());
			return false;
		}
	}

	static List<Item> evaluate(boolean accountsMode, boolean realKey, boolean pendingKey, boolean controlPanelToken,
	        boolean tcrrRules, boolean tcrrRequested, boolean signedExports) {
		return List.of(
		        new Item(Requirement.USER_ACCOUNTS, met(accountsMode)),
		        new Item(Requirement.AUDIT_KEY, met(realKey, pendingKey)),
		        new Item(Requirement.CONTROL_PANEL_TOKEN, met(controlPanelToken)),
		        new Item(Requirement.TCRR_RULES, met(tcrrRules, tcrrRequested)),
		        new Item(Requirement.SIGNED_EXPORTS, met(signedExports)));
	}

	private static Status met(boolean condition) {
		return condition ? Status.MET : Status.NOT_MET;
	}

	private static Status met(boolean inEffect, boolean afterRestart) {
		return inEffect ? Status.MET : afterRestart ? Status.AFTER_RESTART : Status.NOT_MET;
	}

	/** @return the requirements not met when the check is on, else an empty list */
	public static List<Item> problems() {
		if (!Config.getCurrent().isIwfComplianceChecked()) {
			return List.of();
		}
		return evaluate().stream().filter(item -> !item.isMet()).toList();
	}

	/** Logs and audits the result at startup, so the evidence shows how the competition was run. */
	public static void reportAtStartup() {
		if (!Config.getCurrent().isIwfComplianceChecked()) {
			return;
		}
		List<Item> problems = problems();
		if (problems.isEmpty()) {
			logger.info("IWF compliance check: all requirements met");
		} else {
			logger./**/warn("IWF compliance check: requirements not met: {}", problems);
		}
		AuthAudit.iwfComplianceChecked(AuditOrigin.system("startup"),
		        problems.stream().map(item -> item.requirement().name()).toList());
	}
}
