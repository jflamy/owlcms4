import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class CurrentAttempt extends LitElement {
  static get is() {
    return "attempt-board-template";
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
        (this.autoversion ?? "")
        +".css"}"
      />
      <link
        rel="stylesheet"
        type="text/css"
        .href="${"local/" +
        (this.stylesDir ?? "") +
        "/" +
        (this.video ?? "") +
        "resultsCustomization" +
        (this.autoversion ?? "")
        +".css"}"
      />
      <link
        rel="stylesheet"
        type="text/css"
        .href="${"local/" +
        (this.stylesDir ?? "") +
        "/" +
        (this.video ?? "") +
        "attemptboard" +
        (this.autoversion ?? "")
        +".css"}"
      />
      <div class="${"wrapper " + (this.inactiveClass ?? "")}">
        <div style="${this.inactiveBlockStyle}">
          <div class="competitionName">${this.competitionName}</div>
          <br />
          <div class="nextGroup">${this.t?.WaitingNextGroup}</div>
        </div>
        <div
          class="attemptBoard"
          id="attemptBoardDiv"
          style="${this.activeGridStyle}"
        >
          <div
            id="lastNameDiv"
            class="${"lastName" + (this.WithPicture ?? "")}"
          >
            <div>${this.lastName}</div>
          </div>
          <div
            id="firstNameDiv"
            class="${"firstName" + (this.WithPicture ?? "")}"
          >
            <div>${this.firstName}</div>
          </div>
          <div class="teamName" id="teamNameDiv">${this.teamName}</div>
          <div
            id="flagDiv"
            class="${"flag" +
            (this.WithPicture ?? "") +
            " " +
            (this.hideBecauseRecord ?? "") +
            " " +
            (this.hideBecauseDecision ?? "")}"
            .inner-h-t-m-l="${this.teamFlagImg}"
          ></div>
          <div
            id="pictureDiv"
            class="${"picture " +
            (this.hideBecauseRecord ?? "") +
            " " +
            (this.hideBecauseDecision ?? "")}"
            .inner-h-t-m-l="${this.athleteImg}"
          ></div>
          <div id="recordDiv" class="${this.recordKind}">
            ${this.recordMessage}
          </div>
          <div class="startNumber" id="startNumberDiv">
            <span>${this.startNumber}</span>
          </div>
          <div class="category" id="categoryDiv">
            <span style="white-space: nowrap;">${this.category}</span>
          </div>
          <div class="attempt" id="attemptDiv">
            <span .inner-h-t-m-l="${this.attempt}"></span
            ><!-- kludge to have preformatted html -->
          </div>
          <div class="weight" id="weightDiv">
            <span style="white-space: nowrap;"
              >${this.weight}<span style="font-size: 75%"
                >${this.kgSymbol}</span
              ></span
            >
          </div>
          <div class="barbell" id="barbellDiv">
            <slot name="barbell"></slot>
          </div>
          <div class="timer athleteTimer" id="athleteTimerDiv">
            <timer-element id="athleteTimer"></timer-element>
          </div>
          <div class="timer breakTime" id="breakTimerDiv">
            <timer-element id="breakTimer"></timer-element>
          </div>
          <div
            class="decision"
            id="decisionDiv"
            @down="${this.down}"
            @hideX="${this.reset}"
          >
            <decision-element id="decisions"></decision-element>
          </div>
        </div>
      </div>`;
  }

  static get properties() {
    return {
      javaComponentId: {
        type: String,
      },
      lastName: {
        type: String,
      },
      firstName: {
        type: String,
      },
      teamName: {
        type: String,
      },
      startNumber: {
        type: Number,
      },
      attempt: {
        type: String,
      },
      weight: {
        type: Number,
      },
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    this.doBreak();
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
  }

  start() {
    this.renderRoot.querySelector("#timer").start();
  }

  reset() {
    console.warn("attemptBoard reset " + this.javaComponentId);
    //this.renderRoot.querySelector("#attemptBoardDiv").style.display = "grid";
    //this.renderRoot.querySelector("#attemptBoardDiv").style.color = "white";
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "grid";
    this.renderRoot.querySelector("#firstNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#attemptDiv").style.display = "grid";
    this.renderRoot.querySelector("#categoryDiv").style.display = "grid";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#weightDiv").style.display = "grid";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "block";
    this.renderRoot.querySelector("#barbellDiv").style.display = "grid";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
    console.debug("end of attemptBoard reset " + this.javaComponentId);
  }

  down() {
    console.debug("attemptBoard down " + this.javaComponentId);
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "grid";
    console.debug("end of attemptBoard dome " + this.javaComponentId);
  }

  doBreak(showWeight) {
    console.debug(
      "attemptBoard doBreak " +
        this.javaComponentId +
        " showWeight = " +
        showWeight
    );
    //this.renderRoot.querySelector("#attemptBoardDiv").style.display = "grid";
    //this.renderRoot.querySelector("#attemptBoardDiv").style.color = "white";
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "grid";
    this.renderRoot.querySelector("#firstNameDiv").style.display = "grid";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    this.renderRoot.querySelector("#categoryDiv").style.display = "none";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "none";
    if (showWeight) {
      this.renderRoot.querySelector("#weightDiv").style.display = "grid";
      this.renderRoot.querySelector("#barbellDiv").style.display = "grid";
      this.renderRoot.querySelector("#decisionDiv").style.display = "grid";
      //this.renderRoot.querySelector("#breakTimerDiv").style.display = "grid";
    } else {
      this.renderRoot.querySelector("#weightDiv").style.display = "none";
      this.renderRoot.querySelector("#barbellDiv").style.display = "none";
      this.renderRoot.querySelector("#decisionDiv").style.display = "none";
      this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    }
    console.debug("attemptBoard end doBreak " + this.javaComponentId);
  }

  groupDone() {
    console.debug("attemptBoard groupDone " + this.javaComponentId);
    //this.renderRoot.querySelector("#attemptBoardDiv").style.display = "grid";
    //this.renderRoot.querySelector("#attemptBoardDiv").style.color = "white";
    // this.renderRoot.querySelector("#breakTimer").reset();
    this.renderRoot.querySelector("#athleteTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#firstNameDiv").style.display = "none";
    this.renderRoot.querySelector("#teamNameDiv").style.display = "none";
    this.renderRoot.querySelector("#attemptDiv").style.display = "none";
    this.renderRoot.querySelector("#categoryDiv").style.display = "none";
    this.renderRoot.querySelector("#breakTimerDiv").style.display = "none";
    this.renderRoot.querySelector("#weightDiv").style.display = "none";
    this.renderRoot.querySelector("#startNumberDiv").style.display = "none";
    this.renderRoot.querySelector("#barbellDiv").style.display = "none";
    this.renderRoot.querySelector("#decisionDiv").style.display = "none";
    console.debug("attemptBoard end groupDone " + this.javaComponentId);
  }

  clear() {
    console.debug("attemptBoard clear " + this.javaComponentId);
    this.renderRoot.querySelector("#attemptBoardDiv").style.display = "none";
    console.debug("attemptBoard end clear " + this.javaComponentId);
  }

  reload() {
    console.log("reloading");
    window.location.reload();
  }
  constructor() {
    super();
    this.javaComponentId = "";
    this.lastName = "";
    this.firstName = "";
    this.teamName = "";
    this.startNumber = 0;
    this.attempt = "";
    this.weight = 0;
  }
}

customElements.define(CurrentAttempt.is, CurrentAttempt);
