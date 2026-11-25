import { html, LitElement } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class TopTeamsSinclair extends LitElement {
  static get is() {
    return "topteamsinclair-template";
  }

  render() {
    return html`
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}" />
    <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "top")}" />
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
        ${[
          [this.topTeamsWomen, this.womensTeams],
          [this.topTeamsMen, this.mensTeams],
          [this.topTeamsMixed, this.mixedTeams],
        ].filter(([title]) => title).map(
          ([title, teams], index) => html`
            ${index > 0 ? html`<div class="tableGap"></div>` : html``}
            <h2 class="fullName" .innerHTML="${title}"></h2>
            ${this.renderTable(teams)}
          `
        )}
      </div>`;
  }

  renderTable(teams) {
    return html`
      <div class="block">
        <table class="results" style="${this.twOverride ?? ""}">
          <thead>
            <tr class="head">
              <th class="club" .innerHTML="${this.t?.Team}"></th>
              <th class="score" .innerHTML="${this.t?.ScoringTitle}"></th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td class="spacer" colspan="2"></td>
            </tr>
            ${(teams ?? []).map(
              (item) => html`
                <tr class="team">
                    <td class="club"><div class="ellipsis">${item.team}</div></td>
                    <td class="score"><div class="ellipsis">${item.score}</div></td>
                </tr>
              `
            )}
          </tbody>
        </table>
      </div>
    `;
  }

  static get properties() {
    return {
      title: {},
      topTeamsMen: {},
      topTeamsWomen: {},
      topTeamsMixed: {},
      mensTeams: {type: Object},
      womensTeams: {type: Object},
      mixedTeams: {type: Object},
      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},
      darkMode: {},
      sizeOverride: {},
      twOverride: {},
      colorOverride: {},
      t: {type: Object},
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  activeClasses() {
    return "wrapper team-scores";
  }

}

customElements.define(TopTeamsSinclair.is, TopTeamsSinclair);
