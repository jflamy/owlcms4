/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.access;

import java.time.ZoneId;
import java.util.TimeZone;

import app.owlcms.data.account.UserAccount;
import app.owlcms.data.config.Config;

/**
 * Policy shared by remembered account logins.
 */
public final class RememberedAccount {

	private RememberedAccount() {
	}

	/** @return true if this account may stay logged in: enabled, with a password, and not an administrator */
	public static boolean isRememberable(UserAccount account) {
		return account != null && !account.isBuiltInAdmin() && account.isEnabled() && account.hasPassword()
		        && !account.isMustChangePassword()
		        && account.getGrants().stream().noneMatch(grant -> grant.getRole() == Role.ADMIN);
	}

	/** @return the competition time zone when one is set, else the server's */
	public static ZoneId zone(Config config) {
		TimeZone timeZone = config.getTimeZone();
		return timeZone != null ? timeZone.toZoneId() : ZoneId.systemDefault();
	}
}
