import { html, LitElement, css } from "lit";

/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

export class JuryDecisions extends LitElement {
  static get is() {
    return "jurydecisions-template";
  }

  static get properties() {
    return {
      decisions: { type: Array },
      showJuryDecisions: { type: Boolean },
    };
  }

  constructor() {
    super();
    this.showDecisions = false;
  }

  render() {
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "")}.css"/>
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/jurydecisions" + (this.autoversion ?? "")}.css"/>

      <div class="jury-decisions-wrapper" style="${this.colorOverride}">
        <div class="jury-decisions ${this.showJuryDecisions ? "shown" : "hidden"}">
            <div class="decisions">
              ${this.decisions.map(
                d => html`
                  <div class="decision ${d}">
                    ${d === "white" ? "✔" : d === "red" ? "✖" : d === "waiting" ? "?" : ""}
                  </div>
                `
              )}
            </div>
          </div>
        </div>
      </div>
    `;
  }
}

customElements.define(JuryDecisions.is, JuryDecisions);
