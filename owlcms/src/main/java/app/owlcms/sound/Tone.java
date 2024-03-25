/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class Tone {
	private AudioFormat af;
	private byte[] buf;
	private SourceDataLine sdl;

	public Tone(Mixer mixer, int hz, int msecs, double vol) throws IllegalArgumentException, LineUnavailableException {
		if (mixer == null) {
			return;
		}
		init(hz, msecs, vol, mixer);
	}

	/**
	 * @param buf
	 * @param af
	 * @param sdl
	 * @throws LineUnavailableException
	 */
	public void emit() throws IllegalArgumentException, LineUnavailableException {
		if (this.sdl == null) {
			return;
		}
		this.sdl.open(this.af, this.buf.length);
		this.sdl.start();
		this.sdl.write(this.buf, 0, this.buf.length);
		this.sdl.drain();
		this.sdl.close();
	}

	/**
	 * @param hz
	 * @param msecs
	 * @param vol
	 * @param mixer
	 */
	protected void init(int hz, int msecs, double vol, Mixer mixer)
	        throws LineUnavailableException, IllegalArgumentException {
		if (vol > 1.0 || vol < 0.0) {
			throw new IllegalArgumentException("Volume out of range 0.0 - 1.0");
		}
		this.buf = new byte[msecs * 8];

		for (int i = 0; i < this.buf.length; i++) {
			double angle = i / (8000.0 / hz) * 2.0 * Math.PI;
			this.buf[i] = (byte) (Math.sin(angle) * 127.0 * vol);
		}

		// shape the front and back ends of the wave form
		for (int i = 0; i < 20 && i < this.buf.length / 2; i++) {
			this.buf[i] = (byte) (this.buf[i] * i / 20);
			this.buf[this.buf.length - 1 - i] = (byte) (this.buf[this.buf.length - 1 - i] * i / 20);
		}

		this.af = new AudioFormat(8000f, 8, 1, true, false);
		this.sdl = AudioSystem.getSourceDataLine(this.af, mixer.getMixerInfo());
	}

}
