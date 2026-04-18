/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.List;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")

@Tag("results-template")
@JsModule("./components/Results.js")
@JsModule("./components/AudioContext.js")

public class ResultsRankingOrder extends Results {

	public ResultsRankingOrder() {
		getElement().setProperty("showCategoryHeaders", true);
	}

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.RankingOrder") + ": ";
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getResultsOrder();
	}

}
