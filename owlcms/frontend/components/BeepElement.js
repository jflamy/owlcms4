/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
import { PolymerElement, html } from '@polymer/polymer/polymer-element.js';

class BeepElement extends PolymerElement {
	static get is() {
		return 'beep-element'
	}

	static get template() {
		return html`<div></div>`;
	}

	static get properties() {
		return {
			/**
			 * Set to true to have timer not emit sounds
			 * 
			 * @default false
			 */
			silent: {
				type: Boolean,
				value: false
			}
		}
	}

	ready() {
		super.ready();
		console.warn("beeper ready")
		this._init();
	}

	isIOS() {
		return [
			'iPad Simulator',
			'iPhone Simulator',
			'iPod Simulator',
			'iPad',
			'iPhone',
			'iPod'
		].includes(navigator.platform)
			// iPad on iOS 13 detection
			|| (navigator.userAgent.includes("Mac") && "ontouchend" in document)
	}

	_init() {
		this.running = false;
        this._prepareAudio()
	}

	async _prepareAudio() {
		console.warn("window.isIOS=", window.isIOS);
		if (window.isIOS) {
			// prefetched buffers are not available later for some unexplained reason.
			// so we don't attempt fetching.
			return;
		}
		if (!window.beepBeep) {
			this.loadingBeepBeep = true;
			const beepBeep = await this._playTrack("../local/sounds/beepBeep.mp3", null, false, 0);
			window.beepBeep = beepBeep;
			console.warn("loaded beepBeep = " + window.beepBeep);
		} else {
			console.warn("skipping load");
			console.warn("existing beepBeep = " + window.beepBeep);
		}
	}

	async _playTrack(filepath, previousBuffer, play, when) {
		if (previousBuffer) {
			if (play) {
				// play previously fetched buffer
				await this._playAudioBuffer(previousBuffer, when);
			}
			return previousBuffer;
		} else {
			// Safari somehow manages to lose the AudioBuffer.
			// Massive workaround.
			const response = await fetch(filepath);
			const arrayBuffer = await response.arrayBuffer();
			const newBuffer = await window.audioCtx.decodeAudioData(
				arrayBuffer,
				async function (audioBuffer) {
					if (play) {
						// duplicated code from _playAudioBuffer
						// can't figure out how to invoke it with JavaScript "this" semantics.
						const trackSource = await window.audioCtx.createBufferSource();
						trackSource.buffer = audioBuffer;
						trackSource.connect(window.audioCtx.destination);
						if (when <= 0) {
							trackSource.start();
						} else {
							trackSource.start(when, 0);
						}
					}
				},
				(e) => {
					console.error("could not decode " + e.err);
				}
			);
			return newBuffer;
		}
	}

	async _playAudioBuffer(audioBuffer, when) {
		const trackSource = await window.audioCtx.createBufferSource();
		trackSource.buffer = audioBuffer;;
		trackSource.connect(audioCtx.destination);
		if (when <= 0) {
			trackSource.start();
		} else {
			trackSource.start(when, 0);
		}
		return trackSource
	}

    beep() {
        this._playTrack("../local/sounds/beepBeep.mp3", window.beepBeep, true, 0);
    }

}

customElements.define(BeepElement.is, BeepElement);
