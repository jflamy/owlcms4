/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

/**
 * Current state of the competition field of play.
 */
public enum FOPState {
    /** between sessions, until presentation countdown is shown */
    INACTIVE,

    /** during countdown to presentation or first lift and during breaks. */
    BREAK,

    /** current athlete displayed on attempt board. */
    CURRENT_ATHLETE_DISPLAYED,

//	/**
//	 * announcer has announced athlete and indicated so, waiting for timekeeper to start time.
//	 */
//	ANNOUNCER_WAITING_FOR_TIMEKEEPER,
//
//	/**
//	 * timekeeper waiting for announcer to confirm she has announced.
//	 */
//	TIMEKEEPER_WAITING_FOR_ANNOUNCER,

    /**
     * time is running. Either automatically started on announce (if using the default "start on announce", or manually
     * by timekeeper (in traditional mode)
     */
    TIME_RUNNING,

    /** The time is stopped. */
    TIME_STOPPED,

    /** The down signal is visible. */
    DOWN_SIGNAL_VISIBLE,

    /** The decision is visible. */
    DECISION_VISIBLE,
}