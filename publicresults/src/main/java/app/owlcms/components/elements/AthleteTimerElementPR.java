/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.publicresults.DecisionReceiverServlet;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.uievents.TimerEvent;
import app.owlcms.uievents.UpdateEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
public class AthleteTimerElementPR extends TimerElementPR {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteTimerElementPR.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Object origin;

    /**
     * Instantiates a new timer element.
     */
    public AthleteTimerElementPR() {
        this.setOrigin(null); // force exception
        logger.debug("### AthleteTimerElementPR new {}", origin);
    }

    public AthleteTimerElementPR(Object origin) {
        this.setOrigin(origin);
        logger.debug("### AthleteTimerElementPR new {} {}", origin, LoggerUtils.whereFrom());
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientFinalWarning() {
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientInitialWarning() {
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientSyncTime()
     */
    @Override
    @ClientCallable
    public void clientSyncTime() {
        return;
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientTimeOver() {
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStopped(double remainingTime) {
    }

    /**
     * @return the origin
     */
    public Object getOrigin() {
        return origin;
    }

    public void setOrigin(Object origin) {
        this.origin = origin;
    }

    // we do not listen to the bus for this event. Score with leaders forwards this event
    // when appropriate
    public void slaveOrderUpdated(UpdateEvent e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        doSetTimer(e.getTimeAllowed());
    }

    @Subscribe
    public void slaveSetTimer(TimerEvent.SetTime e) {
        Integer milliseconds = e.getTimeRemaining();
        uiEventLogger.debug(">>> set received {} {} {} {}", e, milliseconds, e.getFopName(), getFopName());
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }

        doSetTimer(milliseconds);
    }

    @Subscribe
    public void slaveStartTimer(TimerEvent.StartTime e) {
        Integer milliseconds = e.getTimeRemaining();
        // uiEventLogger.debug(">>> start received {} {} {} {}", e, milliseconds, e.getFopName(), getFopName());
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }

        doStartTimer(milliseconds, e.isSilent());
    }

    @Subscribe
    public void slaveStopTimer(TimerEvent.StopTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contains(e.getFopName())) {
            // event is not for us
            return;
        }
        doStopTimer();
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#init()
     */
    @Override
    protected void init() {
        super.init();
        getModel().setSilent(false); // emit sounds
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();

        eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());
        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
        
        ui = UI.getCurrent();
        this.setFopName((String) OwlcmsSession.getAttribute("fopName"));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        try {
            UpdateReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
        try {
            TimerReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
    }

}
