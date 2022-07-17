/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
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
        uiEventLogger.setLevel(Level.INFO);
    }

    private Object origin;

    /**
     * Instantiates a new timer element.
     */
    public AthleteTimerElementPR() {
        super();
        this.setOrigin(null); // force exception
        logger.trace("### AthleteTimerElement new {}", origin);
    }

    public AthleteTimerElementPR(Object origin) {
        super();
        this.setOrigin(origin);
        logger.trace("### AthleteTimerElement new {} {}", origin, LoggerUtils.whereFrom());
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientFinalWarning(String fopName) {
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientInitialWarning(String fopName) {
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientSyncTime()
     */
    @Override
    @ClientCallable
    public void clientSyncTime(String fopName) {
        return;
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
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
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStopped(String fopName, double remainingTime, String from) {
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
        
        doStartTimer(milliseconds, e.isSilent());  //FIXME: original is e.isServerSound().
    }

    @Subscribe
    public void slaveStopTimer(TimerEvent.StopTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contains(e.getFopName())) {
            // event is not for us
            return;
        }
        Integer milliseconds = e.getTimeRemaining();
        doStopTimer(milliseconds);
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
