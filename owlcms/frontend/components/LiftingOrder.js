/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';       
class Scoreboard extends PolymerElement {
	static get is() {
		return 'liftingorder-template'
	}
       
	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/styles/liftingorder.css">
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
