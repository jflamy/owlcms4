import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class ResultsFull extends LitElement {
  static get is() {
    return "resultsfull-template";
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
        "results" +
        (this.autoversion ?? "")}"
      />
      <link
        rel="stylesheet"
        type="text/css"
        .href="${"local/" +
        (this.stylesDir ?? "") +
        "/" +
        (this.video ?? "") +
        "resultsRanksCustomization" +
        (this.autoversion ?? "")}"
      />

      <div
        class="${"wrapper " +
        (this.teamWidthClass ?? "") +
        " " +
        (this.inactiveClass ?? "")}"
        style="${this.sizeOverride}"
      >
        <div class="blockPositioningWrapper">
          <div class="waiting" style="${this.inactiveFlexStyle}">
            <div>
              <div class="competitionName">${this.competitionName}</div>
              <br />
              <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
            </div>
          </div>
          <div class="attemptBar" style="${this.normalHeaderDisplay}">
            <div class="athleteInfo" id="athleteInfoDiv">
              <div class="startNumber" id="startNumberDiv">
                <span>${this.startNumber}</span>
              </div>
              <div
                class="fullName ellipsis"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.fullName}"
              ></div>
              <div class="clubName ellipsis" id="teamNameDiv">
                ${this.teamName}
              </div>
              <div class="attempt" id="attemptDiv">
                <span .inner-h-t-m-l="${this.attempt}"></span>
              </div>
              <div class="weight" id="weightDiv">
                ${this.weight}<span style="font-size: 75%"
                  >&hairsp;${this.t?.KgSymbol}</span
                >
              </div>
              <div class="timer athleteTimer" id="timerDiv">
                <timer-element id="timer"></timer-element>
              </div>
              <div class="timer breakTime" id="breakTimerDiv">
                <timer-element id="breakTimer"></timer-element>
              </div>
              <div class="decisionBox" id="decisionDiv">
                <decision-element
                  style="width: 100%"
                  id="decisions"
                ></decision-element>
              </div>
            </div>
          </div>
          <div class="group" style="${this.normalHeaderDisplay}">
            <div id="groupDiv">
              <span class="groupName">${this.displayType}${this.groupName}</span
              >${this.liftsDone}
            </div>
          </div>
          <div class="video" style="${this.videoHeaderDisplay}">
            <div class="eventlogo"></div>
            <div class="videoheader">
              <div class="groupName">${this.competitionName}</div>
              <div>${this.groupDescription}</div>
            </div>
            <div class="federationlogo"></div>
          </div>

          <table
            class="results"
            style="${(this.hiddenGridStyle ?? "") +
            "; --top: calc(" +
            (this.resultLines ?? "") +
            " + 1); --bottom: " +
            (this.leaderLines ?? "") +
            "; --nbRanks: " +
            (this.nbRanks ?? "") +
            "; " +
            (this.leadersLineHeight ?? "") +
            "; " +
            (this.twOverride ?? "")}"
          >
            ${this.athletes
              ? html`
                  <tr class="head">
                    <th class="groupCol" style="grid-row: span 2;">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Start}"
                      ></div>
                    </th>
                    <th class="name" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Name}"
                      ></div>
                    </th>
                    <th class="category" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Category}"
                      ></div>
                    </th>
                    <th class="yob" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Birth}"
                      ></div>
                    </th>
                    <th class="custom1" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Custom1}"
                      ></div>
                    </th>
                    <th class="custom2" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Custom2}"
                      ></div>
                    </th>
                    <th class="club" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Team}"
                      ></div>
                    </th>
                    <th class="vspacer"></th>
                    <th
                      style="${"grid-column: span calc(3 + " +
                      (this.nbRanks ?? "")}"
                      .inner-h-t-m-l="${this.t?.Snatch}"
                    ></th>

                    <th class="vspacer"></th>
                    <th
                      style="${"grid-column: span calc(3 + " +
                      (this.nbRanks ?? "")}"
                      .inner-h-t-m-l="${this.t?.Clean_and_Jerk}"
                    ></th>

                    <th class="vspacer"></th>
                    <th
                      style="${"grid-column: span calc(1 + " +
                      (this.nbRanks ?? "")}"
                      .inner-h-t-m-l="${this.t?.Total}"
                    ></th>

                    <th class="sinclair" style="grid-row: span 2">
                      <div
                        style="display: grid; align-self: center"
                        .inner-h-t-m-l="${this.t?.Sinclair}"
                      ></div>
                    </th>
                    <th
                      class="sinclairRank"
                      style="grid-row: span 2"
                      .inner-h-t-m-l="${this.t?.Rank}"
                    ></th>
                  </tr>
                  <tr class="head">
                    <th class="vspacer"></th>
                    <th class="narrow">1</th>
                    <th class="narrow">2</th>
                    <th class="narrow">3</th>
                    <th class="best" .inner-h-t-m-l="${this.t?.Best}"></th>
                    ${(this.ageGroups ?? []).map(
                      (item, index) => html` <th>${this.sag}</th> `
                    )}

                    <th class="vspacer"></th>
                    <th class="narrow">1</th>
                    <th class="narrow">2</th>
                    <th class="narrow">3</th>
                    <th class="best" .inner-h-t-m-l="${this.t?.Best}"></th>
                    ${(this.ageGroups ?? []).map(
                      (item, index) => html`
                        <th class="rank">${this.cjag}</th>
                      `
                    )}
                    <th class="vspacer"></th>
                    <th class="narrow" .inner-h-t-m-l="${this.t?.Total}"></th>
                    ${(this.ageGroups ?? []).map(
                      (item, index) => html` <th class="rank">${this.tag}</th> `
                    )}
                  </tr>
                  ${(this.athletes ?? []).map(
                    (item, index) => html`
                      ${this.l?.isSpacer
                        ? html`
                            <tr>
                              <td
                                class="spacer"
                                style="grid-column: 1 / -1; justify-content: left;"
                                inner-h-t-m-l="&nbsp;"
                              ></td>
                            </tr>
                          `
                        : html``}
                      ${!this.l?.isSpacer
                        ? html`
                            <tr class="athlete">
                              <td
                                class="${"groupCol " +
                                (this.l?.classname ?? "")}"
                              >
                                <div class="${this.l?.classname}">
                                  ${this.l?.startNumber}
                                </div>
                              </td>
                              <td
                                class="${"name " + (this.l?.classname ?? "")}"
                              >
                                <div
                                  class="${"name ellipsis " +
                                  (this.l?.classname ?? "")}"
                                >
                                  ${this.l?.fullName}
                                </div>
                              </td>
                              <td class="category">
                                <div>${this.l?.category}</div>
                              </td>
                              <td class="yob">
                                <div>${this.l?.yearOfBirth}</div>
                              </td>
                              <td class="custom1">
                                <div>${this.l?.custom1}</div>
                              </td>
                              <td class="custom2">
                                <div>${this.l?.custom2}</div>
                              </td>
                              <td
                                class="${"club " + (this.l?.flagClass ?? "")}"
                              >
                                <div
                                  class="${this.l?.flagClass}"
                                  .inner-h-t-m-l="${this.l?.flagURL}"
                                ></div>
                                <div
                                  class="ellipsis"
                                  style="${"width: " +
                                  (this.l?.teamLength ?? "")}"
                                >
                                  ${this.l?.teamName}
                                </div>
                              </td>
                              <td class="vspacer"></td>
                              ${(this.l?.sattempts ?? []).map(
                                (item, index) => html`
                                  <td
                                    class="${(this.attempt?.goodBadClassName ??
                                      "") +
                                    " " +
                                    (this.attempt?.className ?? "")}"
                                  >
                                    <div
                                      class="${(this.attempt
                                        ?.goodBadClassName ?? "") +
                                      " " +
                                      (this.attempt?.className ?? "")}"
                                    >
                                      ${this.attempt?.stringValue}
                                    </div>
                                  </td>
                                `
                              )}
                              <td class="best">
                                <div
                                  .inner-h-t-m-l="${this.l?.bestSnatch}"
                                ></div>
                              </td>
                              ${(this.l?.snatchRanks ?? []).map(
                                (item, index) => html`
                                  <td class="rank">
                                    <div .inner-h-t-m-l="${this.sr}"></div>
                                  </td>
                                `
                              )}
                              <td class="vspacer"></td>
                              ${(this.l?.cattempts ?? []).map(
                                (item, index) => html`
                                  <td
                                    class="${(this.attempt?.goodBadClassName ??
                                      "") +
                                    " " +
                                    (this.attempt?.className ?? "")}"
                                  >
                                    <div
                                      class="${(this.attempt
                                        ?.goodBadClassName ?? "") +
                                      " " +
                                      (this.attempt?.className ?? "")}"
                                    >
                                      ${this.attempt?.stringValue}
                                    </div>
                                  </td>
                                `
                              )}
                              <td class="best">
                                <div
                                  .inner-h-t-m-l="${this.l?.bestCleanJerk}"
                                ></div>
                              </td>
                              ${(this.l?.cleanJerkRanks ?? []).map(
                                (item, index) => html`
                                  <td class="rank">
                                    <div .inner-h-t-m-l="${this.cjr}"></div>
                                  </td>
                                `
                              )}
                              <td class="vspacer"></td>
                              <td class="total">${this.l?.total}</td>
                              ${(this.l?.totalRanks ?? []).map(
                                (item, index) => html`
                                  <td class="totalRank">
                                    <div .inner-h-t-m-l="${this.tr}"></div>
                                  </td>
                                `
                              )}
                              <td class="sinclair">
                                <div>${this.l?.sinclair}</div>
                              </td>
                              <td class="sinclairRank">
                                <div>${this.l?.sinclairRank}</div>
                              </td>
                            </tr>
                          `
                        : html``}
                    `
                  )}
                `
              : html``}
            <tr>
              <td
                class="filler"
                .style="${"grid-column: 1 / -1;" +
                (this.fillerVisibility ?? "")}"
              >
                &nbsp;
              </td>
            </tr>
            ${this.leaders
              ? html`
                  <tbody class="leaders" style="${this.leadersTopVisibility}">
                    <tr class="head">
                      <td
                        style="grid-column: 1 / -1; justify-content: left;"
                        .inner-h-t-m-l="${(this.t?.Leaders ?? "") +
                        " " +
                        (this.categoryName ?? "")}"
                      ></td>
                    </tr>
                    <tr>
                      <td
                        class="headerSpacer"
                        inner-h-t-m-l="&nbsp;"
                        style="${"grid-column: 1 / -1; justify-content: left; " +
                        (this.leadersVisibility ?? "")}"
                      ></td>
                    </tr>
                    ${(this.leaders ?? []).map(
                      (item, index) => html`
                        ${!this.l?.isSpacer
                          ? html`
                              <tr class="athlete">
                                <td class="groupCol">
                                  <div>${this.l?.group}</div>
                                </td>
                                <td
                                  class="${"name " + (this.l?.classname ?? "")}"
                                >
                                  <div class="ellipsis">
                                    ${this.l?.fullName}
                                  </div>
                                </td>
                                <td class="category">
                                  <div>${this.l?.category}</div>
                                </td>
                                <td
                                  class="yob"
                                  style="${this.leadersVisibility}"
                                >
                                  <div>${this.l?.yearOfBirth}</div>
                                </td>
                                <td
                                  class="custom1"
                                  style="${this.leadersVisibility}"
                                >
                                  <div>${this.l?.custom1}</div>
                                </td>
                                <td
                                  class="custom2"
                                  style="${this.leadersVisibility}"
                                >
                                  <div>${this.l?.custom2}</div>
                                </td>
                                <td
                                  class="${"club " + (this.l?.flagClass ?? "")}"
                                >
                                  <div
                                    class="${this.l?.flagClass}"
                                    .inner-h-t-m-l="${this.l?.flagURL}"
                                  ></div>
                                  <div
                                    class="ellipsis"
                                    style="${"width: " +
                                    (this.l?.teamLength ?? "")}"
                                  >
                                    ${this.l?.teamName}
                                  </div>
                                </td>
                                <td class="vspacer"></td>
                                ${(this.l?.sattempts ?? []).map(
                                  (item, index) => html`
                                    <td
                                      class="${(this.attempt
                                        ?.goodBadClassName ?? "") +
                                      " " +
                                      (this.attempt?.className ?? "")}"
                                    >
                                      <div>${this.attempt?.stringValue}</div>
                                    </td>
                                  `
                                )}
                                <td
                                  class="best"
                                  style="${this.leadersVisibility}"
                                >
                                  <div
                                    .inner-h-t-m-l="${this.l?.bestSnatch}"
                                  ></div>
                                </td>
                                ${(this.l?.snatchRanks ?? []).map(
                                  (item, index) => html`
                                    <td
                                      class="rank"
                                      style="${this.leadersVisibility}"
                                    >
                                      <div .inner-h-t-m-l="${this.sr}"></div>
                                    </td>
                                  `
                                )}
                                <td
                                  class="vspacer"
                                  style="${this.leadersVisibility}"
                                ></td>
                                ${(this.l?.cattempts ?? []).map(
                                  (item, index) => html`
                                    <td
                                      class="${(this.attempt
                                        ?.goodBadClassName ?? "") +
                                      " " +
                                      (this.attempt?.className ?? "")}"
                                    >
                                      <div>${this.attempt?.stringValue}</div>
                                    </td>
                                  `
                                )}
                                <td
                                  class="best"
                                  style="${this.leadersVisibility}"
                                >
                                  <div
                                    .inner-h-t-m-l="${this.l?.bestCleanJerk}"
                                  ></div>
                                </td>
                                ${(this.l?.cleanJerkRanks ?? []).map(
                                  (item, index) => html`
                                    <td
                                      class="rank"
                                      style="${this.leadersVisibility}"
                                    >
                                      <div .inner-h-t-m-l="${this.cjr}"></div>
                                    </td>
                                  `
                                )}
                                <td class="vspacer"></td>
                                <td
                                  class="total"
                                  style="${this.leadersVisibility}"
                                >
                                  ${this.l?.total}
                                </td>
                                ${(this.l?.totalRanks ?? []).map(
                                  (item, index) => html`
                                    <td
                                      class="totalRank"
                                      style="${this.leadersVisibility}"
                                    >
                                      <div .inner-h-t-m-l="${this.tr}"></div>
                                    </td>
                                  `
                                )}
                                <td
                                  class="sinclair"
                                  style="${this.leadersVisibility}"
                                >
                                  <div>${this.l?.sinclair}</div>
                                </td>
                                <td
                                  class="sinclairRank"
                                  style="${this.leadersVisibility}"
                                >
                                  <div>${this.l?.sinclairRank}</div>
                                </td>
                              </tr>
                            `
                          : html``}
                      `
                    )}
                  </tbody>
                `
              : html``}
          </table>
          ${this.records
            ? html`
                <div
                  style="${"font-size: calc(var(--tableFontSize) * var(--recordsFontRatio)); " +
                  (this.hiddenBlockStyle ?? "") +
                  "; " +
                  (this.recordsDisplay ?? "")}"
                >
                  <div class="recordsFiller">&nbsp;</div>

                  <div
                    class="recordRow"
                    style="${"--nbRecords: " + (this.records?.nbRecords ?? "")}"
                  >
                    <div>
                      <div class="recordName recordTitle">
                        ${this.t?.records}
                      </div>
                      <div class="recordLiftTypeSpacer">&nbsp;</div>
                      ${(this.records?.recordNames ?? []).map(
                        (item, index) => html`
                          <div class="recordName">${this.n}</div>
                        `
                      )}
                    </div>

                    ${(this.records?.recordTable ?? []).map(
                      (item, index) => html`
                        <div class="recordBox">
                          <div
                            class="recordCat"
                            .inner-h-t-m-l="${this.c?.cat}"
                          ></div>
                          <div>
                            <div class="recordLiftType">${this.t?.recordS}</div>
                            <div class="recordLiftType">
                              ${this.t?.recordCJ}
                            </div>
                            <div class="recordLiftType">${this.t?.recordT}</div>
                          </div>
                          ${(this.c?.records ?? []).map(
                            (item, index) => html`
                              <div>
                                <div
                                  class="${"recordCell " +
                                  (this.r?.snatchHighlight ?? "")}"
                                >
                                  ${this.r?.SNATCH}
                                </div>
                                <div
                                  class="${"recordCell " +
                                  (this.r?.cjHighlight ?? "")}"
                                >
                                  ${this.r?.CLEANJERK}
                                </div>
                                <div
                                  class="${"recordCell " +
                                  (this.r?.totalHighlight ?? "")}"
                                >
                                  ${this.r?.TOTAL}
                                </div>
                              </div>
                            `
                          )}
                        </div>
                      `
                    )}

                    <div
                      class="${"recordNotification " + (this.recordKind ?? "")}"
                    >
                      ${this.recordMessage}
                    </div>
                  </div>
                </div>
              `
            : html``}
        </div>
      </div>`;
  }

  firstUpdated(_changedProperties) {
    console.debug("ready");
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "flex";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#attemptDiv").style.display = "flex";
    this.renderRoot.querySelector("#weightDiv").style.display = "flex";
    this.renderRoot.querySelector("#timerDiv").style.display = "flex";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    console.debug("reset");
    //this.marqueeIfTooBig();
    this.renderRoot
      .querySelector("#timer")
      .reset(this.renderRoot.querySelector("#timer"));
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "flex";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#attemptDiv").style.display = "flex";
    this.renderRoot.querySelector("#weightDiv").style.display = "flex";
    this.renderRoot.querySelector("#timerDiv").style.display = "flex";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  down() {
    console.debug("refereeDecision");
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "flex";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#attemptDiv").style.display = "flex";
    this.renderRoot.querySelector("#weightDiv").style.display = "flex";
    this.renderRoot.querySelector("#timerDiv").style.display = "flex";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "flex";
  }

  doBreak(showWeights) {
    console.debug("break");
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "hidden";
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "none";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    if (showWeights) {
      this.renderRoot.querySelector("#weightDiv").style.display = "block";
      this.renderRoot.querySelector("#breakTimerDiv").style.display = "flex";
    } else {
      this.renderRoot.querySelector("#weightDiv").style.display = "none";
      this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    }

    this.renderRoot.querySelector("#timerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "flex";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  groupDone() {
    console.debug("done");
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "hidden";
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "none";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    this.renderRoot.querySelector("#weightDiv").style.display = "none";
    this.renderRoot.querySelector("#timerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  refereeDecision() {
    console.debug("refereeDecision");
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#decisionDiv").style.display = "flex";
    this.renderRoot.querySelector("#weightDiv").style.display = "flex";
    this.renderRoot.querySelector("#timerDiv").style.display = "flex";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
  }

  _isEqualTo(title, string) {
    return title == string;
  }

  isElementOverflowing(element) {
    var overflowX = element.offsetWidth < element.scrollWidth,
      overflowY = element.offsetHeight < element.scrollHeight;
    console.warn("overflowX " + overflowX);
    return overflowX || overflowY;
  }

  wrapContentsInMarquee(element) {
    var marquee = document.createElement("marquee"),
      contents = element.innerText;

    marquee.innerText = contents;
    element.innerHTML = "";
    element.appendChild(marquee);
  }

  marqueeIfTooBig() {
    var element = this.renderRoot.querySelector("#records");

    if (this.isElementOverflowing(element)) {
      this.wrapContentsInMarquee(element);
    }
  }
}

customElements.define(ResultsFull.is, ResultsFull);
