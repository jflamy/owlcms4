/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
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

    final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyBreakTimer.class);
    {
        logger.setLevel(Level.INFO);
    }

    private int timeRemaining;
    private FieldOfPlay fop;
    private long startMillis;
    private long stopMillis;
    private boolean running = false;
    private int timeRemainingAtLastStop;
    private boolean indefinite;
    private LocalDateTime end;
    private Object origin;
    private long lastStop;
    private Integer breakDuration;

    /**
     * Instantiates a new break timer proxy.
     *
     * @param fop the fop
     */
    public ProxyBreakTimer(FieldOfPlay fop) {
        this.fop = fop;
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
        if (end != null) {
            int until = (int) LocalDateTime.now().until(end, ChronoUnit.MILLIS);
            logger.debug("liveTimeRemaining target {} {}", DurationFormatUtils.formatDurationHMS(until),
                    LoggerUtils.whereFrom());
            return until;
        } else if (running) {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            int tr = (int) (getTimeRemaining() - elapsed);
            logger.debug("liveTimeRemaining running {} {}", DurationFormatUtils.formatDurationHMS(tr),
                    LoggerUtils.whereFrom());
            return tr;
        } else {
            int tr = getTimeRemaining();
            logger.debug("liveTimeRemaining stopped {} {}", DurationFormatUtils.formatDurationHMS(tr),
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
        logger.debug("setting end time = {}", targetTime);
        this.end = targetTime;
    }

    /**
     * @see app.owlcms.fieldofplay.IBreakTimer#setIndefinite()
     */
    @Override
    public void setIndefinite() {
        indefinite = true;
        logger.debug("setting breaktimer indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
        this.setTimeRemaining(0);
        this.setEnd(null);
        fop.pushOut(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), getTimeRemaining(), null,
                true, this));
        running = false;
        indefinite = true;
    }

    @Override
    public void setOrigin(Object origin) {
        this.origin = origin;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
     *
     */
    @Override
    public void setTimeRemaining(int timeRemaining2) {
        indefinite = false;

        this.timeRemaining = timeRemaining2;
//        if (running) {
//            computeTimeRemaining();
//        }

        //logger.debug("setting break timeRemaining = {} [{}]", DurationFormatUtils.formatDurationHMS(this.timeRemaining),LoggerUtils. stackTrace());

//        fop.pushOut(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), timeRemaining,
//                this.indefinite, this));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        startMillis = System.currentTimeMillis();
        UIEvent.BreakStarted event = new UIEvent.BreakStarted(isIndefinite() ? null : getMillis(), getOrigin(), false,
                fop.getBreakType(), fop.getCountdownType());
        logger.debug("posting {}", event);
        fop.pushOut(event);
        running = true;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#stop()
     */
    @Override
    public void stop() {
        if (running) {
            computeTimeRemaining();
        }
        running = false;
        timeRemainingAtLastStop = timeRemaining;
        logger.debug("***stopping Break -- timeRemaining = {} [{}]", getTimeRemaining(), LoggerUtils.whereFrom());
        timeRemainingAtLastStop = getTimeRemaining();
        logger.debug("break stop = {} [{}]", liveTimeRemaining(), LoggerUtils.whereFrom());
        UIEvent.BreakPaused event = new UIEvent.BreakPaused(isIndefinite() ? null : getTimeRemaining(), getOrigin(),
                false,
                fop.getBreakType(), fop.getCountdownType());
        fop.pushOut(event);
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
     */
    @Override
    public void timeOver(Object origin) {
        if (running && !isIndefinite()) {
            long now = System.currentTimeMillis();
            if (now - lastStop > 1000) {
                // ignore rash of timers all signaling break over
                lastStop = System.currentTimeMillis();
                this.stop();
            } else {
                return;
            }
        } else {
            // we've already signaled time over.
            return;
        }
        logger.debug("break {} {} timeover = {} [{}]", running, isIndefinite(), getTimeRemaining(),
                LoggerUtils.whereFrom());

        // should emit sound at end of break
        fop.pushOut(new UIEvent.BreakDone(origin));

        EventBus fopEventBus = fop.getFopEventBus();
        BreakType breakType = fop.getBreakType();
        if (breakType == BreakType.FIRST_SNATCH || breakType == BreakType.FIRST_CJ) {
            fopEventBus.post(new FOPEvent.StartLifting(origin));
        } else if (breakType == BreakType.BEFORE_INTRODUCTION) {
            fopEventBus.post(new FOPEvent.BreakStarted(BreakType.DURING_INTRODUCTION, CountdownType.INDEFINITE, null,
                    null, origin));
        }
    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private int computeTimeRemaining() {
        if (end != null) {
            setTimeRemaining((int) LocalDateTime.now().until(end, ChronoUnit.MILLIS));
        } else {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            setTimeRemaining((int) (getTimeRemaining() - elapsed));
        }
        return getTimeRemaining();
    }

    private int getMillis() {
        return (int) (this.getEnd() != null ? LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS)
                : getTimeRemaining());
    }

}
