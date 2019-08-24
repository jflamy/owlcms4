/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import java.util.List;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;

/**
 * UIEvents are triggered in response to field of play events (FOPEvents). Each
 * field of play has an associated uiEventBus on which the user interface
 * commands are posted. The various browsers subscribe to UIEvents and react
 * accordingly.
 * 
 * @author owlcms
 */
public class UIEvent {

    public static class StartLifting extends UIEvent {
        private Group group;

        public StartLifting(Group group, Object object) {
            super(object);
            this.setGroup(group);
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }
    }

    public static class SwitchGroup extends UIEvent {
        private Group group;

        public SwitchGroup(Group group, Object object) {
            super(object);
            this.setGroup(group);
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }
    }

    /**
     * Class AthleteAnnounced.
     */
    static public class AthleteAnnounced extends UIEvent {

        /**
         * Instantiates a new athlete announced.
         *
         * @param athlete the athlete
         * @param ui      the ui
         */
        public AthleteAnnounced(Athlete athlete, UI ui) {
            super(athlete, ui);
        }
    }

    static public class BarbellOrPlatesChanged extends UIEvent {
        public BarbellOrPlatesChanged(Object object) {
            super(object);
        }
    }

    /**
     * Class BreakDone.
     */
    static public class BreakDone extends UIEvent {

        /**
         * Instantiates a new break done.
         *
         * @param origin the origin
         */
        public BreakDone(Object origin) {
            super(origin);
        }

    }

    /**
     * Class BreakPaused.
     */
    static public class BreakPaused extends UIEvent {

        /**
         * Instantiates a new break paused.
         *
         * @param origin      the origin
         * @param fromConsole TODO
         */
        public BreakPaused(Object origin) {
            super(origin);
        }

    }

    /**
     * Class BreakSetTime
     */
    static public class BreakSetTime extends UIEvent {

        private Integer timeRemaining;
        private boolean indefinite;

        public BreakSetTime(Integer timeRemaining, boolean indefinite, Object origin) {
            super(origin);
            this.timeRemaining = timeRemaining;
            this.indefinite = indefinite;
        }

        public Integer getTimeRemaining() {
            return timeRemaining;
        }

        /**
         * @return true if break lasts indefinitely and timeRemaining should be ignored
         */
        public boolean isIndefinite() {
            return indefinite;
        }
    }

    /**
     * Class BreakStarted.
     */
    static public class BreakStarted extends UIEvent {

        private Integer timeRemaining;
        private boolean displayToggle;

        /**
         * @return true if is a request for toggling display (and not an actual break
         *         start)
         */
        public boolean isDisplayToggle() {
            return displayToggle;
        }

        /**
         * @param displayToggle true to request switching to Break Timer
         */
        public void setDisplayToggle(boolean displayToggle) {
            this.displayToggle = displayToggle;
        }

        public BreakStarted(Integer timeRemaining, Object origin, boolean displayToggle) {
            super(origin);
            this.timeRemaining = timeRemaining;
            this.setDisplayToggle(displayToggle);
        }

        public Integer getTimeRemaining() {
            return timeRemaining;
        }
    }

    /**
     * Class ExplicitDecision.
     */
    static public class Decision extends UIEvent {

        /** decision. */
        public Boolean decision = null;

        /** ref 1. */
        public Boolean ref1;

        /** ref 2. */
        public Boolean ref2;

        /** ref 3. */
        public Boolean ref3;

        /**
         * Instantiates a new referee decision.
         *
         * @param decision the decision
         * @param ref1     the ref 1
         * @param ref2     the ref 2
         * @param ref3     the ref 3
         * @param origin   the origin
         */
        public Decision(Athlete a, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3, Object origin) {
            super(a, origin);
            this.decision = decision;
            this.ref1 = ref1;
            this.ref2 = ref2;
            this.ref3 = ref3;
        }

    }

    /**
     * Class DecisionReset.
     */
    static public class DecisionReset extends UIEvent {

        /**
         * Instantiates a new decision reset.
         *
         * @param origin the origin
         */
        public DecisionReset(Object origin) {
            super(origin);
        }
    }

    /**
     * Class DownSignal.
     */
    static public class DownSignal extends UIEvent {

        /**
         * Instantiates a new down signal.
         *
         * @param origin the origin
         */
        public DownSignal(Object origin) {
            super(origin);
        }
    }

    static public class GroupDone extends UIEvent {

        private Group group;

        /**
         * Instantiates a new athlete announced.
         *
         * @param athlete the athlete
         * @param ui      the ui
         */
        public GroupDone(Group group, UI ui) {
            super(ui);
            this.setGroup(group);
        }

        public Group getGroup() {
            return group;
        }

        public void setGroup(Group group) {
            this.group = group;
        }
    }

    /**
     * Class LiftingOrderUpdated.
     */
    static public class LiftingOrderUpdated extends UIEvent {

