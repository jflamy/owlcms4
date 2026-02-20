import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

/**
 * Rankings by Category display.
 * 
 * Uses the narrow Results.js grid styling (resultsCustomization.css)
 * with the medalCategories data structure from ResultsMedals.js
 * (categories shown as grouping headers).
 * 
 * Medal decorations are controlled by showMedals parameter:
 *  - showMedals="auto" (default): Automatically show medals when category is done (categoryDone flag)
 *  - showMedals="true": Force show medals for all categories
 *  - showMedals="false": Hide medals even when category is done
 */
class ResultsRankingsByCategory extends LitElement {
  static get is() {
    return "resultsrankings-template";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/results" + (this.autoversion ?? "") + ".css"}" />
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/resultsCustomization" + (this.autoversion ?? "") + ".css"}" />
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
              <div class="fullName ellipsis" style="${this.fullNameStyles()}" .innerHTML="${this.headerTitle()}"></div>
            </div>
          </div>
          <div class="video" style="${this.videoHeaderStyles()}">
            <div class="eventlogo"></div>
            <div class="videoheader">
              <div class="groupInfo">${this.competitionName}</div>
              <div>${this.headerTitle()}</div>
            </div>
            <div class="federationlogo"></div>
          </div>
          <!-- hidden elements required because we subclass the results page -->
          <div class="timer athleteTimer" style="display:none">
            <timer-element id="timer"></timer-element>
          </div>
          <div class="timer breakTime" style="display:none">
            <timer-element id="breakTimer"></timer-element>
          </div>
          <div class="decisionBox" style="display:none">
            <decision-element style="width: 100%" id="decisions"></decision-element>
          </div>

          ${this.medalCategories
            ? html`
                <table class="${this.athleteClasses()}" style="${this.athleteStyles()}">
                  ${(this.medalCategories ?? []).map(
                    (mc, index, array) => html`
                      ${this.isSingleCategory() ? html`` : html`
                      <tr>
                        <td class="categoryGroupHeader" style="margin-top: ${index > 0 ? '0.4em' : '0'};" .innerHTML="${mc.categoryName}"></td>
                      </tr>`}
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
                        <th class="rank" .innerHTML="${mc.rankingTitle}"></th>
                        <th class="vspacer"></th>
                        <th style="grid-column: span 3;" .innerHTML="${this.t?.Clean_and_Jerk}"></th>
                        <th class="best" .innerHTML="${this.t?.Best}"></th>
                        <th class="rank" .innerHTML="${mc.rankingTitle}"></th>
                        <th class="vspacer"></th>
                        <th class="total" .innerHTML="${this.t?.Total}"></th>
                        <th class="totalRank" .innerHTML="${mc.rankingTitle}"></th>
                        <th class="sinclair" .innerHTML="${mc.scoreScoringTitle}"></th>
                        <th class="sinclairRank" .innerHTML="${mc.scoreRankingTitle}"></th>
                      </tr>

                      ${(mc.leaders ?? []).map(
                        (item) => html`
                          <tr class="${"athlete" + (item?.classname ?? "")}">
                            <td class="${"start " + (item?.classname ?? "")}">
                              <div class="${item?.classname}">${item?.startNumber}</div>
                            </td>
                            <td class="${"name " + (item?.classname ?? "")}">
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
                                <div class="ellipsis" style="${item?.teamLength !== undefined ? "width: "+item?.teamLength : ""}">${item?.teamName}</div>
                              </div>
                            </td>
                            <td class="vspacer"></td>
                            ${(item?.sattempts ?? []).map(
                              (attempt) => html`
                                <td class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">
                                  <div class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div>
                                </td>
                              `)}
                            <td class="best">
                              <div .innerHTML="${item?.bestSnatch}"></div>
                            </td>
                            <td class="${"rank " + ((this.showMedals === "true" || (this.showMedals === "auto" && mc.categoryDone)) ? (item?.snatchMedal ?? "") : "")}">
                              <div .innerHTML="${item?.snatchRank}"></div>
                            </td>
                            <td class="vspacer"></td>
                            ${(item?.cattempts ?? []).map(
                              (attempt) => html`
                                <td class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">
                                  <div class="${(attempt?.liftStatus ?? "") + " " + (attempt?.className ?? "")}">${attempt?.stringValue}</div>
                                </td>
                              `)}
                            <td class="best">
                              <div .innerHTML="${item?.bestCleanJerk}"></div>
                            </td>
                            <td class="${"rank " + ((this.showMedals === "true" || (this.showMedals === "auto" && mc.categoryDone)) ? (item?.cleanJerkMedal ?? "") : "")}">
                              <div .innerHTML="${item?.cleanJerkRank}"></div>
                            </td>
                            <td class="vspacer"></td>
                            <td class="total">
                              <div>${item?.total}</div>
                            </td>
                            <td class="${"totalRank " + ((this.showMedals === "true" || (this.showMedals === "auto" && mc.categoryDone)) ? (item?.totalMedal ?? "") : "")}">
                              <div .innerHTML="${item?.totalRank}"></div>
                            </td>
                            <td class="sinclair">
                              <div>${item?.sinclair}</div>
                            </td>
                            <td class="${"sinclairRank " + ((this.showMedals === "true" || (this.showMedals === "auto" && mc.categoryDone)) ? (item?.sinclairMedal ?? "") : "")}">
                              <div>${item?.sinclairRank}</div>
                            </td>
                          </tr>
                        `
                      )}
                    `)}
                </table>
              `
            : html``}
            <div style="${this.bottomSpacerStyles()}">&nbsp;
              <div style="position: absolute; bottom: 0.5em; right: 1em; display: flex; align-items: center; font-weight: 100; font-size: 1.6vh;"><img src="local/logos/owlcms-logo.svg" style="height:1.25em; margin-bottom:-0.2em">&nbsp;owlcms</div>
            </div>
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
      displayTitle: {},
      platformName: {},

      // data
      medalCategories: { type: Object },

      // mode
      mode: {},
      decisionVisible: { type: Boolean },
      darkMode: {},

      // dynamic styling
      teamWidthClass: {},
      sizeOverride: {},
      twOverride: {},
      colorOverride: {},
      video: {},
      showLiftRanks: {type: Boolean},
      showBest: {type: Boolean},
      showSinclair: {type: Boolean},
      showSinclairRanks: {type: Boolean},
      showMedals: {type: String}, // "auto", "true", or "false"

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

  wrapperClasses() {
    var classes = "wrapper";
    classes = classes + (this.platformName ? " " + this.platformName : "");
    classes = classes + (this.darkMode ? " " + this.darkMode : "");
    classes = classes + (this.teamWidthClass ? " " + this.teamWidthClass : "");
    classes = classes + (this.mode === "WAIT" ? " bigTitle" : "");
    return classes;
  }

  waitingStyles() {
    return "display: " + "none";
  }

  attemptBarStyles() {
    return "display: none";
  }

  athleteInfoStyles() {
    return "display: flex";
  }

  fullNameStyles() {
    return "display: flex";
  }

  videoHeaderStyles() {
    return "display: flex";
  }

  athleteClasses() {
    return "results "
      + (this.showLiftRanks ? " ranks" : " noranks")
      + (this.showSinclair ? " sinclair" : " nosinclair")
      + (this.showSinclairRank ? " sinclairRank" : " nosinclairRank")
      ;
  }

  athleteStyles() {
    return "display:grid"
      + (this.twOverride ? "; " + this.twOverride : "");
  }

  bottomSpacerStyles() {
    return "line-height: var(--bottomSpacerHeight)";
  }

  isSingleCategory() {
    if (!Array.isArray(this.medalCategories)) {
      return false;
    }
    const nonEmptyCategories = this.medalCategories.filter(
      (mc) => Array.isArray(mc?.leaders) && mc.leaders.length > 0
    );
    return nonEmptyCategories.length === 1;
  }

  headerTitle() {
    const title = (this.displayTitle ?? "").toString().trim();
    const description = (this.groupDescription ?? "").toString().trim();
    if (title.length > 0 && description.length > 0) {
      return `${title} – ${description}`;
    }
    return description.length > 0 ? description : title;
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  constructor() {
    super();
    this.mode = "WAIT";
    // showMedals: "auto" (default, use categoryDone), "true" (force show), "false" (force hide)
    this.showMedals = "auto";
  }
}

customElements.define(ResultsRankingsByCategory.is, ResultsRankingsByCategory);
