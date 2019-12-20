/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import java.time.LocalDateTime;

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
    public int computeTimeRemaining() {
        stopMillis = System.currentTimeMillis();
        long elapsed = stopMillis - startMillis;
        timeRemaining = (int) (timeRemaining - elapsed);
        return timeRemaining;
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
        // used only to keep values as entered
        this.end = targetTime;
    }

    public void setIndefinite() {
        indefinite = true;
        logger.debug("setting break indefinite = {} [{}]", indefinite, LoggerUtils.whereFrom());
        this.timeRemaining = 0;
        fop.getUiEventBus().post(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), timeRemaining,
                this.indefinite, this));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
     */
    @Override
    public void setTimeRemaining(int timeRemaining) {
        indefinite = false;
        if (running) {
            computeTimeRemaining();
        }
        logger.debug("setting break timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        this.timeRemaining = timeRemaining;
        fop.getUiEventBus().post(new UIEvent.BreakSetTime(fop.getBreakType(), fop.getCountdownType(), timeRemaining,
                this.indefinite, this));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        if (!running) {
            startMillis = System.currentTimeMillis();
            logger.debug("starting Break -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
            timeRemainingAtLastStop = timeRemaining;
        }
        UIEvent.BreakStarted event = new UIEvent.BreakStarted(isIndefinite() ? null : timeRemaining, null, false,
                fop.getBreakType(), fop.getCountdownType());
        logger.debug("posting {}", event);
        fop.getUiEventBus().post(event);
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
        logger.debug("***stopping Break -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        timeRemainingAtLastStop = timeRemaining;
        logger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        fop.getUiEventBus().post(new UIEvent.BreakPaused(this));
        running = false;
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
        logger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        
        // should emit sound at end of break
        fop.getUiEventBus().post(new UIEvent.BreakDone(origin));

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
