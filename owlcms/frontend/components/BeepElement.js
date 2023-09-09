import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class BeepElement extends LitElement {
  static get is() {
    return "beep-element";
  }

  render() {
    return html`<audio preload="auto" id="beeper" src="../local/sounds/beepBeep.mp3"></audio>`;
  }

  static get properties() {
    return {
      /**
       * Set to true to have timer not emit sounds
       *
       * @default false
       */
      silent: {
        type: Boolean,
      },
      doBeep: {
        type: Boolean,
      }
    };
  }

  updated(changes) {
    if (changes.has('doBeep') && this.doBeep) {
      this.beep();
    }
  }

  beep() {
    console.warn("beep called");
    this.renderRoot?.querySelector('#beeper').play();
    this.doBeep = false; // will be reset from server side.
  }

  constructor() {
    super();
    this.silent = false;
    this.doBeep = true;
  }
}

customElements.define(BeepElement.is, BeepElement);
