/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.ContextFreeDisplayParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.displays.VideoOverride;
import app.owlcms.displays.options.DisplayOptions;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.BreakDisplay;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.LiftingOrderUpdated;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class Scoreboard
 *
 * Show athlete 6-attempt results and leaders for the athlete's category
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("resultsmedals-template")
@JsModule("./components/ResultsMedals.js")
@Route("displays/resultsMedals")

public class ResultsMedals extends PolymerTemplate<TemplateModel>
        implements ContextFreeDisplayParameters, SafeEventBusRegistration, UIEventProcessor, BreakDisplay,
        HasDynamicTitle, VideoOverride,
        RequireDisplayLogin {

	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedals.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	private Category category;
	private JsonArray cattempts;
	private boolean darkMode = true;
	private Dialog dialog;
	private Timer dialogTimer;
	private Double emFontSize;
	private FieldOfPlay fop;
	private Group group;
	private boolean initializationNeeded;
	private Location location;
	private UI locationUI;

	private TreeMap<Category, TreeSet<Athlete>> medals;
	private String routeParameter;
	private JsonArray sattempts;
	private boolean silenced = true;
	private EventBus uiEventBus;

	private Map<String, List<String>> urlParameterMap = new HashMap<>();
	private boolean snatchCJTotalMedals;
	private boolean video;

	/**
	 * Instantiates a new results board.
	 */
	public ResultsMedals() {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		// js files add the build number to file names in order to prevent cache
		// collisions
		this.getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#addDialogContent(com.vaadin.flow.component.Component,
	 *      com.vaadin.flow.component.orderedlayout.VerticalLayout)
	 */
	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {
		DisplayOptions.addLightingEntries(vl, target, this);
		DisplayOptions.addRule(vl);
		DisplayOptions.addSizingEntries(vl, target, this);
	}

	@Override
	public void doBreak(UIEvent event) {
		if (!(event instanceof UIEvent.BreakStarted)) {
			return;
		}
		// logger.trace("break event = {} {} {}", e.getBreakType(), e.getTrace(),
		// ceremonyGroup);

		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			this.getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			setDisplay(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			this.getElement().callJsFunction("doBreak");
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			Group ceremonyGroup = e.getCeremonyGroup();
			setGroup(ceremonyGroup);
			Category ceremonyCategory = e.getCeremonyCategory();
			setCategory(ceremonyCategory);

			this.getElement().setProperty("fullName",
			        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
			this.getElement().setProperty("teamName", "");
			this.getElement().setProperty("attempt", "");
			setDisplay(false);

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
			this.getElement().callJsFunction("doBreak");
		}));
	}

	public Category getCategory() {
		return category;
	}

	/**
	 * return dialog, but only on first call.
	 *
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#getDialog()
	 */
	@Override
	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public Timer getDialogTimer() {
		return this.dialogTimer;
	}

	@Override
	public Double getEmFontSize() {
		return emFontSize;
	}

	public FieldOfPlay getFop() {
		return fop;
	}

	public Group getGroup() {
		return group;
	}

	@Override
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getPageTitle() {
		return getTranslation("CeremonyType.MEDALS") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public String getRouteParameter() {
		return this.routeParameter;
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public boolean isDarkMode() {
		return darkMode;
	}

	@Override
	public boolean isIgnoreFopFromURL() {
		return false;
	}

	@Override
	public boolean isIgnoreGroupFromURL() {
		return false;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#isShowInitialDialog()
	 */
	@Override
	public boolean isShowInitialDialog() {
		return this.initializationNeeded;
	}

	@Override
	public boolean isSilenced() {
		return silenced;
	}

	@Override
	public void setCategory(Category cat) {
		this.category = cat;
	}

	@Override
	public void setDarkMode(boolean dark) {
		this.darkMode = dark;
	}

	@Override
	public void setDialog(Dialog dialog) {
		this.dialog = dialog;
	}

	@Override
	public void setDialogTimer(Timer timer) {
		this.dialogTimer = timer;
	}

	@Override
	public void setEmFontSize(Double emFontSize) {
		this.emFontSize = emFontSize;
		doChangeEmSize();
	}

	@Override
	public void setFop(FieldOfPlay fop) {
		this.fop = fop;
	}

	@Override
	public void setGroup(Group group) {
		this.group = group;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setRouteParameter(String routeParameter) {
		this.routeParameter = routeParameter;
	}
	
	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setShowInitialDialog(boolean)
	 */
	@Override
	public void setShowInitialDialog(boolean b) {
		this.initializationNeeded = true;
	}

	@Override
	public void setSilenced(boolean silent) {
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveAllEvents(UIEvent e) {
		// logger.trace("*** {}", e);
	}

	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveBreakDone {}", e.getBreakType());
			setDisplay(false);
			doUpdate(e);
		}));
	}

	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveCeremonyDone {}", e.getCeremonyType());
			if (e.getCeremonyType() == CeremonyType.MEDALS) {
				// end of medals break.
				// If this page was opened in replacement of a display, go back to the display.
				unregister(this, uiEventBus);
				retrieveFromSessionStorage("pageURL", result -> {
					if (result != null && !result.isBlank()) {
						UI.getCurrent().getPage().setLocation(result);
					}
				});
			}
		}));
	}

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace("------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		doRefresh(e);
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		doRefresh(e);
	}

	@Subscribe
	public void slaveDecision(UIEvent.DecisionReset e) {
		uiLog(e);
		doRefresh(e);
	}

	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay(false);
			doBreak(e);
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		// logger.trace("****** slaveStartLifting ");
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay(false);
			// If this page was opened in replacement of a display, go back to the display.
			unregister(this, uiEventBus);
			retrieveFromSessionStorage("pageURL", result -> {
				if (result != null && !result.isBlank()) {
					UI.getCurrent().getPage().setLocation(result);
				} else {
					this.getElement().callJsFunction("reset");
				}
			});
		});
	}

	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	@Subscribe
	public void slaveVideoRefresh(UIEvent.VideoRefresh e) {
		this.setGroup(fop.getVideoGroup());
		this.setCategory(fop.getVideoCategory());
		// logger.info("videoRefresh {} {}", getGroup() != null ? getGroup().getName() :
		// null , getCategory() != null ? getCategory().getName() : null);
		doRefresh(e);
	}

	protected void doEmpty() {
		// no need to hide, text is self evident.
		// this.setHidden(true);
	}

	protected void doUpdate(UIEvent e) {
		// logger.trace("---------- doUpdate {} {} {}", e != null ?
		// e.getClass().getSimpleName() : "no event");
		boolean leaveTopAlone = false;
		if (e instanceof UIEvent.LiftingOrderUpdated) {
			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
			if (e2.isInBreak()) {
				leaveTopAlone = !e2.isDisplayToggle();
			} else {
				leaveTopAlone = !e2.isCurrentDisplayAffected();
			}
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (!leaveTopAlone) {
			this.getElement().callJsFunction("reset");
		}
		logger.debug("updating bottom");
		updateBottom(null, fop);
	}

	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
		String category;
		category = curCat != null ? curCat.getTranslatedName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a, liftOrderRank);
		ja.put("sattempts", sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			ja.put("snatchRank", formatRank(mainRankings.getSnatchRank()));
			ja.put("cleanJerkRank", formatRank(mainRankings.getCleanJerkRank()));
			ja.put("totalRank", formatRank(mainRankings.getTotalRank()));
		} else {
			logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup() != null ? a.getGroup().getName() : "");
		Double double1 = a.getAttemptsDone() <= 3 ? a.getSinclairForDelta()
		        : a.getSinclair();
		ja.put("sinclair", double1 > 0.001 ? String.format("%.3f", double1) : "-");
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");
		ja.put("sinclairRank", a.getSinclairRank() != null ? "" + a.getSinclairRank() : "-");

		String highlight = "";
		ja.put("classname", highlight);
	}

	/**
	 * @param groupAthletes, List<Athlete> liftOrder
	 * @return
	 */
	protected JsonValue getAthletesJson(List<Athlete> displayOrder, final FieldOfPlay _unused) {
		snatchCJTotalMedals = Competition.getCurrent().isSnatchCJTotalMedals();
		JsonArray jath = Json.createArray();
		AtomicInteger athx = new AtomicInteger(0);
//        Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();

		athletes.stream()
		        .filter(a -> isMedalist(a))
		        .forEach(a -> {
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

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.
	 * AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			if (this.getCategory() == null) {
				if (this.getGroup() != null) {
					medals = Competition.getCurrent().getMedals(this.getGroup(), false);
				} else {
					// we listen on uiEventBus.
					uiEventBus = uiEventBusRegister(this, fop);
					medals = Competition.getCurrent().getMedals(OwlcmsSession.getFop().getGroup(), false);
				}
			} else {
				TreeSet<Athlete> catMedals = Competition.getCurrent().computeMedalsForCategory(this.getCategory());
				// logger.debug("group {} category {} catMedals {}", getGroup(), getCategory(),
				// catMedals);
				medals = new TreeMap<>();
				medals.put(this.getCategory(), catMedals);
			}
			setDisplay(false);
			computeMedalsJson(medals);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
		switchLightingMode(this, isDarkMode(), true);
		if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			getElement().setProperty("noLiftRanks", "noranks");
		}
		SoundUtils.enableAudioContextNotification(this.getElement());
		
		checkVideo("styles/video/results.css", routeParameter, this);

		this.getElement().setProperty("displayTitle", Translator.translate("CeremonyType.MEDALS"));
	}

	public void setVideo(boolean video) {
		this.video = video;
	}

	protected void setTranslationMap() {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		this.getElement().setPropertyJson("t", translations);
	}

	protected void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	private void computeCategoryMedalsJson(TreeMap<Category, TreeSet<Athlete>> medals2) {
		OwlcmsSession.withFop(fop -> {
			TreeSet<Athlete> medalists = medals2.get(getCategory());
			// logger.debug("medalists {}", medalists);

			JsonArray jsonMCArray = Json.createArray();
			JsonObject jMC = Json.createObject();
			int mcX = 0;
			if (medalists != null && !medalists.isEmpty()) {
				jMC.put("categoryName", getCategory().getTranslatedName());
				jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
				// logger.debug("medalCategory: {}", jMC.toJson());
				jsonMCArray.set(mcX, jMC);
				mcX++;
			}

			this.getElement().setPropertyJson("medalCategories", jsonMCArray);
			if (mcX == 0) {
				this.getElement().setProperty("noCategories", true);
			}
		});
	}

	private void computeGroupMedalsJson(TreeMap<Category, TreeSet<Athlete>> medals2) {
		OwlcmsSession.withFop(fop -> {
			// logger.debug("computeGroupMedalsJson = {} {}", getGroup(),
			// LoggerUtils.stackTrace());
			JsonArray jsonMCArray = Json.createArray();
			int mcX = 0;
			for (Entry<Category, TreeSet<Athlete>> medalCat : medals2.entrySet()) {
				JsonObject jMC = Json.createObject();
				TreeSet<Athlete> medalists = medalCat.getValue();
				if (medalists != null && !medalists.isEmpty()) {
					jMC.put("categoryName", medalCat.getKey().getTranslatedName());
					jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
					// logger.debug("medalCategory: {}", jMC.toJson());
					jsonMCArray.set(mcX, jMC);
					mcX++;
				}
			}
			// logger.debug("medalCategories {}", jsonMCArray.toJson());
			this.getElement().setPropertyJson("medalCategories", jsonMCArray);
			if (mcX == 0) {
				this.getElement().setProperty("noCategories", true);
			}
		});
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void computeMedalsJson(TreeMap<Category, TreeSet<Athlete>> medals2) {
		if (getCategory() != null) {
			computeCategoryMedalsJson(medals2);
		} else {
			computeGroupMedalsJson(medals2);
		}
	}

	private void doChangeEmSize() {
		if (getEmFontSize() != null) {
			this.getElement().setProperty("sizeOverride", " --tableFontSize:" + getEmFontSize() + "rem;");
		}
	}

	private void doRefresh(UIEvent e) {
		Thread t1 = new Thread(() -> {
			UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
				if (this.getCategory() == null) {
					if (this.getGroup() != null) {
						medals = Competition.getCurrent().getMedals(this.getGroup(), false);
					} else {
						OwlcmsSession.getCurrent();
						medals = Competition.getCurrent().getMedals(OwlcmsSession.getFop().getGroup(), false);
					}
				} else {
					TreeSet<Athlete> catMedals = Competition.getCurrent().computeMedalsForCategory(this.getCategory());
					// logger.debug("group {} category {} catMedals {}", getGroup(), getCategory(),
					// catMedals);
					medals = new TreeMap<>();
					medals.put(this.getCategory(), catMedals);
				}
				setDisplay(false);
				computeMedalsJson(medals);
			});
		});
		// medal stuff can wait.
		t1.setPriority(Thread.MIN_PRIORITY);
		t1.start();
	}

	private String formatInt(Integer total) {
		if (total == null || total == 0) {
			return "-";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else if (total < 0) {
			return "(" + Math.abs(total) + ")";
		} else {
			return total.toString();
		}
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private String formatRank(Integer total) {
		if (total == null || total == 0) {
			return "";
		} else if (total == -1) {
			return "inv.";// invited lifter, not eligible.
		} else {
			return total.toString();
		}
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are
	 * formatted.
	 *
	 * @param a
	 * @param fop
	 * @return json string with nested attempts values
	 */
	private void getAttemptsJson(Athlete a, int liftOrderRank) {
		sattempts = Json.createArray();
		cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;

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
							boolean failed = stringValue != null && stringValue.startsWith("-");
							jri.put("goodBadClassName", failed ? "narrow fail" : "narrow good");
							jri.put("stringValue", formatKg(stringValue));
						}
					}
					break;
				default:
					if (stringValue != null && !trim.isEmpty()) {
						// logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

						String highlight = "";
						jri.put("goodBadClassName", "narrow request");
						if (notDone) {
							jri.put("className", highlight);
						}
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

	private Object getOrigin() {
		return this;
	}

	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board on FOP {}", fop.getLoggingName());
			setId("scoreboard-" + fop.getName());
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
	}

	private boolean isMedalist(Athlete a) {
		if (snatchCJTotalMedals) {
			int snatchRank = a.getSnatchRank();
			if (snatchRank <= 3 && snatchRank > 0) {
				return true;
			}
			int cjRank = a.getCleanJerkRank();
			if (cjRank <= 3 && cjRank > 0) {
				return true;
			}
		}
		int totalRank = a.getTotalRank();
		if (totalRank <= 3 && totalRank > 0) {
			return true;
		}
		return false;
	}

	public boolean isVideo() {
		return video;
	}

	private void retrieveFromSessionStorage(String key, SerializableConsumer<String> resultHandler) {
		getElement().executeJs("return window.sessionStorage.getItem($0);", key)
		        .then(String.class, resultHandler);
	}

	private void setDisplay(boolean hidden) {
		this.getElement().setProperty("hiddenBlockStyle", (hidden ? "display:none" : "display:block"));
		this.getElement().setProperty("inactiveBlockStyle", (hidden ? "display:block" : "display:none"));
		this.getElement().setProperty("hiddenGridStyle", (hidden ? "display:none" : "display:grid"));
		this.getElement().setProperty("inactiveGridStyle", (hidden ? "display:grid" : "display:none"));
		this.getElement().setProperty("inactiveClass", (hidden ? "bigTitle" : ""));
		this.getElement().setProperty("videoHeaderDisplay", (hidden || !isVideo() ? "display:none" : "display:flex"));
		this.getElement().setProperty("normalHeaderDisplay", (hidden || isVideo() ? "display:none" : "display:block"));
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		// logger.debug("sync {}", e.getState());
		switch (e.getState()) {
		case INACTIVE:
			doEmpty();
			break;
		case BREAK:
			if (e.getGroup() == null) {
				doEmpty();
			} else {
				doUpdate(e);
				doBreak(e);
			}
			break;
		default:
			setDisplay(false);
			doUpdate(e);
		}
	}

	private void uiLog(UIEvent e) {
		uiEventLogger.debug("### {} {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
		        this.getOrigin(), e.getOrigin(), LoggerUtils.whereFrom());
	}

	private void updateBottom(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom");
		this.getElement().setProperty("groupName", "");
		this.getElement().setProperty("liftDone", "-");
		computeMedalsJson(medals);
	}
}