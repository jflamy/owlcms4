/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Rankings by Category display.
 * 
 * Uses the narrow Results grid styling (resultsCustomization.css) with
 * category name grouping headers. Medal decorations are optional.
 * 
 * Extends ResultsMedals to reuse event handling, category-grouped data
 * computation, and lifecycle management.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("resultsrankings-template")
@JsModule("./components/ResultsRankingsByCategory.js")

public class ResultsRankings extends ResultsMedals implements ResultsParameters {

	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsRankings.class);

	/** Category set from URL parameter — must not be cleared by event handlers. */
	private Category urlCategory;

	public ResultsRankings() {

	}

	/**
	 * Remember the URL-specified category so event handlers don't clear it.
	 * When urlCategory is set, block any attempt to null the category.
	 */
	@Override
	public void setCategory(Category category) {
		if (category != null && this.urlCategory == null) {
			// First non-null category set (from URL param reading) becomes the sticky URL category
			this.urlCategory = category;
		}
		if (category == null && this.urlCategory != null) {
			// Don't allow event handlers to clear the URL-specified category
			return;
		}
		super.setCategory(category);
	}

	@Override
	protected boolean isOnlyFinished() {
		// Rankings should show all athletes, not just finished categories
		return false;
	}

	@Override
	protected JsonValue getAthletesJson(List<Athlete> rankingOrder, final FieldOfPlay _unused) {
		JsonArray jath = Json.createArray();
		AtomicInteger athx = new AtomicInteger(0);
		List<Athlete> athletes = rankingOrder != null ? Collections.unmodifiableList(rankingOrder)
		        : Collections.emptyList();

		athletes.stream()
		        .filter(a -> a.getGroup() != null)
		        .sorted((a, b) -> {
			        int aTotalRank = a.getTotalRank();
			        aTotalRank = aTotalRank == 0 ? 999 : aTotalRank;
			        int bTotalRank = b.getTotalRank();
			        bTotalRank = bTotalRank == 0 ? 999 : bTotalRank;
			        if (aTotalRank == bTotalRank) {
				        // both were 0
				        return Integer.compare(a.getSnatchRank(), b.getSnatchRank());
			        } else {
				        return Integer.compare(aTotalRank, bTotalRank);
			        }
		        })
		        .limit(getTopN())
		        .forEach(a -> {
			        JsonObject ja = Json.createObject();
			        Category curCat = a.getCategory();
			        // no blinking = 0
			        getAthleteJson(a, ja, curCat, 0);
			        String team = a.getTeam();
			        if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				        this.logger.trace("long team {}", team);
				        setWideTeamNames(true);
			        }
			        jath.set(athx.getAndIncrement(), ja);
		        });

		return jath;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		String translation = Translator.translate("Scoreboard.RANKING");
		this.getElement().setProperty("displayTitle", translation);
	}

}
