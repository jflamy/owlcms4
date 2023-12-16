import { html, LitElement, css } from "lit";
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
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "")}.css" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/top" + (this.autoversion ?? "")}.css" /> 
      <div id="resultBoardDiv" class="${this.activeClasses()}">
        ${this.topSinclairWomen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topSinclairWomen}"></h2>
              <table class="results" id="orderDiv">
                <thead>
                  <tr>
                    <th class="name" .innerHTML="${this.t?.Name}"></th>
                    <th class="category" .innerHTML="${this.t?.Category}"></th>
                    <th class="veryNarrow" .innerHTML="${this.t?.Birth}"></th>
                    <th class="club" .innerHTML="${this.t?.Team}"></th>
                    <th colspan="3" .innerHTML="${this.t?.Snatch}"></th>
                    <th colspan="3" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                    <th class="narrow" .innerHTML="${this.t?.Total}"></th>
                    <th class="medium" .innerHTML="${this.t?.BodyWeight}"></th>
                    <th class="medium sinclair" .innerHTML="${this.t?.Sinclair}"></th>
                    <th class="needed" .innerHTML="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                ${(this.sortedWomen ?? []).map(
                  (item) => html`
                    <tr>
                      <td class="name"> <div>${item.fullName}</div>
                      </td>
                      <td class="category">${item.category}</td>
                      <td class="veryNarrow"><div>${item.yearOfBirth}</div></td>
                      <td class="club"><div>${item.teamName}</div></td>
                      ${(item.sattempts ?? []).map(
                        (attempt) => html`
                          <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}">
                            <div>${attempt.stringValue}</div>
                          </td>
                        `
                      )}
                      ${(item.cattempts ?? []).map(
                        (attempt) => html`
                          <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}">
                            <div>${attempt.stringValue}</div>
                          </td>
                        `
                      )}
                      <td class="narrow"><div>${item.total}</div></td>
                      <td class="medium"><div>${item.bw}</div></td>
                      <td class="medium sinclair"><div>${item.sinclair}</div></td>
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
                    <th class="name" .innerHTML="${this.t?.Name}"></th>
                    <th class="category" .innerHTML="${this.t?.Category}"></th>
                    <th class="veryNarrow" .innerHTML="${this.t?.Birth}"></th>
                    <th class="club" .innerHTML="${this.t?.Team}"></th>
                    <th colspan="3" .innerHTML="${this.t?.Snatch}"></th>
                    <th colspan="3" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                    <th class="narrow" .innerHTML="${this.t?.Total}"></th>
                    <th class="medium" .innerHTML="${this.t?.BodyWeight}"></th>
                    <th class="medium sinclair" .innerHTML="${this.t?.Sinclair}"></th>
                    <th class="needed" .innerHTML="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                ${(this.sortedMen ?? []).map(
                  (item) => html`
                    <tr>
                      <td class="name"><div class="name">${item.fullName}</div></td>
                      <td class="category">${item.category}</td>
                      <td class="veryNarrow"><div>${item.yearOfBirth}</div></td>
                      <td class="club"><div>${item.teamName}</div></td>
                      ${(item.sattempts ?? []).map(
                        (attempt) => html`
                          <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}">
                           <div>${attempt.stringValue}</div>
                          </td>
                        `
                      )}
                      ${(item.cattempts ?? []).map(
                        (attempt) => html`
                          <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}">
                            <div>${attempt.stringValue}</div>
                          </td>
                        `
                      )}
                      <td class="narrow"><div>${item.total}</div></td>
                      <td class="medium"><div>${item.bw}</div></td>
                      <td class="medium sinclair"><div>${item.sinclair}</div></td>
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
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
  }

  activeClasses() {
    return  "wrapper "+ (this.wideTeamNames ? "wideTeams" : "narrowTeams" );
  }
  
}

customElements.define(TopSinclair.is, TopSinclair);
