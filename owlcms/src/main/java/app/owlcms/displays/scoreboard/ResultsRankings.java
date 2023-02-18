package app.owlcms.displays.scoreboard;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.router.Route;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FieldOfPlay;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

@SuppressWarnings({ "serial", "deprecation" })
@Tag("resultsmedals-template")
@JsModule("./components/ResultsMedals.js")
@Route("displays/resultsRankings")

public class ResultsRankings extends ResultsMedals {

	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedals.class);

	protected JsonValue getAthletesJson(List<Athlete> displayOrder, final FieldOfPlay _unused) {
		JsonArray jath = Json.createArray();
		AtomicInteger athx = new AtomicInteger(0);
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();

		athletes.stream().limit(15).forEach(a -> {
			JsonObject ja = Json.createObject();
			Category curCat = a.getCategory();
			// no blinking = 0
			getAthleteJson(a, ja, curCat, 0);
			String team = a.getTeam();
			if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				logger.trace("long team {}", team);
				setWideTeamNames(true);
			}
			jath.set(athx.getAndIncrement(), ja);
		});

		return jath;
	}
	
	@Override
	public String getPageTitle() {
		String translation = getTranslation("Scoreboard.RANKINGS");
		return translation;
	}
	
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		String translation = getTranslation("Scoreboard.RANKINGS");
		this.getElement().setProperty("displayTitle", translation);
	}

}
