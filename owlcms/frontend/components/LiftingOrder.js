import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';       
class Scoreboard extends PolymerElement {
	static get is() {
		return 'liftingorder-template'
	}
       
	static get template() {
		return html`<style>
* {
	box-sizing: border-box;
}

.wrapper {
	font-family: Arial, Helvetica, sans-serif;
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
	width: 100vw;
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
	align-items: baseline;
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
	justify-content: flex-end;
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
	display: flex;
	color: aqua;
	justify-items: baseline;
	justify-content: center;
}

.group {
	font-size: 3vh;
	margin-top: 1vh;
	margin-bottom: 1vh;
}

table.results {
	/* table-layout: fixed; */
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


.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
	width: 4vw;
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
</style>

<div class="wrapper" width="100vw">
<div class="attemptBar" style$="[[_computeHidden(hidden)]]">
	<div class="athleteInfo" id="athleteInfoDiv" style$="[[_computeHidden(hidden)]]">
		<div class="fullName" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
	</div>
</div>
<div class="group" id="groupDiv" style$="[[_computeHidden(hidden)]]">
	<span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]
</div>
<table width="100vw" class="results" id="orderDiv" style$="[[_computeHidden(hidden)]]">
	<thead>
		<tr>
			<!--  [[t.x]] is loaded with the translation for key Scoreboard.x in the translation4.csv file -->
			<th class="narrow" style="text-align: center;" inner-h-t-m-l="[[t.Start]]"></th>
			<th width="35%" inner-h-t-m-l="[[t.Name]]"></th>
			<th width="9%" class="narrow" inner-h-t-m-l="[[t.RequestedWeight]]"></th>
			<th width="9%" class="narrow" inner-h-t-m-l="[[t.NextAttempt]]"></th>
			<th width="9%" class$="[[_computeMasters(masters)]]" inner-h-t-m-l="[[t.AgeGroup]]"></th>
			<th width="9%" class="narrow" inner-h-t-m-l="[[t.Category]]"></th>
			<th width="20%" class='club' inner-h-t-m-l="[[t.Team]]"></th>
		</tr>
	</thead>
	<template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
		<template is="dom-if" if="[[l.isSpacer]]">
			<tr><td colspan="0" style="height:0.1ex; border:none"></td></tr>
		</template>
		<template is="dom-if" if="[[!l.isSpacer]]">
			<tr>
				<td class$="narrow"  style="text-align: center;"><div class$="[[l.classname]]">[[l.startNumber]]</div></td>
				<td width="25%"><div class$="[[l.classname]] ellipsis">[[l.fullName]]</div></td>
				<td width="10%" class$="[[l.classname]]" style="text-align: center;">[[l.requestedWeight]]</td>
				<td width="10%" class$="[[l.classname]]" style="text-align: center;">[[l.nextAttemptNo]]</td>			
				<td width="10%"class$="[[_computeMasters(masters)]]">[[l.mastersAgeGroup]]</td>
				<td width="10%" class="narrow">[[l.category]]</td>
				<td width="25%" class="club ellipsis">[[l.teamName]]</td>	
			</tr>
		</template>
	</template>
</table>
</div>`;
	}

	ready() {
		super.ready();
		this.$.groupDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="block";
		this.$.orderDiv.style.visibility="visible"
	}

	start() {
		this.$.groupDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="block";
		this.$.orderDiv.style.visibility="visible"
	}

	reset() {
		console.debug("reset");
		this.$.groupDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="block";
		this.$.orderDiv.style.visibility="visible";
	}

	down() {
		console.debug("down");
	}

	doBreak() {
		console.debug("break");
		this.$.groupDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="block";
		this.$.orderDiv.style.display="block";
	}

	groupDone() {
		console.debug("done");
		this.$.groupDiv.style.visibility="hidden";
		this.$.fullNameDiv.style.display="block";
		this.$.orderDiv.style.visibility="hidden";
	}

	refereeDecision() {
		console.debug("refereeDecision");
	}

	_isEqualTo(title, string) {
		return title == string;
	}

	_computeHidden(hidden) {
		return hidden ? 'display:none' : 'display:block';
	}

	_computeMasters(masters) {
		return masters ? 'masters' : 'mastersHidden';
	}
}

customElements.define(Scoreboard.is, Scoreboard);
