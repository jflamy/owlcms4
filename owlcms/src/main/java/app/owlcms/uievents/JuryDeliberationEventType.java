/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

public enum JuryDeliberationEventType {
    BAD_LIFT, CALL_REFEREES, CALL_TECHNICAL_CONTROLLER, END_CALL_REFEREES, END_DELIBERATION, END_JURY_BREAK,
    END_TECHNICAL_PAUSE,
    GOOD_LIFT, LOADING_ERROR, START_DELIBERATION, TECHNICAL_PAUSE
}
