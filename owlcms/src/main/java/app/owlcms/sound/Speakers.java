/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.sound;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

public class Speakers {
    final static Logger logger = (Logger) LoggerFactory.getLogger(Speakers.class);

    public static List<String> getOutputNames() {
        ArrayList<String> outputNames = new ArrayList<>();
        List<Mixer> outputs = getOutputs();
        for (Mixer mixer : outputs) {
            outputNames.add(mixer.getMixerInfo().getName());
        }
        return outputNames;
    }

    /**
     * @return
     */
    public static List<Mixer> getOutputs() {
        List<Mixer> mixers = null;
        try {
            mixers = outputs(AudioSystem.getMixer(null), AudioSystem.getMixerInfo());
        } catch (Exception e) {
            mixers = new ArrayList<>();
        }
        return mixers;
    }

    public static void main(String[] args) throws Exception {
        List<Mixer> mixers = getOutputs();
        for (Mixer mixer : mixers) {
            System.out.println(mixer.getMixerInfo().getName());
            testSound(mixer);
        }
    }

    /**
     * @param mixer
     */
    public static synchronized void testSound(Mixer mixer) {
        try {
            if (mixer == null) {
                return;
            }
            // both sounds should be heard simultaneously
            new Sound(mixer, "initialWarning2.wav").emit();
            new Tone(mixer, 1100, 1200, 1.0).emit();
        } catch (Exception e) {
            logger.error("failed sound test\n {}", LoggerUtils./**/stackTrace(e));
        }
    }

    /**
     * @param defaultMixer
     * @param infos
     */
    protected static List<Mixer> outputs(Mixer defaultMixer, Mixer.Info[] infos) {
        List<Mixer> mixers = new ArrayList<>();
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);

            try {
                if (!mixer.getMixerInfo().toString().startsWith("Java")) {
                    AudioFormat af = new AudioFormat(44100f, 8, 1, true, false);
                    if (AudioSystem.getSourceDataLine(af, info) != null) {
                        mixers.add(mixer);
                        logger.debug("Mixer {} detected", mixer.getMixerInfo().getName());
                    }
                }
            } catch (LineUnavailableException | IllegalArgumentException e) {
                logger.debug("Mixer {} not compatible.", mixer);
            }
        }
        return mixers;
    }

    /**
     * @param infos
     * @throws LineUnavailableException
     */
    protected static void speakers(Mixer.Info[] infos) throws LineUnavailableException {
        for (Mixer.Info info : infos) {
            Mixer mixer = AudioSystem.getMixer(info);
            if (mixer.isLineSupported(Port.Info.SPEAKER)) {
                Port port = (Port) mixer.getLine(Port.Info.SPEAKER);
                port.open();
                if (port.isControlSupported(FloatControl.Type.VOLUME)) {
                    FloatControl volume = (FloatControl) port.getControl(FloatControl.Type.VOLUME);
                    logger.info("{} - {} - {}" + info, Port.Info.SPEAKER, volume);
                }
                port.close();
            }
        }
    }
}
