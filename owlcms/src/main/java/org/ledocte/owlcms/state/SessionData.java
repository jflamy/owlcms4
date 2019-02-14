/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.state;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter;
import org.ledocte.owlcms.data.athleteSort.AthleteSorter.Ranking;
import org.ledocte.owlcms.data.group.Group;
import org.ledocte.owlcms.data.platform.Platform;
import org.ledocte.owlcms.timer.CountdownTimer;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;


/**
 * Data about a competition group.
 * <p>
 * Manages the lifting order, keeps tabs of which lifters have been called, who is entitled to two minutes, etc. Also holds the master timer
 * for the group.
 * </p>
 *
 * @author jflamy
 */
public class SessionData implements Athlete.UpdateEventListener, Serializable {

    private static final long serialVersionUID = -7621561459948739065L;
    public static final String MASTER_KEY = "GroupData_"; //$NON-NLS-1$

    private static XLogger logger = XLoggerFactory.getXLogger(SessionData.class);

    public List<Athlete> lifters;
    /**
     * list of currently displayed lifters that, if updated, will notify us. We use an IdentitySet because the same Athlete can appear in two
     * windows, as two occurrences that are != but equals.
     */
    private List<Athlete> liftTimeOrder;
    private List<Athlete> displayOrder;
    private List<Athlete> resultOrder;
    //private CompetitionApplication app;

    private Group group;
    private org.ledocte.owlcms.data.athlete.Athlete currentLifter;

    private List<Athlete> currentDisplayOrder;
    private List<Athlete> currentLiftingOrder;
    private List<Athlete> currentResultOrder;

    private int timeAllowed;
    private int liftsDone;

    private RefereeDecisionController refereeDecisionController = null;
    private JuryDecisionController juryDecisionController = null;

    boolean allowAll = false; // allow null group to mean all lifters.
    // will be set to true if the Timekeeping button is pressed.
    private boolean timeKeepingInUse = false;

    private Athlete priorLifter;
    private Integer priorRequest;
    private Integer priorRequestNum;
	public int getLiftsDone() {
        return liftsDone;
    }

    /**
     * This constructor is only meant for unit tests.
     *
     * @param lifters
     */
    public SessionData(List<Athlete> lifters) {
        this.lifters = lifters;
        refereeDecisionController = new RefereeDecisionController(this);
        juryDecisionController = new JuryDecisionController(this);
        updateListsForLiftingOrderChange(null,true, false);
        init();
    }

    /**
     * This constructor is meant to create an independent instance
     *
     * @param lifters
     */
    public SessionData(Group cg, List<Athlete> lifters) {
        this(lifters);
        this.group = cg;
        Platform.getCurrent();
    }


    /**
     * @return information about a session, not connected to a platform.
     */
    
    /**
     * @return
     */
    private void init() {
//        blackBoardEventRouter.register(IntermissionTimerListener.class, IntermissionTimerEvent.class);
//        blackBoardEventRouter.register(PlatesInfoListener.class, PlatesInfoEvent.class);
    }

    /**
     * This method reloads the underlying data. Beware that only "master" views are meant to do this, such as AnnouncerView when mode =
     * ANNOUNCER, or the results view to edit results after a group is over.
     *
     * "slave" views such as the MARSHAL, TIMEKEEPER views should never call this method.
     */
    void loadData() {
//        group = this.getGroup();
//        if (group == null && !allowAll) {
//            // make it so we have to select a group
//            lifters = new ArrayList<Athlete>();
//            logger.debug("current group is empty"); //$NON-NLS-1$
//        } else {
//            CompetitionApplication current = CompetitionApplication.getCurrent();
//            logger.debug("loading data for group {}", group); //$NON-NLS-1$
//            final LifterContainer hbnCont = new LifterContainer(current);
//            // hbnCont will filter automatically to application.getCurrentGroup
//
//            // TODO : avoid using hbnCont -- get from database directly.
//            lifters = hbnCont.getAllPojos();
//        }
    }

    /**
     * @return the Athlete who lifted most recently
     */
    public Athlete getPreviousLifter() {
        if (getLiftTimeOrder() == null) {
            setLiftTimeOrder(AthleteSorter.LiftTimeOrderCopy(lifters));
        }
        if (getLiftTimeOrder().size() == 0)
            return null;

        Athlete Athlete = getLiftTimeOrder().get(0);
        if (Athlete.getPreviousLiftTime() == null)
            return null;
        return Athlete;
    }

