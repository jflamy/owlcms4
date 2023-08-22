import { html, LitElement, css } from "lit";
import {styleMap} from 'lit/directives/style-map.js';
import {classMap} from 'lit/directives/class-map.js';

/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class CurrentAttempt extends LitElement {
  static get is() {
    return "attempt-board-template";
  }
    
  render() {
    return html` 
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "") + ".css"}"/>
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "resultsCustomization" + (this.autoversion ?? "") + ".css"}"/>
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "attemptboard" + (this.autoversion ?? "") + ".css"}"/>

    <div class="wrapper">
      <div class="wrapper bigTitle" style="${this.waitingStyles()}">
        <div class="competitionName">${this.competitionName}</div>
        <br />
        <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
      </div>
      <div class="attemptBoard" style="${this.activeStyles()}">
        <div id="lastNameDiv" class=${this.lastNameClasses()} style=${this.lastNameStyles()}>
          <div>${this.lastName}</div>
        </div>
        <div class="${this.firstNameClasses()}" style="${this.firstNameStyles()}">
          <div>${this.firstName}</div>
        </div>
        <div class="teamName" style="${this.teamNameStyles()}">
          ${this.teamName}
        </div>
        <div class="${this.teamFlagImgClasses()}" style="${this.teamFlagImgStyles()}" .innerHTML="${this.teamFlagImg}"></div>
        <div class="${this.athleteImgClasses()}" style="${this.athleteImgStyles()}" .innerHTML="${this.athleteImg}"></div>
        <div class="${this.recordMessageClasses()}" style="${this.recordMessageStyles()}">
          ${this.recordMessage}
        </div>
        <div class="startNumber" style="${this.startNumberStyles()}">
          <span>${this.startNumber}</span>
        </div>
        <div class="category" style="${this.attemptStyles()}">
          <span style="white-space: nowrap;">${this.category}</span>
        </div>
        <div class="attempt" style="${this.attemptStyles()}">
          <span .innerHTML="${this.attempt}"></span>
        </div>
        <div class="weight" style="${this.weightStyles()}">
          <span style="white-space: nowrap;">${this.weight}<span style="font-size: 75%">${this.kgSymbol}</span></span>
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

  static get properties() {
    return {
      // shared
      lastName: {},
      firstName: {},
      weight: {},
      competitionName: {},

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE
      mode: {},

      // during lifting
      attempt: {},
      athleteImg: {},
      teamName: {},
      teamFlagImg: {},
      startNumber: {},
      decisionVisible: { type: Boolean },
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
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "SESSION_DONE"
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
    return "display: " + (this.mode === "WAIT" ? "block" : "none");
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
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE" && !this.decisionVisible)) ? "grid" : "none");
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "grid" : "none");
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

customElements.define(CurrentAttempt.is, CurrentAttempt);
