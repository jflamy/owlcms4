import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class ResultsMedals extends LitElement {
  static get is() {
    return "resultsmedals-template";
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
        "resultsMedalsCustomization" +
        (this.autoversion ?? "")}"
      />

      <div
        class="${"wrapper medals " +
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
              <div
                class="fullName ellipsis"
                id="fullNameDiv"
                .inner-h-t-m-l="${this.displayTitle}"
              ></div>
            </div>
          </div>
          <div class="video" style="${this.videoHeaderDisplay}">
            <div class="eventlogo"></div>
            <div class="videoheader">
              <div class="groupName">${this.competitionName}</div>
              <div>${this.displayTitle} ${this.groupDescription}</div>
            </div>
            <div class="federationlogo"></div>
          </div>
          ${this.medalCategories
            ? html`
                <table
                  class="${"results medals " + (this.noLiftRanks ?? "")}"
                  style="${(this.hiddenGridStyle ?? "") +
                  "; padding-top: 0.5em; " +
                  (this.twOverride ?? "")}"
                >
                  ${(this.medalCategories ?? []).map(
                    (item, index) => html`
                      <tr class="head" style="${this.leadersDisplay}">
                        <td
                          style="grid-column: 1 / -1; justify-content: left; font-weight: bold; font-size: 120%"
                          .inner-h-t-m-l="${this.mc?.categoryName}"
                        ></td>
                      </tr>
                      <tr>
                        <td
                          class="headerSpacer"
                          style="grid-column: 1 / -1; justify-content: left;"
                          inner-h-t-m-l="&nbsp;"
                        ></td>
                      </tr>
                      <tr class="head" .style="${this.mc?.showCatHeader}">
                        <th
                          class="groupCol"
                          .inner-h-t-m-l="${this.t?.Group}"
                        ></th>
                        <th class="name" .inner-h-t-m-l="${this.t?.Name}"></th>
                        <th
                          class="category"
                          .inner-h-t-m-l="${this.t?.Category}"
                        ></th>
                        <th class="yob" .inner-h-t-m-l="${this.t?.Birth}"></th>
                        <th
                          class="custom1"
                          .inner-h-t-m-l="${this.t?.Custom1}"
                        ></th>
                        <th
                          class="custom2"
                          .inner-h-t-m-l="${this.t?.Custom2}"
                        ></th>
                        <th class="club" .inner-h-t-m-l="${this.t?.Team}"></th>
                        <th class="vspacer"></th>
                        <th
                          style="grid-column: span 3;"
                          .inner-h-t-m-l="${this.t?.Snatch}"
                        ></th>
                        <th class="best" .inner-h-t-m-l="${this.t?.Best}"></th>
                        <th class="rank" .inner-h-t-m-l="${this.t?.Rank}"></th>
                        <th class="vspacer"></th>
                        <th
                          style="grid-column: span 3;"
                          .inner-h-t-m-l="${this.t?.Clean_and_Jerk}"
                        ></th>
                        <th class="best" .inner-h-t-m-l="${this.t?.Best}"></th>
                        <th class="rank" .inner-h-t-m-l="${this.t?.Rank}"></th>
                        <th class="vspacer"></th>
                        <th
                          class="total"
                          .inner-h-t-m-l="${this.t?.Total}"
                        ></th>
                        <th
                          class="totalRank"
                          .inner-h-t-m-l="${this.t?.Rank}"
                        ></th>
                        <th
                          class="sinclair"
                          .inner-h-t-m-l="${this.t?.Sinclair}"
                        ></th>
                        <th
                          class="sinclairRank"
                          .inner-h-t-m-l="${this.t?.Rank}"
                        ></th>
                      </tr>

                      ${(this.mc?.leaders ?? []).map(
                        (item, index) => html`
                          <tr class="athlete" style="${this.leadersDisplay}">
                            <td class="groupCol">
                              <div>${this.l?.group}</div>
                            </td>
                            <td class="${"name " + (this.l?.classname ?? "")}">
                              <div class="ellipsis">${this.l?.fullName}</div>
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
                            <td class="${"club " + (this.l?.flagClass ?? "")}">
                              <div
                                class="${this.l?.flagClass}"
                                .inner-h-t-m-l="${this.l?.flagURL}"
                              ></div>
                              <div class="ellipsis">${this.l?.teamName}</div>
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
                                  <div>${this.attempt?.stringValue}</div>
                                </td>
                              `
                            )}
                            <td class="best">
                              <div .inner-h-t-m-l="${this.l?.bestSnatch}"></div>
                            </td>
                            <td
                              class="${"rank " + (this.l?.snatchMedal ?? "")}"
                            >
                              <div .inner-h-t-m-l="${this.l?.snatchRank}"></div>
                            </td>
                            <td class="vspacer"></td>
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
                            <td class="best">
                              <div
                                .inner-h-t-m-l="${this.l?.bestCleanJerk}"
                              ></div>
                            </td>
                            <td
                              class="${"rank " +
                              (this.l?.cleanJerkMedal ?? "")}"
                            >
                              <div
                                .inner-h-t-m-l="${this.l?.cleanJerkRank}"
                              ></div>
                            </td>
                            <td class="vspacer"></td>
                            <td class="total">
                              <div>${this.l?.total}</div>
                            </td>
                            <td
                              class="${"totalRank " +
                              (this.l?.totalMedal ?? "")}"
                            >
                              <div .inner-h-t-m-l="${this.l?.totalRank}"></div>
                            </td>
                            <td class="sinclair">
                              <div>${this.l?.sinclair}</div>
                            </td>
                            <td class="sinclairRank">
                              <div>${this.l?.sinclairRank}</div>
                            </td>
                          </tr>
                        `
                      )}
                      <tr>
                        <td
                          class="filler"
                          style="${"grid-column: 1 / -1; line-height:100%;" +
                          (this.fillerDisplay ?? "")}"
                        >
                          &nbsp;
                        </td>
                      </tr>
                    `
                  )}
                </table>
              `
            : html``}
        </div>
      </div>`;
  }

  firstUpdated(_changedProperties) {
    console.debug("ready");
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    console.debug("reset");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
  }

  down() {
    console.debug("refereeDecision");
  }

  doBreak() {
    console.debug("break");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
  }

  groupDone() {
    console.debug("done");
    this.renderRoot.querySelector("#fullNameDiv").style.visibility = "visible";
    this.renderRoot.querySelector("#fullNameDiv").style.display = "flex";
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

customElements.define(ResultsMedals.is, ResultsMedals);
