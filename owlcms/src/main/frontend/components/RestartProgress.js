/*******************************************************************************
 * Copyright (c) 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { LitElement, css, html } from "lit";

class RestartProgress extends LitElement {
  static get is() {
    return "restart-progress";
  }

  static properties = {
    message: { type: String },
    expectedDurationMillis: { type: Number },
    startDelayMillis: { type: Number },
    phase: { type: String },
  };

  static styles = css`
    :host {
      display: block;
      box-sizing: border-box;
      min-width: min(24rem, 75vw);
      padding: 1em 2em;
    }

    .message {
      font-size: x-large;
      font-weight: bold;
      text-align: center;
    }

    .track {
      height: 0.55rem;
      margin-top: 0.85rem;
      overflow: hidden;
      border-radius: 999px;
      background: rgb(255 255 255 / 30%);
    }

    .bar {
      width: 100%;
      height: 100%;
      border-radius: inherit;
      background: currentColor;
    }

    .track.importing .bar {
      width: 40%;
      animation: importing 1.2s ease-in-out infinite;
    }

    .track.restarting .bar {
      transform: scaleX(0);
      transform-origin: left;
      will-change: transform;
    }

    @keyframes importing {
      from {
        transform: translateX(-100%);
      }
      to {
        transform: translateX(350%);
      }
    }
  `;

  constructor() {
    super();
    this.message = "";
    this.expectedDurationMillis = 30000;
    this.startDelayMillis = 0;
    this.phase = "importing";
    this._animationFrame = 0;
    this._startTimeout = 0;
    this._startedAt = 0;
  }

  connectedCallback() {
    super.connectedCallback();
    if (this.phase === "restarting") {
      this._restartAnimation();
    }
  }

  disconnectedCallback() {
    cancelAnimationFrame(this._animationFrame);
    clearTimeout(this._startTimeout);
    super.disconnectedCallback();
  }

  updated(changedProperties) {
    if (changedProperties.has("phase") && this.phase === "importing") {
      cancelAnimationFrame(this._animationFrame);
      clearTimeout(this._startTimeout);
      return;
    }
    if (this.phase === "restarting" &&
        (changedProperties.has("phase") ||
         changedProperties.has("expectedDurationMillis") ||
         changedProperties.has("startDelayMillis"))) {
      this._restartAnimation();
    }
  }

  render() {
    return html`
      <div class="message">${this.message}</div>
      <div
        class="track ${this.phase}"
        role="progressbar"
        aria-label=${this.message}
        aria-valuemin="0"
        aria-valuemax="100"
        aria-valuenow="0"
      >
        <div class="bar"></div>
      </div>
    `;
  }

  _animate() {
    const duration = Number.isFinite(this.expectedDurationMillis) && this.expectedDurationMillis > 0
      ? this.expectedDurationMillis
      : 30000;
    const elapsed = Math.max(0, performance.now() - this._startedAt);
    const progress = Math.min(0.95, 0.95 * elapsed / duration);
    const track = this.renderRoot?.querySelector(".track");
    const bar = this.renderRoot?.querySelector(".bar");

    if (track && bar) {
      bar.style.transform = `scaleX(${progress})`;
      track.setAttribute("aria-valuenow", Math.round(progress * 100));
    }

    if (progress < 0.95) {
      this._animationFrame = requestAnimationFrame(() => this._animate());
    } else {
      this._animationFrame = 0;
    }
  }

  _restartAnimation() {
    cancelAnimationFrame(this._animationFrame);
    clearTimeout(this._startTimeout);
    const track = this.renderRoot?.querySelector(".track");
    const bar = this.renderRoot?.querySelector(".bar");
    if (bar) {
      bar.style.transform = "scaleX(0)";
    }
    if (track) {
      track.setAttribute("aria-valuenow", "0");
    }
    const delay = Number.isFinite(this.startDelayMillis) && this.startDelayMillis > 0
      ? this.startDelayMillis
      : 0;
    this._startTimeout = window.setTimeout(() => {
      this._startedAt = performance.now();
      this._animate();
    }, delay);
  }
}

customElements.define(RestartProgress.is, RestartProgress);
