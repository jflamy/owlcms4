package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

import app.owlcms.fieldofplay.BreakType;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.DecisionReset;
import app.owlcms.fieldofplay.UIEvent.DownSignal;
import app.owlcms.fieldofplay.UIEvent.Decision;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class JuryDisplayDecisionElement extends DecisionElement {
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDisplayDecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	public JuryDisplayDecisionElement() {
		super();
		this.setJury(true);
	}

	
	@Override
	public void slaveReset(DecisionReset e) {
		// ignore
	}


	@Override
	public void slaveMajorityDecision(Decision e) {
		// ignore
	}


	@Override
	public void slaveDownSignal(DownSignal e) {
		// ignore
	}
	
	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			uiEventLogger.debug("*** {} referee update ({} {} {})",this.getOrigin(), e.ref1, e.ref2, e.ref3);
			this.getElement().callFunction("showDecisionsForJury", e.ref1, e.ref2, e.ref3, e.ref1Time, e.ref2Time, e.ref3Time);
		});
	}
	
	@Subscribe
	public void slaveStartTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			uiEventLogger.debug("*** {} startTime -> reset",this.getOrigin());
			this.getElement().callFunction("reset", false);
		});
	}
	
	@Subscribe
	public void slaveBreakStarted(UIEvent.BreakStarted e) {
		OwlcmsSession.withFop((fop) -> {
			if (fop.getBreakType() != BreakType.JURY) {
				// don't reset on a break we just created !
				UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
					uiEventLogger.debug("*** {} break start -> reset",this.getOrigin());
					this.getElement().callFunction("reset", false);
				});
			}
		});
	}
	
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		OwlcmsSession.withFop((fop) -> {
			UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
				uiEventLogger.debug("*** {} break start -> reset", this.getOrigin());
				this.getElement().callFunction("reset", false);
			});
		});
	}

}
