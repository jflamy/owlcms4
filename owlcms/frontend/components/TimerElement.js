import {PolymerElement, html} from '@polymer/polymer/polymer-element.js';

class TimerElement extends PolymerElement {
	static get is() {
		return 'timer-element'
	}

	static get template() {
		return html`
<audio id="initialWarning" preload="auto"><source src="../sounds/initialWarning.mp3"></source></audio>
<audio id="finalWarning" preload="auto"><source src="../sounds/finalWarning.mp3"></source></audio>
<audio id="timeOver" preload="auto"><source src="../sounds/timeOver.mp3"></source></audio>
<div inner-h-t-m-l="[[_formattedTime]]"></div>`;
}

	static get properties() {
		return {
			/**
			 * Start time for the timer in seconds
			 * 
			 * @default 60
			 */
			startTime: {
				type: Number,
				reflectToAttribute: true,
				value: 60,
				observer: '_init'
			},
			/**
			 * Current time of the timer, in seconds
			 */
			currentTime: {
				type: Number,
				notify: true
			},
			/**
			 * True if the timer is currently running
			 * 
			 * @default false
			 */
			running: {
				type: Boolean,
				reflectToAttribute: false,
				notify: true,
				value: false
			},
			/**
			 * Set to true to have timer count up
			 * 
			 * @default false
			 */
			countUp: {
				type: Boolean,
				reflectToAttribute: false,
				value: false
			},
			/**
			 * Set to true to have timer not emit sounds
			 * 
			 * @default false
			 */
			silent: {
				type: Boolean,
				value: false
			},
			/**
			 * Set to true to state that timer is indefinite (--:--)
			 * 
			 * @default false
			 */
			indefinite: {
				type: Boolean,
				value: false
			},
			/**
			 * Time the timer has spent running since it was started
			 */
			_elapsedTime: {
				type: Number,
				value: 0
			},
			_formattedTime: {
				type: String,
				value: '0:00'
			},
			_initialWarningGiven: {
				type: Boolean,
				value: false
			},
			_finalWarningGiven: {
				type: Boolean,
				value: false
			},
			_timeOverWarningGiven: {
				type: Boolean,
				value: false
			}
		}
	}

	ready() {
		super.ready();
		console.warn("timer ready")
		this._init();
	}

	start(seconds, indefinite, silent, element) {
		if (indefinite) {
			console.warn("timer indefinite "+seconds);
			this._indefinite()
			return;
		}

		console.warn("timer start "+seconds);
		this._prepareAudio();

		this.currentTime = seconds;
		this.audioStartTime = window.audioCtx.currentTime;
		if ((this.currentTime <= 0 && !this.countUp) 
				|| (this.currentTime >= this.startTime && this.countUp) ) {
			// timer is over
			this.currentTime = this.countUp ? this.startTime : 0;
		}

		this.silent = silent;
		this._initialWarningGiven = (this.currentTime < 90);
		this._finalWarningGiven = (this.currentTime < 30);
		this._timeOverWarningGiven = (this.currentTime < 0);

		this._elapsed = performance.now()/1000;
		this.running = true;
		// console.debug("timer running "+this.currentTime);
		window.requestAnimationFrame(this._decreaseTimer.bind(this));
	}

	pause(seconds, indefinite, silent, element) {
		if (indefinite) {
			this._indefinite()
			return;
		}

		this.running = false;
		if (element.$server != null) {
			element.$server.clientTimerStopped(this.currentTime);      
		} else {
			console.log("no server$");
		}
		
		console.warn("timer pause"+seconds);
		this.currentTime = seconds;
	}
	
	display(seconds, indefinite, silent, element) {
		this.running = false;
		console.log("display "+indefinite);
		if (indefinite) {
			this.set('currentTime', seconds);
			this._indefinite()
		} else if (this.countUp) {
			this.set('currentTime', 0);
			this.set('_formattedTime', '0:00');
		} else {
			this.set('currentTime', seconds);
			this.set('_formattedTime', this._formatTime(seconds));
		}
		this._initialWarningGiven = false;
		this._finalWarningGiven = false;
		this._timeOverWarningGiven = false;		
	}

	reset(element) {
//		console.warn("timer reset");
//		this.pause(this.startTime, false, true, element);
//		this._init();
	}

	_indefinite() {
		this.set('_formattedTime', '&nbsp;');
	}

	 _init() {
		this.running = false;
		console.log("init timer "+this.indefinite);
		if (this.indefinite) {
			this.set('currentTime', this.startTime);
			this._indefinite()
		} else if (this.countUp) {
			this.set('currentTime', 0);
			this.set('_formattedTime', '0:00');
		} else {
			this.set('currentTime', this.startTime);
			this.set('_formattedTime', this._formatTime(this.startTime));
		}
		this._initialWarningGiven = false;
		this._finalWarningGiven = false;
		this._timeOverWarningGiven = false;
	}

