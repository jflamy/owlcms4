/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.publicresults.DecisionReceiverServlet;
import app.owlcms.publicresults.TimerReceiverServlet;
import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.uievents.DecisionEvent;
import app.owlcms.uievents.TimerEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * ExplicitDecision display element.
 */
@Tag("decision-element-pr")
@JsModule("./components/DecisionElement.js")
public class DecisionElementPR extends PolymerTemplate<DecisionElementPR.DecisionModel> implements IFopName {

    /**
     * The Interface DecisionModel.
     */
    public interface DecisionModel extends TemplateModel {

        boolean isEnabled();

        boolean isJury();

        boolean isPublicFacing();

        void setEnabled(boolean b);

        void setJury(boolean juryMode);

        void setPublicFacing(boolean publicFacing);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElementPR.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    protected EventBus uiEventBus;
    protected EventBus fopEventBus;
    private UI ui;
    private String fopName;

    public DecisionElementPR() {
    }

    /** @see app.owlcms.components.elements.IFopName#getFopName() */
    @Override
    public String getFopName() {
        return this.fopName;
    }

    public boolean isPublicFacing() {
        return getModel().isPublicFacing();
    }

    /** @see app.owlcms.components.elements.IFopName#setFopName(java.lang.String) */
    @Override
    public void setFopName(String fopName) {
        this.fopName = fopName;
    }

    public void setJury(boolean juryMode) {
        getModel().setJury(false);
    }

    public void setPublicFacing(boolean publicFacing) {
        getModel().setPublicFacing(publicFacing);
    }

    @Subscribe
    public void slaveDecision(DecisionEvent de) {
        if (getFopName() == null || de.getFopName() == null || !getFopName().contentEquals(de.getFopName())) {
            // event is not for us
            return;
        }
        logger.debug("DecisionElement DecisionEvent {} {}", de.getEventType(), System.identityHashCode(de));
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            if (de.isBreak()) {
                logger.debug("break: slaveDecision disable");
                getModel().setEnabled(false);
            } else {
                switch (de.getEventType()) {
                case DOWN_SIGNAL:
                    logger.debug("showing down");
                    this.getElement().callJsFunction("showDown", false, false);
                    break;
                case FULL_DECISION:
                    logger.debug("calling full decision");
                    this.getElement().callJsFunction("showDecisions", false, de.getDecisionLight1(),
                            de.getDecisionLight2(),
                            de.getDecisionLight3());
                    getModel().setEnabled(false);
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

        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            getModel().setEnabled(true);
        });
    }

    @Subscribe
    public void slaveStartTimer(TimerEvent.StartTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            getModel().setEnabled(true);
        });
    }

    @Subscribe
    public void slaveStopBreakTimer(BreakTimerEvent.BreakDone e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            getModel().setEnabled(true);
        });
    }

    @Subscribe
    public void slaveStopBreakTimer(BreakTimerEvent.BreakPaused e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            getModel().setEnabled(true);
        });
    }

    @Subscribe
    public void slaveStopTimer(TimerEvent.StopTime e) {
        if (getFopName() == null || e.getFopName() == null || !getFopName().contentEquals(e.getFopName())) {
            // event is not for us
            return;
        }
        if (ui == null || ui.isClosing()) {
            return;
        }
        ui.access(() -> {
            getModel().setEnabled(true);
        });
    }

    protected Object getOrigin() {
        // we use the identity of our parent AttemptBoard or AthleteFacingAttemptBoard
        // to identify
        // our actions.
        return this.getParent().get();
    }

    /*
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        init();

        DecisionReceiverServlet.getEventBus().register(this);
        TimerReceiverServlet.getEventBus().register(this);
        setFopName((String) OwlcmsSession.getAttribute("fopName"));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.ui = null;
        try {
            DecisionReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
        try {
            TimerReceiverServlet.getEventBus().unregister(this);
        } catch (Exception e) {
        }
    }

    private void init() {
        DecisionModel model = getModel();
        model.setPublicFacing(true);
    }
}
