/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.littemplate.LitTemplate;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.prutils.SafeEventBusRegistrationPR;
import app.owlcms.publicresults.UpdateReceiverServlet;
import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.uievents.DecisionEvent;
import app.owlcms.uievents.TimerEvent;
import ch.qos.logback.classic.Logger;

/**
 * ExplicitDecision display element.
 */
@Tag("decision-element-pr")
@JsModule("./components/DecisionElementPR.js")
@Uses(Icon.class)
public class DecisionElementPR extends LitTemplate
        implements IFopName, SafeEventBusRegistrationPR {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElementPR.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    protected EventBus uiEventBus;
    protected EventBus fopEventBus;
    private UI ui;
    private String fopName;
    private boolean silenced;

    public DecisionElementPR() {
    }

    /** @see app.owlcms.components.elements.IFopName#getFopName() */
    @Override
    public String getFopName() {
        return this.fopName;
    }

    public boolean isSilenced() {
        return this.silenced;
    }

    /** @see app.owlcms.components.elements.IFopName#setFopName(java.lang.String) */
    @Override
    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    public void setJury(boolean juryMode) {
        // ignore input.d
        getElement().setProperty("jury", false);
    }

    public void setPublicFacing(boolean publicFacing) {
        getElement().setProperty("publicFacing", publicFacing);
    }

    public void setSilenced(boolean silenced) {
        this.silenced = silenced;
    }

    @Subscribe
    public void slaveDecision(DecisionEvent de) {
        if (getFopName() == null || de.getFopName() == null || !getFopName().contentEquals(de.getFopName())) {
            // logger.debug("slaveDecision self={}: {} ignored", getFopName(),
            // de.getFopName());
            // event is not for us
            return;
        }
        // logger.debug("DecisionElement DecisionEvent {} {} {}", de.getEventType(),
        // System.identityHashCode(de), ui);
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }

        this.ui.access(() -> {
            if (de.isBreak()) {
                logger.debug("break: slaveDecision disable");
                this.getElement().callJsFunction("setEnabled", false);
            } else {
                switch (de.getEventType()) {
                    case DOWN_SIGNAL:
                        logger.debug("showing down");
                        this.getElement().callJsFunction("showDown", false, isSilenced());
                        break;
                    case FULL_DECISION:
                        logger.debug("calling full decision");
                        this.getElement().callJsFunction("showDecisions", false, de.getDecisionLight1(),
                                de.getDecisionLight2(),
                                de.getDecisionLight3());
                        this.getElement().callJsFunction("setEnabled", false);
                        break;
                    case RESET:
                        logger.debug("calling reset");
                        getElement().callJsFunction("reset", false);
                        break;
                    default:
                        logger.error("unknown decision event type {}", de.getEventType());
                        break;
                }
            }
        });
    }

    @Subscribe
    public void slaveStartBreakTimer(BreakTimerEvent.BreakStart e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }

        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            // was true !?
            this.getElement().callJsFunction("setEnabled", false);
        });
    }

    @Subscribe
    public void slaveStartTimer(TimerEvent.StartTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            this.getElement().callJsFunction("setEnabled", true);
        });
    }

    @Subscribe
    public void slaveStopBreakTimer(BreakTimerEvent.BreakDone e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            this.getElement().callJsFunction("setEnabled", true);
        });
    }

    @Subscribe
    public void slaveStopBreakTimer(BreakTimerEvent.BreakPaused e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            this.getElement().callJsFunction("setEnabled", true);
        });
    }

    @Subscribe
    public void slaveStopTimer(TimerEvent.StopTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (this.ui == null || this.ui.isClosing()) {
            return;
        }
        this.ui.access(() -> {
            this.getElement().callJsFunction("setEnabled", true);
        });
    }

    protected Object getOrigin() {
        // we use the identity of our parent AttemptBoard or AthleteFacingAttemptBoard
        // to identify
        // our actions.
        return this.getParent().get();
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
     * AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        init();

        this.ui = attachEvent.getUI();
        eventBusRegister(this, UpdateReceiverServlet.getEventBus());
//        eventBusRegister(this, DecisionReceiverServlet.getEventBus());
//        eventBusRegister(this, TimerReceiverServlet.getEventBus());

        setFopName(OwlcmsSession.getFopName());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
    }

    private void init() {
        getElement().setProperty("publicFacing", true);
    }
}
