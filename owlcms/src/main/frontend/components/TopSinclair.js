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
      </div> 
      <div id="resultBoardDiv" class="${this.activeClasses()} ${this.darkMode??"dark"}">
        ${this.topSinclairWomen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topSinclairWomen}"></h2>
              <table class="results" id="orderDiv">
                <thead>
                  <tr>
                    <th class="name veryWide" .innerHTML="${this.t?.Name}"></th>
                    <th class="club wide" .innerHTML="${this.t?.Team}"></th>
                    <th class="veryNarrow age narrow" .innerHTML="${this.t?.Age}"></th>
                    <th class="category narrow" .innerHTML="${this.t?.Category}"></th>
                    <th class="narrow" .innerHTML="${this.t?.BodyWeight}"></th>
                      <th colspan="3" class="${this.displayLifts ? 'showLifts' : 'hideLifts'}" .innerHTML="${this.t?.Snatch}"></th>
                      <th class="best" .innerHTML="${this.t?.Snatch}"></th>
                      <th colspan="3" class="${this.displayLifts ? 'showLifts' : 'hideLifts'}" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                      <th class="best" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                    <th class="narrow" .innerHTML="${this.t?.Total}"></th>
                    <th class="sinclair" .innerHTML="${this.t?.ScoringTitle}"></th>
                    <th class="needed" .innerHTML="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                  ${(this.sortedWomen ?? []).map(
                    (item) => html`
                      <tr>
                            <td class="name veryWide"> <div>${item.fullName}</div></td>
                            <td class="club wide"><div>${item.teamName}</div></td>
                            <td class="veryNarrow age narrow"><div>${item.age}</div></td>
                            <td class="category narrow">${item.category}</td>
                            <td class="narrow"><div>${item.bw}</div></td>
                        <!-- Always render 3 snatch attempts -->
                        ${[0,1,2].map(i => {
                          const attempt = (item.sattempts && item.sattempts[i]) || {};
                          return html`<td class="${this.displayLifts ? 'showLifts' : 'hideLifts'} ${(attempt.liftStatus ?? '') + ' ' + (attempt.className ?? '')}"><div>${attempt.stringValue ?? ''}</div></td>`;
                        })}
                        <td class="best"><div>${item.bestSnatch ?? ''}</div></td>
                        <!-- Always render 3 clean & jerk attempts -->
                        ${[0,1,2].map(i => {
                          const attempt = (item.cattempts && item.cattempts[i]) || {};
                          return html`<td class="${this.displayLifts ? 'showLifts' : 'hideLifts'} ${(attempt.liftStatus ?? '') + ' ' + (attempt.className ?? '')}"><div>${attempt.stringValue ?? ''}</div></td>`;
                        })}
                        <td class="best"><div>${item.bestCleanJerk ?? ''}</div></td>
                        <td class="narrow"><div>${item.total}</div></td>
                        <td class="sinclair"><div>${item.sinclair}</div></td>
                        <td class="needed"><div>${item.needed}</div></td>
                      </tr>
                    `
                  )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
        ${this.topSinclairMen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topSinclairMen}"></h2>
              <table class="results" id="orderDiv">
                <thead>
                  <tr>
                    <th class="name veryWide" .innerHTML="${this.t?.Name}"></th>
                    <th class="club wide" .innerHTML="${this.t?.Team}"></th>
                    <th class="veryNarrow age narrow" .innerHTML="${this.t?.Age}"></th>
                    <th class="category narrow" .innerHTML="${this.t?.Category}"></th>
                    <th class="narrow" .innerHTML="${this.t?.BodyWeight}"></th>
                      <th colspan="3" class="${this.displayLifts ? 'showLifts' : 'hideLifts'}" .innerHTML="${this.t?.Snatch}"></th>
                      <th class="best" .innerHTML="${this.t?.Snatch}"></th>
                      <th colspan="3" class="${this.displayLifts ? 'showLifts' : 'hideLifts'}" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                      <th class="best" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                    <th class="narrow" .innerHTML="${this.t?.Total}"></th>
                    <th class="sinclair" .innerHTML="${this.t?.ScoringTitle}"></th>
                    <th class="needed" .innerHTML="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                  ${(this.sortedMen ?? []).map(
                    (item) => html`
                      <tr>
                        <td class="name veryWide"><div class="name">${item.fullName}</div></td>
                        <td class="club wide"><div>${item.teamName}</div></td>
                        <td class="veryNarrow age narrow"><div>${item.age}</div></td>
                        <td class="category narrow">${item.category}</td>
                        <td class="narrow"><div>${item.bw}</div></td>
                        <!-- Always render 3 snatch attempts -->
                        ${[0,1,2].map(i => {
                          const attempt = (item.sattempts && item.sattempts[i]) || {};
                          return html`<td class="${this.displayLifts ? 'showLifts' : 'hideLifts'} ${(attempt.liftStatus ?? '') + ' ' + (attempt.className ?? '')}"><div>${attempt.stringValue ?? ''}</div></td>`;
                        })}
                        <td class="best"><div>${item.bestSnatch ?? ''}</div></td>
                        <!-- Always render 3 clean & jerk attempts -->
                        ${[0,1,2].map(i => {
                          const attempt = (item.cattempts && item.cattempts[i]) || {};
                          return html`<td class="${this.displayLifts ? 'showLifts' : 'hideLifts'} ${(attempt.liftStatus ?? '') + ' ' + (attempt.className ?? '')}"><div>${attempt.stringValue ?? ''}</div></td>`;
                        })}
                        <td class="best"><div>${item.bestCleanJerk ?? ''}</div></td>
                        <td class="narrow"><div>${item.total}</div></td>
                        <td class="sinclair"><div>${item.sinclair}</div></td>
                        <td class="needed"><div>${item.needed}</div></td>
                      </tr>
                    `
                  )}
              </table>
            `
          : html``}
      </div>`;
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
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
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
  }

  activeClasses() {
    return  "wrapper "+ (this.wideTeamNames ? "wideTeams" : "narrowTeams" );
  }
  showLiftsClass() {
    return this.displayLifts ? "showLifts" : "hideLifts";
  }
}

customElements.define(TopSinclair.is, TopSinclair);
