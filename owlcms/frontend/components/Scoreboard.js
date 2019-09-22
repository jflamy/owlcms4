import {PolymerElement} from '@polymer/polymer/polymer-element.js';       
class Scoreboard extends Polymer.Element {
	static get is() {
		return 'scoreboard-template'
	}
       
	static get template() {
		return html`<style>
* {
	box-sizing: border-box;
}

.wrapper {
	color: white;
	background-color: black;
	height: 100vh;
	width: 100vw;
	padding: 2vmin 2vmin 2vmin 2vmin;
	overflow-y: hidden;
}

.attemptBar {
	display: flex;
	font-size: 3.6vmin;
	justify-content: space-between;
	width: 100%;
	height: 4vmin;
}

.attemptBar .startNumber span {
	font-size: 70%;
	border-width: 0.2ex;
	border-style: solid;
	border-color: red;
	width: 1.5em;
	display: flex;
	justify-content: center;
	align-self: center;
}

.attemptBar .athleteInfo {
	display: flex;
	font-size: 3.6vmin;
	justify-content: space-between;
	width: 100%;
}

.athleteInfo .fullName {
	font-weight: bold;
	flex: 0 0 35%;
	text-align: left;
	margin-left: 1em;
	/*margin-right: auto;*/
	flex-grow: 0.5;
}

.athleteInfo .timer {
	flex: 0 0 15%;
	text-align: right;
	font-weight: bold;
	width: 10vw;
	display: flex;
}

.athleteInfo .decisionBox {
	position: fixed;
	top: 2vmin;
	right: 2vmin;
	width: 15vw;
	height: 10vh;
	background-color: black;
	display: none;
}

.athleteInfo .weight {
	color: aqua;
}

.group {
	font-size: 3vh;
	margin-top: 1vh;
	margin-bottom: 1vh;
}

table.results {
	width: 100%;
	border-collapse: collapse;
	border: none;
	/*margin-bottom: 2vmin;*/
}

th, td {
	border-collapse: collapse;
	border: solid 1px DarkGray;
	padding: 0.4vmin 1vmin 0.4vmin 1vmin;
	font-size: 2.1vh;
	font-weight: normal;
}

@media screen and (min-width: 1030px) {
	.showThRank {
		border-collapse: collapse;
		border: solid 1px DarkGray;
		border-left-style: none;
		padding: 0.5vmin 1vmin 0.5vmin 1vmin;
		font-size: 1.5vh;
		font-weight: normal;
		font-style: italic;
		width: 4vw;
		text-align: center;
	}
}

@media screen and (max-width: 1029px) {
	.showThRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
	}
}

.thRank {
	border-collapse: collapse;
	border: solid 1px DarkGray;
	border-left-style: none;
	padding: 0.5vmin 1vmin 0.5vmin 1vmin;
	font-size: 1.5vh;
	font-weight: normal;
	font-style: italic;
	width: 4vw;
	text-align: center;
}

.masters {
	display: table-cell;
	text-align: center;
	width: 4vw;
}

.mastersHidden {
	display: none;
	width: 0px;
	padding: 0 0 0 0;
	margin: 0 0 0 0;
}

.narrow {
	width: 6vw;
	text-align: center;
}

@media screen and (min-width: 1020px) {
	.showRank {
		display: table-cell;
		width: 4vw;
		text-align: center;
	}
}

@media screen and (max-width: 1029px) {
	.showRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
	}
}

.veryNarrow {
	width: 4vw;
	text-align: center;
}

.club {
	text-align: center;
}

.narrow {
	width: 6vw;
	text-align: center;
}

.good {
	background-color: green;
	font-weight: bold;
}

.fail {
	background-color: red;
	font-weight: bold;
}

.english {
	font-size: 85%;
}

.request {
	background-color: black;
	font-style: italic;
}

.current {
	color: yellow;
	font-weight: bold;
}

.blink {
	animation: blink 1.5s step-start 0s infinite;
	-webkit-animation: blink 1.5s step-start 0s infinite;
}
@keyframes blink { 50% {opacity: 0.0;}}
@-webkit-keyframes blink { 50% {opacity: 0.0;}}

.next {
	color: orange;
	font-weight: bold;
}

.empty {
	background-color: black;
	font-style: italic;
}

.breakTime {
	/* color: #99CCFF; */
	color: SkyBlue;
}

.athleteTimer {
	color: yellow;
}

.v-system-error {
	display: none;
}
</style>

<div class="wrapper" id="resultBoardDiv" style$="[[_computeHidden(hidden)]]">
<div class="attemptBar">
	<div class="athleteInfo" id="athleteInfoDiv">
		<div class="startNumber" id="startNumberDiv"><span>[[startNumber]]</span></div>
		<div class="fullName" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
		<div class="clubName" id="teamNameDiv">[[teamName]]</div>
		<div class="attempt" id="attemptDiv" inner-h-t-m-l="[[attempt]]"></div>
		<div class="weight" id="weightDiv">
			[[weight]]<span style="font-size: 75%">[[t.KgSymbol]]</span>
		</div>
		<div class="timer athleteTimer" id="timerDiv">
			<timer-element id="timer"></timer-element>
		</div>
		<div class="timer breakTime" id="breakTimerDiv">
			<timer-element id="breakTimer"></timer-element>
		</div>		
		<div class="decisionBox" id="decisionDiv">
			<decision-element style="width: 100%" id="decisions"></decision-element>
		</div>
	</div>
</div>
<div class="group" id="groupDiv">
<span class="groupName"></span>[[groupName]]</span> &ndash; [[liftsDone]]
</div>
<table class="results">
	<thead>
		<tr>
			<!--  [[t.x]] references the translation for key Scoreboard.x in the translation4.csv file -->
			<th class="veryNarrow" inner-h-t-m-l="[[t.Start]]"></th>
			<th inner-h-t-m-l="[[t.Name]]"></th><!-- kludge to have preformatted html -->
			<th class$="[[_computeMasters(masters)]]" inner-h-t-m-l="[[t.AgeGroup]]"></th>
			<th class="veryNarrow" inner-h-t-m-l="[[t.Category]]"></th>
			<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
			<th class='club' inner-h-t-m-l="[[t.Team]]"></th>
			<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
			<th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
			<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
			<th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
			<th class="veryNarrow" inner-h-t-m-l="[[t.Total]]"></th>
			<th class="thRank" inner-h-t-m-l="[[t.Rank]]"></th>
		</tr>
	</thead>
	<template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
		<template is="dom-if" if="[[l.isSpacer]]">
			<tr><td colspan="0" style="height:0.1ex; border:none"></td></tr>
		</template>
		<template is="dom-if" if="[[!l.isSpacer]]">
			<tr>
				<td class$="veryNarrow"><div class$="[[l.classname]]">[[l.startNumber]]</div></td>
				<td><div class$="[[l.classname]]">[[l.fullName]]</div></td>
				<td class$="[[_computeMasters(masters)]]">[[l.mastersAgeGroup]]</td>
				<td class="veryNarrow">[[l.category]]</td>
				<td class="veryNarrow">[[l.yearOfBirth]]</td>
				<td class="club">[[l.teamName]]</td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]]"><div class$="[[attempt.className]]">[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank">[[l.snatchRank]]</td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]]"><div class$="[[attempt.className]]">[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank">[[l.cleanJerkRank]]</td>		
				<td class="veryNarrow">[[l.total]]</td>
				<td class="veryNarrow">[[l.totalRank]]</td>
			</tr>
		</template>
	</template>
</table>
</div>`;
	}


