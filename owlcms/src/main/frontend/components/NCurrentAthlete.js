import { html, LitElement, css } from "lit";

import { stylesheetHref } from "./stylesheetHref.js";

/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

export class NCurrentAthlete extends LitElement {
  static get is() {
    return "ncurrentathlete-template";
  }

  static get properties() {
    return {
      boardState: { type: Object, noAccessor: true },
      clockValue: { type: String },
    };
  }

  constructor() {
    super();
    this._boardState = NCurrentAthlete.emptyBoardState();

    this.clockValue = "";

    this.longTeam = 3; // max length for short team names
  }

  render() {
    const board = this.board;
    return html`
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}"/>
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "ncurrentathlete")}"/>

      <div class="lower-third-container">
        <div class="circle-container">
          <div class="shadow-circle"></div>
          <div class="circle"></div>
          <div class="white-circle"></div>
        </div>
        <div class="rectangle">
          <div class="top-row spacer"></div>
          <div class="top-row top-left grid-cell" style="grid-column: 2 / span 7;">
            <div style="${board.team.length > this.longTeam ? "display: block; line-height: 1.05" : "display: flex; align-items: center;"}">
              <div class="fullName">${board.fullName}</div>
              <div class="team ${board.showDetails ? "shown" : "hidden"}" style="${board.team.length > this.longTeam ? "font-size: 85%;" : ""}">${board.team.length > this.longTeam ? board.team : ("\u00A0("+board.team+")")}</div>
            </div>
          </div>
          <div class="top-row top-right grid-cell" style="grid-column: 9 / span 2;">
            <div class="clock${board.showAthleteClock ? "" : " hidden"}">
                <timer-element id="timer"></timer-element>
            </div>
            <div class="clock${board.showBreakClock ? "" : " hidden"}">
                <timer-element id="breakTimer"></timer-element>
            </div>
            <div class="decisions ${board.showDecisions ? "shown" : "hidden"}">
              ${board.decisions.map(
                d => html`<div class="decision ${d}"></div>`
              )}
            </div>
            <decision-element id="decisions" style="display:none"></decision-element>
            <!-- hidden elements required because we subclass the results page -->
            <div class="notused" style="display:none">
              <timer-element id="decisionSectionTimer"></timer-element>
              <timer-element id="decisionSectionBreakTimer"></timer-element>
              <timer-element id="decisionSectionStopwatch"></timer-element>
              <decision-element id="decisionSectionReferee"></decision-element>
            </div>
          </div>
          <div class="bottom-row spacer"></div>
          <div class="bottom-row bottom-left grid-cell" style="grid-column: 2 / span 3;">
            <div class="lift ${board.showDetails ? "shown" : "hidden"}" .innerHTML=${board.lift}></div>
          </div>
          <div class="bottom-row bottom-right grid-cell" style="grid-column: 5 / span 6; gap:0.3em; display:flex;">
            ${board.snIndicators.map(
              (v, i) => html`<div class="indicator ${board.snIndicatorClasses[i]} ${board.showAttemptResults ? "shown" : "hidden"}">${v}</div>`
            )}
            <div class="indicator-spacer"></div>
            ${board.cjIndicators.map(
              (v, i) => v
                ? html`<div class="indicator ${board.cjIndicatorClasses[i]} ${board.showAttemptResults ? "shown" : "hidden"}">${v}</div>`
                : html`<div class="indicator ${board.cjIndicatorClasses[i]} ${board.showAttemptResults ? "shown" : "hidden"}"></div>`
            )}
          </div>
        </div>
      </div>
    `;
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
    this._boardState = value ?? NCurrentAthlete.emptyBoardState();
    this.requestUpdate("boardState", oldValue);
  }

  static emptyBoardState() {
    return {
      cjIndicatorClasses: ["empty", "empty", "empty"],
      cjIndicators: ["", "", ""],
      decisions: ["white", "white", "white"],
      fullName: "",
      lift: "",
      mode: "WAIT",
      sequence: 0,
      showAthleteClock: false,
      showAttemptResults: false,
      showBreakClock: false,
      showDecisions: false,
      showDetails: false,
      snIndicatorClasses: ["empty", "empty", "empty"],
      snIndicators: ["", "", ""],
      team: ""
    };
  }
}

customElements.define(NCurrentAthlete.is, NCurrentAthlete);
