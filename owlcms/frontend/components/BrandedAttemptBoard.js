import { html, LitElement, css } from "lit";
import { styleMap } from 'lit/directives/style-map.js';
import { classMap } from 'lit/directives/class-map.js';

/*******************************************************************************
 * Copyright (c) 2009-2024 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class CurrentAttempt extends LitElement {
  static get is() {
    return "branded-attempt-board-template";
  }

  static styles = css`
  .wrapper {
    height: 100vh;
    background-color: green;
  }

  .attemptBoardGrid {
    display: grid;
    grid-template-rows: 15vh repeat(5,12vh) 25vh;
    grid-template-columns: 1fr repeat(5,12%) 10vh;
  }
  
  .attemptBoard {
    grid-area: 2/2/7/7;
  }

  .topLine {
    grid-area: 1/1/2/-1;
    font-size: 6vh;
    color: white;
    font-weight: 900;
    align-self: center;
    justify-self: center;
  }

  .logos {
    display: grid;
    grid-area: 2/1/7/2;
    grid-template-rows: repeat(auto-fit, 1fr);
    align-items: center;
    justify-items: center;
  }

  .sponsors {
    display: grid;
    grid-area: -1/1/8/-1;
    grid-template-columns: repeat(auto-fit, min-max(200px,1fr));
    align-items: center;
    justify-items: center;
  }
`;

  render() {
    return html` 
    <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "") + ".css"}"/>

    <div class="wrapper">
      <div class="attemptBoardGrid" style="height: 100%">
        <div class="topLine">
          Competition Name Goes Here
        </div>
        <div class="attemptBoard"  style="overflow: hidden">
          <iframe src="displays/attemptBoard"  style="width: 100%; height: 100%;"></iframe>
        </div>
        <div class="logos" style="zoom: 80%">
          <div><img src="local/logos/owlcms-sticker.svg"/></div>
          <div style="width: 30%"><img src="local/logos/blue-owl-sticker.svg" style="width: 100%"/></div>
        </div>
        <div class="sponsors">
          <div><img src="local/logos/WHC.svg" style="width: 10vw"></div>
          <div><img src="local/logos/sudamericana.png" style="width: 10vw"></div>
          <div><img src="local/logos/felp.png" style="width: 10vw"></div>
        </div>
    </div>

    </div>`;
  }

  static get properties() {
    return {
      // mode - mutually exclusive, one of:
      // WAIT INTRO_COUNTDOWN LIFT_COUNTDOWN CURRENT_ATHLETE INTERRUPTION SESSION_DONE CEREMONY
      mode: {},
      breakType: {},
      initMode: { type: Boolean },

      // style sheets & misc.
      javaComponentId: {},
      stylesDir: {},
      autoVersion: {},

      // translation map
      t: { type: Object }
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
  }

  isBreak() {
    return this.mode === "INTERRUPTION" || this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY" || this.mode === "SESSION_DONE" || this.mode === "CEREMONY"
  }

  isCountdown() {
    return this.mode === "INTRO_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN" || this.mode === "LIFT_COUNTDOWN_CEREMONY"
  }

  waitingStyles() {
    return "display: " + (this.mode === "WAIT" ? "grid" : "none");
  }

  activeStyles() {
    return "display: " + (this.mode !== "WAIT" ? "grid" : "none");
  }


  constructor() {
    super();
    this.javaComponentId = "";
    this.lastName = "";
    this.firstName = "";
    this.weight = 0;
    this.competitionName = "";

    this.mode == "WAIT";

    this.attempt = "";
    this.athleteImg = "";
    this.teamName = "";
    this.teamFlagImg = "";
    this.startNumber = 0;
    this.decisionVisible = false;
    this.recordAttempt = false;
    this.recordBroken = false;

    this.stylesDir = "";
    this.autoVersion = 0;
    this.video = "";
  }
}

customElements.define(CurrentAttempt.is, CurrentAttempt);