	async _prepareAudio() {
		if (! window.finalWarning /* && ! this.loadingFinalWarning */ ) {
			this.loadingFinalWarning = true;
			// const response0 = await fetch("../sounds/finalWarning.mp3");
			// const arrayBuffer0 = await response0.arrayBuffer();
			// const finalWarning = await audioCtx.decodeAudioData(arrayBuffer0);
			const finalWarning = await this._playTrack("../sounds/finalWarning.mp3", null, false,0);
			window.finalWarning = finalWarning;
			// this.finalWarning = await this._getAudioBuffer("../sounds/finalWarning.mp3");
			console.warn("loaded finalWarning = "+window.finalWarning);
			//await this._playTrack(this.finalWarning, 0);
		} else {
			console.warn("skipping load");
			console.warn("existing finalWarning = "+window.finalWarning);
		}

		if (! window.initialWarning /* && ! this.loadingInitialWarning */) {
			this.loadingInitialWarning = true;
			// const response1 = await fetch("../sounds/initialWarning.mp3");
			// const arrayBuffer1 = await response1.arrayBuffer();
			// const initialWarning = await audioCtx.decodeAudioData(arrayBuffer1);
			const initialWarning = await this._playTrack("../sounds/initialWarning.mp3", null, false,0);
			window.initialWarning = initialWarning;
			//this.initialWarning = await this._getAudioBuffer("../sounds/initialWarning.mp3");
			console.warn("loaded initialWarning = "+window.initialWarning);
		} else {
			console.warn("skipping load");
			console.warn("existing initialWarning = "+window.initialWarning);
		}

		if (! window.timeOver /* && ! this.loadingTimeOver */) {
			this.loadingTimeOver = true;
			// const response2 = await fetch("../sounds/timeOver.mp3");
			// const arrayBuffer2 = await response2.arrayBuffer();
			// const timeOver = await audioCtx.decodeAudioData(arrayBuffer2);
			const timeOver = await this._playTrack("../sounds/timeOver.mp3", null, false,0);
			window.timeOver = timeOver;
			// this.timeOver = await this._getAudioBuffer("../sounds/timeOver.mp3");
			console.warn("loaded timeOver = "+window.timeOver);
			//await this._playTrack(this.timeOver, 0);
		} else {
			console.warn("skipping load");
			console.warn("existing timeOver duration= "+window.timeOver);
		}
	}

	typeOf(obj) {
		return {}.toString.call(obj).split(' ')[1].slice(0, -1).toLowerCase();
	}

	async _playTrack(filepath, previousBuffer, play,  when) {
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
					console.error("could not decode "+e.err);
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
	  

	_decreaseTimer(timestamp) {
		if (!this.running) {
			return;
		}

		var now = timestamp/1000;
		// Compute the relative progress based on the time spent running
		var progress = now - this._elapsed;
		this.currentTime = this.countUp ? this.currentTime + progress : this.currentTime - progress;


		// we anticipate to use the more precise audio context timer
		if (this.currentTime <= 0.05 && !this._timeOverWarningGiven) {
			// console.debug("calling play "+this.currentTime);
			if (!this.silent) {
				//this.$.timeOver.play();
				console.warn("about to play time over "+window.timeOver);
				this._playTrack("../sounds/timeOver.mp3", window.timeOver, true, this.currentTime);
			}
			this._timeOverWarningGiven = true;
		}
		if (this.currentTime <= 30.05 && !this._finalWarningGiven) {
			// console.debug("currentTime "+this.currentTime);
			if (!this.silent) {
				//this.$.finalWarning.play();
				console.warn("about to play final warning "+window.finalWarning);
				this._playTrack("../sounds/finalWarning.mp3", window.finalWarning, true, this.currentTime - 30);
			}
			if (this.$server != null) this.$server.clientFinalWarning();
			this._finalWarningGiven = true;
		}
		if (this.currentTime <= 90.05 && !this._initialWarningGiven) {
			if (!this.silent) {
				//this.$.initialWarning.play();
				console.warn("about to play initial warning "+window.initialWarning);
				this._playTrack("../sounds/initialWarning.mp3", window.initialWarning, true, this.currentTime - 90);
			}
			if (this.$server != null) this.$server.clientInitialWarning();
			this._initialWarningGiven = true;
		}


		if ((this.currentTime <= 0 && !this.countUp) 
				|| (this.currentTime >= this.startTime && this.countUp) ) {
			// timer is over
			if (this.$server != null) this.$server.clientTimeOver();   
			this.running = false;
			// this.dispatchEvent(new CustomEvent('timer-element-end', {bubbles:
			// true, composed: true}))
			this.currentTime = this.countUp ? this.startTime : 0; 
		}

		this._formattedTime = this._formatTime(this.currentTime);
		this._elapsed = now;
		window.requestAnimationFrame(this._decreaseTimer.bind(this));

	}

	_formatTime(ntime) {
		if (ntime < 0) return "0:00";
		var ntime = Math.round(ntime);
		var hours = Math.trunc(ntime / 3600);
		var minutes = Math.trunc((ntime - (hours*3600)) / 60);
		var seconds = ntime - ((hours*3600)+(minutes*60));
		return (hours > 0 ? (hours + ":" + (minutes < 10 ? "0" : "")) : "")+(minutes+":"+ (seconds < 10 ? "0"+seconds : seconds));
	}

}

customElements.define(TimerElement.is, TimerElement);
