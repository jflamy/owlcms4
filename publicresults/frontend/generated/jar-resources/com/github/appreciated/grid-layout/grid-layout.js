import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';

class GridLayoutElement extends PolymerElement {
    static get is() {
        return 'grid-layout';
    }

    static get template() {
        return html`
<style>
    :host {
        display: flex;
        flex-direction: column;
    }

    :host #grid-layout-element {
        display: grid;
        box-sizing: border-box;
        flex: 1 0;
    }

    :host:not([overflow]) {
        width: inherit;
        height: inherit;
    }

    :host([hidden]) {
        display: none !important;
    }

    :host([theme~="margin"]) {
        margin: 1em;
    }

    :host([theme~="padding"]) #grid-layout-element {
        padding: 1em 1em 0 1em;
    }

    :host([theme~="margin"]) #grid-layout-element {
        margin: var(--lumo-space-m);
    }

    :host([theme~="padding"]) #grid-layout-element {
        padding: var(--lumo-space-m);
    }

    :host([theme~="spacing"]) #grid-layout-element {
        grid-gap: 1em;
    }

    :host([theme~="spacing-xs"]) #grid-layout-element {
        grid-gap: var(--lumo-space-xs);
    }

    :host([theme~="spacing-s"]) #grid-layout-element {
        grid-gap: var(--lumo-space-s);
    }

    :host([theme~="spacing"]) #grid-layout-element {
        grid-gap: var(--lumo-space-m);
    }

    :host([theme~="spacing-l"]) #grid-layout-element {
        grid-gap: var(--lumo-space-l);
    }

    :host([theme~="spacing-xl"]) #grid-layout-element {
        grid-gap: var(--lumo-space-xl);
    }
</style>
<div id="grid-layout-element">
    <slot></slot>
</div>
<div id="queries"></div>
<div id="overflow-helper"></div>`;
    }
}

customElements.define(GridLayoutElement.is, GridLayoutElement);