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
    return html` 
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "" ) + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "results" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "resultsMedalsCustomization" + (this.autoversion ?? "") + ".css"}" />
      <div class="${this.wrapperClasses()}" style="${this.sizeOverride}" >
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
              <div class="fullName ellipsis" style="${this.fullNameStyles()}" .innerHTML="${this.displayTitle}"></div>
            </div>
          </div>
          <div class="video" style="${this.videoHeaderStyles()}">
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
                    (mc) => html`
                      <tr class="head" style="${this.leadersDisplay}">
                        <td style="grid-column: 1 / -1; justify-content: left; font-weight: bold; font-size: 120%" .innerHTML="${mc.categoryName}" ></td>
                      </tr>
                      <tr>
                        <td class="headerSpacer" style="grid-column: 1 / -1; justify-content: left;" inner-h-t-m-l="&nbsp;" ></td>
                      </tr>
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

                      ${(mc.leaders ?? []).map(
                        (leader) => html`
                          <tr class="athlete" style="${this.leadersDisplay}">
                            <td class="groupCol">
                              <div>${leader.group}</div>
                            </td>
                            <td class="${"name " + (leader.classname ?? "")}">
                              <div class="ellipsis">${leader.fullName}</div>
                            </td>
                            <td class="category">
                              <div>${leader.category}</div>
                            </td>
                            <td class="yob">
                              <div>${leader.yearOfBirth}</div>
                            </td>
                            <td class="custom1">
                              <div>${leader.custom1}</div>
                            </td>
                            <td class="custom2">
                              <div>${leader.custom2}</div>
                            </td>
                            <td class="${"club " + (leader.flagClass ?? "")}">
                              <div class="${leader.flagClass}" .innerHTML="${leader.flagURL}"></div>
                              <div class="ellipsis">${leader.teamName}</div>
                            </td>
                            <td class="vspacer"></td>
                            ${(leader.sattempts ?? []).map(
                              (attempt) => html`
                                <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}" >
                                  <div>${attempt.stringValue}</div>
                                </td>
                              `)}
                            <td class="best">
                              <div .innerHTML="${leader.bestSnatch}"></div>
                            </td>
                            <td class="${"rank " + (leader.snatchMedal ?? "")}">
                              <div .innerHTML="${leader.snatchRank}"></div>
                            </td>
                            <td class="vspacer"></td>
                            ${(leader.cattempts ?? []).map(
                              (attempt) => html`
                                <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}" >
                                  <div>${attempt.stringValue}</div>
                                </td>
                              `)}
                            <td class="best">
                              <div .innerHTML="${leader.bestCleanJerk}" ></div>
                            </td>
                            <td class="${"rank " + (leader.cleanJerkMedal ?? "")}">
                              <div .innerHTML="${leader.cleanJerkRank}"></div>
                            </td>
                            <td class="vspacer"></td>
                            <td class="total">
                              <div>${leader.total}</div>
                            </td>
                            <td class="${"totalRank " + (leader.totalMedal ?? "")}">
                              <div .innerHTML="${leader.totalRank}"></div>
                            </td>
                            <td class="sinclair">
                              <div>${leader.sinclair}</div>
                            </td>
                            <td class="sinclairRank">
                              <div>${leader.sinclairRank}</div>
                            </td>
                          </tr>
                        `
                      )}
                      <tr>
                        <td class="filler" style="${"grid-column: 1 / -1; line-height:100%;" + (this.fillerDisplay ?? "")}">&nbsp;</td>
                      </tr>
                    `)}
                </table>
              `
            : html``}
        </div>
      </div>`;
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
      nbRanks: {},
      ageGroups: {},
      
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
    return "display: " + "none";//(this.mode === "WAIT" ? "grid" : "none");
  }

  attemptBarStyles() {
    return  "display: " + (!this.video ? "grid" : "none");
  }

  athleteInfoStyles() {
    return "display: " + "flex";//(this.mode === "WAIT" ? "none" : "flex");
  }

  fullNameStyles() {
    return  "display: " + "flex"; (this.mode === "WAIT" ? "none" : "flex");
  }

  fullNameStyles() {
    return  "display: " + "flex"; (this.mode === "WAIT" ? "none" : "flex");
  }
  

  attemptStyles() {
    return "display: " + ((this.isBreak()) ? "none" : "flex");
  }

  videoHeaderStyles() {
    return "display: " + ((this.video)? "flex" : "none");
  }

  athleteClasses() {
    //return "results " +  (this.noLiftRanks ?? "") + " " + (this.noBest ?? "")
    return "results " 
      + (this.showLiftRanks ? "" : " noranks") 
      + (this.showBest ? "" : " nobest")
      + (this.showSinclair ? " sinclair" : " nosinclair");
  }

  athleteStyles() {
    return (this.mode === "WAIT" ? "display: none" : "display:grid") 
    + "; --top: calc(" + (this.resultLines ?? "") + " + 1)"
    + "; --bottom: " + (this.leaderLines ?? "")
    + "; --nbRanks: " + (this.nbRanks ?? "")
    + "; " + (this.leadersLineHeight ?? "")
    + "; " + (this.twOverride ?? "");
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

customElements.define(ResultsMedals.is, ResultsMedals);
