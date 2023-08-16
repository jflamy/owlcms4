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


  /*     const lastNameClasses = {
      lastName: !this.athleteImg,
      lastNameWithPicture: this.athleteImg,
    }
    const lastNameStyles = { display: "grid" };

    const firstNameClasses = {
      firstName: !this.athleteImg,
      firstNameWithPicture: this.athleteImg,
    }
    const firstNameStyles = { display: "grid" };

    const teamNameStyles = { 
      display: (this.recordAttempt || this.recordBroken || this.isBreak()) ? "none" : "grid" 
    };

    const teamFlagImgClasses = {
      flag: !this.athleteImg,
      flagWithPicture: this.athleteImg,
      hideBecauseDecision: this.decisionVisible,
      hideBecauseRecord: (this.recordAttempt || this.recordBroken),
    }
    const teamFlagImgStyles = { 
      display: this.isBreak() ? "none" : ( this.currentAthleteMode ? "grid" : "none")
    };

    const athleteImgClasses = {
      picture: true,
      hideBecauseDecision: this.decisionVisible,
      hideBecauseRecord: (this.recordAttempt || this.recordBroken),
    }
    const athleteImgStyles = { 
      display: this.isBreak() ? "none" : ( this.currentAthleteMode ? "grid" : "none")
    };

    const recordMessageClasses = {
      recordNotification: true,
      attempt: this.recordAttempt,
      new: this.recordBroken,
    }
    const recordMessageStyles = { 
      display: this.currentAthleteMode && (this.recordAttempt || this.recordBroken) ? "grid" : "none" 
    };

    const attemptStyles = { 
      display: this.isBreak() ? "none" : "grid" 
    };
    const startNumberStyles = { 
      display: this.isBreak() ? "none" : "block" 
    };

    const weightStyles = {
      // weights are visible during countdown
      display: (this.isBreak() && ! this.countdownMode) ? "none" : "grid" 
    };

    const athleteTimerStyles = { 
      display:  this.currentAthleteMode && !this.decisionVisible ? "grid" : "none",
    }
    const breakTimerStyles = {
      display: (this.introCountdownMode || this.liftCountdownMode) ? "grid" : "none"
    };

    const barbellStyles = { 
      display: (this.liftCountdownMode || (this.currentAthleteMode && !this.decisionVisible)) ? "grid" : "none"
    };

    const decisionStyles = { 
      display: (this.currentAthleteMode && this.decisionVisible) ? "grid" : "none"
    };
 */
  render() {
  return html` 
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "") + ".css"}"/>
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "resultsCustomization" + (this.autoversion ?? "") + ".css"}"/>
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "attemptboard" + (this.autoversion ?? "") + ".css"}"/>

    <div class="${"wrapper " + (this.inactiveClass ?? "")}">
      <div style="${this.inactiveBlockStyle}">
        <div class="competitionName">${this.competitionName}</div>
        <br />
        <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
      </div>
      <div class="attemptBoard" style="${this.activeGridStyle}">
        <div id="lastNameDiv" class=${classMap(lastNameClasses)} style=${styleMap(lastNameStyles)}>
          <div>${this.lastName}</div>
        </div>
        <div class="${classMap(firstNameClasses)}" style="${styleMap(firstNameStyles)}">
          <div>${this.firstName}</div>
        </div>
        <div class="teamName" style="${styleMap(teamNameStyles)}">
          ${this.teamName}
        </div>
        <div class="${classMap(teamFlagImgClasses)}" style="${styleMap(teamFlagImgStyles)}" .innerHTML="${this.teamFlagImg}"></div>
        <div class="${classMap(athleteImgClasses)}" style="${styleMap(athleteImgStyles)}" .innerHTML="${this.athleteImg}"></div>
        <div class="${classMap(recordMessageClasses)}" style="${styleMap(recordMessageStyles)}">
          ${this.recordMessage}
        </div>
        <div class="startNumber" style="${styleMap(startNumberStyles)}">
          <span>${this.startNumber}</span>
        </div>
        <div class="category" style="${styleMap(attemptStyles)}">
          <span style="white-space: nowrap;">${this.category}</span>
        </div>
        <div class="attempt" style="${styleMap(attemptStyles)}">
          <span .innerHTML="${this.attempt}"></span>
        </div>
        <div class="weight" style="${styleMap(weightStyles)}">
          <span style="white-space: nowrap;">${this.weight}<span style="font-size: 75%">${this.kgSymbol}</span></span>
        </div>
        <div class="barbell" style="${styleMap(barbellStyles)}">
          <slot></slot>
        </div>
        <div class="timer athleteTimer">
          <timer-element id="athleteTimer"></timer-element>
        </div>
        <div class="timer breakTime">
          <timer-element id="breakTimer"></timer-element>
        </div>
        <div class="decision" id="decisionDiv">
          <decision-element id="decisions"></decision-element>
        </div>
      </div>
    </div>`;
  }

  static get properties() {
    // the "Mode" properties are mutually exclusive.
    // caller is responsible for enforcing this.

    return {
      // shared
      lastName: {},
      firstName: {},
      weight: {},
      competitionName: {},

      // prior to lifting
      waitMode: { type: Boolean },
      introCountdownMode: { type: Boolean },
      liftCountdownMode: { type: Boolean },
      // immediately after last attempt
      doneMode: { type: Boolean },

      // during lifting
      currentAthleteMode: { type: Boolean },
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

  isBreak() {
    return (interruptionMode || introCountdownMode || liftCountdownDownMode)
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    console.warn("attemptBoard reset timers" + this.javaComponentId);
    var s;
    (s = this.renderRoot.querySelector("#athleteTimerDiv")) && (s.reset())
    (s = this.renderRoot.querySelector("#breakTimerDiv")) && (s.reset())
    console.warn("end of attemptBoard reset timers" + this.javaComponentId);
  }

  clear() {
    console.warn("attemptBoard clear " + this.javaComponentId);
    var s;
    (s = this.renderRoot.querySelector("#attemptBoardDiv")) && (s.style.display = "none");
    console.warn("attemptBoard end clear " + this.javaComponentId);
  }

  reload() {
    console.log("reloading");
    window.location.reload();
  }

  update() {
    this.requestUpdate();
  }

  constructor() {
    super();
    this.javaComponentId = "";
    this.lastName = "";
    this.firstName = "";
    this.teamName = "";
    this.startNumber = 0;
    this.attempt = "";
    this.weight = 0;
    this.waitMode = true;
  }
}

customElements.define(CurrentAttempt.is, CurrentAttempt);
