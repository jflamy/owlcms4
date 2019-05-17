/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.scoreboard;

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
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import app.owlcms.components.elements.BreakTimerElement;
import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.attemptboard.AthleteTimerElement;
import app.owlcms.displays.attemptboard.BreakDisplay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.BreakStarted;
import app.owlcms.fieldofplay.UIEvent.RefereeDecision;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.lifting.BreakDialog.BreakType;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 * 
 * Show athlete 6-attempt results
 * 
 */
@SuppressWarnings("serial")
@Tag("scoreboard-template")
@HtmlImport("frontend://components/Scoreboard.html")
@Route("displays/scoreboard")
@Theme(value = Material.class, variant = Material.DARK)
@Push
public class Scoreboard extends PolymerTemplate<Scoreboard.ScoreboardModel>
		implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(Scoreboard.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	/**
	 * ScoreboardModel
	 * 
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript
	 * properties. When the JS properties are changed, a "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String,
	 * com.vaadin.flow.dom.PropertyChangeListener)}
	 *
	 */
	public interface ScoreboardModel extends TemplateModel {
		String getAttempt();

		String getFullName();

		Integer getStartNumber();

		String getTeamName();

		Integer getWeight();
		
		Boolean isHidden();
		
		Boolean isMasters();

		void setAttempt(String formattedAttempt);

		void setGroupName(String name);

		void setHidden(boolean b);
		
		void setFullName(String lastName);

		void setLiftsDone(String formattedDone);

		void setMasters(boolean b);

		void setStartNumber(Integer integer);
		
		void setTeamName(String teamName);
		
		void setWeight(Integer weight);
	}


	@Id("timer")
	private AthleteTimerElement timer; // Flow creates it
	
	@Id("breakTimer")
	private BreakTimerElement breakTimer; // Flow creates it
	
	@Id("decisions")
	private DecisionElement decisions; // Flow creates it
	
	private EventBus uiEventBus;
	private List<Athlete> displayOrder;
	private Group curGroup;
	private int liftsDone;

	JsonArray sattempts;
	JsonArray cattempts;

	/**
	 * Instantiates a new results board.
	 */
	public Scoreboard() {
		timer.setOrigin(this);
	}

	@Subscribe
	public void breakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			doUpdate(a, e);
		});
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	/**
	 * Reset.
	 */
	public void reset() {
		displayOrder = ImmutableList.of();
	}

	@Subscribe
	public void slaveAthleteAnnounced(UIEvent.AthleteAnnounced e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			doUpdate(a, e);
		});
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			this.getElement().callFunction("reset");
		});
	}

	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		// ignore if the down signal was initiated by this result board.
		// (the timer element on the result board will actually process the keyboard codes if devices are attached)
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			this.getElement().callFunction("down");
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			Athlete a = e.getAthlete();
			displayOrder = e.getDisplayOrder();
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			doUpdate(a, e);
		});
	}
	

	@Subscribe
	public void slaveRefereeDecision(UIEvent.RefereeDecision e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			doUpdateBottomPart(e);
			this.getElement().callFunction("refereeDecision");
		});
	}


	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		Athlete a = e.getAthlete();
		this.getElement().callFunction("reset");
		doUpdate(a, e);
	}
	
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		doDone(e.getGroup());
	}

	private void doDone(Group g) {
		if (g == null) return;
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getModel().setFullName(MessageFormat.format("Group {0} Results", g.toString()));
			this.getElement().callFunction("groupDone");
		});
	}
	
	public void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
	}

	private String formatAttempt(Integer attemptNo) {
		return MessageFormat.format("{0}<sup>{0,choice,1#st|2#nd|3#rd}</sup> att.",(attemptNo%3)+1);
	}


	private String formatInt(Integer total) {
		if (total == -1) return "inv.";//invited lifter, not eligible.
		return (total == null || total == 0) ? "-" : (total < 0 ? "("+Math.abs(total)+")" : total.toString());
	}
	
	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-" : (total.startsWith("-") ? "("+total.substring(1)+")" : total);
	}

	private JsonValue getAthletesJson(List<Athlete> list2) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		Category prevCat = null;
		for (Athlete a: list2) {
			JsonObject ja = Json.createObject();
			Category curCat = a.getCategory();
			if (curCat != null && !curCat.equals(prevCat)) {
				// changing categories, put marker before athlete
				ja.put("isSpacer", true);
				jath.set(athx, ja);
				ja = Json.createObject();
				prevCat = curCat;
				athx++;
			}
			String category;
			if (Competition.getCurrent().isMasters()) {
				category = a.getShortCategory();
			} else {
				category = curCat != null ? curCat.getName() : "";
			}
			ja.put("fullName", a.getFullName());
			ja.put("teamName", a.getTeam());
			ja.put("yearOfBirth", a.getYearOfBirth());
			Integer startNumber = a.getStartNumber();
			ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
			ja.put("mastersAgeGroup", a.getMastersAgeGroup());
			ja.put("category", category);
			getAttemptsJson(a);
			ja.put("sattempts", sattempts);
			ja.put("cattempts", cattempts);
			ja.put("total", formatInt(a.getTotal()));
			ja.put("snatchRank", formatInt(a.getSnatchRank()));
			ja.put("cleanJerkRank", formatInt(a.getCleanJerkRank()));
			ja.put("totalRank", formatInt(a.getTotalRank()));
			Integer liftOrderRank = a.getLiftOrderRank();
			boolean notDone = a.getAttemptsDone() < 6;
			String blink =  (notDone ? " blink" : "");
			if (notDone) ja.put("classname", (liftOrderRank == 1 ? "current"+blink : (liftOrderRank == 2) ? "next" : ""));
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}
	
	private Object getOrigin() {
		return this;
	}
	
	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("Starting result board on FOP {}", fop.getName());
			setId("scoreboard-"+fop.getName());
			curGroup = fop.getGroup();
			getModel().setMasters(Competition.getCurrent().isMasters());
		});
		setTranslationMap();
		displayOrder = ImmutableList.of();
	}

	protected void doHide() {
		this.getModel().setHidden(true);
	}
	
	protected void doUpdate(Athlete a, UIEvent e) {
		logger.debug("doUpdate {} {}",a, a != null ? a.getAttemptsDone() : null);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			ScoreboardModel model = getModel();
			model.setHidden(a == null);
			if (a != null) {
				this.getElement().callFunction("reset");
				model.setFullName(a.getFullName());
				model.setTeamName(a.getTeam());
				model.setStartNumber(a.getStartNumber());
				String formattedAttempt = formatAttempt(a.getAttemptsDone());
				model.setAttempt(formattedAttempt);
				model.setWeight(a.getNextAttemptRequestedWeight());
				updateBottom(model,computeLiftType(a));
			}
		});
		if (a == null || a.getAttemptsDone() >= 6) {
			OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
			return;
		}
	}

	private void updateBottom(ScoreboardModel model, String liftType) {
		OwlcmsSession.withFop((fop) -> {
			curGroup = fop.getGroup();
			model.setGroupName(curGroup != null ? MessageFormat.format("Group {0} {1}", curGroup.getName(), liftType) : "");
		});
		model.setLiftsDone(MessageFormat.format("{0,choice,0#No attempts done.|1#1 attempt done.|1<{0} attempts done.}",liftsDone));		
		this.getElement().setPropertyJson("athletes", getAthletesJson(displayOrder));
	}
	
	private void doUpdateBottomPart(RefereeDecision e) {
		ScoreboardModel model = getModel();
		Athlete a = e.getAthlete();
		updateBottom(model,computeLiftType(a));
	}

	private String computeLiftType(Athlete a) {
		if (a == null) return "";
		String liftType = a.getAttemptsDone() >= 3 ? "Clean&Jerk" : "Snatch";
		return liftType;
	}
	/**
	 * Compute Json string ready to be used by web component template
	 * 
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 * 
	 * @param a
	 * @return json string with nested attempts values
	 */
	protected void getAttemptsJson(Athlete a) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer liftOrderRank = x.getLiftOrderRank();
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = (notDone ? " blink" : "");
			
			jri.put("goodBadClassName", "narrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
				case ACTUAL:
					if (!trim.isEmpty()) {
						if (trim.contentEquals("-") || trim.contentEquals("0")) {
							jri.put("goodBadClassName", "narrow fail");
							jri.put("stringValue", "-");
						} else {
							boolean failed = stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
							jri.put("stringValue", formatKg(stringValue));
						}
					}
					break;
				default:
					if (stringValue != null && !trim.isEmpty()) {
						String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
								: (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
						jri.put("goodBadClassName","narrow request");
						if (notDone) jri.put("className", highlight);
						jri.put("stringValue", stringValue);
					}
					break;
				}
			}
			
			if (ix < 3) {
				sattempts.set(ix, jri);
			} else {
				cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}
	
	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via QueryParameterReader interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			// sync with current status of FOP
			displayOrder = fop.getDisplayOrder();
			liftsDone = AthleteSorter.countLiftsDone(displayOrder);
			doUpdate(fop.getCurAthlete(), null);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		//TODO i18n t.key1 --> translation of key1 for getLocale()
		translations.put("key1","value1");
		translations.put("key2","value2");
		this.getElement().setPropertyJson("t", translations);
	}
	
	@Override
	public void doBreak(BreakStarted e) {
		uiEventLogger.debug("$$$ {} [{}]", e.getClass().getSimpleName(), LoggerUtils.whereFrom());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			BreakType breakType = fop.getBreakType();
			getModel().setFullName(inferGroupName()+" "+inferMessage(breakType));
			getModel().setTeamName("");
			getModel().setAttempt("");

			uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			this.getElement().callFunction("doBreak");
		}));
	}

	@Override
	public String getPageTitle() {
		return "Scoreboard";
	}
}
