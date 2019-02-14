/*
 * Copyright 2009-2012, Jean-François Lamy
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.athlete;


import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class RuleViolation {
    final static Logger logger = (Logger) LoggerFactory.getLogger(RuleViolation.class);

    // public static RuleViolationException change1ValueTooSmall(Object... objs)
    // {
    //		return new RuleViolationException(("RuleViolation.change1ValueTooSmall"), objs);   //$NON-NLS-1$
    // }
    //
    // public static RuleViolationException change2ValueTooSmall(Object... objs)
    // {
    //		return new RuleViolationException(("RuleViolation.change2ValueTooSmall"), objs);   //$NON-NLS-1$
    // }
    //
    public static RuleViolationException declarationValueTooSmall(Object... objs) {
        return new RuleViolationException(("RuleViolation.declarationValueTooSmall"), objs); //$NON-NLS-1$
    }

    //
    // public static RuleViolationException liftValueTooSmall(Object... objs) {
    //		return new RuleViolationException(("RuleViolation.liftValueTooSmall"), objs);   //$NON-NLS-1$
    // }

    public static RuleViolationException liftValueNotWhatWasRequested(Object... objs) {
        return new RuleViolationException(("RuleViolation.liftValueNotWhatWasRequested"), objs); //$NON-NLS-1$
    }

    public static RuleViolationException declaredChangesNotOk(Object... objs) {
        return new RuleViolationException(("RuleViolation.declaredChangesNotOk"), objs); //$NON-NLS-1$
    }

    public static RuleViolationException liftValueBelowProgression(int curLift, String actualLift,
            int automaticProgression) {
        return new RuleViolationException(
                ("RuleViolation.liftValueBelowProgression"), curLift, actualLift, automaticProgression); //$NON-NLS-1$
    }

    public static RuleViolationException rule15_20Violated(Object... objs) {
        return new RuleViolationException("RuleViolation.rule15_20Violated", objs); //$NON-NLS-1$
    }

}
