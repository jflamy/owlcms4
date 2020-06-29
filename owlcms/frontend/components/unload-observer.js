import {html, PolymerElement} from '@polymer/polymer/polymer-element.js';

/**
 * A web component that wraps Intersection Observer object and broadcasts events about changes.
 * This requires Flow and a corresponding server-side Java component to work properly.
 * Alternatively, make sure that this.$server.componentStatusChanged(String, double) is available.
 */
export class ComponentObserver extends PolymerElement {

    static get template() {
        return html``;
    }

    static get is() {
        return 'component-observer';
    }

    /**
     * Initialises this object.
     * @param rootElement Root element to use.
     * @param rootMarginString Root margin string.
     * @param range Ranges to listen to.
     */
    initObserver(rootElement, rootMarginString, range) {
        this.observer = new IntersectionObserver( (changes, observerObject) => changes.forEach(change => {
            console.log('observed visibility change to '+change.intersectionRatio+' with key '+change.target.dataset.observerIndex);
            this.$server.componentStatusChanged(change.target.dataset.observerIndex, change.intersectionRatio);
        }), {root: rootElement, rootMargin: rootMarginString, threshold: range});
    }

    /**
     * Starts observing.
     * @param what What to observe (element).
     * @param key What is the key to report to the server-side.
     */
    // using index and dataset is a workaround for https://github.com/vaadin/flow/issues/6372
    observe(what, key) {
        what.dataset.observerIndex = key; // using html data-* attributes
        console.log('observing '+what+' with key '+key);
        this.observer.observe(what);
    }

    /**
     * Stops observing.
     * @param what What to stop observing (element).
     */
    unobserve(what) {
        delete what.dataset.observerIndex; // clears data-* attribute
        // on detach it seems that observer gets undefined
        // since components get unobserved on detach as well, this is a safeguard
        if (this.observer) {
            this.observer.unobserve(what);
        }
    }

}

customElements.define(ComponentObserver.is, ComponentObserver);