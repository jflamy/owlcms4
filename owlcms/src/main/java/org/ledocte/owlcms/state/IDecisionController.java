/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.state;

import org.ledocte.owlcms.data.athlete.Athlete;

public interface IDecisionController {

    /**
     * Need to block decisions if a session is underway, unblocking when Athlete is announced or time has started.
     */
    public abstract boolean isBlocked();

    public void setBlocked(boolean blocked);

    public abstract void reset();

    /**
     * Record a decision made by the officials, broacasting to the listeners.
     *
     * @param refereeNo
     * @param accepted
     */
    public abstract void decisionMade(int refereeNo, boolean accepted);

    /**
     * Register a new DecisionEventListener in order to be informed of updates.
     *
     * @param listener
     */
    public abstract void addListener(DecisionEventListener listener);

    /**
     * Remove a specific SessionData.Listener object
     *
     * @param listener
     */
    public abstract void removeListener(DecisionEventListener listener);

    // timer events
    public abstract void finalWarning(int timeRemaining);

    public abstract void forceTimeRemaining(int startTime,
            CompetitionApplication originatingApp,
            InteractionNotificationReason reason);

    public abstract void initialWarning(int timeRemaining);

    public abstract void noTimeLeft(int timeRemaining);

    public abstract void normalTick(int timeRemaining);

    public abstract void pause(int timeRemaining, CompetitionApplication app,
            InteractionNotificationReason reason);

    public abstract void start(int timeRemaining);

    public abstract void stop(int timeRemaining, CompetitionApplication app,
            InteractionNotificationReason reason);

    public abstract void addListener(IRefereeConsole refereeConsole,
            int refereeIndex);

    public abstract Athlete getLifter();

    public abstract void initDownSignal();
}
