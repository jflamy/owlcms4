import { html, LitElement, css } from "lit";

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

  static styles = css`
    :host {
      display: block;
      --circle-size: 9.72em;
      --circle-half: 4.86em;
      --circle-overlap: 2.43em;
      --white-circle-size: 8.262em;
    }
    body {
      margin: 0;
      padding: 0;
      height: 100vh;
      background-color: #00b140;
      font-family: Arial, sans-serif;
      position: relative;
      font-size: 1rem;
    }
    .lower-third-container {
      position: absolute;
      bottom: 5vh;
      left: 50%;
      transform: translateX(-50%);
      width: 60em;
      height: 7.2em;
      display: flex;
      align-items: center;
    }
    .circle-container {
      position: relative;
      width: var(--circle-size);
      height: var(--circle-size);
      flex-shrink: 0;
      margin-right: calc(-1 * var(--circle-half));
      align-self: center;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .shadow-circle {
      width: var(--circle-size);
      height: var(--circle-size);
      background-color: white;
      border-radius: 50%;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 1;
    }
    .circle {
      width: var(--circle-size);
      height: var(--circle-size);
      background-color: #6a1b9a;
      opacity: 0.5;
      border-radius: 50%;
      position: absolute;
      top: 0;
      left: 0;
      z-index: 2;
    }
    .white-circle {
      width: var(--white-circle-size);
      height: var(--white-circle-size);
      background-color: white;
      border-radius: 50%;
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      z-index: 3;
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .rectangle {
      height: 100%;
      border-radius: 0 0.5em 0.5em 0;
      margin-left: 0;
      flex: 1;
      display: grid;
      grid-template-rows: 1fr 1fr;
      grid-template-columns: var(--circle-half) repeat(9, 1fr);
      padding-left: 0;
      color: white;
      font-size: 1.35em;
      font-weight: bold;
      background: none;
      overflow: hidden;
    }
    .top-row {
      display: contents;
      background: #6a1b9a;
    }
    .bottom-row {
      display: contents;
      background: #181828;
    }
    .top-left,
    .bottom-left,
    .bottom-right,
    .top-right {
      display: flex;
      align-items: center;
      padding-right: 1em;
    }
    .top-left,
    .bottom-left {
      grid-column: 2;
    }
    .top-right,
    .bottom-right {
      grid-column: 3;
    }
    .spacer {
      grid-column: 1;
      min-width: var(--circle-half);
      padding: 0;
      margin: 0;
      display: block;
    }
    .indicator-spacer {
      min-width: 1em;
      height: auto;
      background: transparent;
      display: inline-block;
    }
    .indicator {
      border-radius: 0.3em;
      margin: 0.1em 0.2em;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1em;
      font-weight: bold;
      min-height: 1em;
      min-width: 3em;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      padding-top: 0.3em;
      padding-bottom: 0.3em;
      max-height: 100%;
      box-sizing: border-box;
    }
    .indicator.white {
      background-color: white;
      color: #1e3c72;
    }
    .indicator.red {
      background-color: #e74c3c;
      color: white;
    }
    .lift.hidden {
      visibility: hidden;}
    .indicator.hidden {
      visibility: hidden;
    }
    .indicator.empty {
      visibility: hidden;
    }
    .indicator.current {
      background: #ffd700;
      border: none;
      color: #000;
      font-weight: bold;
    }
    .indicator-spacer {
      min-width: 1em;
      height: auto;
      background: transparent;
      display: inline-block;
    }
    .rank-number {
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1em;
      font-weight: bold;
      color: white;
    }
    .fullName {
      font-size: 1.2em;
      font-weight: bold;
    }
    .lift {
      font-size: 1em;
      opacity: 0.9;
      line-height: 1.0;
    }
    .clock {
      color: #fff;
      background: none;
      border-radius: 0.3em;
      min-width: 3.5em;
      text-align: left;
      align-self: left;
      font-family: 'Consolas', 'Courier New', monospace;
      font-size: 1.4em;
      font-weight: bold;
      letter-spacing: 0.05em;
    }
    .team.hidden {
      display: none !important;
    }
    .clock.hidden {
      display: none !important;
    }
    .decisions.hidden {
      display: none !important;
    }
    .decisions.shown {
      display: flex !important;
      align-self: center;
      justify-content: center;
      gap: 0.3em;
    }
    .decision {
      width: 1.4em;
      height: 1.4em;
      border-radius: 50%;
      background: none;
      border: 2px solid #222;
      display: inline-block;
    }
    .decision.true {
      background: #fff;
      border-color: #bbb;
    }
    .decision.false {
      background: #e74c3c;
      border-color: #b71c1c;
    }
    .top-row.top-right.grid-cell {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      justify-content: center;
      gap: 0.7em;
      height: 100%;
    }
  `;

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
      <div class="lower-third-container">
        <div class="circle-container">
          <div class="shadow-circle"></div>
          <div class="circle"></div>
          <div class="white-circle">
            <img src="${this.logoSrc}" alt="Logo"
              style="position: absolute; left: 50%; top: 50%; width: 58.5%; height: 58.5%; transform: translate(-50%, -50%); object-fit: contain;" />
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
