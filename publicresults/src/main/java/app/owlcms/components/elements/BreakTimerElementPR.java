/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
public class BreakTimerElementPR extends TimerElementPR {

    public Long id;
    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakTimerElementPR.class);

    private String parentName = "";
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());

    {
        this.logger.setLevel(Level.INFO);
        this.uiEventLogger.setLevel(Level.INFO);
    }

    public BreakTimerElementPR() {
        this.id = IdUtils.getTimeBasedId();
        // logger./**/warn(LoggerUtils./**/stackTrace());
    }

    /**
     * Instantiates a new timer element.
     */
    public BreakTimerElementPR(String parentName) {
        this.id = IdUtils.getTimeBasedId();
        // logger./**/warn(LoggerUtils./**/stackTrace());
    }

    @Override
    public void clientFinalWarning(String fopName) {
        // ignored
    }

    @Override
    public void clientInitialWarning(String fopName) {
        // ignored
    }

    /**
     * Set the remaining time when the timer element has been hidden for a long
     * time.
     */
    @Override
    @ClientCallable
    public void clientSyncTime(String fopName) {
    }

    /**
     * Timer stopped
     *
     * @param remaining Time the remaining time
     */
    @Override
    @ClientCallable
    public void clientTimeOver(String fopName) {
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStarting(String fopName, double remainingTime, double lateMillis, String from) {
        this.logger.trace("break timer {} starting on client: remaining = {}", from, remainingTime);
    }

    /**
     * Timer stopped
     *
     * @param remaining Time the remaining time
     */
    @Override
    @ClientCallable
    public void clientTimerStopped(String fopName, double remainingTime, String from) {
        this.logger.trace("break timer {} stopped on client: remaining = {}", from, remainingTime);
    }

    public void setParent(String s) {
        this.parentName = s;
    }

    @Subscribe
    public void slaveBreakDone(BreakTimerEvent.BreakDone e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        doStopTimer(0);
    }

    @Subscribe
    public void slaveBreakPause(BreakTimerEvent.BreakPaused e) {
        if (!this.parentName.startsWith("BreakManagement")) {
            this.uiEventLogger.trace("&&& breakTimerElement pause {} {}", this.parentName, e.getTimeRemaining());
        }
        doStopTimer(e.getTimeRemaining());
    }

    @Subscribe
    public void slaveBreakSet(BreakTimerEvent.BreakSetTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }

        Integer milliseconds;

        milliseconds = e.isIndefinite() ? null : e.getTimeRemaining();
        this.uiEventLogger.debug("&&& breakTimer set {} {} {} {}", this.parentName, formatDuration(milliseconds),
                e.isIndefinite(), LoggerUtils.whereFrom());
        doSetTimer(milliseconds);
    }

    @Subscribe
    public void slaveBreakStart(BreakTimerEvent.BreakStart e) {
        //logger.debug("slaveBreakStart {} {} {} indefinite={}",e.getFopName(),getFopName(), e.getTimeRemaining(), e.isIndefinite());
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        Integer tr = e.isIndefinite() ? null : e.getTimeRemaining();
        this.uiEventLogger.debug("&&& breakTimer start {} {} {}", this.parentName, tr, LoggerUtils.whereFrom());
        doStartTimer(tr, true); // true means "silent".
    }

//    @Subscribe
//    public void slaveOrderUpdated(UpdateEvent e) {
//        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
//            // event is not for us
//            return;
//        }
//        Integer breakRemaining = e.getBreakRemaining();
//        if (e.isBreak() && breakRemaining > 0) {
//            doSetTimer(breakRemaining);
//            doStartTimer(breakRemaining, true);
//        }
//    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#init()
     */
    @Override
    protected void init() {
        super.init();
        setSilenced(true);
        getElement().setProperty("silent", true); // do not emit sounds
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        init();

        //eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());

        setFopName(OwlcmsSession.getFopName());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;
        try {
            UpdateReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
        try {
            TimerReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
    }

    private String formatDuration(Integer milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
                : (milliseconds != null ? milliseconds.toString() : "-");
    }
    
}
