/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.displays.results;

import java.util.List;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.athlete.LiftInfo;
import org.ledocte.owlcms.data.athlete.XAthlete;
import org.ledocte.owlcms.displays.attemptboard.DecisionElement;
import org.ledocte.owlcms.displays.attemptboard.TimerElement;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.UIEvent;
import org.ledocte.owlcms.ui.home.QueryParameterReader;
import org.ledocte.owlcms.ui.home.SafeEventBusRegistration;
import org.ledocte.owlcms.ui.lifting.UIEventProcessor;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class ResultsBoard
 * 
 * Show athlete 6-attempt results
 * 
 */
@SuppressWarnings("serial")
@Tag("results-board-template")
@HtmlImport("frontend://components/ResultsBoard.html")
@Route("displays/resultsBoard")
@Theme(value = Material.class, variant = Material.DARK)
@Push
public class ResultsBoard extends PolymerTemplate<ResultsBoard.ResultBoardModel>
		implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(ResultsBoard.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");

	/**
	 * ResultBoardModel
	 * 
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript
	 * properties. When the JS properties are changed, a "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String,
	 * com.vaadin.flow.dom.PropertyChangeListener)}
	 *
	 */
	public interface ResultBoardModel extends TemplateModel {
		String getLastName();

		String getFirstName();

		String getTeamName();

		Integer getStartNumber();

		String getAttempt();

		Integer getWeight();

		void setLastName(String lastName);

		void setFirstName(String firstName);

		void setTeamName(String teamName);

		void setStartNumber(Integer integer);

		void setAttempt(String formattedAttempt);

		void setWeight(Integer weight);
	}

	@Id("timer")
	private TimerElement timer; // created by Flow during template instanciation
	@Id("decisions")
	private DecisionElement decisions; // created by Flow during template instanciation
	private EventBus uiEventBus;
	private List<Athlete> list;

	/**
	 * Instantiates a new attempt board.
	 */
	public ResultsBoard() {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.DEBUG);
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		translations.put("key1","value1");
		translations.put("key2","value2");
		this.getElement().setPropertyJson("t", translations);
	}

	protected void setGroupProperties() {
		JsonObject groupProperties = Json.createObject();
		groupProperties.put("isMasters", true);
		this.getElement().setPropertyJson("g", groupProperties);
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via QueryParameterReader interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			// sync with current status of FOP
			list = fop.getLifters();
			doUpdate(fop.getCurAthlete(), null);
			// we send on fopEventBus, listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.debug("Starting result board on FOP {}", fop.getName());
			setId("result-board-template");
		});
		setGroupProperties();
		setTranslationMap();
		list = ImmutableList.of();
	}

	@Subscribe
	public void orderUpdated(UIEvent.LiftingOrderUpdated e) {
		Athlete a = e.getAthlete();
		list = e.getAthletes();
		doUpdate(a, e);
	}

	@Subscribe
	public void athleteAnnounced(UIEvent.AthleteAnnounced e) {
		Athlete a = e.getAthlete();
		doUpdate(a, e);
	}

	@Subscribe
	public void intermissionDone(UIEvent.IntermissionDone e) {
		Athlete a = e.getAthlete();
		doUpdate(a, e);
	}

	protected void doUpdate(Athlete a, UIEvent e) {
		if (a == null)
			return;
		XAthlete x = new XAthlete(a);
		LiftInfo l = x.getCurrentRequestInfo();
		logger.warn("lift = {}, style={}",l.getLiftNo(),l.getChange());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			uiEventLogger.debug("&&& resultBoard update {}", a);
			ResultBoardModel model = getModel();
			model.setLastName(a.getLastName());
			model.setFirstName(a.getFirstName());
			model.setTeamName(a.getTeam());
			model.setStartNumber(a.getStartNumber());
			String formattedAttempt = formatAttempt(a.getAttemptsDone());
			model.setAttempt(formattedAttempt);
			model.setWeight(a.getNextAttemptRequestedWeight());
			
			this.getElement().setPropertyJson("athletes", getAthletesJson(list));
//			
//			List<AthleteDisplayData> nList = new ArrayList<>(20);
//			if (list != null && !list.isEmpty()) {
//				nList = list.stream()
//					.map(ath -> new AthleteDisplayData(ath))
//					.collect(Collectors.toList());
//			}
//			model.setAthletes(nList);
		});
	}

	private JsonValue getAthletesJson(List<Athlete> list2) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		for (Athlete a: list2) {
			JsonObject ja = Json.createObject();
			ja.put("lastName", a.getLastName().toUpperCase());
			ja.put("firstName", a.getFirstName());
			ja.put("teamName", a.getTeam());
			ja.put("startNumber", a.getStartNumber());
			
			XAthlete x = new XAthlete(a);
			JsonArray jattempts = Json.createArray();
			int ix = 0;
			for (LiftInfo i : x.getRequestInfoArray()) {
				JsonObject jri = Json.createObject();
				String stringValue = i.getStringValue();
				jri.put("className",i.getChangeNo() >= 0 ? i.getChange() : "EMPTY");
				if (stringValue != null) jri.put("stringValue",stringValue);
				jattempts.set(ix, jri);
				ix++;
			}
			ja.put("attempts", jattempts);
			jath.set(athx, ja);
			athx++;
		}

		System.err.println(jath);
		return jath;
	}

	private String formatAttempt(Integer attemptsDone) {
		return ((attemptsDone % 3 + 1) + " att.");
	}

	/**
	 * Reset.
	 */
	public void reset() {
//		this.getElement().callFunction("reset");
		list = ImmutableList.of();
	}
}
