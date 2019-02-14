/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.timer;

import org.ledocte.owlcms.state.CompetitionApplication;
import org.ledocte.owlcms.state.InteractionNotificationReason;

public interface CountdownTimerListener {

    void finalWarning(int timeRemaining);

    void initialWarning(int timeRemaining);

    void noTimeLeft(int timeRemaining);

    void normalTick(int timeRemaining);

    /**
     * timer has been stopped, lifter is still associated with timer.
     * 
     * @param timeRemaining
     * @param reason
     * @param competitionApplication
     */
    void pause(int timeRemaining, CompetitionApplication originatingApp, InteractionNotificationReason reason);

    void start(int timeRemaining);

    /**
     * timer has been stopped and associated lifter has been cleared.
     * 
     * @param timeRemaining
     */
    void stop(int timeRemaining, CompetitionApplication originatingApp, InteractionNotificationReason reason);

    /**
     * someone is forcing the amount of time.
     * 
     * @param startTime
     */
    void forceTimeRemaining(int startTime, CompetitionApplication originatingApp, InteractionNotificationReason reason);

    /**
     * Show a notification without stopping the timer
     * 
     * @param reason
     */
    void showInteractionNotification(CompetitionApplication originatingApp, InteractionNotificationReason reason);

}
