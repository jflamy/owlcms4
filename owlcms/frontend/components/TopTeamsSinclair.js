import { html, LitElement, css } from "lit";
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
    return html` <link
        rel="stylesheet"
        type="text/css"
        .href="${"local/" +
        (this.stylesDir ?? "") +
        "/" +
        (this.video ?? "") +
        "colors" +
        (this.autoversion ?? "")}"
      />
      <link
        rel="stylesheet"
        type="text/css"
        .href="${"local/" +
        (this.stylesDir ?? "") +
        "/top" +
        (this.autoversion ?? "")}"
      />
      <div
        id="resultBoardDiv"
        class="${"wrapper " +
        (this._computeTeamWidth ?? "")(this.wideTeamNames ?? "")}"
      >
        ${this.topTeamsWomen
          ? html`
              <h2
                class="fullName"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.topTeamsWomen}"
              ></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="club" .inner-h-t-m-l="${this.t?.Team}"></th>

                    <th
                      class="medium"
                      .inner-h-t-m-l="${this.t?.Sinclair}"
                    ></th>
                  </tr>
                </thead>
                ${(this.womensTeams ?? []).map(
                  (item, index) => html`
                    <tr>
                      <td class="club"><div>${this.l?.team}</div></td>

                      <td class="medium"><div>${this.l?.score}</div></td>
                    </tr>
                  `
                )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
        ${this.topTeamsMen
          ? html`
              <h2
                class="fullName"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.topTeamsMen}"
              ></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="club" .inner-h-t-m-l="${this.t?.Team}"></th>

                    <th
                      class="medium"
                      .inner-h-t-m-l="${this.t?.Sinclair}"
                    ></th>
                  </tr>
                </thead>
                ${(this.mensTeams ?? []).map(
                  (item, index) => html`
                    <tr>
                      <td class="club"><div>${this.l?.team}</div></td>

                      <td class="medium"><div>${this.l?.score}</div></td>
                    </tr>
                  `
                )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
      </div>`;
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "block";
  }

  start() {
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "block";
  }

  reset() {
    console.debug("reset");
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "block";
  }

  down() {
    console.debug("down");
  }

  doBreak() {
    console.debug("break");
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "block";
  }

  groupDone() {
    console.debug("done");
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "block";
  }

  refereeDecision() {
    console.debug("refereeDecision");
  }

  _isEqualTo(title, string) {
    return title == string;
  }

  clear() {
    this.renderRoot.querySelector("#resultBoardDiv").style.display = "none";
  }

  _computeTeamWidth(w) {
    return w ? "wideTeams" : "narrowTeams";
  }
}

customElements.define(TopTeamsSinclair.is, TopTeamsSinclair);
