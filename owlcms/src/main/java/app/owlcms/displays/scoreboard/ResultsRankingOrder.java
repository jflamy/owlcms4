/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import app.owlcms.i18n.Translator;

@SuppressWarnings("serial")

public class ResultsRankingOrder extends ResultsRankings {

	public ResultsRankingOrder() {
		getElement().setProperty("showCategoryHeaders", true);
		getElement().setAttribute("ranking-order", "true");
	}

	@Override
	public String getDisplayType() {
		return Translator.translate("Scoreboard.RankingOrder") + ": ";
	}

}
