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
    return html`<audio preload="auto" id="beeper" src="../local/sounds/beepBeep.mp3">`;
  }

  static get properties() {
    return {
      /**
       * Set to true to have timer not emit sounds
       *
       * @default false
       */
      doBeep: {
        type: Boolean,
      }
    };
  }

  firstUpdated(_changedProperties) {
    super.firstUpdated(_changedProperties);
    this.beeper = this.renderRoot?.querySelector('#beeper');
  }

  updated(changes) {
    console.warn(changes+" doBeep ="+this.doBeep);
    if (changes.has('doBeep') && this.doBeep) {
      this.beep();
    }
  }

  beep() {
    console.warn("beep");
    this.beeper.volume = 1;
    this.beeper.play();
    this.doBeep = false; // will be reset from server side.
  }

  initSounds() {
    this.beeper.volume = 0;
    this.beeper.play();
  }

  constructor() {
    console.warn("constructor");
    super();
    this.doBeep = false;
    this.beep = this.beep.bind(this);
    this.initSounds = this.initSounds.bind(this);
  }
}

customElements.define(BeepElement.is, BeepElement);
