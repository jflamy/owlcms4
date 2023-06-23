/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class Monitor extends PolymerElement {
	static get is() {
		return 'eventmonitor-template'
	}

	static get template() {
		return html`
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]colors[[autoversion]].css">
<link rel="stylesheet" type="text/css" href="local/[[stylesDir]]/[[video]]eventmonitor[[autoversion]].css">
<div class="wrapper">
	<div class$="notification [[notificationClass]]">
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
