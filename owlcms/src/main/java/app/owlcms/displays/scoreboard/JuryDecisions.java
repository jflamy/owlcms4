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
	
	/** If true, keep showing initial vote during deliberation */
	private boolean keepInitialDecision = true;
	/** If true, keep showing final vote until next athlete */
	private boolean keepFinalDecision = true;

	public JuryDecisions(AbstractDisplayPage page) {
		uiEventLogger.setLevel(Level.INFO);
		OwlcmsFactory.waitDBInitialized();
		setDarkMode(true);
		getElement().setProperty("autoversion", StartupUtils.getAutoVersion());
	}

	@Override
	public void doBreak(UIEvent e) {
		ui.access(() -> {
			uiEventLogger.debug("$$$ currentAthlete calling doBreak()");
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
	}

	@Override
	public void doCeremony(UIEvent.CeremonyStarted e) {
		ui.access(() -> {
			setDisplay();
			setShowJuryDecisions(getElement(), false);
		});
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
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
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
		uiLog(e);
		// If keepFinalDecision is true, don't clear until clock actually starts
		// Otherwise, clear on new clock (next athlete)
		if (!keepFinalDecision) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
				clear();
			});
		}
	}
	
	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		uiLog(e);
		logger.debug("slaveTimeStarted received, clearing jury decisions");
		// When clock actually starts running, clear the decisions
		// This is when keepFinalDecision=true should finally hide them
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			clear();
		});
	}
	
	public boolean isKeepInitialDecision() {
		return keepInitialDecision;
	}
	
	public void setKeepInitialDecision(boolean keepInitialDecision) {
		this.keepInitialDecision = keepInitialDecision;
	}
	
	public boolean isKeepFinalDecision() {
		return keepFinalDecision;
	}
	
	public void setKeepFinalDecision(boolean keepFinalDecision) {
		this.keepFinalDecision = keepFinalDecision;
	}

	@Override
	@Subscribe
	public void slaveStartBreak(UIEvent.BreakStarted e) {
		ui.access(() -> {
			// If keepInitialDecision is true and this is a JURY deliberation, don't hide the initial vote
			if (keepInitialDecision && e.getBreakType() == BreakType.JURY) {
				// Keep decisions visible - only set the mode, don't hide decisions
				getElement().setProperty("mode", "INTERRUPTION");
			} else {
				setDisplay();
				resetJuryVoting();
				doBreak(e);
			}
		});
	}

	@Override
	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		uiLog(e);
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			setDisplay();
			// If keepFinalDecision is true, don't hide the final vote when lifting resumes
			if (!keepFinalDecision) {
				setShowJuryDecisions(getElement(), false);
			}
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
		FieldOfPlay fop = getFop();
		logger.trace("{}Starting result board", FieldOfPlay.getLoggingName(fop));
		setId("scoreboard-" + fop.getName());
		setWideTeamNames(false);
		getElement().setProperty("competitionName", Competition.getCurrent().getCompetitionName());
		setTranslationMap();
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		FieldOfPlay fop = getFop();
		setId("jurydecisions-" + fop.getName());
		init();
		ui = UI.getCurrent();
		computeStylesDir(this);

		// we listen on uiEventBus.
		this.uiEventBus = uiEventBusRegister(this, fop);
		logger.warn("JuryDecisions registered on uiEventBus for fop {}", fop.getName());
		getElement().setProperty("platformName", CSSUtils.sanitizeCSSClassName(fop.getName()));
		getElement().setProperty("showJuryDecisions", true);
		getElement().setPropertyJson("decisions", Json.createArray());
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
		logger.debug("clear() called");
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
		FieldOfPlay fop = getFop();
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
	}

	private void setShowJuryDecisions(Element element, boolean b) {
		getElement().setProperty("showJuryDecisions", b);
	}

}
