/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.athlete;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * The Class RuleViolation.
 */
public class RuleViolation {

    /** The Constant logger. */
    final static Logger logger = (Logger) LoggerFactory.getLogger(RuleViolation.class);

    // public static RuleViolationException change1ValueTooSmall(Object... objs)
    // {
    // return new RuleViolationException(("RuleViolation.change1ValueTooSmall"),
    // objs);
    // }
    //
    // public static RuleViolationException change2ValueTooSmall(Object... objs)
    // {
    // return new RuleViolationException(("RuleViolation.change2ValueTooSmall"),
    // objs);
    // }
    /**
     * Declaration value too small.
     *
     * @param objs the objs
     * @return the rule violation exception
     */
    //
    public static RuleViolationException declarationValueTooSmall(Object... objs) {
        return new RuleViolationException(("RuleViolation.declarationValueTooSmall"), objs);
    }

    //
    // public static RuleViolationException liftValueTooSmall(Object... objs) {
    // return new RuleViolationException(("RuleViolation.liftValueTooSmall"), objs);
    // }

    /**
     * Declared changes not ok.
     *
     * @param objs the objs
     * @return the rule violation exception
     */
    public static RuleViolationException declaredChangesNotOk(Object... objs) {
        return new RuleViolationException(("RuleViolation.declaredChangesNotOk"), objs);
    }

    /**
     * Lift value below progression.
     *
     * @param curLift              the cur lift
     * @param actualLift           the actual lift
     * @param automaticProgression the automatic progression
     * @return the rule violation exception
     */
    public static RuleViolationException liftValueBelowProgression(int curLift, String actualLift,
            int automaticProgression) {
        return new RuleViolationException(("RuleViolation.liftValueBelowProgression"), curLift, actualLift,
                automaticProgression);
    }

    /**
     * Lift value not what was requested.
     *
     * @param objs the objs
     * @return the rule violation exception
     */
    public static RuleViolationException liftValueNotWhatWasRequested(Object... objs) {
        return new RuleViolationException(("RuleViolation.liftValueNotWhatWasRequested"), objs);
    }

    /**
     * Rule 15 20 violated.
     *
     * @param objs the objs
     * @return the rule violation exception
     */
    public static RuleViolationException rule15_20Violated(Object... objs) {
        return new RuleViolationException("RuleViolation.rule15_20Violated", objs);
    }

    /**
     * Clock was started on a higher weight already.  
     * Cannot request.
     * @param objs
     * @return
     */
    public static RuleViolationException valueBelowStartedClock(Object... objs) {
        return new RuleViolationException("RuleViolation.valueBelowStartedClock", objs);
    }

    /**
     * Someone else lifted a higher weight already.
     * @param requested
     * @param startNumber
     * @param weight
     * @return
     */
    public static RuleViolationException weightBelowAlreadyLifted(Integer requested, int startNumber, int weight) {
        return new RuleViolationException("RuleViolation.weightBelowAlreadyLifted", new Object[] {requested, startNumber, weight});
    }

    /**
     * Someone else lifted same weight on a higher attempt number.
     * @param nextAttemptRequestedWeight
     * @param prevAttemptNo
     * @param prevWeight
     * @return
     */
    public static RuleViolationException attemptNumberTooLow(Integer nextAttemptRequestedWeight, int prevAttemptNo, int prevWeight) {
        return new RuleViolationException("RuleViolation.attemptNumberTooLow", new Object[] {nextAttemptRequestedWeight, prevAttemptNo, prevWeight});
    }

    /**
     * On first lift cannot move ahead of someone with lower start number than ourself
     * @param requestedWeight
     * @param prevLotNumber
     * @param curLotNumber
     * @return
     */
    public static RuleViolationException startNumberTooHigh(Integer requestedWeight, int prevLotNumber, int curLotNumber) {
        return new RuleViolationException("RuleViolation.startNumberTooHigh", new Object[] {requestedWeight, prevLotNumber, curLotNumber});
    }

    /**
     * If start numbers not allocated, on first lift cannot move ahead of someone with lower lot number than ourself
     * @param requestedWeight
     * @param prevLotNumber
     * @param curLotNumber
     * @return
     */
    public static RuleViolationException lotNumberTooHigh(Integer requestedWeight, int prevLotNumber, int curLotNumber) {
        return new RuleViolationException("RuleViolation.lotNumberTooHigh", new Object[] {requestedWeight, prevLotNumber, curLotNumber});
    }

    /**
     * Athlete athlete1 lifted earlier than athlete2.
     * Cannot take the same weight than athlete2 but lift later -- manipulating clock to gain more rest.
     * @param athlete1
     * @param athlete2
     * @return
     */
    public static RuleViolationException liftedEarlier(Athlete athlete1, Athlete athlete2) {
        return new RuleViolationException("RuleViolation.liftedEarlier", new Object[] {athlete1, athlete2});
    }

//	public static RuleViolationException declarationValueRequired(int curLift) {
//		 return new RuleViolationException("RuleViolation.declarationRequired");
//	}

}
