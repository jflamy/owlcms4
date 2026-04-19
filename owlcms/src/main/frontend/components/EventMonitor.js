import { html, LitElement, css } from "lit";
import { stylesheetHref } from "./stylesheetHref.js";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class Monitor extends LitElement {
  static get is() {
    return "eventmonitor-template";
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, `${this.video ?? ""}colors`) }" />
      <link rel="stylesheet" type="text/css" .href="${stylesheetHref(this, `${this.video ?? ""}eventmonitor`) }" />
      <div class="wrapper">
        <div class="${"notification " + (this.notificationClass ?? "")}">
          ${this.title}
        </div>
      </div>
    `;
  }

  firstUpdated(_changedProperties) {
    //console.debug("monitor is ready");
    super.firstUpdated(_changedProperties);
  }

  setTitle(title) {
    //console.log("title = "+title);
    document.title = title;
  }

  static get properties() {
    return {
      title: {},
      notificationClass: {},
    };
  }
}

customElements.define(Monitor.is, Monitor);
