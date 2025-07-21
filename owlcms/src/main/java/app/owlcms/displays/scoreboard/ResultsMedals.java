/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.CategoryRepository;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.team.Team;
import app.owlcms.fieldofplay.FOPState;
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

	private static final boolean ONLY_FINISHED = true;
	final private Logger logger = (Logger) LoggerFactory.getLogger(ResultsMedals.class);
	@SuppressWarnings("unused")
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	private Category category;
	private JsonArray cattempts;
	private TreeMap<String, List<Athlete>> medals;
	private JsonArray sattempts;
	private EventBus uiEventBus;
	private boolean snatchCJTotalMedals;
	private AgeGroup ageGroup;
	private boolean teamFlags;
	private Championship ageDivision;
	private String ageGroupPrefix;
	private UI ui;
	private boolean ceremony;

	public ResultsMedals() {
		getTimer().setSilenced(true);
		getBreakTimer().setSilenced(true);
		getDecisions().setSilenced(true);
	}

	@Override
	public void doBreak(UIEvent event) {
		if (!(event instanceof UIEvent.BreakStarted)) {
			return;
		}
		OwlcmsSession.withFop(fop -> doBreak(fop));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		this.setCeremony(true);
		OwlcmsSession.withFop((fop) -> {
			Group ceremonyGroup = e.getCeremonySession();
			setGroup(ceremonyGroup);
			Category ceremonyCategory = e.getCeremonyCategory();
			setCategory(ceremonyCategory);
			// logger.debug("ceremony event = {} {} {} {}", e, ceremonyGroup, ceremonyCategory, LoggerUtils.stackTrace());

			// medalsInit();
			computeStylesDir(this);
			this.teamFlags = URLUtils.checkFlags();
			doMedals(this.getFop());

			if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
				getElement().setProperty("noLiftRanks", "noranks");
			}
			this.getElement().setProperty("displayTitle", Translator.translate("CeremonyType.MEDALS"));
		});
	}

	@Override
	public AgeGroup getAgeGroup() {
		return this.ageGroup;
	}

	@Override
	public String getAgeGroupPrefix() {
		return this.ageGroupPrefix;
	}

	@Override
	public Category getCategory() {
		return this.category;
	}

	@Override
	public Championship getChampionship() {
		return this.ageDivision;
	}

	public boolean isCeremony() {
		return this.ceremony;
	}

	@Override
	public boolean isShowInitialDialog() {
		return false;
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

	public void setCeremony(boolean ceremony) {
		this.ceremony = ceremony;
	}

	@Override
	public void setChampionship(Championship ageDivision) {
		this.ageDivision = ageDivision;
	}

	public void setScoreRanks(boolean scoreNeeded) {
		this.getElement().setProperty("showSinclair", scoreNeeded);
		this.getElement().setProperty("showSinclairRank", scoreNeeded);
	}

	@Override
	public void setSilenced(boolean silent) {
	}

	public void setTitles(JsonObject jMC, Category cat) {
		jMC.put("categoryName", cat.getDisplayName());
		AgeGroup ageGroup2 = cat.getAgeGroup();
		if (ageGroup2 == null) {
			logger.error("category without ageGroup: {}",cat);
		}
		Ranking scoringSystem = ageGroup2 != null ? ageGroup2.getScoringSystem() : null;
		String rankingTitle = Translator.translate("Rank");
		if (scoringSystem != null && scoringSystem != Ranking.TOTAL) {
			String scoreScoringTitle = Translator.translate("Score");
			scoreScoringTitle = Ranking.getScoringTitle(scoringSystem);
			jMC.put("rankingTitle", "");
			jMC.put("scoreScoringTitle", scoreScoringTitle);
			jMC.put("scoreRankingTitle", rankingTitle);
		} else {
			jMC.put("rankingTitle", rankingTitle);
			jMC.put("scoreScoringTitle", "");
			jMC.put("scoreRankingTitle", "");
		}
	}

	@Subscribe
	public void slaveAllEvents(UIEvent e) {
		// uiLog(e);
	}

	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		uiLog(e);
		this.getUi().access(() -> OwlcmsSession.withFop(fop -> {
			// logger.trace("------- slaveBreakDone {}", e.getBreakType());
			setDisplay();
			doUpdate(e);
		}));
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		this.setCeremony(false);
		this.getUi().access(() -> OwlcmsSession.withFop(fop -> {
			if (e.getCeremonyType() == CeremonyType.MEDALS) {
				// end of medals break.
				syncWithFOP(fop);
			}
		}));
	}

	@Override
	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		// logger.debug("------- slaveCeremonyStarted {} {} {}", e.getCeremonyType(), e.getCeremonySession(), e.getCeremonyCategory());
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			setDisplay();
			doCeremony(e);
		});
	}

	@Subscribe
	public void slaveDecision(UIEvent.DecisionReset e) {
		uiLog(e);
		this.getUi().access(() -> {
			doRefresh(e);
		});
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		this.setCategory(null);
		this.setGroup(e.getGroup());
		this.getUi().access(() -> {
			doRefresh(e);
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		this.getUi().access(() -> {
			doRefresh(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		uiLog(e);
		this.getUi().access(() -> {
			setDisplay();
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		this.getUi().access(() -> {
			setDisplay();
			// this is suspicious. when used behind main scoreboard
			// we probably need a toggle to ignore updates.
		});
	}

	@Override
	@Subscribe
	public void slaveSwitchGroup(UIEvent.SwitchGroup e) {
		uiLog(e);
		this.getUi().access(() -> {
			syncWithFOP(e);
		});
	}

	@Subscribe
	public void slaveVideoRefresh(UIEvent.VideoRefresh e) {
		if (!isVideo()) {
			return;
		}
		this.ui.access(() -> {
			uiLog(e);
			var fop = e.getFop();
			this.setGroup(fop.getVideoGroup());
			this.setCategory(fop.getVideoCategory());
			doRefresh(e);
		});
	}

	public void syncWithFOP(FieldOfPlay fop) {
		// logger.debug("syncWithFOP");
		switch (fop.getState()) {
			case INACTIVE:
				this.setGroup(null);
				this.setCategory(null);
				doEmpty();
				break;
			// case BREAK:
			default:
				setCeremony(fop.getCeremonyType() == CeremonyType.MEDALS);
				if (!this.isCeremony()) {
					this.setGroup(fop.getGroup());
					this.setCategory(null);
					doRefresh(new UIEvent.SwitchGroup(fop.getGroup(), FOPState.BREAK, fop.getCurAthlete(), this, fop));
				}
				break;
		}
		ui.access(() -> {
			pushEmSize(this.getElement());
			pushTeamWidth(this.getElement());
		});
	}

	@Override
	protected void doEmpty() {
		// no need to hide, text is self evident.
		// this.setHidden(true);
	}

	protected void doUpdate(UIEvent e) {
		FieldOfPlay fop = e.getFop();
		this.logger.debug("updating bottom");
		updateDisplay(null, fop);
	}

	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank) {
		if (a.getGroup() == null) {
			return;
		}
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
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
		ja.put("sattempts", this.sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.put("cattempts", this.cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));

		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			int snatchRank = mainRankings.getSnatchRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("snatchRank", formatRank(snatchRank));
				ja.put("snatchMedal", snatchRank <= 3 ? "medal" + snatchRank : "");
			} else {
				ja.put("snatchRank", "");
				ja.put("snatchMedal", "");
			}

			int cleanJerkRank = mainRankings.getCleanJerkRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("cleanJerkRank", formatRank(cleanJerkRank));
				ja.put("cleanJerkMedal", cleanJerkRank <= 3 ? "medal" + cleanJerkRank : "");
			} else {
				ja.put("cleanJerkRank", "");
				ja.put("cleanJerkMedal", "");
			}

			int totalRank = mainRankings.getTotalRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("totalRank", formatRank(totalRank));
				ja.put("totalMedal", totalRank <= 3 ? "medal" + totalRank : "");
			} else {
				ja.put("totalRank", "");
				ja.put("totalMedal", "");
			}
		} else {
			this.logger.error("main rankings null for {}", a);
		}
		ja.put("group", a.getGroup().getName());
		ja.put("subCategory", a.getSubCategory());

		if (a.getComputedScoringSystem() != Ranking.TOTAL) {
			ja.put("sinclair", computedScore(a));
			int computedScoreRank = mainRankings.getCategoryScoreRank();
			ja.put("sinclairRank", computedScoreRank);
			ja.put("sinclairMedal", computedScoreRank <= 3 ? "medal" + computedScoreRank : "");
		}

		ja.put("custom1", a.getCustom1() != null ? a.getCustom1() : "");
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		String prop = null;
		if (!Config.getCurrent().featureSwitch("medalsForCategoryOnly")) {
			// only show flags when medals are for a single category
			String team = a.getTeam();
			if (this.teamFlags && !team.isBlank()) {
				prop = Team.getImgTag(team, "");
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
		this.snatchCJTotalMedals = Competition.getCurrent().isSnatchCJTotalMedals();
		JsonArray jath = Json.createArray();
		AtomicInteger athx = new AtomicInteger(0);
		// Category prevCat = null;
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
				        this.logger.trace("long team {}", team);
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
		this.setUi(attachEvent.getUI());
		// we listen on uiEventBus.
		OwlcmsSession.withFop(fop -> {
			this.uiEventBus = uiEventBusRegister(this, fop);
			if (this.getFop() == null) {
				this.setFop(fop);
			}
			doMedalsDisplay();
		});
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

	@Override
	protected void setWideTeamNames(boolean wide) {
		this.getElement().setProperty("teamWidthClass", (wide ? "wideTeams" : "narrowTeams"));
	}

	@Override
	protected void uiLog(UIEvent e) {
		// this.logger./**/warn(">>>>> {} {} {} {}",
		// this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.getOrigin(),
		// LoggerUtils.whereFrom());
	}

	@Override
	protected void updateDisplay(String liftType, FieldOfPlay fop) {
		// logger.debug("updateBottom");
		this.getElement().setProperty("groupInfo", "");
		this.getElement().setProperty("liftDone", "-");
		computeMedalsJson(this.getMedals());
	}

	private void computeCategoryMedalsJson(TreeMap<String, List<Athlete>> medals2) {
		OwlcmsSession.withFop(fop -> {
			String catCode = getCategory().getCode();
			List<Athlete> medalists = medals2.get(catCode);
			Category cat = CategoryRepository.findByCode(catCode);
			boolean scoreNeeded = (medalists != null && !medalists.isEmpty()) &&
			        (medalists.get(0).getComputedScoringSystem() != Ranking.TOTAL);
			setScoreRanks(scoreNeeded);

			JsonArray jsonMCArray = Json.createArray();
			JsonObject jMC = Json.createObject();
			int mcX = 0;
			if (medalists != null && !medalists.isEmpty()) {
				jMC.put("categoryName", getCategory().getDisplayName());
				setTitles(jMC, cat);
				jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), fop));
				jsonMCArray.set(mcX, jMC);
				mcX++;
			}

			this.getElement().setPropertyJson("medalCategories", jsonMCArray);
			if (mcX == 0) {
				this.getElement().setProperty("noCategories", true);
			}
		});
	}

	private void computeGroupMedalsJson(TreeMap<String, List<Athlete>> medals2) {
		// logger.debug("computeGroupMedalsJson group = {}\n{}", this.getGroup(), LoggerUtils.stackTrace());
		JsonArray jsonMCArray = Json.createArray();
		int mcX = 0;

		boolean scoreNeeded = false;
		for (Entry<String, List<Athlete>> medalCat : medals2.entrySet()) {
			List<Athlete> athletes = medalCat.getValue();
			if (athletes != null && !athletes.isEmpty()) {
				if (athletes.get(0).getComputedScoringSystem() != Ranking.TOTAL) {
					scoreNeeded = true;
					break;
				}
			}
		}

		for (Entry<String, List<Athlete>> medalCat : medals2.entrySet()) {
			JsonObject jMC = Json.createObject();
			List<Athlete> medalists = medalCat.getValue();
			if (medalists != null && !medalists.isEmpty()) {
				String key = medalCat.getKey();
				Category cat = CategoryRepository.findByCode(key);

				setTitles(jMC, cat);

				jMC.put("leaders", getAthletesJson(new ArrayList<>(medalists), null));
				if (mcX == 0) {
					jMC.put("showCatHeader", "");
				} else {
					jMC.put("showCatHeader", "display:none;");
				}

				setScoreRanks(scoreNeeded);
				jsonMCArray.set(mcX, jMC);
				mcX++;
			}
		}
		this.getElement().setPropertyJson("medalCategories", jsonMCArray);
		if (mcX == 0) {
			this.getElement().setProperty("noCategories", true);
		}
	}

	private String computeLiftType(Athlete a) {
		if (a == null || a.getAttemptsDone() > 6) {
			return null;
		}
		String liftType = a.getAttemptsDone() >= 3 ? Translator.translate("Clean_and_Jerk")
		        : Translator.translate("Snatch");
		return liftType;
	}

	private void computeMedalsJson(TreeMap<String, List<Athlete>> medals2) {
		if (getCategory() != null) {
			computeCategoryMedalsJson(medals2);
		} else {
			computeGroupMedalsJson(medals2);
		}
	}

	private void doBreak(FieldOfPlay fop) {
		this.getElement().setProperty("fullName",
		        inferGroupName() + " &ndash; " + inferMessage(fop.getBreakType(), fop.getCeremonyType(), true));
		this.getElement().setProperty("teamName", "");
		this.getElement().setProperty("attempt", "");
		setDisplay();
		updateDisplay(computeLiftType(fop.getCurAthlete()), fop);
	}

	// private void retrieveFromSessionStorage(String key, SerializableConsumer<String> resultHandler) {
	// getElement().executeJs("return window.sessionStorage.getItem($0);", key)
	// .then(String.class, resultHandler);
	// }

	private void doMedals(FieldOfPlay fop2) {
		if (this.getCategory() == null) {
			if (this.getGroup() != null) {
				// logger.debug("=== getgroup {}", this.getGroup());
				this.setMedals(Competition.getCurrent().getMedals(this.getGroup(), ONLY_FINISHED));
			} else {
				// logger.debug("=== getgroup from FOP {}", fop2.getGroup());
				this.setMedals(Competition.getCurrent().getMedals(fop2.getGroup(), ONLY_FINISHED));
			}
			// this.getElement().setProperty("fillerDisplay", "");
		} else {
			List<Athlete> catMedals = Competition.getCurrent().computeMedalsForCategory(this.getCategory());
			// logger.debug("=== group {} category {} catMedals {}", getGroup(), getCategory(), catMedals.stream().map(a -> a.getAbbreviatedName()).toList());
			this.setMedals(new TreeMap<>());
			this.getMedals().put(this.getCategory().getCode(), catMedals);
		}
		setDisplay();
		this.getElement().setProperty("showLiftRanks", Competition.getCurrent().isSnatchCJTotalMedals());
		this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop2.getName()));
		computeMedalsJson(this.getMedals());
	}

	private void doMedalsDisplay() {
		medalsInit();
		computeStylesDir(this);
		this.teamFlags = URLUtils.checkFlags();
		doMedals(this.getFop());

		if (!Competition.getCurrent().isSnatchCJTotalMedals()) {
			getElement().setProperty("noLiftRanks", "noranks");
		}
		this.getElement().setProperty("displayTitle", Translator.translate("CeremonyType.MEDALS"));
	}

	private void doRefresh(UIEvent e) {
		FieldOfPlay fop2 = e.getFop();
		doMedals(fop2);
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
		this.sattempts = Json.createArray();
		this.cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;

			jri.put("liftStatus", "empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
					case ACTUAL:
						if (!trim.isEmpty()) {
							if (trim.contentEquals("-") || trim.contentEquals("0")) {
								jri.put("liftStatus", "fail");
								jri.put("stringValue", "-");
							} else {
								boolean failed = stringValue != null && stringValue.startsWith("-");
								jri.put("liftStatus", failed ? "fail" : "good");
								jri.put("stringValue", formatKg(stringValue));
							}
						}
						break;
					default:
						if (stringValue != null && !trim.isEmpty()) {
							// logger.debug("{} {} {}", fop.getState(), x.getShortName(), curLift);

							String highlight = "";
							jri.put("liftStatus", "request");
							if (notDone) {
								jri.put("className", highlight);
							}
							jri.put("stringValue", stringValue);
						}
						break;
				}
			}

			if (ix < 3) {
				this.sattempts.set(ix, jri);
			} else {
				this.cattempts.set(ix % 3, jri);
			}
			ix++;
		}
	}

	private TreeMap<String, List<Athlete>> getMedals() {
		return this.medals;
	}

	private UI getUi() {
		return this.ui;
	}

	private boolean isMedalist(Athlete a) {
		if (a.getGroup() == null) {
			return false;
		}
		if (this.snatchCJTotalMedals) {
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

	private void medalsInit() {
		OwlcmsSession.withFop(fop -> {
			this.logger.trace("{}Starting result board on FOP {}", FieldOfPlay.getLoggingName(fop));
			setId("medals-" + fop.getName());
			setWideTeamNames(false);
			this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
			// CODEREVIEW: confusing
			// this.setGroup(fop.getVideoGroup());
			// this.setCategory(fop.getVideoCategory());
			this.setGroup(fop.getGroup());
			this.setCategory(null);
		});
		setTranslationMap();
	}

	private void setDisplay() {
		OwlcmsSession.withFop(fop -> {
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());
			this.getElement().setProperty("groupDescription", "");
		});
	}

	private void setMedals(TreeMap<String, List<Athlete>> medals) {
		this.medals = medals;
	}

	private void setUi(UI ui) {
		this.ui = ui;
	}

	private void syncWithFOP(UIEvent.SwitchGroup e) {
		switch (e.getState()) {
			case INACTIVE:
				this.setGroup(null);
				this.setCategory(null);
				doEmpty();
				break;
			// case BREAK:
			default:
				setCeremony(e.getFop().getCeremonyType() == CeremonyType.MEDALS);
				if (!this.isCeremony()) {
					this.setGroup(e.getGroup());
					this.setCategory(null);
					if (e.getGroup() == null) {
						doEmpty();
					} else {
						doUpdate(e);
						doBreak(e);
					}
				}
				break;
			// default:
			// setDisplay();
			// doUpdate(e);
		}
	}

}