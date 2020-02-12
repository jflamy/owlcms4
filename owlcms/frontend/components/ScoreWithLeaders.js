import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';  

class ScoreLeader extends PolymerElement {
	static get is() {
		return 'scoreleader-template'
	}
       
	static get template() {
		return html`<style>
* {
	box-sizing: border-box;
}

:root {
	  --narrow-width: 5ch;
	  --max-narrow-width: 5ch;
	  --veryNarrow-width: 4.5ch;
	  --max-veryNarrow-width: 4.5ch;
	  --medium-width: 6ch;
}

/* wide screen */
@media screen and (min-width: 1401px) {
	.wideTeams {
	  --fontSizeRank-height: 0.9em;
	  --fontSizeRows-height: 0.9em;
	  --name-width: 20vw;
	  --name-max-width: 100%;
	  --club-width: 20vw;
	  --club-max-width: 100%;
	  --category-width: 10ch;
	  --category-max-width: 100%;
	  --rank-width: 5ch;
	  --group-width: 5vw;
	}
	.narrowTeams {
	  --fontSizeRank-height: 0.95em;
	  --fontSizeRows-height: 1.15em;
	  --name-width: 35vw;
	  --name-max-width: 100%;
	  --club-width: 8vw;
	  --club-max-width: 100%;
	  --category-width: 12ch;
	  --category-max-width: 100%;
	  --rank-width: 5ch;
	  --group-width: 5vw;
	}
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
		font-size: var(--fontSizeRank-height);
	}
	.showRank {
		display: table-cell;
		width: var(--rank-width);
		font-size: var(--fontSizeRows-height);
		text-align: center;
	}
	.showRank div{
		width: var(--rank-width);
		margin: auto;
	}
	th,td {
		font-size: var(--fontSizeRows-height);
	}
}

/* 720 screen or 1366 laptop */
@media screen and (max-width: 1400px) and (min-width: 1280px) {
	.wideTeams {
	  --fontSizeRank-height: 0.9em;
	  --fontSizeRows-height: 0.9em;
	  --name-width: 20vw;
	  --name-max-width: 20vw;
	  --club-width: 25vw;
	  --club-max-width: 100%;
	  --category-width: 10ch;
	  --category-max-width: 100%;
	  --rank-width: 5ch;
	  --group-width: 5vw;
	}
	.narrowTeams {
	  --fontSizeRank-height: 0.9em;
	  --fontSizeRows-height: 1.1em;
	  --name-width: 35vw;
	  --name-max-width: 23vw;
	  --club-width: 12ch;
	  --club-max-width: 100%;
	  --category-width: 12ch;
	  --rank-width: 5ch;
	  --group-width: 5vw;
	}
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
		font-size: var(--fontSizeRank-height);
	}
	.showRank {
		display: table-cell;
		font-size: var(--fontSizeRows-height);
		text-align: center;
		width: var(--rank-width);
	}
	.showRank div{
		width: var(--rank-width);
		margin: auto;
	}
	th,td {
		font-size: var(--fontSizeRows-height);
	}
}

/* 1024 projector */
@media screen and (max-width: 1279px) {
	.wideTeams {
	  --fontSizeRank-height: 0.8em;
	  --fontSizeRows-height: 0.75em;
      --name-width: 20vw;
	  --name-max-width: 30ch;
	  --club-width: 20vw;
	  --club-max-width: 30ch;
	  --clubName-max-width: 20ch;
	  --category-width: 10ch;
	  --rank-width: 6ch;
	  --group-width: 5vw;
	}
	.narrowTeams {
	  --fontSizeRank-height: 0.8em;
	  --fontSizeRows-height: 0.75em;
	  --name-width: 35vw;
	  --name-max-width: 100%;
	  --club-width: 12ch;
	  --club-max-width: 100%;
	  --category-width: inherit;
	  --rank-width: 5ch;
	  --group-width: 5vw;
	}
	.showThRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		font-size: var(--fontSizeRank-height);
	}	
	.showRank {
		display: none;
		width: 0px;
		padding: 0 0 0 0;
		margin: 0 0 0 0;
		font-size: var(--fontSizeRows-height);
	}
	th,td {
		font-size: var(--fontSizeRows-height);
	}
}

.wrapper {
	font-family: Arial, Helvetica, sans-serif;
	color: white;
	background-color: black;
	min-height: 100vh;
	padding: 2vmin 2vmin 2vmin 2vmin;
	overflow: hidden;
	display: flex;
	flex-direction: column;
	flex-wrap: no-wrap;
	justify-content: flex-start;
}

#results {
	flex: 1 0 auto;
}

#leaders {
	flex: 0 0 auto;
	width: 100%;
	min-height: 0;
	align-self: flex-end;
	background-color: blue;
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
	max-width: calc(var(--name-max-width));
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
	max-width: var(--club-max-width);
	text-align: center;
	margin: auto;
	white-space: nowrap;
	overflow: hidden;
	text-overflow: ellipsis;
}

.ellipsis {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.thRank {
	border-collapse: collapse;
	border: solid 1px DarkGray;
	border-left-style: none;
	padding: 0.5vmin 1vmin 0.5vmin 1vmin;
	font-weight: normal;
	font-style: italic;
	width: var(--rank-width);
	text-align: center;
	font-size: var(--fontSizeRank-height);
}
.thRank div{
	width: var(--rank-width);
	margin: auto;
}


.narrow {
	width: var(--narrow-width);
	text-align: center;
}
.narrow div {
	width: var(--max-narrow-width);
	display: inline-block;
}

.veryNarrow {
	width: var(--veryNarrow-width);
	text-align: center;
}
.veryNarrow div {
	width: var(--max-veryNarrow-width);
	display: inline-block;
}

.groupCol {
	width: var(--group-width);
	white-space: nowrap;
	text-align: center;
	font-size: var(--fontSizeRank-height)
}
.groupCol div {
	width: var(--group-width);
	display: inline-block;
}

.category {
	width: var(--category-width);
	white-space: nowrap;
	text-align: center;
}

.category div {
	width: var(--category-width);
	display: inline-block;
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


table#leaders-table thead tr.hide {
	visibility: hidden; height:1px; line-height: 1px; font-size:1px;
}
table#leaders-table thead tr.hide th {
	visibility: hidden; height:1px; line-height: 1px; font-size:1px; padding:0;
}

.clubNameEllipsis {
	max-width: var(--clubName-max-width);
	white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin: auto;
    text-align: left;
}

.hiddenTitle {
	display: none;
}
.bigTitle {
	align-items: center;
	justify-content: center;
	text-align: center;
}
.competitionName {
	font-size: 4em;
}
.nextGroup {
	font-size: 2em;
}
</style>
<div class$="wrapper [[_computeTeamWidth(wideTeamNames)]] [[_computeInactiveClass(hidden)]]">
<div style$="[[_computeInactive(hidden)]]">
	<div class="competitionName">[[competitionName]]</div><br>
	<div class="nextGroup">[[t.WaitingNextGroup]]</div>
</div>
<div class="attemptBar" style$="[[_computeHidden(hidden)]]">
	<div class="athleteInfo" id="athleteInfoDiv">
		<div class="startNumber" id="startNumberDiv"><span>[[startNumber]]</span></div>
		<div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[fullName]]">[[fullName]]</div>
		<div class="clubName ellipsis" id="teamNameDiv"><div class="clubNameEllipsis">[[teamName]]</div></div>
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
<div class="group" style$="[[_computeHidden(hidden)]]">
<div id="groupDiv"><span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]</div>
</div>
<div id="results" style$="[[_computeHidden(hidden)]]">
<table class="results" style$="[[_computeHidden(hidden)]]">
	<thead>
		<tr>
			<!--  [[t.x]] references the translation for key ScoreLeader.x in the translation4.csv file -->
			<th class="groupCol" inner-h-t-m-l="[[t.Start]]"></th>
			<th class="name" inner-h-t-m-l="[[t.Name]]"></th><!-- kludge to have preformatted html -->
			<th class="category" inner-h-t-m-l="[[t.Category]]"></th>
			<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
			<th class="club" inner-h-t-m-l="[[t.Team]]"></th>
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
				<td class$="groupCol [[l.classname]] "><div>[[l.startNumber]]</div></td>
				<td class$="name [[l.classname]]">
					<div>[[l.fullName]]</div>
				</td>
				<td class="category"><div>[[l.category]]</div></td>
				<td class="veryNarrow"><div>[[l.yearOfBirth]]</div></td>
				<td class="club"><div>[[l.teamName]]</div></td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank"><div>[[l.snatchRank]]</div></td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank"><div>[[l.cleanJerkRank]]</div></td>		
				<td class="veryNarrow">[[l.total]]</td>
				<td class="veryNarrow"><div>[[l.totalRank]]</div></td>
			</tr>
		</template>
	</template>
</table>
</div>
<template is="dom-if" if="[[leaders]]">
<div id="leaders" style$="[[_computeHidden(hidden)]]">
<table class="results" id="leaders-table" style$="[[_computeHidden(hidden)]]">
	<thead>
		<tr><td colspan="100%" inner-h-t-m-l="[[t.Leaders]] [[categoryName]]"></td></tr>
	</thead>
	<template is="dom-repeat" id="result-table" items="[[leaders]]" as="l">
		<template is="dom-if" if="[[l.isSpacer]]">
			<tr><td colspan="100%" style="height:0.1ex; border:none" class="spacer"></td></tr>
		</template>
		<template is="dom-if" if="[[!l.isSpacer]]">
			<tr>
				<td class="groupCol"><div>[[l.group]]</div></td>
				<td class$="name [[l.classname]]">
					<div>[[l.fullName]]</div>
				</td>
				<td class="category"><div>[[l.category]]</div></td>
				<td class="veryNarrow">[[l.yearOfBirth]]</td>
				<td class="club"><div>[[l.teamName]]</div></td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank"><div>[[l.snatchRank]]</div></td>
				<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
					<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
				</template>
				<td class="showRank"><div>[[l.cleanJerkRank]]</div></td>	
				<td class="veryNarrow"><div>[[l.total]]</div></td>
				<td class="thRank"><div>[[l.totalRank]]</div></td>
			</tr>
		</template>
	</template>
</table>
</div>
</template>
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

	_computeHidden(hidden) {
		return hidden ? 'display:none' : 'display:block';
	}
	_computeInactive(hidden) {
		return hidden ? 'display:block' : 'display:none' ;
	}
	_computeInactiveClass(hidden) {
		return hidden ? 'bigTitle' : '';
	}
	_computeTeamWidth(w) {
		return w ? 'wideTeams' : 'narrowTeams';
	}
}

customElements.define(ScoreLeader.is, ScoreLeader);
