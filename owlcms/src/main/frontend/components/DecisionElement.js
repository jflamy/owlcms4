import { html, LitElement, css } from "lit";

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

  static get styles() {
    return [
      css`
        :host {
          display: flex;
          height: 100%;
        }

        :host([size="large"]) {
          --attempt-font-size: 25vh;
        }

        :host([size="x-large"]) {
          --attempt-font-size: 40vh;
        }

        :host(:not([size])),
        :host([size="small"]) {
          --attempt-font-size: 1.2em;
        }
        .decisionWrapper {
          width: 100%;
          height: 100%;
          text-align: center;
        }

        .decisions {
          display: none;
          height: 100%;
          width: 100%;
          display: flex;
          align-items: stretch;
          justify-content: space-between;
        }

        .decision {
          border-radius: 5%;
          border: 2px solid var(--lumo-contrast);
          margin: 3%;
          /* 	background-color: #333333; */
          width: 30%;
        }

        .soloDecision {
          border-radius: 50%;
          border: 2px solid var(--lumo-contrast);
          margin: 0;
          padding: 0;
          width: var(--solo-decision-size, var(--attempt-font-size, 20vh));
          height: var(--solo-decision-size, var(--attempt-font-size, 20vh));
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: calc(var(--solo-decision-size, var(--attempt-font-size, 20vh)) * 0.6);
          line-height: 1;
          color: black;
          align-self: center;
        }

        /* .soloDecision.none {
          visibility: hidden;
        } */

        .red {
          background-color: red;
        }

        .white {
          background-color: white;
        }

        .none {
          background-color: var(--lumo-contrast-20pct);
          border: 2px dashed var(--lumo-contrast);
        }

        .invisible {
          visibility: hidden;
        }

        .down {
          display: flex;
          align-items: center;
          justify-content: space-evenly;
          font-weight: normal;
          color: lime;
          display: block;
          font-family: 'Arial Black', Arial, Helvetica, sans-serif;
        }
      `,
    ];
  }

  render() {
    return html` 
      <audio preload="auto" id="down" src="../local/sounds/down.mp3"></audio>
      <div class="decisionWrapper" style="${this.decisionWrapperStyle()}" >
        <div class="down" style="font-weight: 900; ${this.downStyles()}"><vaadin-icon icon="vaadin:arrow-circle-down"></vaadin-icon></div>
        <div class="decisions" style="${this.decisionsStyles()}">
          <span class="${this.decisionClasses(1)}">&nbsp;</span>
          <span class="${this.decisionClasses(2)}" style="${((this.singleRef && this.ref2 !== null) ? "border: 2px solid var(--lumo-contrast); font-weight: bold" : "")}">${((this.singleRef && this.ref2 === true) ? "✓" : (this.singleRef && this.ref2 === false) ? "✕" : "")}</span>
          <span class="${this.decisionClasses(3)}">&nbsp;</span>
        </div>
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
      ref1Time: {
        type: Number,
      },
      ref2Time: {
        type: Number,
      },
      ref3Time: {
        type: Number,
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
      enabled: {
        type: Boolean,
        state: true,
      },
      fopName: {
        type: String,
        notify: true,
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
      }
    };
  }

  constructor() {
    super();
    this.ref1 = null;
    this.ref2 = null;
    this.ref3 = null;
    this.ref1Time = 0;
    this.ref2Time = 0;
    this.ref3Time = 0;
    this.publicFacing = true;
    this.jury = false;
    this.singleRef = false;
    this.enabled = false;
    this.silent = false;
    this._downShown = false;
    this._showDecision = false;
    this.size = "small";
    this.decisionPayload = null;
    // sequence of the last decisionPayload applied; drops stale/out-of-order payloads.
    this._lastDecisionSequence = 0;
    this._localDownSoundPlayed = false;
    // important - the handlers must be bound so "this" is the current DecisionElement instance.
    this._readRef = this._readRef.bind(this);
    this.initSounds = this.initSounds.bind(this)
  }

  connectedCallback() {
    super.connectedCallback();
    // The server (DecisionElement.java) is the single source of truth for the
    // decision state. Never clear ref/decision properties here: on re-attach the
    // server re-syncs them, and self-clearing would race that sync and blank the boxes.
    document.body.addEventListener('keydown', this._readRef);
    document.addEventListener('initSounds', this.initSounds);
  }

  disconnectedCallback() {
    document.body.removeEventListener('keydown', this._readRef);
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

  _readRef(e) {
    if (!this.enabled || this.jury) return;

    switch (e.key) {
      case "1":
        this.ref1 = true;
        this.ref1Time = Date.now();
        break;
      case "2":
        this.ref1 = false;
        this.ref1Time = Date.now();
        break;
      case "3":
        this.ref2 = true;
        this.ref2Time = Date.now();
        break;
      case "4":
        this.ref2 = false;
        this.ref2Time = Date.now();
        break;
      case "5":
        this.ref3 = true;
        this.ref3Time = Date.now();
        break;
      case "6":
        this.ref3 = false;
        this.ref3Time = Date.now();
        break;
      default:
        return;
    }
    // Keyboard-driven: this device issues the down itself as soon as a majority
    // agrees, for zero-latency feedback. The server still broadcasts the
    // authoritative down and the decision lights back through decisionPayload.
    this._issueLocalDownIfMajority();
    this.masterRefereeUpdate(this.ref1, this.ref2, this.ref3);
  }

  /* Local down for keyboard-driven decisions. Sets the down state directly (it does
     not consume a decisionPayload sequence), so the later server "down" payload is
     idempotent and the "decision" payload still flips to the lights atomically. */
  _issueLocalDownIfMajority() {
    if (this._downShown) {
      return;
    }
    let whites = 0;
    let reds = 0;
    for (const ref of [this.ref1, this.ref2, this.ref3]) {
      if (ref === true) {
        whites++;
      } else if (ref === false) {
        reds++;
      }
    }
    if (whites >= 2 || reds >= 2) {
      this.singleRef = false;
      this._downShown = true;
      this._showDecision = false;
      if (!this.silent) {
        this.doDown();
        this._localDownSoundPlayed = true;
      }
    }
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
    switch (payload.mode) {
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
        this._localDownSoundPlayed = false;
        break;
      case "reset":
        this.ref1 = null;
        this.ref2 = null;
        this.ref3 = null;
        this._downShown = false;
        this._showDecision = Boolean(payload.showDecision);
        this._localDownSoundPlayed = false;
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

  /* the individual values are set in the this.refN properties. this tells the server that the
     values are are available; the server will call back the slaves operating in jury display
     mode to update their displays immediately.  the slaves not operating in jury display mode
     (e.g. the attempt board) will be updated after 3 seconds */
  masterRefereeUpdate(ref1, ref2, ref3) {
    this.$server.masterRefereeUpdate(
      this.fopName,
      ref1,
      ref2,
      ref3,
      this.ref1Time,
      this.ref2Time,
      this.ref3Time
    );
  }

  decisionClasses(position) {
    var mainClass = "decision ";

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

  playDownSound() {
    if (this._localDownSoundPlayed) {
      this._localDownSoundPlayed = false;
      return;
    }
    if (!this.silent) {
      this.doDown();
    }
  }

}

customElements.define(DecisionElement.is, DecisionElement);
