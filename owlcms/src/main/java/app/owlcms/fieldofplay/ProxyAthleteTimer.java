/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.fieldofplay;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class ProxyBreakTimer. Relay timer instructions from {@link FieldOfPlay} to the actual timers associated with each
 * screen. Memorize the elapsed time and timer state.
 *
 * @author Jean-François Lamy
 */
public class ProxyAthleteTimer implements IProxyTimer {

    final private Logger logger = (Logger) LoggerFactory.getLogger(ProxyAthleteTimer.class);
    {
        logger.setLevel(Level.INFO);
    }

    private int timeRemaining;
    private FieldOfPlay fop;
    private long startMillis;
    private long stopMillis;
    private boolean running = false;
    private int timeRemainingAtLastStop;

    /**
     * Instantiates a new countdown timer.
     *
     * @param fop
     */
    public ProxyAthleteTimer(FieldOfPlay fop) {
        this.setFop(fop);
    }

    @Override
    public void finalWarning(Object origin) {
        getFop().emitFinalWarning();
    }

    /**
     * @return the fop
     */
    public FieldOfPlay getFop() {
        return fop;
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
        getFop().emitInitialWarning();
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
        if (running) {
            stopMillis = System.currentTimeMillis();
            long elapsed = stopMillis - startMillis;
            int tr = (int) (getTimeRemaining() - elapsed);
            logger.debug("liveTimeRemaining running {} {}", formattedDuration(tr),
                    LoggerUtils.whereFrom());
            return tr;
        } else {
            int tr = getTimeRemaining();
            logger.debug("liveTimeRemaining stopped {} {}", formattedDuration(tr),
                    LoggerUtils.whereFrom());
            return tr;
        }
    }

    /**
     * @param fop the fop to set
     */
    @Override
    public void setFop(FieldOfPlay fop) {
        this.fop = fop;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
     */
    @Override
    public void setTimeRemaining(int timeRemaining) {
        if (running) {
            computeTimeRemaining();
        }
        logger.info("{}setting Time -- timeRemaining = {}", getFop().getLoggingName(), timeRemaining);
        this.timeRemaining = timeRemaining;
        getFop().pushOut(new UIEvent.SetTime(timeRemaining, null));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        if (!running) {
            startMillis = System.currentTimeMillis();
            logger.info("{}starting Time -- timeRemaining = {}", getFop().getLoggingName(), timeRemaining);
            timeRemainingAtLastStop = timeRemaining;
        }
        getFop().pushOut(new UIEvent.StartTime(timeRemaining, null, getFop().isEmitSoundsOnServer(),LoggerUtils.stackTrace()));
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
        logger.info("{}stopping Time -- timeRemaining = {}", getFop().getLoggingName(), timeRemaining);
        timeRemainingAtLastStop = timeRemaining;
        getFop().pushOut(new UIEvent.StopTime(timeRemaining, null));
        running = false;
    }

    @Override
    public void timeOver(Object origin) {
        // avoid sending multiple events to FOP
        boolean needToSendEvent = !getFop().isTimeoutEmitted();
        if (needToSendEvent) {
            getFop().emitTimeOver();
            getFop().fopEventPost(new FOPEvent.TimeOver(origin));
        }
        // leave enough time for buzzer event to propagate allowing for some clock drift
        if (running) {
            try {
                // timers that are more than 1 sec. late will now stop silently.
                Thread.sleep(1000);
                this.stop();
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private void computeTimeRemaining() {
        stopMillis = System.currentTimeMillis();
        long elapsed = stopMillis - startMillis;
        timeRemaining = (int) (timeRemaining - elapsed);
    }

    private String formattedDuration(Integer milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
                : (milliseconds != null ? milliseconds.toString() : "-");
    }
}
