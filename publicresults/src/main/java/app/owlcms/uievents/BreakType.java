/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

/**
 * <p>
 * A break type is either a countdown, or a ceremony taking place during a
 * countdown. When a ceremony is taking place, a countdown timer MUST be running
 * with the state that we go to when the ceremony ends.
 * </p>
 * <p>
 * Rule: because it is possible to hide/show the break dialog, the break timer
 * shown on entry is the one already set or running. So the timer must be set at
 * the end of the previous break. E.g. end of BEFORE_INTRODUCTION sets
 * BEFORE_SNATCH 10:00 minutes as the duration, and pauses the timer.
 * </p>
 * <ol>
 * <li>Medals for group 1 before introduction of group 2, countdown started
 * *after* medals
 *
 * <pre>
 * GROUP_DONE (indefinite timer, end break = resume current group that was just done)
 * start medals
 * GROUP_DONE MEDALS (ceremony, GROUP_DONE indefinite timer, display switches to medals for group that was just done)
 * end medals
 * GROUP_DONE (indefinite timer, manual end break = go to group that just finished and medaled - shows results)
 * click intro timer
 * BEFORE_INTRODUCTION (default to next 30 minute wall clock, end break = switch to BEFORE_SNATCH timer paused 10:00)
 * start before intro timer
 * timer ends, automatic switch to BEFORE_SNATCH, timer paused 10:00
 * start intro
 * BEFORE_SNATCH DURING_INTRODUCTION (ceremony)
 * end intro
 * BEFORE_SNATCH (BEFORE_SNATCH timer set for the default 10 minues)
 * start timer
 * start officials intro
 * BEFORE_SNATCH DURING_OFFICIALS_INTRODUCTION (ceremony)
 * end ceremony
 * BEFORE_SNATCH
 * timer ends, automatic switch
 * CURRENT_ATHLETE_DISPLAYED
 * </pre>
 *
 * <li>Medals for group 1 given after the introduction of the next group 2.
 * Officials before or after medals does not matter.
 *
 * <pre>
 * GROUP_DONE (indefinite timer, end break = resume current group that was just done)
 * click intro timer
 * BEFORE_INTRODUCTION (on click shows BEFORE_INTRODUCTION and next half-hour time, end break = switch to BEFORE_SNATCH timer paused 10:00)
 * start before intro timer
 * timer ends, automatic switch to BEFORE_SNATCH, timer paused 10:00
 * open dialog
 * BEFORE_SNATCH (BEFORE_SNATCH timer paused 10:00)
 * start intro
 * BEFORE_SNATCH DURING_INTRODUCTION (ceremony, BEFORE_SNATCH timer paused 10:00)
 * end intro
 * BEFORE_SNATCH (timer resumed with time left - 10:00)
 * start timer
 * start officials intro
 * BEFORE_SNATCH DURING_OFFICIALS_INTRODUCTION (ceremony, BEFORE_SNATCH running)
 * end officials intro
 * BEFORE_SNATCH
 * start medals
 * BEFORE_SNATCH MEDALS (ceremony, BEFORE_SNATCH running, displays switch to group 1, end break = go back to group 2)
 * end medals
 * BEFORE_SNATCH
 * timer ends, automatic switch
 * CURRENT_ATHLETE_DISPLAYED
 * </pre>
 *
 * </li>
 *
 * </li>
 * <li>Doable even though not compliant with rules: first snatch at scheduled
 * time, no introduction timer (introduction done informally)
 *
 * <pre>
 * GROUP_DONE (indefinite timer, manual end break = resume current group that was just done)
 * click on snatch timer
 * BEFORE_SNATCH (on click shows BEFORE_INTRODUCTION and next half-hour time, paused, requires a manual change to BEFORE_SNATCH)
 * start intro
 * BEFORE_SNATCH DURING_INTRODUCTION (ceremony, BEFORE_SNATCH timer 10:00, paused)
 * end intro
 * BEFORE_SNATCH (timer resumed with time left)
 * start officials intro
 * BEFORE_SNATCH DURING_OFFICIALS_INTRODUCTION (ceremony)
 * end officials intro
 * BEFORE_SNATCH
 * timer ends, automatic switch
 * CURRENT_ATHLETE_DISPLAYED
 * </pre>
 *
 * </li>
 *
 * @author JF
 *
 */
public enum BreakType {

	BEFORE_INTRODUCTION(false, true),
	FIRST_SNATCH(false, true),
	SNATCH_DONE(false, false),
	FIRST_CJ(false, true),
	GROUP_DONE(false, true),
	CEREMONY(false,true),

	TECHNICAL(true, false),
	MARSHAL(true, false),
	JURY(true, false),
	CHALLENGE(true,false);

	private boolean countdown;
	private boolean interruption;

	BreakType(boolean interruption, boolean countdown) {
		this.countdown = countdown;
		this.interruption = interruption;
	}

	public boolean isCountdown() {
		return countdown;
	}

	public boolean isInterruption() {
		return interruption;
	}

}