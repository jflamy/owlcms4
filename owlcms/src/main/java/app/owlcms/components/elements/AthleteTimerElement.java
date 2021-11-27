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

import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
public class AthleteTimerElement extends TimerElement {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AthleteTimerElement.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        uiEventLogger.setLevel(Level.INFO);
    }

    private Object origin;

    /**
     * Instantiates a new timer element.
     */
    public AthleteTimerElement() {
        this.setOrigin(null); // force exception
        logger.trace("### AthleteTimerElement new {}", origin);
    }

    public AthleteTimerElement(Object origin) {
        this.setOrigin(origin);
        logger.trace("### AthleteTimerElement new {} {}", origin, LoggerUtils.whereFrom());
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientFinalWarning() {
        logger.trace("Received final warning.");
        OwlcmsSession.withFop(fop -> {
            fop.getAthleteTimer().finalWarning(this);
        });
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientInitialWarning() {
        logger.trace("Received initial warning.");
        OwlcmsSession.withFop(fop -> {
            fop.getAthleteTimer().initialWarning(this);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientSyncTime()
     */
    @Override
    @ClientCallable
    public void clientSyncTime() {
        OwlcmsSession.withFop(fop -> {
            int timeRemaining = fop.getAthleteTimer().getTimeRemaining();
            // logger./**/warn("Fetched time = {} for {}", timeRemaining, fop.getCurAthlete());
            doSetTimer(timeRemaining);
        });
        return;
    }

    /**
     * @see app.owlcms.components.elements.TimerElement#clientTimeOver()
     */
    @Override
    @ClientCallable
    public void clientTimeOver() {
        logger.trace("Received time over.");
        OwlcmsSession.withFop(fop -> {
            fop.getAthleteTimer().timeOver(this);
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStarting(double remainingTime, double lateMillis, String from) {
        logger.debug("timer {} starting on client: remaining = {}, late={}, roundtrip={}", from, remainingTime,
                lateMillis, delta(lastStartMillis));
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStopped(double remainingTime, String from) {
        logger.debug("timer {} stopped on client: remaining = {}, roundtrip={}", from, remainingTime,
                delta(lastStopMillis));
        // do not stop the server-side timer, this is getting called as a result of the
        // server-side timer issuing a command. Otherwise we create an infinite loop.
    }

    public void detach() {
        OwlcmsSession.withFop(fop -> {
            try {
                fop.getFopEventBus().unregister(this);
            } catch (Exception e) {
                // ignored
            }
        });
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
    public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
        uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                (e.isCurrentDisplayAffected() ? "stop_timer" : "leave_asis"), this.getOrigin(), e.getOrigin());
        if (e.isCurrentDisplayAffected()) {
            clientSyncTime();
        }
//		else {
//			uiEventLogger.trace(LoggerUtils./**/stackTrace());
//		}
    }

    @Subscribe
    public void slaveSetTimer(UIEvent.SetTime e) {
        Integer milliseconds = e.getTimeRemaining();
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), milliseconds,
                e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        doSetTimer(milliseconds);
    }

    @Subscribe
    public void slaveStartTimer(UIEvent.StartTime e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
        Integer milliseconds = e.getTimeRemaining();
        uiEventLogger.debug(">>> start received {} {}", e, milliseconds);
        doStartTimer(milliseconds, e.isServerSound());
    }

    @Subscribe
    public void slaveStopTimer(UIEvent.StopTime e) {
        uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
                this.getOrigin(), e.getOrigin());
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
//        setSilenced(false);
//        getModel().setSilent(false); // emit sounds
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();
        OwlcmsSession.withFop(fop -> {
            // sync with current status of FOP
            IProxyTimer athleteTimer = fop.getAthleteTimer();
            if (athleteTimer != null) {
                if (athleteTimer.isRunning()) {
                    doStartTimer(athleteTimer.liveTimeRemaining(), isSilenced() || fop.isEmitSoundsOnServer());
                } else {
                    doSetTimer(athleteTimer.getTimeRemaining());
                }
            }
            // we listen on uiEventBus.
            uiEventBusRegister(this, fop);
        });
    }

    private long delta(long lastMillis) {
        if (lastMillis == 0) {
            return 0;
        } else {
            return System.currentTimeMillis() - lastMillis;
        }
    }

}
