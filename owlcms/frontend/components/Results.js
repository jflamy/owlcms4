import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class Results extends LitElement {
  static get is() {
    return "results-template";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "results" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "resultsCustomization" + (this.autoversion ?? "") + ".css"}" />

      <div class="${"wrapper " +  (this.teamWidthClass ?? "") + " " + (this.inactiveClass ?? "")}"  style="${this.sizeOverride}">
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
              <div class="startNumber" id="startNumberDiv"><span>${this.startNumber}</span></div>
              <div class="fullName ellipsis" id="fullNameDiv" .innerHTML="${this.fullName}"></div>
              <div class="clubName ellipsis" id="teamNameDiv">${this.teamName}</div>
              <div class="attempt" id="attemptDiv"><span .innerHTML="${this.attempt}"></span></div>
              <div class="weight" id="weightDiv">${this.weight}<span style="font-size: 75%">&hairsp;${this.t?.KgSymbol}</span></div>
              <div class="timer athleteTimer" id="timerDiv">
                <timer-element id="timer"></timer-element>
              </div>
              <div class="timer breakTime" id="breakTimerDiv">
                <timer-element id="breakTimer"></timer-element>
              </div>
              <div class="decisionBox" id="decisionDiv">
                <decision-element style="width: 100%" id="decisions"></decision-element>
              </div>
            </div>
          </div>
          <div class="group" style="${this.normalHeaderDisplay}">
            <div id="groupDiv">
              <span class="groupName">${this.displayType}${this.groupName}</span>${this.liftsDone}
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

          <table class="${"results " +  (this.noLiftRanks ?? "") + " " + (this.noBest ?? "")}" style="${(this.hiddenGridStyle ?? "") + "; --top: " +  (this.resultLines ?? "") + "; --bottom: " + (this.leaderLines ?? "") + "; " + (this.leadersLineHeight ?? "") + "; " + (this.twOverride ?? "")}">
            ${this.athletes 
              ? html`
                <tr class="head">
                  <th class="groupCol" .innerHTML="${this.t?.Start}"></th>
                  <th class="name" .innerHTML="${this.t?.Name}"></th>
                  <th class="category" .innerHTML="${this.t?.Category}"></th>
                  <th class="yob" .innerHTML="${this.t?.Birth}"></th>
                  <th class="custom1" .innerHTML="${this.t?.Custom1}"></th>
                  <th class="custom2" .innerHTML="${this.t?.Custom2}"></th>
                  <th class="club" .innerHTML="${this.t?.Team}"></th>
                  <th class="vspacer"></th>
                  <th style="grid-column: span 3;" .innerHTML="${this.t?.Snatch}"></th>
                  <th class="best" .innerHTML="${this.t?.Best}"></th>
                  <th class="rank" .innerHTML="${this.t?.Rank}"></th>
                  <th class="vspacer"></th>
                  <th style="grid-column: span 3;" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                  <th class="best" .innerHTML="${this.t?.Best}"></th>
                  <th class="rank" .innerHTML="${this.t?.Rank}"></th>
                  <th class="vspacer"></th>
                  <th class="total" .innerHTML="${this.t?.Total}"></th>
                  <th class="totalRank" .innerHTML="${this.t?.Rank}"></th>
                  <th class="sinclair"  .innerHTML="${this.t?.Sinclair}"></th>
                  <th class="sinclairRank" .innerHTML="${this.t?.Rank}"></th>
                </tr>
                ${(this.athletes ?? []).map(
                    (item, index) => 
                      html`
                        ${item?.isSpacer 
                          ? html`
                            <tr>
                              <td class="spacer" style="grid-column: 1 / -1; justify-content: left;" innerHTML="-" ></td> </tr>
                          `
                          : html`
                            <tr class="athlete">
                              <td class="${"start " + (item?.classname ?? "")}">
                                <div class="${item?.classname}"> ${item?.startNumber}</div>
                              </td>
                              <td class="${"name " + (item.classname ?? "")}">
                                <div class="${"name ellipsis " + (item?.classname ?? "")}">${item?.fullName}</div>
                              </td>
                              <td class="category">
                                <div>${item?.category}</div>
                              </td>
                              <td class="yob">
                                <div>${item?.yearOfBirth}</div>
                              </td>
                              <td class="custom1">
                                <div>${item?.custom1}</div>
                              </td>
                              <td class="custom2">
                                <div>${item?.custom2}</div>
                              </td>
                              <td class="${"club " + (item?.flagClass ?? "")}">
                                <div class="${item?.flagClass}" .innerHTML="${item?.flagURL} "></div>
                                <div class="ellipsis" style="${"width: " + (item?.teamLength ?? "")}"> ${item?.teamName}
                                </div>
                              </td>
                              <td class="vspacer"></td>
                              ${(item?.sattempts ?? []).map(
                                (attempt, index) => 
                                  html`
                                    <td class="${(attempt?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}">   
                                      <div class="${(attempt?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div>
                                    </td>
                                  `)}
                              <td class="best">
                                <div .innerHTML="${item?.bestSnatch} "></div>
                              </td>
                              <td class="rank">
                                <div .innerHTML="${item?.snatchRank} "></div>
                              </td>
                              <td class="vspacer"></td>
                              ${(item?.cattempts ?? []).map(
                                (attempt, index) => 
                                  html`
                                    <td class="${(attempt?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}">
                                      <div class="${(attempt?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div> 
                                    </td>
                                  `)}
                              <td class="best">
                                <div .innerHTML="${item?.bestCleanJerk}"></div>
                              </td>
                              <td class="rank">
                                <div .innerHTML="${item?.cleanJerkRank}"></div>
                              </td>
                              <td class="vspacer"></td>
                              <td class="total">
                                <div>${item?.total}</div>
                              </td>
                              <td class="totalRank">
                                <div .innerHTML="${item?.totalRank}"></div>
                              </td>
                              <td class="sinclair">
                                <div>${item?.sinclair}</div>
                              </td>
                              <td class="sinclairRank">
                                <div>${item?.sinclairRank}</div>
                              </td>
                            </tr>
                          `}
                  `)}
              `
              : html``}
            <tr>
              <td class="filler" .style="${"grid-column: 1 / -1; " + (this.fillerVisibility ?? "")}"> &nbsp; </td>
            </tr>
            ${this.leaders
              ? html`
                <tbody class="leaders" style="${this.leadersTopVisibility}">
                  <tr class="head">
                    <td class="leaderTitle" .innerHTML="${(this.t?.Leaders ?? "") + " " + (this.categoryName ?? "")}"></td>
                  </tr>
                  <tr>
                    <td class="headerSpacer" innerHTML="&nbsp;" style="${"grid-column: 1 / -1; justify-content: left; " + (this.leadersVisibility ?? "")}"></td>
                  </tr>
                  ${(this.leaders ?? []).map(
                    (item, index) => 
                      html`
                        ${!item?.isSpacer 
                          ? html`
                              <tr class="athlete">
                                <td class="groupCol" style="${this.leadersVisibility} "> <div>${item?.group}</div></td>
                                <td class="${"name " + (item?.classname ?? "")}" style="${this.leadersVisibility} "> <div class="ellipsis">   ${item?.fullName} </div></td>
                                <td class="category" style="${this.leadersVisibility} "> <div>${item?.category}</div></td>
                                <td class="yob" style="${this.leadersVisibility} "> <div>${item?.yearOfBirth}</div></td>
                                <td class="custom1" style="${this.leadersVisibility} "> <div>${item?.custom1}</div></td>
                                <td class="custom2" style="${this.leadersVisibility} "> <div>${item?.custom2}</div></td>
                                <td class="${"club " + (item?.flagClass ?? "")} ">
                                  <div class="${item?.flagClass}" .innerHTML="${item?.flagURL}"></div>
                                  <div class="ellipsis"   style="${"width: " + (item?.teamLength ?? "")}">   ${item?.teamName} </div>
                                </td>
                                <td class="vspacer"></td>
                                ${(item?.sattempts ?? []).map(
                                  (attempt, index) => 
                                    html`
                                      <td class="${(attempt ?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}"><div>${attempt?.stringValue}</div></td>
                                    `)}
                                <td class="best" style="${this.leadersVisibility} "> <div .innerHTML="${item?.bestSnatch}"></div></td>
                                <td class="rank" style="${this.leadersVisibility} "> <div .innerHTML="${item?.snatchRank}"></div></td>
                                <td class="vspacer" style="${this.leadersVisibility} "></td>
                                ${(item?.cattempts ?? []).map(
                                  (attempt, index) => 
                                    html`
                                      <td class="${(attempt ?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}"><div>${attempt?.stringValue}</div></td>
                                    `)}
                                <td class="best" style="${this.leadersVisibility} "> <div .innerHTML="${item?.bestCleanJerk}"></div></td>
                                <td class="rank" style="${this.leadersVisibility} "> <div .innerHTML="${item?.cleanJerkRank}"></div></td>
                                <td class="vspacer"></td>
                                <td class="total" style="${this.leadersVisibility} "> <div>${item?.total}</div></td>
                                <td class="totalRank" style="${this.leadersVisibility} "> <div .innerHTML="${item?.totalRank}"></div></td>
                                <td class="sinclair" style="${this.leadersVisibility} "> <div>${item?.sinclair}</div></td>
                                <td class="sinclairRank" style="${this.leadersVisibility} "> <div>${item?.sinclairRank}</div></td>
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
              <div style="${"font-size: calc(var(--tableFontSize) * var(--recordsFontRatio)); " +  (this.hiddenBlockStyle ?? "") + "; " + (this.recordsDisplay ?? "")}">
                <div class="recordsFiller">&nbsp;</div>
                <div class="recordRow" style="${(this.hiddenGridStyle ?? "") + "; --nbRecords: " + (this.records?.nbRecords ?? "")}">
                  <div>
                    <div class="recordName recordTitle">${this.t?.records}
                    </div>
                    <div class="recordLiftTypeSpacer">&nbsp;</div>
                    ${(this.records?.recordNames ?? []).map(
                      (n, index) => 
                        html`
                          <div class="recordName">${n}</div>
                        `)}
                  </div>

                  ${(this.records?.recordTable ?? []).map(
                    (c, index) => 
                      html`
                        <div class="${c?.recordClass}">
                          <div class="recordCat" .innerHTML="${c?.cat}"></div>
                          <div>
                            <div class="recordLiftType">${this.t?.recordS}</div>
                            <div class="recordLiftType">${this.t?.recordCJ}</div>
                            <div class="recordLiftType">${this.t?.recordT}</div>
                          </div>
                          ${(c?.records ?? []).map(
                            (r, index) => 
                              html`
                                <div>
                                  <div class="${"recordCell " + (r?.snatchHighlight ?? "")} ">${r?.SNATCH}</div>
                                  <div class="${"recordCell " + (r?.cjHighlight ?? "")} ">${r?.CLEANJERK}</div>
                                  <div class="${"recordCell " + (r?.totalHighlight ?? "")} ">${r?.TOTAL}</div>
                                </div>
                            `)}
                        </div>
                      `)}
                  <div class="${"recordNotification " + (this.recordKind ?? "")}"> ${this.recordMessage} </div>
                </div>
              </div>
            `
            : html``}
        </div>
      </div>
    `;
  }

  static get properties() {
    return {
      competitionName: {},
      // shared
      startNumber: {},
      fullName: {},
      teamName: {},
      attempt: {},
      weight: {},
      decisionVisible: { type: Boolean },
      displayType: {},
      groupName: {},
      groupDescription: {},
      
      // during lifting
      athletes: { type: Object },
      leaders: { type: Object },
      records: { type: Object },

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},

      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
      video: {},

      // translation map
      t: { type: Object }
    };
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
    //this.renderRoot.querySelector("#groupDiv").style.visibility = "visible";
    var s;
    (s = this.renderRoot.querySelector("#fullNameDiv")) && (s.style.visibility = "visible");
    (s = this.renderRoot.querySelector("#fullNameDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#startNumberDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#teamNameDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#attemptDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#weightDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#timerDiv")) && (s.style.display = "flex");
    (s = this.renderRoot.querySelector("#breakTimerDiv")) && (s.style.display = "none");
    (s = this.renderRoot.querySelector("#decisionDiv")) && (s.style.display = "none");
  }

  down() {
    console.debug("down");
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

customElements.define(Results.is, Results);
