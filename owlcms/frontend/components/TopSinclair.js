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
        ${this.topSinclairWomen
          ? html`
              <h2
                class="fullName"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.topSinclairWomen}"
              ></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="name" .inner-h-t-m-l="${this.t?.Name}"></th>
                    <th
                      class="category"
                      .inner-h-t-m-l="${this.t?.Category}"
                    ></th>
                    <th
                      class="veryNarrow"
                      .inner-h-t-m-l="${this.t?.Birth}"
                    ></th>
                    <th class="club" .inner-h-t-m-l="${this.t?.Team}"></th>
                    <th colspan="3" .inner-h-t-m-l="${this.t?.Snatch}"></th>
                    <th
                      colspan="3"
                      .inner-h-t-m-l="${this.t?.Clean_and_Jerk}"
                    ></th>
                    <th class="narrow" .inner-h-t-m-l="${this.t?.Total}"></th>
                    <th
                      class="medium"
                      .inner-h-t-m-l="${this.t?.BodyWeight}"
                    ></th>
                    <th
                      class="medium sinclair"
                      .inner-h-t-m-l="${this.t?.Sinclair}"
                    ></th>
                    <th class="needed" .inner-h-t-m-l="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                ${(this.sortedWomen ?? []).map(
                  (item, index) => html`
                    <tr>
                      <td class="${"name " + (this.l?.classname ?? "")}">
                        <div>${this.l?.fullName}</div>
                      </td>
                      <td class="category">${this.l?.category}</td>
                      <td class="veryNarrow">
                        <div>${this.l?.yearOfBirth}</div>
                      </td>
                      <td class="club"><div>${this.l?.teamName}</div></td>
                      ${(this.l?.sattempts ?? []).map(
                        (item, index) => html`
                          <td
                            class="${(this.attempt?.goodBadClassName ?? "") +
                            " " +
                            (this.attempt?.className ?? "")}"
                          >
                            <div>${this.attempt?.stringValue}</div>
                          </td>
                        `
                      )}
                      ${(this.l?.cattempts ?? []).map(
                        (item, index) => html`
                          <td
                            class="${(this.attempt?.goodBadClassName ?? "") +
                            " " +
                            (this.attempt?.className ?? "")}"
                          >
                            <div>${this.attempt?.stringValue}</div>
                          </td>
                        `
                      )}
                      <td class="narrow"><div>${this.l?.total}</div></td>
                      <td class="medium"><div>${this.l?.bw}</div></td>
                      <td class="medium sinclair">
                        <div>${this.l?.sinclair}</div>
                      </td>
                      <td class="needed"><div>${this.l?.needed}</div></td>
                    </tr>
                  `
                )}
              </table>
              <h2>&nbsp;</h2>
            `
          : html``}
        ${this.topSinclairMen
          ? html`
              <h2
                class="fullName"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.topSinclairMen}"
              ></h2>
              <table class="results" id="orderDiv" style$="">
                <thead>
                  <tr>
                    <th class="name" .inner-h-t-m-l="${this.t?.Name}"></th>
                    <th
                      class="category"
                      .inner-h-t-m-l="${this.t?.Category}"
                    ></th>
                    <th
                      class="veryNarrow"
                      .inner-h-t-m-l="${this.t?.Birth}"
                    ></th>
                    <th class="club" .inner-h-t-m-l="${this.t?.Team}"></th>
                    <th colspan="3" .inner-h-t-m-l="${this.t?.Snatch}"></th>
                    <th
                      colspan="3"
                      .inner-h-t-m-l="${this.t?.Clean_and_Jerk}"
                    ></th>
                    <th class="narrow" .inner-h-t-m-l="${this.t?.Total}"></th>
                    <th
                      class="medium"
                      .inner-h-t-m-l="${this.t?.BodyWeight}"
                    ></th>
                    <th
                      class="medium sinclair"
                      .inner-h-t-m-l="${this.t?.Sinclair}"
                    ></th>
                    <th class="needed" .inner-h-t-m-l="${this.t?.Needed}"></th>
                  </tr>
                </thead>
                ${(this.sortedMen ?? []).map(
                  (item, index) => html`
                    <tr>
                      <td class="${"name " + (this.l?.classname ?? "")}">
                        <div>${this.l?.fullName}</div>
                      </td>
                      <td class="category">${this.l?.category}</td>
                      <td class="veryNarrow">
                        <div>${this.l?.yearOfBirth}</div>
                      </td>
                      <td class="club"><div>${this.l?.teamName}</div></td>
                      ${(this.l?.sattempts ?? []).map(
                        (item, index) => html`
                          <td
                            class="${(this.attempt?.goodBadClassName ?? "") +
                            " " +
                            (this.attempt?.className ?? "")}"
                          >
                            <div>${this.attempt?.stringValue}</div>
                          </td>
                        `
                      )}
                      ${(this.l?.cattempts ?? []).map(
                        (item, index) => html`
                          <td
                            class="${(this.attempt?.goodBadClassName ?? "") +
                            " " +
                            (this.attempt?.className ?? "")}"
                          >
                            <div>${this.attempt?.stringValue}</div>
                          </td>
                        `
                      )}
                      <td class="narrow"><div>${this.l?.total}</div></td>
                      <td class="medium"><div>${this.l?.bw}</div></td>
                      <td class="medium sinclair">
                        <div>${this.l?.sinclair}</div>
                      </td>
                      <td class="needed"><div>${this.l?.needed}</div></td>
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

customElements.define(TopSinclair.is, TopSinclair);
