import { html, LitElement } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";

/*********************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class DecisionElement extends LitElement {
  static get is() {
    return "decision-element";
  }

  render() {
    return html` 
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "decision-lights")}" />
      <audio preload="auto" id="down" src="../local/sounds/down.mp3"></audio>
      <div class="decisionWrapper ${this.showJuryRow() ? 'withJury' : ''} ${this.showJuryRow() && this._panelRefStates ? 'panelVote' : ''}" style="${this.decisionWrapperStyle()}" >
        <div class="down" style="font-weight: 900; ${this.downStyles()}"><vaadin-icon icon="vaadin:arrow-circle-down"></vaadin-icon></div>
        <div class="decisions" style="${this.decisionsStyles()}">
          <span class="${this.decisionClasses(1)}">&nbsp;</span>
          <span class="${this.decisionClasses(2)}" style="${((this.singleRef && this.ref2 !== null) ? "border: 2px solid var(--lumo-contrast); font-weight: bold" : "")}">${((this.singleRef && this.ref2 === true) ? "✓" : (this.singleRef && this.ref2 === false) ? "✕" : "")}</span>
          <span class="${this.decisionClasses(3)}">&nbsp;</span>
        </div>
        ${this.showJuryRow() ? html`
          <div class="juryDecisions">
            ${this.juryDecisions.map((decision, index) => html`
              <span class="juryLightSlot juror-${index + 1}">
                <vaadin-icon class="juryLight ${decision}"
                  icon="${this.juryIcon(decision)}"></vaadin-icon>
              </span>
            `)}
          </div>` : ''}
      </div>`;
  }

  static get properties() {
    return {
      ref1: {
        type: Boolean,
      },
      ref2: {
        type: Boolean,
      },
      ref3: {
        type: Boolean,
      },
      decision: {
        type: Boolean,
      },
      publicFacing: {
        type: Boolean,
      },
      jury: {
        type: Boolean,
        state: true,
      },
      singleRef: {
        type: Boolean,
        reflect: true,
      },
      silent: {
        type: Boolean,
      },
      _downShown: {
        type: Boolean,
        state: true,
      },
      _showDecision: {
        type: Boolean,
        state: true,
      },
      size: {
        type: String,
        reflect: true
      },
      decisionPayload: {
        type: Object,
      },
      showJuryDecisions: { type: Boolean },
      juryDecisions: { type: Array },
      _panelRefStates: { type: Array, state: true },
    };
  }

  constructor() {
    super();
    this.ref1 = null;
    this.ref2 = null;
    this.ref3 = null;
    this.publicFacing = true;
    this.jury = false;
    this.singleRef = false;
    this.silent = false;
    this._downShown = false;
    this._showDecision = false;
    this.size = "small";
    this.stylesDir = "css";
    this.decisionPayload = null;
    this.showJuryDecisions = false;
    this.juryDecisions = [];
    this._panelRefStates = null;
    // sequence of the last decisionPayload applied; drops stale/out-of-order payloads.
    this._lastDecisionSequence = 0;
    this.initSounds = this.initSounds.bind(this)
  }

  connectedCallback() {
    super.connectedCallback();
    // The server (DecisionElement.java) is the single source of truth for the
    // decision state. Never clear ref/decision properties here: on re-attach the
    // server re-syncs them, and self-clearing would race that sync and blank the boxes.
    document.addEventListener('initSounds', this.initSounds);
  }

  disconnectedCallback() {
    document.removeEventListener('initSounds', this.initSounds);
    super.disconnectedCallback();
  }

  initSounds() {
    var r = this.renderRoot;
    if (r == undefined) {
      console.warn("initSound down NOT READY");
      r = this;
    } else {
      console.warn("initSound down");
      r.querySelector('#down').muted = true;
      r.querySelector('#down').play();
    }
  }

  doDown() {
    console.warn("down called");
    this.renderRoot.querySelector('#down').muted = false;
    this.renderRoot?.querySelector('#down').play();
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    if (changedProperties.has("decisionPayload")) {
      this._applyDecisionPayload();
    }
  }

  /* The server batches every display transition (down, decision, reset) into a
     single ordered decisionPayload. Applying it atomically here guarantees there
     is never an intermediate render between the down signal and the decision
     boxes. A monotonic sequence drops stale/out-of-order payloads (e.g. the
     ignored initial decision arriving after the real one). */
  _applyDecisionPayload() {
    const payload = this.decisionPayload;
    if (!payload) {
      return;
    }
    const seq = Number(payload.sequence);
    if (Number.isFinite(seq)) {
      if (seq <= this._lastDecisionSequence) {
        return;
      }
      this._lastDecisionSequence = seq;
    }
    if (payload.singleRef !== undefined && payload.singleRef !== null) {
      this.singleRef = Boolean(payload.singleRef);
    }
    if (payload.mode !== "panel") {
      this._panelRefStates = null;
    }
    switch (payload.mode) {
      case "panel":
        // seven-person panel review vote: 3 referee boxes and 4 jury circles, revealed together
        this._panelRefStates = payload.refStates || ["empty", "empty", "empty"];
        this.juryDecisions = payload.juryStates || [];
        this._downShown = false;
        this._showDecision = true;
        break;
      case "down":
        this._downShown = true;
        this._showDecision = false;
        break;
      case "decision":
        this.ref1 = this._coerceRef(payload.ref1);
        this.ref2 = this._coerceRef(payload.ref2);
        this.ref3 = this._coerceRef(payload.ref3);
        this._downShown = false;
        this._showDecision = true;
        break;
      case "reset":
        this.ref1 = null;
        this.ref2 = null;
        this.ref3 = null;
        this._downShown = false;
        this._showDecision = Boolean(payload.showDecision);
        break;
      default:
        break;
    }
    this._traceDecisionPayloadApplied(payload);
  }

  _coerceRef(value) {
    return value === true ? true : value === false ? false : null;
  }

  _traceDecisionPayloadApplied(payload) {
    if (!this.$server?.decisionPayloadApplied) {
      return;
    }
    this.$server.decisionPayloadApplied(
      String(payload.sequence ?? ""),
      String(payload.mode ?? ""),
      payload.singleRef === true,
      payload.announcerForced === true,
      this._coerceRef(payload.ref1),
      this._coerceRef(payload.ref2),
      this._coerceRef(payload.ref3)
    );
  }

  decisionClasses(position) {
    var mainClass = "decision ";

    if (this._panelRefStates) {
      // athlete facing boards show the referees right to left
      const index = this.publicFacing ? position - 1 : 3 - position;
      const state = this._panelRefStates[index];
      return mainClass + (state === "white" || state === "red" || state === "voted" ? state : "none");
    }

    var single = this.singleRef;
    if (single) {
      mainClass = "soloDecision "
      if (position == 1 || position == 3) {
        return "invisible"
      } else {
          if (!this._showDecision) {
            return mainClass + "none";
          }
        return mainClass + (this.ref2 ? "white" : (this.ref2 === false) ? "red" : "none");
      }
    }

    if (this.publicFacing) {
      if (position == 1) {
        return mainClass + (this.ref1 ? "white" : (this.ref1 === false) ? "red" : "none");
      } else if (position == 2) {
        return mainClass + (this.ref2 ? "white" : (this.ref2 === false) ? "red" : "none");
      } else if (position == 3) {
        return mainClass + (this.ref3 ? "white" : (this.ref3 === false) ? "red" : "none");
      }
    } else {
      // athlete facing, go the other way, right to left
      if (position == 1) {
        return mainClass + (this.ref3 ? "white" : (this.ref3 === false) ? "red" : "none");
      } else if (position == 2) {
        return mainClass + (this.ref2 ? "white" : (this.ref2 === false) ? "red" : "none");
      } else if (position == 3) {
        return mainClass + (this.ref1 ? "white" : (this.ref1 === false) ? "red" : "none");
      }
    }
    return mainClass;
  }

  downStyles() {
    return "display: " + (this._downShown ? "flex" : "none");
  }

  decisionsStyles() {
    return "display: " + (this._downShown ? "none" : "flex");
  }

  decisionWrapperStyle() {
    return "display: grid";
  }

  showJuryRow() {
    // the seven-person panel review vote is always shown with its jury circles
    return (this.showJuryDecisions || this._panelRefStates) && this.juryDecisions.length > 0 && this._showDecision;
  }

  juryIcon(decision) {
    switch (decision) {
      case "voted":
        return "vaadin:circle";
      case "white":
        return "vaadin:check-circle";
      case "red":
        return "vaadin:close-circle";
      default:
        return "vaadin:circle-thin";
    }
  }

  playDownSound() {
    if (!this.silent) {
      this.doDown();
    }
  }

}

customElements.define(DecisionElement.is, DecisionElement);
