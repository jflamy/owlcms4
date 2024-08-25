import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class TimerElementPR extends LitElement {
  static get is() {
    return "timer-element-pr";
  }

  render() {
    return html`<div .innerHTML="${this._formattedTime}"></div>`;
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
        state: true,
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
    console.debug("timer ready");
    this._init();
  }

  start(seconds, indefinite, silent, element, serverMillis, from) {
    if (indefinite) {
      console.debug("timer indefinite " + seconds);
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

    this._prepareAudio();

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
    console.debug("timer running " + this.currentTime);
    window.requestAnimationFrame(this._decreaseTimer);
  }

  pause(seconds, indefinite, silent, element, serverMillis, from) {
    if (indefinite) {
      this._indefinite();
      return;
    }

    // var localMillis = Date.now();
    // var lateMillis = (localMillis - parseInt(serverMillis,10));
    // if (lateMillis < 0) {
    // 	lateMillis = 0;
    // }
    this.running = false;
    console.debug("paused"+" running=false");
    // if (this.$server != null) {
    this.$server.clientTimerStopped(
      this.fopName,
      this.currentTime,
      (this.isIOS() ? "iPad" : "browser") + " " + from
    );
    // } else {
    // 	console.debug("no server$");
    // }

    console.debug("timer pause " + seconds);

    this.currentTime = seconds;
    this._formattedTime = this._formatTime(this.currentTime);
  }

  display(seconds, indefinite, silent, element) {
    this.running = false;
    console.warn("display " + indefinite + " " + seconds+" running=false");
    if (indefinite) {
      this.currentTime = seconds;
      this._indefinite();
    } else {
      this.currentTime = seconds;
      this._formattedTime = this._formatTime(seconds);
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
    this._formattedTime = "&nbsp;";
  }

  _init() {
    console.debug("init timer " + this.indefinite+" running="+this.running);
    if (this.indefinite) {
      this.currentTime = this.startTime;
      this._indefinite();
    }

    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
  }

  async _prepareAudio() {
    console.debug("window.isIOS=", window.isIOS);
    if (window.isIOS) {
      // prefetched buffers are not available later for some unexplained reason.
      // so we don't attempt fetching.
      return;
    }
    if (!window.finalWarning /* && ! this.loadingFinalWarning */) {
      this.loadingFinalWarning = true;
      const finalWarning = await this._playTrack(
        "../local/sounds/finalWarning.mp3",
        null,
        false,
        0
      );
      window.finalWarning = finalWarning;
      console.debug("loaded finalWarning = " + window.finalWarning);
    } else {
      console.debug("skipping load");
      console.debug("existing finalWarning = " + window.finalWarning);
    }

    if (!window.initialWarning /* && ! this.loadingInitialWarning */) {
      this.loadingInitialWarning = true;
      const initialWarning = await this._playTrack(
        "../local/sounds/initialWarning.mp3",
        null,
        false,
        0
      );
      window.initialWarning = initialWarning;
      console.debug("loaded initialWarning = " + window.initialWarning);
    } else {
      console.debug("skipping load");
      console.debug("existing initialWarning = " + window.initialWarning);
    }

    if (!window.timeOver /* && ! this.loadingTimeOver */) {
      this.loadingTimeOver = true;
      const timeOver = await this._playTrack(
        "../local/sounds/timeOver.mp3",
        null,
        false,
        0
      );
      window.timeOver = timeOver;
      console.debug("loaded timeOver = " + window.timeOver);
    } else {
      console.debug("skipping load");
      console.debug("existing timeOver duration= " + window.timeOver);
    }
  }

  typeOf(obj) {
    return {}.toString.call(obj).split(" ")[1].slice(0, -1).toLowerCase();
  }

  async _playTrack(filepath, previousBuffer, play, when) {
    if (previousBuffer) {
      if (play) {
        // play previously fetched buffer
        await this._playAudioBuffer(previousBuffer, when);
      }
      return previousBuffer;
    } else {
      // Safari somehow manages to lose the AudioBuffer.
      // Massive workaround.
      const response = await fetch(filepath);
      const arrayBuffer = await response.arrayBuffer();
      const newBuffer = await window.audioCtx.decodeAudioData(
        arrayBuffer,
        async function (audioBuffer) {
          if (play) {
            // duplicated code from _playAudioBuffer
            // can't figure out how to invoke it with JavaScript "this" semantics.
            if (window.audioCtx) {
              const trackSource = await window.audioCtx.createBufferSource();
              if (trackSource) {
                trackSource.buffer = audioBuffer;
                trackSource.connect(window.audioCtx.destination);
                if (when <= 0) {
                  trackSource.start();
                } else {
                  trackSource.start(when, 0);
                }
              }
            }
          }
        },
        (e) => {
          console.error("could not decode sound");
        }
      );
      return newBuffer;
    }
  }

  async _playAudioBuffer(audioBuffer, when) {
    const trackSource = await window.audioCtx.createBufferSource();
    trackSource.buffer = audioBuffer;
    trackSource.connect(audioCtx.destination);
    if (when <= 0) {
      trackSource.start();
    } else {
      trackSource.start(when, 0);
    }

    return trackSource;
  }

  _decreaseTimer(timestamp) {
    //console.debug(timestamp + " " + this.running);
    if (!this.running) {
      return;
    }

    var now = timestamp / 1000;
    // Compute the relative progress based on the time spent running
    var progress = now - this._elapsed;
    this.currentTime = this.countUp
      ? this.currentTime + progress
      : this.currentTime - progress;

    // we anticipate to use the more precise audio context timer
    if (this.currentTime <= 0.05 && !this._timeOverWarningGiven) {
      console.debug("calling play " + this.currentTime);
      if (!this.silent) {
        console.debug("about to play time over " + window.timeOver);
        this._playTrack(
          "../sounds/timeOver.mp3",
          window.timeOver,
          true,
          this.currentTime
        );
      }
      // tell server to emit sound if server-side sounds
      console.warn("timeOver "+this.fopName+" "+this.$server);
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this._timeOverWarningGiven = true;
    }
    if (this.currentTime <= 30.05 && !this._finalWarningGiven) {
      console.debug(
        "final warning " +
        this.currentTime +
        " " +
        this.silent +
        " " +
        this.$server
      );
      if (!this.silent) {
        //this.renderRoot.querySelector("#finalWarning").play();
        console.debug("about to play final warning " + window.finalWarning);
        this._playTrack(
          "../sounds/finalWarning.mp3",
          window.finalWarning,
          true,
          this.currentTime - 30
        );
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientFinalWarning(this.fopName);
      this._finalWarningGiven = true;
    }
    if (this.currentTime <= 90.05 && !this._initialWarningGiven) {
      if (!this.silent) {
        //this.renderRoot.querySelector("#initialWarning").play();
        console.debug("about to play initial warning " + window.initialWarning);
        this._playTrack(
          "../sounds/initialWarning.mp3",
          window.initialWarning,
          true,
          this.currentTime - 90
        );
      }
      // tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientInitialWarning(this.fopName);
      this._initialWarningGiven = true;
    }

    this._formattedTime = this._formatTime(this.currentTime);
    console.debug(this._formattedTime);
    this._elapsed = now;
    window.requestAnimationFrame(this._decreaseTimer);

    if (
      (this.currentTime < -0.1 && !this.countUp) ||
      (this.currentTime >= this.startTime && this.countUp)
    ) {
      console.debug("time over stop running " + this.$server+" running=false");
      // timer is over; tell server to emit sound if server-side sounds
      if (this.$server != null) this.$server.clientTimeOver(this.fopName);
      this.running = false;
      this.formatted_time = this._formatTime(0);
      // this.dispatchEvent(new CustomEvent('timer-element-end', {bubbles:
      // true, composed: true}))
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
    console.debug("constructor"+" running=false");
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

customElements.define(TimerElementPR.is, TimerElementPR);
