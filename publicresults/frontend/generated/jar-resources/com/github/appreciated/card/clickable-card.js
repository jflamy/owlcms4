import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';

class ClickableCard extends PolymerElement {

    static get template() {
        return html`
<style>
    #card-content {
        transition: box-shadow 0.35s ease, background-color 0.35s ease;
        box-shadow: 0 0 0 0 rgba(0, 0, 0, 0), 0 0 0 0 rgba(0, 0, 0, 0);
        border-radius: var(--lumo-border-radius);
        overflow: hidden;
        position: relative;
        user-select: none;
        -webkit-tap-highlight-color: transparent;
        background-color: rgba(255, 255, 255, 0.06);
        width: 100%;
        height: 100%;
    }

    #card-content.touch {
        transition: box-shadow 0.1s ease;
    }

    #card-content[elevation="1"] {
        box-shadow: var(--lumo-box-shadow-s);
    }

    #card-content[elevation="2"] {
        box-shadow: var(--lumo-box-shadow-m);
    }

    #card-content[elevation="3"] {
        box-shadow: var(--lumo-box-shadow-l);
    }

    #card-content:not(.no-elevate).no-touch:hover {
        box-shadow: var(--lumo-box-shadow-s);
    }

    #card-content.no-touch:hover {
        background-color: var(--lumo-contrast-5pct);
    }

    #card-content[elevation="1"]:not(.no-elevate).no-touch:hover {
        box-shadow: var(--lumo-box-shadow-m);
    }

    #card-content[elevation="2"]:not(.no-elevate).no-touch:hover {
        box-shadow: var(--lumo-box-shadow-l);
    }

    #card-content[elevation="3"]:not(.no-elevate).no-touch:hover {
        box-shadow: var(--lumo-box-shadow-xl);
    }

    #card-content:not(.no-elevate).touch:not(.no-elevate):active {
        box-shadow: var(--lumo-box-shadow-s);
    }

    #card-content.touch:not(.no-elevate):active {
        background-color: var(--lumo-contrast-5pct);
    }

    #card-content[elevation="1"]:not(.no-elevate).touch:active {
        box-shadow: var(--lumo-box-shadow-m);
    }

    #card-content[elevation="2"]:not(.no-elevate).touch:active {
        box-shadow: var(--lumo-box-shadow-l);
    }

    #card-content[elevation="3"]:not(.no-elevate).touch:active {
        box-shadow: var(--lumo-box-shadow-xl);
    }
</style>
<div id="card-content">
    <slot></slot>
</div>`;
    }

    static get is() {
        return 'clickable-card'
    }

    static get properties() {
        return {elevationEnabled: Boolean}
    }

    ready() {
        super.ready();
        if ("ontouchstart" in document.documentElement) {
            this.shadowRoot.querySelector("#card-content").classList.add('touch');
        } else {
            this.shadowRoot.querySelector("#card-content").classList.add('no-touch');
        }
        if (this.elevationEnabled === true) {
            this.shadowRoot.querySelector("#card-content").classList.remove('no-elevate');
        } else {
            this.shadowRoot.querySelector("#card-content").classList.add('no-elevate');
        }
    }

    enableElevate(enable) {
        this.elevationEnabled = enable;
    }

    preventElementEventPropagation(element) {
        element.addEventListener('down', function (ev) {
            ev.stopPropagation();
        })
        element.addEventListener('click', function (ev) {
            ev.stopPropagation();
        })
    }
}

customElements.define(ClickableCard.is, ClickableCard);