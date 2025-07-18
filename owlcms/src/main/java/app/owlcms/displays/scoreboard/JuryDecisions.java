/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.displays.scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.dom.Element;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.uievents.UIEvent.GroupDone;
import app.owlcms.utils.CSSUtils;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * NCurrentAthlete: feeds the NCurrentAthlete.js web component.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Tag("jurydecisions-template")
@JsModule("./components/JuryDecisions.js")
public class JuryDecisions extends BaseResults {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryDecisions.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private UI ui;
	private EventBus uiEventBus;

	public JuryDecisions(AbstractDisplayPage page) {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	public void doBreak(UIEvent e) {
		OwlcmsSession.withFop(fop -> ui.access(() -> {
			uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		}));
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		OwlcmsSession.withFop(fop -> ui.access(() -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		}));
	}

	@Override
	public void reset() {
	}

	@Override
	@Subscribe
	public void slaveCeremonyDone(UIEvent.CeremonyDone e) {
		uiLog(e);
		ui.access(() -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Override
	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {
		ui.access(() -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Override
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Override
	public void slaveGroupDone(GroupDone e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Subscribe
	public void slaveJuryMemberDecision(UIEvent.JuryUpdate e) {
		ui.access(() -> {
			checkAllVoted();
			getElement().setProperty("showJuryDecisions", true);
		});

	}

	@Override
	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
	}

	@Subscribe
	public void slaveResetOnNewClock(UIEvent.ResetOnNewClock e) {
		ui.access(() -> clear());
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		ui.access(() -> {
			setDisplay();
			resetJuryVoting();
			doBreak(e);
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Override
	@Subscribe
	public void slaveStopBreak(UIEvent.BreakDone e) {
		ui.access(() -> {
			setDisplay();
			checkAllVoted();
		});
	}

	@Override
	protected void doEmpty() {
	}

	@Override
	protected void doUpdate(Athlete a, UIEvent e) {
	}

	@Override
	protected void getAthleteJson(Athlete a, JsonObject ja, Category curCat, int liftOrderRank, FieldOfPlay fop) {
	}

	protected void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("{}Starting result board", FieldOfPlay.getLoggingName(fop));
			setId("scoreboard-" + fop.getName());
			setWideTeamNames(false);
			getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		});
		setTranslationMap();
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsSession.withFop(fop -> {
			setId("jurydecisions-" + fop.getName());
			init();
			ui = UI.getCurrent();
			computeStylesDir(this);

			// we listen on uiEventBus.
			this.uiEventBus = uiEventBusRegister(this, fop);
			getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
			getElement().setProperty("showJuryDecisions", true);
			getElement().setPropertyJson("decisions", Json.createArray());
		});
	}

	@Override
	protected void updateDisplay(String liftType, FieldOfPlay fop) {
	}

	private void checkAllVoted() {
		boolean allVoted = true;
		for (int i = 0; i < getNbJurors(); i++) {
			Boolean juryVote = getFop().getJuryMemberDecision()[i];
			if (juryVote == null) {
				allVoted = false;
				break;
			}
		}

		JsonArray decisions = Json.createArray();
		if (allVoted) {
			for (int i = 0; i < getNbJurors(); i++) {
				Boolean juryVote = getFop().getJuryMemberDecision()[i];
				decisions.set(i, juryVote ? "white" : "red");
			}
		} else {
			for (int i = 0; i < getNbJurors(); i++) {
				decisions.set(i, "waiting");
			}
		}
		getElement().setPropertyJson("decisions", decisions);
		getElement().setProperty("showJuryDecisions", true);
	}

	private void clear() {
		JsonArray decisions = Json.createArray();
		for (int i = 0; i < getNbJurors(); i++) {
			decisions.set(i, "empty");
		}
		getElement().setPropertyJson("decisions", decisions);
	}

	private int getNbJurors() {
		return Competition.getCurrent().getJurySize();
	}

	private void resetJuryVoting() {
		getElement().setProperty("showJuryDecisions", false);
	}

	private void setDisplay() {
		OwlcmsSession.withFop(fop -> {
			FOPState fopState = fop.getState();
			BreakType breakType = fop.getBreakType();
			Element element = getElement();
			BoardMode bm = computeBoardMode(fopState, breakType, fop.getCeremonyType());
			if (logger.isDebugEnabled())
				logger.debug("********* setting board mode {} {}", bm.name(), LoggerUtils.whereFrom());
			switch (bm) {
				case WAIT:
					element.setProperty("mode", "WAIT");
					setShowJuryDecisions(element, false);
					break;
				case INTRO_COUNTDOWN:
					element.setProperty("mode", "INTRO_COUNTDOWN");
					setShowJuryDecisions(element, false);
					break;
				case CEREMONY:
					element.setProperty("mode", "CEREMONY");
					setShowJuryDecisions(element, false);
					break;
				case LIFT_COUNTDOWN:
					element.setProperty("mode", "LIFT_COUNTDOWN");
					setShowJuryDecisions(element, false);
					break;
				case CURRENT_ATHLETE:
					element.setProperty("mode", "CURRENT_ATHLETE");
					break;
				case INTERRUPTION:
					element.setProperty("mode", "INTERRUPTION");
					setShowJuryDecisions(element, false);
					break;
				case SESSION_DONE:
					element.setProperty("mode", "SESSION_DONE");
					setShowJuryDecisions(element, false);
					break;
				case LIFT_COUNTDOWN_CEREMONY:
					element.setProperty("mode", "LIFT_COUNTDOWN_CEREMONY");
					setShowJuryDecisions(element, false);
					break;
			}
		});
	}

	private void setShowJuryDecisions(Element element, boolean b) {
		getElement().setProperty("showJuryDecisions", b);
	}

}
