/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The subclasses of FOPEvent are all the events that can take place on the field of play.
 *
 * @author owlcms
 */
public class FOPEvent {

    /**
     * Class BarbellOrPlatesChanged
     */
    static public class BarbellOrPlatesChanged extends FOPEvent {

        public BarbellOrPlatesChanged(Object origin) {
            super(origin);
        }
    }


    /**
     * Class BreakPaused.
     */
    static public class BreakPaused extends FOPEvent {

        public BreakPaused(Object origin) {
            super(origin);
        }
    }


    /**
     * Class BreakStarted.
     */
    static public class BreakStarted extends FOPEvent {

        private BreakType breakType;
        private int breakDuration;

        public BreakStarted(BreakType breakType, int timeRemaining, Object origin) {
            super(origin);
            this.setBreakType(breakType);
            this.setBreakDuration(timeRemaining);
        }

        public int getBreakDuration() {
            return breakDuration;
        }

        public BreakType getBreakType() {
            return breakType;
        }

        public void setBreakDuration(int breakDuration) {
            this.breakDuration = breakDuration;
        }

        public void setBreakType(BreakType breakType) {
            this.breakType = breakType;
        }
    }


    /**
     * Report an individual decision.
     *
     * No subclassing relationship with {@link ExplicitDecision} because of different @Subscribe requirements
     */
    public static class DecisionFullUpdate extends FOPEvent {
        public Boolean ref1;
        public Boolean ref2;
        public Boolean ref3;
        public Integer ref1Time;
        public Integer ref2Time;
        public Integer ref3Time;

        public DecisionFullUpdate(Object origin, Athlete athlete, Boolean ref1, Boolean ref2, Boolean ref3,
                Integer long1, Integer long2, Integer long3) {
            super(athlete, origin);
            this.ref1 = ref1;
            this.ref2 = ref2;
            this.ref3 = ref3;
            this.ref1Time = long1;
            this.ref2Time = long2;
            this.ref3Time = long3;
        }

    }


    /**
     * The Class DecisionReset.
     */
    static public class DecisionReset extends FOPEvent {

        public DecisionReset(Object object) {
            super(object);
        }

    }
    static public class DecisionUpdate extends FOPEvent {

        public boolean decision;
        public int refIndex;

        public DecisionUpdate(Object origin, int refIndex, boolean decision) {
            super(origin);
            this.refIndex = refIndex;
            this.decision = decision;
        }

    }

    /**
     * The Class DownSignal.
     */
    static public class DownSignal extends FOPEvent {

        public DownSignal(Object origin) {
            super(origin);
        }

    }
    /**
     * The Class ExplicitDecision.
     */
    static public class ExplicitDecision extends FOPEvent {

        /** The decision. */
        public Boolean success = null;
        public Boolean ref1;
        public Boolean ref2;
        public Boolean ref3;

        /**
         * Instantiates a new referee decision.
         * @param decision the decision
         * @param ref1
         * @param ref2
         * @param ref3
         */
        public ExplicitDecision(Athlete athlete, Object origin, boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
            super(athlete, origin);
            logger.trace("referee decision for {}", athlete);
            this.success = decision;
            this.ref1 = ref1;
            this.ref2 = ref2;
            this.ref3 = ref3;
        }
    }

    static public class ForceTime extends FOPEvent {

        public int timeAllowed;

        public ForceTime(int timeAllowed, Object object) {
            super(object);
            this.timeAllowed = timeAllowed;
        }
    }

    public static class JuryDecision extends FOPEvent {
        /** The decision. */
        public Boolean success = null;

        public JuryDecision(Athlete athlete, Object origin, boolean decision) {
            super(athlete, origin);
            logger.trace("jury decision for {}", athlete);
            this.success = decision;
        }

    }

    /**
     * The Class StartLifting.
     */
    static public class StartLifting extends FOPEvent {

        public StartLifting(Object origin) {
            super(origin);
        }

    }

    public static class SwitchGroup extends FOPEvent {

        private Group group;

        public SwitchGroup(Group g, Object origin) {
            super(origin);
            this.group = g;
        }

        public Group getGroup() {
            return group;
        }
    }

    static public class TimeOver extends FOPEvent{

        public TimeOver(Object origin) {
            super(origin);
        }

    }

    //	/**
    //	 * The Class AthleteAnnounced.
    //	 */
    //	static public class AthleteAnnounced extends FOPEvent {
    //
    //		public AthleteAnnounced(Object object) {
    //			super(object);
    //		}
    //
    //	}

    /**
     * The Class StartTime.
     */
    static public class TimeStarted extends FOPEvent {

        public TimeStarted(Object object) {
            super(object);
        }

    }

    /**
     * The Class StopTime.
     */
    static public class TimeStopped extends FOPEvent {

        public TimeStopped(Object object) {
            super(object);
        }

    }

    /**
     * Class WeightChange.
     */
    static public class WeightChange extends FOPEvent {

        public WeightChange(Object origin, Athlete a) {
            super(a, origin);
        }

    }

    final Logger logger = (Logger)LoggerFactory.getLogger(FOPEvent.class);

    {logger.setLevel(Level.DEBUG);}

    /**
     * When a FOPEvent (for example stopping the clock) is handled, it is often reflected
     * as a series of UIEvents (for example, all the displays running the clock get told to
     * stop it).  The user interface that gave the order doesn't want to be notified again,
     * so we memorize which user interface element created the original order so it can ignore it.
     */
    protected Object origin;

    protected Athlete athlete;

    public FOPEvent(Athlete athlete, Object origin) {
        this.athlete = athlete;
        this.origin = origin;
    }

    FOPEvent (Object origin) {
        this(null, origin);
    }

    public Athlete getAthlete() {
        return athlete;
    }

    public Object getOrigin() {
        return origin;
    }


    public void setAthlete(Athlete athlete) {
        this.athlete = athlete;
    }

}