        private Athlete nextAthlete;
        private Athlete previousAthlete;
        private Integer timeAllowed;
        private List<Athlete> liftingOrder;
        private List<Athlete> displayOrder;
        private boolean stopAthleteTimer;
        private Athlete changingAthlete;
        private boolean displayToggle;

        /**
         * Instantiates a new lifting order updated.
         *
         * @param athlete         the current athlete after recalculation
         * @param nextAthlete     the next athlete that will lift (cannot be the same as
         *                        athlete)
         * @param previousAthlete the last athlete to have lifted (can be the same as
         *                        athlete)
         * @param changingAthlete the athlete who triggered the lifting update
         * @param liftingOrder    the lifting order
         * @param displayOrder    the display order
         * @param timeAllowed     the time allowed
         * @param displayToggle TODO
         * @param origin          the origin
         */
        public LiftingOrderUpdated(Athlete athlete, Athlete nextAthlete, Athlete previousAthlete,
                Athlete changingAthlete, List<Athlete> liftingOrder, List<Athlete> displayOrder, Integer timeAllowed,
                boolean stopTimer, boolean displayToggle, Object origin) {
            super(athlete, origin);
            this.nextAthlete = nextAthlete;
            this.previousAthlete = previousAthlete;
            this.changingAthlete = changingAthlete;
            this.timeAllowed = timeAllowed;
            this.liftingOrder = liftingOrder;
            this.displayOrder = displayOrder;
            this.stopAthleteTimer = stopTimer;
            this.setDisplayToggle(displayToggle);
        }

        public Athlete getChangingAthlete() {
            return changingAthlete;
        }

        /**
         * Gets the display order.
         *
         * @return the display order
         */
        public List<Athlete> getDisplayOrder() {
            return displayOrder;
        }

        /**
         * Gets the lifting order.
         *
         * @return the lifting order
         */
        public List<Athlete> getLiftingOrder() {
            return liftingOrder;
        }

        /**
         * Gets the next athlete.
         *
         * @return the next athlete
         */
        public Athlete getNextAthlete() {
            return nextAthlete;
        }

        /**
         * Gets the previous athlete.
         *
         * @return the previous athlete
         */
        public Athlete getPreviousAthlete() {
            return previousAthlete;
        }

        /**
         * Gets the time allowed.
         *
         * @return the timeAllowed
         */
        public Integer getTimeAllowed() {
            return timeAllowed;
        }

        /**
         * @return true if the current event requires to stop the timer
         */
        public boolean isStopAthleteTimer() {
            return stopAthleteTimer;
        }
        
        public boolean isDisplayToggle() {
            return displayToggle;
        }

        public void setDisplayToggle(boolean displayToggle) {
            this.displayToggle = displayToggle;
        }

    }

    /**
     * Individual referee decision.
     * 
     * No subclassing wrt ExplicitDecision because @Subscribe must be distinct.
     * 
     * @author owlcms
     */
    static public class RefereeUpdate extends UIEvent {
        public Boolean ref1;
        public Boolean ref2;
        public Boolean ref3;
        public Integer ref1Time;
        public Integer ref2Time;
        public Integer ref3Time;

        public RefereeUpdate(Athlete a, Boolean ref1, Boolean ref2, Boolean ref3, Integer refereeTime,
                Integer refereeTime2, Integer refereeTime3, Object origin) {
            super(a, origin);
            this.ref1 = ref1;
            this.ref2 = ref2;
            this.ref3 = ref3;
            this.ref1Time = refereeTime;
            this.ref2Time = refereeTime2;
            this.ref3Time = refereeTime3;
        }
    }

    /**
     * Class SetTime.
     */
    static public class SetTime extends UIEvent {

        private Integer timeRemaining;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public SetTime(Integer timeRemaining, Object origin) {
            super(origin);
            this.timeRemaining = timeRemaining;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }

    }

    /**
     * Class StartTime.
     */
    static public class StartTime extends UIEvent {

        private Integer timeRemaining;

        /**
         * Instantiates a new start time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public StartTime(Integer timeRemaining, Object origin) {
            super(origin);
            this.timeRemaining = timeRemaining;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }

    }

    /**
     * Class StopTime.
     */
    static public class StopTime extends UIEvent {

        private int timeRemaining;

        /**
         * Instantiates a new stop time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public StopTime(int timeRemaining, Object origin) {
            super(origin);
            this.timeRemaining = timeRemaining;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }
    }

    private Athlete athlete;

    private Object origin;

    private UIEvent(Athlete athlete, Object origin) {
        this(origin);
        this.athlete = athlete;
    }

    private UIEvent(Object origin) {
        this.origin = origin;
    }

    /**
     * Gets the athlete.
     *
     * @return the athlete
     */
    public Athlete getAthlete() {
        return athlete;
    }

    /**
     * Gets the origin.
     *
     * @return the originating object
     */
    public Object getOrigin() {
        return origin;
    }

}
