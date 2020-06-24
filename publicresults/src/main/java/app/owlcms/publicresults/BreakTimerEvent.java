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

    public static class BreakPaused extends BreakTimerEvent {
        private Integer timeRemaining;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         */
        public BreakPaused(Integer timeRemaining) {
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

    public static class BreakSetTime extends BreakTimerEvent {
        private Integer timeRemaining;
        private boolean indefinite;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         * @param indefinite
         */
        public BreakSetTime(Integer timeRemaining, boolean indefinite) {
            super();
            this.timeRemaining = timeRemaining;
            this.indefinite = indefinite;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }

        public boolean isIndefinite() {
            return indefinite;
        }
    }

    public static class BreakStart extends BreakTimerEvent {
        private Integer timeRemaining;
        private boolean indefinite;

        /**
         * Instantiates a new sets the time.
         *
         * @param timeRemaining the time remaining
         * @param indefinite
         */
        public BreakStart(Integer timeRemaining, boolean indefinite) {
            super();
            this.timeRemaining = timeRemaining;
            this.indefinite = indefinite;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return timeRemaining;
        }

        public boolean isIndefinite() {
            return indefinite;
        }
    }

    private String fopName;

    public BreakTimerEvent() {
    }

    public String getFopName() {
        return fopName;
    }

    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

}
