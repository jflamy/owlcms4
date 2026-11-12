/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

/** An import was refused: no IMPORT_DATA permission, or imports are turned off on the Administration page. */
@SuppressWarnings("serial")
public class ImportsNotAllowedException extends RuntimeException {

	public enum Reason {
		NO_PERMISSION("no-import-permission", "Imports.NotPermitted"),
		NOT_ALLOWED("imports-not-allowed", "Imports.NotAllowed");

		private final String auditValue;
		private final String messageKey;

		Reason(String auditValue, String messageKey) {
			this.auditValue = auditValue;
			this.messageKey = messageKey;
		}

		public String auditValue() {
			return this.auditValue;
		}

		/** Translation key of the message shown to the user. */
		public String messageKey() {
			return this.messageKey;
		}
	}

	private final ImportGate.Kind kind;
	private final Reason reason;

	public ImportsNotAllowedException(ImportGate.Kind kind, Reason reason) {
		super(kind + " import refused: " + reason.auditValue());
		this.kind = kind;
		this.reason = reason;
	}

	public ImportGate.Kind getKind() {
		return this.kind;
	}

	public Reason getReason() {
		return this.reason;
	}
}
