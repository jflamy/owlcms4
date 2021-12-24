/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class Monitor extends PolymerElement {
	static get is() {
		return 'monitor-template'
	}

	static get template() {
		return html`
	[[title]]
	`;
	}

	ready() {
		console.debug("monitor ready");
		super.ready();
	}

}

customElements.define(Monitor.is, Monitor);
