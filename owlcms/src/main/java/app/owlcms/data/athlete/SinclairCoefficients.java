/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.athlete;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * The Class SinclairCoefficients.
 */
public class SinclairCoefficients {

    static Logger logger = (Logger) LoggerFactory.getLogger(SinclairCoefficients.class);

    private static HashMap<Integer, Float> smm = null;
    static Properties props = null;
    static Double menCoefficient = null;
    static Double womenCoefficient = null;
    static Double menMaxWeight = null;
    static Double womenMaxWeight = null;

    /**
     * @param age
     * @return the Sinclair-Malone-Meltzer Coefficient for that age.
     * @throws IOException
     */
    public static Float getSMMCoefficient(Integer age) {
        if (smm == null) {
            loadSMM();
        }
        if (age <= 30) {
            return 1.0F;
        }
        if (age >= 90) {
            return smm.get(90);
        }
        return smm.get(age);
    }

    /**
     * @return
     */
    public static Double menCoefficient() {
        if (menCoefficient == null) {
            loadCoefficients();
        }
        return menCoefficient;
    }

    /**
     * @return
     */
    public static Double menMaxWeight() {
        if (menMaxWeight == null) {
            loadCoefficients();
        }
        return menMaxWeight;
    }

    /**
     * @return
     */
    public static Double womenCoefficient() {
        if (womenCoefficient == null) {
            loadCoefficients();
        }
        return womenCoefficient;
    }

    /**
     * @return
     */
    public static Double womenMaxWeight() {
        if (womenMaxWeight == null) {
            loadCoefficients();
        }
        return womenMaxWeight;
    }

    private static void loadCoefficients() {
        if (props == null) {
            loadProps();
        }
        menCoefficient = Double.valueOf((String) props.get("sinclair.menCoefficient"));
        menMaxWeight = Double.valueOf((String) props.get("sinclair.menMaxWeight"));
        womenCoefficient = Double.valueOf((String) props.get("sinclair.womenCoefficient"));
        womenMaxWeight = Double.valueOf((String) props.get("sinclair.womenMaxWeight"));
    }

    /**
     * @throws IOException
     */
    private static void loadProps() {
        props = new Properties();
        try {
            InputStream stream = ResourceWalker.getResourceAsStream("/sinclair/sinclair.properties");
            props.load(stream);
        } catch (IOException e) {
            LoggerUtils.logError(logger, e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private static HashMap<Integer, Float> loadSMM() {

        if (props == null) {
            loadProps();
        }

        smm = new HashMap<>((int) (props.size() * 1.4));
        for (Entry<Object, Object> entry : props.entrySet()) {
            String curKey = (String) entry.getKey();
            if (curKey.startsWith("smm.")) {
                smm.put(Integer.valueOf(curKey.replace("smm.", "")), Float.valueOf((String) entry.getValue()));
            }
        }
        return smm;
    }
}
