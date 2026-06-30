import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class TimerElement extends LitElement {
  static get is() {
    return "timer-element";
  }

  render() {
    return html`
      <audio preload="auto" id="finalWarning" src="../local/sounds/finalWarning.mp3"></audio>
      <audio preload="auto" id="initialWarning" src="../local/sounds/initialWarning.mp3"></audio>
      <audio preload="auto" id="timeOver" src="../local/sounds/timeOver.mp3"></audio>
      <div id="timer" .innerHTML="&nbsp;&nbsp;&nbsp;&nbsp;"></div>`;
  }

  connectedCallback() {
    super.connectedCallback();
    document.addEventListener('initSounds', this.initSounds);
  }

  disconnectedCallback() {
    document.removeEventListener('initSounds', this.initSounds);
    super.disconnectedCallback();
  }

  static get properties() {
    return {
      _formattedTime: {
        type: String,
      },
      initialWarningThresholdSeconds: {
        type: Number,
      },
      finalWarningThresholdSeconds: {
        type: Number,
      },
      timerStatePayload: {
        type: Object,
      },
      timerSettingsPayload: {
        type: Object,
      },
      timerCommandPayload: {
        type: Object,
      },
      serverTickEnabled: {
        type: Boolean,
      },
      serverRunningCheckEnabled: {
        type: Boolean,
      },
    };
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    if (changedProperties.has("timerStatePayload")) {
      this._applyTimerState();
    }
    if (changedProperties.has("timerSettingsPayload")) {
      this._applyTimerSettings();
    }
    if (changedProperties.has("timerCommandPayload")) {
      this._applyTimerCommand();
    }
  }

  _applyTimerState() {
    const payload = this.timerStatePayload;
    if (!payload) {
      return;
    }
    this._stopAnimation();
    this.startTime = Number.isFinite(payload.startTime) ? payload.startTime : 60;
    this.currentTime = Number.isFinite(payload.currentTime) ? payload.currentTime : 0;
    this.countUp = Boolean(payload.countUp);
    this.running = Boolean(payload.running);
    this.silent = Boolean(payload.silent);
    this.serverTickEnabled = Boolean(payload.serverTickEnabled);
    this.serverRunningCheckEnabled = Boolean(payload.serverRunningCheckEnabled);
    this.fopName = payload.fopName || "";
    this.initialWarningThresholdSeconds = Number.isFinite(payload.initialWarningThresholdSeconds)
      ? payload.initialWarningThresholdSeconds
      : -1;
    this.finalWarningThresholdSeconds = Number.isFinite(payload.finalWarningThresholdSeconds)
      ? payload.finalWarningThresholdSeconds
      : -1;
    this._elapsed = null;
    this.updateTime(this.currentTime);
    if (this.running) {
      this._scheduleTimerFrame();
    }
  }

  _applyTimerSettings() {
    const payload = this.timerSettingsPayload;
    if (!payload) {
      return;
    }
    this.silent = Boolean(payload.silent);
    this.serverTickEnabled = Boolean(payload.serverTickEnabled);
    this.serverRunningCheckEnabled = Boolean(payload.serverRunningCheckEnabled);
    this.initialWarningThresholdSeconds = Number.isFinite(payload.initialWarningThresholdSeconds)
      ? payload.initialWarningThresholdSeconds
      : -1;
    this.finalWarningThresholdSeconds = Number.isFinite(payload.finalWarningThresholdSeconds)
      ? payload.finalWarningThresholdSeconds
      : -1;
  }

  _applyTimerCommand() {
    const payload = this.timerCommandPayload;
    if (!payload || !payload.command) {
      return;
    }
    this.initialWarningThresholdSeconds = Number.isFinite(payload.initialWarningThresholdSeconds)
      ? payload.initialWarningThresholdSeconds
      : -1;
    this.finalWarningThresholdSeconds = Number.isFinite(payload.finalWarningThresholdSeconds)
      ? payload.finalWarningThresholdSeconds
      : -1;
    const seconds = Number.isFinite(payload.seconds) ? payload.seconds : 0;
    const indefinite = Boolean(payload.indefinite);
    const silent = Boolean(payload.silent);
    this.silent = silent;
    this.serverTickEnabled = Boolean(payload.serverTickEnabled);
    this.serverRunningCheckEnabled = Boolean(payload.serverRunningCheckEnabled);
    if (this._isNoopTimerCommand(payload.command, seconds, indefinite)) {
      if (payload.command === "pause") {
        this._notifyServerTimerStopped(payload.sequence);
      }
      return;
    }
    if (payload.command === "start") {
      this._start(seconds, indefinite, silent, payload.serverMillis, payload.from);
    } else if (payload.command === "pause") {
      this._pause(seconds, indefinite, silent, payload.serverMillis, payload.from, payload.sequence);
    } else if (payload.command === "display") {
      this._display(seconds, indefinite, silent);
    } else {
      console.warn("unknown timer command " + payload.command);
    }
  }

  _isNoopTimerCommand(command, seconds, indefinite) {
    if (command === "pause") {
      return !indefinite && !this.running && this._timeEquals(this.currentTime, seconds);
    }
    if (command === "display") {
      return !indefinite && !this.running && this._timeEquals(this.currentTime, seconds)
        && !this._initialWarningGiven && !this._finalWarningGiven && !this._timeOverWarningGiven;
    }
    return false;
  }

  _timeEquals(left, right) {
    return Number.isFinite(left) && Number.isFinite(right) && Math.abs(left - right) < 0.001;
  }

  _scheduleTimerFrame() {
    if (this._animationFrameId !== null) {
      window.cancelAnimationFrame(this._animationFrameId);
    }
    this._animationFrameId = window.requestAnimationFrame(this._decreaseTimer);
  }

  _stopAnimation() {
    if (this._animationFrameId !== null) {
      window.cancelAnimationFrame(this._animationFrameId);
      this._animationFrameId = null;
    }
  }

  getInitialWarningThresholdSeconds() {
    return Number.isFinite(this.initialWarningThresholdSeconds)
      ? this.initialWarningThresholdSeconds
      : -1;
  }

  getFinalWarningThresholdSeconds() {
    return Number.isFinite(this.finalWarningThresholdSeconds)
      ? this.finalWarningThresholdSeconds
      : -1;
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    this._init();
  }

  initSounds() {
    /*
      Sounds are played once in response to a user gesture.  The
      SoundEnabler component triggers a document "initSounds" event that other components
      listen to. When this routine is called, a user interaction event is below us on the stack.
      This is an enforced requirement on iOS. Note: The "volume" variable is 
      read-only on iOS, hence the use of "muted" instead.
      Once played once sounds can be played again without user interaction. 
    */
    console.warn("initSound timer");
    this.renderRoot.querySelector('#initialWarning').muted = true;
    this.renderRoot.querySelector('#initialWarning').play();
    this.renderRoot.querySelector('#finalWarning').muted = true;
    this.renderRoot.querySelector('#finalWarning').play();
    this.renderRoot.querySelector('#timeOver').muted = true;
    this.renderRoot.querySelector('#timeOver').play();
  }

  soundInitialWarning() {
    console.warn("initialWarning called");
    this.renderRoot.querySelector('#initialWarning').muted = false;
    this.renderRoot.querySelector('#initialWarning').play();
  }

  soundFinalWarning() {
    console.warn("finalWarning called");
    this.renderRoot.querySelector('#finalWarning').muted = false;
    this.renderRoot.querySelector('#finalWarning').play();
  }

  soundTimeOver() {
    console.warn("timeOver called");
    this.renderRoot.querySelector('#timeOver').muted = false;
    this.renderRoot.querySelector('#timeOver').play();
  }

  _start(seconds, indefinite, silent, serverMillis, from) {
    this._stopAnimation();
    if (indefinite) {
      this.running = false;
      console.warn("timer indefinite " + seconds);
      this._indefinite();
      return;
    }

    var lateMillis = 0;
    if (this.isIOS()) {
      // iPad devices can react several seconds late; catch up with time
      // this assumes that iPad is in sync with NTP time (it should be)
      var localMillis = Date.now();
      var serverMillisInt = parseInt(serverMillis, 10);
      lateMillis = Number.isFinite(serverMillisInt) ? localMillis - serverMillisInt : 0;
      if (lateMillis < 0) {
        lateMillis = 0;
      }
    }

    console.warn("timer start " + seconds + " late = " + lateMillis + "ms");

    this.currentTime = seconds - lateMillis / 1000;
    if (
      (this.currentTime <= 0 && !this.countUp) ||
      (this.currentTime >= this.startTime && this.countUp)
    ) {
      // timer is over
      this.currentTime = this.countUp ? this.startTime : 0;
    }

    this.silent = silent;
    const initialWarningThresholdSeconds = this.getInitialWarningThresholdSeconds();
    const finalWarningThresholdSeconds = this.getFinalWarningThresholdSeconds();
    this._initialWarningGiven = initialWarningThresholdSeconds < 0 || this.currentTime < initialWarningThresholdSeconds;
    this._finalWarningGiven = finalWarningThresholdSeconds < 0 || this.currentTime < finalWarningThresholdSeconds;
    this._timeOverWarningGiven = this.currentTime < 0;

    this._elapsed = null;  // Will be initialized on first _decreaseTimer call
    this.running = true;
    console.warn("timer running " + this.currentTime);
    this._scheduleTimerFrame();
  }

  _pause(seconds, indefinite, silent, serverMillis, from, sequence) {
    this._stopAnimation();
    this.silent = silent;
    if (indefinite) {
      this.running = false;
      this._indefinite();
      this._notifyServerTimerStopped(sequence);
      return;
    }

    this.running = false;
    console.warn("paused" + " running=false");

    console.warn("timer pause " + seconds);
    this.currentTime = seconds;

    // this._formattedTime = this._formatTime(this.currentTime);
    this.updateTime(this.currentTime)
    this._notifyServerTimerStopped(sequence);
  }

  _notifyServerTimerStopped(sequence) {
    if (!this.$server || typeof this.$server.clientTimerStopped !== "function") {
      return;
    }
    window.requestAnimationFrame(() => window.requestAnimationFrame(() => {
      const timer = this.shadowRoot && this.shadowRoot.querySelector('#timer');
      const display = timer && timer.innerText ? timer.innerText.trim().replace(/\s+/g, ' ') : '';
      this.$server.clientTimerStopped(String(sequence || ''), display, Boolean(this.running), Number(this.currentTime));
    }));
  }

  _display(seconds, indefinite, silent) {
    this._stopAnimation();
    this.silent = silent;
    this.running = false;
    console.warn("display " + indefinite + " " + seconds + " running=false");
    if (indefinite) {
      this.currentTime = seconds;
      this._indefinite();
    } else {
      this.currentTime = seconds;
      this.updateTime(this.currentTime)
    }
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
  }

  updateTime(time) {
    var newTime = this._formatTime(time);
    if (newTime == "NaN:NaN") return;
    if (newTime != this.lastTime) {
      var s = this.renderRoot.querySelector('#timer');
      if (s) {
        s.innerHTML = newTime;
        this.requestUpdate();
        console.warn("displayed " + newTime + " : " + (s ? s.innerHTML : "-"));
        this.lastTime = newTime;
        if (this.running && this.serverTickEnabled) {
          this._notifyServerTimerTick(newTime);
        }
      } else {
        console.warn("no root to update");
      }

    } else {
      // console.warn("same time "+newTime);
    }
  }

  _notifyServerTimerTick(display) {
    if (!this.$server || typeof this.$server.clientTimerTick !== "function") {
      return;
    }
    this.$server.clientTimerTick(String(display || ''), Number(this.currentTime));
  }

  _notifyServerRunningCheck(display) {
    if (!this.serverRunningCheckEnabled || !this.$server || typeof this.$server.clientTimerRunningCheck !== "function") {
      return;
    }
    this.$server.clientTimerRunningCheck(String(display || ''), Number(this.currentTime));
  }

  reset(element) {
  }

  isIOS() {
    return (
      [
        "iPad Simulator",
        "iPhone Simulator",
        "iPod Simulator",
        "iPad",
        "iPhone",
        // "iPod",
      ].includes(navigator.platform) ||
      // iPad on iOS 13 detection
      (navigator.userAgent.includes("Mac") && "ontouchend" in document)
    );
  }

  _indefinite() {
    this.updateTime(-1);
  }

  _init() {
    console.warn("init timer " + this.indefinite + " running=" + this.running + " start " + this.startTime + " " + this.currentTime);
    if (this.indefinite) {
      this.currentTime = this.startTime;
      this._indefinite();
    } else {
      this.updateTime(this.currentTime);
    }
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
  }

  typeOf(obj) {
    return {}.toString.call(obj).split(" ")[1].slice(0, -1).toLowerCase();
  }

  _decreaseTimer(timestamp) {
    this._animationFrameId = null;
    //console.warn(timestamp + " " + this.running);
    if (!this.running) {
      return;
    }

    var now = timestamp / 1000;
    // On first call, initialize _elapsed to current timestamp so progress is ~0
    if (this._elapsed === null) {
      this._elapsed = now;
    }
    // Compute the relative progress based on the time spent running
    var progress = now - this._elapsed;
    this.currentTime = this.countUp
      ? this.currentTime + progress
      : this.currentTime - progress;

    if (this.currentTime <= 0.05 && !this._timeOverWarningGiven) {
      console.warn("calling play " + this.currentTime);
      if (!this.silent) {
        console.warn("about to play time over " + window.timeOver);
        this.soundTimeOver();
      }

      this._timeOverWarningGiven = true;
    }
    const finalWarningThresholdSeconds = this.getFinalWarningThresholdSeconds();
    if (finalWarningThresholdSeconds >= 0 && this.currentTime <= finalWarningThresholdSeconds + 0.05 && !this._finalWarningGiven) {
      console.warn("final warning " + this.currentTime + " " + this.silent + " " + this.$server);
      if (!this.silent) {
        console.warn("about to play final warning " + window.finalWarning);
        this.soundFinalWarning();
      }
      this._finalWarningGiven = true;
    }
    const initialWarningThresholdSeconds = this.getInitialWarningThresholdSeconds();
    if (initialWarningThresholdSeconds >= 0 && this.currentTime <= initialWarningThresholdSeconds + 0.05 && !this._initialWarningGiven) {
      if (!this.silent) {
        this.soundInitialWarning();
      }
      this._initialWarningGiven = true;
    }

    const nextDisplay = this._formatTime(this.currentTime);
    if (nextDisplay !== this.lastTime) {
      // Playwright observe-only diagnostic: do NOT block the animation frame.
      // If the FOP timer is already stopped, the server records failure evidence.
      this._notifyServerRunningCheck(nextDisplay);
    }
    if (!this.running) {
      return;
    }

    //this._formattedTime = this._formatTime(this.currentTime);
    this.updateTime(this.currentTime)

    // console.warn(this._formattedTime);
    if ((this.currentTime < -0.1 && !this.countUp) || (this.currentTime >= this.startTime && this.countUp)) {
      console.warn("time over stop running " + this.$server + " running=false");

      this.running = false;
      this.formatted_time = this._formatTime(0);
      this.currentTime = this.countUp ? this.startTime : 0;
      return;
    }

    this._elapsed = now;
    this._scheduleTimerFrame();
  }

  _formatTime(ntime) {
    if (ntime <= 0) return "0:00";
    var ntime = Math.ceil(ntime);
    var hours = Math.trunc(ntime / 3600);
    var minutes = Math.trunc((ntime - hours * 3600) / 60);
    var seconds = ntime - (hours * 3600 + minutes * 60);
    return (
      (hours > 0 ? hours + ":" + (minutes < 10 ? "0" : "") : "") +
      (minutes + ":" + (seconds < 10 ? "0" + seconds : seconds))
    );
  }

  set startTime(newValue) {
    const oldValue = this.startTime;
    this._startTime = newValue;
  }

  get startTime() {
    return this._startTime;
  }

  constructor() {
    super();
    this._decreaseTimer = this._decreaseTimer.bind(this);
    this.startTime = 60;
    this.running = false;
    console.warn("constructor" + " running=false");
    this.countUp = false;
    this.silent = false;
    this.indefinite = false;
    this._elapsedTime = 0;
    this._formattedTime = "&nbsp;&nbsp;&nbsp;&nbsp;";
    this.initialWarningThresholdSeconds = -1;
    this.finalWarningThresholdSeconds = -1;
    this.timerStatePayload = null;
    this.timerSettingsPayload = null;
    this.timerCommandPayload = null;
    this.serverTickEnabled = false;
    this.serverRunningCheckEnabled = false;
    this._animationFrameId = null;
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
    this.initSounds = this.initSounds.bind(this);
  }
}

customElements.define(TimerElement.is, TimerElement);
