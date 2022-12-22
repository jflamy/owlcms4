import {html, PolymerElement} from '@polymer/polymer';
import {mixinBehaviors} from '@polymer/polymer/lib/legacy/class.js';
import {IronOverlayBehavior} from '@polymer/iron-overlay-behavior/iron-overlay-behavior.js';

class IronOverlayWrapper extends mixinBehaviors(IronOverlayBehavior, PolymerElement) {

    static get template() {
        return html`
      <style>
        :host {
           margin-top: unset !important;
        }
      </style> 
      <slot></slot>
    `;
    }

    _openedChanged(opened) {
        super._openedChanged(opened);
        if (opened) {
            document.body.style.overflow = "hidden";
        } else {
            document.body.style.overflow = null;
        }
    }
}

customElements.define('iron-overlay-wrapper', IronOverlayWrapper);
