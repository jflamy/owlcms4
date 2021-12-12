/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.publicresults.DecisionReceiverServlet;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@Tag("timer-element")
@JsModule("./components/TimerElement.js")
public abstract class TimerElementPR extends PolymerTemplate<TimerElementPR.TimerModel> implements IFopName, SafeEventBusRegistrationPR {

    /**
     * TimerModel Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties.
     * When the JS properties are changed, a "propname-changed" event is triggered.
     * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
     */
    public interface TimerModel extends TemplateModel {

        /**
         * Gets the current time.
         *
         * @return the current time
         */
        double getCurrentTime();

        /**
         * Gets the start time (the time to which the timer will reset)
         *
         * @return the start time
         */
        double getStartTime();

        /**
         * Checks if is counting up.
         *
         * @return true, if is counting up
         */
        boolean isCountUp();

        boolean isIndefinite();

        /**
         * Checks if timer is running.
         *
         * @return true, if is running
         */
        boolean isRunning();

        /**
         * Checks if timer is silent.
         *
         * @return true, if sounds are to be emitted.
         */
        boolean isSilent();

        /**
         * Sets the count direction.
         *
         * @param countUp counts up if true, down if false.
         */
        void setCountUp(boolean countUp);

        /**
         * Sets the current time.
         *
         * @param seconds the new current time
         */
        void setCurrentTime(double seconds);

        /**
         * If indefinite, the timer doesn't start or stop, it just stays there with --:--
         *
         * @param b
         */
        void setIndefinite(boolean b);

        /**
         * Sets the timer running with true, stops if false.
         *
         * @param running setting to true starts the timer, false stops it.
         */
        void setRunning(boolean running);

        /**
         * Determine whether sounds are emitted at 90, 30 and 0 seconds
         *
         * @param quiet true indicates no sound
         */
        void setSilent(boolean quiet);

        /**
         * Sets the start time (the time to which the timer will reset)
         *
         * @param seconds the new start time
         */
        void setStartTime(double seconds);
    }

    private String fopName;

    final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElementPR.class);
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Element timerElement;
    private boolean indefinite;
    private Integer msRemaining;
    private boolean silent;
    protected UI ui;

    /**
     * Instantiates a new timer element.
     */
    public TimerElementPR() {
    }

    @ClientCallable
    abstract public void clientFinalWarning();

    @ClientCallable
    abstract public void clientInitialWarning();

    /**
     * Client requests that the server send back the remaining time. Intended to be used after client has been hidden
     * and is made visible again.
     */
    @ClientCallable
    abstract public void clientSyncTime();

    /**
     * Timer ran down to zero.
     */
    @ClientCallable
    abstract public void clientTimeOver();

    /**
     * Timer has been stopped on the client side.
     *
     * @param remainingTime
     */
    @ClientCallable
    abstract public void clientTimerStopped(double remainingTime);

    /** @see app.owlcms.components.elements.IFopName#getFopName() */
    @Override
    public String getFopName() {
        return this.fopName;
    }

    /** @see app.owlcms.components.elements.IFopName#setFopName(java.lang.String) */
    @Override
    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    protected final void doSetTimer(Integer milliseconds) {
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            stop(getMsRemaining(), isIndefinite(), isSilent());
            initTime(milliseconds);
        });
    }

    protected void doStartTimer(Integer milliseconds, boolean silent) {
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            setIndefinite(milliseconds == null);
            setMsRemaining(milliseconds);
            getModel().setSilent(silent);
            start(milliseconds, isIndefinite(), isSilent());
        });
    }

    protected void doStopTimer() {
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            stop(getMsRemaining(), isIndefinite(), isSilent());
        });
    }

    protected Element getTimerElement() {
        return timerElement;
    }

    protected void init() {
        setTimerElement(this.getElement());
        double seconds = 0.00D;
        setMsRemaining(0);
        setSilent(true);
        setIndefinite(false);
        UI.getCurrent().access(() -> {
            TimerModel model = getModel();
            model.setStartTime(0.0D);
            model.setCurrentTime(seconds);
            model.setCountUp(false);
            model.setRunning(false);
            model.setSilent(true);
        });
    }

    protected boolean isIndefinite() {
        return indefinite;
    }

    protected boolean isSilent() {
        return silent;
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        init();

        eventBusRegister(this, TimerReceiverServlet.getEventBus());
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());
        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
        
        setFopName((String) OwlcmsSession.getAttribute("fopName"));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;

        try {
            TimerReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }

        // tell the javascript to stay quiet
        setSilent(true);
        setTimerElement(null);
        getModel().setSilent(true);
    }

    protected void setIndefinite(boolean indefinite) {
        this.indefinite = indefinite;
    }

    protected void setSilent(boolean b) {
        silent = b;
    }

    protected void setTimerElement(Element timerElement) {
        this.timerElement = timerElement;
    }

    private String formatDuration(Integer milliseconds) {
        return milliseconds != null ? DurationFormatUtils.formatDurationHMS(milliseconds) : null;
    }

    private Integer getMsRemaining() {
        return msRemaining;
    }

    private void initTime(Integer milliseconds) {
        if (this instanceof BreakTimerElementPR) {
            logger.debug("set time remaining = {} from {} ", formatDuration(milliseconds), LoggerUtils.whereFrom());
        }
        setIndefinite(milliseconds == null);
        setMsRemaining(milliseconds);

        if (!isIndefinite()) {
            if (this instanceof BreakTimerElementPR) {
                logger.debug("not indefinite {}", formatDuration(milliseconds));
            }
            setDisplay(milliseconds, isIndefinite(), isSilent());
        } else {
            if (this instanceof BreakTimerElementPR) {
                logger.debug("indefinite");
            }
            setDisplay(milliseconds, true, true);
        }
    }

    private void setDisplay(Integer milliseconds, Boolean indefinite, Boolean silent) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("display", seconds, indefinite, silent, timerElement2);
        }
    }

    private void setMsRemaining(Integer milliseconds) {
        msRemaining = milliseconds;
    }

    private void start(Integer milliseconds, Boolean indefinite, Boolean silent) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("start", seconds, indefinite, silent, timerElement2);
        }
    }

    private void stop(Integer milliseconds, Boolean indefinite, Boolean silent) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("pause", seconds, indefinite, silent, timerElement2);
        }
    }
}
