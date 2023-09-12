import { html, LitElement, css } from "lit";
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
    return html` 
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "")}.css"/>
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "decisionboard" + (this.autoversion ?? "")}.css"/>
      
      <div class="wrapper" @click="${this._handleClick}">
        <div class="wrapper bigTitle" style="${this.waitingStyles()}">
          <div class="competitionName">${this.competitionName}</div>
          <br />
          <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
        </div>
        <div class="decisionBoard" style="${this.activeStyles()}">
          <div class="barbell" style="${this.barbellStyles()}">
            <slot name="barbell"></slot>
          </div>
          <div class="barbell" style="${this.barbellStyles()}">
            <slot name="barbell"></slot>
          </div>
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
      // top
      fullName: {},
      weight: {},
      attempt: {},
      teamName: {},
      startNumber: {},
      decisionVisible: { type: Boolean },
      competitionName: {},
      groupName: {},
      liftsDone: {},
    
      athletes: {type: Object},
      leaders: {type: Object},
      records: {type: Object},

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},

      // during lifting
      recordAttempt: {},
      recordBroken: {},

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
    this.$server.openDialog();
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  isCountdown() {
    return  this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN"
  }

  athleteImgClasses() {
    var mainClass = "picture";
    return mainClass + 
      (this.decisionVisible ?  " hideBecauseDecision" : "") +
      ((this.recordAttempt || this.recordBroken) ? " hideBecauseRecord" : "");
  }
  teamFlagImgClasses() {
    var mainClass = this.athleteImg ? "flagWithPicture" : "flag";
    return mainClass + 
      (this.decisionVisible ?  " hideBecauseDecision" : "") +
      ((this.recordAttempt || this.recordBroken) ? " hideBecauseRecord" : "");
  }

  waitingStyles() {
    return "display: " + (this.mode === "WAIT" ? "grid" : "none");
  }

  activeStyles() {
    return "display: " + (this.mode !== "WAIT" ? "grid" : "none");
  }

  lastNameClasses() {
    return (this.athleteImg ? "lastNameWithPicture" : "lastName");
  }
  lastNameStyles() {
    return "display: grid";
  }

  firstNameClasses() {
    return "display: " + (this.athleteImg ? "firstNameWithPicture" : "firstName");
  }
  firstNameStyles() {
    return "display: grid";
  }

  teamNameStyles() {
    return "display: " + ((this.recordAttempt || this.recordBroken || this.isBreak()) ? "none" : "grid");
  }

  teamFlagImgStyles() {
    return "display: " + (this.isBreak() ? "none" : ( this.mode === "CURRENT_ATHLETE" ? "grid" : "none"));
  }


  athleteImgStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && (this.recordAttempt || this.recordBroken)) ? "grid" : "none");
  }

  recordMessageClasses() {
    var mainClass = "recordNotification";
    return mainClass +
      (this.recordAttempt ? " attempt" : "") +
      (this.recordBroken ? " new" : "") + 
      (!this.recordAttempt && !this.recordBroken ? " none" : "");
  }

  recordMessageStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && (this.recordAttempt || this.recordBroken)) ? "grid" : "none");
  }

  attemptStyles() {
    return "display: " + ((this.isBreak() || this.decisionVisible) ? "none" : "grid");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "block");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE")) ? "grid" : "none");
  }

  athleteTimerStyles() {
    return "display:" + ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "grid" : "none");
  }

  breakTimerStyles() {
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN") ? "grid" : "none");
  }

  barbellStyles() {
    return "display: none";
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "grid" : "none");
  }

  _handleClick() {
    this.$server.openDialog();
  }

  constructor() {
    super();
    this.javaComponentId = "";
    this.lastName = "";
    this.firstName = "";
    this.weight = 0;
    this.competitionName = "";

    this.mode == "WAIT";

    this.attempt = "";
    this.athleteImg = "";
    this.teamName = "";
    this.teamFlagImg = "";
    this.startNumber = 0;
    this.decisionVisible = false;
    this.recordAttempt = false;
    this.recordBroken = false;

    this.stylesDir = "";
    this.autoVersion = 0;
    this.video = "";
  }
}

customElements.define(DecisionBoard.is, DecisionBoard);
