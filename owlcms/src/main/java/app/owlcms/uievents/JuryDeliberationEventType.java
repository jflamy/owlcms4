/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

public enum JuryDeliberationEventType {
    START_DELIBERATION, GOOD_LIFT, BAD_LIFT, LOADING_ERROR, CALL_TECHNICAL_CONTROLLER, CALL_REFEREES, END_DELIBERATION,
    TECHNICAL_PAUSE, END_JURY_BREAK, END_CALL_REFEREES, END_TECHNICAL_PAUSE, MARSHALL
}
