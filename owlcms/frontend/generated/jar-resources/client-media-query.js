import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';
import '@polymer/iron-media-query/iron-media-query.js';

// Extend the PolymerElement base class
class ClientMediaQuery extends PolymerElement {


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
        return 'client-media-query'
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
                type: String
            },

            /**
             * The CSS this should be applied to the element
             */
            queryCss: {
                type: String
            },

            /**
             * The element to which the styles should be applied
             */
            element: {
                type: Object
            }
        }
    }
    ready() {
        super.ready();
        this.addEventListener('querymatches-changed', this.tryApplyQueryCss);
    }

    tryApplyQueryCss(){
        if (this.querymatches && this.queryCss != null && this.element != null) {
            var mediaQueryCss = JSON.parse(this.queryCss);
            for (var x in mediaQueryCss) {
                this.element.style[x] = mediaQueryCss[x];
            }
        }
    }

    setElement(element) {
        this.element = element;
        if (this.querymatches){
            this.tryApplyQueryCss();
        }
    }


}
// Register the new element with the browser.
customElements.define(ClientMediaQuery.is, ClientMediaQuery);

export { ClientMediaQuery };