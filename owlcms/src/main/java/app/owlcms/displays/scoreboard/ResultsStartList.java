/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.function.BiPredicate;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")

@Tag("startlist-template")
@JsModule("./components/ResultsStartList.js")
@JsModule("./components/AudioContext.js")

public class ResultsStartList extends Results {

	public ResultsStartList() {
		var dt = Translator.translate("Scoreboard.StartList") + " – ";
		this.getElement().setProperty("displayType", dt);
	}

	@Override
	public String getDisplayType() {
		var dt = Translator.translate("Scoreboard.StartList") + " – ";
		this.getElement().setProperty("displayType", dt);
		return dt;
	}

	@Override
	protected List<Athlete> getOrder(FieldOfPlay fop) {
		return fop.getDisplayOrder();
	}
	
	@Override
	protected BiPredicate<Athlete, Athlete> getSeparatorPredicate() {
		return super.getSeparatorPredicate();
	}

}
