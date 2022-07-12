/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
class TopSinclair extends PolymerElement {
	static get is() {
		return 'topsinclair-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/styles/top.css">
<div class$="wrapper [[_computeTeamWidth(wideTeamNames)]]" id="resultBoardDiv">
	<template is="dom-if" if="[[topSinclairWomen]]">
		<h2 class="fullName" id="fullNameDiv" inner-h-t-m-l="[[topSinclairWomen]]"></h2>
		<table class="results" id="orderDiv" style$="">
			<thead>
				<tr>
					<th class="name" inner-h-t-m-l="[[t.Name]]"></th>
					<th class="category" inner-h-t-m-l="[[t.Category]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
					<th class="club" inner-h-t-m-l="[[t.Team]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
					<th class="narrow"  inner-h-t-m-l="[[t.Total]]"></th>
					<th class="medium" inner-h-t-m-l="[[t.BodyWeight]]"></th>
					<th class="medium sinclair" inner-h-t-m-l="[[t.Sinclair]]"></th>
					<th class="needed" inner-h-t-m-l="[[t.Needed]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[sortedWomen]]" as="l">
				<tr>
					<td class$="name [[l.classname]]">
						<div>[[l.fullName]]</div>
					</td>
					<td class="category">[[l.category]]</td>
					<td class="veryNarrow"><div>[[l.yearOfBirth]]</div></td>
					<td class="club"><div>[[l.teamName]]</div></td>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
					</template>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
					</template>
					<td class="narrow"><div>[[l.total]]</div></td>	
					<td class="medium"><div>[[l.bw]]</div></td>
					<td class="medium sinclair"><div>[[l.sinclair]]</div></td>
					<td class="needed"><div>[[l.needed]]</div></td>
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
					<th class="name" inner-h-t-m-l="[[t.Name]]"></th>
					<th class="category" inner-h-t-m-l="[[t.Category]]"></th>
					<th class="veryNarrow" inner-h-t-m-l="[[t.Birth]]"></th>
					<th class="club" inner-h-t-m-l="[[t.Team]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Snatch]]"></th>
					<th colspan="3" inner-h-t-m-l="[[t.Clean_and_Jerk]]"></th>
					<th class="narrow" inner-h-t-m-l="[[t.Total]]"></th>
					<th class="medium" inner-h-t-m-l="[[t.BodyWeight]]"></th>
					<th class="medium sinclair" inner-h-t-m-l="[[t.Sinclair]]"></th>
					<th class="needed" inner-h-t-m-l="[[t.Needed]]"></th>
				</tr>
			</thead>
			<template is="dom-repeat" id="result-table" items="[[sortedMen]]" as="l">
				<tr>
					<td class$="name [[l.classname]]">
						<div>[[l.fullName]]</div>
					</td>
					<td class="category">[[l.category]]</td>
					<td class="veryNarrow"><div>[[l.yearOfBirth]]</div></td>
					<td class="club"><div>[[l.teamName]]</div></td>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.sattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
					</template>
					<template is="dom-repeat" id="result-table-attempts" items="[[l.cattempts]]" as="attempt">
						<td class$="[[attempt.goodBadClassName]] [[attempt.className]]"><div>[[attempt.stringValue]]</div></td>
					</template>
					<td class="narrow"><div>[[l.total]]</div></td>	
					<td class="medium"><div>[[l.bw]]</div></td>
					<td class="medium sinclair"><div>[[l.sinclairForDelta]]</div></td>
					<td class="needed"><div>[[l.needed]]</div></td>
				</tr>
			</template>
		</table>
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

customElements.define(TopSinclair.is, TopSinclair);
