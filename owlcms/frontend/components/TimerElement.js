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
    };
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

  start(seconds, indefinite, silent, element, serverMillis, from) {
    if (indefinite) {
      console.warn("timer indefinite " + seconds);
      this._indefinite();
      return;
    }

    var lateMillis = 0;
    if (this.isIOS()) {
      // iPad devices can react several seconds late; catch up with time
      // this assumes that iPad is in sync with NTP time (it should be)
      var localMillis = Date.now();
      lateMillis = localMillis - parseInt(serverMillis, 10);
      if (lateMillis < 0) {
        lateMillis = 0;
      }
    }

    console.warn("timer start " + seconds + " late = " + lateMillis + "ms");
    this.$server.clientTimerStarting(
      this.fopName,
      seconds,
      lateMillis,
      (this.isIOS() ? "iPad" : "browser") + " " + from
    );

    this.currentTime = seconds - lateMillis / 1000;
    if (
      (this.currentTime <= 0 && !this.countUp) ||
      (this.currentTime >= this.startTime && this.countUp)
    ) {
      // timer is over
      this.currentTime = this.countUp ? this.startTime : 0;
    }

    this.silent = silent;
    this._initialWarningGiven = this.currentTime < 90;
    this._finalWarningGiven = this.currentTime < 30;
    this._timeOverWarningGiven = this.currentTime < 0;

    this._elapsed = performance.now() / 1000;
    this.running = true;
    console.warn("timer running " + this.currentTime);
    window.requestAnimationFrame(this._decreaseTimer);
  }

  pause(seconds, indefinite, silent, element, serverMillis, from) {
    if (indefinite) {
      this._indefinite();
      return;
    }

    this.running = false;
    console.warn("paused" + " running=false");
    // if (this.$server != null) {
    this.$server.clientTimerStopped(
      this.fopName,
      this.currentTime,
      (this.isIOS() ? "iPad" : "browser") + " " + from
    );


    console.warn("timer pause " + seconds);
    this.currentTime = seconds;

    // this._formattedTime = this._formatTime(this.currentTime);
    this.updateTime(this.currentTime)
  }

  display(seconds, indefinite, silent, element) {
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
      } else {
        console.warn("no root to update");
      }

    } else {
      // console.warn("same time "+newTime);
    }
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
    //console.warn(timestamp + " " + this.running);
    if (!this.running) {
      return;
    }

    var now = timestamp / 1000;
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

      // tell server to emit sound if server-side sounds
      console.warn("timeOver " + this.fopName + " " + this.$server);
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this._timeOverWarningGiven = true;
    }
    if (this.currentTime <= 30.05 && !this._finalWarningGiven) {
      console.warn("final warning " + this.currentTime + " " + this.silent + " " + this.$server);
      if (!this.silent) {
        console.warn("about to play final warning " + window.finalWarning);
        this.soundFinalWarning();
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientFinalWarning(this.fopName);
      this._finalWarningGiven = true;
    }
    if (this.currentTime <= 90.05 && !this._initialWarningGiven) {
      if (!this.silent) {
        this.soundInitialWarning();
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientInitialWarning(this.fopName);
      this._initialWarningGiven = true;
    }

    //this._formattedTime = this._formatTime(this.currentTime);
    this.updateTime(this.currentTime)

    // console.warn(this._formattedTime);
    this._elapsed = now;
    window.requestAnimationFrame(this._decreaseTimer);

    if ((this.currentTime < -0.1 && !this.countUp) || (this.currentTime >= this.startTime && this.countUp)) {
      console.warn("time over stop running " + this.$server + " running=false");

      // timer is over; tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this.running = false;
      this.formatted_time = this._formatTime(0);
      this.currentTime = this.countUp ? this.startTime : 0;
    }
  }

  _formatTime(ntime) {
    if (ntime <= 0) return "0:00";
    var ntime = Math.round(ntime);
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
    this._formattedTime = "0:00";
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
    this.initSounds = this.initSounds.bind(this);
  }
}

customElements.define(TimerElement.is, TimerElement);
