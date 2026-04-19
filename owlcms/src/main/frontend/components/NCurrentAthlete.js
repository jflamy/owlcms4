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
      fullName: { type: String },
      team: { type: String },
      lift: { type: String },
      snIndicators: { type: Array },
      snIndicatorClasses: { type: Array },
      cjIndicators: { type: Array },
      cjIndicatorClasses: { type: Array },
      clockValue: { type: String },
      decisions: { type: Array },

      showAthleteClock: { type: Boolean },
      showBreakClock: { type: Boolean },
      showDecisions: { type: Boolean },
      showDetails: { type: Boolean },

      logoSrc: { type: String }
    };
  }

  constructor() {
    super();
    this.fullName = "";
    this.team = "";
    this.lift = "";
    this.snIndicators = ["", "", ""];
    this.snIndicatorClasses = ["empty", "empty", "empty"];
    this.cjIndicators = ["", "", ""];
    this.cjIndicatorClasses = ["empty", "empty", "empty"];
    this.decisions = ["white", "white", "white"]; // default decisions

    this.clockValue = "";

    this.showAthleteClock = false;
    this.showBreakClock = false;
    this.showDecisions = false;
    this.showDetails = false;

    this.longTeam = 3; // max length for short team names

    this.logoSrc = "../../../../local/logos/left.png";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}"/>
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "ncurrentathlete")}"/>

      <div class="lower-third-container">
        <div class="circle-container">
          <div class="shadow-circle"></div>
          <div class="circle"></div>
          <div class="white-circle">
            <img src="${this.logoSrc}" alt="Logo" />
          </div>
        </div>
        <div class="rectangle">
          <div class="top-row spacer"></div>
          <div class="top-row top-left grid-cell" style="grid-column: 2 / span 7;">
            <div style="${this.team.length > this.longTeam ? "display: block; line-height: 1.05" : "display: flex; align-items: center;"}">
              <div class="fullName">${this.fullName}</div>
              <div class="team ${this.showDetails ? "shown" : "hidden"}" style="${this.team.length > this.longTeam ? "font-size: 85%;" : ""}">${this.team.length > this.longTeam ? this.team : ("\u00A0("+this.team+")")}</div>
            </div>
          </div>
          <div class="top-row top-right grid-cell" style="grid-column: 9 / span 2;">
            <div class="clock${this.showAthleteClock ? "" : " hidden"}">
                <timer-element id="timer"></timer-element>
            </div>
            <div class="clock${this.showBreakClock ? "" : " hidden"}">
                <timer-element id="breakTimer"></timer-element>
            </div>
            <div class="decisions ${this.showDecisions ? "shown" : "hidden"}">
              ${this.decisions.map(
                d => html`<div class="decision ${d}"></div>`
              )}
            </div>
            <decision-element id="decisions" style="display:none"></decision-element>
          </div>
          <div class="bottom-row spacer"></div>
          <div class="bottom-row bottom-left grid-cell" style="grid-column: 2 / span 3;">
            <div class="lift ${this.showDetails ? "shown" : "hidden"}" .innerHTML=${this.lift}></div>
          </div>
          <div class="bottom-row bottom-right grid-cell" style="grid-column: 5 / span 6; gap:0.3em; display:flex;">
            ${this.snIndicators.map(
              (v, i) => html`<div class="indicator ${this.snIndicatorClasses[i]} ${this.showDetails ? "shown" : "hidden"}">${v}</div>`
            )}
            <div class="indicator-spacer"></div>
            ${this.cjIndicators.map(
              (v, i) => v
                ? html`<div class="indicator ${this.cjIndicatorClasses[i]} ${this.showDetails ? "shown" : "hidden"}">${v}</div>`
                : html`<div class="indicator ${this.cjIndicatorClasses[i]} ${this.showDetails ? "shown" : "hidden"}"></div>`
            )}
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define(NCurrentAthlete.is, NCurrentAthlete);
