/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Non-Profit Open Software License ("Non-Profit OSL") 3.0 
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.displays.results;

import java.text.MessageFormat;
import java.util.List;

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

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.displays.attemptboard.DecisionElement;
import app.owlcms.displays.attemptboard.TimerElement;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.SafeEventBusRegistration;
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
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

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

		void setHidden(boolean b);
	}

	@Id("timer")
	private TimerElement timer; // Flow creates it
	
	@Id("decisions")
	private DecisionElement decisions; // Flow creates it
	
	private EventBus uiEventBus;
	private List<Athlete> displayOrder;

	/**
	 * Instantiates a new results board.
	 */
	public ResultsBoard() {
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.DEBUG);
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		//TODO i18n t.key1 --> translation of key1 for getLocale()
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
			displayOrder = fop.getDisplayOrder();
			doUpdate(fop.getCurAthlete(), null);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.debug("Starting result board on FOP {}", fop.getName());
			setId("results-board-"+fop.getName());
		});
		setGroupProperties();
		setTranslationMap();
		displayOrder = ImmutableList.of();
	}

	@Subscribe
	public void orderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			displayOrder = e.getDisplayOrder();
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void athleteAnnounced(UIEvent.AthleteAnnounced e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void breakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	public void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
	}

	private Object getOrigin() {
		return this;
	}

	@Subscribe
	public void refereeDecision(UIEvent.RefereeDecision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			logger.debug("resultBoard refereeDecision");
			this.getElement().callFunction("refereeDecision");
		});
	}

	@Subscribe
	public void decisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			logger.debug("resultBoard decisionReset");
			this.getElement().callFunction("reset");
		});
	}


	protected void doUpdate(Athlete a, UIEvent e) {

			
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			ResultBoardModel model = getModel();
			model.setHidden(a == null);
			if (a == null) return;
			
			model.setLastName(a.getLastName().toUpperCase());
			model.setFirstName(a.getFirstName());
			model.setTeamName(a.getTeam());
			model.setStartNumber(a.getStartNumber());
			String formattedAttempt = formatAttempt(a.getAttemptsDone());
			model.setAttempt(formattedAttempt);
			model.setWeight(a.getNextAttemptRequestedWeight());		
			this.getElement().setPropertyJson("athletes", getAthletesJson(displayOrder));
		});
	}
	
	protected void doHide() {
		this.getModel().setHidden(true);
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
			JsonArray jattempts = getAttemptsJson(a);
			ja.put("attempts", jattempts);
			ja.put("total", formatInt(a.getTotal()));
			ja.put("totalRank", formatInt(a.getTotalRank()));
			Integer liftOrderRank = a.getLiftOrderRank();
			ja.put("classname", (liftOrderRank == 1 ? "current" : (liftOrderRank == 2) ? "next" : ""));
//			ja.put("snatchRank", a.getSnatchRank());
//			ja.put("cleanJerkRank", a.getCleanJerkRank());
			
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}
	
	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-" : (total.startsWith("-") ? "("+total.substring(1)+")" : total);
	}
	
	private String formatInt(Integer total) {
		return (total == null || total == 0) ? "-" : (total < 0 ? "("+Math.abs(total)+")" : total.toString());
	}

	/**
	 * Compute Json string ready to be used by web component template
	 * 
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 * 
	 * @param a
	 * @return json string with nested attempts values
	 */
	//TODO: add a marker for breaks between categories
	protected JsonArray getAttemptsJson(Athlete a) {
		XAthlete x = new XAthlete(a);
		Integer liftOrderRank = x.getLiftOrderRank();
		Integer curLift = x.getAttemptsDone();
		JsonArray jattempts = Json.createArray();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			
			jri.put("className", "narrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (stringValue != null && !stringValue.trim().isEmpty()) {
						boolean failed = stringValue.startsWith("-");
						jri.put("className", failed ? "narrow fail" : "narrow good");
						jri.put("stringValue", formatKg(stringValue));
					}
					break;
				default:
					if (stringValue != null && !stringValue.trim().isEmpty()) {
						String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? " current"
								: (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
						jri.put("className",
							"narrow request" + highlight);
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}
			
			jattempts.set(ix, jri);
			ix++;
		}
		return jattempts;
	}
	
	private String formatAttempt(Integer attemptNo) {
		return MessageFormat.format("{0}<sup>{0,choice,1#st|2#nd|3#rd}</sup> att.",(attemptNo%3)+1);
	}

	/**
	 * Reset.
	 */
	public void reset() {
//		this.getElement().callFunction("reset");
		displayOrder = ImmutableList.of();
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}
}
