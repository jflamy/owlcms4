import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';  

class Scoreboard extends PolymerElement {
	static get is() {
		return 'scoreboard-template'
	}
       
	static get template() {
		return html`<style>
* {
	box-sizing: border-box;
}

:root {
  --narrow-width: 6%;
  --veryNarrow-width: 4%;
  --group-width: 7ch;
  --category-width: 6ch;
  
  --fontSizeRank-height: 1.05em;
  --fontSizeRows-height: 1.16em;
  --fontSizeRank-heightXGA: 0.9em;
  --fontSizeRows-heightXGA: 1.1em;
}

@media screen and (min-width:1401px) {
	:root {  
	  --name-width: 20vw;
	  --club-width: 15vw;
	}
}

@media screen and (max-width:1400px) {
	:root {  
	  --name-width: 22vw;
	  --club-width: 8vw;
	}
}

.wrapper {
	font-family: Arial, Helvetica, sans-serif;
	color: white;
	background-color: black;
	height: 100vh;
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

.attemptBar .startNumber {
	align-self: center;
}

.attemptBar .startNumber span {
	font-size: 70%;
	font-weight: bold;
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
	color: aqua;
	display: flex;
	justify-content: center;
	align-items: baseline;
}

.group {
	font-size: 3vh;
	margin-top: 1vh;
	margin-bottom: 2vh;
}

table.results {
    table-layout: fixed;
	width: 100%;
	border-collapse: collapse;
	border: none;
	background-color: black;
	/*margin-bottom: 2vmin;*/
}

:host(.dark) table.results tr {
	background-color: black;
	color: white;
}

:host(.light) table.results tr {
	background-color: white;
	color: black;
}

th, td {
	border-collapse: collapse;
	border: solid 1px DarkGray;
	padding: 0.4vmin 1vmin 0.4vmin 1vmin;
	font-size: var(--fontSizeRows-height);
}

:host(.dark) th, td {
	font-weight: normal;
}

:host(.light) th, td {
	font-weight: bold;
}

.name {
	width: var(--name-width);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.name div {
	width: calc(var(--name-width)*1.2);
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.club {
	width: var(--club-width);
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.club div {
	width: var(--club-width);
	text-align: center;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* header cells for rank in the main table, wide screen */
@media screen and (min-width: 1401px) {
	.showThRank {
		border-collapse: collapse;
		border: solid 1px DarkGray;
		border-left-style: none;
		padding: 0.5vmin 1vmin 0.5vmin 1vmin;
		font-size: var(--fontSizeRank-height);
		font-weight: normal;
		font-style: italic;
		width: 4vw;
		text-align: center;
	}
}

/* header cells for rank in the main table, XGA projector */
@media screen and (max-width: 1400px) {
	.showThRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		font-size: var(--fontSizeRank-heightXGA);
	}
}

.thRank {
	border-collapse: collapse;
	border: solid 1px DarkGray;
	border-left-style: none;
	padding: 0.5vmin 1vmin 0.5vmin 1vmin;
	font-size: var(--fontSizeRows-height);
	font-weight: normal;
	font-style: italic;
	width: var(--veryNarrow-width);
	text-align: center;
}

.narrow {
	width: var(--narrow-width);
	text-align: center;
}

/* rank cells in the main table, wide screen */
@media screen and (min-width: 1401px) {
	.showRank {
		display: table-cell;
		width: var(--veryNarrow-width);
		font-size: var(--fontSizeRows-height);
		text-align: center;
	}
}

/* rank cells in the main table, XGA projector */
@media screen and (max-width: 1400px) {
	.showRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		font-size: var(--fontSizeRows-heightXGA);
	}
	th,td {
		font-size: var(--fontSizeRows-heightXGA);
	}
}

.veryNarrow {
	width: var(--veryNarrow-width);
	text-align: center;
}

.groupCol {
	width: var(--group-width);
	white-space: nowrap;
	text-align: center;
}
.groupCol div {
	width: var(--group-width);
}

.category {
	width: var(--category-width);
	white-space: nowrap;
	text-align: center;
}

.narrow {
	width: var(--narrow-width);
	text-align: center;
}

:host(.dark) .good {
	background-color: green;
	font-weight: bold;
}

:host(.light) .good {
	background-color: green;
	font-weight: bold;
	color: white;
}

:host(.dark) .fail {
	background-color: red;
	font-weight: bold;
}

:host(.light) .fail {
	background-color: red;
	font-weight: bold;
	color: white;
}

:host(.dark)  .spacer {
	background-color: black;
}


:host(.light)  .spacer {
	background-color: gray;
}

.english {
	font-size: 85%;
}

:host(.dark) .request {
	background-color: black;
	font-style: italic;
}

:host(.light) .request {
	background-color: white;
	font-style: italic;
}

:host(.dark) .current {
	color: yellow;
	font-weight: bold;
}

:host(.light) .current {
	background-color: yellow;
	font-weight: bold;
}

.blink {
	animation: blink 1.5s step-start 0s infinite;
	-webkit-animation: blink 1.5s step-start 0s infinite;
}
@keyframes blink {
 50% {opacity: 0.0;}
}
@-webkit-keyframes blink {
 50% {opacity: 0.0;}
}

:host(.dark) .next {
	color: orange;
	font-weight: bold;
}

:host(.light) .next {
	background-color: gold;
	font-weight: bold;
}

:host(.dark) .empty {
	background-color: black;
	font-style: italic;
}

:host(.light) .empty {
	background-color: white;
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
<div class="wrapper">
<div class="attemptBar" style$="[[_computeHidden(hidden)]]">
	<div class="athleteInfo" id="athleteInfoDiv">
		<div class="startNumber" id="startNumberDiv"><span>[[startNumber]]</span></div>
		<div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
		<div class="clubName ellipsis" id="teamNameDiv">[[teamName]]</div>
		<div class="attempt" id="attemptDiv"><span inner-h-t-m-l="[[attempt]]"></span></div>
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
<div class="group" id="groupDiv" style$="[[_computeHidden(hidden)]]">
<span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]
</div>
<table class="results" style$="[[_computeHidden(hidden)]]">
	<thead>
		<tr>
			<!--  [[t.x]] references the translation for key Scoreboard.x in the translation4.csv file -->
			<th class="veryNarrow" inner-h-t-m-l="[[t.Start]]"></th>
			<th inner-h-t-m-l="[[t.Name]]"></th><!-- kludge to have preformatted html -->
			<th class$="[[_computeCatWidth(wideCategory)]]" inner-h-t-m-l="[[t.Category]]"></th>
			<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
			<th class="club ellipsis" inner-h-t-m-l="[[t.Team]]"></th>
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
			<tr><td colspan="100%" style="height:0.1ex; border:none" class="spacer"></td></tr>
		</template>
		<template is="dom-if" if="[[!l.isSpacer]]">
			<tr>
				<td class$="[[l.classname]] veryNarrow"><div>[[l.startNumber]]</div></td>
				<td width="30%" class$="ellipsis [[l.classname]]"><div class$="">[[l.fullName]]</div></td>
				<td class$="[[_computeCatWidth(wideCategory)]]">[[l.category]]</td>
				<td class="veryNarrow">[[l.yearOfBirth]]</td>
				<td class="ellipsis club">[[l.teamName]]</td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div class$="">[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank">[[l.snatchRank]]</td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div class$="">[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank">[[l.cleanJerkRank]]</td>		
				<td class="veryNarrow">[[l.total]]</td>
				<td class="veryNarrow">[[l.totalRank]]</td>
			</tr>
		</template>
	</template>
</table>
</div>
</div>`;
	}

