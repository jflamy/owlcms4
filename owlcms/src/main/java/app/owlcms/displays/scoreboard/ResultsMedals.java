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

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BaseJsonNode;
import tools.jackson.databind.node.ObjectNode;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.ResultsParameters;
import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.Championship;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.MedalCategoryComparator;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.category.Participation;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.config.FeatureSwitch;
import app.owlcms.data.group.Group;
import app.owlcms.data.team.Team;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.JsonUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

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
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + this.logger.getName());
	private Category category;
	private ArrayNode cattempts;
	private TreeMap<String, List<Athlete>> medals;
	private ArrayNode sattempts;
	private EventBus uiEventBus;
	private boolean snatchCJTotalMedals;
	private AgeGroup ageGroup;
	private boolean teamFlags;
	private Championship ageDivision;
	private String ageGroupPrefix;
	private UI ui;
	private boolean ceremony;
	private boolean categoryPinnedFromURL;

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
		doBreak(getFop());
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		this.setCeremony(true);
		Group ceremonyGroup = e.getCeremonySession();
		setGroup(ceremonyGroup);
		Category ceremonyCategory = e.getCeremonyCategory();
		setCategory(ceremonyCategory);
		// logger.debug("ceremony event = {} {} {} {}", e, ceremonyGroup, ceremonyCategory, LoggerUtils.stackTrace());

		// medalsInit();
		computeStylesDir(this);
		this.teamFlags = URLUtils.checkFlags();
		doMedals(this.getFop());
		this.getElement().setProperty("displayTitle", Translator.translate("CeremonyType.MEDALS"));
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
		if (category == null && this.categoryPinnedFromURL && this.category != null) {
			return;
		}
		this.category = category;
	}

	public boolean isCategoryPinnedFromURL() {
		return this.categoryPinnedFromURL;
	}

	public void setCategoryPinnedFromURL(boolean categoryPinnedFromURL) {
		this.categoryPinnedFromURL = categoryPinnedFromURL;
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

	public void setTitles(ObjectNode jMC, Category cat) {
		jMC.put("categoryName", cat.getDisplayName());
		AgeGroup ageGroup2 = cat.getAgeGroup();
		if (ageGroup2 == null) {
			logger.error("category without ageGroup: {}", cat);
		}
		Ranking scoringSystem = ageGroup2 != null ? ageGroup2.getComputedScoringSystem() : null;
		String rankingTitle = Translator.translate("Scoreboard.Rank");
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
		this.getUi().access(() -> {
			// logger.trace("------- slaveBreakDone {}", e.getBreakType());
			setDisplay();
			doUpdate(e);
		});
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		this.setCeremony(false);
		this.getUi().access(() -> {
			if (e.getCeremonyType() == CeremonyType.MEDALS) {
				// end of medals break.
				syncWithFOP(getFop());
			}
		});
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
		if (!isCategoryPinnedFromURL()) {
			this.setCategory(null);
		}
		if (!isGroupPinnedFromURL()) {
			this.setGroup(e.getGroup());
		}
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
				if (!isGroupPinnedFromURL()) {
					this.setGroup(null);
				}
				if (!isCategoryPinnedFromURL()) {
					this.setCategory(null);
				}
				doEmpty();
				break;
			// case BREAK:
			default:
				setCeremony(fop.getCeremonyType() == CeremonyType.MEDALS);
				if (!this.isCeremony()) {
					if (!isGroupPinnedFromURL()) {
						this.setGroup(fop.getGroup());
					}
					if (!isCategoryPinnedFromURL()) {
						this.setCategory(null);
					}
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

	protected void getAthleteJson(Athlete a, ObjectNode ja, Category curCat, int liftOrderRank) {
		if (a.getGroup() == null) {
			return;
		}
		String category;
		category = curCat != null ? curCat.getDisplayName() : "";
		ja.put("fullName", getScoreboardDisplayName(a));
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a);
		ja.set("sattempts", this.sattempts);
		ja.put("bestSnatch", formatInt(a.getBestSnatch()));
		ja.set("cattempts", this.cattempts);
		ja.put("bestCleanJerk", formatInt(a.getBestCleanJerk()));
		ja.put("total", formatInt(a.getTotal()));

		Participation mainRankings = a.getMainRankings();
		if (mainRankings != null) {
			boolean liftMedals = awardsLiftMedals(a);
			int snatchRank = mainRankings.getSnatchRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("snatchRank", formatRank(snatchRank));
				ja.put("snatchMedal", liftMedals && snatchRank >= 1 && snatchRank <= 3 ? "medal" + snatchRank : "");
			} else {
				ja.put("snatchRank", "");
				ja.put("snatchMedal", "");
			}

			int cleanJerkRank = mainRankings.getCleanJerkRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("cleanJerkRank", formatRank(cleanJerkRank));
				ja.put("cleanJerkMedal", liftMedals && cleanJerkRank >= 1 && cleanJerkRank <= 3 ? "medal" + cleanJerkRank : "");
			} else {
				ja.put("cleanJerkRank", "");
				ja.put("cleanJerkMedal", "");
			}

			int totalRank = mainRankings.getTotalRank();
			if (a.getComputedScoringSystem() == Ranking.TOTAL) {
				ja.put("totalRank", formatRank(totalRank));
				ja.put("totalMedal", totalRank >= 1 && totalRank <= 3 ? "medal" + totalRank : "");
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
			if (mainRankings != null) {
				int computedScoreRank = mainRankings.getCategoryScoreRank();
				ja.put("sinclairRank", computedScoreRank);
				ja.put("sinclairMedal", computedScoreRank <= 3 ? "medal" + computedScoreRank : "");
			}
		}

		ja.put("custom1", getCustom1Value(a));
		ja.put("custom2", a.getCustom2() != null ? a.getCustom2() : "");

		String prop = null;
		if (!Config.getCurrent().featureSwitch(FeatureSwitch.MEDALS_FOR_CATEGORY_ONLY)) {
			// only show flags when medals are for a single category
			String team = a.getTeam();
			if (this.teamFlags && !team.isBlank()) {
				prop = Team.getImgTag(team, "");
			}
			ja.put("flagURL", prop != null ? prop : "");
			boolean longTeamWithFlag = prop != null && team.length() > Competition.SHORT_TEAM_LENGTH;
			ja.put("flagClass", longTeamWithFlag ? "flags longTeam" : "flags");
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
	protected BaseJsonNode getAthletesJson(List<Athlete> displayOrder, final FieldOfPlay _unused) {
		ArrayNode jath = JsonUtils.array();
		AtomicInteger athx = new AtomicInteger(0);
		// Category prevCat = null;
		List<Athlete> athletes = displayOrder != null ? Collections.unmodifiableList(displayOrder)
		        : Collections.emptyList();

		athletes.stream()
		        .filter(a -> isMedalist(a))
		        .forEach(a -> {
					ObjectNode ja = JsonUtils.object();
			        Category curCat = a.getCategory();
			        // no blinking = 0
			        getAthleteJson(a, ja, curCat, 0);
			        String team = a.getTeam();
			        if (team != null && team.trim().length() > Competition.SHORT_TEAM_LENGTH) {
				        this.logger.trace("long team {}", team);
				        setWideTeamNames(true);
			        }
					JsonUtils.set(jath, athx.getAndIncrement(), ja);
		        });

		return jath;
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		this.setUi(attachEvent.getUI());
		this.getElement().setProperty("showCustom1", Config.getCurrent().featureSwitch(FeatureSwitch.DISPLAY_BODY_WEIGHT));
		// we listen on uiEventBus.
		FieldOfPlay fop = getFop();
		this.uiEventBus = uiEventBusRegister(this, fop);
		doMedalsDisplay();
	}

	@Override
	protected void setTranslationMap() {
		ObjectNode translations = JsonUtils.object();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		if (Config.getCurrent().featureSwitch(FeatureSwitch.DISPLAY_BODY_WEIGHT)) {
			translations.put("Custom1", getCustom1Label());
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
		FieldOfPlay fop = getFop();
		String catCode = getCategory().getCode();
		List<Athlete> medalists = medals2.get(catCode);
		boolean scoreNeeded = (medalists != null && !medalists.isEmpty()) &&
		        (medalists.get(0).getComputedScoringSystem() != Ranking.TOTAL);
		setScoreRanks(scoreNeeded);

		ArrayNode jsonMCArray = JsonUtils.array();
		ObjectNode jMC = JsonUtils.object();
		int mcX = 0;
		if (medalists != null && !medalists.isEmpty()) {
			BaseJsonNode leaders = getAthletesJson(new ArrayList<>(medalists), fop);
			// isMedalist filtering can leave no rows; skip the category entirely
			if (leaders.size() > 0) {
				jMC.put("categoryName", getCategory().getDisplayName());
				setTitles(jMC, medalists.get(0).getCategory());
				jMC.set("leaders", leaders);

				// Check if all eligible athletes in this category have finished lifting
				Group g = this.getGroup();
				boolean allDone = medalists.stream()
				        .noneMatch(a -> !a.isDone(g) && a.isEligibleForIndividualRanking());
				jMC.put("categoryDone", allDone);

				JsonUtils.set(jsonMCArray, mcX, jMC);
				mcX++;
			}
		}

		this.getElement().setPropertyJson("medalCategories", jsonMCArray);
		if (mcX == 0) {
			this.getElement().setProperty("noCategories", true);
		}
	}

	private void computeGroupMedalsJson(TreeMap<String, List<Athlete>> medals2) {
		// logger.debug("computeGroupMedalsJson group = {}\n{}", this.getGroup(), LoggerUtils.stackTrace());
		ArrayNode jsonMCArray = JsonUtils.array();
		int mcX = 0;

		boolean scoreNeeded = false;
		List<Entry<String, List<Athlete>>> orderedMedalCategories = new ArrayList<>(medals2.entrySet());
		MedalCategoryComparator categoryComparator = new MedalCategoryComparator(this.getGroup());
		orderedMedalCategories.sort((a, b) -> categoryComparator.compare(
		        medalCategoryAthlete(a), medalCategoryAthlete(b)));
		for (Entry<String, List<Athlete>> medalCat : orderedMedalCategories) {
			List<Athlete> athletes = medalCat.getValue();
			if (athletes != null && !athletes.isEmpty()) {
				if (athletes.get(0).getComputedScoringSystem() != Ranking.TOTAL) {
					scoreNeeded = true;
					break;
				}
			}
		}

		for (Entry<String, List<Athlete>> medalCat : medals2.entrySet()) {
			ObjectNode jMC = JsonUtils.object();
			List<Athlete> medalists = medalCat.getValue();
			if (medalists != null && !medalists.isEmpty()) {
				if (!isMedalingCategoryForSession(medalists, this.getGroup())) {
					continue;
				}
				BaseJsonNode leaders = getAthletesJson(new ArrayList<>(medalists), null);
				// isMedalist filtering can leave no rows; skip the category entirely
				if (leaders.size() == 0) {
					continue;
				}
				setTitles(jMC, medalists.get(0).getCategory());

				jMC.set("leaders", leaders);
				if (mcX == 0) {
					jMC.put("showCatHeader", "");
				} else {
					jMC.put("showCatHeader", "display:none;");
				}

				// Check if all eligible athletes in this category have finished lifting
				Group g = this.getGroup();
				boolean allDone = medalists.stream()
				        .noneMatch(a -> !a.isDone(g) && a.isEligibleForIndividualRanking());
				jMC.put("categoryDone", allDone);

				setScoreRanks(scoreNeeded);
				JsonUtils.set(jsonMCArray, mcX, jMC);
				mcX++;
			}
		}
		this.getElement().setPropertyJson("medalCategories", jsonMCArray);
		if (mcX == 0) {
			this.getElement().setProperty("noCategories", true);
		}
	}

	private Athlete medalCategoryAthlete(Entry<String, List<Athlete>> medalCategory) {
		List<Athlete> medalists = medalCategory.getValue();
		return medalists != null && !medalists.isEmpty() ? medalists.get(0) : null;
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

	protected boolean isOnlyFinished() {
		// we want live medals
		return false;
	}

	/**
	 * A category awards medals at the end of session g only if the session lifts its A (or unsplit)
	 * portion; the medalists shown may still come from B/C/D groups in earlier sessions.
	 */
	private boolean isMedalingCategoryForSession(List<Athlete> medalists, Group g) {
		if (g == null) {
			return true;
		}
		List<Athlete> inSession = medalists.stream()
		        .filter(a -> g.equals(a.getGroup()))
		        .toList();
		if (inSession.isEmpty()) {
			// category not contested in this session
			return false;
		}
		return inSession.stream().allMatch(a -> {
			String sub = a.getSubCategory();
			return sub == null || sub.isBlank() || sub.equalsIgnoreCase("A");
		});
	}

	private void doMedals(FieldOfPlay fop2) {
		this.snatchCJTotalMedals = resolveLiftRankVisibility(fop2);
		if (this.getCategory() == null) {
			if (this.getGroup() != null) {
				// logger.debug("getgroup {}", this.getGroup());
				this.setMedals(Competition.getCurrent().getMedals(fop2, this.getGroup(), isOnlyFinished()));
			} else {
				// logger.debug("getgroup from FOP {}", fop2.getGroup());
				this.setMedals(Competition.getCurrent().getMedals(fop2, fop2.getGroup(), isOnlyFinished()));
			}
			// this.getElement().setProperty("fillerDisplay", "");
		} else {
			List<Athlete> catMedals = Competition.getCurrent().computeMedalsForCategory(fop2, this.getCategory());
			// logger.debug("group {} category {} catMedals {}", getGroup(), getCategory(), catMedals.stream().map(a -> a.getAbbreviatedName()).toList());
			this.setMedals(new TreeMap<>());
			this.getMedals().put(this.getCategory().getCode(), catMedals);
		}
		setDisplay();
		this.getElement().setProperty("showLiftRanks", this.snatchCJTotalMedals);
		this.getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop2.getName()));
		computeMedalsJson(this.getMedals());
	}

	private void doMedalsDisplay() {
		medalsInit();
		computeStylesDir(this);
		this.teamFlags = URLUtils.checkFlags();
		doMedals(this.getFop());
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
		this.sattempts = JsonUtils.array();
		this.cattempts = JsonUtils.array();
		XAthlete x = new XAthlete(a);
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			ObjectNode jri = JsonUtils.object();
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
				JsonUtils.set(this.sattempts, ix, jri);
			} else {
				JsonUtils.set(this.cattempts, ix % 3, jri);
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
		if (awardsLiftMedals(a)) {
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

	private boolean resolveLiftRankVisibility(FieldOfPlay fop) {
		if (getCategory() != null && getCategory().getAgeGroup() != null) {
			return getCategory().getAgeGroup().getChampionship().isSnatchCJTotalMedals();
		}
		if (fop != null) {
			return Championship.anyMultiMedal(fop.getScoreboardChampionships());
		}
		return Championship.of(null).isSnatchCJTotalMedals();
	}

	private void medalsInit() {
		FieldOfPlay fop = getFop();
		this.logger.trace("{}Starting result board on FOP {}", FieldOfPlay.getLoggingName(fop));
		setId("medals-" + fop.getName());
		setWideTeamNames(false);
		this.getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		
		// Don't override group/category that may have been set from URL parameters
		Group existingGroup = this.getGroup();
		if (existingGroup == null) {
			if (isVideo()) {
				if (!isGroupPinnedFromURL()) {
					this.setGroup(fop.getVideoGroup());
				}
				if (!isCategoryPinnedFromURL()) {
					this.setCategory(fop.getVideoCategory());
				}
			} else {
				if (!isGroupPinnedFromURL()) {
					this.setGroup(fop.getGroup());
				}
				if (!isCategoryPinnedFromURL()) {
					this.setCategory(null);
				}
			}
		}
		setTranslationMap();
	}

	private void setDisplay() {
		FieldOfPlay fop = getFop();
		if (fop == null) {
			return;
		}
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), this.getElement());
		Group group = getGroup();
		String description = null;
		if (group != null) {
			description = group.getDescription();
			if (description == null) {
				description = Translator.translate("Group_number", group.getName());
			}
		}
		this.getElement().setProperty("groupDescription", description != null ? description : "");
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
				if (!isGroupPinnedFromURL()) {
					this.setGroup(null);
				}
				if (!isCategoryPinnedFromURL()) {
					this.setCategory(null);
				}
				doEmpty();
				break;
			// case BREAK:
			default:
				setCeremony(e.getFop().getCeremonyType() == CeremonyType.MEDALS);
				if (!this.isCeremony()) {
					if (!isGroupPinnedFromURL()) {
						this.setGroup(e.getGroup());
					}
					if (!isCategoryPinnedFromURL()) {
						this.setCategory(null);
					}
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