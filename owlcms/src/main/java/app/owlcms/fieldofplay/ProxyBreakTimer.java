/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.fieldofplay;

import org.slf4j.LoggerFactory;

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
        logger.setLevel(Level.DEBUG);
    }

    private int timeRemaining;
    private FieldOfPlay fop;
    private long startMillis;
    private long stopMillis;
    private boolean running = false;
    private int timeRemainingAtLastStop;
    private boolean indefinite;

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
        indefinite = false;
        if (running) {
            computeTimeRemaining();
        }
        logger.debug("setting break timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        this.timeRemaining = timeRemaining;
        fop.getUiEventBus().post(new UIEvent.BreakSetTime(timeRemaining, indefinite, this));
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
        fop.getUiEventBus().post(new UIEvent.BreakStarted(timeRemaining, this));
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
        logger.trace("***stopping Break -- timeRemaining = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        timeRemainingAtLastStop = timeRemaining;
        logger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        fop.getUiEventBus().post(new UIEvent.BreakPaused(this));
        running = false;
    }

    /* (non-Javadoc)
     * @see app.owlcms.fieldofplay.IProxyTimer#timeOut(java.lang.Object) */
    @Override
    public void timeOver(Object origin) {
        if (running) {
            this.stop();
        }
        logger.debug("break stop = {} [{}]", timeRemaining, LoggerUtils.whereFrom());
        fop.getUiEventBus().post(new UIEvent.BreakDone(origin));
        fop.getFopEventBus().post(new FOPEvent.StartLifting(origin));

    }

    /**
     * Compute time elapsed since start and adjust time remaining accordingly.
     */
    private void computeTimeRemaining() {
        stopMillis = System.currentTimeMillis();
        long elapsed = stopMillis - startMillis;
        timeRemaining = (int) (timeRemaining - elapsed);
    }

    public void setIndefinite() {
       this.indefinite = true;
    }

    /**
     * @return the indefinite
     */
    public boolean isIndefinite() {
        return indefinite;
    }

}
