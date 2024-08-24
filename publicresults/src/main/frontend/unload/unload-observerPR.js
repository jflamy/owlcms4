import { html, LitElement, css } from "lit";
/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

/**
 * A web component that listens to {@code beforeunload} events and sends them to server-side.
 * This requires Flow and a corresponding server-side Java component to work properly.
 * Alternatively, make sure that this.$server.visibilityChange() is available.
 * Based on the code written by Kaspar Scherrer and Stuart Robinson: https://vaadin.com/forum/thread/17523194/unsaved-changes-detect-page-exit-or-reload
 *
 * author: miki@vaadin.com
 */
export class UnloadObserver extends LitElement {
  render() {
    return html`
<form id="reloadForm" action="../expired" method="post" style="display: none;">
  <input type="text" id="reloadTitle" name="reloadTitle" value="${this.reloadTitle}">
  <input type="text" id="reloadText" name="reloadText" value="${this.reloadText}">
  <input type="text" id="reloadLabel" name="reloadLabel" value="${this.reloadLabel}">
  <input type="text" id="reloadUrl" name="reloadUrl" value="${this.reloadUrl}">
</form>`;
  }

  static get is() {
    return "unload-observer-pr";
  }

  static get properties() {
    return {
      reloadTitle: {},
      reloadText: {},
      reloadLabel: {},
      reloadUrl: {},
    }
  }

  updated(changedProperties) {
    console.warn("changed properties {} ", changedProperties)
    if (changedProperties.has('reloadUrl')) {
      this.postReload();
    }
  }

  postReload() {
    let form = this.shadowRoot.querySelector('#reloadForm');
    console.warn("reload page, target location "+this.reloadUrl+" ");
    form.submit();
  }

  /**
   * Initialises the observer and registers a beforeunload listener.
   */
  initObserver() {
    const src = this;
    if (window.Vaadin.unloadObserver === undefined) {
      window.Vaadin.unloadObserver = {
        attemptHandler: undefined,
      };
    }
    if (window.Vaadin.unloadObserver.attemptHandler !== undefined) {
      window.removeEventListener(
        "beforeunload",
        window.Vaadin.unloadObserver.attemptHandler
      );
      window.removeEventListener(
        "unload",
        window.Vaadin.unloadObserver.unloadHandler
      );
      window.removeEventListener(
        "pagehide",
        window.Vaadin.unloadObserver.unloadHandler
      );
      document.removeEventListener(
        "visibilitychange",
        window.Vaadin.unloadObserver.visibilityHandler
      );
      window.removeEventListener(
        "blur",
        window.Vaadin.unloadObserver.blurHandler
      );
      window.removeEventListener(
        "focus",
        window.Vaadin.unloadObserver.focusHandler
      );
    }

    window.Vaadin.unloadObserver.attemptHandler = (event) => {
      src.visibilityChange(src, event, "beforeunload")
    };

    window.Vaadin.unloadObserver.unloadHandler = (event) => {
      src.visibilityChange(src, event, "unload")
    };

    window.Vaadin.unloadObserver.hideHandler = (event) =>
      src.visibilityChange(src, event, "pagehide");

    window.Vaadin.unloadObserver.visibilityHandler = (event) => 
      {
        console.warn("visibility "+document.visibilityState);
        if (document.visibilityState === 'hidden') {
          src.visibilityChange(src, event, "visibilityHidden");
        } else {
          src.visibilityChange(src, event, "visibilityShown");
        }
      }

    window.Vaadin.unloadObserver.blurHandler = (event) => 
      {
        console.warn("blur "+document.visibilityState);
        src.visibilityChange(src, event, "blur");
      }

      window.Vaadin.unloadObserver.focusHandler = (event) => 
        {
          console.warn("focus "+document.visibilityState);
          src.visibilityChange(src, event, "focus");
        }

    window.addEventListener(
      "beforeunload",
      window.Vaadin.unloadObserver.attemptHandler
    );
    window.addEventListener(
      "unload",
      window.Vaadin.unloadObserver.unloadHandler
    );
    window.addEventListener(
      "pagehide",
      window.Vaadin.unloadObserver.hideHandler
    );
    window.addEventListener(
      "blur",
      window.Vaadin.unloadObserver.blurHandler
    );
    window.addEventListener(
      "focus",
      window.Vaadin.unloadObserver.focusHandler
    );
    document.addEventListener(
      "visibilitychange",
      window.Vaadin.unloadObserver.visibilityHandler
    );

  }

  /**
   * Invoked in response to beforeunload browser event.
   * @param source An unload observer.
   * @param event Event that happened.
   */
  visibilityChange(source, event, change) {
    if (window.Vaadin.unloadObserver.query) {
      console.warn("UO: responding to unload attempt...");
      event.preventDefault();
      event.returnValue = "";
      if (source.$server) {
        source.$server.visibilityChange(change);
      }
    } else {
      source.$server.unloadHappened(change);
    }
  }

  /**
   * Controls whether or not prevent unload events.
   * @param value When {@code truthy} (recommended String "true"), unload event will be prevented.
   */
  queryOnUnload(value) {
    if (value) {
      window.Vaadin.unloadObserver.query = "true";
    } else {
      delete window.Vaadin.unloadObserver.query;
    }
  }

}

customElements.define(UnloadObserver.is, UnloadObserver);
