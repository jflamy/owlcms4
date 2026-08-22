import { html, LitElement } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";

class PassiveDecisionElement extends LitElement {
  static get is() {
    return "passive-decision-element";
  }

  static get properties() {
    return {
      decisionPayload: { type: Object },
      publicFacing: { type: Boolean },
      size: { type: String, reflect: true },
      ref1: { type: Boolean, state: true },
      ref2: { type: Boolean, state: true },
      ref3: { type: Boolean, state: true },
      singleRef: { type: Boolean, state: true },
      showDecision: { type: Boolean, state: true },
    };
  }

  constructor() {
    super();
    this.decisionPayload = null;
    this.publicFacing = true;
    this.size = "small";
    this.ref1 = null;
    this.ref2 = null;
    this.ref3 = null;
    this.singleRef = false;
    this.showDecision = false;
    this._lastDecisionSequence = 0;
    this.stylesDir = "css";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "decision-lights")}" />
      <div class="decisionWrapper">
        <div class="decisions">
          <span class="${this.decisionClasses(1)}">&nbsp;</span>
          <span class="${this.decisionClasses(2)}">${this.singleRef && this.ref2 === true
            ? "✓"
            : this.singleRef && this.ref2 === false
              ? "✕"
              : ""}</span>
          <span class="${this.decisionClasses(3)}">&nbsp;</span>
        </div>
      </div>
    `;
  }

  updated(changedProperties) {
    if (changedProperties.has("decisionPayload")) {
      this._applyDecisionPayload();
    }
  }

  _applyDecisionPayload() {
    const payload = this.decisionPayload;
    if (!payload) {
      return;
    }
    const sequence = Number(payload.sequence);
    if (Number.isFinite(sequence) && sequence <= this._lastDecisionSequence) {
      return;
    }
    if (Number.isFinite(sequence)) {
      this._lastDecisionSequence = sequence;
    }
    this.singleRef = Boolean(payload.singleRef);
    this.showDecision = payload.mode !== "reset";
    this.ref1 = this._coerceRef(payload.ref1);
    this.ref2 = this._coerceRef(payload.ref2);
    this.ref3 = this._coerceRef(payload.ref3);
  }

  _coerceRef(value) {
    return value === true ? true : value === false ? false : null;
  }

  decisionClasses(position) {
    if (this.singleRef) {
      if (position !== 2) {
        return "invisible";
      }
      return `soloDecision ${this.showDecision ? this._decisionColor(this.ref2) : "none"}`;
    }

    const displayPosition = this.publicFacing ? position : 4 - position;
    return `decision ${this._decisionColor(this[`ref${displayPosition}`])}`;
  }

  _decisionColor(value) {
    if (!this.showDecision || value === null) {
      return "none";
    }
    return value ? "white" : "red";
  }
}

customElements.define(PassiveDecisionElement.is, PassiveDecisionElement);