    /**
     * Saves changes made to object to Hibernate Session. Note that run is most likely detached due session-per-request patterns so we'll
     * use merge. Actual database update will happen by Vaadin's transaction listener in the end of request.
     *
     * If one wanted to make sure that this operation will be successful a (Hibernate) transaction commit and error checking ought to be
     * done.
     *
     * @param object
     */
    public void persistPojo(Object object) {
    }

    /**
     * Sort the various lists to reflect new lifting order.
     * @param automaticProgression
     * @param letClockRun
     */
    public void updateListsForLiftingOrderChange(Athlete updatedLifter, boolean automaticProgression, boolean letClockRun) {
        logger.debug("updateListsForLiftingOrderChange next = {} change for = {}", currentLifter, updatedLifter); //$NON-NLS-1$

        final CountdownTimer timer2 = getTimer();
        if (timer2 != null) {
            // athlete that was set to lift made a change
            Athlete timerOwner = timer2.getOwner();
            logger.debug("updateListsForLiftingOrderChange next = {} timerOwner = {} updatedLifter={} declarationSameAsAutomatic={}", currentLifter, timerOwner, updatedLifter, letClockRun); //$NON-NLS-1$

            if (currentLifter != null && updatedLifter == currentLifter) {
                if (automaticProgression) {
                    // automatic progression or initial declaration, don't notify announcer
                    // stop the timer if it was running, and make sure event is broadcast
                    timer2.pause();
                } else if (! letClockRun){
                    // the declared weight is different from what was automatically requested
                    // stop the timer if it was running, and make sure event is broadcast
                    timer2.pause(InteractionNotificationReason.CURRENT_LIFTER_CHANGE_DONE);
                }

            }
        }

        sortLists(letClockRun);

    }

    /**
     * @param b
     *
     */
    private boolean sortLists(boolean letClockRun) {
        logger.debug("sortLists"); //$NON-NLS-1$

        displayOrder = AthleteSorter.displayOrderCopy(lifters);
        setLiftTimeOrder(AthleteSorter.LiftTimeOrderCopy(lifters));
        setResultOrder(AthleteSorter.resultsOrderCopy(lifters, Ranking.TOTAL));
        AthleteSorter.assignCategoryRanks(getResultOrder(), Ranking.TOTAL);
        this.liftsDone = AthleteSorter.countLiftsDone(lifters);

        AthleteSorter.liftingOrder(lifters);
        currentLifter = AthleteSorter.markCurrentLifter(lifters);

        Integer currentRequest = (currentLifter != null ? currentLifter.getNextAttemptRequestedWeight() : null);
        Integer currentRequestNum = (currentLifter != null ? currentLifter.getAttemptsDone() : null);

        boolean sameLifter = currentLifter == priorLifter;
        boolean sameWeightRequest = ObjectUtils.compare(priorRequest, currentRequest) == 0;
        boolean sameAttemptNo = ObjectUtils.compare(priorRequestNum, currentRequestNum) == 0;
        boolean needToAnnounce = ! (sameLifter && sameWeightRequest && sameAttemptNo);



        logger.debug("needToAnnounce={} : new/old {}/{}  {}/{}  {}/{} {}", //$NON-NLS-1$
                needToAnnounce, currentLifter, priorLifter, currentRequest, priorRequest, currentRequestNum,
                priorRequestNum, letClockRun);

        if (needToAnnounce) {
//            setAnnounced(false);
            // stop the timer if it was running, and make sure event is broadcast
            final CountdownTimer timer2 = getTimer();
            if (timer2 != null && ! letClockRun) {
                // This also broadcasts an event to all listeners
                timer2.pause();
            }
            setTimeAllowed(timeAllowed(currentLifter));

            logger.trace("timeAllowed={}, timeRemaining={}", timeAllowed, timer2.getTimeRemaining()); //$NON-NLS-1$
        }

        if (currentLifter != null) {
            // copy values from current Athlete.
            priorLifter = currentLifter;
            priorRequest = (currentLifter != null ? currentLifter.getNextAttemptRequestedWeight() : null);
            priorRequestNum = (currentLifter != null ? currentLifter.getAttemptsDone() : null);
        } else {
            priorLifter = null;
            priorRequest = null;
            priorRequestNum = null;
        }

        return needToAnnounce;
    }



    public List<Athlete> getLifters() {
        return lifters;
    }

    /**
     * @return lifters in standard display order
     */
    public List<Athlete> getDisplayOrder() {
        return displayOrder;
    }

    /**
     * @return lifters in lifting order
     */
    public List<Athlete> getAttemptOrder() {
        return lifters;
    }


