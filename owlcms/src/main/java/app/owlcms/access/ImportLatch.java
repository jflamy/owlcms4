/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import java.text.Collator;
import java.text.ListFormat;
import java.util.List;
import java.util.Locale;

import app.owlcms.access.ImportGate.Kind;
import app.owlcms.access.ImportsNotAllowedException.Reason;
import app.owlcms.audit.ApplicationAudit;
import app.owlcms.audit.AuditOrigin;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;

/** Live checks for imports that cannot safely run while a platform owns a lifting session. */
public final class ImportLatch {

	/** Identifies the selected session and the platform that owns its lifting state. */
	public record ActiveSession(Long groupId, String sessionName, String platformName) {
	}

	private ImportLatch() {
	}

	/** @return every currently selected lifting session, resolved from the live platform registry. */
	public static List<ActiveSession> activeSessions() {
		return OwlcmsFactory.getFOPsWithSessionInProgress().stream()
		        .map(ImportLatch::activeSession)
		        .toList();
	}

	public static boolean hasActiveSession() {
		return !activeSessions().isEmpty();
	}

	/** @return distinct active platform names, sorted for the supplied locale. */
	public static List<String> activePlatformNames(List<ActiveSession> sessions, Locale locale) {
		Collator collator = Collator.getInstance(locale);
		return sessions.stream()
		        .map(ActiveSession::platformName)
		        .filter(name -> name != null && !name.isBlank())
		        .distinct()
		        .sorted(collator)
		        .toList();
	}

	/** Selects the singular/plural translation and lets Java localize the list conjunction. */
	public static String activePlatformMessage(List<ActiveSession> sessions, Locale locale) {
		List<String> platforms = activePlatformNames(sessions, locale);
		if (platforms.isEmpty()) {
			return "";
		}
		String list = ListFormat.getInstance(locale, ListFormat.Type.STANDARD, ListFormat.Style.FULL)
		        .format(platforms);
		String key = platforms.size() == 1
		        ? "Imports.SessionActiveOnPlatform"
		        : "Imports.SessionsActiveOnPlatforms";
		return Translator.translate(key, locale, list);
	}

	/** Refuse an import immediately before it starts mutating competition data. */
	public static void requireNoActiveSession(Kind kind, AuditOrigin origin) {
		List<ActiveSession> activeSessions = activeSessions();
		if (activeSessions.isEmpty()) {
			return;
		}
		ApplicationAudit.importRefused(origin, kind.name().toLowerCase(), Reason.SESSION_IN_PROGRESS.auditValue(),
		        activeSessions.stream().map(ActiveSession::sessionName).toList(),
		        activeSessions.stream().map(ActiveSession::platformName).toList());
		throw new ImportsNotAllowedException(kind, Reason.SESSION_IN_PROGRESS, activeSessions);
	}

	private static ActiveSession activeSession(FieldOfPlay fop) {
		Group group = fop.getGroup();
		return new ActiveSession(
		        group != null ? group.getId() : null,
		        group != null ? group.getName() : "",
		        fop.getName());
	}
}
