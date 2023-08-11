import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

import "@polymer/iron-icon/iron-icon.js";
import "@polymer/iron-icons/iron-icons.js";

class DecisionElementPR extends LitElement {
  static get is() {
    return "decision-element-pr";
  }

  static get styles() {
    return [
      css`
        .decisionWrapper {
          width: 100%;
          height: 100%;
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
          border: medium solid var(--lumo-contrast);
          margin: 3%;
          /* 	background-color: #333333; */
          width: 30%;
        }
        .red {
          background-color: red;
        }

        .white {
          background-color: white;
        }

        .none {
          background-color: var(--lumo-contrast-20pct);
          border: medium dashed var(--lumo-contrast);
        }

        .down {
          display: flex;
          align-items: center;
          justify-content: space-evenly;
          --iron-icon-height: 35%;
          --iron-icon-width: 35%;
          font-weight: normal;
          color: lime;
          display: block;
        }
      `,
    ];
  }
  render() {
    return html` <div class="decisionWrapper">
      <div class="down" id="downDiv">
        <iron-icon id="down-arrow" icon="icons:file-download"></iron-icon>
      </div>
      <div class="decisions" id="decisionsDiv">
        <span class="decision" id="ref1span"></span>&nbsp;
        <span class="decision" id="ref2span"></span>&nbsp;
        <span class="decision" id="ref3span"></span>&nbsp;
      </div>
    </div>`;
  }

