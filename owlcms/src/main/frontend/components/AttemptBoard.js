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
    const board = this.board;
    return html` 
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}"/>
    <!-- link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/resultsCustomization" + (this.autoversion ?? "") + ".css"}"/ -->
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "attemptboard")}"/>

    <div class="${this.wrapperClasses()}" style="${this.colorOverride}">
      <div class="${this.wrapperClasses()} bigTitle" style="${this.waitingStyles()}">
        <div class="competitionName">${board.competitionName}</div>
        <br />
        <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
      </div>
      <div class="attemptBoard" style="${this.activeStyles()}">
        <div id="lastNameDiv" data-testid="attempt-board-last-name" class="${this.lastNameClasses()}" style="${this.lastNameStyles()}">
          <div style="${board.nameSizeOverride}">${board.lastName}</div>
        </div>
        <div data-testid="attempt-board-first-name" class="${this.firstNameClasses()}" style="${this.firstNameStyles()}">
          <div style="${board.firstNameSizeOverride}">${board.firstName}</div>
        </div>
        <div class="${this.teamNameClasses()}" style="${this.teamNameStyles()}">
          ${board.teamName}
        </div>
        <div class="${this.teamFlagImgClasses()}" style="${this.teamFlagImgStyles()}" .innerHTML="${board.teamFlagImg}"></div>
        <div class="${this.athleteImgClasses()}" style="${this.athleteImgStyles()}" .innerHTML="${board.athleteImg}"></div>
        <div class="${this.recordMessageClasses()}" style="${this.recordMessageStyles()}">
          <css-ticker
            text="${board.recordMessage ? board.recordMessage + '     ' : ''}"
            speed="${board.recordMessageSpeed}"
          ></css-ticker>
        </div>
        <div data-testid="attempt-board-start-number" class="startNumber" style="${this.startNumberStyles()}">
          <span>${board.startNumber}</span>
        </div>
        <div class=${classMap({ category: true, longCategory: this.isLongCategory() })} style="${this.attemptStyles()}">
          ${this.categoryContent()}
        </div>
        <div data-testid="attempt-board-attempt" class="attempt" style="${this.attemptStyles()}">
          <span .innerHTML="${board.attempt}"></span>
        </div>
        <div data-testid="attempt-board-weight" class="weight" style="${this.weightStyles()}">
          <span style="white-space: nowrap;">${board.weight}<span style="font-size: 75%">${this.kgSymbol}</span></span>
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
    boardState: { type: Object, noAccessor: true },
      // top
      decisionVisible: { type: Boolean },
      platformName: {},

      athletes: { type: Object },
      leaders: { type: Object },
      records: { type: Object },

      attemptTraces: { type: Boolean },

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
	if (!this.attemptTraces || !changedProperties.has("boardState")) {
      return;
    }
    const renderedStartNumber = this.shadowRoot?.querySelector('[data-testid="attempt-board-start-number"]')?.textContent?.trim() ?? "";
    const renderedWeight = this.shadowRoot?.querySelector('[data-testid="attempt-board-weight"]')?.textContent?.trim() ?? "";
    const weightUsedForRendering = String(this.board.weight ?? "");
    const weightVisible = this.weightStyles().includes("display: grid");
    this.$server?.attemptBoardWeightRendered(
      String(this.board.sequence ?? ""),
      weightUsedForRendering,
      Date.now(),
      renderedStartNumber,
      renderedWeight,
      this.board.mode,
      weightVisible
    );
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
  this._boardState = value ?? CurrentAttempt.emptyBoardState();
  this.requestUpdate("boardState", oldValue);
  }

  static emptyBoardState() {
  return {
    athleteImg: "",
    attempt: "",
    breakType: "",
    category: "",
    competitionName: "",
    firstName: "",
    firstNameSizeOverride: "",
    lastName: "",
    mode: "WAIT",
    nameSizeOverride: "",
    recordAttempt: false,
    recordBroken: false,
    recordMessage: "",
    recordMessageSpeed: 0,
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

  isLongCategory() {
	return this.board.category.length > 10;
  }

  categoryContent() {
  const cat = this.board.category;
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
      ((this.board.recordAttempt || this.board.recordBroken) ? " hideBecauseRecord" : "");
  }
  teamNameClasses() {
    const hasPicture = this.board.athleteImg || this.athletePictures;
    const hasFlag = Boolean(this.board.teamFlagImg) && this.board.mode === "CURRENT_ATHLETE" && !this.isBreak();
    const teamName = this.board.teamName;
    const longTeamName = teamName.length > 28 ? " longTeamName" : "";
    if (hasPicture && hasFlag) return "teamName teamNameWithPictureAndFlag" + longTeamName;
    if (hasPicture) return "teamName teamNameWithPicture" + longTeamName;
    return "teamName" + longTeamName;
  }

  teamFlagImgClasses() {
  var mainClass = (this.board.athleteImg || this.athletePictures) ? "flagWithPicture" : "flag";
    return mainClass +
      (this.decisionVisible ? " hideBecauseDecision" : "") +
      ((this.board.recordAttempt || this.board.recordBroken) ? " hideBecauseRecord" : "");
  }

  waitingStyles() {
  return "display: " + (this.board.mode === "WAIT" ? "grid" : "none");
  }

  activeStyles() {
  return "display: " + (this.board.mode !== "WAIT" ? "grid" : "none");
  }

  lastNameClasses() {
  return (this.board.athleteImg ? "lastNameWithPicture" : "lastName");
  }
  lastNameStyles() {
    return "display: grid";
  }

  firstNameClasses() {
  const hasPicture = this.board.athleteImg || this.athletePictures;
  const showTeamFlag = Boolean(this.board.teamFlagImg) && this.board.mode === "CURRENT_ATHLETE" && !this.isBreak();
    if (hasPicture) {
      return "firstNameWithPicture";
    }
    if (showTeamFlag) {
      return "firstNameWithFlags";
    }
    return "firstName";
  }
  firstNameStyles() {
  const hasPicture = this.board.athleteImg || this.athletePictures;
  const showTeamFlag = Boolean(this.board.teamFlagImg) && this.board.mode === "CURRENT_ATHLETE" && !this.isBreak();
    if (hasPicture || showTeamFlag) {
      return ""; // Let CSS handle the display for these variants
    }
    return "display: grid";
  }

  teamNameStyles() {
  return "display: " + ((this.board.recordAttempt || this.board.recordBroken || this.isBreak()) ? "none" : "grid");
  }

  teamFlagImgStyles() {
  return "display: " + (this.isBreak() ? "none" : (this.board.mode === "CURRENT_ATHLETE" && this.board.teamFlagImg ? "grid" : "none"));
  }


  athleteImgStyles() {
  return "display: " + ((this.board.mode === "CURRENT_ATHLETE" && (this.board.athleteImg || this.athletePictures) && !(this.board.recordAttempt || this.board.recordBroken)) ? "grid" : "none");
  }

  recordMessageClasses() {
    var mainClass = "recordNotification";
    return mainClass +
      (this.board.recordAttempt ? " attempt" : "") +
      (this.board.recordBroken ? " new" : "") +
      (!this.board.recordAttempt && !this.board.recordBroken ? " none" : "");
  }

  recordMessageStyles() {
  return "display: " + ((this.board.mode === "CURRENT_ATHLETE" && (this.board.recordAttempt || this.board.recordBroken)) ? "grid" : "none") +
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
	return "display: " + ((this.board.mode === "LIFT_COUNTDOWN" || (this.board.mode === "CURRENT_ATHLETE") || (this.board.mode === "INTERRUPTION" && this.board.breakType === "TECHNICAL")) ? "grid" : "none");
  }

  athleteTimerStyles() {
  return "display:" + ((this.board.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "grid" : "none") + "; padding-bottom: 10px;";
  }

  breakTimerStyles() {
  return "display:" + ((this.board.mode === "INTRO_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN" || this.board.mode === "LIFT_COUNTDOWN_CEREMONY") ? "grid" : "none");
  }

  barbellStyles() {
  return "display: " + ((this.board.mode === "LIFT_COUNTDOWN" || (this.board.mode === "CURRENT_ATHLETE" && !this.decisionVisible) || (this.board.mode === "INTERRUPTION" && this.board.breakType === "TECHNICAL")) ? "grid" : "none");
  }

  decisionStyles() {
  const style = "display: " + ((this.board.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "grid" : "none");
    return style;
  }

  brandingStyles() {
  const style =  ((this.board.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "display: none"
       : "position: absolute; bottom: 0.5em; right: 2em; align-items: center; font-weight: thin; font-size: 1.5em; line-height: 1.5em");
    return style;
  }

  constructor() {
    super();
    this.javaComponentId = "";
  this._boardState = CurrentAttempt.emptyBoardState();
    this.decisionVisible = false;

    this.stylesDir = "";
    this.autoVersion = 0;
    this.video = "";
  }
}

customElements.define(CurrentAttempt.is, CurrentAttempt);
