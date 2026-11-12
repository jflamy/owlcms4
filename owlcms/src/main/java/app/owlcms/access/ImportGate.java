/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import app.owlcms.access.ImportsNotAllowedException.Reason;
import app.owlcms.audit.ApplicationAudit;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.ConfigRepository;

/**
 * The registration, SBDE and database imports started from the Preparation page: they need the IMPORT_DATA permission
 * ({@link Permission#IMPORT_DATA}) and "Imports are allowed" (Administration page, System tab). The startup load of
 * a checkpoint by the control panel is not an import for this purpose, and records-only mode ignores the setting.
 */
public final class ImportGate {

	/** The imports covered, as recorded in the audit trail. */
	public enum Kind {
		DATABASE, REGISTRATION, SBDE
	}

	private ImportGate() {
	}

	public static boolean isAllowed() {
		Config config = Config.getCurrent();
		return config.isRecordRepository() || config.allowsImports();
	}

	/** @return true if the setting changed */
	public static boolean setAllowed(boolean allowed, AuditOrigin origin) {
		if (Config.getCurrent().allowsImports() == allowed) {
			return false;
		}
		ConfigRepository.apply(c -> c.setImportsAllowed(allowed), origin);
		ApplicationAudit.importsAllowedChanged(origin, allowed);
		return true;
	}

	/**
	 * Called where the import runs, when the file has arrived: a dialog opened while imports were allowed is refused
	 * if they were turned off since. The refusal is audited.
	 *
	 * @param principal who imports, captured on the UI thread where the import was started
	 */
	public static void requireAllowed(Kind kind, Principal principal, AuditOrigin origin) {
		Reason refusal = null;
		if (!AccessPolicy.hasPermission(principal, Permission.IMPORT_DATA, null)) {
			refusal = Reason.NO_PERMISSION;
		} else if (!isAllowed()) {
			refusal = Reason.NOT_ALLOWED;
		}
		if (refusal == null) {
			return;
		}
		ApplicationAudit.importRefused(origin, kind.name().toLowerCase(), refusal.auditValue());
		throw new ImportsNotAllowedException(kind, refusal);
	}
}