  static get properties() {
    return {
      ref1: {
        type: Boolean,
        reflect: true,
        notify: true,
      },
      ref2: {
        type: Boolean,
        reflect: true,
        notify: true,
      },
      ref3: {
        type: Boolean,
        reflect: true,
        notify: true,
      },
      ref1Time: {
        type: Number,

        notify: false,
      },
      ref2Time: {
        type: Number,

        notify: false,
      },
      ref3Time: {
        type: Number,

        notify: false,
      },
      decision: {
        type: Boolean,
        notify: true,
      },
      publicFacing: {
        type: Boolean,
        notify: true,
        reflect: true,
      },
      jury: {
        type: Boolean,
        notify: true,
        reflect: true,
      },
      audio: {
        type: Boolean,
      },
      enabled: {
        type: Boolean,
      },
      fopName: {
        type: String,
        notify: true,
      },
      /**
       * Set to true to have no sound on down signal
       *
       * @default false
       */
      silent: {
        type: Boolean,
      },
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    console.warn("de decision ready");
    if (!this.jury) {
      document.body.addEventListener("keydown", (e) => this._readRef(e));
    }
    this._init();
  }

  _init() {
    this.renderRoot.querySelector("#decisionsDiv").style.display = "none";
    this.renderRoot.querySelector("#downDiv").style.display = "none";
    this.downShown = false;

    this.renderRoot.querySelector("#ref1span").className = "decision none";
    this.renderRoot.querySelector("#ref2span").className = "decision none";
    this.renderRoot.querySelector("#ref3span").className = "decision none";
    this.set("ref1", null);
    this.set("ref2", null);
    this.set("ref3", null);
    this._setupAudio();
  }

  _setupAudio() {
    window.AudioContext = window.AudioContext || window.webkitAudioContext;
    this.audioContext = new AudioContext();
    this._prepareAudio();
  }

  _readRef(e) {
    if (!this.enabled) return;

    var key = e.key;
    console.warn("de key " + key);
    switch (e.key) {
      case "1":
        this.set("ref1", true);
        this.set("ref1Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "2":
        this.set("ref1", false);
        this.set("ref1Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "3":
        this.set("ref2", true);
        this.set("ref2Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "4":
        this.set("ref2", false);
        this.set("ref2Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "5":
        this.set("ref3", true);
        this.set("ref3Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "6":
        this.set("ref3", false);
        this.set("ref3Time", Date.now());
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      default:
        break;
    }
  }

  _registerVote(code) {
    console.warn("de vote " + key);
  }

  /* this is called from the client side to signal that a decision has been made
         immediate feedback is given if majority has been reached */
  _majority(ref1, ref2, ref3) {
    var countWhite = 0;
    var countRed = 0;
    var maj = false;

    if (ref1 === true) {
      countWhite++;
    } else if (ref1 === false) {
      countRed++;
    }
    if (ref2 === true) {
      countWhite++;
    } else if (ref2 === false) {
      countRed++;
    }
    if (ref3 === true) {
      countWhite++;
    } else if (ref3 === false) {
      countRed++;
    }
    var count = countWhite + countRed;
    if (!this.downShown && (countWhite == 2 || countRed == 2)) {
      this.decision = countWhite >= 2;
      //if (!this.jury) this.showDown(true);
    }
    if (countWhite + countRed >= 3) {
      this.decision = countWhite >= 2;
      maj = countWhite >= 2;
    } else {
      maj = undefined;
    }
    //this.masterRefereeUpdate(ref1, ref2, ref3);
    return maj;
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

  setColors(parent, ref1, ref2, ref3) {
    var redStyle = "decision red";
    var whiteStyle = "decision white";
    if (this.publicFacing) {
      if (ref1 === true) {
        parent.$.ref1span.className = whiteStyle;
      } else if (ref1 === false) {
        parent.$.ref1span.className = redStyle;
      }
      if (ref2 === true) {
        parent.$.ref2span.className = whiteStyle;
      } else if (ref2 === false) {
        parent.$.ref2span.className = redStyle;
      }
      if (ref3 === true) {
        parent.$.ref3span.className = whiteStyle;
      } else if (ref3 === false) {
        parent.$.ref3span.className = redStyle;
      }
    } else {
      // athlete facing, go the other way, right to left
      if (ref1 === true) {
        parent.$.ref3span.className = whiteStyle;
      } else if (ref1 === false) {
        parent.$.ref3span.className = redStyle;
      }
      if (ref2 === true) {
        parent.$.ref2span.className = whiteStyle;
      } else if (ref2 === false) {
        parent.$.ref2span.className = redStyle;
      }
      if (ref3 === true) {
        parent.$.ref1span.className = whiteStyle;
      } else if (ref3 === false) {
        parent.$.ref1span.className = redStyle;
      }
    }
  }

  /*
	This is called from the browser side when the decision is taken locally (majority vote from keypads).
	It can also be called from the server side when the decision is taken elsewhere.
	The server side is responsible for not calling this again if the event took place in this element.
	*/
  showDown(isMaster, silent) {
    console.warn("de showDown " + silent);
    if (!this.silent && !silent) {
      this._playTrack("../local/sounds/down.mp3", window.downSignal, true, 0);
    }
    this.downShown = true;
    this.renderRoot.querySelector("#downDiv").style.display = "flex";
    this.renderRoot.querySelector("#decisionsDiv").style.display = "none";

    // hide the down arrow after 2 seconds -- the decisions will show when available
    // (there will be no decision lights for at least one second, more if last referee
    // waits after the other two have given down.
    if (!this.jury) setTimeout(this.hideDown.bind(this), 2000);
  }

  hideDown() {
    this.renderRoot.querySelector("#downDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionsDiv").style.display = "flex";
  }

  showDecisions(isMaster, ref1, ref2, ref3) {
    this.hideDown();
    console.warn("de showDecision: " + ref1 + " " + ref2 + " " + ref3);
    this.setColors(this, ref1, ref2, ref3);
    console.warn("de colorsShown");
  }

  showDecisionsForJury(ref1, ref2, ref3, ref1Time, ref2Time, ref3Time) {
    this.hideDown();
    console.warn("de showDecisionForJury: " + ref1 + " " + ref2 + " " + ref3);
    this.setColors(this, ref1, ref2, ref3);
    console.warn("de jury colorsShown");
  }

  reset(isMaster) {
    console.warn("de reset " + isMaster);
    this.hideDecisions();
    this._init();
  }

  hideDecisions() {
    // tell our parent to hide us.
    this.dispatchEvent(
      new CustomEvent("hide", { bubbles: true, composed: true })
    );
  }

  async _playTrack(filepath, previousBuffer, play, when) {
    if (previousBuffer) {
      console.warn("de reuse track source");
      if (play) {
        // play previously fetched buffer
        await this._playAudioBuffer(previousBuffer, when);
        console.warn("de sound done");
      }
      return previousBuffer;
    } else {
      console.warn("de no previous buffer");
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
            const trackSource = await window.audioCtx.createBufferSource();
            trackSource.buffer = audioBuffer;
            trackSource.connect(window.audioCtx.destination);
            if (when <= 0) {
              trackSource.start();
            } else {
              trackSource.start(when, 0);
            }
          }
        },
        (e) => {
          console.error("could not decode " + e.err);
        }
      );
      return newBuffer;
    }
  }

  setEnabled(isEnabled) {
    console.warn("setEnabled " + isEnabled + " " + this.audioContext);
    this.enabled = isEnabled;
    if (isEnabled) {
      this.trackSource = this.audioContext.createBufferSource();
      this.trackSource.buffer = window.downSignal;
      this.trackSource.connect(this.audioContext.destination);
      console.warn("connected tracksource");
    }
  }

  _playAudioBuffer(audioBuffer, when) {
    console.warn("when " + when);
    if (when <= 0) {
      this.trackSource.start();
    } else {
      this.trackSource.start(when, 0);
    }

    return this.trackSource;
  }

  async _prepareAudio() {
    if (window.isIOS) {
      // prefetched buffers are not available later for some unexplained reason.
      // so we don't attempt fetching.
      return;
    }
    if (!window.downSignal /* && ! this.loadingDownSignal */) {
      this.loadingDownSignal = true;
      const downSignal = await this._playTrack(
        "../local/sounds/down.mp3",
        null,
        false,
        0
      );
      window.downSignal = downSignal;
      console.warn("loaded downSignal = " + window.downSignal);
    } else {
      console.warn("skipping downSignal load");
      console.warn("existing downSignal = " + window.downSignal);
    }
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
    this.audio = true;
    this.enabled = false;
    this.silent = false;
  }
}

customElements.define(DecisionElementPR.is, DecisionElementPR);
