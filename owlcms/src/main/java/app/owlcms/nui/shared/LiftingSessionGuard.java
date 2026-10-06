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
		Group group = fop.getGroup();
		String session = group != null ? group.getName() : "";
		NotificationUtils.centeredError(Translator.translate(messageKey, session, fop.getName()));
	}
}
