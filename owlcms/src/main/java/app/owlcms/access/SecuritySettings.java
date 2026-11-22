package app.owlcms.access;

import app.owlcms.audit.AuditOrigin;
import app.owlcms.audit.AuthAudit;
import app.owlcms.data.account.UserAccountRepository;
import app.owlcms.data.access.RememberedLoginRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;
import app.owlcms.data.config.FeatureSwitch;

/**
 * The security settings of the Administration page: the access mode (PIN or user accounts), which needs the control
 * panel token (cp challenge, asked by the page), whether PIN and account logins may be remembered until midnight, and
 * the IWF compliance check, which only shows the requirements not met (see {@link IwfComplianceCheck}).
 */
public final class SecuritySettings {

	public enum Outcome {
		CHANGED,
		UNCHANGED,
		ADMIN_PASSWORD_REQUIRED
	}

	private SecuritySettings() {
	}

	/** Takes effect at once; the caller ends the sessions. Audited as an access-mode change when the settings are saved. */
	public static Outcome switchAccessMode(AccessMode target, AuditOrigin origin) {
		Config config = Config.getCurrent();
		if (config.getParamAccessMode() == target) {
			return Outcome.UNCHANGED;
		}
		if (target == AccessMode.ACCOUNTS && !UserAccountRepository.adminHasPassword()) {
			return Outcome.ADMIN_PASSWORD_REQUIRED;
		}
		ConfigRepository.apply(c -> c.setAccessMode(target), origin);
		RememberedLoginRepository.deleteAll();
		return Outcome.CHANGED;
	}

	/** Takes effect at once: the check is evaluated each time a page shows it. */
	public static Outcome setIwfComplianceCheck(boolean on, AuditOrigin origin) {
		Config config = Config.getCurrent();
		if (config.isIwfComplianceChecked() == on) {
			return Outcome.UNCHANGED;
		}
		ConfigRepository.apply(c -> c.setIwfCompliance(on), origin);
		AuthAudit.iwfComplianceCheckChanged(origin, on);
		return Outcome.CHANGED;
	}

	/** Takes effect at once: turning it off makes remembered PIN and account logins unusable. */
	public static Outcome setRememberAccountLogins(boolean allowed, AuditOrigin origin) {
		if (Config.getCurrent().allowsRememberedLogins() == allowed) {
			return Outcome.UNCHANGED;
		}
		ConfigRepository.apply(c -> c.setRememberAccountLogins(allowed), origin);
		AuthAudit.rememberAccountLoginsChanged(origin, allowed);
		return Outcome.CHANGED;
	}

	/** Applies one of the IWF feature switches shown in the compliance dialog. */
	public static Outcome setIwfFeatureSwitch(FeatureSwitch featureSwitch, boolean enabled, AuditOrigin origin) {
		if (featureSwitch != FeatureSwitch.TCRR_COMPLIANCE && featureSwitch != FeatureSwitch.NO_V1_EXPORT) {
			throw new IllegalArgumentException("not an IWF feature switch: " + featureSwitch);
		}
		if (featureSwitch.isEffective() == enabled) {
			return Outcome.UNCHANGED;
		}
		ConfigRepository.apply(c -> c.setFeatureSwitchValue(featureSwitch, enabled), origin);
		return Outcome.CHANGED;
	}
}
