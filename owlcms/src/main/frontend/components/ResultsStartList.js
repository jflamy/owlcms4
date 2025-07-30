import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class ResultsStartList extends LitElement {
  static get is() {
    return "startlist-template";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/results" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/startListCustomization" + (this.autoversion ?? "") + ".css"}" />

      <div class="${this.wrapperClasses()}" style="${this.sizeOverride} ${this.colorOverride}">
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
                <timer-element id="timer"></timer-element>
              </div>
              <div class="timer breakTime" style="${this.breakTimerStyles()}">
                <timer-element id="breakTimer"></timer-element>
              </div>
              <div class="decisionBox" style="${this.decisionStyles()}">
                <decision-element style="width: 100%" id="decisions"></decision-element>
              </div>
            </div>
          </div>
          <div class="group" style="${this.attemptBarStyles()}">
            <div id="groupDiv">
              <span class="groupInfo">${this.displayType}${this.groupInfo}</span>${this.liftsDone}
            </div>
          </div>
          <div class="video" style="${this.videoHeaderStyles()}">
            <div class="eventlogo"></div>
            <div class="videoheader">
              <div class="groupInfo">${this.competitionName}</div>
              <div>${this.t?.StartList} – ${this.groupDescription}</div>
            </div>
            <div class="federationlogo"></div>
          </div>

          <table class="${this.athleteClasses()} startList" style="${this.athleteStyles()}">
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
                  <th class="entry" .innerHTML="${this.t?.EntryTotal}"></th>
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
                            <tr class="${"athlete" + (item?.classname ?? "")}">
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
                                <div class="clubName">
                                  <div class="ellipsis" style="${"width: " + (item?.teamLength ?? "")}">${item?.teamName}</div>
                                </div>
                              </td>
                              <td class="entry">
                                <div>${item?.entryTotal}</div>
                              </td>
                            </tr>
                          `}
                  `)}
              `
              : html``}
          </table>
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
      platformName: {},
      scoreboardType: {},

      // during lifting
      athletes: { type: Object },
      leaders: { type: Object },
      records: { type: Object },

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},
      decisionVisible: { type: Boolean }, // sub-mode of CURRENT_ATHLETE

      // dynamic styling
      darkMode: {},
      teamWidthClass: {},
      sizeOverride: {},
      twOverride: {},
	    colorOverride: {},
      video: {},
      showLiftRanks: {type: Boolean},
      showBest: {type: Boolean},
      showSinclair: {type: Boolean},
      showSinclairRanks: {type: Boolean},
      showLeaders: {type: Boolean},
      showRecords: {type: Boolean},
      logoSrc: {},

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
    classes = classes + (this.platformName ? " " + this.platformName : "");
    classes = classes + (this.darkMode ? " " + this.darkMode : "");
    classes = classes + (this.teamWidthClass ? " " + this.teamWidthClass : "");
    classes = classes + (this.mode === "WAIT" ? " bigTitle" : "");
    classes = classes + (this.scoreboardType ? " " + this.scoreboardType : "");
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
    return  "display: " + (this.mode === "WAIT" ? "none" : "block");
  }

  teamNameStyles() {
    return "display: " + (this.isBreak() ? "none" : "block");
  }

  attemptStyles() {
    return "display: " + (this.isBreak() ? "none" : "flex");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "flex");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || (this.mode === "CURRENT_ATHLETE")) ? "flex" : "none");
  }

  athleteTimerStyles() {
   let visible = ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "display" : "hidden");
   return "visibility: " + (this.isBreak() ? "hidden" : visible);
  }

  breakTimerStyles() {
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY") ? "flex" : "none");
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "flex" : "none");
  }

  videoHeaderStyles() {
    return "display: " + ((this.mode !== "WAIT" && this.video)? "flex" : "none");
  }

  bottomSpacerStyles() {
    return "line-height: var(--bottomSpacerHeight)";
  }

  athleteClasses() {
    var classes = "results "
    + (this.showTotal ? " total" : " nototal")
    + (this.showLiftRanks ? " ranks" : " noranks")
    + (this.showBest ? " best" : " nobest")
    + (this.showTotalRank ? " totalRank" : " nototalRank")
    + (this.showSinclair ? " sinclair" : " nosinclair")
    + (this.showSinclairRank ? " sinclairRank" : " nosinclairRank")
    ;
    //console.log("athleteClasses = "+classes);
    return classes;
}

  athleteStyles() {
    return (this.mode === "WAIT" ? "display: none" : "display:grid ")
      + (this.resultLines ? ("; --top: " + this.resultLines) : "")
      + (this.leaderLines ? "; --bottom: " + this.leaderLines : "")
      + (this.leadersLineHeight ? "; " + this.leadersLineHeight : "")
      + (this.leaderFillerHeight ? "; " + this.leaderFillerHeight : "")
      + (this.twOverride ? "; " + this.twOverride : "")
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
      : "font-size: var(--recordsFontRatio); display: block" ;
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  isCountdown() {
    return  this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY"
  }

  constructor() {
    super();
    this.mode = "WAIT";
  }
 }

customElements.define(ResultsStartList.is, ResultsStartList);
