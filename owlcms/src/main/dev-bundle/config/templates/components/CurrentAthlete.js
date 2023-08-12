import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class CurrentAthlete extends LitElement {
  static get is() {
    return "currentathlete-template";
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
        "/" +
        (this.video ?? "") +
        "currentathlete" +
        (this.autoversion ?? "")}"
      />
      <div
        class="${"wrapper " +
        (this.teamWidthClass ?? "") +
        " " +
        (this.inactiveClass ?? "")}"
      >
        <!-- this div is SHOWN when the platform is inactive -->
        <div style="${this.inactiveGridStyle}">
          <!-- div class="competitionName">[[competitionName]]</div><br -->
          <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
        </div>

        <!-- this div is HIDDEN when the platform is inactive -->
        <div class="attemptBar" style="${this.hiddenGridStyle}">
          <div class="startNumber" id="startNumberDiv">
            <span>${this.startNumber}</span>
          </div>
          <div
            class="fullName ellipsis"
            id="fullNameDiv"
            .inner-h-t-m-l="${this.fullName}"
          >
            ${this.fullName}
          </div>
          <div class="clubName ellipsis" id="teamNameDiv">
            <div class="clubNameEllipsis">${this.teamName}</div>
          </div>
          <div class="attempt" id="attemptDiv">
            <span .inner-h-t-m-l="${this.attempt}"></span>
          </div>
          <div class="weight" id="weightDiv">
            <span
              >${this.weight}<span style="font-size: 75%"
                >&nbsp;${this.t?.KgSymbol}</span
              ></span
            >
          </div>
          <div class="timer athleteTimer" id="timerDiv">
            <timer-element id="timer"></timer-element>
          </div>
          <div class="timer breakTime" id="breakTimerDiv">
            <timer-element id="breakTimer"></timer-element>
          </div>
          <div class="decisionBox" id="decisionDiv">
            <decision-element id="decisions"></decision-element>
          </div>
          <div class="attempts">
            <table class="results" id="resultsDiv" style="${this.hiddenStyle}">
              ${(this.athletes ?? []).map(
                (item, index) => html`
                  ${!this.l?.isSpacer
                    ? html`
                        <tr>
                          <td class="category">
                            <div>${this.l?.category}</div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div .inner-h-t-m-l="${this.t?.Snatch}"></div>
                          </td>
                          ${(this.l?.sattempts ?? []).map(
                            (item, index) => html`
                              <td
                                class="${(this.attempt?.goodBadClassName ??
                                  "") +
                                " " +
                                (this.attempt?.className ?? "")}"
                              >
                                <div>${this.attempt?.stringValue}</div>
                              </td>
                            `
                          )}
                          <td class="showRank">
                            <div>
                              ${this.t?.Rank} <b>${this.l?.snatchRank}</b>
                            </div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div
                              .inner-h-t-m-l="${this.t?.Clean_and_Jerk}"
                            ></div>
                          </td>
                          ${(this.l?.cattempts ?? []).map(
                            (item, index) => html`
                              <td
                                class="${(this.attempt?.goodBadClassName ??
                                  "") +
                                " " +
                                (this.attempt?.className ?? "")}"
                              >
                                <div>${this.attempt?.stringValue}</div>
                              </td>
                            `
                          )}
                          <td class="showRank">
                            <div>
                              ${this.t?.Rank} <b>${this.l?.cleanJerkRank}</b>
                            </div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div
                              id="totalNameTd"
                              style="${this.hideInherited}"
                              .inner-h-t-m-l="${this.t?.Total}"
                            ></div>
                          </td>
                          <td class="total" style="${this.hideTableCell}">
                            <div
                              id="totalCellTd"
                              style="${(this.noneBlock ?? "") +
                              ";" +
                              (this.hideInherited ?? "")}"
                            >
                              ${this.l?.total}
                            </div>
                          </td>
                          <td class="totalRank">
                            <div
                              id="totalRankTd"
                              style="${(this.hideBlock ?? "") +
                              ";" +
                              (this.hideInherited ?? "")}"
                            >
                              ${this.t?.Rank} <b>${this.l?.totalRank}</b>
                            </div>
                          </td>
                        </tr>
                      `
                    : html``}
                `
              )}
            </table>
          </div>
        </div>
      </div>`;
  }

  firstUpdated(_changedProperties) {
    console.debug("ready");
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "grid";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#attemptDiv").style.display = "grid";
    this.renderRoot.querySelector("#weightDiv").style.display = "grid";
    this.renderRoot.querySelector("#timerDiv").style.display = "grid";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
    this.renderRoot.querySelector("#resultsDiv").style.visibility = "visible";
    // this.renderRoot.querySelector("#totalNameTd").style.display = "block";
    // this.renderRoot.querySelector("#totalCellTd").style.display = "block";
    // this.renderRoot.querySelector("#totalRankTd").style.display = "block";
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    console.debug("reset");
    this.renderRoot
      .querySelector("#timer")
      .reset(this.renderRoot.querySelector("#timer"));
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "grid";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#attemptDiv").style.display = "grid";
    this.renderRoot.querySelector("#weightDiv").style.display = "grid";
    this.renderRoot.querySelector("#timerDiv").style.display = "grid";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
    this.renderRoot.querySelector("#resultsDiv").style.visibility = "visible";
    // this.renderRoot.querySelector("#totalNameTd").style.display = "block";
    // this.renderRoot.querySelector("#totalCellTd").style.display = "block";
    // this.renderRoot.querySelector("#totalRankTd").style.display = "block";
  }

  down() {
    console.debug("refereeDecision");
    this.renderRoot.querySelector("#startNumberDiv").style.display = "grid";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#attemptDiv").style.display = "grid";
    this.renderRoot.querySelector("#weightDiv").style.display = "grid";
    this.renderRoot.querySelector("#timerDiv").style.display = "grid";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "grid";
    // this.renderRoot.querySelector("#totalNameTd").style.display = "none";
    // this.renderRoot.querySelector("#totalCellTd").style.display = "none";
    // this.renderRoot.querySelector("#totalRankTd").style.display = "none";
  }

  doBreak() {
    console.debug("break");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "none";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    this.renderRoot.querySelector("#weightDiv").style.display = "none";
    this.renderRoot.querySelector("#timerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "grid";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
    this.renderRoot.querySelector("#resultsDiv").style.visibility = "hidden";
  }

  groupDone() {
    console.debug("done");
    this.doBreak();
  }

  refereeDecision() {
    console.debug("refereeDecision");
    this.renderRoot.querySelector("#decisionDiv").style.display = "grid";
    this.renderRoot.querySelector("#weightDiv").style.display = "grid";
    this.renderRoot.querySelector("#timerDiv").style.display = "grid";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    // this.renderRoot.querySelector("#totalNameTd").style.display = "none";
    // this.renderRoot.querySelector("#totalCellTd").style.display = "none";
    // this.renderRoot.querySelector("#totalRankTd").style.display = "none";
  }

  _isEqualTo(title, string) {
    return title == string;
  }
}

customElements.define(CurrentAthlete.is, CurrentAthlete);
