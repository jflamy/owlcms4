/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.agegroup.reassign;

/**
 * Why an athlete fits no category of a championship after a definition change. Order in the reassignment dialog.
 */
public enum LeaveReason {
	AGE,
	GENDER,
	QT,
	INACTIVE,
	MISSING_BIRTH_DATE,
	MISSING_BODY_WEIGHT
}
