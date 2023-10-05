import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class SoundEnabler extends LitElement {
  static get is() {
    return "soundenabler-element";
  }

  render() {
    return html`<div class="soundEnabler" id="enabler">${this.caption}</button>`;
  }

  static get properties() {
    return {
      caption: {},
    };
  }

  createRenderRoot() {
    return this;
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    this.enabler = this.renderRoot?.querySelector('#enabler');
    this.enabler.addEventListener('click', () => {
      const event = new Event("initSounds");
      document.dispatchEvent(event);
      this.$server.soundEnabled();
    })
  }

  disconnectedCallback() {
    super.disconnectedCallback();
  }

  constructor() {
    super();
  }
}

customElements.define(SoundEnabler.is, SoundEnabler);
