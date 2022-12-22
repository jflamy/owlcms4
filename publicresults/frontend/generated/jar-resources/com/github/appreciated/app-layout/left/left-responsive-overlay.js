/*
@license
Copyright (c) 2016 The Polymer Project Authors. All rights reserved.
This code may only be used under the BSD style license found at http://polymer.github.io/LICENSE.txt
The complete set of authors may be found at http://polymer.github.io/AUTHORS.txt
The complete set of contributors may be found at http://polymer.github.io/CONTRIBUTORS.txt
Code distributed by Google as part of the polymer project is also
subject to an additional IP rights grant found at http://polymer.github.io/PATENTS.txt
*/
import {html, PolymerElement} from '@polymer/polymer';
import {ThemableMixin} from '@vaadin/vaadin-themable-mixin';
import {ElementMixin} from '@vaadin/vaadin-element-mixin';
import '@vaadin/vaadin-themable-mixin/vaadin-themable-mixin.js';
import '@vaadin/vaadin-icons/vaadin-icons.js';
import '@polymer/app-layout/app-drawer/app-drawer.js';
import '@polymer/app-layout/app-drawer-layout/app-drawer-layout.js';
import '@polymer/app-layout/app-header/app-header.js';
import '@polymer/app-layout/app-header-layout/app-header-layout.js';
import '@polymer/app-layout/app-toolbar/app-toolbar.js';
import '@polymer/iron-media-query/iron-media-query.js';

class AppLayoutLeftResponsiveOverlay extends ElementMixin(ThemableMixin(PolymerElement)) {

    static get template() {
        return html`<style>
            :root {
                --paper-badge-background: var(--app-layout-badge-background);
                --paper-badge-text-color: var(--app-layout-badge-font-color);
                --primary-color: var(--app-layout-primary-color);
            }

            slot[name="app-bar-content"]::slotted(*) {
                pointer-events: inherit;
            }

            app-header {
                right: 0px !important;
                /* Since the app-bar is visually behind the drawer its background needs to be darker */
                background: var(--app-layout-drawer-background-base-color);
                top: 0px;
                height: var(--app-layout-bar-height);
            }

            app-header-layout {
                z-index: 1;
            }

            app-toolbar {
                height: var(--app-layout-bar-height);
                /* Since the app-bar is visually behind the drawer its background needs to be darker */
                background: var(--app-layout-drawer-background-color);
                box-shadow: var(--app-layout-bar-shadow);
                color: var(--app-layout-bar-font-color);
                padding: var(--app-layout-bar-padding);
                position: absolute;
                top: 0px;
                left: 0px;
                right: 0px;
                z-index: 2;
            }

            #app-bar-elements {
                min-width: 0;
            }

            .application-content {
                width: 100%;
                background: var(--app-layout-background);
            }

            :host([narrow]) .application-content {
                margin-top: calc(var(--app-layout-bar-height) * -1);
                padding-top: var(--app-layout-bar-height);
                box-sizing: border-box;
            }

            app-menu {
                background: transparent;
            }

            #drawer {
                --app-drawer-content-container: {
                    box-shadow: var(--app-layout-drawer-shadow);
                    /* Since the drawer is visually in front of the app-bar its background needs to be lighter */
                    background-color: var(--app-layout-bar-background-base-color);
                    max-width: 100%;
                };
                transition: 1s width;
                --expand-icon-fill-color: var(--iron-icon-fill-color, currentcolor) !important;
            }

            #drawer .drawer-content {
                height: 100%;
                width: 100%;
                /* Since the drawer is visually in front of the app-bar its background needs to be lighter */
                background: var(--app-layout-bar-background-color);
            }

            #toggle {
                flex-shrink: 0;
                width: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
                height: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
                margin: var(--app-layout-space-s);
                min-width: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
            }

            :host(:not([narrow])) #toggle:not(.show-back-arrow) {
                display: none;
            }
        </style>
        <app-drawer-layout id="drawerLayout" fullbleed>
            <app-header-layout id="layout-wrapper">
                <app-header part="app-bar" fixed slot="header">
                    <app-toolbar style="height: var(--app-layout-bar-height);">
                        <vaadin-button id="toggle" theme="large tertiary icon">
                        </vaadin-button>
                        <div class="app-bar-content" id="app-bar-elements" style="width: 100%;height: 100%;">
                            <slot name="app-bar-content"></slot>
                        </div>
                    </app-toolbar>
                </app-header>
            </app-header-layout>
            <app-drawer part="drawer" id="drawer" slot="drawer">
                <div class="drawer-content" id="menu-elements">
                    <slot name="drawer-content"></slot>
                </div>
            </app-drawer>
            <div class="application-content" part="application-content" id="content">
                <slot name="application-content"></slot>
            </div>
        </app-drawer-layout>
        <iron-media-query query="[[_computeMediaQuery(responsiveWidth)]]" on-query-matches-changed="_onQueryMatchesChanged"></iron-media-query>`;
    }

    static get is() {
        return 'app-layout-left-responsive-overlay'
    }

    static get properties() {
        return {
            responsiveWidth: {
                type: String,
                value: "640px",
                observer: "_responsiveWidthChanged"
            }
        };
    }

    ready() {
        super.ready();
        this.shadowRoot.querySelector("#toggle").addEventListener('click', evt => this.onclick());
    }

    onclick() {
        var drawer = this.shadowRoot.querySelector("#drawer");
        if (!this.shadowRoot.querySelector("#toggle").classList.contains('show-back-arrow')) {
            drawer.toggle();
        } else {
            this.onUpNavigation();
        }
    }

    onUpNavigation() {
    }

    closeIfNotPersistent() {
        var drawer = this.shadowRoot.querySelector("#drawer");
        if (!drawer.persistent) {
            drawer.close();
        }
    }

    _responsiveWidthChanged() {
        this.shadowRoot.querySelector("#drawerLayout").responsiveWidth = this.responsiveWidth;
    }

    _onQueryMatchesChanged(event) {
        if (event.detail.value)
            this.setAttribute("narrow", "");
        else
            this.removeAttribute("narrow");
    }

    _computeMediaQuery(responsiveWidth) {
        return "(max-width: " + responsiveWidth + ")";
    }
}

customElements.define(AppLayoutLeftResponsiveOverlay.is, AppLayoutLeftResponsiveOverlay);