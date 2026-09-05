import { html, LitElement, css } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class DecisionBoard extends LitElement {
  static get is() {
    return "decision-board-template";
  }

  render() {
    const board = this.board;
    return html` 
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}"/>
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "decisionboard")}"/>
	  <style>
      .container {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 100%;
        font-weight: light; /* This will affect text within the container if not overridden */
        font-family: "Segoe UI", "Helvetica Neue", Helvetica, Arial, sans-serif;
      }

      .octagon-container {
        position: relative; /* Needed for stacking the octagons */
        width: 65vh;
        height: 65vh;
      }

      .octagon {
        width: 100%;
        height: 100%;
        position: absolute; /* Allows overlapping */
        clip-path: polygon(
          30% 0%,
          70% 0%,
          100% 30%,
          100% 70%,
          70% 100%,
          30% 100%,
          0% 70%,
          0% 30%
        );
        display: flex;
        justify-content: center;
        align-items: center;
      }

      .main-octagon {
        background-color: red;
        color: white;
        font-size: 20vh;
        font-weight: bold;
        text-align: center;
        z-index: 1; /* Ensure it's on top */
      }

      .border-octagon {
        background-color: white;
        width: calc(100% + 2vh); /* Adjust for border thickness */
        height: calc(100% + 2vh); /* Adjust for border thickness */
        top: -1vh; /* Center behind the main octagon */
        left: -1vh; /* Center behind the main octagon */
        z-index: 0; /* Ensure it's behind */
      }

        .blink {
            animation: blink 1.5s step-end infinite;
        }

        @keyframes blink {
            0%, 74% {  /*  1,5 seconds of 2.5s = 75% */
                opacity: 1;
            }
            75%, 100% { /* 0.5 seconds of 2.5s = 25% */
                opacity: 0;
            }
        }

	  </style>
    <div class="wrapper" style="${this.colorOverride}">
      <div class="wrapper bigTitle" style="${this.waitingStyles()}">
        <div class="competitionName">${board.competitionName}</div>
        <br />
        <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
      </div>
      <div class="container blink" style="${this.stopStyles()}">
        <div class="octagon-container">
          <div class="octagon border-octagon"></div>
          <div class="octagon main-octagon">${this.STOP}</div>
        </div>
      </div>
      <div class="decisionBoard" style="${this.activeStyles()}">
        <div class="timer athleteTimer" style="${this.athleteTimerStyles()}">
          <timer-element id="athleteTimer"></timer-element>
        </div>
        <div class="timer breakTime" style="${this.breakTimerStyles()}">
          <timer-element id="breakTimer"></timer-element>
        </div>
        <div class="decision" id="decisionDiv" style="${this.decisionStyles()}">
          <decision-element id="decisions"></decision-element>
        </div>
      </div>
    </div>`;
  }

  /* what follows is integrally copied from attempt board */

  static get properties() {
    return {
      boardState: { type: Object, noAccessor: true },
    
      athletes: {type: Object},
      leaders: {type: Object},
      records: {type: Object},

      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},

      // translation map
      t: { type: Object }
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  get board() {
    return this._boardState;
  }

  get boardState() {
    return this._boardState;
  }

  set boardState(value) {
    const oldValue = this._boardState;
    if (oldValue && value && Number(value.sequence) < Number(oldValue.sequence)) {
      return;
    }
    this._boardState = value ?? DecisionBoard.emptyBoardState();
    this.requestUpdate("boardState", oldValue);
  }

  static emptyBoardState() {
    return {
      athleteImg: "",
      attempt: "",
      competitionName: "",
      decisionVisible: false,
      mode: "WAIT",
      recordAttempt: false,
      recordBroken: false,
      sequence: 0,
      startNumber: 0,
      teamFlagImg: "",
      teamName: "",
      weight: ""
    };
  }

  isBreak() {
    return this.board.mode === "INTERRUPTION" || this.board.mode === "INTRO_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN_CEREMONY" || this.board.mode === "SESSION_DONE" || this.board.mode === "CEREMONY"
  }

  isCountdown() {
    return this.board.mode === "INTRO_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN_CEREMONY"
  }

  athleteImgClasses() {
    var mainClass = "picture";
    return mainClass + 
      (this.board.decisionVisible ?  " hideBecauseDecision" : "") +
      ((this.board.recordAttempt || this.board.recordBroken) ? " hideBecauseRecord" : "");
  }
  teamFlagImgClasses() {
    var mainClass = this.board.athleteImg ? "flagWithPicture" : "flag";
    return mainClass + 
      (this.board.decisionVisible ?  " hideBecauseDecision" : "") +
      ((this.board.recordAttempt || this.board.recordBroken) ? " hideBecauseRecord" : "");
  }

  waitingStyles() {
    return "display: " + (this.board.mode === "WAIT" ? "grid" : "none");
  }

  activeStyles() {
    return "display: " + ((this.board.mode !== "WAIT" && this.board.mode !== "INTERRUPTION") ? "grid" : "none");
  }

  stopStyles() {
    return "display: " + (this.board.mode === "INTERRUPTION" ? "flex" : "none");
  }

  lastNameClasses() {
    return (this.board.athleteImg ? "lastNameWithPicture" : "lastName");
  }
  lastNameStyles() {
    return "display: grid";
  }

  firstNameClasses() {
    return "display: " + (this.board.athleteImg ? "firstNameWithPicture" : "firstName");
  }
  firstNameStyles() {
    return "display: grid";
  }

  teamNameStyles() {
    return "display: " + ((this.board.recordAttempt || this.board.recordBroken || this.isBreak()) ? "none" : "grid");
  }

  teamFlagImgStyles() {
    return "display: " + (this.isBreak() ? "none" : (this.board.mode === "CURRENT_ATHLETE" ? "grid" : "none"));
  }


  athleteImgStyles() {
    return "display: " + ((this.board.mode === "CURRENT_ATHLETE" && (this.board.recordAttempt || this.board.recordBroken)) ? "grid" : "none");
  }

  recordMessageClasses() {
    var mainClass = "recordNotification";
    return mainClass +
      (this.board.recordAttempt ? " attempt" : "") +
      (this.board.recordBroken ? " new" : "") +
      (!this.board.recordAttempt && !this.board.recordBroken ? " none" : "");
  }

  recordMessageStyles() {
    return "display: " + ((this.board.mode === "CURRENT_ATHLETE" && (this.board.recordAttempt || this.board.recordBroken)) ? "grid" : "none");
  }

  attemptStyles() {
    return "display: " + ((this.isBreak() || this.board.decisionVisible) ? "none" : "grid");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "block");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.board.mode === "LIFT_COUNTDOWN" || this.board.mode === "CURRENT_ATHLETE") ? "grid" : "none");
  }

  athleteTimerStyles() {
    return "display:" + ((this.board.mode === "CURRENT_ATHLETE" && !this.board.decisionVisible) ? "grid" : "none");
  }

  breakTimerStyles() {
    return "display:" + ((this.board.mode === "INTRO_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN_CEREMONY") ? "grid" : "none");
  }

  barbellStyles() {
    return "display: none";
  }

  decisionStyles() {
    return "display: " + ((this.board.mode === "CURRENT_ATHLETE" && this.board.decisionVisible) ? "grid" : "none");
  }

  constructor() {
    super();
    this.javaComponentId = "";
    this._boardState = DecisionBoard.emptyBoardState();

    this.stylesDir = "";
    this.autoVersion = 0;
    this.video = "";
  }
}

customElements.define(DecisionBoard.is, DecisionBoard);
