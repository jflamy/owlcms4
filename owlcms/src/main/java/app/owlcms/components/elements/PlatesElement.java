/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;

@Tag("plates-element")
public class PlatesElement extends FlexLayout {

	@SuppressWarnings("unused")
	private static final Logger logger = (Logger) LoggerFactory.getLogger(PlatesElement.class);
	private static final long serialVersionUID = 8340222363211435843L;

	private int weight;

	public PlatesElement() {
		this.getClassNames().add("loadChart");
	}

	public void computeImageArea(FieldOfPlay fop, boolean showCaption) {
		if (fop == null) {
			return;
		}

		final Athlete currentAthlete = fop.getCurAthlete();
		final Integer barWeight = computeBarWeight(fop);
		if (currentAthlete == null) {
			return;
		}
		weight = currentAthlete.getNextAttemptRequestedWeight();
		final String caption = getTranslation("Kg", weight);

		createImageArea(fop, barWeight, (showCaption ? caption : ""));
	}

	/**
	 * @param availablePlates
	 * @param style
	 * @param plateWeight
	 * @return
	 */
	private int addPlates(Integer availablePlates, String style, double plateWeight) {
		int subtractedWeight = 0;
		while (availablePlates > 0 && weight >= plateWeight) {
			NativeLabel plate = new NativeLabel();
			plate.setSizeUndefined();
			plate.getElement().getClassList().add(style);
			if (!style.startsWith("bar") && !style.startsWith("C")) {
				plate.getElement().getClassList().add("plate");
			}
			this.add(plate);
			this.setAlignSelf(Alignment.CENTER, plate);
			final long delta = Math.round(plateWeight);
			weight -= delta;
			subtractedWeight += delta;
			availablePlates--;
		}
		return subtractedWeight;
	}

	private Integer computeBarWeight(FieldOfPlay fop) {
		if (fop == null) {
			return 0;
		}
		Platform platform = fop.getPlatform();
		if (platform.isNonStandardBar() && platform.getLightBar() > 0) {
			return platform.getLightBar();
		} else {
			return computeOfficialBarWeight(fop, platform);
		}
	}

	/**
	 * @return
	 */
	private Integer computeOfficialBarWeight(FieldOfPlay fop, Platform platform) {
		if (fop == null || platform == null) {
			return 0;
		}

		final Athlete currentAthlete = fop.getCurAthlete();
		Gender gender = Gender.M;
		if (currentAthlete != null) {
			gender = currentAthlete.getGender();
		}
		final int expectedBarWeight = Gender.M.equals(gender) ? 20 : 15;
		return expectedBarWeight;
	}

	/**
	 * @param platform
	 * @param barWeight
	 * @param caption
	 */
	private void createImageArea(FieldOfPlay fop, final Integer barWeight, final String caption) {
		this.removeAll();
		Platform platform = fop.getPlatform();

		if (weight == 0) {
			return;
			// compute the bar and collar first.
		}

		addPlates(1, "bar", barWeight);
		addPlates(1, "barInner", 0);
		final Integer collarAvailable = platform.getNbC_2_5();
		boolean useCollar = collarAvailable > 0;

		if (weight >= 25) {
			if (useCollar) {
				// we only take off the collar weight because we need to
				// wait before showing the collar.
				weight -= 5;
			}

			// use large plates first
			addPlates(platform.getNbL_25(), "L_25", 2 * 25);
			addPlates(platform.getNbL_20(), "L_20", 2 * 20);
			addPlates(platform.getNbL_15(), "L_15", 2 * 15);
			addPlates(platform.getNbL_10(), "L_10", 2 * 10);
		} else {
			int nonBarWeight = weight;
			// make sure that large 5 and large 2.5 are only used when warranted
			// (must not require manual intervention if they are available)
			if (platform.getNbL_2_5() > 0 && nonBarWeight < 10 || platform.getNbL_5() > 0 && nonBarWeight < 15) {
				useCollar = false;
			}
			if (useCollar) {
				// we take off the collar weight because we need to
				// wait before showing the collar.
				weight -= 5;
				nonBarWeight -= 5;
			}
			addPlates(platform.getNbL_10(), "L_10", 2 * 10);
			addPlates(platform.getNbL_5(), "L_5", 2 * 5);
			if (nonBarWeight < 10 || platform.getNbL_5() == 0) {
				addPlates(platform.getNbL_2_5(), "L_2_5", 2 * 2.5);
			}

		}

		// add the small plates
		addPlates(platform.getNbS_5(), "S_5", 2 * 5);
		addPlates(platform.getNbS_2_5(), "S_2_5", 2 * 2.5);
		// collar is depicted here
		if (useCollar) {
			// we add back the collar weight we took off above
			weight += 5;
			addPlates(collarAvailable, "C_2_5", 2 * 2.5);
		}
		// remainder of small plates
		addPlates(platform.getNbS_2(), "S_2", 2 * 2);
		addPlates(platform.getNbS_1_5(), "S_1_5", 2 * 1.5);
		addPlates(platform.getNbS_1(), "S_1", 2 * 1);
		addPlates(platform.getNbS_0_5(), "S_0_5", 2 * 0.5);
		addPlates(1, "barOuter", 0);
	}

}
