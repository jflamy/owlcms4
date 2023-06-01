/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * ExplicitDecision display element.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("decision-element")
@JsModule("./components/DecisionElement.js")
public class DecisionElement extends PolymerTemplate<TemplateModel>
        implements SafeEventBusRegistration {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(DecisionElement.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	protected EventBus fopEventBus;
	protected EventBus uiEventBus;
	private boolean silenced;
//    private Boolean prevRef1;
//    private Boolean prevRef2;
//    private Boolean prevRef3;
//    private MqttAsyncClient client;
	private boolean juryMode;

	public DecisionElement() {
//        if (isMqttDecisions()) {
//            try {
//                client = MQTTMonitor.createMQTTClient(OwlcmsSession.getFop());
//                String userName = StartupUtils.getStringParam("mqttUserName");
//                String password = StartupUtils.getStringParam("mqttPassword");
//                MqttConnectOptions connOpts = setUpConnectionOptions(userName != null ? userName : "",
//                        password != null ? password : "");
//                ;
//                client.connect(connOpts).waitForCompletion();
//            } catch (MqttException e) {
//            }
//        }
	}

//    private MqttConnectOptions setUpConnectionOptions(String username, String password) {
//        MqttConnectOptions connOpts = new MqttConnectOptions();
//        connOpts.setCleanSession(true);
//        if (username != null) {
//            connOpts.setUserName(username);
//        }
//        if (password != null) {
//            connOpts.setPassword(password.toCharArray());
//        }
//        connOpts.setCleanSession(true);
//        // connOpts.setAutomaticReconnect(true);
//        return connOpts;
//    }

	/**
	 * @return the silenced
	 */
	public boolean isSilenced() {
		return silenced;
	}

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or
	 * break
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
		Object origin = this.getOrigin();
		OwlcmsSession.withFop((fop) -> {
			if (!fopName.contentEquals(fop.getName())) {
				return;
			}
			// logger.debug("master referee update {} ({} {} {})", fop.getCurAthlete(),
			// ref1, ref2, ref3, ref1Time,
			// ref2Time, ref3Time);
//            if (isMqttDecisions()) {
//                if (ref1 != null && prevRef1 != ref1) {
//                    // logger.debug("update 1 {}", ref1);
//                    mqttPublish("owlcms/decision/A", "1 " + (ref1 ? "good" : "bad"));
//                    prevRef1 = ref1;
//                }
//                if (ref2 != null && prevRef2 != ref2) {
//                    // logger.debug("update 2 {}", ref2);
//                    mqttPublish("owlcms/decision/A", "2 " + (ref2 ? "good" : "bad"));
//                    prevRef2 = ref2;
//                }
//                if (ref3 != null && prevRef3 != ref3) {
//                    // logger.debug("update 3 {}", ref3);
//                    mqttPublish("owlcms/decision/A", "3 " + (ref3 ? "good" : "bad"));
//                    prevRef3 = ref3;
//                }
//            } else {
			fop.fopEventPost(
			        new FOPEvent.DecisionFullUpdate(origin, fop.getCurAthlete(), ref1, ref2, ref3,
			                Long.valueOf(ref1Time),
			                Long.valueOf(ref2Time), Long.valueOf(ref3Time), false));
//            }
		});

	}

//    private void mqttPublish(String topic, String message) {
//        try {
//            client.publish(topic, new MqttMessage(message.getBytes(StandardCharsets.UTF_8)));
//        } catch (MqttException e) {
//            throw new RuntimeException(e);
//        }
//    }

//    private boolean isMqttDecisions() {
//        return Config.getCurrent().featureSwitch("mqttDecisions");
//    }

	@ClientCallable
	/**
	 * client side only sends after timer has been started until decision reset or
	 * break
	 *
	 * @param decision
	 * @param ref1
	 * @param ref2
	 * @param ref3
	 */
	public void masterShowDown(String fopName, Boolean decision, Boolean ref1, Boolean ref2, Boolean ref3) {
		Object origin = this.getOrigin();
		// logger.debug("=== master {} down: decision={} ({} {} {})", origin,
		// decision.getClass().getSimpleName(), ref1,
		// ref2, ref3);
		OwlcmsSession.getFop().fopEventPost(new FOPEvent.DownSignal(origin));
	}

	public void setJury(boolean juryMode) {
		this.setJuryMode(juryMode);
		getElement().setProperty("jury", juryMode);
	}

	public void setPublicFacing(boolean publicFacing) {
		getElement().setProperty("publicFacing", publicFacing);
	}

	public void setSilenced(boolean b) {
		//logger.debug("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
		getElement().setProperty("silent", b);
		silenced = b;
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			logger.debug("slaveBreakStart disable");
			this.getElement().callJsFunction("setEnabled", false);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			getElement().callJsFunction("reset", false);
//            prevRef1 = null;
//            prevRef2 = null;
//            prevRef3 = null;
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		// logger.trace("slaveDownSignal {} {} {}", this, this.getOrigin(),
		// e.getOrigin());
		if (isJuryMode() || (this.getOrigin() == e.getOrigin())) {
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
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			getElement().callJsFunction("reset", false);
//            prevRef1 = null;
//            prevRef2 = null;
//            prevRef3 = null;
		});
	}

	@Subscribe
	public void slaveShowDecision(UIEvent.Decision e) {
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), () -> {
			uiEventLogger.debug("*** {} majority decision ({})", this.getOrigin(),
			        this.getParent().get().getClass().getSimpleName());
			this.getElement().callJsFunction("showDecisions", false, e.ref1, e.ref2, e.ref3);
			this.getElement().callJsFunction("setEnabled", false);
		});
	}

	@Subscribe
	public void slaveStartTimer(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			logger.debug("slaveStartTimer enable");
			this.getElement().callJsFunction("setEnabled", true);
		});
	}

	@Subscribe
	public void slaveStopTimer(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			logger.debug("slaveStopTimer enable");
			this.getElement().callJsFunction("setEnabled", true);
		});
	}

	private void init(String fopName) {
		getElement().setProperty("publicFacing", true);
		getElement().setProperty("fopName", fopName);

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

	protected boolean isJuryMode() {
		return juryMode;
	}

	private void setJuryMode(boolean juryMode) {
		this.juryMode = juryMode;
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
		super.onAttach(attachEvent);
		OwlcmsSession.withFop(fop -> {
			init(fop.getName());
			// we send on fopEventBus, listen on uiEventBus.
			fopEventBus = fop.getFopEventBus();
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
}
