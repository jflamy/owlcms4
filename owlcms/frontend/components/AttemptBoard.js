/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class CurrentAttempt extends PolymerElement {
	static get is() {
		return 'attempt-board-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/styles/attemptboard.css">
<div class="wrapper">
<div class="attemptBoard" id="attemptBoardDiv">
	<div class="lastName" id="lastNameDiv">[[lastName]]</div>
	<div class="firstName" id="firstNameDiv">[[firstName]]</div>
	<div class="teamName" id="teamNameDiv">[[teamName]]</div>
	<div class="startNumber" id="startNumberDiv">
		<span>[[startNumber]]</span>
	</div>
	<div class="attempt" id="attemptDiv" inner-h-t-m-l="[[attempt]]"></div><!-- kludge to have preformatted html -->
	<div class="weight" id="weightDiv">
		<nobr>[[weight]]<span style="font-size: 75%">[[kgSymbol]]</span></nobr>
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
	<div class="decision" id="decisionDiv" on-down="down" on-hideX="reset">
		<decision-element id="decisions"></decision-element>
	</div>
</div>
</div>`;
	}

	static get properties() {
		return {
			javaComponentId: {
				type: String,
				value: ''
			},
			lastName: {
				type: String,
				value: ''
			},
			firstName: {
				type: String,
				value: ''
			},
			teamName: {
				type: String,
				value: ''
			},
			startNumber: {
				type: Number,
				value: 0
			},
			attempt: {
				type: String,
				value: ''
			},
			weight: {
				type: Number,
				value: 0
			}
		}
	}

	ready() {
		super.ready();
		this.doBreak();
		this.$.athleteTimerDiv.style.display = "none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		console.warn("attemptBoard reset " + this.javaComponentId);
		this.$.attemptBoardDiv.style.display = "grid";
		this.$.attemptBoardDiv.style.color = "white";
		this.$.athleteTimer.reset(this.$.athleteTimer);
		this.$.athleteTimerDiv.style.display = "block";
		this.$.firstNameDiv.style.display = "block";
		this.$.teamNameDiv.style.display = "block";
		this.$.attemptDiv.style.display = "block";
		this.$.breakTimerDiv.style.display = "none";
		this.$.weightDiv.style.display = "block";
		this.$.startNumberDiv.style.display = "block";
		this.$.barbellDiv.style.display = "block";
		this.$.decisionDiv.style.display = "none";
		console.debug("end of attemptBoard reset " + this.javaComponentId);
	}

	down() {
		console.debug("attemptBoard down " + this.javaComponentId);
		this.$.athleteTimerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "block";
		console.debug("end of attemptBoard dome " + this.javaComponentId);
	}

	doBreak() {
		console.debug("attemptBoard doBreak " + this.javaComponentId);
		this.$.attemptBoardDiv.style.display = "grid";
		this.$.attemptBoardDiv.style.color = "white";
		this.$.athleteTimerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "block";
		this.$.firstNameDiv.style.display = "block";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.startNumberDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
		console.debug("attemptBoard end doBreak " + this.javaComponentId);
	}

	groupDone() {
		console.debug("attemptBoard groupDone " + this.javaComponentId);
		this.$.attemptBoardDiv.style.display = "grid";
		this.$.attemptBoardDiv.style.color = "white";
		// this.$.breakTimer.reset();
		this.$.athleteTimerDiv.style.display = "none";
		this.$.firstNameDiv.style.display = "none";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.startNumberDiv.style.display = "none";
		this.$.barbellDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
		console.debug("attemptBoard end groupDone " + this.javaComponentId);
	}

	clear() {
		console.debug("attemptBoard clear " + this.javaComponentId);
		this.$.attemptBoardDiv.style.display = "none";
		console.debug("attemptBoard end clear " + this.javaComponentId);
	}

	reload() {
		console.log("reloading")
		window.location.reload();
	}
}

customElements.define(CurrentAttempt.is, CurrentAttempt);

