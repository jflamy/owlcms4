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
          width: 75%; /* Set width to 75% of parent */
          margin-left: auto; /* Use auto margins for centering */
          margin-right: auto; /* Use auto margins for centering */
          justify-self: center; /* Helps centering in grid containers */
          overflow: hidden;
          white-space: nowrap;
          border: 1px solid #ccc;
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
        this.speed = 10; // Default animation duration in seconds
      }

      render() {
        return html`
          <div class="css-ticker-container">
            <div class="css-ticker-wrapper" id="cssTickerWrapper" style="--animation-duration: ${this.speed}s">
              <div class="css-ticker-text original-css">${this.text}</div>
              <div class="css-ticker-text css-duplicate">${this.text}</div>
            </div>
          </div>
        `;
      }

      updated(changedProperties) {
        super.updated(changedProperties);
        
        if (changedProperties.has('text')) {
          // Need to wait for the DOM to be updated
          this.updateComplete.then(() => {
            this._setWrapperWidth();
          });
        }
      }

      _setWrapperWidth() {
        const cssTickerWrapper = this.shadowRoot.getElementById('cssTickerWrapper');
        const originalCssText = this.shadowRoot.querySelector('.original-css');
        
        if (cssTickerWrapper && originalCssText) {
          // Use getBoundingClientRect for potentially more accurate width, especially with Unicode
          const textRect = originalCssText.getBoundingClientRect();
          const textWidth = textRect.width;
          
          // Ensure width is positive before setting
          if (textWidth > 0) {
            cssTickerWrapper.style.width = (textWidth * 2) + 'px';
          } else {
            // Fallback or wait if width is zero (e.g., element not fully rendered)
            // Requesting another frame might help
            requestAnimationFrame(() => this._setWrapperWidth());
          }
        }
      }

      firstUpdated() {
        // Set initial width after first render
        // Use requestAnimationFrame to ensure styles are applied and measurable
        requestAnimationFrame(() => this._setWrapperWidth());
      }
    }

    customElements.define('css-ticker', CssTicker);