	ready() {
		super.ready();
		this.$.groupDiv.style.display="block";
		this.$.startNumberDiv.style.display="block";
		this.$.teamNameDiv.style.display="block";
		this.$.attemptDiv.style.display="block";
		this.$.weightDiv.style.display="block";
		this.$.timerDiv.style.display="block";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		console.debug("reset");
		this.$.timer.reset();
		this.$.groupDiv.style.visibility="visible";
		this.$.startNumberDiv.style.display="block";
		this.$.teamNameDiv.style.display="block";
		this.$.attemptDiv.style.display="block";
		this.$.weightDiv.style.display="block";
		this.$.timerDiv.style.display="block";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	down() {
		console.debug("refereeDecision");
		this.$.groupDiv.style.visibility="visible";
		this.$.startNumberDiv.style.display="block";
		this.$.teamNameDiv.style.display="block";
		this.$.attemptDiv.style.display="block";
		this.$.weightDiv.style.display="block";
		this.$.timerDiv.style.display="block";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="flex";
	}

	doBreak() {
		console.debug("break");
		this.$.groupDiv.style.visibility="hidden";
		this.$.startNumberDiv.style.display="none";
		this.$.teamNameDiv.style.display="none";
		this.$.attemptDiv.style.display="none";
		this.$.weightDiv.style.display="none";
		this.$.timerDiv.style.display="none";
		this.$.breakTimerDiv.style.display="block";
		this.$.decisionDiv.style.display="none";
	}

	groupDone() {
		console.debug("done");
		this.$.groupDiv.style.visibility="hidden";
		this.$.fullNameDiv.style.display="block";
		this.$.startNumberDiv.style.display="none";
		this.$.teamNameDiv.style.display="none";
		this.$.attemptDiv.style.display="none";
		this.$.weightDiv.style.display="none";
		this.$.timerDiv.style.display="none";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	refereeDecision() {
		console.debug("refereeDecision");
		this.$.groupDiv.style.visibility="visible";
		this.$.decisionDiv.style.display="flex";
		this.$.weightDiv.style.display="block";
		this.$.timerDiv.style.display="block";
		this.$.breakTimerDiv.style.display="none";
	}

	_isEqualTo(title, string) {
		return title == string;
	}

	clear() {
		this.$.resultBoardDiv.style.display="none";
	}

	_computeHidden(hidden) {
		return hidden ? 'display:none' : 'display:block';
	}

	_computeMasters(masters) {
		return masters ? 'masters' : 'mastersHidden';
	}
}

customElements.define(Scoreboard.is, Scoreboard);
