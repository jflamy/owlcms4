/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import java.util.List;

import app.owlcms.access.ImportLatch.ActiveSession;

/** An import was refused by the permission, administration, or live-session gate. */
@SuppressWarnings("serial")
public class ImportsNotAllowedException extends RuntimeException {

	public enum Reason {
		NO_PERMISSION("no-import-permission", "Imports.NotPermitted"),
		NOT_ALLOWED("imports-not-allowed", "Imports.NotAllowed"),
		SESSION_IN_PROGRESS("SESSION_IN_PROGRESS", "SessionInProgress.CannotEditRegistration");

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
	private final List<ActiveSession> activeSessions;

	public ImportsNotAllowedException(ImportGate.Kind kind, Reason reason) {
		this(kind, reason, List.of());
	}

	public ImportsNotAllowedException(ImportGate.Kind kind, Reason reason, List<ActiveSession> activeSessions) {
		super(kind + " import refused: " + reason.auditValue());
		this.kind = kind;
		this.reason = reason;
		this.activeSessions = List.copyOf(activeSessions);
	}

	public ImportGate.Kind getKind() {
		return this.kind;
	}

	public Reason getReason() {
		return this.reason;
	}

	public List<ActiveSession> getActiveSessions() {
		return this.activeSessions;
	}
}
