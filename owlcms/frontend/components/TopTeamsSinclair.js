import { html, LitElement } from "lit";
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
     <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "")}.css" />
     <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/top" + (this.autoversion ?? "")}.css" />
     <div id="resultBoardDiv" class="${this.activeClasses()}">
        ${this.topTeamsWomen
          ? html`
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topTeamsWomen}" ></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="club" .innerHTML="${this.t?.Team}"></th>
                    <th class="medium" .innerHTML="${this.t?.Sinclair}" ></th>
                  </tr>
                </thead>
                ${(this.womensTeams ?? []).map(
                  (item) => html`
                    <tr>
                      <td class="club"><div>${item.team}</div></td>
                      <td class="medium"><div>${item.score}</div></td>
                    </tr>
                  `
                )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
        ${this.topTeamsMen 
          ? html` 
              <h2 class="fullName" id="fullNameDiv" .innerHTML="${this.topTeamsMen}"></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="club" .innerHTML="${this.t?.Team}"></th>
                    <th class="medium" .innerHTML="${this.t?.Sinclair}"></th>
                  </tr>
                </thead>
                ${(this.mensTeams ?? []).map(
                  (item) => html`
                    <tr>
                      <td class="club"><div>${item.team}</div></td>
                      <td class="medium"><div>${item.score}</div></td>
                    </tr>
                  `
                )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
      </div>`;
  }

  static get properties() {
    return {
      title: {},
      topTeamsMen: {},
      topTeamsWomen: {},
      mensTeams: {type: Object},
      womensTeams: {type: Object},
      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},
      t: {type: Object},
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
  }

  activeClasses() {
    return "wrapper ";
  }

}

customElements.define(TopTeamsSinclair.is, TopTeamsSinclair);
