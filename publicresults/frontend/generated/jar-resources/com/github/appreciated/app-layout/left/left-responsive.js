/*
@license
Copyright (c) 2015 The Polymer Project Authors. All rights reserved.
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

class AppLayoutLeftResponsive extends ElementMixin(ThemableMixin(PolymerElement)) {
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
                background: var(--app-layout-bar-background-base-color);
                top: 0px;
                height: var(--app-layout-bar-height);
            }

            app-toolbar {
                height: var(--app-layout-bar-height);
                background: var(--app-layout-bar-background-color);
                box-shadow: var(--app-layout-bar-shadow);
                color: var(--app-layout-bar-font-color);
                padding: var(--app-layout-bar-padding);
            }

            #app-bar-elements {
                min-width: 0;
            }

            /* non-overlay */
            :root {
                --app-drawer-scrim-background: rgba(0, 0, 0, 0);
            }

            app-toolbar {
                z-index: 1;
                position: absolute;
                top: 0px;
                left: 0px;
                right: 0px;
            }

            #drawer {
                margin-top: var(--app-layout-bar-height);
            }

            #drawer .drawer-content {
                height: 100%;
                width: 100%;
                background: var(--app-layout-drawer-background-color);
            }

            /* non-overlay */
            #drawer {
                --app-drawer-content-container: {
                    box-shadow: var(--app-layout-drawer-shadow);
                    background-color: var(--app-layout-drawer-background-base-color);
                    max-width: 100%;
                };
                transition: 1s width;
                --expand-icon-fill-color: var(--iron-icon-fill-color, currentcolor) !important;
            }

            #toggle {
                flex-shrink: 0;
                width: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
                height: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
                margin: var(--app-layout-space-s);
                min-width: calc(var(--app-layout-bar-height) - calc(var(--app-layout-space-s) * 2));
            }
            
            #toggle iron-icon {
               margin-right: var(--app-layout-bar-toggle-icon-right);
            }

            app-menu {
                background: transparent;
            }

            .application-content {
                width: 100%;
                padding-top: var(--app-layout-bar-height);
                box-sizing: border-box;
                background: var(--app-layout-background);
            }

            :host(:not([narrow])) #toggle:not(.show-back-arrow) {
                display: none;
            }
        </style>
        <app-header-layout id="layout-wrapper" fullbleed>
            <app-header fixed part="app-bar" slot="header">
                <app-toolbar style="height: var(--app-layout-bar-height);">
                    <vaadin-button id="toggle" theme="large tertiary icon">
                    </vaadin-button>
                    <div class="app-bar-content" id="app-bar-elements" style="width: 100%;height: 100%;">
                        <slot name="app-bar-content"></slot>
                    </div>
                </app-toolbar>
            </app-header>
            <app-drawer-layout id="drawerLayout" fullbleed>
                <app-drawer part="drawer" id="drawer" slot="drawer">
                    <div class="drawer-content" id="menu-elements">
                        <slot name="drawer-content"></slot>
                    </div>
                </app-drawer>
                <div class="application-content" part="application-content" id="content">
                    <slot name="application-content"></slot>
                </div>
            </app-drawer-layout>
        </app-header-layout>
        <iron-media-query query="[[_computeMediaQuery(responsiveWidth)]]" on-query-matches-changed="_onQueryMatchesChanged"></iron-media-query>`;
    }

    static get is() {
        return 'app-layout-left-responsive'
    }

    static get properties() {
        return {
            responsiveWidth: {
                type: String,
                value: "640px",
                observer: "_responsiveWidthChanged"
            },
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

customElements.define(AppLayoutLeftResponsive.is, AppLayoutLeftResponsive);