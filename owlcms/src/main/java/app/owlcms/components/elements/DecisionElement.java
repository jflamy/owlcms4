/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * ExplicitDecision display element.
 */
@SuppressWarnings("serial")
@Tag("decision-element")
@JsModule("./components/DecisionElement.js")
public class DecisionElement extends PolymerTemplate<DecisionElement.DecisionModel>
        implements SafeEventBusRegistration {

    /**
     * The Interface DecisionModel.
     */
    public interface DecisionModel extends TemplateModel {

        boolean isEnabled();

        boolean isJury();

        boolean isPublicFacing();

        void setEnabled(boolean b);

        void setFopName(String fopName);

        void setJury(boolean juryMode);

        void setPublicFacing(boolean publicFacing);

        void setSilent(boolean b);
    }

    final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    protected EventBus fopEventBus;
    protected EventBus uiEventBus;
    private boolean silenced;

    public DecisionElement() {
    }

    public boolean isPublicFacing() {
        return getModel().isPublicFacing();
    }

    /**
     * @return the silenced
     */
    public boolean isSilenced() {
        return silenced;
    }

    @ClientCallable
    /**
     * client side only sends after timer has been started until decision reset or break
     *
     * @param ref1
     * @param ref2
     * @param ref3
     * @param ref1Time
     * @param ref2Time
     * @param ref3Time
     */
    public void masterRefereeUpdate(String fopName, Boolean ref1, Boolean ref2, Boolean ref3, Integer ref1Time,
            Integer ref2Time,
            Integer ref3Time) {
        logger.debug("master referee decision update");
        Object origin = this.getOrigin();
        OwlcmsSession.withFop((fop) -> {
            if (!fopName.contentEquals(fop.getName())) {
                return;
            }
            logger.debug("master referee update {} ({} {} {})", fop.getCurAthlete(), ref1, ref2, ref3, ref1Time,
                    ref2Time,
                    ref3Time);
            fop.fopEventPost(new FOPEvent.DecisionFullUpdate(origin, fop.getCurAthlete(), ref1, ref2, ref3, ref1Time,
                    ref2Time, ref3Time));
        });

    }

    @ClientCallable
    /**
     * client side only sends after timer has been started until decision reset or break
     *
     * @param decision
     * @param ref1
     * @param ref2
     * @param ref3
     */
    public void masterShowDown(String fopName, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
        Object origin = this.getOrigin();
        logger.debug("=== master {} down: decision={} ({} {} {})", origin, decision.getClass().getSimpleName(), ref1,
                ref2, ref3);
        OwlcmsSession.getFop().fopEventPost(new FOPEvent.DownSignal(origin));
    }

    public void setJury(boolean juryMode) {
        getModel().setJury(juryMode);
    }

    public void setPublicFacing(boolean publicFacing) {
        getModel().setPublicFacing(publicFacing);
    }

    public void setSilenced(boolean b) {
        // logger.trace("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
        getModel().setSilent(b);
        silenced = b;
    }

    @Subscribe
    public void slaveBreakStart(UIEvent.BreakStarted e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            logger.debug("slaveBreakStart disable");
            this.getElement().callJsFunction("setEnabled", false);
            // getModel().setEnabled(false);
        });
    }

    @Subscribe
    public void slaveDownSignal(UIEvent.DownSignal e) {
        // logger.trace("slaveDownSignal {} {} {}", this, this.getOrigin(), e.getOrigin());
        if (this.getOrigin() == e.getOrigin()) {
            // we emitted the down signal, don't do it again.
            // logger.trace("skipping down, {} is origin",this.getOrigin());
            return;
        }
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            uiEventLogger.debug("!!! {} down ({})", this.getOrigin(),
                    this.getParent().get().getClass().getSimpleName());
            this.getElement().callJsFunction("showDown", false,
                    isSilenced() || OwlcmsSession.getFop().isEmitSoundsOnServer());
        });
    }

    @Subscribe
    public void slaveReset(UIEvent.DecisionReset e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            getElement().callJsFunction("reset", false);
            logger.debug("slaveReset disable");
        });
    }

    @Subscribe
    public void slaveShowDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            uiEventLogger.debug("*** {} majority decision ({})", this.getOrigin(),
                    this.getParent().get().getClass().getSimpleName());
            this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
            this.getElement().callJsFunction("setEnabled", false);
            // getModel().setEnabled(false);
        });
    }

    @Subscribe
    public void slaveStartTimer(UIEvent.StartTime e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            logger.debug("slaveStartTimer enable");
            this.getElement().callJsFunction("setEnabled", true);
            // getModel().setEnabled(true);
        });
    }

    @Subscribe
    public void slaveStopTimer(UIEvent.StopTime e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            logger.debug("slaveStopTimer enable");
            this.getElement().callJsFunction("setEnabled", true);
            // getModel().setEnabled(true);
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
        super.onAttach(attachEvent);
        OwlcmsSession.withFop(fop -> {
            init(fop.getName());
            // we send on fopEventBus, listen on uiEventBus.
            fopEventBus = fop.getFopEventBus();
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    private void init(String fopName) {
        DecisionModel model = getModel();
        model.setPublicFacing(true);
        model.setFopName(fopName);

        Element elem = this.getElement();
        elem.addPropertyChangeListener("ref1", "ref1-changed", (e) -> {
            uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
        });
        elem.addPropertyChangeListener("ref2", "ref2-changed", (e) -> {
            uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
        });
        elem.addPropertyChangeListener("ref3", "ref3-changed", (e) -> {
            uiEventLogger.trace(e.getPropertyName() + " changed to " + e.getValue());
        });
        elem.addPropertyChangeListener("decision", "decision-changed", (e) -> {
            uiEventLogger.debug(e.getPropertyName() + " changed to " + e.getValue());
        });
    }
}
