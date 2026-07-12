import { html, LitElement, css } from "lit";
import { styleMap } from 'lit/directives/style-map.js';
import { classMap } from 'lit/directives/class-map.js';
import { stylesheetHref } from "./stylesheetHref.js";

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
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}"/>
    <!-- link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/resultsCustomization" + (this.autoversion ?? "") + ".css"}"/ -->
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "attemptboard")}"/>

    <div class="${this.wrapperClasses()}" style="${this.colorOverride}">
      <div class="${this.wrapperClasses()} bigTitle" style="${this.waitingStyles()}">
        <div class="competitionName">${this.competitionName}</div>
        <br />
        <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
      </div>
      <div class="attemptBoard" style="${this.activeStyles()}">
        <div id="lastNameDiv" data-testid="attempt-board-last-name" class="${this.lastNameClasses()}" style="${this.lastNameStyles()}">
          <div style="${this.nameSizeOverride}">${this.lastName}</div>
        </div>
        <div data-testid="attempt-board-first-name" class="${this.firstNameClasses()}" style="${this.firstNameStyles()}">
          <div style="${this.firstNameSizeOverride}">${this.firstName}</div>
        </div>
        <div class="${this.teamNameClasses()}" style="${this.teamNameStyles()}">
          ${this.teamName}
        </div>
        <div class="${this.teamFlagImgClasses()}" style="${this.teamFlagImgStyles()}" .innerHTML="${this.teamFlagImg}"></div>
        <div class="${this.athleteImgClasses()}" style="${this.athleteImgStyles()}" .innerHTML="${this.athleteImg}"></div>
        <div class="${this.recordMessageClasses()}" style="${this.recordMessageStyles()}">
          <css-ticker
            text="${this.recordMessage ? this.recordMessage + '     ' : ''}"
            speed="${this.recordMessageSpeed}"
          ></css-ticker>
        </div>
        <div data-testid="attempt-board-start-number" class="startNumber" style="${this.startNumberStyles()}">
          <span>${this.startNumber}</span>
        </div>
        <div class=${classMap({ category: true, longCategory: this.isLongCategory() })} style="${this.attemptStyles()}">
          ${this.categoryContent()}
        </div>
        <div data-testid="attempt-board-attempt" class="attempt" style="${this.attemptStyles()}">
          <span .innerHTML="${this.attempt}"></span>
        </div>
        <div data-testid="attempt-board-weight" class="weight" style="${this.weightStyles()}">
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
      <div class="branding" style="${this.brandingStyles()}"><img src="local/logos/owlcms-logo.svg" style="height:1.25em; margin-bottom:-0.2em">&nbsp;owlcms</div>
    </div>`;
  }

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
      platformName: {},

      athletes: { type: Object },
      leaders: { type: Object },
      records: { type: Object },

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},
      breakType: {},
      initMode: {type: Boolean },

      // during lifting

      recordAttempt: {},
      recordBroken: {},
      recordMessage: {},
      recordMessageSpeed: {},
      attemptTraces: { type: Boolean },
      displaySequence: {},

      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},
	    colorOverride: {},
      athletePictures: { type: Boolean },

      // translation map
      t: { type: Object }
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    if (!this.attemptTraces || !changedProperties.has("weight")) {
      return;
    }
    const renderedWeight = this.shadowRoot?.querySelector('[data-testid="attempt-board-weight"]')?.textContent?.trim() ?? "";
    this.$server?.attemptBoardWeightRendered(
      String(this.displaySequence ?? ""),
      String(this.weight ?? ""),
      Date.now(),
      performance.now(),
      renderedWeight
    );
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  isCountdown() {
    return this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY"
  }

  isLongCategory() {
    return (this.category ?? "").length > 10;
  }

  categoryContent() {
    const cat = this.category ?? "";
    const words = cat.trim().split(/\s+/);
    if (words.length < 4) {
      return html`<span style="white-space: nowrap;">${cat}</span>`;
    }
    const first = words.slice(0, words.length - 2).join(" ");
    const last = words.slice(words.length - 2).join(" ");
    return html`<span style="white-space: pre;">${first + "\n" + last}</span>`;
  }

  wrapperClasses() {
    var classes = "wrapper dark";
    classes = classes + (this.platformName ? " " + this.platformName : "");
    return classes;
  }

  athleteImgClasses() {
    var mainClass = "picture";
    return mainClass +
      (this.decisionVisible ? " hideBecauseDecision" : "") +
      ((this.recordAttempt || this.recordBroken) ? " hideBecauseRecord" : "");
  }
  teamNameClasses() {
    const hasPicture = this.athleteImg || this.athletePictures;
    const hasFlag = Boolean(this.teamFlagImg) && this.mode === "CURRENT_ATHLETE" && !this.isBreak();
    const teamName = this.teamName ?? "";
    const longTeamName = teamName.length > 28 ? " longTeamName" : "";
    if (hasPicture && hasFlag) return "teamName teamNameWithPictureAndFlag" + longTeamName;
    if (hasPicture) return "teamName teamNameWithPicture" + longTeamName;
    return "teamName" + longTeamName;
  }

  teamFlagImgClasses() {
    var mainClass = (this.athleteImg || this.athletePictures) ? "flagWithPicture" : "flag";
    return mainClass +
      (this.decisionVisible ? " hideBecauseDecision" : "") +
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
    const hasPicture = this.athleteImg || this.athletePictures;
    const showTeamFlag = Boolean(this.teamFlagImg) && this.mode === "CURRENT_ATHLETE" && !this.isBreak();
    if (hasPicture) {
      return "firstNameWithPicture";
    }
    if (showTeamFlag) {
      return "firstNameWithFlags";
    }
    return "firstName";
  }
  firstNameStyles() {
    const hasPicture = this.athleteImg || this.athletePictures;
    const showTeamFlag = Boolean(this.teamFlagImg) && this.mode === "CURRENT_ATHLETE" && !this.isBreak();
    if (hasPicture || showTeamFlag) {
      return ""; // Let CSS handle the display for these variants
    }
    return "display: grid";
  }

  teamNameStyles() {
    return "display: " + ((this.recordAttempt || this.recordBroken || this.isBreak()) ? "none" : "grid");
  }

  teamFlagImgStyles() {
    return "display: " + (this.isBreak() ? "none" : (this.mode === "CURRENT_ATHLETE" && this.teamFlagImg ? "grid" : "none"));
  }


  athleteImgStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && (this.athleteImg || this.athletePictures) && !(this.recordAttempt || this.recordBroken)) ? "grid" : "none");
  }

  recordMessageClasses() {
    var mainClass = "recordNotification";
    return mainClass +
      (this.recordAttempt ? " attempt" : "") +
      (this.recordBroken ? " new" : "") +
      (!this.recordAttempt && !this.recordBroken ? " none" : "");
  }

  recordMessageStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && (this.recordAttempt || this.recordBroken)) ? "grid" : "none") +
           "; height: auto; overflow: hidden; align-items: stretch; padding: 0; margin: 0;";
  }

  attemptStyles() {
    return "display: " + ((this.isBreak() || this.decisionVisible) ? "none" : "grid");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "block");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE") || (this.mode === "INTERRUPTION" && this.breakType === "TECHNICAL")) ? "grid" : "none");
  }

  athleteTimerStyles() {
    return "display:" + ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "grid" : "none") + "; padding-bottom: 10px;";
  }

  breakTimerStyles() {
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY") ? "grid" : "none");
  }

  barbellStyles() {
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) || (this.mode === "INTERRUPTION" && this.breakType === "TECHNICAL")) ? "grid" : "none");
  }

  decisionStyles() {
    const style = "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "grid" : "none");
    return style;
  }

  brandingStyles() {
    const style =  ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "display: none"
       : "position: absolute; bottom: 0.5em; right: 2em; align-items: center; font-weight: thin; font-size: 1.5em; line-height: 1.5em");
    return style;
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