    /**
     * Check if Athlete is following himself, and that no other Athlete has been announced since (if time starts running for another Athlete,
     * then the two minute privilege is lost).
     *
     * @param Athlete
     *
     */
    public int timeAllowed(Athlete Athlete) {
        logger.trace("timeAllowed start"); //$NON-NLS-1$
        // if clock was running for the current Athlete, return the remaining
        // time.
        if (getTimer().getOwner() == Athlete) {
            logger.trace("timeAllowed current Athlete {} was running.", Athlete); //$NON-NLS-1$
            int timeRemaining = getTimer().getTimeRemaining();
            if (timeRemaining < 0)
                timeRemaining = 0;
            logger.info("resuming time for Athlete {}: {} ms remaining", Athlete, timeRemaining); //$NON-NLS-1$
            return timeRemaining;
        }
        logger.trace("not current Athlete"); //$NON-NLS-1$
        final Athlete previousLifter = getPreviousLifter();
        if (previousLifter == null) {
            logger.trace("A one minute (first Athlete): previousLifter null: startedLifters={} Athlete={}", //$NON-NLS-1$
                    new Object[] { getTimer().getOwner(), Athlete });
            return 60000;
        } else if (Athlete.getAttemptsDone() % 3 == 0) {
            // no 2 minutes if starting snatch or starting c-jerk
            logger.trace("B one minute (first Athlete): first attempt Athlete={}", Athlete); //$NON-NLS-1$
            return 60000;
        } else if (getTimer().getOwner() == null) {
            if (Athlete.equals(previousLifter)) {
                logger.trace("C two minutes (same, timer did not start): startedLifters={} Athlete={} previousLifter={}", //$NON-NLS-1$
                        new Object[] { getTimer().getOwner(), Athlete, previousLifter });
                return 120000;
            } else {
                logger.trace("D one minute (not same): startedLifters={} Athlete={} previousLifter={}", //$NON-NLS-1$
                        new Object[] { getTimer().getOwner(), Athlete, previousLifter });
                return 60000;
            }
        } else {
            logger.trace("E one minute (same, timer started for someone else) : startedLifters={} Athlete={} previousLifter={}", //$NON-NLS-1$
                    new Object[] { getTimer().getOwner(), Athlete, previousLifter });
            return 60000;
        }
    }

    /**
     * @param Athlete
     */
    @SuppressWarnings("unused")
    private void setTimerForTwoMinutes(Athlete Athlete) {
        logger.info("setting timer owner to {}", Athlete);
        getTimer().stop();
        getTimer().setOwner(Athlete); // so time is kept for this Athlete after
                                     // switcheroo
        getTimer().setTimeRemaining(120000);
    }


    public void callLifter(Athlete Athlete) {
        // beware: must call timeAllowed *before* setLifterAnnounced.

        CountdownTimer timer2 = getTimer();
        final int timeRemaining = timer2.getTimeRemaining();
        Long runningTimeRemaining = timer2.getRunningTimeRemaining();

        if (timeExpiredForCurrentLifter(Athlete, timer2, timeRemaining, runningTimeRemaining))
            return;

//        if (timer2.isRunning()) {
//            logger.info("TIMER RUNNING! call of Athlete {} :  - {}ms remaining", Athlete, runningTimeRemaining); //$NON-NLS-1$
//        } else if (isForcedByTimekeeper() && (timeRemaining == 120000 || timeRemaining == 60000)) {
//            setForcedByTimekeeper(true, timeRemaining);
//            logger.info("call of Athlete {} : {}ms FORCED BY TIMEKEEPER", Athlete, timeRemaining); //$NON-NLS-1$
//        } else {
//            if (!getTimeKeepingInUse()) {
//                int allowed = getTimeAllowed();
//                timer2.setTimeRemaining(allowed);
//                logger.info("call of Athlete {} : {}ms allowed", Athlete, allowed); //$NON-NLS-1$
//            } else {
//                logger.info("call of Athlete {} : {}ms remaining", Athlete, timeRemaining); //$NON-NLS-1$
//            }
//            setForcedByTimeKeeper(false);
//        }

        refereeDecisionController.reset();
        juryDecisionController.reset();
        announced = false;


            startUpdateModel();

        // we just did the announce.
        setAnnounced(true);

        return;
    }

    public boolean timeExpiredForCurrentLifter(Athlete Athlete, CountdownTimer timer2, final int timeRemaining, Long runningTimeRemaining) {
        // time expired for Athlete
        boolean timeExpiredForCurrentLifter = false;
        if (Athlete == timer2.getOwner()) {
            if (timeRemaining <= 0 || (runningTimeRemaining != null && runningTimeRemaining <= 0)) {
                timer2.forceTimeRemaining(0, InteractionNotificationReason.CLOCK_EXPIRED);
                announced = true;
                timeExpiredForCurrentLifter = true;
            }
        }
        return timeExpiredForCurrentLifter;
    }

