/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

public enum BreakType {
    
    // this is a flat list because it is about "what is going on on the field of play"
    BEFORE_INTRODUCTION(false, true, false), 
    DURING_INTRODUCTION(true, false, false),
    DURING_OFFICIALS_INTRODUCTION(true, false, false),
    FIRST_SNATCH(false, true, false), 
    FIRST_CJ(false, true, false), 
    TECHNICAL(false, false, true), 
    JURY(false, false, true),
    MEDALS(true, false, false), 
    GROUP_DONE(false, false, false);

    /**
     * if true, the current break timer should keep running.
     */
    private boolean ceremony;
    private boolean countdown;
    private boolean interruption;

    BreakType(boolean ceremony, boolean countdown, boolean interruption) {
        this.ceremony = ceremony;
        this.countdown = countdown;
        this.interruption = interruption;
    }

    public boolean isCountdown() {
        return countdown;
    }

    public boolean isInterruption() {
        return interruption;
    }

    public boolean isCeremony() {
        return ceremony;
    }
}