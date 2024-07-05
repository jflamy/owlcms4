/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.components.elements;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.FlexLayout;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.platform.Platform;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Tag("plates-element")
public class PlatesElement extends FlexLayout {

	@SuppressWarnings("unused")
	private static final Logger logger = (Logger) LoggerFactory.getLogger(PlatesElement.class);
	private int weight;
	private UI ui;

	public PlatesElement(UI ui) {
		this.getClassNames().add("loadChart");
		this.ui = ui;
	}

	public void computeImageArea(FieldOfPlay fop, boolean showCaption) {
		if (fop == null) {
			return;
		}

		final Athlete currentAthlete = fop.getCurAthlete();
		if (currentAthlete == null) {
			return;
		}

		final int barWeight = fop.getBarWeight();
		this.setWeight(currentAthlete.getNextAttemptRequestedWeight());
		final String caption = Translator.translate("Kg", this.getWeight());
		//logger.debug("caption {}",caption);

		//logger.debug("before createImageArea save, platform identity={} 5kg={}",System.identityHashCode(fop.getPlatform()), fop.getPlatform().getNbB_5());
		createImageArea(fop, barWeight, (showCaption ? caption : ""));
	}

	/**
	 * @param availablePlates
	 * @param style
	 * @param plateWeight
	 * @param fop             field of play
	 * @param outline         show the outline
	 * @return
	 */
	private int addPlates(Integer availablePlates, String style, double plateWeight, FieldOfPlay fop, boolean outline) {
		int subtractedWeight = 0;

		//logger.debug("adding plates {} {} {} {} -- {}", availablePlates, style, getWeight(), plateWeight, LoggerUtils.whereFrom());
		while (availablePlates > 0 && this.getWeight() >= plateWeight) {
			NativeLabel plate = new NativeLabel();
			plate.setSizeUndefined();
			plate.getElement().getClassList().add(style);
			if (!style.startsWith("bar") && !style.startsWith("C")) {
				plate.getElement().getClassList().add("plate");
			} else if (fop.isLightBarInUse() && (style.startsWith("bar"))) {
				int barWeight = fop.getBarWeight();
				if (outline) {
					plate.getElement().getStyle().set("outline-color", "black");
					plate.getElement().getStyle().set("outline-width", "thin");
					plate.getElement().getStyle().set("outline-style", "solid");
				}

				// brown is used for non-standard bar (typically 15lb, 7kg)
				if (barWeight < 4.99) {
					plate.getElement().getStyle().set("background-color", "brown");
				} else if (barWeight <= 5) {
					plate.getElement().getStyle().set("background-color", "white");
				} else if (barWeight < 9.99) {
					plate.getElement().getStyle().set("background-color", "brown");
				} else if (barWeight <= 10) {
					plate.getElement().getStyle().set("background-color", "limegreen");
				} else if (barWeight <= 15) {
					plate.getElement().getStyle().set("background-color", "yellow");
				} else if (barWeight <= 20) {
					plate.getElement().getStyle().set("background-color", "blue");
				}
			}
			this.add(plate);
			this.setAlignSelf(Alignment.CENTER, plate);
			final int delta = (int) Math.round(plateWeight);

			this.setWeight(this.getWeight() - delta);
			subtractedWeight += delta;
			availablePlates--;
		}
		return subtractedWeight;
	}


	/**
	 * @param platform
	 * @param barWeight
	 * @param caption
	 */
	private void createImageArea(FieldOfPlay fop, final Integer barWeight, final String caption) {
		ui.access(() -> {
			this.removeAll();
			Platform platform = fop.getPlatform();
			boolean outline = caption != null && !caption.isBlank();

			if (this.getWeight() == 0) {
				return;
				// compute the bar and collar first.
			}

			//logger.debug("barWeight {}",barWeight);
			int nonBarWeight = this.getWeight() - barWeight;

			addPlates(1, "bar", barWeight, fop, outline);

			final Integer collarAvailable = platform.getNbC_2_5();
			boolean useCollar = collarAvailable > 0 && fop.isUseCollarsIfAvailable();

			if (nonBarWeight >= 0) {
				addPlates(1, "barInner", 0, fop, outline);

				//logger.debug("barWeight = {} nonBarWeight = {}", barWeight, nonBarWeight);
				// make sure that large 5 and large 2.5 are only used when warranted
				// (must not require manual intervention if they are available)
				if (platform.getNbL_2_5() > 0 && nonBarWeight < 10 || platform.getNbL_5() > 0 && nonBarWeight < 15) {
					useCollar = false;
				}
				if (useCollar) {
					// we take off the collar weight because we need to
					// wait before showing the collar.
					this.setWeight(this.getWeight() - 5);
					nonBarWeight -= 5;
				}

				// use large plates first
				int subtractedWeight = 0;
				subtractedWeight += addPlates(platform.getNbL_25(), "L_25", 2 * 25, fop, outline);
				subtractedWeight += addPlates(platform.getNbL_20(), "L_20", 2 * 20, fop, outline);
				subtractedWeight += addPlates(platform.getNbL_15(), "L_15", 2 * 15, fop, outline);
				subtractedWeight += addPlates(platform.getNbL_10(), "L_10", 2 * 10, fop, outline);

				//logger.debug("1 subtractedWeight {} nonBarWeight {}", subtractedWeight, nonBarWeight);
				if (subtractedWeight == 0 && nonBarWeight >= 10) {
					// we have not used a large plate
					subtractedWeight += addPlates(platform.getNbL_5(), "L_5", 2 * 5, fop, outline);
				}

				//logger.debug("2 subtractedWeight {} nonBarWeight {}", subtractedWeight, nonBarWeight);
				if (subtractedWeight == 0 && nonBarWeight >= 5) {
					// we have not used a large plate
					subtractedWeight += addPlates(platform.getNbL_2_5(), "L_2_5", 2 * 2.5, fop, outline);
				}
				
				//logger.debug("3 subtractedWeight {} nonBarWeight {}", subtractedWeight, nonBarWeight);
			
				// add the small plates
				addPlates(platform.getNbS_5(), "S_5", 2 * 5, fop, outline);
				addPlates(platform.getNbS_2_5(), "S_2_5", 2 * 2.5, fop, outline);
				
				// collar is depicted here
				if (useCollar) {
					this.setWeight(this.getWeight() + 5);
					addPlates(collarAvailable, "C_2_5", 2 * 2.5, fop, outline);
				}
				// remainder of small plates
				addPlates(platform.getNbS_2(), "S_2", 2 * 2, fop, outline);
				addPlates(platform.getNbS_1_5(), "S_1_5", 2 * 1.5, fop, outline);
				addPlates(platform.getNbS_1(), "S_1", 2 * 1, fop, outline);
				addPlates(platform.getNbS_0_5(), "S_0_5", 2 * 0.5, fop, outline);
				addPlates(1, "barOuter", 0, fop, outline);
			}
		});
	}

	private int getWeight() {
		return weight;
	}

	private void setWeight(int weight) {
		//logger.debug("weight = {} -- {}", weight, LoggerUtils.whereFrom());
		this.weight = weight;
	}

}
