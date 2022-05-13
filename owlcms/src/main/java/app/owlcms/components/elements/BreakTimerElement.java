/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;

import app.owlcms.fieldofplay.IBreakTimer;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.IdUtils;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
public class BreakTimerElement extends TimerElement implements SafeEventBusRegistration {

    public Long id;
    final private Logger logger = (Logger) LoggerFactory.getLogger(BreakTimerElement.class);

    private String parentName = "";
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    public BreakTimerElement() {
        super();
        id = IdUtils.getTimeBasedId();
        // logger./**/warn(LoggerUtils./**/stackTrace());
    }

    /**
     * Instantiates a new timer element.
     */
    public BreakTimerElement(String parentName) {
        super();
        id = IdUtils.getTimeBasedId();
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
     * Set the remaining time when the timer element has been hidden for a long time.
     */
    @Override
    @ClientCallable
    public void clientSyncTime(String fopName) {
        OwlcmsSession.withFop(fop -> {
            if (!fopName.contentEquals(fop.getName())) {
                return;
            }
            logger.debug("break timer element fetching time");
            IBreakTimer breakTimer = fop.getBreakTimer();
            doSetTimer(breakTimer.isIndefinite() ? null : breakTimer.liveTimeRemaining());
        });
        return;
    }

    /**
     * Timer stopped
     *
     * @param remaining Time the remaining time
     */
    @Override
    @ClientCallable
    public void clientTimeOver(String fopName) {
        OwlcmsSession.withFop(fop -> {
            if (!fopName.contentEquals(fop.getName())) {
                return;
            }
//            logger.debug("clientTimeOver", fopName);
            IBreakTimer breakTimer = fop.getBreakTimer();
//            logger.debug("{} {} break time over {}", fopName, fop.getName(), breakTimer.isIndefinite());
            if (!breakTimer.isIndefinite()) {
                fop.getBreakTimer().timeOver(this);
            }
        });
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.displays.attemptboard.TimerElement#clientTimerStopped(double)
     */
    @Override
    @ClientCallable
    public void clientTimerStarting(String fopName, double remainingTime, double lateMillis, String from) {
        logger.trace("break timer {} starting on client: remaining = {}", from, remainingTime);
    }

    /**
     * Timer stopped
     *
     * @param remaining Time the remaining time
     */
    @Override
    @ClientCallable
    public void clientTimerStopped(String fopName, double remainingTime, String from) {
        logger.trace("break timer {} stopped on client: remaining = {}", from, remainingTime);
    }

    public void setParent(String s) {
        parentName = s;
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        if (!parentName.startsWith("BreakManagement")) {
            uiEventLogger.trace("&&& break done {} {}", parentName, e.getOrigin());
        }
        doStopTimer(0);
    }

    @Subscribe
    public void slaveBreakPause(UIEvent.BreakPaused e) {
        if (!parentName.startsWith("BreakManagement")) {
            uiEventLogger.trace("&&& breakTimerElement pause {} {}", parentName, e.getMillis());
        }
        doStopTimer(e.getMillis());
    }

    @Subscribe
    public void slaveBreakSet(UIEvent.BreakSetTime e) {
        Integer milliseconds;
        if (e.getEnd() != null) {
            milliseconds = (int) LocalDateTime.now().until(e.getEnd(), ChronoUnit.MILLIS);
        } else {
            milliseconds = e.isIndefinite() ? null : e.getTimeRemaining();
            if (!parentName.startsWith("BreakManagement")) {
                uiEventLogger.trace("&&& breakTimerElement set {} {} {} {} {}", parentName,
                        formatDuration(milliseconds), e.isIndefinite(), id, LoggerUtils.stackTrace());
            }
        }
        doSetTimer(milliseconds);
    }

    @Subscribe
    public void slaveBreakStart(UIEvent.BreakStarted e) {
        if (e.isDisplayToggle()) {
            return;
        }
        Integer tr = e.isIndefinite() ? null : e.getMillis();
        if (!parentName.startsWith("BreakManagement")) {
            uiEventLogger.trace("&&& breakTimerElement start {} {} {} {}", parentName, tr, e.getOrigin(),
                    LoggerUtils.whereFrom());
        }
        if (Boolean.TRUE.equals(e.getPaused())) {
            doSetTimer(tr);
        } else {
            doStartTimer(tr, true); // true means "silent".
        }
    }

    public void syncWithFopBreakTimer() {
        OwlcmsSession.withFop(fop -> {
            init(fop.getName());
            // sync with current status of FOP
            IBreakTimer breakTimer = fop.getBreakTimer();
            if (breakTimer != null) {
                if (!parentName.startsWith("BreakManagement")) {
                    uiEventLogger.trace("&&& breakTimerElement sync running {} indefinite {}", breakTimer.isRunning(),
                            breakTimer.isIndefinite());
                }
                if (breakTimer.isRunning()) {
                    if (breakTimer.isIndefinite()) {
                        doStartTimer(null, fop.isEmitSoundsOnServer());
                    } else {
                        doStartTimer(breakTimer.liveTimeRemaining(), isSilenced() || fop.isEmitSoundsOnServer());
                    }
                } else {
                    if (breakTimer.isIndefinite()) {
                        doSetTimer(null);
                    } else {
                        doSetTimer(breakTimer.getTimeRemainingAtLastStop());
                    }
                }
            }
        });
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        OwlcmsSession.withFop(fop -> {
            // we listen on uiEventBus; this method ensures we stop when detached.
            if (!parentName.startsWith("BreakManagement")) {
                uiEventLogger.trace("&&& breakTimerElement register {} {}", parentName, LoggerUtils.whereFrom());
            }
            uiEventBusRegister(this, fop);
        });
        syncWithFopBreakTimer();
    }

    private String formatDuration(Integer milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
                : (milliseconds != null ? milliseconds.toString() : "-");
    }

}
