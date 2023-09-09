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
      <div id="timer" .innerHTML="${this._formattedTime}"></div>`;
  }

  doInitialWarning() {
    console.warn("initialWarning called");
    this.renderRoot?.querySelector('#initialWarning').play();
  }

  doFinalWarning() {
    console.warn("finalWarning called");
    this.renderRoot?.querySelector('#finalWarning').play();
  }

  doTimeOver() {
    console.warn("timeOver called");
    this.renderRoot?.querySelector('#timeOver').play();
  }

  static get properties() {
    return {
      /**
       * Start time for the timer in seconds
       *
       * @default 60
       */
      startTime: {
        type: Number,
        reflect: true,
      },
      /**
       * Current time of the timer, in seconds
       */
      currentTime: {
        type: Number,
        notify: true,
      },
      /**
       * True if the timer is currently running
       *
       * @default false
       */
      running: {
        type: Boolean,
        notify: true,
      },
      /**
       * Set to true to have timer count up
       *
       * @default false
       */
      countUp: {
        type: Boolean,
      },
      /**
       * Set to true to have timer not emit sounds
       *
       * @default false
       */
      silent: {
        type: Boolean,
      },
      /**
       * Set to true to state that timer is indefinite (--:--)
       *
       * @default false
       */
      indefinite: {
        type: Boolean,
      },
      /**
       * Time the timer has spent running since it was started
       */
      _elapsedTime: {
        type: Number,
      },
      _formattedTime: {
        type: String,
      },
      _initialWarningGiven: {
        type: Boolean,
      },
      _finalWarningGiven: {
        type: Boolean,
      },
      _timeOverWarningGiven: {
        type: Boolean,
      },
      fopName: {
        type: String,
        notify: true,
      },
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    console.warn("timer ready");
    this._init();
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
      // this assumes that iPad is in sync with NTP time (it should)
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

    // this._prepareAudio();

    this.currentTime = seconds - lateMillis / 1000;
    this.audioStartTime = window.audioCtx.currentTime;
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
    console.warn("paused"+" running=false");
    // if (this.$server != null) {
    this.$server.clientTimerStopped(
      this.fopName,
      this.currentTime,
      (this.isIOS() ? "iPad" : "browser") + " " + from
    );


    console.warn("timer pause " + seconds);
    this.currentTime = seconds;

    // this._formattedTime = this._formatTime(this.currentTime);
    var s; (s = this.renderRoot.querySelector('#timer')) && (s.innerHTML=this._formatTime(this.currentTime));
  }

  display(seconds, indefinite, silent, element) {
    this.running = false;
    console.warn("display " + indefinite + " " + seconds+" running=false");
    if (indefinite) {
      this.currentTime = seconds;
      this._indefinite();
    } else {
      this.currentTime = seconds;

      // this._formattedTime = this._formatTime(seconds);
      var s; (s = this.renderRoot.querySelector('#timer')) && (s.innerHTML=this._formatTime(this.currentTime));
    }
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
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
    // this._formattedTime = "&nbsp;";
    var s; (s = this.renderRoot.querySelector('#timer')) && (s.innerHTML="&nbsp;");
  }

  _init() {
    console.warn("init timer " + this.indefinite+" running="+this.running);
    if (this.indefinite) {
      this.currentTime = this.startTime;
      this._indefinite();
    }
    var s; (s = this.renderRoot.querySelector('#timer')) && (s.innerHTML=this._formatTime(this.currentTime));
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
        this.doTimeOver();
      }

      // tell server to emit sound if server-side sounds
      console.warn("timeOver "+this.fopName+" "+this.$server);
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this._timeOverWarningGiven = true;
    }
    if (this.currentTime <= 30.05 && !this._finalWarningGiven) {
      console.warn( "final warning " + this.currentTime + " " + this.silent + " " + this.$server );
      if (!this.silent) {
        console.warn("about to play final warning " + window.finalWarning);
        this.doFinalWarning();
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientFinalWarning(this.fopName);
      this._finalWarningGiven = true;
    }
    if (this.currentTime <= 90.05 && !this._initialWarningGiven) {
      if (!this.silent) {
        this.doInitialWarning();
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientInitialWarning(this.fopName);
      this._initialWarningGiven = true;
    }

    //this._formattedTime = this._formatTime(this.currentTime);
    var s; (s = this.renderRoot.querySelector('#timer')) && (s.innerHTML=this._formatTime(this.currentTime));

    // console.warn(this._formattedTime);
    this._elapsed = now;
    window.requestAnimationFrame(this._decreaseTimer);

    if ((this.currentTime < -0.1 && !this.countUp) || (this.currentTime >= this.startTime && this.countUp)) {
      console.warn("time over stop running " + this.$server+" running=false");

      // timer is over; tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this.running = false;
      this.formatted_time = this._formatTime(0);
      this.currentTime = this.countUp ? this.startTime : 0;
    }
  }

  _formatTime(ntime) {
    if (ntime < 0) return "0:00";
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
    console.warn("constructor"+" running=false");
    this.countUp = false;
    this.silent = false;
    this.indefinite = false;
    this._elapsedTime = 0;
    this._formattedTime = "0:00";
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
  }
}

customElements.define(TimerElement.is, TimerElement);
