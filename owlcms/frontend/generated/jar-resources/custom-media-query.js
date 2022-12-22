
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-media-query/iron-media-query.js';

// Extend the PolymerElement base class
class CustomMediaQuery extends PolymerElement {


    /**
     * Implement `render` to define a template for your element.
     *
     * You must provide an implementation of `render` for any element
     * that uses PolymerElement as a base class.
     */
    static get template() {
        return html`
        <iron-media-query query="{{query}}" query-matches="{{querymatches}}"></iron-media-query>
    `;
    }
    static get is() {
        return 'custom-media-query'
    }

    static get properties() {
        return {

            /**
             * The Boolean return value of the media query.
             */
            querymatches: {
                type: Boolean,
                notify: true
            },

            /**
             * The CSS media query to evaluate.
             */
            query: {
                type: String,
                notify: true
            }
        }
    }
    ready() {
        this.addEventListener('query-changed', function (ev) {
            this.dispatchEvent(new CustomEvent('querymatches', {}));
        });
        super.ready();
    }


}
// Register the new element with the browser.
customElements.define(CustomMediaQuery.is, CustomMediaQuery);

export { CustomMediaQuery };