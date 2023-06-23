/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class Monitor extends PolymerElement {
	static get is() {
		return 'notifications-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]colors[[autoversion]].css">
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]currentathlete[[autoversion]].css">
<div class$="wrapper" style="background-color: #00ff00; display: flex">
	<div style$="height: 100px; line-height: 100px; background-color: [[bgColor]]; color: [[txtColor]]; text-align: center; font-size: 30px; width: 50%; margin: auto;">
		[[title]]
	</div>
</div>
	`;
	}

	ready() {
		//console.debug("monitor is ready");
		super.ready();
	}

	setTitle(title) {
		//console.log("title = "+title);
		document.title = title;
	}
}

customElements.define(Monitor.is, Monitor);
