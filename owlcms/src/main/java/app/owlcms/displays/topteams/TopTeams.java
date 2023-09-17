/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-Fran�ois Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.topteams;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.athleteSort.Ranking;
import app.owlcms.data.category.AgeDivision;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.team.Team;
import app.owlcms.data.team.TeamResultsTreeData;
import app.owlcms.data.team.TeamTreeItem;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Class TopTeams
 *
 * Show Best teams point scores
 *
 * @author Jean-François Lamy
 *
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("topteams-template")
@JsModule("./components/TopTeams.js")

public class TopTeams extends Results {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TopTeams.class);
	private static final int SHOWN_ON_BOARD = 5;
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	private AgeDivision ageDivision = null;
	private String ageGroupPrefix = null;
	private DecimalFormat floatFormat;
	private List<TeamTreeItem> mensTeams;
	private EventBus uiEventBus;
	private List<TeamTreeItem> womensTeams;
	private String routeParameter;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public TopTeams() {
		super();
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			// just update the display
			setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());
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

		TeamResultsTreeData teamResultsTreeData = new TeamResultsTreeData(getAgeGroupPrefix(), getAgeDivision(),
		        (Gender) null,
		        Ranking.SNATCH_CJ_TOTAL, false);
		Map<Gender, List<TeamTreeItem>> teamsByGender = teamResultsTreeData.getTeamItemsByGender();

		mensTeams = teamsByGender.get(Gender.M);
		if (mensTeams != null) {
			mensTeams.sort(TeamTreeItem.pointComparator);
		}
		mensTeams = topN(mensTeams);

		womensTeams = teamsByGender.get(Gender.F);
		if (womensTeams != null) {
			womensTeams.sort(TeamTreeItem.pointComparator);
		}
		womensTeams = topN(womensTeams);

		updateBottom();
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

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();

		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doUpdate(competition);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		Competition competition = Competition.getCurrent();
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
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
		FieldOfPlay fop = OwlcmsSession.getFop();
		setBoardMode(fop.getState(), fop.getBreakType(), fop.getCeremonyType(), getElement());
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
		logger.debug("doUpdate {} {}", a, a != null ? a.getAttemptsDone() : null);
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			if (a != null) {
				updateBottom();
			}
		});
	}

	/*
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		checkVideo(Config.getCurrent().getParamStylesDir() + "/video/top.css", routeParameter, this);
		setWide(false);
		setTranslationMap();
		for (FieldOfPlay fop : OwlcmsFactory.getFOPs()) {
			// we listen on all the uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		}
		Competition competition = Competition.getCurrent();
		doUpdate(competition);

		buildDialog(this);
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

	private String computeAgeGroupSuffix() {
		String suffix = null;
		if (getAgeGroupPrefix() != null) {
			suffix = getAgeGroupPrefix();
		}
		return (suffix != null ? " &ndash; " + suffix : "");
	}

	private String formatDouble(double d) {
		if (floatFormat == null) {
			floatFormat = new DecimalFormat();
			floatFormat.setMinimumIntegerDigits(1);
			floatFormat.setMaximumFractionDigits(0);
			floatFormat.setGroupingUsed(false);
		}
		return floatFormat.format(d);
	}

	private AgeDivision getAgeDivision() {
		return ageDivision;
	}

	private String getAgeGroupPrefix() {
		return ageGroupPrefix;
	}

	@SuppressWarnings("unused")
	private Object getOrigin() {
		return this;
	}

	private void getTeamJson(Team t, JsonObject ja) {
		ja.put("team", t.getName());
		ja.put("counted", formatInt(t.getCounted()));
		ja.put("size", formatInt((int) t.getSize()));
		ja.put("score", formatDouble(t.getSinclairScore()));
		ja.put("points", formatInt(t.getPoints()));
	}

	private JsonValue getTeamsJson(List<TeamTreeItem> teamItems, boolean overrideTeamWidth) {
		JsonArray jath = Json.createArray();
		int athx = 0;
		List<Team> list3 = teamItems != null
		        ? teamItems.stream().map(TeamTreeItem::getTeam).collect(Collectors.toList())
		        : Collections.emptyList();
		if (overrideTeamWidth) {
			// when we are called for the second time, and there was a wide team in the top
			// section.
			// we use the wide team setting for the remaining sections.
			setWide(false);
		}

		for (Team t : list3) {
			JsonObject ja = Json.createObject();
			// Gender curGender = t.getGender();

			getTeamJson(t, ja);
			String teamName = t.getName();
			if (teamName != null && teamName.length() > Competition.SHORT_TEAM_LENGTH) {
				setWide(true);
			}
			jath.set(athx, ja);
			athx++;
		}
		return jath;
	}

	private void setWide(boolean b) {
		this.getElement().setProperty("wideTeamNames", b);
	}

	private List<TeamTreeItem> topN(List<TeamTreeItem> list) {
		if (list == null) {
			return new ArrayList<>();
		}
		int size = list.size();
		if (size > 0) {
			int min = Math.min(size, SHOWN_ON_BOARD);
			list = list.subList(0, min);
		}
		return list;
	}

	private void updateBottom() {
		this.getElement().setProperty("topTeamsMen",
		        mensTeams != null && mensTeams.size() > 0
		                ? getTranslation("Scoreboard.TopTeamsMen") + computeAgeGroupSuffix()
		                : "");
		this.getElement().setPropertyJson("mensTeams", getTeamsJson(mensTeams, true));

		this.getElement().setProperty("topTeamsWomen",
		        womensTeams != null && womensTeams.size() > 0
		                ? getTranslation("Scoreboard.TopTeamsWomen") + computeAgeGroupSuffix()
		                : "");
		this.getElement().setPropertyJson("womensTeams", getTeamsJson(womensTeams, false));
	}
}
