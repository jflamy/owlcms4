import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class DecisionBoard extends LitElement {
  static get is() {
    return "decision-board-template";
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
        "decisionboard" +
        (this.autoversion ?? "")}"
      />
      <div class="${"wrapper " + (this.inactiveClass ?? "")}">
        <div style="${this.inactiveBlockStyle}">
          <div class="competitionName">${this.competitionName}</div>
          <br />
          <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
        </div>
        <div
          class="decisionBoard"
          id="decisionBoardDiv"
          style="${this.activeGridStyle}"
        >
          <div class="barbell" id="barbellDiv">
            <slot name="barbell"></slot>
          </div>
          <div class="timer athleteTimer" id="athleteTimerDiv">
            <timer-element id="athleteTimer"></timer-element>
          </div>
          <div class="timer breakTime" id="breakTimerDiv">
            <timer-element id="breakTimer"></timer-element>
          </div>
          <div class="decision" id="decisionDiv" @down="${this.down}">
            <decision-element id="decisions"></decision-element>
          </div>
        </div>
      </div>`;
  }

  static get properties() {
    return {
      weight: {
        type: Number,
      },
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    console.log("decision board ready.");
    this.doBreak();
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    this.renderRoot.querySelector("#decisionBoardDiv").style.display = "grid";
    //this.renderRoot.querySelector("#decisionBoardDiv").style.color="white";
    this.renderRoot
      .querySelector("#athleteTimer")
      .reset(this.renderRoot.querySelector("#athleteTimer"));
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "block";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  down() {
    this.renderRoot.querySelector("#decisionBoardDiv").style.display = "grid";
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "block";
  }

  doBreak() {
    //console.debug("decisionBoard doBreak");
    this.renderRoot.querySelector("#decisionBoardDiv").style.display = "grid";
    //this.renderRoot.querySelector("#decisionBoardDiv").style.color="white";
    this.renderRoot.querySelector("#breakTimer").style.display = "block";
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "block";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
  }

  groupDone() {
    this.clear();
  }

  clear() {
    this.renderRoot.querySelector("#decisionBoardDiv").style.display = "none";
  }

  reload() {
    console.log("reloading");
    window.location.reload();
  }
  constructor() {
    super();
    this.weight = 0;
  }
}

customElements.define(DecisionBoard.is, DecisionBoard);
