/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
/**
 * @author JF
 *
 */
@Tag("timer-element")
@JsModule("./components/TimerElement.js")
public abstract class TimerElement extends PolymerTemplate<TimerElement.TimerModel> {

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

    final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
    final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Element timerElement;
    protected UI ui;

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

    /**
     * Timer has been stopped on the client side.
     *
     * @param remainingTime
     */
    @ClientCallable
    abstract public void clientTimerStopped(double remainingTime);

    protected final void doSetTimer(Integer milliseconds) {
        ui.access(() -> {
            stop();
            setTimeRemaining(milliseconds);
        });
    }

    protected void doStartTimer(Integer milliseconds, boolean silent) {
        ui.access(() -> {
            setTimeRemaining(milliseconds);
            getModel().setSilent(silent);
            start();
        });
    }

    protected void doStopTimer() {
        ui.access(() -> {
            stop();
        });
    }

    protected Element getTimerElement() {
        return timerElement;
    }

    protected void init() {
        double seconds = 0.00D;
        UI.getCurrent().access(() -> {
            TimerModel model = getModel();
            model.setStartTime(0.0D);
            model.setCurrentTime(seconds);
            model.setCountUp(false);
            model.setRunning(false);
            model.setSilent(true);
            setTimerElement(this.getElement());
        });
    }


    protected void setTimerElement(Element timerElement) {
        this.timerElement = timerElement;
    }

    private void setTimeRemaining(Integer milliseconds) {
        logger.trace("=== time remaining = {} from {} ", milliseconds, LoggerUtils.whereFrom());
        TimerModel model = getModel();
        boolean indefinite = milliseconds == null;

        if (!indefinite) {
            logger.trace("not indefinite");
            double seconds = milliseconds / 1000.0D;
            model.setCurrentTime(seconds);
            model.setStartTime(seconds);
            model.setIndefinite(false);
        } else {
            logger.trace("indefinite");
            model.setIndefinite(true);
            model.setSilent(true);
        }
        // should not be necessary
        getTimerElement().callJsFunction("reset");
    }

    private void start() {
        getTimerElement().callJsFunction("start");
    }

    private void stop() {
        getTimerElement().callJsFunction("pause");
    }

}
