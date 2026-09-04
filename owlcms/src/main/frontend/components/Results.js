import { html, LitElement, css } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";
import { renderResultsAthleteRow } from "./renderResultsAthleteRow.js";
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
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "colors")}" />
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "results")}" />
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, "resultsCustomization")}" />

      <div class="${this.wrapperClasses()}" style="${this.sizeOverride} ${this.colorOverride}">
        <div class="blockPositioningWrapper">
          <div class="decisionSection" style="${this.decisionSectionStyles()}">
            <div class="dsTimerSlot" style="${this.dsTimerSlotStyles()}">
              <div class="timer athleteTimer" style="${this.dsAthleteTimerStyles()}">
                <timer-element id="decisionSectionTimer"></timer-element>
              </div>
              <div class="timer breakTime" style="${this.dsBreakTimerStyles()}">
                <timer-element id="decisionSectionBreakTimer"></timer-element>
              </div>
              <div class="timer breakTime dsStopwatch" style="${this.dsStopwatchStyles()}">
                <timer-element id="decisionSectionStopwatch"></timer-element>
              </div>
            </div>
            <div class="dsDecisionAthlete name" style="${this.dsDecisionAthleteStyles()}">
              <span class="dsDecisionStartNumber" style="${this.dsDecisionStartNumberStyles()}">${this.decisionSectionStartNumber}</span>
              <span class="dsDecisionAthleteName ellipsis">${this.decisionSectionName()}<span style="${this.decisionSectionAgeGroupsStyles()}"> (${this.decisionSectionAgeGroups})</span></span>
            </div>
            <div class="dsProjectedRanksSlot ${this.dsProjectedRanksMode()}" style="${this.dsProjectedRanksStyles()}">${this.projectedRankText}</div>
            <div class="dsDecisions" style="${this.dsDecisionsStyles()}">
              <div class="dsRefereeSlot" style="${this.dsRefereeSlotStyles()}">
                <decision-element id="decisionSectionReferee"></decision-element>
              </div>
              <div class="dsJuryMessage" style="${this.dsJuryMessageStyles()}">${this.juryMessage}</div>
              <div class="dsJurySlot" style="${this.dsJurySlotStyles()}">
                ${(this.juryDecisions ?? []).map(d => html`<vaadin-icon class="juryIcon ${d}" icon="${this.juryIcon(d)}"></vaadin-icon>`)}
              </div>
            </div>
            <div class="dsBranding" style="${this.dsBrandingStyles()}"><img class="brandingLogo" src="local/logos/owlcms-logo.svg">&nbsp;owlcms</div>
          </div>
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
                  <th class="sinclair"  .innerHTML="${this.t?.ScoringTitle}"></th>
                  <th class="sinclairRank" .innerHTML="${this.t?.Rank}"></th>
                </tr>
                ${(this.athletes ?? []).map(
                    (item, index, athletes) =>
                      html`
                        ${item?.isSpacer
                          ? this.renderSpacerRow(item, index, athletes)
                          : renderResultsAthleteRow(item)}
                  `)}
              `
              : html``}
            ${this.leaders
              ? html`
                <tbody class="leaders" style="${this.leadersStyles()}">
                  <tr>
                    <td class="filler" style="grid-column: 1 / -1; ${this.fillerStyles()}"> &nbsp; </td>
                  </tr>
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
                                <td class="groupCol" style="${this.leadingAthleteStyles()} "> <div>${item?.subCategory}</div></td>
                                <td class="${"name " + (item?.classname ?? "")}" style="${this.leadingAthleteStyles()} "> <div class="ellipsis">   ${item?.fullName} </div></td>
                                <td class="category" style="${this.leadingAthleteStyles()} "> <div>${item?.category}</div></td>
                                <td class="yob" style="${this.leadingAthleteStyles()} "> <div>${item?.yearOfBirth}</div></td>
                                <td class="custom1" style="${this.leadingAthleteStyles()} "> <div>${item?.custom1}</div></td>
                                <td class="custom2" style="${this.leadingAthleteStyles()} "> <div>${item?.custom2}</div></td>
                                <td class="${"club " + (item?.flagClass ?? "")} ">
                                  <div class="${item?.flagClass}" .innerHTML="${item?.flagURL}"></div>
                                  <div class="clubName">
                                    <div class="ellipsis" style="${"width: " + (item?.teamLength ?? "")}">${item?.teamName}</div>
                                  </div>
                                </td>
                                <td class="vspacer"></td>
                                ${(item?.sattempts ?? []).map(
                                  (attempt, index) =>
                                    html`
                                      <td class="${(attempt ?.liftStatus ?? "") + " " + (attempt?.className ?? "")}"><div>${attempt?.stringValue}</div></td>
                                    `)}
                                <td class="best" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.bestSnatch}"></div></td>
                                <td class="${"rank " + (item?.snatchMedal ?? "")}" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.snatchRank}"></div></td>
                                <td class="vspacer" style="${this.leadingAthleteStyles()} "></td>
                                ${(item?.cattempts ?? []).map(
                                  (attempt, index) =>
                                    html`
                                      <td class="${(attempt ?.liftStatus ?? "") + " " + (attempt?.className ?? "")}"><div>${attempt?.stringValue}</div></td>
                                    `)}
                                <td class="best" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.bestCleanJerk}"></div></td>
                                <td class="${"rank " + (item?.cleanJerkMedal ?? "")}" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.cleanJerkRank}"></div></td>
                                <td class="vspacer sinclairVspacer"></td>
                                <td class="total" style="${this.leadingAthleteStyles()} "> <div>${item?.total}</div></td>
                                <td class="${"totalRank " + (item?.totalMedal ?? "")}" style="${this.leadingAthleteStyles()} "> <div .innerHTML="${item?.totalRank}"></div></td>
                                <td class="sinclair" style="${this.leadingAthleteStyles()} "> <div>${item?.sinclair}</div></td>
                                <td class="${"sinclairRank " + (item?.sinclairMedal ?? "")}" style="${this.leadingAthleteStyles()} "> <div>${item?.sinclairRank}</div></td>
                              </tr>
                          `
                          : html``}
                      `)}
                </tbody>
              `
              : html``}
          </table>
          ${this.records && this.showRecords
            ? html`
              <div style="${this.recordsStyles()}">
                <div class="recordsFiller">&nbsp;</div>
                <div class="recordRow" style="${(this.hiddenGridStyle ?? "") + "; --nbRecords: " + (this.records?.nbRecords ?? "")}">
                  <div class="recordTitleBlock">
                    <div class="recordName recordTitle">${this.t?.records}</div>
                    <div class="recordLiftTypeSpacer"><span class="recordLiftTypeSpacer">&nbsp;</span></div>
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
                          <div class="recordLiftType"><span class="recordLiftType">${this.t?.recordS}</span></div>
                          <div class="recordLiftType"><span class="recordLiftType">${this.t?.recordCJ}</span></div>
                          <div class="recordLiftType"><span class="recordLiftType">${this.t?.recordT}</span></div>
                          ${(c?.records ?? []).map(
                            (r, index) =>
                              html`
                                <div class="${"recordCell " + (r?.snatchHighlight ?? "")} ">${r?.SNATCH}</div>
                                <div class="${"recordCell " + (r?.cjHighlight ?? "")} ">${r?.CLEANJERK}</div>
                                <div class="${"recordCell " + (r?.totalHighlight ?? "")} ">${r?.TOTAL}</div>
                              `)}
                        </div>
                      `)}
                  <div class="${"recordNotification " + (this.recordKind ?? "")}"> ${this.recordMessage} </div>
                  <div class="branding" style="position: absolute; bottom: 2em; right: 2em; display: flex; align-items: center; font-weight: 100; font-size: 1.6vh;"><img src="local/logos/owlcms-logo.svg" style="height:1.25em; margin-bottom:-0.2em">&nbsp;owlcms</div>
                </div>
              </div>
            `
            : html`<div style="${this.bottomSpacerStyles()}">&nbsp;
              <div class="branding" style="position: absolute; bottom: 0.5em; right: 2em; align-items: center; font-weight: 100; font-size: 1.6vh; line-height: 1.25em"><img src="local/logos/owlcms-logo.svg" style="height:1.25em; margin-bottom:-0.2em">&nbsp;owlcms</div>
            </div>
            `}
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
      medalCeremony: {type: Boolean},
      hideBreakTimer: {type: Boolean},

      // dynamic styling
      darkMode: {},
      teamWidthClass: {},
      sizeOverride: {},
      twOverride: {},
      colorOverride: {},
      video: {},
      currentAttempt: {},
      showCategoryHeaders: {type: Boolean},
      showLiftRanks: {type: Boolean},
      showBest: {type: Boolean},
      showSinclair: {type: Boolean},
      showSinclairRanks: {type: Boolean},
      showCustom1: {type: Boolean},
      showLeaders: {type: Boolean},
      showRecords: {type: Boolean},
      showDecisionSection: {type: Boolean},
      showProjectedRanks: {type: Boolean},
      showScoreboardTimers: {type: Boolean},
      decisionSectionDecisionActive: {type: Boolean},
      decisionSectionCurrentActive: {type: Boolean},
      decisionSectionStartNumber: {},
      decisionSectionAthleteName: {},
      decisionSectionAgeGroups: {},
      decisionSectionBreakText: {},
      projectedRankText: {},
      juryDecisions: {type: Array},
      decisionSectionHideJuryLights: {type: Boolean},
      decisionSectionHideRefereeLights: {type: Boolean},
      juryMessage: {},
      dsShowStopwatch: {type: Boolean},
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
    classes = classes + (this.showDecisionSection && this.mode !== "WAIT" && !(this.scoreboardType ?? "").includes("Jury") ? " dsActive" : "");
    return classes;
  }

  waitingStyles() { /* originally flex */
    return "display: " + (this.mode === "WAIT" ? "grid" : "none");
  }

  attemptBarStyles() {
    const showAttempt = this.currentAttempt === true || this.currentAttempt === "true";
    return "display: " + (this.mode === "WAIT" || this.video || !showAttempt ? "none" : "block");
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
  //  let visible = ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "display" : "hidden");
  //  return "visibility: " + (this.isBreak() ? "hidden" : visible);
   let visible = ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "flex" : "none");
   return "display: " + (this.isBreak() ? "none" : visible);
  }

  breakTimerStyles() {
    if (this.isBreak() && this.hideBreakTimer) return "display:none";
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY") ? "flex" : "none");
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "flex" : "none");
  }

  decisionSectionStyles() {
    const juryScoreboard = (this.scoreboardType ?? "").includes("Jury");
    if (this.mode === "WAIT" || juryScoreboard) return "display:none";
    if (this.showDecisionSection) return "display:flex";
    if (this.showScoreboardTimers) return "display:flex";
    if (this.showProjectedRanks && this.projectedRankText) return "display:flex";
    return "display:none";
  }

  dsTimerSlotStyles() {
    if (this.isBreak() && this.hideBreakTimer) return "display:none";
    if (this.showDecisionSection && this.decisionSectionDecisionActive) return "display:none";
    return (this.showDecisionSection || this.showScoreboardTimers) ? "" : "display:none";
  }

  dsBrandingStyles() {
    return this.medalCeremony ? "margin-inline-start:auto" : "";
  }

  dsDecisionAthleteStyles() {
    if (!this.showDecisionSection) return "display:none";
    if (this.decisionSectionDecisionActive) return "display: flex";
    const current = this.decisionSectionCurrentActive && this.mode === "CURRENT_ATHLETE";
    return "display: " + (current || this.dsBreakDescriptionVisible() ? "flex" : "none");
  }

  dsBreakDescriptionVisible() {
    return this.isBreak() && Boolean(this.decisionSectionBreakText);
  }

  dsDecisionStartNumberStyles() {
    return "display: " + (this.dsBreakDescriptionVisible() ? "none" : "");
  }

  decisionSectionName() {
    return this.dsBreakDescriptionVisible() ? this.decisionSectionBreakText : this.decisionSectionAthleteName;
  }

  decisionSectionAgeGroupsStyles() {
    return "display: " + (!this.dsBreakDescriptionVisible() && this.decisionSectionAgeGroups ? "inline" : "none");
  }

  dsRefereeSlotStyles() {
    return this.showDecisionSection && !this.decisionSectionHideRefereeLights ? "" : "display:none";
  }

  dsDecisionsStyles() {
    return this.medalCeremony ? "display:none" : "";
  }

  dsJurySlotStyles() {
    return this.showDecisionSection && !this.decisionSectionHideJuryLights ? "" : "display:none";
  }

  dsProjectedRanksStyles() {
    if (!this.showProjectedRanks || !this.projectedRankText) return "display:none";
    if (this.mode !== "CURRENT_ATHLETE" || this.decisionVisible) return "display:none";
    return "";  // CSS class (pjOverlay or pjInline) handles layout
  }

  dsProjectedRanksMode() {
    if (!this.showProjectedRanks || !this.projectedRankText) return "";
    if (this.mode !== "CURRENT_ATHLETE" || this.decisionVisible) return "";
    // pjOverlay: absolute, fills full decisionSection (clock floats above)
    // pjInline:  flex item, fills remaining space to right of lights
    return this.showDecisionSection ? "pjInline" : "pjOverlay";
  }

  juryIcon(decision) {
    switch (decision) {
      case "voted":
        return "vaadin:circle";
      case "white":
        return "vaadin:check-circle";
      case "red":
        return "vaadin:close-circle";
      default:
        return "vaadin:circle-thin";
    }
  }

  dsAthleteTimerStyles() {
    if (!this.showDecisionSection && !this.showScoreboardTimers) return "display:none";
    // In the decision section the timer stays visible for the whole current-athlete
    // phase (including while a decision is shown), alongside referee and jury lights.
    let visible = ((this.mode === "CURRENT_ATHLETE") ? "flex" : "none");
    return "display: " + (this.isBreak() ? "none" : visible);
  }

  dsBreakTimerStyles() {
    if (!this.showDecisionSection && !this.showScoreboardTimers) return "display:none";
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY") ? "flex" : "none");
  }

  dsStopwatchStyles() {
    // Count up only while a jury deliberation / challenge is actually in progress.
    // Hide it the moment the break ends (mode leaves INTERRUPTION), exactly like the
    // break bar hides the Challenge notification. White text for now.
    const visible = this.stopwatchVisible();
    return "display: " + (visible ? "flex" : "none");
  }

  stopwatchVisible() {
    return Boolean(this.dsShowStopwatch) && Boolean(this.juryMessage) && this.mode === "INTERRUPTION";
  }

  dsJuryMessageStyles() {
    return "display: " + (this.showDecisionSection && this.juryMessage ? "block" : "none");
  }

  videoHeaderStyles() {
    const showAttempt = this.currentAttempt === true || this.currentAttempt === "true";
    return "display: " + ((this.mode !== "WAIT" && (this.video || !showAttempt))? "flex" : "none");
  }

  bottomSpacerStyles() {
    return "line-height: var(--bottomSpacerHeight)";
  }

  hasMultipleCategorySections(athletes = this.athletes ?? []) {
    return athletes.filter((item) => item?.isSpacer).length > 1;
  }

  renderSpacerRow(_item, index, athletes) {
    if (this.showCategoryHeaders && (this.hasMultipleCategorySections(athletes) || this.hasAttribute("always-category-header"))) {
      const categoryTitle = athletes[index + 1]?.category ?? "";
      if (categoryTitle) {
        return html`
          <tr>
            <td class="categoryGroupHeader" style="margin-top: ${index > 0 ? "0.4em" : "var(--firstCategorySpacerHeight, 0)"};">${categoryTitle}</td>
          </tr>
        `;
      }
    }

    return html`
      <tr>
        <td class="spacer" style="grid-column: 1 / -1; justify-content: left;">&nbsp;</td>
      </tr>
    `;
  }

  athleteClasses() {
    var classes = "results "
    + (this.showTotal ? " total" : " nototal")
    + (this.showLiftRanks ? " ranks" : " noranks")
    + (this.showBest ? " best" : " nobest")
    + (this.showTotalRank ? " totalRank" : " nototalRank")
    + (this.showCustom1 ? " custom1" : " nocustom1")
    + (this.showSinclair ? " sinclair" : " nosinclair")
    + (this.showSinclairRank ? " sinclairRank" : " nosinclairRank")
    ;
    //console.log("athleteClasses = "+classes);
    return classes;
}

  athleteStyles() {
    return (this.mode === "WAIT" ? "display: none" : "display:grid ")
      + (this.resultLines ? ("; --top: calc(" + this.resultLines + ")") : "") // FIXME suspicious + 1 removed
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
    return (this.showLeaders && this.mode !== "WAIT") ? " display:grid" : " display:none";
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
    this.medalCeremony = false;
    this.hideBreakTimer = false;
    this.showCategoryHeaders = false;
    this.showDecisionSection = false;
    this.showProjectedRanks = false;
    this.showScoreboardTimers = false;
    this.decisionSectionDecisionActive = false;
    this.decisionSectionCurrentActive = false;
    this.decisionSectionStartNumber = "";
    this.decisionSectionAthleteName = "";
    this.decisionSectionAgeGroups = "";
    this.decisionSectionBreakText = "";
    this.projectedRankText = "";
    this.juryDecisions = [];
    this.decisionSectionHideJuryLights = false;
    this.decisionSectionHideRefereeLights = false;
    this.juryMessage = "";
    this.dsShowStopwatch = false;
  }
 }

customElements.define(Results.is, Results);
