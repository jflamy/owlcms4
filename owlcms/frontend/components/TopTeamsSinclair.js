/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
class TopTeamsSinclair extends PolymerElement {
	static get is() {
		return 'topteamsinclair-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/styles/top_[[autoversion]].css">
<div class$="wrapper [[_computeTeamWidth(wideTeamNames)]]" id="resultBoardDiv">
	<template is="dom-if" if="[[topTeamsWomen]]">
		<h2 class="fullName" id="fullNameDiv" inner-h-t-m-l="[[topTeamsWomen]]"></h2>
		<table class="results" id="orderDiv" style$="">
			<thead>
				<tr>
					<th class="club" inner-h-t-m-l="[[t.Team]]"></th>
					<!-- th class="medium" inner-h-t-m-l="[[t.Done]]"></th -->
					<!-- th class="medium" inner-h-t-m-l="[[t.TeamSize]]"></th -->
					<th class="medium"  inner-h-t-m-l="[[t.Sinclair]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[womensTeams]]" as="l">
				<tr>
					<td class="club"><div>[[l.team]]</div></td>
					<!-- td class="medium"><div>[[l.counted]]</div></td -->	
					<!-- td class="medium"><div>[[l.size]]</div></td -->
					<td class="medium"><div>[[l.score]]</div></td>
				</tr>
			</template>
		</table>
		<h2>&nbsp;</h2>
	</template>
	<template is="dom-if" if="[[topTeamsMen]]">
		<h2 class="fullName" id="fullNameDiv" inner-h-t-m-l="[[topTeamsMen]]"></h2>
		<table class="results" id="orderDiv" style$="">
			<thead>
				<tr>
					<th class="club" inner-h-t-m-l="[[t.Team]]"></th>
					<!-- th class="medium" inner-h-t-m-l="[[t.Done]]"></th -->
					<!-- th class="medium" inner-h-t-m-l="[[t.TeamSize]]"></th -->
					<th class="medium"  inner-h-t-m-l="[[t.Sinclair]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[mensTeams]]" as="l">
				<tr>
					<td class="club"><div>[[l.team]]</div></td>
					<!-- td class="medium"><div>[[l.counted]]</div></td -->		
					<!-- td class="medium"><div>[[l.size]]</div></td -->
					<td class="medium"><div>[[l.score]]</div></td>
				</tr>
			</template>
		</table>
		<h2>&nbsp;</h2>
	</template>
</div>`;
	}

	ready() {
		super.ready();
		document.body.setAttribute("theme","dark");
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

	_computeTeamWidth(w) {
		return w ? 'wideTeams' : 'narrowTeams';
	}

}

customElements.define(TopTeamsSinclair.is, TopTeamsSinclair);
