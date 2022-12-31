/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';
class AthleteCard extends PolymerElement {
	static get is() {
		return 'athlete-card-template'
	}

	static get template() {
		return html`
<style>
#athleteCardDiv {
	-webkit-print-color-adjust: exact !important;
	color-adjust: exact;
}

@media print {
	#Header, #Footer, #printing {
		display: none !important;
		visibility: hidden  !important;
	}
	
	.printing {
		display: none !important;
		visibility: hidden  !important;
	}
}

.style0 {
	mso-number-format: General;
	text-align: general;
	vertical-align: bottom;
	white-space: nowrap;
	color: black;
	font-size: 11.0pt;
	font-weight: 400;
	font-style: normal;
	text-decoration: none;
	font-family: Calibri, sans-serif;
	border: none;
}

td {
	padding-top: 1px;
	padding-right: 1px;
	padding-left: 1px;
	color: black;
	font-size: 11.0pt;
	font-weight: 400;
	font-style: normal;
	text-decoration: none;
	font-family: Calibri, sans-serif;
	text-align: general;
	vertical-align: bottom;
	border: none;
	white-space: nowrap;
}

.xl65 {
	font-size: 10.0pt;
	text-align: right;
	white-space: normal;
}

.xl66 {
	font-size: 20.0pt;
	font-weight: 700;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl67 {
	font-size: 18.0pt;
	font-weight: 700;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl68 {
	font-size: 20.0pt;
	font-weight: 700;
	text-align: right;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl69 {
	font-size: 14.0pt;
	font-weight: 700;
	text-align: right;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl70 {
	font-size: 14.0pt;
	text-align: center;
}

.xl71 {
	font-size: 16.0pt;
	font-weight: 700;
	text-align: center;
}

.xl72 {
	font-size: 16.0pt;
	font-weight: 700;
	text-align: center;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl73 {
	font-size: 14.0pt;
	border-top: 1.0pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: 1.0pt solid black;
}

.xl74 {
	font-size: 14.0pt;
	border-top: 1.0pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl75 {
	font-size: 14.0pt;
	font-weight: 700;
	text-align: center;
	vertical-align: middle;
	border-top: 1.0pt solid black;
	border-right: 1.0pt solid black;
	border-bottom: none;
	border-left: 1.0pt solid black;
}

.xl76 {
	font-size: 14.0pt;
	text-align: center;
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: 1.0pt solid black;
	border-left: 1.0pt solid black;
}

.xl77 {
	font-size: 14.0pt;
	text-align: center;
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: 1.0pt solid black;
	border-left: .5pt solid black;
}

.xl78 {
	font-size: 14.0pt;
	text-align: center;
	border-top: .5pt solid black;
	border-right: .5pt solid black;
	border-bottom: none;
	border-left: 1.0pt solid black;
}

.xl79 {
	font-size: 14.0pt;
	text-align: center;
	border-top: .5pt solid black;
	border-right: .5pt solid black;
	border-bottom: none;
	border-left: .5pt solid black;
}

.xl80 {
	font-size: 14.0pt;
	text-align: center;
	border-top: .5pt solid black;
	border-right: 1.0pt solid black;
	border-bottom: none;
	border-left: .5pt solid black;
}

.xl81 {
	font-size: 14.0pt;
	font-weight: 700;
	vertical-align: middle;
	border-top: none;
	border-right: 1.0pt solid black;
	border-bottom: 1.0pt solid black;
	border-left: 1.0pt solid black;
}

.xl82 {
	color: #333333;
	border-top: 1.0pt solid black;
	border-right: .5pt solid black;
	border-bottom: .5pt solid black;
	border-left: 1.0pt solid black;
	background: #333333;
	mso-pattern: #333300 none;
}

.xl83 {
	border-top: 1.0pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: .5pt solid black;
	background: #CCCCCC;
	mso-pattern: #CCCCFF none;
}

.xl84 {
	border-top: 1.0pt solid black;
	border-right: 1.0pt solid black;
	border-bottom: none;
	border-left: 1.0pt solid black;
}

.xl85 {
	text-align: center;
}

.xl86 {
	font-size: 8.0pt;
	vertical-align: top;
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: 1.0pt solid black;
	white-space: normal;
}

.xl87 {
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: .5pt solid black;
}

.xl88 {
	border-top: none;
	border-right: 1.0pt solid black;
	border-bottom: none;
	border-left: 1.0pt solid black;
}

.xl89 {
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: 1.0pt solid black;
}

.xl90 {
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: 1.0pt solid black;
	border-left: 1.0pt solid black;
}

.xl91 {
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: 1.0pt solid black;
	border-left: .5pt solid black;
}

.xl92 {
	border-top: none;
	border-right: 1.0pt solid black;
	border-bottom: 1.0pt solid black;
	border-left: 1.0pt solid black;
}

.xl93 {
	font-size: 10.0pt;
	text-align: center;
	vertical-align: middle;
	white-space: normal;
}

.xl94 {
	font-size: 10.0pt;
	text-align: right;
	vertical-align: middle;
	white-space: normal;
}

.xl95 {
	font-size: 16.0pt;
	text-align: center;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}

.xl96 {
	font-size: 8.0pt;
	text-align: center;
	vertical-align: top;
	border-top: .5pt solid black;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: 1.0pt solid black;
	white-space: normal;
}

.xl97 {
	font-size: 12.0pt;
	font-weight: 700;
	text-align: center;
	border-top: none;
	border-right: none;
	border-bottom: .5pt solid black;
	border-left: none;
}
</style>
<div id='athleteCardDiv'>
	<table
		style='border-collapse: collapse; table-layout: fixed; width: 96vw'>
		<col span=8>
		<tr style='height: 3em'>
			<td class=xl65>[[t.name]]</td>
			<td class=xl66 colspan=3>&nbsp;&nbsp;[[fullName]]</td>
			<td class=xl68 colspan=2>[[team]]</td>
			<td class=xl94 style='vertical-align: bottom'>[[t.bodyWeight]]</td>
			<td class=xl72>[[bodyWeight]][[kgSymbol]]</td>
		</tr>
		<tr style='height: 3em'>
			<td class=xl65>[[t.group]]</td>
			<td class=xl72>[[group]]</td>
			<td class=xl65>[[t.startNumber]]</td>
			<td class=xl72>[[startNumber]]</td>
			<td class=xl65>[[t.lotNumber]]</td>
			<td class=xl95>[[lotNumber]]</td>
			<td class=xl65>[[t.category]]</td>
			<td class=xl72>[[category]]</td>
		</tr>
		<tr style='height: 3em'>
			<template is="dom-if" if="[[ageDivision]]">
				<td class=xl65>[[t.ageDivision]]</td>
				<td class=xl72>[[ageDivision]]</td>
			</template>
			<template is="dom-if" if="[[!ageDivision]]">
				<td class=xl65></td>
				<td class=xl65></td>
			</template>
			<template is="dom-if" if="[[ageGroup]]">
				<td class=xl65>[[t.ageGroup]]</td>
				<td class=xl72>[[ageGroup]]</td>
			</template>
			<template is="dom-if" if="[[!ageGroup]]">
				<td class=xl65></td>
				<td class=xl65></td>
			</template>
			<td class=xl65>[[t.birthDate]]</td>
			<td class=xl72>[[birth]]</td>
			<td class=xl65>[[t.entryTotal]]</td>
			<td class=xl72>[[entryTotal]]</td>
		</tr>
		<tr style='height: 1.5em'>
			<td class=xl70 colspan=8></td>
		</tr>
		<tr style='height: 2em'>
			<td class=xl70></td>
			<td class=xl73 colspan=3 style='text-align:center'>[[t.snatch]]</td>
			<td class=xl73 colspan=3 style='text-align:center'>[[t.cleanJerk]]</td>
			<td class=xl75 style='text-align:center'>[[t.total]]</td>
		</tr>
		<tr style='height: 2em'>
			<td class=xl70></td>
			<td class=xl76>1</td>
			<td class=xl77>2</td>
			<td class=xl77>3</td>
			<td class=xl78>1</td>
			<td class=xl77>2</td>
			<td class=xl77>3</td>
			<td class=xl81 style='border-top: none'>&nbsp;</td>
		</tr>
		<tr style='height: 4.5em'>
			<td class=xl93>[[t.automaticProgression]]</td>
			<td class=xl82>&nbsp;</td>
			<td class=xl83>&nbsp;</td>
			<td class=xl83>&nbsp;</td>
			<td class=xl82>&nbsp;</td>
			<td class=xl83>&nbsp;</td>
			<td class=xl83>&nbsp;</td>
			<td class=xl84>&nbsp;</td>
		</tr>
		<tr style='height: 4.5em'>
			<td class=xl93>[[t.declaration]]</td>
			<td class=xl96 style='width: 12vw'>[[t.startingWeight]] <br>
				<br> <span style='font-size: 1.5rem; font-weight: normal'>[[snatch1Declaration]]</span>
			</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl96 style='width: 12vw'>[[t.startingWeight]] <br>
				<br> <span style='font-size: 1.5rem; font-weight: normal'>[[cleanJerk1Declaration]]</span>
			</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl88>&nbsp;</td>
		</tr>
		<tr style='height: 4.5em'>
			<td class=xl93>[[t.change1]]</td>
			<td class=xl89>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl89>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl87>&nbsp;</td>
			<td class=xl88>&nbsp;</td>
		</tr>
		<tr style='height: 4.5em'>
			<td class=xl93>[[t.change2]]</td>
			<td class=xl90>&nbsp;</td>
			<td class=xl91>&nbsp;</td>
			<td class=xl91>&nbsp;</td>
			<td class=xl90>&nbsp;</td>
			<td class=xl91>&nbsp;</td>
			<td class=xl91>&nbsp;</td>
			<td class=xl92>&nbsp;</td>
		</tr>
	</table>
</div>`;
	}

	static get properties() {
		return {
			lastName: {
				type: String,
				value: ''
			},
			firstName: {
				type: String,
				value: ''
			},
			teamName: {
				type: String,
				value: ''
			},
			startNumber: {
				type: Number,
				value: 0
			},
			lotNumber: {
				type: Number,
				value: 0
			},
			attempt: {
				type: String,
				value: ''
			}, 
			bodyWeight: {
				type: String,
				value: ''
			},
			ageDivision: {
				type: String,
				value: ''
			},
			category: {
				type: String,
				value: ''
			},
			startSnatch: {
				type: String,
				value: ''
			},
			startCJ: {
				type: String,
				value: ''
			},
			entryTotal: {
				type: String,
				value: ''
			}
		}
	}

	ready() {
		super.ready();
	}
}

customElements.define(AthleteCard.is, AthleteCard);



