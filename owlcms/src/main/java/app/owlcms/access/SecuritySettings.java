package app.owlcms.access;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.config.Config;
import app.owlcms.utils.StartupUtils;

/**
 * The security settings of the Administration page: the access mode (PIN or user accounts), which needs the control
 * panel token (cp challenge, asked by the page), and the IWF compliance check, which only shows the requirements not
 * met (see {@link IwfComplianceCheck}).
 */
public final class SecuritySettings {

	public enum Outcome {
		CHANGED,
		UNCHANGED,
		/** A startup parameter (OWLCMS_ACCESSMODE or OWLCMS_IWFCOMPLIANCE) sets the value. */
		SET_AT_STARTUP,
		ADMIN_PASSWORD_REQUIRED
	}

	private SecuritySettings() {
	}

	/** Takes effect at once; the caller ends the sessions. Audited as an access-mode change when the settings are saved. */
	public static Outcome switchAccessMode(AccessMode target, AuditOrigin origin) {
		String override = StartupUtils.getStringParam("accessMode");
		if (override != null && !override.isBlank()) {
			return Outcome.SET_AT_STARTUP;
		}
		Config config = Config.getCurrent();
		if (config.getParamAccessMode() == target) {
			return Outcome.UNCHANGED;
		}
		if (target == AccessMode.ACCOUNTS && !UserAccountRepository.adminHasPassword()) {
			return Outcome.ADMIN_PASSWORD_REQUIRED;
		}
		config.chooseAccessMode(target);
		Config.setCurrent(config, origin);
		return Outcome.CHANGED;
	}

	/** Takes effect at once: the check is evaluated each time a page shows it. */
	public static Outcome setIwfComplianceCheck(boolean on, AuditOrigin origin) {
		if (Config.isIwfComplianceSetAtStartup()) {
			return Outcome.SET_AT_STARTUP;
		}
		Config config = Config.getCurrent();
		if (config.isIwfComplianceChecked() == on) {
			return Outcome.UNCHANGED;
		}
		config.setIwfCompliance(on);
		Config.setCurrent(config, origin);
		AuthAudit.iwfComplianceCheckChanged(origin, on);
		return Outcome.CHANGED;
	}
}
