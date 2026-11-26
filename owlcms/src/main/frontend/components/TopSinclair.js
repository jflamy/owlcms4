import { html, LitElement, css } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class TopSinclair extends LitElement {
  static get is() {
    return "topsinclair-template";
  }

  render() {
    return html` 
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}" />
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "topSinclair")}" />
      <div class="notused" style="display:none">
        <timer-element id="timer"></timer-element>
        <timer-element id="breakTimer"></timer-element>
        <decision-element id="decisions"></decision-element>
        <timer-element id="decisionSectionTimer"></timer-element>
        <timer-element id="decisionSectionBreakTimer"></timer-element>
        <timer-element id="decisionSectionStopwatch"></timer-element>
        <decision-element id="decisionSectionReferee"></decision-element>
      </div> 
      <div id="resultBoardDiv" class="${this.activeClasses()} ${this.darkMode??"dark"}"
          style="${this.sizeOverride ?? ""} ${this.colorOverride ?? ""}">
        ${this.topSinclairWomen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topSinclairWomen}"></h2>
              ${this.renderTable(this.sortedWomen)}
              <div class="tableGap"></div>
            `
          : html``}
        ${this.topSinclairMen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topSinclairMen}"></h2>
              ${this.renderTable(this.sortedMen)}
            `
          : html``}
      </div>`;
  }

  renderTable(athletes) {
    const showNeeded = this.showNeeded !== false;
    const nbColumns = 5 + 1 + (this.displayLifts ? 4 : 1) + 1 + (this.displayLifts ? 4 : 1) + 1 + 2 + (showNeeded ? 1 : 0);
    return html`
      <div class="block">
        <table class="results" id="orderDiv" style="${this.twOverride ?? ""}">
          <thead>
            <tr class="head">
              <th class="name" .innerHTML="${this.t?.Name}"></th>
              <th class="club" .innerHTML="${this.t?.Team}"></th>
              <th class="age" .innerHTML="${this.t?.Age}"></th>
              <th class="category" .innerHTML="${this.t?.Category}"></th>
              <th class="bodyWeight" .innerHTML="${this.t?.BodyWeight}"></th>
              <th class="vspacer"></th>
              ${this.displayLifts
                ? html`<th class="lifts" colspan="3" .innerHTML="${this.t?.Snatch}"></th>
                       <th class="best" .innerHTML="${this.t?.Best}"></th>`
                : html`<th class="best liftOnly" .innerHTML="${this.t?.Snatch}"></th>`}
              <th class="vspacer"></th>
              ${this.displayLifts
                ? html`<th class="lifts" colspan="3" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                       <th class="best" .innerHTML="${this.t?.Best}"></th>`
                : html`<th class="best liftOnly" .innerHTML="${this.t?.Clean_and_Jerk}"></th>`}
              <th class="vspacer"></th>
              <th class="${this.displayLifts ? "total" : "total liftOnly"}" .innerHTML="${this.t?.Total}"></th>
              <th class="score" .innerHTML="${this.t?.ScoringTitle}"></th>
              ${showNeeded ? html`<th class="needed" .innerHTML="${this.neededTitle || this.t?.Needed}"></th>` : html``}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="spacer" colspan="${nbColumns}"></td>
            </tr>
            ${(athletes ?? []).map(
              (item) => html`
                <tr class="athlete">
                  <td class="name"><div class="ellipsis">${item.fullName}</div></td>
                  <td class="club"><div class="ellipsis">${item.teamName}</div></td>
                  <td class="age"><div>${item.age}</div></td>
                  <td class="category"><div>${item.category}</div></td>
                  <td class="bodyWeight"><div>${item.bw}</div></td>
                  <td class="vspacer"></td>
                  ${this.displayLifts ? this.renderAttempts(item.sattempts) : html``}
                  <td class="best"><div>${item.bestSnatch ?? ""}</div></td>
                  <td class="vspacer"></td>
                  ${this.displayLifts ? this.renderAttempts(item.cattempts) : html``}
                  <td class="best"><div>${item.bestCleanJerk ?? ""}</div></td>
                  <td class="vspacer"></td>
                  <td class="total"><div>${item.total}</div></td>
                  <td class="score"><div>${item.sinclair}</div></td>
                  ${showNeeded ? html`<td class="needed"><div>${item.needed}</div></td>` : html``}
                </tr>
              `
            )}
          </tbody>
        </table>
      </div>
    `;
  }

  renderAttempts(attempts) {
    return [0, 1, 2].map((i) => {
      const attempt = (attempts && attempts[i]) || {};
      return html`<td class="${(attempt.liftStatus ?? "") + " " + (attempt.className ?? "")}"><div>${attempt.stringValue ?? ""}</div></td>`;
    });
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  static get properties() {
    return {
      title: {},
      topSinclairMen: {},
      topSinclairWomen: {},
      sortedMen: {type: Object},
      sortedWomen: {type: Object},
      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},
      t: {type: Object},
      wideTeamNames: {},
      // dynamic styling
      darkMode: {},
      displayLifts: {type: Boolean},
      showNeeded: {type: Boolean},
      neededTitle: {},
      sizeOverride: {},
      twOverride: {},
      colorOverride: {},
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  activeClasses() {
    return "wrapper athlete-scores " + (this.wideTeamNames ? "wideTeams" : "narrowTeams");
  }
}

customElements.define(TopSinclair.is, TopSinclair);
