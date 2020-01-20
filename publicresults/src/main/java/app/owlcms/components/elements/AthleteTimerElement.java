/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;

import app.owlcms.publicresults.EventReceiverServlet;
import app.owlcms.publicresults.TimerEvent;
import app.owlcms.publicresults.UpdateEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
public class AthleteTimerElement extends TimerElement {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteTimerElement.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Object origin;

    /**
     * Instantiates a new timer element.
     */
    public AthleteTimerElement() {
        this.setOrigin(null); // force exception
        logger.debug("### AthleteTimerElement new {}", origin);
    }

    public AthleteTimerElement(Object origin) {
        this.setOrigin(origin);
        logger.debug("### AthleteTimerElement new {} {}", origin, LoggerUtils.whereFrom());
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
//        OwlcmsSession.withFop(fop -> {
//            int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
//            logger.trace("Fetched time = {} for {}", timeRemaining, fop.getCurAthlete());
//            doSetTimer(timeRemaining);
//        });
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

    @Subscribe
    public void slaveOrderUpdated(UpdateEvent e) {
        ui.access(() -> {
            doSetTimer(e.getTimeAllowed());
        });
    }

    @Subscribe
    public void slaveSetTimer(TimerEvent.SetTime e) {
        Integer milliseconds = e.getTimeRemaining();
        uiEventLogger.debug(">>> set received {} {}", e, milliseconds);
        doSetTimer(milliseconds);
    }

    @Subscribe
    public void slaveStartTimer(TimerEvent.StartTime e) {
        Integer milliseconds = e.getTimeRemaining();
        uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
        doStartTimer(milliseconds, e.isSilent());
    }

    @Subscribe
    public void slaveStopTimer(TimerEvent.StopTime e) {
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

        EventReceiverServlet.getEventBus().register(this);
        ui = UI.getCurrent();
    }


    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        EventReceiverServlet.getEventBus().unregister(this);
    }
    
}
