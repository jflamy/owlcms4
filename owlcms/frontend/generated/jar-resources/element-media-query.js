import { LitElement, html } from 'lit-element';
import elementMatchMedia from 'element-match-media';

/**
 * The original code comes from https://gist.github.com/ebidel/3a42415ee1bb9d44020b159270382eba
 * And has been changed
 */
const ResizeObservableElement = (superclass) => class extends superclass {
    static get observer() {
        if (!this._observer) {
            // Set up a single RO for all elements that inherit from this class. This
            // has much better performance than creating a separate RO in every
            // element instance. See https://goo.gl/5uLKZN.
            this._observer = new ResizeObserver(entries => {
                window.requestAnimationFrame(() => {
                    if (!Array.isArray(entries) || !entries.length) {
                        return;
                    }
                    // your code
                    for (const entry of entries) {
                        // Custom event works for both node.onresize and node.addEventListener('resize') cases.
                        const evt = new CustomEvent('resize', {detail: entry, bubbles: false})
                        entry.target.dispatchEvent(evt);
                    }
                });
            });
        }
        return this._observer;
    }

    constructor() {
        super();
    }
};

class ElementMediaQuery extends ResizeObservableElement(LitElement) {
    static get properties() {
        return {
            /**
             * The CSS media query to evaluate.
             */
            query: {
                type: String
            },

            querymatches: {
                type: Boolean
            },
            /**
             * The element to which the styles should be applied
             */
            element: {
                type: Object
            },
            _elementMatchMedia: {
                type: Object
            }
        };
    }

    static get is() { return 'element-media-query'; }

    constructor() {
        super();
    }
    setElement(element) {
        this.element = element;
        this.constructor.observer.observe(element);

        element.addEventListener('resize', e => {
            let matches = elementMatchMedia(this.element, this.query).matches;
            if (this.querymatches !== matches) {
                this.querymatches = matches;
                this.$server.querymatchesChanged(this.querymatches);
            }
        });
    }

    setQuery(query) {
        this.query = query;
    }

    connectedCallback() {
        super.connectedCallback();
        this.querymatches = elementMatchMedia(this.element, this.query).matches;
        this.$server.querymatchesChanged(this.querymatches);
    }

    disconnectedCallback() {
        this.constructor.observer.unobserve(this.element);
    }
}

customElements.define(ElementMediaQuery.is, ElementMediaQuery);