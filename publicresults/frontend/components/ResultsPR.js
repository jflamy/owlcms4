import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class ResultsPR extends LitElement {
  static get is() {
    return "results-template-pr";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "results" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "resultsCustomization" + (this.autoversion ?? "") + ".css"}" />

      <div class="${this.wrapperClasses()}"  style="${this.sizeOverride}">
        <div class="blockPositioningWrapper">
          <div class="waiting" style="${this.waitingStyles()}">
            <div>
              <div class="competitionName">${this.competitionName}</div>
              <br />
              <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
            </div>
          </div>
          <div class="attemptBar" style="${this.attemptBarStyles()}">
            <div class="athleteInfo" style="${this.athleteInfoStyles()}">
              <div class="startNumber" style="${this.startNumberStyles()}"><span>${this.startNumber}</span></div>
              <div class="fullName ellipsis" style="${this.fullNameStyles()}" .innerHTML="${this.fullName}"></div>
              <div class="clubName ellipsis" style="${this.teamNameStyles()}">${this.teamName}</div>
              <div class="attempt" style="${this.attemptStyles()}"><span .innerHTML="${this.attempt}"></span></div>
              <div class="weight" style="${this.weightStyles()}">${this.weight}<span style="font-size: 75%">&hairsp;${this.t?.KgSymbol}</span></div>
              <div class="timer athleteTimer" style="${this.athleteTimerStyles()}">
                <timer-element-pr id="timer-pr"></timer-element-pr>
              </div>
              <div class="timer breakTime" style="${this.breakTimerStyles()}">
                <timer-element-pr id="breaktimer-pr"></timer-element-pr>
              </div>
              <div class="decisionBox" style="${this.decisionStyles()}">
                <decision-element-pr style="width: 100%" id="decisions-pr"></decision-element-pr>
              </div>
            </div>
          </div>
          <div class="group" style="${this.attemptBarStyles()}">
            <div id="groupDiv">
              <span class="groupName">${this.displayType}${this.groupName}</span>${this.liftsDone}
            </div>
          </div>
          <div class="video" style="${this.videoHeaderStyles()}">
            <div class="eventlogo"></div>
            <div class="videoheader">
              <div class="groupName">${this.competitionName}</div>
              <div>${this.groupDescription}</div>
            </div>
            <div class="federationlogo"></div>
          </div>

          <table class="${this.athleteClasses()}" style="${this.athleteStyles()}">
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
                    (item) => 
                      html`
                        ${item?.isSpacer 
                          ? html`
                            <tr>
                              <td class="spacer" style="grid-column: 1 / -1; justify-content: left;" innerHTML="-" ></td>
                            </tr>
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
              <td class="filler" .style="grid-column: 1 / -1; ${this.fillerStyles()}"> &nbsp; </td>
            </tr>
            ${this.leaders
              ? html`
                <tbody class="leaders" style="${this.leadersStyles()}">
                  <tr class="head">
                    <td class="leaderTitle" .innerHTML="${(this.t?.Leaders ?? "") + " " + (this.categoryName ?? "")}"></td>
                  </tr>
                  <tr>
                    <td class="headerSpacer" innerHTML="&nbsp;" style="${"grid-column: 1 / -1; justify-content: left; " + this.leadingAthleteStyles()}"></td>
                  </tr>
                  ${(this.leaders ?? []).map(
                    (item, index) => 
                      html`
                        ${!item?.isSpacer 
                          ? html`
                              <tr class="athlete">
                                <td class="groupCol" style="${this.leadingAthleteStyles()} "> <div>${item?.group}</div></td>
                                <td class="${"name " + (item?.classname ?? "")}" style="${this.leadingAthleteStyles()} "> <div class="ellipsis">   ${item?.fullName} </div></td>
                                <td class="category" style="${this.leadingAthleteStyles()} "> <div>${item?.category}</div></td>
                                <td class="yob" style="${this.leadingAthleteStyles()} "> <div>${item?.yearOfBirth}</div></td>
                                <td class="custom1" style="${this.leadingAthleteStyles()} "> <div>${item?.custom1}</div></td>
                                <td class="custom2" style="${this.leadingAthleteStyles()} "> <div>${item?.custom2}</div></td>
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
                                <td class="best" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.bestSnatch}"></div></td>
                                <td class="rank" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.snatchRank}"></div></td>
                                <td class="vspacer" style="${this.leadingAthleteStyles()} "></td>
                                ${(item?.cattempts ?? []).map(
                                  (attempt, index) => 
                                    html`
                                      <td class="${(attempt ?.goodBadClassName ?? "") + " " + (attempt?.className ?? "")}"><div>${attempt?.stringValue}</div></td>
                                    `)}
                                <td class="best" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.bestCleanJerk}"></div></td>
                                <td class="rank" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.cleanJerkRank}"></div></td>
                                <td class="vspacer"></td>
                                <td class="total" style="${this.leadingAthleteStyles()} "> <div>${item?.total}</div></td>
                                <td class="totalRank" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.totalRank}"></div></td>
                                <td class="sinclair" style="${this.leadingAthleteStyles()} "> <div>${item?.sinclair}</div></td>
                                <td class="sinclairRank" style="${this.leadingAthleteStyles()} "> <div>${item?.sinclairRank}</div></td>
                              </tr>
                          `
                          : html``}
                      `)}
                </tbody>
              `
              : html``}
          </table>
          ${this.records  
            ? html`
              <div style="${this.recordsStyles()}">
                <div class="recordsFiller">&nbsp;</div>
                <div class="recordRow" style="${(this.hiddenGridStyle ?? "") + "; --nbRecords: " + (this.records?.nbRecords ?? "")}">
                  <div>
                    <div class="recordName recordTitle">${this.t?.records}</div>
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
      decisionVisible: { type: Boolean }, // sub-mode of CURRENT_ATHLETE

      // dynamic styling
      teamWidthClass: {},
      sizeOverride: {},
      twOverride: {},
      video: {},
      showLiftRanks: {type: Boolean},
      showBest: {type: Boolean},
      showSinclair: {type: Boolean},
      showLeaders: {type: Boolean},
      showRecords: {type: Boolean},

      // translation map
      t: { type: Object },

      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},
    };
  }

  firstUpdated(_changedProperties) {
    console.debug("ready");
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  _isEqualTo(title, string) {
    return title == string;
  }

  wrapperClasses() {
    var classes = "wrapper";
    classes = classes + (this.darkMode ? " " + this.darkMode : "");
    classes = classes + (this.teamWidthClass ? " " + this.teamWidthClass : "");
    classes = classes + (this.mode === "WAIT" ? " bigTitle" : "");
    return classes;
  }

  waitingStyles() { /* originally flex */
    return "display: " + (this.mode === "WAIT" ? "grid" : "none");
  }

  attemptBarStyles() {
    return "display: " + (this.mode === "WAIT" || this.video ? "none" : "block");
  }

  athleteInfoStyles() {
    return "display: " + (this.mode === "WAIT" ? "none" : "flex");
  }

  fullNameStyles() {
    return  "display: " + (this.mode === "WAIT" ? "none" : "flex");
  }

  teamNameStyles() {
    return "display: " + ((this.isBreak()) ? "none" : "flex");
  }

  attemptStyles() {
    return "display: " + ((this.isBreak()) ? "none" : "flex");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "flex");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE")) ? "flex" : "none");
  }

  athleteTimerStyles() {
   //return "display:" + ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "flex" : "none");
   return "display: " + (this.isBreak() ? "none" : "flex");
  }

  breakTimerStyles() {
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN") ? "flex" : "none");
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "flex" : "none");
  }

  videoHeaderStyles() {
    return "display: " + ((this.mode !== "WAIT" && this.video)? "flex" : "none");
  }

  athleteClasses() {
    //return "results " +  (this.noLiftRanks ?? "") + " " + (this.noBest ?? "")
    return "results " 
      + (this.showLiftRanks ? "" : " noranks") 
      + (this.showBest ? "" : " nobest")
      + (this.showSinclair ? " sinclair" : " nosinclair");
  }

  athleteStyles() {
    return (this.mode === "WAIT" ? "display: none" : "display:grid") + 
      "; --top: " +  (this.resultLines ?? "") + 
      "; --bottom: " + (this.leaderLines ?? "") + 
      "; " + (this.leadersLineHeight ?? "") + 
      "; " + (this.twOverride ?? "");
  }

  leadersStyles() {
    return this.showLeaders ?  " display:content" : " display:none";
  }

  leadingAthleteStyles() {
    return this.showLeaders ? "" : " display:none";
  }

  fillerStyles() { // was display:flex
    return this.showLeaders && this.mode !== "WAIT" ? " display:grid" : " display:none";
  }

  recordsStyles() {
    return (!this.showRecords || this.mode !== "CURRENT_ATHLETE") 
      ? "display:none" 
      : "font-size: calc(var(--tableFontSize) * var(--recordsFontRatio)); display: block" ;
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  isCountdown() {
    return  this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN"
  }

  constructor() {
    super();
    this.mode = "WAIT";
  }
 }

customElements.define(ResultsPR.is, ResultsPR);
