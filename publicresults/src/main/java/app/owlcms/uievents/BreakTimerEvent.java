/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

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
            this.timeRemaining = timeRemaining;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return this.timeRemaining;
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
            this.timeRemaining = timeRemaining;
            this.indefinite = indefinite;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return this.timeRemaining;
        }

        public boolean isIndefinite() {
            return this.indefinite;
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
            this.timeRemaining = timeRemaining;
            this.indefinite = indefinite;
        }

        /**
         * Gets the time remaining.
         *
         * @return the time remaining
         */
        public Integer getTimeRemaining() {
            return this.timeRemaining;
        }

        public boolean isIndefinite() {
            return this.indefinite;
        }
    }

    private String fopName;
    private BreakType breakType;
    private String groupName;
    private String mode;

    public BreakTimerEvent() {
    }

    public BreakType getBreakType() {
        return this.breakType;
    }

    public String getFopName() {
        return this.fopName;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public String getMode() {
        return this.mode;
    }

    public void setBreakType(BreakType breakType) {
        this.breakType = breakType;
    }

    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

}
