/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.prutils.DebugUtils;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@Tag("timer-element-pr")
@JsModule("./components/TimerElementPR.js")
public abstract class TimerElementPR extends LitTemplate
        implements IFopName, SafeEventBusRegistrationPR {

    private String fopName;

    final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElementPR.class);
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
    {
        this.logger.setLevel(Level.INFO);
        this.uiEventLogger.setLevel(Level.INFO);
    }

    private Element timerElement;
    private boolean indefinite;
    private Integer msRemaining;
    private boolean silenced = true;
    protected UI ui;

    public long lastStartMillis;
    public long lastStopMillis;

    private boolean serverSound;

    /**
     * Instantiates a new timer element.
     */
    public TimerElementPR() {
    }

    @ClientCallable
    abstract public void clientFinalWarning(String fopName);

    @ClientCallable
    abstract public void clientInitialWarning(String fopName);

    /**
     * Client requests that the server send back the remaining time. Intended to be
     * used after client has been hidden
     * and is made visible again.
     */
    @ClientCallable
    abstract public void clientSyncTime(String fopName);

    /**
     * Timer ran down to zero.
     */
    @ClientCallable
    abstract public void clientTimeOver(String fopName);

    @ClientCallable
    abstract public void clientTimerStarting(String fopName, double remainingTime, double lateMillis, String from);

    /**
     * Timer has been stopped on the client side.
     *
     * @param remainingTime
     */
    @ClientCallable
    abstract public void clientTimerStopped(String fopName, double remainingTime, String from);

    /** @see app.owlcms.components.elements.IFopName#getFopName() */
    @Override
    public String getFopName() {
        return this.fopName;
    }

    public boolean isServerSound() {
        return this.serverSound;
    }

    /** @see app.owlcms.components.elements.IFopName#setFopName(java.lang.String) */
    @Override
    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    public void setSilenced(boolean b) {
        this.logger.debug("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
        this.silenced = b;
    }

    protected final void doSetTimer(Integer milliseconds) {
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            stop(getMsRemaining(), isIndefinite(), isSilenced(), parent);
            initTime(milliseconds);
        });
    }

    protected void doStartTimer(Integer milliseconds, boolean silent) {
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            setIndefinite(milliseconds == null);
            setMsRemaining(milliseconds);
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            this.lastStartMillis = System.currentTimeMillis();
            this.logger.trace("server starting timer {}, {}, {}", parent, milliseconds, this.lastStartMillis);
            getElement().setProperty("silent", isSilent());
            start(milliseconds, isIndefinite(), isSilent(), parent);
        });
    }

    protected void doStopTimer(Integer milliseconds) {
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            setMsRemaining(milliseconds);
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            this.lastStopMillis = System.currentTimeMillis();
            this.logger.trace("server stopping timer {}, {}, {}", parent, milliseconds, this.lastStopMillis);
            stop(getMsRemaining(), isIndefinite(), isSilent(), parent);
        });
    }

    protected Element getTimerElement() {
        return this.timerElement;
    }

    protected void init() {
        setFopName(OwlcmsSession.getFopName());
        setTimerElement(this.getElement());
        double seconds = 0.00D;
        setMsRemaining(0);
        setServerSound(false);
        setSilenced(false);
        setIndefinite(false);
        if (UI.getCurrent() == null) {
            return;
        }
        UI.getCurrent().access(() -> {
            getElement().setProperty("startTime", 0.0D);
            getElement().setProperty("currentTime", seconds);
            getElement().setProperty("countUp", false);
            getElement().setProperty("running", false);
            getElement().setProperty("silent", false);
            getElement().setProperty("fopName", getFopName());
        });
    }

    protected boolean isIndefinite() {
        return this.indefinite;
    }

    protected boolean isSilenced() {
        return this.silenced;
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        init();

//        eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());
//        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);

        detachEvent.getUI().access(() -> {
            // tell the javascript to stay quiet
            setSilenced(true);
            setTimerElement(null);
            getElement().setProperty("silent", true);
        });
    }

    protected void setIndefinite(boolean indefinite) {
        this.indefinite = indefinite;
    }

    protected void setTimerElement(Element timerElement) {
        this.timerElement = timerElement;
    }

    private Integer getMsRemaining() {
        return this.msRemaining;
    }

    private void initTime(Integer milliseconds) {
        if (this instanceof BreakTimerElementPR) {
            // logger.trace("set time remaining = {} from {} ",
            // formatDuration(milliseconds), LoggerUtils.whereFrom());
        }
        setIndefinite(milliseconds == null);
        setMsRemaining(milliseconds);

        if (!isIndefinite()) {
            if (this instanceof BreakTimerElementPR) {
                // logger.trace("not indefinite {}", formatDuration(milliseconds));
            }
            setDisplay(milliseconds, isIndefinite(), isSilenced());
        } else {
            if (this instanceof BreakTimerElementPR) {
                // logger.trace("indefinite");
            }
            setDisplay(milliseconds, true, true);
        }
    }

    /**
     * No sound if sound is emitted on server, or if silenced through the interface.
     *
     * @return
     */
    private boolean isSilent() {
        return isServerSound() || (!isServerSound() && isSilenced());
    }

    private void setDisplay(Integer milliseconds, Boolean indefinite, Boolean silent) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("display", seconds, indefinite, silent, timerElement2);
        }
    }

    private void setMsRemaining(Integer milliseconds) {
        this.msRemaining = milliseconds;
    }

    private void setServerSound(boolean serverSound) {
        this.serverSound = serverSound;
    }

    private void start(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            logger.debug("start {} seconds",seconds);
            timerElement2.callJsFunction("start", seconds, indefinite, silent, timerElement2,
                    Long.toString(System.currentTimeMillis()), from);
        }
    }

    private void stop(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            logger.debug("stop");
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("pause", seconds, indefinite, silent, timerElement2,
                    Long.toString(System.currentTimeMillis()), from);
        }
    }

}
