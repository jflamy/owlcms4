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

class AppLayoutLeftResponsiveHybridOverlayNoAppBar extends ElementMixin(ThemableMixin(PolymerElement)) {
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

            app-header-layout {
                z-index: 1;
            }

            app-toolbar {
                height: var(--app-layout-bar-height);
                background: var(--app-layout-bar-background-color);
                box-shadow: var(--app-layout-bar-shadow);
                color: var(--app-layout-bar-font-color);
                padding: var(--app-layout-bar-padding);
                position: absolute;
                top: 0px;
                left: 0px;
                right: 0px;
            }

            #app-bar-elements {
                min-width: 0;
            }

            #drawer {
                --app-drawer-content-container: {
                    box-shadow: var(--app-layout-drawer-shadow);
                    background-color: var(--app-layout-drawer-background-base-color);
                    max-width: 100%;
                };
                transition: 1s width;
            }

            #drawer .drawer-content {
                height: 100%;
                width: 100%;
                background: var(--app-layout-drawer-background-color);
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

            :host(:not([narrow])) app-header {
                display: none;
            }

            :host(:not([narrow])) #drawer {
                margin-top: 0px;
            }

            :host(:not([narrow])) app-menu-icon-item {
                transition: color 0.2s ease;
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

            #drawerLayout {
                --expand-icon-fill-color: var(--iron-icon-fill-color, currentcolor) !important;
            }

            app-menu {
                background: transparent;
            }

            :host(:not([narrow]):not([wide])) #drawerLayout {
                --app-drawer-width: var(--app-layout-drawer-small-width);
                --app-layout-badge-width: var(--app-layout-badge-small-width);
                --app-layout-badge-height: var(--app-layout-badge-small-height);
                --app-layout-badge-top: var(--app-layout-badge-small-top);
                --app-layout-badge-right: var(--app-layout-badge-small-right);
                --app-layout-badge-color: transparent;
                --app-layout-menu-button-margin: 0;
                --app-layout-menu-button-padding: 0 11px;
                --app-layout-bar-padding: var(--app-layout-bar-padding-small);
            }

            :host(:not([narrow]):not([wide])) #drawerLayout .app-menu-item {
                overflow: hidden;
            }

            :host(:not([narrow]):not([wide])) #drawerLayout * {
                --expand-icon-fill-color: transparent !important;
                --app-layout-badge-font-color: transparent;
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
        <iron-media-query query="[[_computeNarrowMediaQuery(responsiveWidthNarrow)]]" on-query-matches-changed="_onNarrowQueryMatchesChanged"></iron-media-query>
        <iron-media-query query="[[_computeWideMediaQuery(responsiveWidthWide)]]" on-query-matches-changed="_onWideQueryMatchesChanged"></iron-media-query>`;
    }

    static get is() {
        return 'app-layout-left-responsive-hybrid-overlay-no-app-bar'
    }

    static get properties() {
        return {
            responsiveWidthNarrow: {
                type: String,
                value: "640px"
            },
            responsiveWidthWide: {
                type: String,
                value: "1024px"
            }
        };
    }

    static get observers() {
        return [
            "_responsiveWidthChanged(responsiveWidthNarrow, responsiveWidthWide)"
        ];
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
        this.shadowRoot.querySelector("#drawerLayout").responsiveWidth = this.responsiveWidthNarrow;
    }

    _onNarrowQueryMatchesChanged(event) {
        if (event.detail.value)
            this.setAttribute("narrow", "");
        else
            this.removeAttribute("narrow");
    }

    _onWideQueryMatchesChanged(event) {
        if (event.detail.value)
            this.setAttribute("wide", "");
        else
            this.removeAttribute("wide");
    }

    _computeNarrowMediaQuery(responsiveWidthNarrow) {
        return "(max-width: " + responsiveWidthNarrow + ")";
    }

    _computeWideMediaQuery(responsiveWidthWide) {
        return "(min-width: " + responsiveWidthWide + ")";
    }
}

customElements.define(AppLayoutLeftResponsiveHybridOverlayNoAppBar.is, AppLayoutLeftResponsiveHybridOverlayNoAppBar);