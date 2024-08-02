/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.top;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athlete.LiftDefinition.Changes;
import app.owlcms.data.athlete.LiftInfo;
import app.owlcms.data.athlete.XAthlete;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.scoring.GAMX;
import app.owlcms.data.scoring.QPoints;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.spreadsheet.PAthlete;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class TopSinclair
 *
 * Show athlete lifting order
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("topsinclair-template")
@JsModule("./components/TopSinclair.js")

public class TopSinclair extends AbstractTop {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TopSinclair.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	JsonArray cattempts;
	JsonArray sattempts;
	private List<Athlete> sortedMen;
	private List<Athlete> sortedWomen;
	private double topManScore;
	private double topWomanScore;
	private EventBus uiEventBus;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Ranking scoringSystem;
	private QPoints qpoints = new QPoints(2023);

	public TopSinclair() {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			// just update the display
			doUpdate(fop.getCurAthlete(), null);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		doBreak(e);
	}

	public void doUpdate(Competition competition) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());

		// create copies because we want to change the list
		AthleteSorter.TopScore topScores;
		List<Athlete> sortedMen2 = new ArrayList<>(competition.getGlobalScoreRanking(Gender.M));
		topScores = (AthleteSorter.topScore(sortedMen2, 10));
		setSortedMen(topScores.topAthletes);
		this.topManScore = topScores.best;

		List<Athlete> sortedWomen2 = new ArrayList<>(competition.getGlobalScoreRanking(Gender.F));
		topScores = (AthleteSorter.topScore(sortedWomen2, 10));
		setSortedWomen(topScores.topAthletes);
		this.topWomanScore = topScores.best;

		updateBottom();
	}

	public void getAthleteJson(Athlete a, JsonObject ja, Gender g, int needed) {
		String category;
		category = a.getCategory() != null ? a.getCategory().getDisplayName() : "";
		ja.put("fullName", a.getFullName() != null ? a.getFullName() : "");
		ja.put("teamName", a.getTeam() != null ? a.getTeam() : "");
		ja.put("yearOfBirth", a.getYearOfBirth() != null ? a.getYearOfBirth().toString() : "");
		Integer startNumber = a.getStartNumber();
		ja.put("startNumber", (startNumber != null ? startNumber.toString() : ""));
		ja.put("category", category != null ? category : "");
		getAttemptsJson(a);
		ja.put("sattempts", this.sattempts);
		ja.put("cattempts", this.cattempts);
		ja.put("total", formatInt(a.getTotal()));
		ja.put("bw", String.format("%.2f", a.getBodyWeight()));
		ja.put("sinclair", String.format("%.3f", Ranking.getRankingValue(a, this.scoringSystem)));
		ja.put("needed", formatInt(needed));
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.DisplayParameters#setSilenced(boolean)
	 */
	@Override
	public void setSilenced(boolean silent) {
		// no-op, silenced by definition
	}

	@Override
	public void setVideo(boolean video) {
	}

	@Subscribe
	public void slaveGlobalRankingUpdated(UIEvent.GlobalRankingUpdated e) {
		computeTop(e);
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			doUpdate(competition);
		});
	}

	@Override
	public void uiLog(UIEvent e) {
		if (e == null) {
			uiEventLogger.debug("### {} {}", this.getClass().getSimpleName(), LoggerUtils.whereFrom());
		} else {
			uiEventLogger.debug("### {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			        LoggerUtils.whereFrom());
		}
	}

	@Override
	protected void doEmpty() {
		logger.trace("doEmpty");
		getElement().setProperty("hidden", true);
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			if (a != null) {
				getElement().setProperty("fullName", Translator.translate("Scoreboard.TopSinclair"));
				updateBottom();
			}
		});
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
		this.sattempts = Json.createArray();
		this.cattempts = Json.createArray();
		XAthlete x = new XAthlete(a);
		Integer liftOrderRank = x.getLiftOrderRank();
		Integer curLift = x.getAttemptsDone();
		int ix = 0;
		for (LiftInfo i : x.getRequestInfoArray()) {
			JsonObject jri = Json.createObject();
			String stringValue = i.getStringValue();
			boolean notDone = x.getAttemptsDone() < 6;
			String blink = (notDone ? " blink" : "");

			jri.put("liftStatus", "veryNarrow empty");
			jri.put("stringValue", "");
			if (i.getChangeNo() >= 0) {
				String trim = stringValue != null ? stringValue.trim() : "";
				switch (Changes.values()[i.getChangeNo()]) {
					case ACTUAL:
						if (!trim.isEmpty()) {
							if (trim.contentEquals("-") || trim.contentEquals("0")) {
								jri.put("liftStatus", "veryNarrow fail");
								jri.put("stringValue", "-");
							} else {
								boolean failed = stringValue != null && stringValue.startsWith("-");
								jri.put("liftStatus", failed ? "veryNarrow fail" : "veryNarrow good");
								jri.put("stringValue", formatKg(stringValue));
							}
						}
						break;
					default:
						if (stringValue != null && !trim.isEmpty()) {
							String highlight = i.getLiftNo() == curLift && liftOrderRank == 1 ? (" current" + blink)
							        : (i.getLiftNo() == curLift && liftOrderRank == 2) ? " next" : "";
							jri.put("liftStatus", "veryNarrow request");
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

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.debug("onAttach start");
		checkVideo(this);
		setWide(false);
		setTranslationMap(null, true);
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			// we listen on all the uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);
		}
		Competition competition = Competition.getCurrent();
		this.scoringSystem = competition.getScoringSystem();
		doUpdate(competition);
	}

	@Override
	protected void setTranslationMap(Ranking ignored, boolean globalRanking) {
		JsonObject translations = Json.createObject();
		Enumeration<String> keys = Translator.getKeys();
		while (keys.hasMoreElements()) {
			String curKey = keys.nextElement();
			if (curKey.startsWith("Scoreboard.")) {
				translations.put(curKey.replace("Scoreboard.", ""), Translator.translate(curKey));
			}
		}
		String scoringTitle = Ranking.getScoringTitle(Competition.getCurrent().getScoringSystem());
		translations.put("ScoringTitle", scoringTitle != null ? scoringTitle : Translator.translate("Sinclair"));
		this.getElement().setPropertyJson("t", translations);
	}

	private void computeTop(UIEvent e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, this.uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	private String formatKg(String total) {
		return (total == null || total.trim().isEmpty()) ? "-"
		        : (total.startsWith("-") ? "(" + total.substring(1) + ")" : total);
	}

	private JsonValue getAthletesJson(List<Athlete> list2, boolean overrideTeamWidth) {
		JsonArray jath = Json.createArray();
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		int athx = 0;
		List<Athlete> list3 = list2 != null ? Collections.unmodifiableList(list2) : Collections.emptyList();
		if (overrideTeamWidth) {
			// when we are called for the second time, and there was a wide team in the top
			// section.
			// we use the wide team setting for the remaining sections.
			setWide(false);
		}

		for (Athlete a : list3) {
			JsonObject ja = Json.createObject();
			Gender curGender = a.getGender();
			int needed = 0;

			switch (scoringSystem) {
				case BW_SINCLAIR:
					if (curGender == Gender.F) {
						needed = (int) Math.round(
						        Math.ceil((this.topWomanScore - a.getSinclairForDelta()) / a.getSinclairFactor()));
					} else {
						needed = (int) Math.round(
						        Math.ceil((this.topManScore - a.getSinclairForDelta()) / a.getSinclairFactor()));
					}
					break;
				case CAT_SINCLAIR:
					if (curGender == Gender.F) {
						needed = (int) Math.round(
						        Math.ceil((this.topWomanScore - a.getCategorySinclair()) / a.getCatSinclairFactor()));
					} else {
						needed = (int) Math.round(
						        Math.ceil((this.topManScore - a.getCategorySinclair()) / a.getCatSinclairFactor()));
					}
					break;
				case GAMX:
					if (curGender == Gender.F) {
						int tot = a.getBestSnatch() + a.getBestCleanJerk();
						needed = GAMX.kgTarget(curGender, this.topWomanScore, a.getBodyWeight()) - tot;
						if (needed < 0) {
							needed = 0;
						}
					} else {
						int tot = a.getBestSnatch() + a.getBestCleanJerk();
						needed = GAMX.kgTarget(curGender, this.topWomanScore, a.getBodyWeight()) - tot;
						if (needed < 0) {
							needed = 0;
						}
					}
					break;
				case QPOINTS:
					if (curGender == Gender.F) {
						needed = (int) Math.round(
						        Math.ceil((this.topWomanScore - a.getQPoints())
						                / this.qpoints.qPointsFactor(Gender.F, a.getBodyWeight())));
					} else {
						needed = (int) Math.round(
						        Math.ceil((this.topManScore - a.getQPoints())
						                / this.qpoints.qPointsFactor(Gender.M, a.getBodyWeight())));
					}
					break;
				case ROBI:
					double robiScore = 0.0D;
					if (curGender == Gender.F) {
						robiScore = this.topWomanScore;
					} else {
						robiScore = this.topManScore;
					}

					double A = 1000.0D / Math.pow(a.getRobiWr(), Category.ROBI_B);
					double b = Category.ROBI_B;

					int total = a.getBestCleanJerk() + a.getBestSnatch();
					needed = ((int) Math.pow((robiScore) / A, 1 / b)) - total;

					// if (a.getFirstName().startsWith("Kelin")) {
					// logger.trace("athlete ++++ {} robi {} bestRobi {} A {} total {}", a.getShortName(),
					// a.getRobi(), robiScore, A, needed);
					// }

					break;
				case SMM:
					if (curGender == Gender.F) {
						needed = (int) Math.round(
						        Math.ceil((this.topWomanScore - a.getSmhfForDelta()) / a.getSmhfFactor()));
					} else {
						needed = (int) Math.round(
						        Math.ceil((this.topManScore - a.getSmhfForDelta()) / a.getSmhfFactor()));
					}
					break;
				default:
					break;

			}

			getAthleteJson(a, ja, curGender, needed);
			String team = a.getTeam();
			if (team != null && team.length() > Competition.SHORT_TEAM_LENGTH) {
				setWide(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	@SuppressWarnings("unused")
	private Object getOrigin() {
		return this;
	}

	private List<Athlete> getSortedMen() {
		return this.sortedMen;
	}

	private List<Athlete> getSortedWomen() {
		return this.sortedWomen;
	}

	private List<Athlete> nodups(List<Athlete> athletes) {
		// massive kludge because we have same athlete in multiple age groups
		athletes = athletes.stream()
		        .map((p) -> p instanceof PAthlete ? ((PAthlete) p)._getAthlete() : p)
		        .collect(Collectors.toSet())
		        .stream()
		        .sorted((a, b) -> ObjectUtils.compare(Ranking.getRankingValue(b, this.scoringSystem),
		                Ranking.getRankingValue(a, this.scoringSystem)))
		        .collect(Collectors.toList());
		return athletes;
	}

	private void setSortedMen(List<Athlete> sortedMen) {
		this.sortedMen = sortedMen;
		// logger.debug("sortedMen = {} -- {}", getSortedMen().size(), LoggerUtils.whereFrom());
	}

	private void setSortedWomen(List<Athlete> sortedWomen) {
		this.sortedWomen = sortedWomen;
		// logger.debug("sortedWomen = {} -- {}", getSortedWomen().size(), LoggerUtils.whereFrom());
	}

	private void setWide(boolean b) {
		getElement().setProperty("wideTeamNames", b);
	}

	private void updateBottom() {
		Ranking scoringSystem = Competition.getCurrent().getScoringSystem();
		String ssTitle = Ranking.getScoringTitle(scoringSystem);
		getElement().setProperty("fullName", Translator.translate("Scoreboard.TopScore"));
		List<Athlete> sortedMen2 = getSortedMen();
		sortedMen2 = nodups(sortedMen2);
		this.getElement().setProperty("topSinclairMen",
		        sortedMen2 != null && sortedMen2.size() > 0 ? Translator.translate("Scoreboard.TopScoreMen", ssTitle)
		                : "");
		this.getElement().setPropertyJson("sortedMen", getAthletesJson(sortedMen2, true));

		List<Athlete> sortedWomen2 = getSortedWomen();
		sortedWomen2 = nodups(sortedWomen2);
		this.getElement().setProperty("topSinclairWomen",
		        sortedWomen2 != null && sortedWomen2.size() > 0
		                ? Translator.translate("Scoreboard.TopScoreWomen", ssTitle)
		                : "");
		this.getElement().setPropertyJson("sortedWomen", getAthletesJson(sortedWomen2, false));

		logger.debug("updateBottom {} {}", sortedWomen2, sortedMen2);
	}

}
