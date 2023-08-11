import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class Monitor extends LitElement {
  static get is() {
    return "monitor-template";
  }

  render() {
    return html` ${this.title} `;
  }

  firstUpdated(_changedProperties) {
    //console.debug("monitor is ready");
    super.firstUpdated(_changedProperties);
  }

  setTitle(title) {
    //console.log("title = "+title);
    document.title = title;
  }
}

customElements.define(Monitor.is, Monitor);
