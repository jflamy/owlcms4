/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

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
        this.fop = fop;
    }

    @Override
    public void finalWarning(Object origin) {
        fop.emitFinalWarning();
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
        fop.emitInitialWarning();
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#isRunning()
     */
    @Override
    public boolean isRunning() {
        return running;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#setTimeRemaining(int)
     */
    @Override
    public void setTimeRemaining(int timeRemaining) {
        if (running) {
            computeTimeRemaining();
        }
        logger.debug("setting Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        this.timeRemaining = timeRemaining;
        fop.pushOut(new UIEvent.SetTime(timeRemaining, null));
        running = false;
    }

    /**
     * @see app.owlcms.fieldofplay.IProxyTimer#start()
     */
    @Override
    public void start() {
        if (!running) {
            startMillis = System.currentTimeMillis();
            logger.debug("starting Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
            timeRemainingAtLastStop = timeRemaining;
        }
        fop.pushOut(new UIEvent.StartTime(timeRemaining, null, fop.isEmitSoundsOnServer()));
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
        logger.debug("***stopping Time -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        timeRemainingAtLastStop = timeRemaining;
        fop.pushOut(new UIEvent.StopTime(timeRemaining, null));
        running = false;
    }

    @Override
    public void timeOver(Object origin) {
        if (running) {
            this.stop();
        }
        fop.emitTimeOver();
        fop.getFopEventBus().post(new FOPEvent.TimeOver(origin));
    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private void computeTimeRemaining() {
        stopMillis = System.currentTimeMillis();
        long elapsed = stopMillis - startMillis;
        timeRemaining = (int) (timeRemaining - elapsed);
    }

}
