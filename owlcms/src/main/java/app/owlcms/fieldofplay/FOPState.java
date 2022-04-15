/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

/**
 * Current state of the competition field of play.
 */
public enum FOPState {
    /**
     * during countdown to presentation or first lift and during breaks. Note that BreakType describes a substate.
     *
     * @see app.owlcms.uievents.BreakType
     */
    BREAK,

    /** current athlete displayed on attempt board. */
    CURRENT_ATHLETE_DISPLAYED,

    /** The decision is visible. */
    DECISION_VISIBLE,

    /** The down signal is visible. */
    DOWN_SIGNAL_VISIBLE,

    /** between sessions, until presentation countdown is shown */
    INACTIVE,

    /**
     * time is running. Either automatically started on announce (if using the default "start on announce", or manually
     * by timekeeper (in traditional mode)
     */
    TIME_RUNNING,

    /** The time is stopped. */
    TIME_STOPPED,
}