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
    return html`<audio preload="auto" id="beeper" src="../local/sounds/beepBeep.mp3"></audio><button type='button' id="enabler">Enable Sound</button>`;
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
    this.enabler = this.renderRoot?.querySelector('#enabler');
    this.enabler.addEventListener('click', () => {
      const event = new Event("initSounds");
      document.dispatchEvent(event);
    })
  }

  updated(changes) {
    console.warn(changes+" doBeep ="+this.doBeep);
    if (changes.has('doBeep') && this.doBeep) {
      this.beep();
    }
  }

  connectedCallback() {
    super.connectedCallback();
    document.addEventListener('initSounds', this.initSounds);
  }

  disconnectedCallback() {
    document.removeEventListener('initSounds', this.initSounds);
    super.disconnectedCallback();
  }

  beep() {
    console.warn("beep");
    this.beeper.muted = false;
    this.beeper.play();
    this.doBeep = false; // will be reset from server side.
  }

  initSounds() {
    console.warn("initSound beep");
    this.renderRoot.querySelector('#beeper').muted = true;
    this.renderRoot.querySelector('#beeper').play();
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
