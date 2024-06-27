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
 * Alternatively, make sure that this.$server.unloadAttempted() is available.
 * Based on the code written by Kaspar Scherrer and Stuart Robinson: https://vaadin.com/forum/thread/17523194/unsaved-changes-detect-page-exit-or-reload
 *
 * author: miki@vaadin.com
 */
export class UnloadObserver extends LitElement {
  render() {
    return html``;
  }

  static get is() {
    return "unload-observer";
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
        "pagehide",
        window.Vaadin.unloadObserver.hideHandler
      );
      document.removeEventListener(
        "visibilitychange",
        window.Vaadin.unloadObserver.visibilityHandler
      );
    }

    window.Vaadin.unloadObserver.attemptHandler = (event) => {
      src.unloadAttempted(src, event)
    };
    window.Vaadin.unloadObserver.hideHandler = (event) =>
      src.unloadAttempted(src, event);
    window.Vaadin.unloadObserver.visibilityHandler = (event) => 
      {
        console.warn("visibility "+document.visibilityState);
        if (document.visibilityState === 'hidden') {
          src.unloadAttempted(src, event);
        }
      }

    window.addEventListener(
      "beforeunload",
      window.Vaadin.unloadObserver.attemptHandler
    );
    window.addEventListener(
      "pagehide",
      window.Vaadin.unloadObserver.hideHandler
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
  unloadAttempted(source, event) {
    if (window.Vaadin.unloadObserver.query) {
      console.warn("UO: responding to unload attempt...");
      event.preventDefault();
      event.returnValue = "";
      if (source.$server) {
        source.$server.unloadAttempted();
      }
    } else {
      source.$server.unloadHappened();
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

  static get properties() {
    return {};
  }
}

customElements.define(UnloadObserver.is, UnloadObserver);
