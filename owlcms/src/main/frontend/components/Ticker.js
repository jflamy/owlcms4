import { LitElement, html, css } from 'lit';

/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

class CssTicker extends LitElement {
  static get is() { return 'css-ticker'; }
  static get elementName() { return 'css-ticker'; }
  static properties = {
    text: { type: String },
    speed: { type: Number }
  };

  static styles = css`
        :host {
          display: block;
          width: 100%; 
          margin-left: auto; /* Use auto margins for centering */
          margin-right: auto; /* Use auto margins for centering */
          justify-self: center; /* Helps centering in grid containers */
          overflow: hidden;
          white-space: nowrap;
          padding: 5px;
          box-sizing: border-box; /* Ensure padding/border are included in width */
        }

        .css-ticker-container {
          width: 100%; /* Container takes full width of the host */
          overflow: hidden;
          white-space: nowrap;
        }

        .css-ticker-wrapper {
          display: inline-block;
          animation: cssTicker var(--animation-duration, 10s) linear infinite;
        }

        .css-ticker-text {
          display: inline-block;
          white-space: nowrap;
        }

        .css-duplicate {
          margin-left: 0;
        }

        @keyframes cssTicker {
          0% { transform: translateX(0); }
          100% { transform: translateX(-50%); }
        }
      `;

  constructor() {
    super();
    this.text = '';
    this.speed = 10;
  }

  updated(changedProperties) {
    super.updated(changedProperties);
    if (changedProperties.has('text') || changedProperties.has('speed')) {
      //console.log("Ticker: updated with text:", this.text, "speed:", this.speed);
      this.updateComplete.then(() => {
        this._setWrapperWidth();
      });
    }
  }

  render() {
    console.log("Ticker: render with text:", this.text, "speed:", this.speed);
    return html`
          <div class="css-ticker-container">
            <div class="css-ticker-wrapper" id="cssTickerWrapper" style="--animation-duration: ${this.speed}s">
              <div class="css-ticker-text original-css">${this.text}</div>
              <div class="css-ticker-text css-duplicate">${this.text}</div>
            </div>
          </div>
        `;
  }

  _setWrapperWidth() {
    const cssTickerWrapper = this.shadowRoot.getElementById('cssTickerWrapper');
    const originalCssText = this.shadowRoot.querySelector('.original-css');

    if (cssTickerWrapper && originalCssText) {
      const textRect = originalCssText.getBoundingClientRect();
      const textWidth = textRect.width + 20; // Buffer

      if (textWidth > 20) { // Check against buffer value
        cssTickerWrapper.style.width = (textWidth * 2) + 'px';

        // Calculate viewport ratio relative to 1920px baseline
        const baselineWidth = 1920;
        const currentWidth = window.innerWidth;
        const ratio = currentWidth / baselineWidth;

        // Adjust the base speed (duration). Wider viewport = faster animation (shorter duration).
        // Ensure ratio is not zero to avoid division by zero.
        const adjustedSpeed = ratio > 0 ? this.speed / ratio : this.speed;

        // Update the CSS variable with the adjusted speed
        cssTickerWrapper.style.setProperty('--animation-duration', `${adjustedSpeed}s`);

        // When resetting animation, ensure it uses the CSS variable
        cssTickerWrapper.style.animation = 'none';
        void cssTickerWrapper.offsetWidth; // Force reflow
        // Use the updated CSS variable value directly in the animation definition
        cssTickerWrapper.style.animation = `cssTicker ${adjustedSpeed}s linear infinite`;

      } else {
        requestAnimationFrame(() => this._setWrapperWidth());
      }
    }
  }

  _handleResize() {
    // Recalculate width and speed on resize
    this._setWrapperWidth();
  }

  connectedCallback() {
    super.connectedCallback();
    // Bind the resize handler context
    this._boundHandleResize = this._handleResize.bind(this);
    window.addEventListener('resize', this._boundHandleResize);
  }

  disconnectedCallback() {
    super.disconnectedCallback();
    window.removeEventListener('resize', this._boundHandleResize);
  }

  firstUpdated() {
    // Set initial width after first render
    // Use requestAnimationFrame to ensure styles are applied and measurable
    requestAnimationFrame(() => this._setWrapperWidth());
  }
}

customElements.define('css-ticker', CssTicker);