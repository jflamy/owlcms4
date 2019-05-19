/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.displays.attemptboard;

import java.text.MessageFormat;

import org.slf4j.LoggerFactory;

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
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.BreakStarted;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.lifting.BreakDialog.BreakType;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class AttemptBoard.
 */
@SuppressWarnings("serial")
@Tag("attempt-board-template")
@HtmlImport("frontend://components/AttemptBoard.html")
@Route("displays/attemptBoard")
@Theme(value = Material.class, variant = Material.DARK)
@Push
public class AttemptBoard extends PolymerTemplate<AttemptBoard.AttemptBoardModel>
		implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor, BreakDisplay, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AttemptBoard.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	/**
	 * AttemptBoardModel
	 * 
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript
	 * properties. When the JS properties are changed, a "propname-changed" event is triggered.
	 * 
	 * {@link Element.#addPropertyChangeListener(String, String,
	 * com.vaadin.flow.dom.PropertyChangeListener)}
	 */
	public interface AttemptBoardModel extends TemplateModel {
		String getAttempt();

		String getFirstName();

		String getLastName();

		Integer getStartNumber();

		String getTeamName();

		Integer getWeight();

		Boolean isPublicFacing();

		void setAttempt(String formattedAttempt);

		void setFirstName(String firstName);

		void setLastName(String lastName);

		void setPublicFacing(Boolean publicFacing);

		void setStartNumber(Integer integer);

		void setTeamName(String teamName);

		void setWeight(Integer weight);
	}

	@Id("athleteTimer")
	private AthleteTimerElement athleteTimer; // created by Flow during template instanciation

	@Id("breakTimer")
	private BreakTimerElement breakTimer; // created by Flow during template instanciation

	@Id("decisions")
	protected DecisionElement decisions; // created by Flow during template instanciation

	private EventBus uiEventBus;

	/**
	 * Instantiates a new attempt board.
	 */
	public AttemptBoard() {
		athleteTimer.setOrigin(this);
	}

	public void doReset() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			this.getElement().callFunction("reset");
		});
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.QueryParameterReader#isIgnoreGroupFromURL() */
	@Override
	public boolean isIgnoreGroupFromURL() {
		return true;
	}

	@Subscribe
	public void slaveAthleteAnnounced(UIEvent.AthleteAnnounced e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		Athlete a = e.getAthlete();
		doAthleteUpdate(a, e);
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show down on the slave
	 * devices -- the master device is the one where refereeing buttons are attached.
	 * 
	 * @param e
	 */
	@Subscribe
	public void slaveDownSignal(UIEvent.DownSignal e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		// hide the athleteTimer except if the down signal came from this ui.
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			this.getElement().callFunction("down");
		});
	}

	@Subscribe
	public void slaveOrderUpdated(UIEvent.LiftingOrderUpdated e) {
		uiEventLogger.debug("### {} {} stop={} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(), e.isStopAthleteTimer(),
				this.getOrigin(), e.getOrigin());
		OwlcmsSession.withFop(fop -> {
			FOPState state = fop.getState();
			if (state == FOPState.BREAK || state == FOPState.INACTIVE) {
				return;
			} else if (!e.isStopAthleteTimer()) {
				// order change does not affect current lifter
				return;
			} else {
				Athlete a = e.getAthlete();
				doAthleteUpdate(a, e);
			}
		});
	}
	
	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			this.getElement().callFunction("reset");
		});
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist. We need to show decisions on the
	 * slave devices -- the master device is the one where refereeing buttons are attached.
	 * 
	 * @param e
	 */
	@Subscribe
	public void slaveRefereeDecision(UIEvent.RefereeDecision e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
			this.getOrigin(), e.getOrigin());
		// hide the athleteTimer except if the down signal came from this ui.
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			this.getElement().callFunction("down");
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
		doAthleteUpdate(a, e);
	}
	
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		Group g = e.getGroup();
		doDone(g);
	}

	private void doDone(Group g) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getModel().setLastName(MessageFormat.format("Group {0} done.", g.toString()));
			this.getElement().callFunction("groupDone");
		});			
	}

	protected void doAthleteUpdate(Athlete a, UIEvent e) {
		if (a == null) {
			doEmpty();
			return;
		} else if (a.getAttemptsDone() >= 6) {
			OwlcmsSession.withFop((fop) -> doDone(fop.getGroup()));
			return;
		}

		FieldOfPlay fop = OwlcmsSession.getFop();
		if (fop.getState() == FOPState.INACTIVE || fop.getState() == FOPState.BREAK) {
			doEmpty();
			return;
		}
		String trace = null; LoggerUtils.stackTrace();
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
			this.getElement().callFunction("reset");
			uiEventLogger.debug("$$$ attemptBoard doUpdate from\n{}", trace);
			AttemptBoardModel model = getModel();
			model.setLastName(a.getLastName());
			model.setFirstName(a.getFirstName());
			model.setTeamName(a.getTeam());
			model.setStartNumber(a.getStartNumber());
			String formattedAttempt = formatAttempt(a.getAttemptNumber());
			model.setAttempt(formattedAttempt);
			model.setWeight(a.getNextAttemptRequestedWeight());
		});
	}

	@Override
	public void doBreak(BreakStarted e) {
		uiEventLogger.debug("$$$ {} [{}]", e.getClass().getSimpleName(), LoggerUtils.whereFrom());
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			BreakType breakType = fop.getBreakType();
			getModel().setLastName(inferGroupName());
			getModel().setFirstName(inferMessage(breakType));
			getModel().setTeamName("");
			getModel().setAttempt("");

			uiEventLogger.debug("$$$ attemptBoard calling doBreak()");
			this.getElement().callFunction("doBreak");
		}));
	}

	/**
	 * Restoring the attempt board during a break. The information about how/why the break was started
	 * is unavailable.
	 * 
	 * @param fop
	 */
	protected void doBreak(FieldOfPlay fop) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			getModel().setLastName(inferGroupName());
			getModel().setFirstName(inferMessage(inferBreakType(fop)));
			getModel().setTeamName("");
			getModel().setAttempt("");
			this.getElement().callFunction("doBreak", 5 * 60);
			uiEventLogger.debug("$$$ attemptBoard doBreak(fop)");
		});
	}

	protected void doEmpty() {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			this.getElement().callFunction("clear");
		});
	}

	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via QueryParameterReader interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();

			// sync with current status of FOP
			if (fop.getState() == FOPState.INACTIVE) {
				doEmpty();
			} else {
				Athlete curAthlete = fop.getCurAthlete();
				if (fop.getState() == FOPState.BREAK) {
					if (curAthlete != null && curAthlete.getAttemptsDone() >= 6) {
						doDone(fop.getGroup());
					} else {
						doBreak(fop);
					}
				} else {
					doAthleteUpdate(curAthlete, null);
				}
			}
			// we send on fopEventBus, listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	private String formatAttempt(Integer attemptNo) {
		return MessageFormat.format("{0}<sup>{0,choice,1#st|2#nd|3#rd}</sup> att.", attemptNo);
	}

	private Object getOrigin() {
		return this;
	}


	private void init() {
		OwlcmsSession.withFop(fop -> {
			logger.trace("Starting attempt board on FOP {}", fop.getName());
			setId("attempt-board-template");
		});
	}

	@Override
	public String getPageTitle() {
		return "Attempt";
	}

}
