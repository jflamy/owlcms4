/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.Decision;
import app.owlcms.uievents.UIEvent.DecisionReset;
import app.owlcms.uievents.UIEvent.DownSignal;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends DecisionElement {
    final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    public JuryDisplayDecisionElement() {
        super();
        this.setJury(true);
        this.getElement().getStyle().set("font-size", "19vh");
    }

    @Subscribe
    public void slaveBreakDone(UIEvent.BreakDone e) {
        OwlcmsSession.withFop((fop) -> {
            UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
                uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
                this.getElement().callJsFunction("reset", false);
            });
        });
    }

    @Subscribe
    public void slaveBreakStarted(UIEvent.BreakStarted e) {
        if (e.isDisplayToggle()) {
            return;
        }
        OwlcmsSession.withFop((fop) -> {
            if (fop.getBreakType() != BreakType.JURY) {
                // don't reset on a break we just created !
                UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
                    uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
                    this.getElement().callJsFunction("reset", false);
                });
            }
        });
    }

    @Override
    public void slaveDownSignal(DownSignal e) {
        // ignore
    }

    @Subscribe
    public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            uiEventLogger.debug("*** {} referee update ({} {} {})", this.getOrigin(), e.ref1, e.ref2, e.ref3);
            this.getElement().callJsFunction("showDecisionsForJury", e.ref1, e.ref2, e.ref3, e.ref1Time, e.ref2Time,
                    e.ref3Time);
        });
    }

    @Override
    public void slaveReset(DecisionReset e) {
        // ignore
    }

    @Override
    public void slaveShowDecision(Decision e) {
        // ignore
    }

    @Subscribe
    public void slaveStartTime(UIEvent.StartTime e) {
        UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
            uiEventLogger.debug("*** {} startTime -> reset", this.getOrigin());
            this.getElement().callJsFunction("reset", false);
        });
    }

}
