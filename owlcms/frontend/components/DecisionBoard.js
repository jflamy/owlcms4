import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';       
class DecisionBoard extends PolymerElement {
	static get is() {
		return 'decision-board-template'
	}

	static get template() {
		return html`<style>
* {
	box-sizing: border-box;
}

.wrapper {
	font: Arial;
	color: white;
	background-color: black;
	height: 100vh;
	width: 100vw;
}

.decisionBoard {
	font-family: Arial, Helvetica, sans-serif;
	color: white;
	background-color: black;
	display: grid;
	width: 100vw;
	height: 100vh;
	grid-template-columns:
		[barbell-start down-start decision-start timer-start]1fr
		[barbell-end timer-end name down-end decision-end];
	grid-template-rows: 0.25fr[down-start decision-start barbell-start timer-start]1fr
		[barbell-end timer-end down-end	decision-end]0.25fr;
	justify-content: center;
	align-content: center;
	align-items: stretch;
	justify-items: stretch;
	padding: 5vmin;
}


.decisionBoard .barbell {
	grid-area: barbell-start/barbell-start/barbell-end/barbell-end;
}

.decisionBoard .timer {
	font-size: 50vh;
	font-weight: bold;
	grid-area: timer-start/timer-start/timer-end/timer-end;
	align-self: center;
	justify-self: center;
}

@media screen and (max-width: 1300px) {
	.decisionBoard .timer {
		font-size: 35vw;
	}
}

.breakTime {
	/* color: #99CCFF; */
	color: SkyBlue;
}

.athleteTimer {
	color: yellow;
}

.decisionBoard .down {
	grid-area: down-start/down-start/down-end/down-end;
	align-self: center;
	justify-self: center;
	--iron-icon-height: 10%;
	--iron-icon-width: 10%;
	font-weight: normal;
	color: pink;
	display: none;
	overflow: hidden;
}

.decisionBoard .decision {
	grid-area: decision-start/decision-start/decision-end/decision-end;
	font-size: 50vh;
	height: 100%;
	align-self: center;
}

.v-system-error {
	display: none;
}
</style>
<div class="wrapper">
<div class="decisionBoard" id="decisionBoardDiv">
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
		this.$.athleteTimerDiv.style.display="none";
		this.$.barbellDiv.style.display="none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		this.$.decisionBoardDiv.style.display="grid";
		this.$.decisionBoardDiv.style.color="white";
		this.$.athleteTimer.reset(this.$.athleteTimer);
		this.$.athleteTimerDiv.style.display="block";
		this.$.breakTimerDiv.style.display="none";
		this.$.barbellDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	down() {
		this.$.athleteTimerDiv.style.display="none";
		this.$.breakTimerDiv.style.display="none";
		this.$.barbellDiv.style.display="none";
		this.$.decisionDiv.style.display="block";
	}

	doBreak() {
		//console.debug("decisionBoard doBreak");
		this.$.decisionBoardDiv.style.display="grid";
		this.$.decisionBoardDiv.style.color="white";
		//this.$.breakTimer.style.display="block";
		this.$.athleteTimerDiv.style.display="none";
		this.$.breakTimerDiv.style.display="block";
		this.$.barbellDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	groupDone() {
		this.clear();
	}

	clear() {
		this.$.decisionBoardDiv.style.display="none";
	}
	
	reload() {
		console.log("reloading");
		window.location.reload();
	}
}

customElements.define(DecisionBoard.is, DecisionBoard);

