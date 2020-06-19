/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.publicresults;

public class BreakTimerEvent {
    public static class BreakDone extends BreakTimerEvent {

        public BreakDone(Object object) {
        }

    }

    public static class BreakStart extends BreakTimerEvent {
        private Integer timeRemaining;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public BreakStart(Integer timeRemaining, Object origin) {
            super();
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
     * Class SetTime.
     */
    static public class SetTime extends BreakTimerEvent {

        private Integer timeRemaining;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public SetTime(Integer timeRemaining, Object origin) {
            super();
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
    static public class StartTime extends BreakTimerEvent {

        private Integer timeRemaining;
        private boolean silent;

        /**
         * Instantiates a new start time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         * @param silent
         */
        public StartTime(Integer timeRemaining, Object origin, boolean silent) {
            super();
            this.timeRemaining = timeRemaining;
            this.silent = silent;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }

        public boolean isSilent() {
            return silent;
        }
    }

    /**
     * Class StopTime.
     */
    static public class StopTime extends BreakTimerEvent {

        private int timeRemaining;

        /**
         * Instantiates a new stop time.
         *
         * @param timeRemaining the time remaining
         * @param origin        the origin
         */
        public StopTime(int timeRemaining, Object origin) {
            super();
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

    public BreakTimerEvent() {
    }

    public String getFopName() {
        // FIXME should come from event
        return null;
    }

}
