package app.owlcms.publicresults;

public class TimerEvent {
    public TimerEvent(Object origin) {
    }

    /**
     * Class SetTime.
     */
    static public class SetTime extends TimerEvent {

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
    static public class StartTime extends TimerEvent {

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
            super(origin);
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
    static public class StopTime extends TimerEvent {

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

}
