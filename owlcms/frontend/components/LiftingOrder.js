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

:root {
  --veryNarrow-width: 4%;
  --narrow-width: 6%;
  --group-width: 7ch;
  --category-width: 6ch;
}

/* header cells for rank in the main table, wide screen */
@media screen and (min-width: 1401px) {
	:root {
	  --fontSizeRank-height: 0.95em;
	  --fontSizeRows-height: 1.15em;
	  --name-width: 20vw;
	  --club-width: 15vw;
	}
}

/* header cells for rank in the main table, 720 screen or 1366 laptop */
@media screen and (max-width: 1400px) {
	:root {
	  --fontSizeRank-height: 0.9em;
	  --fontSizeRows-height: 1.1em;
	  --name-width: 20vw;
	  --club-width: 15vw;
	}
}

/* header cells for rank in the main table, 1024 projector */
@media screen and (max-width: 1024px) {
	:root {
	  --narrow-width: 7%;
	  --veryNarrow-width: 4.5ch;
	  --medium-width: 9%;
	  --category-width: 6ch;
	  --team-width: 10%;
	  --name-width: 16vw;
	  --club-width: 10vw;
	  
	  --fontSizeRank-height: 0.8em;
	  --fontSizeRows-height: 0.9em;
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
		font-size: var(--fontSizeRows-height);
	}
	th,td {
		font-size: var(--fontSizeRows-height);
	}
}

.veryNarrow {
	width: var(--veryNarrow-width);
	text-align: center;
}

.medium {
	width: var(--narrow-width);
	white-space: nowrap;
	text-align: center;
}

.club {
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
@keyframes blink { 50% {opacity: 0.0;}}
@-webkit-keyframes blink { 50% {opacity: 0.0;}}

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

<div class="wrapper" width="100%">
<div class="attemptBar" style$="[[_computeHidden(hidden)]]">
	<div class="athleteInfo" id="athleteInfoDiv" style$="[[_computeHidden(hidden)]]">
		<div class="fullName" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
	</div>
</div>
<div class="group" id="groupDiv" style$="[[_computeHidden(hidden)]]">
	<span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]
</div>
<table width="100%" class="results" id="orderDiv" style$="[[_computeHidden(hidden)]]">
	<thead>
		<tr>
			<!--  [[t.x]] is loaded with the translation for key Scoreboard.x in the translation4.csv file -->
			<th width="5%" style="text-align: center;" inner-h-t-m-l="[[t.Start]]"></th>
			<th width="35%" inner-h-t-m-l="[[t.Name]]"></th>
			<th class="narrow" inner-h-t-m-l="[[t.RequestedWeight]]"></th>
			<th class="narrow" inner-h-t-m-l="[[t.NextAttempt]]"></th>
			<th class="medium" inner-h-t-m-l="[[t.Category]]"></th>
			<th width="30%" class='club' inner-h-t-m-l="[[t.Team]]"></th>
		</tr>
	</thead>
	<template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
		<template is="dom-if" if="[[l.isSpacer]]">
			<tr><td colspan="0" style="height:0.1ex; border:none"></td></tr>
		</template>
		<template is="dom-if" if="[[!l.isSpacer]]">
			<tr>
				<td width="5%" style="text-align: center;" class$="[[l.classname]]">[[l.startNumber]]</td>
				<td width="35%" class$="[[l.classname]] ellipsis">[[l.fullName]]</td>
				<td class$="[[l.classname]]" style="text-align: center;">[[l.requestedWeight]]</td>
				<td class$="[[l.classname]]" style="text-align: center; white-space: nowrap">[[l.nextAttemptNo]]</td>			
				<td class="medium" style="text-align: center; white-space: nowrap" >[[l.category]]</td>
				<td width="30%" class="club ellipsis">[[l.teamName]]</td>	
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

	_computeCatWidth(wideCategory) {
		return wideCategory ? 'medium' : 'narrow';
	}
}

customElements.define(Scoreboard.is, Scoreboard);
