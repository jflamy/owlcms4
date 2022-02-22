/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.uievents;

public enum BreakType {
    
    // this is a flat list because it is about "what is going on on the field of play"
    BEFORE_INTRODUCTION(false), 
    DURING_INTRODUCTION(true), 
    FIRST_SNATCH(false), 
    FIRST_CJ(false), 
    TECHNICAL(false), 
    JURY(false),
    MEDALS(true), 
    GROUP_DONE(false);

    /**
     * if true, the current break timer should keep running.
     */
    private boolean ceremony;

    BreakType(boolean b) {
        this.ceremony = b;
    }

    public boolean isCeremony() {
        return ceremony;
    }
}