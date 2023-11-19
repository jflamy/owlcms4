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
    return html` 
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "colors" + (this.autoversion ?? "")}.css"/>
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/" + (this.video ?? "") + "currentathlete" + (this.autoversion ?? "")}.css"/>
     
      <div class="${this.wrapperClasses()}">
        <div class="waiting" style="${this.waitingStyles()}">
          <!-- div class="competitionName">[[competitionName]]</div><br -->
          <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
        </div>

        <div class="attemptBar" style="${this.attemptBarStyles()}">
          <div class="startNumber" style="${this.startNumberStyles()}"><span>${this.startNumber}</span> </div>
          <div class="fullName ellipsis" style="${this.fullNameStyles()}" .innerHTML="${this.fullName}"></div>
          <div class="clubName ellipsis" style="${this.teamNameStyles()}"><div class="clubNameEllipsis">${this.teamName}</div></div>
          <div class="attempt" style="${this.attemptStyles()}"><span .innerHTML="${this.attempt}"></span></div>
          <div class="weight" style="${this.weightStyles()}">
            <span >${this.weight}<span style="font-size: 75%" >&nbsp;${this.t?.KgSymbol}</span></span>
          </div>
          <div class="timer athleteTimer" style="${this.athleteTimerStyles()}">
            <timer-element id="timer"></timer-element>
          </div>
          <div class="timer breakTime" style="${this.breakTimerStyles()}">
            <timer-element id="breakTimer"></timer-element>
          </div>
          <div class="decisionBox" style="${this.decisionStyles()}">
            <decision-element id="decisions" style="padding:1ex"></decision-element>
          </div>
          <div class="attempts" style="${this.attemptStyles()}">
            <table class="results" id="resultsDiv">
              ${(this.athletes ?? []).map(
                (item) => html`
                  ${!item.isSpacer
                    ? html`
                        <tr>
                          <td class="category">
                            <div>${item.category}</div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div .innerHTML="${this.t?.Snatch}"></div>
                          </td>
                          ${(item.sattempts ?? []).map(
                            (attempt) => html`
                              <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}" >
                                <div>${attempt.stringValue}</div>
                              </td>
                            `)}
                          <td class="showRank">
                            <div>
                              ${this.t?.Rank} <b>${item.snatchRank}</b>
                            </div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div
                              .innerHTML="${this.t?.Clean_and_Jerk}"
                            ></div>
                          </td>
                          ${(item.cattempts ?? []).map(
                            (attempt) => html`
                              <td class="${(attempt.goodBadClassName ?? "") + " " + (attempt.className ?? "")}">
                                <div>${attempt.stringValue}</div>
                              </td>
                            `)}
                          <td class="showRank">
                            <div>
                              ${this.t?.Rank} <b>${item.cleanJerkRank}</b>
                            </div>
                          </td>
                          <td class="spacer">&nbsp;</td>
                          <td class="liftName">
                            <div id="totalNameTd" style="${this.decisionHiddenStyles()}" .innerHTML="${this.t?.Total}"></div>
                          </td>
                          <td class="total" style="${this.decisionHiddenStyles()}">
                            <div id="totalCellTd" style="${this.decisionHiddenStyles()}">${item.total}</div>
                          </td>
                          <td class="totalRank">
                            <div id="totalRankTd" style="${this.decisionHiddenStyles()}">${this.t?.Rank} <b>${item.totalRank}</b> </div>
                          </td>
                        </tr>
                      `
                    : html``}
                `)}
            </table>
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

      // mode (mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},
      decisionVisible: { type: Boolean }, // sub-mode of CURRENT_ATHLETE

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
    classes = classes + (this.darkMode ? " " + this.darkMode : "");
    classes = classes + (this.teamWidthClass ? " " + this.teamWidthClass : "");
    classes = classes + ((this.mode === "WAIT") ? " bigTitle" : "");
    return classes;
  }

  waitingStyles() { /* originally flex */
    return "display: " + ((this.mode === "WAIT")  ? "grid" : "none");
  }

  attemptBarStyles() {
    return "display: " + ((this.mode === "WAIT") ? "none" : "grid");
  }

  fullNameStyles() {
    return  "display: " + ((this.mode === "WAIT") ? "none" : "grid");
  }

  teamNameStyles() {
    return "display: " + ((this.isBreak()) ? "none" : "grid");
  }

  attemptStyles() {
    return "display: grid; visibility: " + ((this.isBreak()) ? "; visibility: hidden" : "");
  }

  startNumberStyles() {
    return "display: " + (this.isBreak() ? "none" : "grid");
  }

  weightStyles() {
    // weights are visible during lift countdowns
    return "display: " + ((this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY" || (this.mode === "CURRENT_ATHLETE")) ? "grid" : "none");
  }

  athleteTimerStyles() {
   //return "display:" + ((this.mode === "CURRENT_ATHLETE" && !this.decisionVisible) ? "flex" : "none");
   return "display: " + (this.isBreak() ? "none" : "grid");
  }

  breakTimerStyles() {
    return "display:" + ((this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY") ? "grid" : "none");
  }

  decisionStyles() {
    return "display: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "grid" : "none");
  }

  decisionHiddenStyles() {
    return "visibility: " + ((this.mode === "CURRENT_ATHLETE" && this.decisionVisible) ? "hidden" : "");
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "CEREMONY" || this.mode === "SESSION_DONE"
  }

  isCountdown() {
    return  this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN"
  }

  constructor() {
    super();
    this.mode = "WAIT";
  }

  firstUpdated(_changedProperties) {
    console.debug("ready");
    super.firstUpdated(_changedProperties);
    document.body.setAttribute("theme", "dark");
  }

}

customElements.define(CurrentAthlete.is, CurrentAthlete);
