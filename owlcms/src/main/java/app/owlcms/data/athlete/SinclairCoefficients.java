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

import javax.annotation.Nullable;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import ch.qos.logback.classic.Logger;

/**
 * The Class SinclairCoefficients.
 */
public class SinclairCoefficients {

    Logger logger = (Logger) LoggerFactory.getLogger(SinclairCoefficients.class);

    Double menCoefficient = null;
    Double menMaxWeight = null;
    Properties props = null;
    Double womenCoefficient = null;
    Double womenMaxWeight = null;
    private HashMap<Integer, Float> smf = null;
    private HashMap<Integer, Float> smhf = null;

    private int sinclairYear;

    public SinclairCoefficients(int i) {
        sinclairYear = i;
    }

    /**
     * @param age
     * @return the Sinclair-Malone-Meltzer Coefficient for that age.
     * @throws IOException
     */
    public Float getAgeGenderCoefficient(@Nullable Integer age, @Nullable Gender gender) {
        if (gender == null) {
            return 0.0F;
        }
        if (age == null) {
            return 0.0F;
        }
        switch (gender) {  
        case M:
            if (smf == null) {
                loadSMM();
            }
            if (age <= 30) {
                return 1.0F;
            }
            if (age >= 90) {
                return smf.get(90);
            }
            return smf.get(age);
        case F:
            if (smhf == null) {
                loadSMM();
            }
            if (age <= 30) {
                return 1.0F;
            }
            if (age >= 80) {
                return smhf.get(80);
            }
            return smhf.get(age);
        }
        return 0.0F;
    }

    /**
     * @return
     */
    public Double menCoefficient() {
        if (menCoefficient == null) {
            loadCoefficients();
        }
        return menCoefficient;
    }

    /**
     * @return
     */
    public Double menMaxWeight() {
        if (menMaxWeight == null) {
            loadCoefficients();
        }
        return menMaxWeight;
    }

    /**
     * @return
     */
    public Double womenCoefficient() {
        if (womenCoefficient == null) {
            loadCoefficients();
        }
        return womenCoefficient;
    }

    /**
     * @return
     */
    public Double womenMaxWeight() {
        if (womenMaxWeight == null) {
            loadCoefficients();
        }
        return womenMaxWeight;
    }

    private void loadCoefficients() {
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
    private void loadProps() {
        props = new Properties();
        try {
            InputStream stream = ResourceWalker.getResourceAsStream("/sinclair/sinclair"+sinclairYear+".properties");
            props.load(stream);
        } catch (IOException e) {
            LoggerUtils.logError(logger, e);
        }
    }

    /**
     * @return
     * @throws IOException
     */
    private HashMap<Integer, Float> loadSMM() {

        if (props == null) {
            loadProps();
        }

        smf = new HashMap<>((int) (props.size()));
        smhf = new HashMap<>((int) (props.size()));
        
        for (Entry<Object, Object> entry : props.entrySet()) {
            String curKey = (String) entry.getKey();
            if (curKey.startsWith("smf.")) {
                smf.put(Integer.valueOf(curKey.replace("smf.", "")), Float.valueOf((String) entry.getValue()));
            } else if (curKey.startsWith("smhf.")) {
                smhf.put(Integer.valueOf(curKey.replace("smhf.", "")), Float.valueOf((String) entry.getValue()));
            } 
        }
        return smf;
    }
}
