/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState.State;
import app.owlcms.state.UIEvent;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.SafeEventBusRegistration;
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
public class AttemptBoard extends PolymerTemplate<AttemptBoard.AttemptBoardModel> implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor {
	
	/**
	 * ResultBoardModel
	 * 
	 * Vaadin Flow propagates these variables to the corresponding Polymer template JavaScript properties.
	 * When the JS properties are changed, a "propname-changed" event is triggered.
	 * {@link Element.#addPropertyChangeListener(String, String, com.vaadin.flow.dom.PropertyChangeListener)}
	 *
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
	final private static Logger logger = (Logger)LoggerFactory.getLogger(AttemptBoard.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	@Id("timer")
	private TimerElement timer; // created by Flow during template instanciation
	@Id("decisions")
	protected DecisionElement decisions; // created by Flow during template instanciation
	private EventBus uiEventBus;
	private boolean isBreak;

	/**
	 * Instantiates a new attempt board.
	 */
	public AttemptBoard() {
	}

	@Subscribe
	public void athleteAnnounced(UIEvent.AthleteAnnounced e) {
		if (this.isBreak) {
			stopBreak();
		}
		Athlete a = e.getAthlete();
		doUpdate(a, e);
	}

	@Subscribe
	public void breakDone(UIEvent.BreakDone e) {
		stopBreak();
		Athlete a = e.getAthlete();
		doUpdate(a, e);
	}

	@Subscribe
	public void breakStarted(UIEvent.BreakStarted e) {
		doBreak(e.getTimeRemaining());
	}

	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist.
	 * We need to show down on the slave devices -- the master device is
	 * the one where refereeing buttons are attached.
	 * @param e
	 */
	@Subscribe
	public void downSignal(UIEvent.DownSignal e) {
		if (this instanceof AthleteFacingAttemptBoard) {
			logger.trace("%%% {} DownSignal {} {}", this.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
		} else {
			logger.trace("&&& {} DownSignal {} {}", this.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
		}
		// hide the timer except if the down signal came from this ui.
		UIEventProcessor.uiAccess(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			this.getElement().callFunction("down");
		});
	}

	@Override
	public boolean isIgnoreGroup() {
		return true;
	}

	@Subscribe
	public void orderUpdated(UIEvent.LiftingOrderUpdated e) {
		if (this.isBreak) return;
		Athlete a = e.getAthlete();
		doUpdate(a, e);
	}
	
	
	/**
	 * Multiple attempt boards and athlete-facing boards can co-exist.
	 * We need to show decisions on the slave devices -- the master device is
	 * the one where refereeing buttons are attached.
	 * @param e
	 */
	@Subscribe
	public void refereeDecision(UIEvent.RefereeDecision e) {
		if (this instanceof AthleteFacingAttemptBoard) {
			logger.trace("%%% {} RefereeDecision {} {}", this.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
		} else {
			logger.trace("&&& {} RefereeDecision {} {}", this.getClass().getSimpleName(), this.getOrigin(), e.getOrigin());
		}
		// hide the timer except if the down signal came from this ui.
		UIEventProcessor.uiAccess(this, uiEventBus, e, this.getOrigin(), e.getOrigin(), () -> {
			this.getElement().callFunction("down");
		});
	}
	
	public void stopBreak() {
		this.isBreak = false;
		this.timer.stop();
	}

	private void doBreak(Integer timeRemaining) {
		if (timeRemaining != null) {
			OwlcmsSession.withFop(fop -> fop.getTimer().setTimeRemaining(timeRemaining));
		}
		this.isBreak = true;
		getModel().setLastName("Break");
		getModel().setFirstName("");
		getModel().setTeamName("");
		getModel().setAttempt("");
		this.getElement().callFunction("doBreak");
		uiEventLogger.warn("$$$ attemptBoard doBreak");
		this.timer.start();
	}
	
	private void doEmpty() {
		this.getElement().callFunction("clear");
	}
	
	private String formatAttempt(Integer attemptNo) {
		return MessageFormat.format("{0}<sup>{0,choice,1#st|2#nd|3#rd}</sup> att.",attemptNo);
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
	
	protected void doUpdate(Athlete a, UIEvent e) {
		if (a == null) return;
		UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
				this.getElement().callFunction("reset");
				uiEventLogger.warn("$$$ attemptBoard doUpdate");
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


	/* @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent) */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// fop obtained via QueryParameterReader interface default methods.
		OwlcmsSession.withFop(fop -> {
			init();
			
			// sync with current status of FOP
			if (fop.getState() == State.INACTIVE) {
				doEmpty();
			} else if (fop.getState() == State.BREAK) {
				doBreak(null);
			} else {
				doUpdate(fop.getCurAthlete(), null);
			} 
			// we send on fopEventBus, listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
}
