/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
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
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Countdown timer element.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("timer-element")
@JsModule("./components/TimerElement.js")
public abstract class TimerElement extends LitTemplate
        implements SafeEventBusRegistration {

	public long lastStartMillis;
	public long lastStopMillis;

	protected String fopName;

	protected VaadinSession vsession;
	private boolean indefinite;
	final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
	private Integer msRemaining;
	private boolean serverSound;
	private boolean silenced = true;
	private Element timerElement;
	protected EventBus uiEventBus;
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	{
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
	}

	@ClientCallable
	abstract public void clientFinalWarning(String fopName);

	@ClientCallable
	abstract public void clientInitialWarning(String fopName);

	/**
	 * Client requests that the server send back the remaining time. Intended to be
	 * used after client has been hidden and is made visible again.
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

	public boolean isServerSound() {
		return serverSound;
	}

	public void setSilenced(boolean b) {
		logger.debug("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
		silenced = b;
	}

	@SuppressWarnings("unused")
	private String formatDuration(Integer milliseconds) {
		return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
		        : (milliseconds != null ? milliseconds.toString() : "-");
	}

	private Integer getMsRemaining() {
		return msRemaining;
	}

	private void initTime(Integer milliseconds) {
		if (this instanceof BreakTimerElement) {
			// logger.trace("set time remaining = {} from {} ",
			// formatDuration(milliseconds), LoggerUtils.whereFrom());
		}
		setIndefinite(milliseconds == null);
		setMsRemaining(milliseconds);

		if (!isIndefinite()) {
			if (this instanceof BreakTimerElement) {
				// logger.trace("not indefinite {}", formatDuration(milliseconds));

//                this.getElement()
//                        .setVisible(OwlcmsSession.getFop().getBreakType() == BreakType.GROUP_DONE && milliseconds <= 0);
			}
			setDisplay(milliseconds, isIndefinite(), isSilenced());
		} else {
			if (this instanceof BreakTimerElement) {
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
	protected boolean isSilent() {
		return isServerSound() || (!isServerSound() && isSilenced());
	}

	private void setDisplay(Integer milliseconds, Boolean indefinite, Boolean silent) {
		Element timerElement2 = getTimerElement();
		// logger.debug("setDisplay {} {}",milliseconds, timerElement2);
		if (timerElement2 != null) {
			double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
			timerElement2.callJsFunction("display", seconds, indefinite, silent, timerElement2);
		}
	}

	protected void setMsRemaining(Integer milliseconds) {
		// logger.debug("setMsRemaining {}",milliseconds);
		msRemaining = milliseconds;
	}

	protected void setServerSound(boolean serverSound) {
		this.serverSound = serverSound;
	}

	protected void start(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
		Element timerElement2 = getTimerElement();
		if (timerElement2 != null) {
			double seconds = indefinite ? 0.0D : milliseconds / 1000.0D;
			logger.debug("start {}s",seconds);
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

	protected final void doSetTimer(Integer milliseconds) {
		// logger.debug("doSetTimer {}",milliseconds);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			stop(getMsRemaining(), isIndefinite(), isSilenced(), parent);
			initTime(milliseconds);
		});
	}

	protected void doStartTimer(Integer milliseconds, boolean serverSound) {
		logger.debug("doStartTimer {}",milliseconds);
		setServerSound(serverSound);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setIndefinite(milliseconds == null);
			setMsRemaining(milliseconds);
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			lastStartMillis = System.currentTimeMillis();
			//logger.debug("server starting timer {}, {}, {}", parent, milliseconds, lastStartMillis);
			getElement().setProperty("silent", isSilent());
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

	protected void init(String fopName) {
		this.fopName = fopName;
		setTimerElement(this.getElement());
		double seconds = 0.00D;
		setMsRemaining(0);
//        setSilenced(true);
		setIndefinite(false);
		if (UI.getCurrent() == null) {
			return;
		}
		// logger.debug("init 0");
		UI.getCurrent().access(() -> {
			getElement().setProperty("startTime", 0.0D);
			getElement().setProperty("currentTime", seconds);
			getElement().setProperty("countUp", false);
			getElement().setProperty("running", false);
			getElement().setProperty("silent", true);
			getElement().setProperty("fopName", fopName);
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
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsSession.withFop(fop -> {
			init(fop.getName());
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
		getElement().setProperty("silent", true);
	}

	protected void setIndefinite(boolean indefinite) {
		this.indefinite = indefinite;
	}

	protected void setTimerElement(Element timerElement) {
		this.timerElement = timerElement;
	}

}