	ready() {
		console.debug("ready");
		super.ready();
		this.$.groupDiv.style.visibility="visible";
		this.$.fullNameDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="flex";
		this.$.startNumberDiv.style.display="flex";
		this.$.teamNameDiv.style.display="flex";
		this.$.attemptDiv.style.display="flex";
		this.$.weightDiv.style.display="flex";
		this.$.timerDiv.style.display="flex";
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
		this.$.fullNameDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="flex";
		this.$.startNumberDiv.style.display="flex";
		this.$.teamNameDiv.style.display="flex";
		this.$.attemptDiv.style.display="flex";
		this.$.weightDiv.style.display="flex";
		this.$.timerDiv.style.display="flex";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="none";
	}

	down() {
		console.debug("refereeDecision");
		this.$.groupDiv.style.visibility="visible";
		this.$.startNumberDiv.style.display="flex";
		this.$.teamNameDiv.style.display="flex";
		this.$.attemptDiv.style.display="flex";
		this.$.weightDiv.style.display="flex";
		this.$.timerDiv.style.display="flex";
		this.$.breakTimerDiv.style.display="none";
		this.$.decisionDiv.style.display="flex";
	}

	doBreak() {
		console.debug("break");
		this.$.groupDiv.style.visibility="hidden";
		this.$.fullNameDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="flex";
		this.$.startNumberDiv.style.display="none";
		this.$.teamNameDiv.style.display="none";
		this.$.attemptDiv.style.display="none";
		this.$.weightDiv.style.display="none";
		this.$.timerDiv.style.display="none";
		this.$.breakTimerDiv.style.display="flex";
		this.$.decisionDiv.style.display="none";
	}

	groupDone() {
		console.debug("done");
		this.$.groupDiv.style.visibility="hidden";
		this.$.fullNameDiv.style.visibility="visible";
		this.$.fullNameDiv.style.display="flex";
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
		this.$.weightDiv.style.display="flex";
		this.$.timerDiv.style.display="flex";
		this.$.breakTimerDiv.style.display="none";
	}

	_isEqualTo(title, string) {
		return title == string;
	}

//	clear() {
//		this.$.resultBoardDiv.style.display="none";
//	}

	_computeHidden(hidden) {
		return hidden ? 'display:none' : 'display:block';
	}

	_computeCatWidth(wideCategory) {
		return wideCategory ? 'medium' : 'narrow';
	}
}

customElements.define(Scoreboard.is, Scoreboard);
