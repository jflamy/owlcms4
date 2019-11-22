/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class Tone {
    private byte[] buf;
    private AudioFormat af;
    private SourceDataLine sdl;

    public Tone(Mixer mixer, int hz, int msecs, double vol) throws IllegalArgumentException {
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
    public void emit() {
        if (sdl == null) {
            return;
        }
        try {
            sdl.open(af, buf.length);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.close();
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param hz
     * @param msecs
     * @param vol
     * @param mixer
     */
    protected void init(int hz, int msecs, double vol, Mixer mixer) {
        if (vol > 1.0 || vol < 0.0) {
            throw new IllegalArgumentException("Volume out of range 0.0 - 1.0");
        }
        buf = new byte[msecs * 8];

        for (int i = 0; i < buf.length; i++) {
            double angle = i / (8000.0 / hz) * 2.0 * Math.PI;
            buf[i] = (byte) (Math.sin(angle) * 127.0 * vol);
        }

        // shape the front and back ends of the wave form
        for (int i = 0; i < 20 && i < buf.length / 2; i++) {
            buf[i] = (byte) (buf[i] * i / 20);
            buf[buf.length - 1 - i] = (byte) (buf[buf.length - 1 - i] * i / 20);
        }

        af = new AudioFormat(8000f, 8, 1, true, false);
        try {
            sdl = AudioSystem.getSourceDataLine(af, mixer.getMixerInfo());
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

}