    /**
     * @param b
     */
    @SuppressWarnings("unused")
	private void setForcedByTimeKeeper(boolean b) {

    }

    public List<Athlete> getLiftTimeOrder() {
        return liftTimeOrder;
    }

    public void liftDone(Athlete Athlete, boolean success) {
        logger.debug("lift done: notifiers={}"); //$NON-NLS-1$
        final CountdownTimer timer2 = getTimer();
        timer2.setOwner(null);
        timer2.stop(); // in case timekeeper has failed to stop it.
        timer2.setTimeRemaining(0);
//        setAnnounced(false); // now done in caller
        setTimerStarted(false);
    }

    CountdownTimer timer;
	private boolean timerStarted;
	private boolean announced;

    public CountdownTimer getTimer() {
        if (timer == null) {
            timer = new CountdownTimer();
        }
        ;
        return timer;
    }


    /**
     * @return the currentDisplayOrder
     */
    public List<Athlete> getCurrentDisplayOrder() {
        return currentDisplayOrder;
    }

    /**
     * @return the currentDisplayOrder
     */
    public List<Athlete> getCurrentResultOrder() {
        return currentResultOrder;
    }

    /**
     * @return the currentLiftingOrder
     */
    public List<Athlete> getCurrentLiftingOrder() {
        return currentLiftingOrder;
    }


    void setCurrentSession(Group newCurrentSession) {
    }

    /**
     * @return the currentSession
     */
    public Group getGroup() {
        return group;
    }

    /**
     * Copied from interface. React to Athlete changes by recomputing the lists.
     *
     * @see org.concordiainternational.competition.data.Athlete.UpdateEventListener#updateEvent(org.concordiainternational.competition.data.Athlete.UpdateEvent)
     */
    @Override
    public void updateEvent(Athlete.UpdateEvent updateEvent) {
        List<String> propertyIds = updateEvent.getPropertyIds();
        logger.debug("Athlete {}, changed {}", updateEvent.getSource(), propertyIds); //$NON-NLS-1$
        boolean automaticProgression = false;
        if (propertyIds != null) automaticProgression = propertyIds.contains("automatic");
        boolean declaration = false;
        for (String propertyId: propertyIds) {
            declaration = propertyId.endsWith("Declaration");
            if (declaration) break;
        }
        Athlete source = (Athlete) updateEvent.getSource();

        // we don't want to stop the clock if the coach declares the same weight as was automatically determined
        boolean declarationSameAsAutomatic = false;
        if (declaration) {
            Object currentAutomatic = source.getCurrentAutomatic();
            // if the automatic declaration is changed after other data has been entered (e.g. correcting typo), stop clock.
            String currentChange1 = source.getCurrentChange1();
            declarationSameAsAutomatic = Objects.equals(source.getCurrentDeclaration(),currentAutomatic) && (currentChange1 != null && currentChange1.isEmpty());
        }

        updateListsForLiftingOrderChange(source, automaticProgression, declarationSameAsAutomatic);
        persistPojo(updateEvent.getSource());
    }

    public Athlete getCurrentLifter() {
        return currentLifter;
    }

    /**
     * @param liftTimeOrder
     *            the liftTimeOrder to set
     */
    void setLiftTimeOrder(List<Athlete> liftTimeOrder) {
        this.liftTimeOrder = liftTimeOrder;
    }

    /**
     * @param resultOrder
     *            the resultOrder to set
     */
    void setResultOrder(List<Athlete> resultOrder) {
        this.resultOrder = resultOrder;
    }

    /**
     * @return the resultOrder
     */
    List<Athlete> getResultOrder() {
        return resultOrder;
    }


    /**
     * @param timeAllowed
     *            the timeAllowed to set
     */
    private void setTimeAllowed(int timeAllowed) {
        this.timeAllowed = timeAllowed;
    }

    /**
     * @return the timeAllowed
     */
    public int getTimeAllowed() {
        return timeAllowed;
    }

    public int getTimeRemaining() {
        if (timer != null) {
            Long runningTimeRemaining = timer.getRunningTimeRemaining();
            return runningTimeRemaining != null ? runningTimeRemaining.intValue() : timer.getTimeRemaining() ;
        } else {
            return timeAllowed;
        }

    }

    public boolean getAllowAll() {
        return allowAll;
    }

