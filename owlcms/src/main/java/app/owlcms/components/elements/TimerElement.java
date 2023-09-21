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
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.internal.AllowInert;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
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
implements SafeEventBusRegistration, Focusable<Div> {

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
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	{
		this.logger.setLevel(Level.INFO);
		this.uiEventLogger.setLevel(Level.INFO);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
	}

	@AllowInert
	@ClientCallable
	abstract public void clientFinalWarning(String fopName);

	@AllowInert
	@ClientCallable
	abstract public void clientInitialWarning(String fopName);

	/**
	 * Client requests that the server send back the remaining time. Intended to be
	 * used after client has been hidden and is made visible again.
	 */
	@AllowInert
	@ClientCallable
	abstract public void clientSyncTime(String fopName);

	/**
	 * Timer ran down to zero.
	 */
	@AllowInert
	@ClientCallable
	abstract public void clientTimeOver(String fopName);

	@AllowInert
	@ClientCallable
	abstract public void clientTimerStarting(String fopName, double remainingTime, double lateMillis, String from);

	/**
	 * Timer has been stopped on the client side.
	 *
	 * @param remainingTime
	 */
	@AllowInert
	@ClientCallable
	abstract public void clientTimerStopped(String fopName, double remainingTime, String from);

	@Override
	public void focus() {
	}

	public boolean isServerSound() {
		return this.serverSound;
	}

	public void setSilenced(boolean b) {
		this.logger.debug("{} silenced = {} from {}", this.getClass().getSimpleName(), b, LoggerUtils.whereFrom(1));
		this.silenced = b;
	}

	public abstract void syncWithFopTimer();

	final protected long delta(long lastMillis) {
		if (lastMillis == 0) {
			return 0;
		} else {
			return System.currentTimeMillis() - lastMillis;
		}
	}

	protected final void doSetTimer(Integer milliseconds) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("doSetTimer {} {}", milliseconds, LoggerUtils.whereFrom());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			stop(getMsRemaining(), isIndefinite(), isSilenced(), parent);
			initTime(milliseconds);
		});
	}

	protected void doStartTimer(Integer milliseconds, boolean serverSound) {
		this.logger.debug("doStartTimer {}", milliseconds);
		setServerSound(serverSound);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setIndefinite(milliseconds == null);
			setMsRemaining(milliseconds);
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			this.lastStartMillis = System.currentTimeMillis();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("server starting timer {}, {}, {}", parent, milliseconds, this.lastStartMillis);
			}
			getElement().setProperty("silent", isSilent());
			start(milliseconds, isIndefinite(), isSilent(), parent);
		});
	}

	protected void doStopTimer(Integer milliseconds) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setMsRemaining(milliseconds);
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			this.lastStopMillis = System.currentTimeMillis();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("server stopping timer {}, {}, {}", parent, milliseconds, this.lastStopMillis);
			}
			stop(getMsRemaining(), isIndefinite(), isSilent(), parent);
		});
	}

	protected abstract IProxyTimer getFopTimer(FieldOfPlay fop);

	protected Element getTimerElement() {
		return this.timerElement;
	}

	protected void init(String fopName) {
		this.fopName = fopName;
		setTimerElement(this.getElement());
		double seconds = 0.00D;
		setMsRemaining(0);
		// setSilenced(true);
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
		this.vsession = VaadinSession.getCurrent();
	}

	protected boolean isIndefinite() {
		return this.indefinite;
	}

	protected boolean isSilenced() {
		return this.silenced;
	}

	/**
	 * No sound if sound is emitted on server, or if silenced through the interface.
	 *
	 * @return
	 */
	protected boolean isSilent() {
		return isServerSound() || (!isServerSound() && isSilenced());
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
			this.uiEventBus = uiEventBusRegister(this, fop);
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

	protected void setMsRemaining(Integer milliseconds) {
		// logger.debug("setMsRemaining {}",milliseconds);
		this.msRemaining = milliseconds;
	}

	protected void setServerSound(boolean serverSound) {
		this.serverSound = serverSound;
	}

	protected void setTimerElement(Element timerElement) {
		this.timerElement = timerElement;
	}

	protected void start(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
		Element timerElement2 = getTimerElement();
		if (timerElement2 != null && (indefinite || milliseconds != null)) {
			double seconds = (indefinite) ? 0.0D : milliseconds / 1000.0D;
			if (this instanceof BreakTimerElement) {
				if (this.logger.isDebugEnabled()) {
					this.logger.debug("start {}s", seconds);
				}
			}
			timerElement2.callJsFunction("start", seconds, indefinite, silent, timerElement2,
					Long.toString(System.currentTimeMillis()), from);
		}
	}

	@SuppressWarnings("unused")
	private String formatDuration(Integer milliseconds) {
		return (milliseconds != null && milliseconds >= 0) ? DurationFormatUtils.formatDurationHMS(milliseconds)
				: (milliseconds != null ? milliseconds.toString() : "-");
	}

	private Integer getMsRemaining() {
		return this.msRemaining;
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
			}
			setDisplay(milliseconds, isIndefinite(), isSilenced());
		} else {
			if (this instanceof BreakTimerElement) {
			}
			setDisplay(milliseconds, true, true);
		}
	}

	private void setDisplay(Integer milliseconds, Boolean indefinite, Boolean silent) {
		Element timerElement2 = getTimerElement();
		if (this instanceof BreakTimerElement) {// && this.logger.isDebugEnabled()) {
			this.logger.debug("setDisplay {} {}", milliseconds, timerElement2);
		}
		if (timerElement2 != null) {
			double seconds = indefinite ? 0.0D : (milliseconds != null ? milliseconds / 1000.0D : 0D);
			timerElement2.callJsFunction("display", seconds, indefinite, silent, timerElement2);
		}
	}

	private void stop(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
		Element timerElement2 = getTimerElement();
		if (timerElement2 != null && (indefinite || milliseconds != null)) {
			double seconds = (indefinite) ? 0.0D : milliseconds / 1000.0D;
			if (this instanceof BreakTimerElement) {
				this.logger.debug("stop {}s", seconds);
			}
			timerElement2.callJsFunction("pause", seconds, indefinite, silent, timerElement2,
					Long.toString(System.currentTimeMillis()), from);
		}
	}
}
