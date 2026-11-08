/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.shared;

import app.owlcms.apputils.NotificationUtils;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;

/**
 * Checks, when a form is opened or saved, whether the athlete's session is in progress on a platform (see
 * {@link FieldOfPlay#isSessionInProgress()}). The platform keeps its own copies of these athletes and would overwrite
 * changes made elsewhere. Nothing is stored: once the session is unselected, the check passes again.
 */
public final class LiftingSessionGuard {

	/** Message key; {0} is the session, {1} the platform. */
	public static final String CANNOT_EDIT_REGISTRATION = "SessionInProgress.CannotEditRegistration";
	/**
	 * Message key; {0} is the session, {1} the platform. A full registration form was closed because its athlete's
	 * session started; its unsaved changes were discarded.
	 */
	public static final String REGISTRATION_CLOSED = "SessionInProgress.RegistrationClosed";
	/**
	 * Message key; {0} is the session, {1} the platform. A full registration form could not be saved because its
	 * session is in progress; the restricted form is offered instead when the athlete is opened again.
	 */
	public static final String CANNOT_SAVE_REGISTRATION = "SessionInProgress.CannotSaveRegistration";
	/** Message key; {0} is the session, {1} the platform. Shown at the top of the restricted form. */
	public static final String SESSION_IN_PROGRESS_NOTICE = "Athlete.SessionInProgressNotice";
	/** Message key; {0} is the session, {1} the platform. */
	public static final String CANNOT_EDIT_RESULTS = "SessionInProgress.CannotEditResults";

	private LiftingSessionGuard() {
	}

	/**
	 * @param group      the session of the athlete(s) to be edited
	 * @param messageKey {@link #CANNOT_EDIT_REGISTRATION} or {@link #CANNOT_EDIT_RESULTS}
	 * @return true, after notifying the user, if the session is in progress on a platform
	 */
	public static boolean refuseIfLifting(Group group, String messageKey) {
		FieldOfPlay fop = OwlcmsFactory.getFOPSessionInProgress(group);
		if (fop == null) {
			return false;
		}
		notifyLifting(fop, messageKey);
		return true;
	}

	public static void notifyLifting(FieldOfPlay fop, String messageKey) {
		NotificationUtils.centeredError(message(fop, messageKey));
	}

	/** @return the translated message with the session as {0} and the platform as {1}. */
	public static String message(FieldOfPlay fop, String messageKey) {
		Group group = fop.getGroup();
		String session = group != null ? group.getName() : "";
		return Translator.translate(messageKey, session, fop.getName());
	}
}
