import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class Scoreboard extends PolymerElement {
	static get is() {
		return 'scoreboard-template'
	}

	static get template() {
		return html`
	<link rel="stylesheet" type="text/css" href="local/styles/scoreboard.css">
	<div class$="wrapper [[_computeTeamWidth(wideTeamNames)]] [[_computeInactiveClass(hidden)]]">
		<div style$="[[_computeInactive(hidden)]]">
			<div class="competitionName">[[competitionName]]</div><br>
			<div class="nextGroup">[[t.WaitingNextGroup]]</div>
		</div>
		<div class="attemptBar" style$="[[_computeHidden(hidden)]]">
			<div class="athleteInfo" id="athleteInfoDiv">
				<div class="startNumber" id="startNumberDiv">
					<span>[[startNumber]]</span>
				</div>
				<div class="fullName ellipsis" id="fullNameDiv" inner-h-t-m-l="[[fullName]]"></div>
				<div class="clubName ellipsis" id="teamNameDiv">
					[[teamName]]
				</div>
				<div class="attempt" id="attemptDiv">
					<span inner-h-t-m-l="[[attempt]]"></span>
				</div>
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
			<div id="groupDiv">
				<span class="groupName">[[groupName]]</span> &ndash; [[liftsDone]]
			</div>
		</div>
		<table class="results" style$="[[_computeHidden(hidden)]]">
			<thead>
				<tr>
					<!--  [[t.x]] references the translation for key Scoreboard.x in the translation4.csv file -->
					<th class="veryNarrow" inner-h-t-m-l="[[t.Start]]"></th>
					<th class$="name [[_computeTeamWidth(wideTeamNames)]]" inner-h-t-m-l="[[t.Name]]"></th>
					<!-- kludge to have preformatted html -->
					<th class="category" inner-h-t-m-l="[[t.Category]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Birth]]"></th>
					<th class$="club [[_computeTeamWidth(wideTeamNames)]]" inner-h-t-m-l="[[t.Team]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
					<th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
					<th class="showThRank" inner-h-t-m-l="[[t.Rank]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
					<th class="thRank" inner-h-t-m-l="[[t.Rank]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[athletes]]" as="l">
				<template is="dom-if" if="[[l.isSpacer]]">
					<tr>
						<td colspan="100%" style="height:0.1ex; border:none" class="spacer"></td>
					</tr>
				</template>
				<template is="dom-if" if="[[!l.isSpacer]]">
					<tr>
						<td class$="[[l.classname]] veryNarrow">
							<div>[[l.startNumber]]</div>
						</td>
						<td class$="name [[_computeTeamWidth(wideTeamNames)]] [[l.classname]]">
							<div>[[l.fullName]]</div>
						</td>
						<td class="category">
							<div>[[l.category]]</div>
						</td>
						<td class="narrow">
							<div>[[l.yearOfBirth]]</div>
						</td>
						<td class$="club [[_computeTeamWidth(wideTeamNames)]]">
							<div>[[l.teamName]]</div>
						</td>
						<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
							<td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
								<div>[[attempt.stringValue]]</div>
							</td>
						</template>
						<td class="showRank">[[l.snatchRank]]</td>
						<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
							<td class$="[[attempt.goodBadClassName]] [[attempt.className]]">
								<div>[[attempt.stringValue]]</div>
							</td>
						</template>
						<td class="showRank">[[l.cleanJerkRank]]</td>
						<td class="narrow">[[l.total]]</td>
						<td class="veryNarrow">[[l.totalRank]]</td>
					</tr>
				</template>
			</template>
		</table>
	</div>`;
	}

	ready() {
		console.debug("ready");
		super.ready();
		this.$.groupDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "flex";
		this.$.startNumberDiv.style.display = "flex";
		this.$.teamNameDiv.style.display = "flex";
		this.$.attemptDiv.style.display = "flex";
		this.$.weightDiv.style.display = "flex";
		this.$.timerDiv.style.display = "flex";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	start() {
		this.$.timer.start();
	}

	reset() {
		console.debug("reset");
		this.$.timer.reset(this.$.timer);
		this.$.groupDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "flex";
		this.$.startNumberDiv.style.display = "flex";
		this.$.teamNameDiv.style.display = "flex";
		this.$.attemptDiv.style.display = "flex";
		this.$.weightDiv.style.display = "flex";
		this.$.timerDiv.style.display = "flex";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	down() {
		console.debug("refereeDecision");
		this.$.groupDiv.style.visibility = "visible";
		this.$.startNumberDiv.style.display = "flex";
		this.$.teamNameDiv.style.display = "flex";
		this.$.attemptDiv.style.display = "flex";
		this.$.weightDiv.style.display = "flex";
		this.$.timerDiv.style.display = "flex";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "flex";
	}

	doBreak() {
		console.debug("break");
		this.$.groupDiv.style.visibility = "hidden";
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "flex";
		this.$.startNumberDiv.style.display = "none";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.timerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "flex";
		this.$.decisionDiv.style.display = "none";
	}

	groupDone() {
		console.debug("done");
		this.$.groupDiv.style.visibility = "hidden";
		this.$.fullNameDiv.style.visibility = "visible";
		this.$.fullNameDiv.style.display = "flex";
		this.$.startNumberDiv.style.display = "none";
		this.$.teamNameDiv.style.display = "none";
		this.$.attemptDiv.style.display = "none";
		this.$.weightDiv.style.display = "none";
		this.$.timerDiv.style.display = "none";
		this.$.breakTimerDiv.style.display = "none";
		this.$.decisionDiv.style.display = "none";
	}

	refereeDecision() {
		console.debug("refereeDecision");
		this.$.groupDiv.style.visibility = "visible";
		this.$.decisionDiv.style.display = "flex";
		this.$.weightDiv.style.display = "flex";
		this.$.timerDiv.style.display = "flex";
		this.$.breakTimerDiv.style.display = "none";
	}

	_isEqualTo(title, string) {
		return title == string;
	}

	_computeHidden(hidden) {
		return hidden ? 'display:none' : 'display:block';
	}
	_computeInactive(hidden) {
		return hidden ? 'display:block' : 'display:none';
	}
	_computeInactiveClass(hidden) {
		return hidden ? 'bigTitle' : '';
	}
	_computeTeamWidth(w) {
		return w ? 'wideTeams' : 'narrowTeams';
	}
}

customElements.define(Scoreboard.is, Scoreboard);
