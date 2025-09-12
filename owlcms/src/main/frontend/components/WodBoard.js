import { html, LitElement, css } from "lit";

class WodBoard extends LitElement {
  static get is() {
    return "wod-board";
  }

  static get properties() {
    return {
      athletes: { type: Array },
      stylesDir: {},
      autoversion: {},
    };
  }

  constructor() {
    super();
    this.athletes = [
      { name: "", club: "" },
      { name: "", club: "" },
      { name: "", club: "" },
      { name: "", club: "" },
    ];
  }

  render() {
    // Defensive: always 4 slots
    const a = this.athletes || [];
    const [a1, a2, a3, a4] = [a[0] || {}, a[1] || {}, a[2] || {}, a[3] || {}];
    return html`
      <link rel="stylesheet" type="text/css" .href="${"local/" + (this.stylesDir ?? "") + "/colors" + (this.autoversion ?? "") + ".css"}" />
      <div class="timer-overlay">
        <div class="timer-frame">
          <timer-element id="breakTimer"></timer-element>
        </div>
      </div>
      <div class="wod-board-grid">
        <div class="athlete top left">
          <div class="athlete-name">${a1.name}</div>
          <div class="athlete-club">${a1.club}</div>
        </div>
        <div class="athlete top right">
          <div class="athlete-name">${a2.name}</div>
          <div class="athlete-club">${a2.club}</div>
        </div>
        <div class="athlete bottom left">
          <div class="athlete-name">${a3.name}</div>
          <div class="athlete-club">${a3.club}</div>
        </div>
        <div class="athlete bottom right">
          <div class="athlete-name">${a4.name}</div>
          <div class="athlete-club">${a4.club}</div>
        </div>
      </div>
    `;
  }

  static get styles() {
    return css`
      .wod-board-grid {
        position: relative;
        display: grid;
        grid-template-columns: 1fr 1fr;
        grid-template-rows: 1fr 1fr;
        gap: 2vh; /* space between the four colored frames */
        padding: 2vh; /* outer gap around the grid */
        box-sizing: border-box; /* include padding in width/height */
        height: 100vh;
        width: 100vw;
  /* Use the shared theme variable used across components */
  background: var(--pageBackgroundColor, black);
        align-items: center;
        justify-items: center;
      }
      .athlete {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        width: 100%;
        font-size: 2.3em;
        color: #fff;
        min-width: 10vw;
        min-height: 6vh;
        padding: 0.5em 1em;
        border-radius: 0.5em;
        background: rgba(0,0,0,0.2);
        box-sizing: border-box;
        overflow: hidden;
        text-align: center;
      }
      .top.left { border: 8px solid red; }
      .top.right { border: 8px solid white; }
      .bottom.left { border: 8px solid blue; }
      .bottom.right { border: 8px solid gold; }
      .athlete-name {
        font-weight: bold;
        font-size: 1.38em;
        text-align: center;
        width: 100%;
        display: block;
      }
      .athlete-club {
        font-size: 1.035em;
        opacity: 0.8;
        text-align: center;
        width: 100%;
        display: block;
      }
      .top.left, .top.right, .bottom.left, .bottom.right {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        height: 100%;
        width: 100%;
      }
      .timer-overlay {
        position: absolute;
        left: 50%;
        top: 50%;
        transform: translate(-50%, -50%);
        z-index: 10;
        display: flex;
        align-items: center;
        justify-content: center;;
        pointer-events: none;
      }
      .timer-frame {
        /* use an outer box-shadow to draw the gray rounded border while keeping
          the inner area fully black */
        border: none;
        border-radius: 1.0em; /* increased outer radius */
        padding: 0; /* inner element sits flush inside */
        background: #000000; /* inner area fully black */
        box-shadow: 0 0 0 6px #888888; /* gray border thickness */
        display: flex;
        align-items: center;
        justify-content: center;
        pointer-events: none;
        box-sizing: border-box;
      }
      timer-element#breakTimer {
        font-size: 6.19vw;
        color: #fff;
        background: transparent; /* let the frame supply the black background */
        border-radius: 0; /* inner element square; frame provides rounded corners */
        padding: 0.25em 0.35em; /* give inner breathing room for the digits */
        box-sizing: border-box;
        box-shadow: none;
        width: 20vw !important;
        height: 10.8vw;
        min-width: unset;
        max-width: unset;
        min-height: unset;
        max-height: unset;
        display: flex;
        align-items: center;
        justify-content: center;
        margin: 0 auto;
      }
    `;
  }
}

customElements.define(WodBoard.is, WodBoard);
