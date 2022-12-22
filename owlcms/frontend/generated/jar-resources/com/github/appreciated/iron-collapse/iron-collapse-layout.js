import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-collapse/iron-collapse.js';

class IronCollapseLayout extends PolymerElement {
    static get is() {
        return 'iron-collapse-layout'
    }

    static get template() {
        return html`
        <div id="toggle">
            <slot></slot>
        </div>
        <iron-collapse id="collapse">
            <slot name="collapsible"></slot>
        </iron-collapse>
            `;
    }

    connectedCallback() {
        super.connectedCallback();
        var root = this.shadowRoot;
        var rootElement = this;
        root.querySelector("#toggle").onclick = function () {
            root.querySelector("#collapse").toggle();
            if (root.querySelector("#collapse").opened) {
                rootElement.toggleAttribute("opened");
            } else {
                rootElement.removeAttribute("opened");
            }
        }
    }
}

customElements.define(IronCollapseLayout.is, IronCollapseLayout);