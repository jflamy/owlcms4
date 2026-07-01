/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import java.util.concurrent.atomic.AtomicBoolean;

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
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.server.VaadinSession;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.Main;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.IProxyTimer;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonObject;

/**
 * Countdown timer element.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("timer-element")
@JsModule("./components/TimerElement.js")
public abstract class TimerElement extends LitTemplate
		implements SafeEventBusRegistration, Focusable<Div> {

	// Note: onAttach is abstract - subclasses must implement and call
	// super.onAttach() first
	private static final String TRACE_TIMER_CLIENT_RENDER_SCRIPT = "return new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(() => {"
			+ " const timer = this.shadowRoot && this.shadowRoot.querySelector('#timer');"
			+ " const text = timer && timer.innerText ? timer.innerText.trim().replace(/\\s+/g, ' ') : '';"
			+ " const payload = this.timerCommandPayload || {};"
			+ " resolve(\"payloadCommand='\" + (payload.command || '') + \"' payloadSeconds=\" + payload.seconds"
			+ " + \" display='\" + text + \"' running=\" + this.running + \" currentTime=\" + this.currentTime);"
			+ "})));";
	private static final long SERVER_RUNNING_CHECK_STOP_GRACE_MILLIS = 750L;
	private static final AtomicBoolean playwrightTimerTripwireTriggered = new AtomicBoolean(false);

	public long lastStartMillis;
	public long lastStopMillis;
	protected String fopName;
	protected FieldOfPlay fop;
	protected VaadinSession vsession;
	private boolean indefinite;
	private final String instanceId = Integer.toHexString(System.identityHashCode(this));
	protected final AtomicBoolean attached = new AtomicBoolean(false);
	final private Logger logger = (Logger) LoggerFactory.getLogger(TimerElement.class);
	private Integer msRemaining;
	private boolean serverSound;
	private boolean silenced = true;
	private Element timerElement;
	private long timerCommandSequence;
	public long lastClientStoppedSequence;
	public long lastClientStoppedMillis;
	private long timerSettingsSequence;
	private volatile long recentStopCommandMillis;
	private Object origin;
	protected EventBus uiEventBus;
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	protected UI ui;
	{
		this.logger.setLevel(Level.WARN);
		this.uiEventLogger.setLevel(Level.WARN);
	}

	/**
	 * Instantiates a new timer element.
	 */
	public TimerElement() {
	}

	public void setFop(FieldOfPlay fop) {
		FieldOfPlay previousFop = this.fop;
		if (previousFop == fop) {
			return; // no change: avoid redundant re-register / timer resync
		}
		if (previousFop != null && this.uiEventBus != null) {
			unregister(this, previousFop.getUiEventBus());
		}
		this.fop = fop;
		if (this.getUI().isPresent()) {
			onFopAssignedWhileAttached();
		}
	}

	protected void onFopAssignedWhileAttached() {
	}

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	protected Object getOrigin() {
		return this.origin;
	}

	@Override
	public void focus() {
	}

	public boolean isServerSound() {
		return this.serverSound;
	}

	public void setSilenced(boolean b) {
		// this.logger.debug("{} silenced = {} from {}",
		// this.getClass().getSimpleName(), b, LoggerUtils.stackTrace());
		this.silenced = b;
		syncTimerSettings();
	}

	protected double getInitialWarningThresholdSeconds() {
		return -1.0D;
	}

	protected double getFinalWarningThresholdSeconds() {
		return -1.0D;
	}

	public abstract void syncWithFopTimer(FieldOfPlay fop);

	final protected long delta(long lastMillis) {
		if (lastMillis == 0) {
			return 0;
		} else {
			return System.currentTimeMillis() - lastMillis;
		}
	}

	/**
	 * Last applied timer-event sequence number for this element. Timer events
	 * travel on an
	 * asynchronous, multi-threaded bus, so their delivery order is not guaranteed.
	 * Each event
	 * carries a per-FOP monotonic sequence; we only apply events newer than the
	 * last one applied.
	 */
	private volatile long lastAppliedTimerSeq = 0L;

	/**
	 * Best-effort record of whether this element's timer was last told to run. Used
	 * only to make the
	 * {@code LiftingOrderUpdated} timer re-assertion idempotent (avoid restarting
	 * an already-running
	 * clock on every recompute). Correctness is enforced by the sequence gate, not
	 * by this flag.
	 */
	private volatile boolean elementRunning = false;

	protected boolean isElementRunning() {
		return this.elementRunning;
	}

	protected void reassertTimerState(boolean shouldRun, Integer milliseconds, boolean serverSound, long seq,
			Runnable onMismatch) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (shouldRun) {
				this.recentStopCommandMillis = 0L;
				if (this.elementRunning) {
					return;
				}
				if (isStaleTimerEvent(seq)) {
					return;
				}
				onMismatch.run();
				this.elementRunning = true;
				setServerSound(serverSound);
				setIndefinite(milliseconds == null);
				setMsRemaining(milliseconds);
				String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
				this.lastStartMillis = System.currentTimeMillis();
				start(milliseconds, isIndefinite(), isSilent(), parent);
				if (ui != null) {
					ui.push(); // should not be required...
				}
			} else {
				if (!this.elementRunning) {
					return;
				}
				if (isStaleTimerEvent(seq)) {
					return;
				}
				onMismatch.run();
				this.elementRunning = false;
				setMsRemaining(milliseconds);
				String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
				this.lastStopMillis = System.currentTimeMillis();
				this.recentStopCommandMillis = this.lastStopMillis;
				stop(getMsRemaining(), isIndefinite(), isSilent(), parent);
			}
		});
	}

	/**
	 * Drop timer events that arrive out of order. MUST be called on the UI thread
	 * (inside a
	 * {@code uiAccess} block) so that the compare-and-update is atomic for this
	 * element.
	 *
	 * @param seq the event sequence; 0 means "not sequence-checked" (break timer /
	 *            direct sync) and is always applied
	 * @return true if the event is stale and must be ignored
	 */
	private boolean isStaleTimerEvent(long seq) {
		if (seq == 0L) {
			return false;
		}
		if (seq <= this.lastAppliedTimerSeq) {
			return true;
		}
		this.lastAppliedTimerSeq = seq;
		return false;
	}

	protected final void doSetTimer(Integer milliseconds) {
		doSetTimer(milliseconds, 0L);
	}

	protected final void doSetTimer(Integer milliseconds, long seq) {
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("{} doSetTimer {} {}", this, milliseconds,
					LoggerUtils.stackTrace());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (isStaleTimerEvent(seq)) {
				return;
			}
			this.recentStopCommandMillis = System.currentTimeMillis();
			this.elementRunning = false;
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			initTime(milliseconds);
			stop(getMsRemaining(), isIndefinite(), isSilenced(), parent);
			initTime(milliseconds);
		});
	}

	protected void doStartTimer(Integer milliseconds, boolean serverSound) {
		doStartTimer(milliseconds, serverSound, 0L);
	}

	protected void doStartTimer(Integer milliseconds, boolean serverSound, long seq) {
		this.logger.debug("{} doStartTimer {}", this, milliseconds);
		setServerSound(serverSound);
		// String trace = LoggerUtils.stackTrace();
		// TEMPORARY (playwright) diagnostic: symmetric with doStopTimer so we can see
		// the seq the
		// start applied with and the resulting lastAppliedTimerSeq, to reason about
		// whether a later
		// StopTime is being stale-dropped. Remove once root cause confirmed.
		boolean timerDebug = isPlaywrightTimerDiagnosticEnabled();
		if (timerDebug) {
			this.logger./*playwright*/warn(
					"{}doStartTimer subscriber fired seq={} ms={} timer={} lastAppliedSeq={} elementRunning={} {}",
					FieldOfPlay.getLoggingName(this.fop), seq, milliseconds, describeTimerForDiagnostics(),
					this.lastAppliedTimerSeq, this.elementRunning, LoggerUtils.whereFrom());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (isStaleTimerEvent(seq)) {
				if (timerDebug) {
					this.logger./*playwright*/warn("{}doStartTimer STALE-DROPPED seq={} timer={} lastAppliedSeq={}",
							FieldOfPlay.getLoggingName(this.fop), seq, describeTimerForDiagnostics(),
							this.lastAppliedTimerSeq);
				}
				return;
			}
			this.recentStopCommandMillis = 0L;
			if (timerDebug) {
				this.logger./*playwright*/warn("{}doStartTimer applying start seq={} ms={} timer={}",
						FieldOfPlay.getLoggingName(this.fop), seq, milliseconds, describeTimerForDiagnostics());
			}
			this.elementRunning = true;
			setIndefinite(milliseconds == null);
			setMsRemaining(milliseconds);
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			this.lastStartMillis = System.currentTimeMillis();
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("server starting timer {}, {}, {}", parent, milliseconds, this.lastStartMillis);
			}
			start(milliseconds, isIndefinite(), isSilent(), parent);
			if (ui != null) {
				ui.push(); // should not be required...
			}
		});
	}

	protected void doStopTimer(Integer milliseconds) {
		doStopTimer(milliseconds, 0L);
	}

	protected void doStopTimer(Integer milliseconds, long seq) {
		long stopAcceptedMillis = System.currentTimeMillis();
		this.recentStopCommandMillis = stopAcceptedMillis;
		// TEMPORARY (playwright) diagnostic: proves slaveStopTimer reached doStopTimer
		// (subscriber
		// fired) and that the uiAccess command actually ran. Distinguishes a
		// bus-delivery gap from a
		// stale-gate drop for the missed-StopTime investigation. Remove once root cause
		// confirmed.
		boolean timerDebug = isPlaywrightTimerDiagnosticEnabled();
		if (timerDebug) {
			this.logger./*playwright*/warn(
					"{}doStopTimer subscriber fired seq={} ms={} timer={} lastAppliedSeq={} elementRunning={} {}",
					FieldOfPlay.getLoggingName(this.fop), seq, milliseconds, describeTimerForDiagnostics(),
					this.lastAppliedTimerSeq, this.elementRunning, LoggerUtils.whereFrom());
		}
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			if (isStaleTimerEvent(seq)) {
				if (this.recentStopCommandMillis == stopAcceptedMillis) {
					this.recentStopCommandMillis = 0L;
				}
				if (timerDebug) {
					this.logger./*playwright*/warn("{}doStopTimer STALE-DROPPED seq={} timer={} lastAppliedSeq={}",
							FieldOfPlay.getLoggingName(this.fop), seq, describeTimerForDiagnostics(),
							this.lastAppliedTimerSeq);
				}
				return;
			}
			if (timerDebug) {
				this.logger./*playwright*/warn("{}doStopTimer applying stop seq={} ms={} timer={}",
						FieldOfPlay.getLoggingName(this.fop), seq, milliseconds, describeTimerForDiagnostics());
			}
			this.elementRunning = false;
			setMsRemaining(milliseconds);
			String parent = DebugUtils.getOwlcmsParentName(this.getParent().get());
			this.lastStopMillis = System.currentTimeMillis();
			this.recentStopCommandMillis = this.lastStopMillis;
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
		if (UI.getCurrent() == null) {
			return;
		}
		this.vsession = VaadinSession.getCurrent();
		syncTimerSettings();
	}

	private void syncTimerSettings() {
		Element timerElement2 = getTimerElement();
		if (timerElement2 == null) {
			return;
		}
		JsonObject payload = Json.createObject();
		payload.put("sequence", Long.toString(++this.timerSettingsSequence));
		payload.put("silent", isSilent());
		payload.put("serverTickEnabled", isServerTickEnabled());
		payload.put("serverRunningCheckEnabled", isServerRunningCheckEnabled());
		payload.put("initialWarningThresholdSeconds", getInitialWarningThresholdSeconds());
		payload.put("finalWarningThresholdSeconds", getFinalWarningThresholdSeconds());
		timerElement2.setPropertyJson("timerSettingsPayload", payload);
	}

	private boolean isServerTickEnabled() {
		return Config.getCurrent().featureSwitch("playwright") && isDisplayAthleteTimer();
	}

	private boolean isServerRunningCheckEnabled() {
		return Config.getCurrent().featureSwitch("playwright")
				&& (isAthleteTimerOnControlPage() || isAttemptBoardAthleteTimer());
	}

	/**
	 * The Playwright-only server-authoritative running check is a diagnostic safety
	 * net for athlete
	 * timers that Playwright is verifying: operator control surfaces and attempt
	 * boards. Matching is
	 * done by simple names to avoid a components -> nui layering dependency.
	 */
	public boolean isAthleteTimerOnControlPageForDiagnostics() {
		return isAthleteTimerOnControlPage();
	}

	public String describeTimerForDiagnostics() {
		Object origin = getOrigin();
		String originName = origin == null ? "null" : origin.getClass().getSimpleName();
		String fopName = this.fop == null ? "null" : this.fop.getName();
		String busName = this.uiEventBus == null ? "null" : this.uiEventBus.identifier();
		String parentName = this.getParent().map(DebugUtils::getOwlcmsParentName).orElse("none");
		return String.format(
				"%s{role=%s,control=%s,origin=%s,fop=%s,bus=%s,parent=%s,attached=%s,running=%s,lastSeq=%s}",
				this.instanceId, timerDiagnosticRole(), isAthleteTimerOnControlPage(), originName, fopName, busName,
				parentName, this.getUI().isPresent(), this.elementRunning, this.lastAppliedTimerSeq);
	}

	private String timerDiagnosticRole() {
		Object origin = getOrigin();
		String originName = origin == null ? "" : origin.getClass().getSimpleName();
		String parentName = this.getParent().map(DebugUtils::getOwlcmsParentName).orElse("");
		if ("AnnouncerContent".equals(originName)) {
			return "announcer";
		}
		if (originName.contains("AttemptBoard") || parentName.contains("AttemptBoard")) {
			return "attempt";
		}
		return isAthleteTimerOnControlPage() ? "control" : "other";
	}

	private boolean isPlaywrightTimerDiagnosticEnabled() {
		return Config.getCurrent().featureSwitch("playwright")
				&& (isAthleteTimerOnControlPage() || isAttemptBoardAthleteTimer());
	}

	private boolean isAttemptBoardAthleteTimer() {
		if (!(this instanceof AthleteTimerElement)) {
			return false;
		}
		Object origin = getOrigin();
		String originName = origin == null ? "" : origin.getClass().getSimpleName();
		String parentName = this.getParent().map(DebugUtils::getOwlcmsParentName).orElse("");
		return originName.contains("AttemptBoard") || parentName.contains("AttemptBoard");
	}

	private boolean isAthleteTimerOnControlPage() {
		if (!(this instanceof AthleteTimerElement)) {
			return false;
		}
		Object origin = getOrigin();
		if (origin == null) {
			return false;
		}
		for (Class<?> c = origin.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
			if ("AthleteGridContent".equals(c.getSimpleName())) {
				return true;
			}
		}
		return false;
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
	 * Subclasses MUST call super.onAttach(attachEvent) first to ensure guard is
	 * checked.
	 * 
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// Guard against double-attach - subclasses must call this via super.onAttach()
		if (!this.attached.compareAndSet(false, true)) {
			this.logger.debug("TimerElement.onAttach called twice for instance={} parent={} - ignoring", instanceId,
					DebugUtils.getOwlcmsParentName(this.getParent().orElse(null)));
			return; // Silently ignore subsequent attach calls
		}
		this.ui = UI.getCurrent();
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		// // tell the javascript to stay quiet
		// setSilenced(true);
		// setTimerElement(null);
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
			setTimerCommand(timerElement2, "start", seconds, indefinite, silent, from);
		}
	}

	private void setTimerCommand(Element timerElement2, String command, double seconds, Boolean indefinite,
			Boolean silent,
			String from) {
		JsonObject payload = Json.createObject();
		long sequence = ++this.timerCommandSequence;
		payload.put("sequence", Long.toString(sequence));
		payload.put("command", command);
		payload.put("seconds", seconds);
		payload.put("indefinite", Boolean.TRUE.equals(indefinite));
		payload.put("silent", Boolean.TRUE.equals(silent));
		payload.put("serverTickEnabled", isServerTickEnabled());
		payload.put("serverRunningCheckEnabled", isServerRunningCheckEnabled());
		payload.put("serverMillis", Long.toString(System.currentTimeMillis()));
		payload.put("from", from != null ? from : "");
		payload.put("initialWarningThresholdSeconds", getInitialWarningThresholdSeconds());
		payload.put("finalWarningThresholdSeconds", getFinalWarningThresholdSeconds());
		timerElement2.setPropertyJson("timerCommandPayload", payload);
		traceDisplayTimerClientRender(command, seconds, sequence);
	}

	private void traceDisplayTimerClientRender(String command, double seconds, long sequence) {
		if (!Config.getCurrent().featureSwitch("playwright") || !isDisplayAthleteTimer() || getUI().isEmpty()) {
			return;
		}
		final String ctx = FieldOfPlay.getLoggingName(this.fop) + timerDiagnosticRole() + " timer ";
		final long stamp = System.currentTimeMillis();
		try {
			getElement().executeJs(TRACE_TIMER_CLIENT_RENDER_SCRIPT).then(String.class, rendered -> {
				this.logger./*playwright*/warn("{}{} client rendered +{}ms seq={} seconds={} {}", ctx, command,
						System.currentTimeMillis() - stamp, sequence, seconds, rendered != null ? rendered : "<null>");
			});
		} catch (RuntimeException e) {
			this.logger.debug("{}{} client render callback failed", ctx, command, e);
		}
	}

	@ClientCallable
	public void clientTimerStopped(String sequence, String display, Boolean running, Double currentTime) {
		long parsedSequence = parseTimerCommandSequence(sequence);
		this.lastClientStoppedSequence = parsedSequence;
		this.lastClientStoppedMillis = System.currentTimeMillis();
		if (Config.getCurrent().featureSwitch("playwright") && isDisplayAthleteTimer()) {
			this.logger./*playwright*/warn(
					"{}{} timer stop client acknowledged seq={} display='{}' running={} currentTime={}",
					FieldOfPlay.getLoggingName(this.fop), timerDiagnosticRole(), parsedSequence, display, running,
					currentTime);
		}
	}

	@ClientCallable
	public void clientTimerTick(String display, Double currentTime) {
		if (!isServerTickEnabled()) {
			return;
		}
		this.logger./*playwright*/warn("{}{} timer tick display='{}' currentTime={}",
				FieldOfPlay.getLoggingName(this.fop), timerDiagnosticRole(), display, currentTime);
		if (("0:56".equals(display) || "1:56".equals(display))
				&& playwrightTimerTripwireTriggered.compareAndSet(false, true)) {
			this.logger./*playwright*/warn(
					"{}TEMPORARY playwright timer tripwire display='{}' currentTime={} - stopping OWLCMS",
					FieldOfPlay.getLoggingName(this.fop), display, currentTime);
			new Thread(() -> {
				Main.prepareForExit();
				try {
					Thread.sleep(250L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				System.exit(0);
			}, "Playwright-Timer-Tripwire").start();
		}
	}

	@ClientCallable
	public void clientTimerRunningCheck(String display, Double currentTime) {
		if (!isServerRunningCheckEnabled()) {
			return;
		}
		reportStoppedServerTimerFromTick(display, currentTime);
	}

	/**
	 * Playwright observe-only check: when the client is about to render a lower
	 * second but the
	 * authoritative FOP timer is already stopped, record evidence. Do not send a
	 * corrective timer
	 * command from this path; Playwright diagnostics must fail rather than
	 * compensate.
	 */
	private void reportStoppedServerTimerFromTick(String display, Double currentTime) {
		if (!(this instanceof AthleteTimerElement) || this.fop == null || !this.elementRunning) {
			return;
		}
		IProxyTimer fopTimer = getFopTimer(this.fop);
		if (fopTimer == null || fopTimer.isRunning()) {
			return;
		}
		if (isStopCommandInGraceWindow()) {
			return;
		}
		Integer milliseconds = fopTimer.getTimeRemainingAtLastStop();
		String parent = DebugUtils.getOwlcmsParentName(this.getParent().orElse(null));
		if (isPlaywrightTimerDiagnosticEnabled()) {
			this.logger./*playwright*/warn(
					"{}timer tick mismatch timer={} display='{}' currentTime={} serverStoppedAt={}ms parent={} observeOnly=true",
					FieldOfPlay.getLoggingName(this.fop), describeTimerForDiagnostics(), display, currentTime,
					milliseconds, parent);
		}
	}

	private boolean isStopCommandInGraceWindow() {
		long stopMillis = this.recentStopCommandMillis;
		return stopMillis > 0L && System.currentTimeMillis() - stopMillis < SERVER_RUNNING_CHECK_STOP_GRACE_MILLIS;
	}

	private long parseTimerCommandSequence(String sequence) {
		if (sequence == null || sequence.isBlank()) {
			return 0L;
		}
		try {
			return Long.parseLong(sequence);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private boolean isAnnouncerAthleteTimer() {
		Object origin = getOrigin();
		return this instanceof AthleteTimerElement && origin != null
				&& "AnnouncerContent".equals(origin.getClass().getSimpleName());
	}

	private boolean isDisplayAthleteTimer() {
		return isAnnouncerAthleteTimer() || isAttemptBoardAthleteTimer();
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
			setTimerCommand(timerElement2, "display", seconds, indefinite, silent, null);
		}
	}

	private void stop(Integer milliseconds, Boolean indefinite, Boolean silent, String from) {
		Element timerElement2 = getTimerElement();
		if (timerElement2 != null && (indefinite || milliseconds != null)) {
			double seconds = (indefinite) ? 0.0D : milliseconds / 1000.0D;
			if (this instanceof BreakTimerElement) {
				this.logger.debug("stop {}s", seconds);
			}
			setTimerCommand(timerElement2, "pause", seconds, indefinite, silent, from);
		}
	}
}
