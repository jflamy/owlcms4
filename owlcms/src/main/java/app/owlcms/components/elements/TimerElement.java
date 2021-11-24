/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings("serial")
@Tag("timer-element")
@JsModule("./components/TimerElement.js")
public abstract class TimerElement extends PolymerTemplate<TimerElement.TimerModel>
        implements SafeEventBusRegistration {

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
         * Checks if timer is silenced.
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

    final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private EventBus uiEventBus;
    private Element timerElement;
    private boolean indefinite;
    private Integer msRemaining;
    private boolean silenced = true;
    protected VaadinSession vsession;
    public long lastStartMillis;
    public long lastStopMillis;
    private boolean serverSound;

    /**
     * Instantiates a new timer element.
     */
    public TimerElement() {
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

    @ClientCallable
    abstract public void clientTimerStarting(double remainingTime, double lateMillis, String from);

    /**
     * Timer has been stopped on the client side.
     *
     * @param remainingTime
     */
    @ClientCallable
    abstract public void clientTimerStopped(double remainingTime, String from);

    public boolean isServerSound() {
        return serverSound;
    }

    public void setSilenced(boolean b) {
        logger.debug("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
        silenced = b;
    }

    protected final void doSetTimer(Integer milliseconds) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            stop(getMsRemaining(), isIndefinite(), isSilenced(), parent);
            initTime(milliseconds);
        });
    }

    protected void doStartTimer(Integer milliseconds, boolean serverSound) {
        setServerSound(serverSound);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            setIndefinite(milliseconds == null);
            setMsRemaining(milliseconds);
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            lastStartMillis = System.currentTimeMillis();
            logger.trace("server starting timer {}, {}, {}", parent, milliseconds, lastStartMillis);
            getModel().setSilent(isSilent());
            start(milliseconds, isIndefinite(), isSilent(), parent);
        });
    }

    protected void doStopTimer(Integer milliseconds) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            setMsRemaining(milliseconds);
            String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
            lastStopMillis = System.currentTimeMillis();
            logger.trace("server stopping timer {}, {}, {}", parent, milliseconds, lastStopMillis);
            stop(getMsRemaining(), isIndefinite(), isSilent(), parent);
        });
    }

    protected Element getTimerElement() {
        return timerElement;
    }

    protected void init() {
        setTimerElement(this.getElement());
        double seconds = 0.00D;
        setMsRemaining(0);
//        setSilenced(true);
        setIndefinite(false);
        if (UI.getCurrent() == null) {
            return;
        }
        UI.getCurrent().access(() -> {
            TimerModel model = getModel();
            model.setStartTime(0.0D);
            model.setCurrentTime(seconds);
            model.setCountUp(false);
            model.setRunning(false);
            model.setSilent(true);
        });
        vsession = VaadinSession.getCurrent();
    }

    protected boolean isIndefinite() {
        return indefinite;
    }

    protected boolean isSilenced() {
        return silenced;
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        init();
        OwlcmsSession.withFop(fop -> {
            // sync with current status of FOP
            doSetTimer(fop.getAthleteTimer().getTimeRemaining());
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // tell the javascript to stay quiet
        setSilenced(true);
        setTimerElement(null);
        getModel().setSilent(true);
    }

    protected void setIndefinite(boolean indefinite) {
        this.indefinite = indefinite;
    }

    protected void setTimerElement(Element timerElement) {
        this.timerElement = timerElement;
    }

    private String formatDuration(Integer milliseconds) {
        return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds) : milliseconds.toString();
    }

    private Integer getMsRemaining() {
        return msRemaining;
    }

    private void initTime(Integer milliseconds) {
        if (this instanceof BreakTimerElement) {
            logger.trace("set time remaining = {} from {} ", formatDuration(milliseconds), LoggerUtils.whereFrom());
        }
        setIndefinite(milliseconds == null);
        setMsRemaining(milliseconds);

        if (!isIndefinite()) {
            if (this instanceof BreakTimerElement) {
                logger.trace("not indefinite {}", formatDuration(milliseconds));
            }
            setDisplay(milliseconds, isIndefinite(), isSilenced());
        } else {
            if (this instanceof BreakTimerElement) {
                logger.trace("indefinite");
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
        msRemaining = milliseconds;
    }

    private void setServerSound(boolean serverSound) {
        this.serverSound = serverSound;
    }

    private void start(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("start", seconds, indefinite, silent, timerElement2,
                    Long.toString(System.currentTimeMillis()), from);
        }
    }

    private void stop(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
        Element timerElement2 = getTimerElement();
        if (timerElement2 != null) {
            double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
            timerElement2.callJsFunction("pause", seconds, indefinite, silent, timerElement2,
                    Long.toString(System.currentTimeMillis()), from);
        }
    }

}
