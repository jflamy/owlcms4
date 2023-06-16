/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
class DecisionBoard extends PolymerElement {
	static get is() {
		return 'decision-board-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]colors[[autoversion]].css">
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]decisionboard[[autoversion]].css">
<div class$="wrapper [[inactiveClass]]">
	<div style$="[[inactiveBlockStyle]]" >
		<div class="competitionName">[[competitionName]]</div><br>
		<div class="nextGroup">[[t.WaitingNextGroup]]</div>
	</div>
	<div class="decisionBoard" id="decisionBoardDiv" style$="[[activeGridStyle]]">
		<div class="barbell" id="barbellDiv">
			<slot name="barbell"></slot>
		</div>
		<div class="timer athleteTimer" id="athleteTimerDiv">
			<timer-element id="athleteTimer"></timer-element>
		</div>
		<div class="timer breakTime" id="breakTimerDiv">
			<timer-element id="breakTimer"></timer-element>
		</div>
		<div class="decision" id="decisionDiv" on-down="down">
			<decision-element id="decisions"></decision-element>
		</div>
	</div>
</div>`;
	}

	static get properties() {
		return {
			weight: {
				type: Number,
				value: 0
			}
		}
	}

	ready() {
		super.ready();
		console.log("decision board ready.")
		this.doBreak();
		this.$.athleteTimerDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		//this.$.decisionBoardDiv.style.display="grid";
		//this.$.decisionBoardDiv.style.color="white";
		this.$.athleteTimer.reset(this.$.athleteTimer);
		this.$.athleteTimerDiv.style.display = "block";
		this.$.breakTimerDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	down() {
		this.$.athleteTimerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "block";
	}

	doBreak() {
		//console.debug("decisionBoard doBreak");
		//this.$.decisionBoardDiv.style.display="grid";
		//this.$.decisionBoardDiv.style.color="white";
		this.$.breakTimer.style.display = "block";
		this.$.athleteTimerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display="block";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	groupDone() {
		this.clear();
	}

	clear() {
		this.$.decisionBoardDiv.style.display = "none";
	}

	reload() {
		console.log("reloading");
		window.location.reload();
	}
}

customElements.define(DecisionBoard.is, DecisionBoard);

