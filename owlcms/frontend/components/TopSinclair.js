import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
class TopSinclair extends PolymerElement {
	static get is() {
		return 'topsinclair-template'
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
/* 	margin-left: 1em; */
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

@media screen and (min-width: 1025px) {
	.showRank {
		display: table-cell;
		width: 4vw;
		text-align: center;
	}
}

@media screen and (max-width: 1024px) {
	.showRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
	}
}

.veryNarrow {
	width: 6vw;
	text-align: center;
}

.club {
	text-align: center;
}

.narrow {
	width: 8vw;
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

.sinclair {
	font-weight: bold;
}

h2 {
  font-size: 3.0vh;
}
</style>
<div class="wrapper" id="resultBoardDiv">
	<template is="dom-if" if="[[topSinclairWomen]]">
		<h2 class="fullName" id="fullNameDiv" inner-h-t-m-l="[[topSinclairWomen]]"></h2>
		<table class="results" id="orderDiv" style$="">
			<thead>
				<tr>
					<th inner-h-t-m-l="[[t.Name]]"></th>
					<th class$="[[_computeMasters(masters)]]" inner-h-t-m-l="[[t.AgeGroup]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Category]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
					<th class='club' inner-h-t-m-l="[[t.Team]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Total]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.BodyWeight]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Sinclair]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Needed]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[sortedWomen]]" as="l">
				<tr>
					<td><div class$="[[l.classname]]">[[l.fullName]]</div></td>
					<td class$="[[_computeMasters(masters)]]">[[l.mastersAgeGroup]]</td>
					<td class="veryNarrow">[[l.category]]</td>
					<td class="veryNarrow">[[l.yearOfBirth]]</td>
					<td class="club">[[l.teamName]]</td>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]]"><div
							class$="[[attempt.className]]">[[attempt.stringValue]]</div></td>
					</template>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]]"><div
							class$="[[attempt.className]]">[[attempt.stringValue]]</div></td>
					</template>
					<td class="veryNarrow">[[l.total]]</td>
					<td class="veryNarrow">[[l.bw]]</td>
					<td class="veryNarrow sinclair">[[l.sinclair]]</td>
					<td class="veryNarrow">[[l.needed]]</td>
				</tr>
			</template>
		</table>
		<h2>&nbsp;</h2>
	</template>
	<template is="dom-if" if="[[topSinclairMen]]">
		<h2 class="fullName" id="fullNameDiv" inner-h-t-m-l="[[topSinclairMen]]"></h2>
		<table class="results" id="orderDiv" style$="">
			<thead>
				<tr>
					<th inner-h-t-m-l="[[t.Name]]"></th>
					<th class$="[[_computeMasters(masters)]]" inner-h-t-m-l="[[t.AgeGroup]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Category]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
					<th class='club' inner-h-t-m-l="[[t.Team]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Total]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.BodyWeight]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Sinclair]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Needed]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[sortedMen]]" as="l">
				<tr>
					<td><div class$="[[l.classname]]">[[l.fullName]]</div></td>
					<td class$="[[_computeMasters(masters)]]">[[l.mastersAgeGroup]]</td>
					<td class="veryNarrow">[[l.category]]</td>
					<td class="veryNarrow">[[l.yearOfBirth]]</td>
					<td class="club">[[l.teamName]]</td>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]]">
							<div class$="[[attempt.className]]">[[attempt.stringValue]]</div>
						</td>
					</template>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]]">
						<div class$="[[attempt.className]]">[[attempt.stringValue]]</div>
					</td>
					</template>
					<td class="veryNarrow">[[l.total]]</td>
					<td class="veryNarrow">[[l.bw]]</td>
					<td class="veryNarrow sinclair">[[l.sinclair]]</td>
					<td class="veryNarrow">[[l.needed]]</td>
				</tr>
			</template>
		</table>
	</template>
</div>`;
	}

	ready() {
		super.ready();
		this.$.resultBoardDiv.style.display="block";
	}

	start() {
		this.$.resultBoardDiv.style.display="block";
	}

	reset() {
		console.debug("reset");
		this.$.resultBoardDiv.style.display="block";
	}

	down() {
		console.debug("down");
	}

	doBreak() {
		console.debug("break");
		this.$.resultBoardDiv.style.display="block";
	}

	groupDone() {
		console.debug("done");
		this.$.resultBoardDiv.style.display="block";
	}

	refereeDecision() {
		console.debug("refereeDecision");
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

customElements.define(TopSinclair.is, TopSinclair);
