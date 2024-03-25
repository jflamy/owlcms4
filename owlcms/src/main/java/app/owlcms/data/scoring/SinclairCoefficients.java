/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.scoring;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.Nullable;

import org.slf4j.LoggerFactory;

import app.owlcms.data.athlete.Gender;
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
		this.sinclairYear = i;
	}

	/**
	 * @param age
	 * @return the Sinclair-Malone-Meltzer Coefficient for that age.
	 * @throws IOException
	 */
	public Float getAgeGenderCoefficient(@Nullable Integer age, @Nullable Gender gender) {
		if ((gender == null) || (age == null)) {
			return 0.0F;
		}
		switch (gender) {
			case M:
				if (this.smf == null) {
					loadSMM();
				}
				if (age <= 30) {
					return 1.0F;
				}
				if (age >= 90) {
					return this.smf.get(90);
				}
				return this.smf.get(age);
			case F:
				if (this.smhf == null) {
					loadSMM();
				}
				if (age <= 30) {
					return 1.0F;
				}
				if (age >= 80) {
					return this.smhf.get(80);
				}
				return this.smhf.get(age);
			case I:
				return 1.0F;
			default:
				break;
		}
		return 0.0F;
	}

	/**
	 * @return
	 */
	public Double menCoefficient() {
		if (this.menCoefficient == null) {
			loadCoefficients();
		}
		return this.menCoefficient;
	}

	/**
	 * @return
	 */
	public Double menMaxWeight() {
		if (this.menMaxWeight == null) {
			loadCoefficients();
		}
		return this.menMaxWeight;
	}

	/**
	 * @return
	 */
	public Double womenCoefficient() {
		if (this.womenCoefficient == null) {
			loadCoefficients();
		}
		return this.womenCoefficient;
	}

	/**
	 * @return
	 */
	public Double womenMaxWeight() {
		if (this.womenMaxWeight == null) {
			loadCoefficients();
		}
		return this.womenMaxWeight;
	}

	private void loadCoefficients() {
		if (this.props == null) {
			loadProps();
		}
		// logger.debug("loadCoefficicients {}",props.get("sinclair.menCoefficient"));
		this.menCoefficient = Double.valueOf((String) this.props.get("sinclair.menCoefficient"));
		this.menMaxWeight = Double.valueOf((String) this.props.get("sinclair.menMaxWeight"));
		this.womenCoefficient = Double.valueOf((String) this.props.get("sinclair.womenCoefficient"));
		this.womenMaxWeight = Double.valueOf((String) this.props.get("sinclair.womenMaxWeight"));
	}

	/**
	 * @throws IOException
	 */
	private void loadProps() {
		this.props = new Properties();
		String name = "/sinclair/sinclair" + this.sinclairYear + ".properties";
		try {
			InputStream stream = ResourceWalker.getResourceAsStream(name);
			this.props.load(stream);
			// logger.debug("props loaded {}",props.get("sinclair.menCoefficient"));
		} catch (Throwable e) {
			this.logger.error("could not load {} because {}\n{}", name, e, LoggerUtils.stackTrace(e));
		}
	}

	/**
	 * @return
	 * @throws IOException
	 */
	private HashMap<Integer, Float> loadSMM() {

		if (this.props == null) {
			loadProps();
		}

		this.smf = new HashMap<>((this.props.size()));
		this.smhf = new HashMap<>((this.props.size()));

		for (Entry<Object, Object> entry : this.props.entrySet()) {
			String curKey = (String) entry.getKey();
			if (curKey.startsWith("smf.")) {
				this.smf.put(Integer.valueOf(curKey.replace("smf.", "")), Float.valueOf((String) entry.getValue()));
			} else if (curKey.startsWith("smhf.")) {
				this.smhf.put(Integer.valueOf(curKey.replace("smhf.", "")), Float.valueOf((String) entry.getValue()));
			}
		}
		return this.smf;
	}
}