    public void setAllowAll(boolean allowAll) {
        this.allowAll = allowAll;
    }

    public IDecisionController getRefereeDecisionController() {
        return (IDecisionController) refereeDecisionController;
    }

    public IDecisionController getJuryDecisionController() {
        return (IDecisionController) juryDecisionController;
    }

    public void majorityDecision(Decision[] refereeDecisions) {
        final Athlete currentLifter2 = getCurrentLifter();
        int pros = 0;
        for (int i = 0; i < refereeDecisions.length; i++) {
            if (refereeDecisions[i] != null && refereeDecisions[i].accepted)
                pros++;
        }
        final boolean success = pros >= 2;
        liftDone(currentLifter2, success);
        if (success) {
            logger.info("Referee decision for {}: GOOD lift",currentLifter2);
            if (currentLifter2 != null)
                currentLifter2.successfulLift();
        } else {
            logger.info("Referee decision for {}: NO lift",currentLifter2);
            if (currentLifter2 != null)
                currentLifter2.failedLift();
        }

        // record the decision.
        if (currentLifter2 != null) {
            saveLifter(currentLifter2);
        } else {
            logger.warn("No current Athlete.");
        }
    }

    /**
     * @param currentLifter2
     */
    public void saveLifter(final Athlete currentLifter2) {

    }

    public void downSignal() {
        notifyPrematureDecision();
    }

    /**
	 *
	 */
    synchronized public void notifyPrematureDecision() {
        CountdownTimer timer2 = getTimer();
        if (!isAnnounced()) {
            timer2.stop(InteractionNotificationReason.NOT_ANNOUNCED);
        } else if (timeKeepingInUse && timer2.isRunning()) {
            timer2.stop(InteractionNotificationReason.REFEREE_DECISION);
        } else if (timeKeepingInUse && !isTimerStarted()) {
            timer2.stop(InteractionNotificationReason.NO_TIMER);
        }
    }

    public void setAnnounced(boolean b) {
        //logger.debug("announced = {}",b);
        announced = b;
//            LoggerUtils.traceBack(logger, "setAnnounced");
    }

    public boolean isAnnounced() {
        return announced;
    }

    /**
     * @param timeKeepingInUse
     *            the timeKeepingInUse to set
     */
    public void setTimeKeepingInUse(boolean timeKeepingInUse) {
        this.timeKeepingInUse = timeKeepingInUse;
    }



    void noCurrentLifter() {
        // most likely completely obsolete.
        // getTimer().removeAllListeners();
    }

    public void refresh(boolean isMaster) {
    }



	public int getDisplayTime() {
        if (currentLifter != timer.getOwner()) {
            return getTimeAllowed();
        } else {
            return getTimeRemaining();
        }
    }

    public void startUpdateModel() {
        final CountdownTimer timer1 = this.getTimer();
        final Athlete Athlete = getCurrentLifter();

        CountdownTimer timer2 = getTimer();
        final int timeRemaining = timer2.getTimeRemaining();
        Long runningTimeRemaining = timer2.getRunningTimeRemaining();

        if (timeExpiredForCurrentLifter(Athlete, timer2, timeRemaining, runningTimeRemaining))
            return;

        //timingLogger.debug("start timer.isRunning()={}", running); //$NON-NLS-1$
        timer1.restart();
        setTimerStarted(true);
        getRefereeDecisionController().setBlocked(false);
    }

    public void stopUpdateModel() {
        getTimer().pause(); // pause() does not clear the associated Athlete
    }

    public void oneMinuteUpdateModel() {
        if (getTimer().isRunning()) {
            timer.forceTimeRemaining(60000); // pause() does not clear the associated Athlete
        }
        setForcedByTimekeeper(true, 60000);
    }

    private void setForcedByTimekeeper(boolean b, int i) {
		// TODO Auto-generated method stub
		
	}

	public void twoMinuteUpdateModel() {
        if (getTimer().isRunning()) {
            timer.forceTimeRemaining(120000); // pause() does not clear the associated Athlete
        }
        setForcedByTimekeeper(true, 120000);
    }

    public void okLiftUpdateModel() {
        Athlete currentLifter2 = getCurrentLifter();
        liftDone(currentLifter2, true);
        currentLifter2.successfulLift();
    }

    public void noLiftUpdateModel() {
        Athlete currentLifter2 = getCurrentLifter();
        liftDone(currentLifter2, false);
        currentLifter2.failedLift();
    }

    private boolean isTimerStarted() {
        return timerStarted;
    }

    private void setTimerStarted(boolean timerStarted) {
        this.timerStarted = timerStarted;
    }

}
