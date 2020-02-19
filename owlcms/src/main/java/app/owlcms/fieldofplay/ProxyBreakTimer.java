/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.ui.shared.BreakManagement.CountdownType;
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
public class ProxyBreakTimer implements IProxyTimer {

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

    /**
     * Instantiates a new break timer proxy.
     *
     * @param fop the fop
     */
    public ProxyBreakTimer(FieldOfPlay fop) {
        this.fop = fop;
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

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    public int liveTimeRemaining() {
        if (end != null) {
            int until = (int) LocalDateTime.now().until(end, ChronoUnit.MILLIS);
            logger.warn("liveTimeRemaining target {}", DurationFormatUtils.formatDurationHMS(until));
            return until;
        } else if (running) {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            int tr = (int) (getTimeRemaining() - elapsed);
            logger.warn("liveTimeRemaining running {}", DurationFormatUtils.formatDurationHMS(tr));
            return tr;
        } else {
            int tr = getTimeRemaining();
            logger.warn("liveTimeRemaining stopped {}", DurationFormatUtils.formatDurationHMS(tr));
            return tr;
        }
    }

    @Override
    public void finalWarning(Object origin) {
        // ignored
    }

    public LocalDateTime getEnd() {
        return end;
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

    public void setEnd(LocalDateTime targetTime) {
        indefinite = false;
        // end != null overrides duration computation
        logger.warn("setting end time = {}", targetTime);
        this.end = targetTime;
    }

    public void setIndefinite() {
        indefinite = true;
        logger.debug("setting break indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
        this.setTimeRemaining(0);
        this.setEnd(null);
        fop.pushOut(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), getTimeRemaining(),
                true, this));
        running = false;
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

        logger.warn("setting break timeRemaining = {} [{}]", DurationFormatUtils.formatDurationHMS(this.timeRemaining),
                LoggerUtils.whereFrom());

//        fop.pushOut(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), timeRemaining,
//                this.indefinite, this));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
//        if (end != null) {
//            timeRemaining = (int) LocalDateTime.now().until(end, ChronoUnit.MILLIS);
//        } else if (!running) {
//            startMillis = System.currentTimeMillis();
//            logger.debug("starting Break -- timeRemaining = {} [{}]", getTimeRemaining(), LoggerUtils.whereFrom());
//        }
        startMillis = System.currentTimeMillis();
        UIEvent.BreakStarted event = new UIEvent.BreakStarted(isIndefinite() ? null : getMillis(), null, false,
                fop.getBreakType(), fop.getCountdownType());
        logger.warn("posting {}", event);
        fop.pushOut(event);
        running = true;
    }

    private int getMillis() {
        return (int) (this.getEnd() != null ? LocalDateTime.now().until(getEnd(), ChronoUnit.MILLIS) : getTimeRemaining());
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
        fop.pushOut(new UIEvent.BreakPaused(this));
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object)
     */
    @Override
    public void timeOver(Object origin) {
        if (running && !isIndefinite()) {
            this.stop();
        } else {
            // we've already signaled time over.
            return;
        }
        logger.warn("break timeover = {} [{}]", getTimeRemaining(), LoggerUtils.whereFrom());

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

}
