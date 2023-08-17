import { html, LitElement, css } from "lit";
/*******************************************************************************
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
          font-weight: normal;
          color: lime;
          display: block;
        }
      `,
    ];
  }
  render() {
    return html` 
    <div class="decisionWrapper" style="${this.decisionWrapperStyle()}" >
      <div class="down" style="font-weight: 900; ${this.downStyles()}">&#x2B73;</div>
      <div class="decisions" style="${this.decisionsStyles()}">
        <span class="${this.decisionClasses(1)}">&nbsp;</span>
        <span class="${this.decisionClasses(2)}">&nbsp;</span>
        <span class="${this.decisionClasses(3)}">&nbsp;</span>
      </div>
    </div>`;
  }

  static get properties() {
    return {
      ref1: {
        type: Boolean,
        reflect: true,
      },
      ref2: {
        type: Boolean,
        reflect: true,
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
        reflect: true,
      },
      jury: {
        type: Boolean,
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
      silent: {
        type: Boolean,
      },
      _downShown: {
        type: Boolean,
        state: true,
      },
      showDownOrDecision: {
        type: Boolean,
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
    this.audio = true;
    this.enabled = false;
    this.silent = false;
  }
  
  _init() {
    console.debug("_init");
    this.downShown = false;
    this.ref1 = null;
    this.ref2 = null;
    this.ref3 = null;
    this.enabled = false;
    this._setupAudio();
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    console.debug("de decision ready");
    if (!this.jury) {
      document.body.addEventListener("keydown", (e) => this._readRef(e));
    }
    this._init();
  }

  _setupAudio() {
    window.AudioContext = window.AudioContext || window.webkitAudioContext;
    this.audioContext = new AudioContext();
    this._prepareAudio();
  }

  _readRef(e) {
    //if (!this.enabled) return;

    var key = e.key;
    console.debug("de key " + key);
    switch (e.key) {
      case "1":
        this.ref1 = true;
        this.ref1Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "2":
        this.ref1 = false;
        this.ref1Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "3":
        this.ref2 = true;
        this.ref2Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "4":
        this.ref2 = false;
        this.ref2Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "5":
        this.ref3 = true;
        this.ref3Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      case "6":
        this.ref3 = false;
        this.ref3Time = Date.now();
        this._majority(this.ref1, this.ref2, this.ref3);
        break;
      default:
        break;
    }
  }

  _registerVote(code) {
    console.debug("de vote " + key);
  }

  /* this is called based on browser input.
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
    if (!this._downShown && (countWhite == 2 || countRed == 2)) {
      this.decision = countWhite >= 2;
      if (!this.jury) this.downShown(true);
    }
    if (countWhite + countRed >= 3) {
      this.decision = countWhite >= 2;
      maj = countWhite >= 2;
    } else {
      maj = undefined;
    }
    this.masterRefereeUpdate(ref1, ref2, ref3);
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

  decisionClasses(position) {
    var mainClass = "decision ";
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
    return "display: flex";
  }

  /*
  This is called from the browser side when the decision is taken locally (majority vote from keypads).
  It can also be called from the server side when the decision is taken elsewhere.
  The server side is responsible for not calling this again if the event took place in this element.
  */
  showDown(isMaster, silent) {
    console.debug("de showDown -- " + !this.silent + " " + !silent);
    if (!this.silent && !silent) {
      this._playTrack("../local/sounds/down.mp3", window.downSignal, true, 0);
    }
    this._downShown = true;

    // hide the down arrow after 2 seconds -- the decisions will show when available
    // (there will be no decision lights for at least one second, more if last referee
    // waits after the other two have given down.
    if (!this.jury) setTimeout(this.hideDown.bind(this), 2000);
  }

  hideDown() {
    this._downShown = false;
  }

  showDecisions(isMaster, ref1, ref2, ref3) {
    console.debug("de showDecision: " + ref1 + " " + ref2 + " " + ref3);
    this.ref1 = ref1;
    this.ref2 = ref2;
    this.ref3 = ref3;
    this.hideDown();
    console.debug("de colorsShown");
  }

  showDecisionsForJury(ref1, ref2, ref3, ref1Time, ref2Time, ref3Time) {
    console.debug("de showDecisionForJury: " + ref1 + " " + ref2 + " " + ref3);
    this.ref1 = ref1;
    this.ref2 = ref2;
    this.ref3 = ref3;
    this.ref1Time = ref1Time;
    this.ref2Time = ref2Time;
    this.ref3Time = ref3Time;
    this.hideDown();
    console.debug("de jury colorsShown");
  }

  reset(isMaster) {
    console.debug("de reset " + isMaster);
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
      console.debug("de reuse track source");
      if (play) {
        // play previously fetched buffer
        await this._playAudioBuffer(previousBuffer, when);
        console.debug("de sound done");
      }
      return previousBuffer;
    } else {
      console.debug("de no previous buffer");
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
    console.debug("setEnabled " + isEnabled + " " + this.audioContext);
    this.enabled = isEnabled;
    if (isEnabled) {
      this.trackSource = this.audioContext.createBufferSource();
      this.trackSource.buffer = window.downSignal;
      this.trackSource.connect(this.audioContext.destination);
      console.debug("connected tracksource");
    }
  }

  _playAudioBuffer(audioBuffer, when) {
    console.debug("when " + when);
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
      console.debug("loaded downSignal = " + window.downSignal);
    } else {
      console.debug("skipping downSignal load");
      console.debug("existing downSignal = " + window.downSignal);
    }
  }

}

customElements.define(DecisionElement.is, DecisionElement);
