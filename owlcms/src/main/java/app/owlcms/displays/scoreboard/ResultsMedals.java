/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.URLUtils;
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

public class ResultsMedals extends Results implements ResultsParameters, DisplayParameters {

	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedals.class);
	@SuppressWarnings("unused")
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	private Category category;
	private JsonArray cattempts;
	private TreeMap<Category, TreeSet<Athlete>> medals;
	private JsonArray sattempts;
	private EventBus uiEventBus;
	private boolean snatchCJTotalMedals;
	private AgeGroup ageGroup;
	private boolean teamFlags;
	private AgeDivision ageDivision;
	private String ageGroupPrefix;

	public ResultsMedals() {
		super();
		getTimer().setSilenced(true);
		getBreakTimer().setSilenced(true);
		getDecisions().setSilenced(true);
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
			setDisplay();

			updateBottom(computeLiftType(fop.getCurAthlete()), fop);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		// logger.debug("+++++++ ceremony event = {} {}", e, e.getTrace());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			Group ceremonyGroup = e.getCeremonyGroup();
			setGroup(ceremonyGroup);
			Category ceremonyCategory = e.getCeremonyCategory();
			setCategory(ceremonyCategory);
			doMedalsDisplay();
		}));
	}

	@Override
	public AgeDivision getAgeDivision() {
		return ageDivision;
	}

	@Override
	public AgeGroup getAgeGroup() {
		return ageGroup;
	}

	@Override
	public String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@Override
	public Category getCategory() {
		return category;
	}

	@Override
	public void setAgeDivision(AgeDivision ageDivision) {
		this.ageDivision = ageDivision;
	}

	@Override
	public void setAgeGroup(AgeGroup ageGroup) {
		this.ageGroup = ageGroup;
	}

	@Override
	public void setAgeGroupPrefix(String ageGroupPrefix) {
		this.ageGroupPrefix = ageGroupPrefix;
	}

	@Override
	public void setCategory(Category category) {
		this.category = category;
	}

	@Override
	public void setSilenced(boolean silent) {
	}


	@Subscribe
	public void slaveAllEvents(UIEvent e) {
		// logger.trace("*** {}", e);
	}

	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveBreakDone {}", e.getBreakType());
			setDisplay();
			doUpdate(e);
		}));
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveCeremonyDone {}", e.getCeremonyType());
			if (e.getCeremonyType() == CeremonyType.MEDALS) {
				// end of medals break.
				// do Nothing.
			}
		}));
	}

	@Override
	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.trace("------- slaveCeremonyStarted {}", e.getCeremonyType());
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.DecisionReset e) {
		uiLog(e);
		doRefresh(e);
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		doRefresh(e);
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		doRefresh(e);
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			setDisplay();
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		// logger.trace("****** slaveStartLifting ");
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			setDisplay();
			// If this page was opened in replacement of a display, go back to the display.
			unregister(this, uiEventBus);
//			retrieveFromSessionStorage("pageURL", result -> {
//				if (result != null && !result.isBlank()) {
//					UI.getCurrent().getPage().setLocation(result);
//				} else {
//					this.getElement().callJsFunction("reset");
//				}
//			});
		});
	}

	@Override
	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			syncWithFOP(e);
		});
	}

	@Subscribe
	public void slaveVideoRefresh(UIEvent.VideoRefresh e) {
		var fop = OwlcmsSession.getFop();
		this.setGroup(fop.getVideoGroup());
		this.setCategory(fop.getVideoCategory());
		// logger.info("videoRefresh {} {}", getGroup() != null ? getGroup().getName() :
		// null , getCategory() != null ? getCategory().getName() : null);
		doRefresh(e);
	}

	@Override
	protected void doEmpty() {
		// no need to hide, text is self evident.
		// this.setHidden(true);
	}

	protected void doUpdate(UIEvent e) {
		// logger.trace("---------- doUpdate {} {} {}", e != null ?
		// e.getClass().getSimpleName() : "no event");
		//boolean leaveTopAlone = false;
//		if (e instanceof UIEvent.LiftingOrderUpdated) {
//			LiftingOrderUpdated e2 = (UIEvent.LiftingOrderUpdated) e;
//			if (e2.isInBreak()) {
//				leaveTopAlone = !e2.isDisplayToggle();
//			} else {
//				leaveTopAlone = !e2.isCurrentDisplayAffected();
//			}
//		}

		FieldOfPlay fop = OwlcmsSession.getFop();
//		if (!leaveTopAlone) {
//			this.getElement().callJsFunction("reset");
//		}
		logger.debug("updating bottom");
		updateBottom(null, fop);
	}

	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
		String category;
		category = curCat != null ? curCat.getTranslatedName() : "";
		if (isAbbreviatedName()) {
			ja.put("fullName", a.getAbbreviatedName() != null ? a.getAbbreviatedName() : "");
		} else {
			ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		}
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a);
		ja.put("sattempts", sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));
		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			int snatchRank = mainRankings.getSnatchRank();
			ja.put("snatchRank", formatRank(snatchRank));
			ja.put("snatchMedal", snatchRank <= 3 ? "medal" + snatchRank : "");

			int cleanJerkRank = mainRankings.getCleanJerkRank();
			ja.put("cleanJerkRank", formatRank(cleanJerkRank));
			ja.put("cleanJerkMedal", cleanJerkRank <= 3 ? "medal" + cleanJerkRank : "");

			int totalRank = mainRankings.getTotalRank();
			ja.put("totalRank", formatRank(totalRank));
			ja.put("totalMedal", totalRank <= 3 ? "medal" + totalRank : "");
		} else {
			logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());
		
		ja.put("sinclair", computedScore(a));
		ja.put("sinclairRank", computedScoreRank(a));
		
		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		// only show flags when medals are for a single category
		String prop = null;
		if (getCategory() != null) {
			String team = a.getTeam();
			String teamFileName = URLUtils.sanitizeFilename(team);

			if (teamFlags && !team.isBlank()) {
				prop = URLUtils.getImgTag("flags/", teamFileName, ".svg");
				if (prop == null) {
					prop = URLUtils.getImgTag("flags/", teamFileName, ".png");
					if (prop == null) {
						prop = URLUtils.getImgTag("flags/", teamFileName, ".jpg");
					}
				}
			}
			ja.put("flagURL", prop != null ? prop : "");
			ja.put("flagClass", "flags");
		} else {
			ja.put("flagURL", prop != null ? prop : "");
		}

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
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		doMedalsDisplay();
//		if (!isSilenced() || !isDownSilenced()) {
//			SoundUtils.enableAudioContextNotification(this.getElement());
//		}
	}

	@Override
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

	@Override
	protected void updateBottom(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom");
		this.getElement().setProperty("groupName", "");
		this.getElement().setProperty("liftDone", "-");
		computeMedalsJson(medals);
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
					if (mcX == 0) {
						jMC.put("showCatHeader", "");
					} else {
						jMC.put("showCatHeader", "display:none;");
					}
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

	private void doMedalsDisplay() {
		// fop obtained via FOPParameters interface default methods.
		OwlcmsSession.withFop(fop -> {
			medalsInit();
			checkVideo(this);
			teamFlags = URLUtils.checkFlags();
			if (this.getCategory() == null) {
				if (this.getGroup() != null) {
					medals = Competition.getCurrent().getMedals(this.getGroup(), false);
				} else {
					// we listen on uiEventBus.
					uiEventBus = uiEventBusRegister(this, fop);
					medals = Competition.getCurrent().getMedals(OwlcmsSession.getFop().getGroup(), false);
				}
				this.getElement().setProperty("fillerDisplay", "");
			} else {
				TreeSet<Athlete> catMedals = Competition.getCurrent().computeMedalsForCategory(this.getCategory());
				// logger.debug("group {} category {} catMedals {}", getGroup(), getCategory(),
				// catMedals);
				medals = new TreeMap<>();
				medals.put(this.getCategory(), catMedals);
				this.getElement().setProperty("fillerDisplay", "display: none;");
			}
			setDisplay();
			this.getElement().setProperty("showLiftRanks", Competition.getCurrent().isSnatchCJTotalMedals()); 
			computeMedalsJson(medals);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
            this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
		});

		if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			getElement().setProperty("noLiftRanks", "noranks");
		}
		this.getElement().setProperty("displayTitle", Translator.translate("CeremonyType.MEDALS"));
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
				setDisplay();
				computeMedalsJson(medals);
			});
		});
		// medal stuff can wait.
		t1.setPriority(Thread.MIN_PRIORITY);
		t1.start();
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	/**
	 * Compute Json string ready to be used by web component template
	 *
	 * CSS classes are pre-computed and passed along with the values; weights are formatted.
	 *
	 * @param a
	 * @param fop
	 * @return json string with nested attempts values
	 */
	private void getAttemptsJson(Athlete a) {
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

	private void medalsInit() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board on FOP {}", FieldOfPlay.getLoggingName(fop));
			setId("medals-" + fop.getName());
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
			this.setGroup(fop.getVideoGroup());
			this.setCategory(fop.getVideoCategory());
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

//	private void retrieveFromSessionStorage(String key, SerializableConsumer<String> resultHandler) {
//		getElement().executeJs("return window.sessionStorage.getItem($0);", key)
//		        .then(String.class, resultHandler);
//	}

	private void setDisplay() {
		OwlcmsSession.withFop(fop -> {
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());
			Group group = fop.getGroup();
			String description = null;
			if (group != null) {
				description = group.getDescription();
				if (description == null) {
					description = Translator.translate("Group_number", group.getName());
				}
			}
			this.getElement().setProperty("groupDescription", description != null ? description : "");
		});
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
			setDisplay();
			doUpdate(e);
		}
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
	}
	
	private String computedScoreRank(Athlete a) {
		Integer value = Ranking.getRanking(a, Competition.getCurrent().getScoringSystem());
		return value != null && value > 0 ? "" + value : "-";
	}

	private String computedScore(Athlete a) {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		double value = Ranking.getRankingValue(a, scoringSystem);
		String score = value > 0.001 ? String.format("%.3f", value) : "-";
		return score;
	}

}