/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers
 * associated with each screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
/**
 * @author owlcms
 *
 */
public class ProxyBreakTimer implements IProxyTimer, IBreakTimer {

    private Integer breakDuration;

    private LocalDateTime end;
    private FieldOfPlay fop;
    private boolean indefinite;
    private long lastStop;
    final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyBreakTimer.class);
    private Object origin;
    private boolean running = false;
    private long startMillis;
    private long stopMillis;
    private int timeRemaining;
    private int timeRemainingAtLastStop;

    {
        logger.setLevel(Level.INFO);
    }

    /**
     * Instantiates a new break timer proxy.
     *
     * @param fop the fop
     */
    public ProxyBreakTimer(FieldOfPlay fop) {
        this.setFop(fop);
    }

    @Override
    public void finalWarning(Object origin) {
        // ignored
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#getBreakDuration()
     */
    @Override
    public Integer getBreakDuration() {
        return breakDuration;
    }

    public LocalDateTime getEnd() {
        return end;
    }

    public Object getOrigin() {
        return origin;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemaining()
     */
    @Override
    public int getTimeRemaining() {
        return timeRemaining;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#getTimeRemainingAtLastStop()
     */
    @Override
    public int getTimeRemainingAtLastStop() {
        return timeRemainingAtLastStop;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#initialWarning(java.lang.Object)
     */
    @Override
    public void initialWarning(Object origin) {
        // ignored
    }

    /**
     * @return the indefinite
     */
    @Override
    public boolean isIndefinite() {
        return indefinite;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#isRunning()
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * Compute time elapsed since start.
     */
    @Override
    public int liveTimeRemaining() {
        if (getEnd() != null) {
            int until = (int) LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS);
            logger.debug("liveTimeRemaining target {} {}",
                    until >= 0 ? DurationFormatUtils.formatDurationHMS(until) : until,
                    LoggerUtils.whereFrom());
            return until;
        } else if (isRunning()) {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            int tr = (int) (getTimeRemaining() - elapsed);
            logger.debug("liveTimeRemaining running {} {}", tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
                    LoggerUtils.whereFrom());
            return tr;
        } else {
            int tr = getTimeRemaining();
            logger.debug("liveTimeRemaining stopped {} {}", tr >= 0 ? DurationFormatUtils.formatDurationHMS(tr) : tr,
                    LoggerUtils.whereFrom());
            return tr;
        }
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setBreakDuration(java.lang.Integer)
     */
    @Override
    public void setBreakDuration(Integer breakDuration) {
        this.breakDuration = breakDuration;
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setEnd(java.time.LocalDateTime)
     */
    @Override
    public void setEnd(LocalDateTime targetTime) {
        indefinite = false;
        // end != null overrides duration computation
        // logger.trace("setting end time = {} \n{}", targetTime, LoggerUtils.stackTrace());
        this.end = targetTime;
    }

    /**
     * @param fop the fop to set
     */
    @Override
    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setIndefinite()
     */
    @Override
    public void setIndefinite() {
        CeremonyType ceremonyType = getFop().getCeremonyType();
        if (ceremonyType != null) {
            // we never start a timer for a ceremony
            return;
        }
        indefinite = true;
        //logger.debug("setting breaktimer indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
        this.setTimeRemaining(0, false);
        this.setEnd(null);
        getFop().pushOutUIEvent(
                new UIEvent.BreakSetTime(getFop().getBreakType(), getFop().getCountdownType(), getTimeRemaining(), null,
                        true, this, LoggerUtils.stackTrace()));
        indefinite = true;
    }

    @Override
    public void setOrigin(Object origin) {
        this.origin = origin;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int, boolean)
     *
     */
    @Override
    public void setTimeRemaining(int timeRemaining2, boolean indefinite) {
        //logger.trace("ProxyBreakTimer setting breaktimer timeremaining={} indefinite={} from {}", timeRemaining2, indefinite, LoggerUtils.whereFrom());
        this.indefinite = indefinite;
        this.timeRemaining = timeRemaining2;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        BreakType breakType = getFop().getBreakType();
        //logger.debug("****** starting break with breakType = {} fop={}", breakType, System.identityHashCode(getFop()));
        if (breakType == null) {
            logger.error("null breaktype {}", LoggerUtils.stackTrace());
        }
        // CeremonyType ceremonyType = getFop().getCeremonyType();
        startMillis = System.currentTimeMillis();

        Integer millisRemaining = getMillis();
 
        UIEvent.BreakStarted event = new UIEvent.BreakStarted(
                millisRemaining, getOrigin(), false,
                breakType,
                getFop().getCountdownType(), LoggerUtils.stackTrace(), this.indefinite);
        // logger.debug("posting {}", event);
        getFop().pushOutUIEvent(event);
        setRunning(true);
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#stop()
     */
    @Override
    public void stop() {
//        CeremonyType ceremonyType = getFop().getCeremonyType();
//        if (ceremonyType != null) {
//            // we never start a timer for a ceremony
//            return;
//        }
        if (isRunning()) {
            computeTimeRemaining();
        }
        setRunning(false);
        timeRemainingAtLastStop = timeRemaining;
        // logger.debug("*** stopping Break -- timeRemaining = {} [{}]",
        // getTimeRemaining(), LoggerUtils.whereFrom());
        timeRemainingAtLastStop = getTimeRemaining();
        // logger.debug("break stop = {} [{}]", liveTimeRemaining(), LoggerUtils.whereFrom());
        UIEvent.BreakPaused event = new UIEvent.BreakPaused(isIndefinite() ? null : getTimeRemaining(), getOrigin(),
                false,
                getFop().getBreakType(), getFop().getCountdownType());
        getFop().pushOutUIEvent(event);
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
     */
    @Override
    public void timeOver(Object origin) {
        //logger.debug("****** break {} {} timeover = {} [{}]", isRunning(), isIndefinite(), getTimeRemaining(), LoggerUtils.whereFrom());
        if (isRunning() && !isIndefinite()) {
            long now = System.currentTimeMillis();
            if (now - lastStop > 1000) {
                // ignore rash of timers all signaling break over
                lastStop = System.currentTimeMillis();
                this.stop();
            } else {
                return;
            }
            // should emit sound at end of break
            //logger.debug("******* timeOver \n{}", LoggerUtils.whereFrom());
            getFop().pushOutUIEvent(new UIEvent.BreakDone(origin, getFop().getBreakType()));
            getFop().fopEventPost(new FOPEvent.BreakDone(getFop().getBreakType(), origin));
        } else {
            // we've already signaled time over.
            return;
        }
    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private int computeTimeRemaining() {
        if (getEnd() != null) {
            setTimeRemaining((int) LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS), false);
        } else {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            setTimeRemaining((int) (getTimeRemaining() - elapsed), false);
        }
        return getTimeRemaining();
    }

    private int getMillis() {
        return (int) (this.getEnd() != null ? LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS)
                : getTimeRemaining());
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return the fop
     */
    FieldOfPlay getFop() {
        return fop;
    }
}
