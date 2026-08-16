import { html, LitElement } from "lit";

/*******************************************************************************
 * Copyright (c) 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class PassiveTimerElement extends LitElement {
  static get is() {
    return "passive-timer-element";
  }

  static get properties() {
    return {
      formattedTime: { type: String },
      timerCommandPayload: { type: Object },
      timerSettingsPayload: { type: Object },
      initialWarningSoundUrl: { type: String },
      finalWarningSoundUrl: { type: String },
      timeOverSoundUrl: { type: String },
    };
  }

  constructor() {
    super();
    this.formattedTime = "\u00a0\u00a0\u00a0\u00a0";
    this.currentTime = 0;
    this.running = false;
    this.silent = true;
    this.initialWarningThresholdSeconds = -1;
    this.finalWarningThresholdSeconds = -1;
    this.initialWarningSoundUrl = "../local/sounds/initialWarning.mp3";
    this.finalWarningSoundUrl = "../local/sounds/finalWarning.mp3";
    this.timeOverSoundUrl = "../local/sounds/timeOver.mp3";
    this._animationFrameId = null;
    this._elapsed = null;
    this._lastCommandSequence = 0;
    this._initialWarningGiven = false;
    this._finalWarningGiven = false;
    this._timeOverWarningGiven = false;
    this._decreaseTimer = this._decreaseTimer.bind(this);
    this.initSounds = this.initSounds.bind(this);
  }

  connectedCallback() {
    super.connectedCallback();
    document.addEventListener("initSounds", this.initSounds);
  }

  disconnectedCallback() {
    document.removeEventListener("initSounds", this.initSounds);
    this._stopAnimation();
    super.disconnectedCallback();
  }

  render() {
    return html`
      <audio preload="auto" id="finalWarning" src="${this.finalWarningSoundUrl}"></audio>
      <audio preload="auto" id="initialWarning" src="${this.initialWarningSoundUrl}"></audio>
      <audio preload="auto" id="timeOver" src="${this.timeOverSoundUrl}"></audio>
      <div id="timer">${this.formattedTime}</div>
    `;
  }

  updated(changedProperties) {
    if (changedProperties.has("timerSettingsPayload")) {
      this._applySettings(this.timerSettingsPayload);
    }
    if (changedProperties.has("timerCommandPayload")) {
      this._applyCommand(this.timerCommandPayload);
    }
  }

  initSounds() {
    for (const id of ["initialWarning", "finalWarning", "timeOver"]) {
      const audio = this.renderRoot.querySelector(`#${id}`);
      if (audio) {
        audio.muted = true;
        const playResult = audio.play();
        if (playResult) {
          playResult.catch(() => {});
        }
      }
    }
  }

  _applyCommand(payload) {
    if (!payload || !payload.command) {
      return;
    }
    const sequence = Number.parseInt(payload.sequence, 10);
    if (Number.isFinite(sequence) && sequence <= this._lastCommandSequence) {
      return;
    }
    if (Number.isFinite(sequence)) {
      this._lastCommandSequence = sequence;
    }
    this._applySettings(payload);
    const seconds = Number.isFinite(payload.seconds) ? payload.seconds : 0;
    const indefinite = Boolean(payload.indefinite);
    if (payload.command === "start") {
      this._start(seconds, indefinite, payload.issuedAtMillis);
    } else if (payload.command === "pause") {
      this._pause(seconds, indefinite);
    } else if (payload.command === "display") {
      this._display(seconds, indefinite);
    }
  }

  _applySettings(payload) {
    if (!payload) {
      return;
    }
    this.silent = Boolean(payload.silent);
    this.initialWarningThresholdSeconds = Number.isFinite(payload.initialWarningThresholdSeconds)
      ? payload.initialWarningThresholdSeconds
      : -1;
    this.finalWarningThresholdSeconds = Number.isFinite(payload.finalWarningThresholdSeconds)
      ? payload.finalWarningThresholdSeconds
      : -1;
    this.initialWarningSoundUrl = payload.initialWarningSoundUrl || this.initialWarningSoundUrl;
    this.finalWarningSoundUrl = payload.finalWarningSoundUrl || this.finalWarningSoundUrl;
    this.timeOverSoundUrl = payload.timeOverSoundUrl || this.timeOverSoundUrl;
  }

  _start(seconds, indefinite, issuedAtMillis) {
    this._stopAnimation();
    if (indefinite) {
      this.running = false;
      this.currentTime = 0;
      this.formattedTime = "0:00";
      return;
    }
    this.currentTime = Math.max(0, seconds - this._clientDelaySeconds(issuedAtMillis));
    this.running = this.currentTime > 0;
    this._elapsed = null;
    this._resetWarnings();
    this.formattedTime = this._formatTime(this.currentTime);
    if (this.running) {
      this._scheduleTimerFrame();
    }
  }

  _pause(seconds, indefinite) {
    this._stopAnimation();
    this.running = false;
    this.currentTime = indefinite ? 0 : seconds;
    this.formattedTime = indefinite ? "0:00" : this._formatTime(seconds);
  }

  _display(seconds, indefinite) {
    this._pause(seconds, indefinite);
    this._resetWarnings();
  }

  _clientDelaySeconds(issuedAtMillis) {
    if (!this._isIOS()) {
      return 0;
    }
    const issuedAt = Number.parseInt(issuedAtMillis, 10);
    return Number.isFinite(issuedAt) ? Math.max(0, Date.now() - issuedAt) / 1000 : 0;
  }

  _scheduleTimerFrame() {
    this._animationFrameId = window.requestAnimationFrame(this._decreaseTimer);
  }

  _stopAnimation() {
    if (this._animationFrameId !== null) {
      window.cancelAnimationFrame(this._animationFrameId);
      this._animationFrameId = null;
    }
  }

  _decreaseTimer(timestamp) {
    this._animationFrameId = null;
    if (!this.running) {
      return;
    }
    const now = timestamp / 1000;
    if (this._elapsed === null) {
      this._elapsed = now;
    }
    this.currentTime -= now - this._elapsed;
    this._soundWarnings();
    this.formattedTime = this._formatTime(this.currentTime);
    if (this.currentTime < -0.1) {
      this.running = false;
      this.currentTime = 0;
      this.formattedTime = "0:00";
      return;
    }
    this._elapsed = now;
    this._scheduleTimerFrame();
  }

  _soundWarnings() {
    if (this.currentTime <= 0.05 && !this._timeOverWarningGiven) {
      this._playSound("timeOver");
      this._timeOverWarningGiven = true;
    }
    if (this.finalWarningThresholdSeconds >= 0
        && this.currentTime <= this.finalWarningThresholdSeconds + 0.05
        && !this._finalWarningGiven) {
      this._playSound("finalWarning");
      this._finalWarningGiven = true;
    }
    if (this.initialWarningThresholdSeconds >= 0
        && this.currentTime <= this.initialWarningThresholdSeconds + 0.05
        && !this._initialWarningGiven) {
      this._playSound("initialWarning");
      this._initialWarningGiven = true;
    }
  }

  _playSound(id) {
    if (this.silent) {
      return;
    }
    const audio = this.renderRoot.querySelector(`#${id}`);
    if (audio) {
      audio.muted = false;
      const playResult = audio.play();
      if (playResult) {
        playResult.catch(() => {});
      }
    }
  }

  _resetWarnings() {
    this._initialWarningGiven = this.initialWarningThresholdSeconds < 0
      || this.currentTime < this.initialWarningThresholdSeconds;
    this._finalWarningGiven = this.finalWarningThresholdSeconds < 0
      || this.currentTime < this.finalWarningThresholdSeconds;
    this._timeOverWarningGiven = this.currentTime < 0;
  }

  _formatTime(time) {
    if (time <= 0) {
      return "0:00";
    }
    const roundedTime = Math.ceil(time);
    const hours = Math.trunc(roundedTime / 3600);
    const minutes = Math.trunc((roundedTime - hours * 3600) / 60);
    const seconds = roundedTime - (hours * 3600 + minutes * 60);
    return `${hours > 0 ? `${hours}:${minutes < 10 ? "0" : ""}` : ""}${minutes}:${seconds < 10 ? "0" : ""}${seconds}`;
  }

  _isIOS() {
    return ["iPad Simulator", "iPhone Simulator", "iPod Simulator", "iPad", "iPhone"]
      .includes(navigator.platform)
      || (navigator.userAgent.includes("Mac") && "ontouchend" in document);
  }
}

customElements.define(PassiveTimerElement.is, PassiveTimerElement);