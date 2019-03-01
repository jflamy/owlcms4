/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.lifting;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FOPEvent;
import org.ledocte.owlcms.state.UIEvent;
import org.ledocte.owlcms.ui.home.MainNavigationLayout;
import org.ledocte.owlcms.ui.home.SafeEventBusRegistration;
import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings("serial")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@HtmlImport("frontend://styles/shared-styles.html")
@Theme(Lumo.class)
@Push
public class AnnouncerLayout extends MainNavigationLayout implements SafeEventBusRegistration {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerLayout.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");
	
	private H2 lastName;
	private H3 firstName;
	private Html attempt;
	private H3 weight;
	private TextField timeField;
	private HorizontalLayout announcerBar;
	private HorizontalLayout lifter;
	
	public AnnouncerLayout() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.
	 * github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		this.announcerBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		createAnnouncerBar(announcerBar);
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px");
		return appLayout;
	}

	protected void createAnnouncerBar(HorizontalLayout announcerBar) {
		lastName = new H2();
		lastName.setText("\u2013");
		lastName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		firstName = new H3("");
		firstName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		Div div = new Div(
				lastName,
				firstName);

		attempt = new Html("<span></span>");
		weight = new H3();
		weight.setText("");
		lifter = new HorizontalLayout(
				attempt,
				weight);
		lifter.setAlignItems(FlexComponent.Alignment.CENTER);

		timeField = new TextField();
		timeField.setValue("0:00");
		timeField.setWidth("4em");
		HorizontalLayout buttons = new HorizontalLayout(
				timeField,
				new Button("announce", (e) -> {
					getFopEventBus().post(new FOPEvent.AthleteAnnounced(announcerBar.getUI().get()));
				}),
				new Button("start", (e) -> {
					getFopEventBus().post(new FOPEvent.TimeStartedManually(announcerBar.getUI().get()));
				}),
				new Button("stop", (e) -> {
					getFopEventBus().post(new FOPEvent.TimeStoppedManually(announcerBar.getUI().get()));
				}),
				new Button("1 min"),
				new Button("2 min"));
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		HorizontalLayout decisions = new HorizontalLayout(
				new Button("good", (e) -> {
					getFopEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get(), true, true, true, true));
					getFopEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
				}),
				new Button("bad", (e) -> {
					getFopEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get(), false, false, false, false));
					getFopEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
				}));

		decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		announcerBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		announcerBar.removeAll();
		announcerBar.add(div, lifter, buttons, decisions);
		announcerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		announcerBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	private EventBus getFopEventBus() {
		return OwlcmsSession.getFop().getEventBus();
	}

	@Subscribe
	public void updateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
		Optional<UI> ui2 = announcerBar.getUI();
		if (ui2.isPresent()) {
			uiEventLogger.debug("+++ received {}", e);
			ui2.get()
				.access(() -> {
					Athlete athlete = e.getAthlete();
					Integer timeAllowed = e.getTimeAllowed();
					doUpdateAnnouncerBar(athlete, timeAllowed);
				});
		} else {
			uiEventLogger.debug("+++ received {}, but announcer bar detached from UI", e);
			uiEventUnregister();
		}
	}
	
	@Subscribe
	public void setTime(UIEvent.SetTime e) {
		Optional<UI> ui2 = announcerBar.getUI();
		if (ui2.isPresent()) {
			uiEventLogger.debug("+++ received {}", e);
			ui2.get()
				.access(() -> {
					Integer timeRemaining = e.getTimeRemaining();
					timeField.setValue(msToString(timeRemaining));
				});
		} else {
			uiEventLogger.debug("+++ received {}, but announcer bar detached from UI", e);
			uiEventUnregister();
		}
	}
	
	@Subscribe
	public void startTime(UIEvent.StartTime e) {
		Optional<UI> ui2 = announcerBar.getUI();
		if (ui2.isPresent()) {
			uiEventLogger.debug("+++ received {}", e);
			ui2.get()
				.access(() -> {
					Integer timeRemaining = e.getTimeRemaining();
					timeField.setValue(msToString(timeRemaining));
				});
		} else {
			uiEventLogger.debug("+++ received {}, but announcer bar detached from UI", e);
			uiEventUnregister();
		}
	}
	
	@Subscribe
	public void stopTime(UIEvent.StopTime e) {
		Optional<UI> ui2 = announcerBar.getUI();
		if (ui2.isPresent()) {
			uiEventLogger.debug("+++ received {}", e);
			ui2.get()
				.access(() -> {
					Integer timeRemaining = e.getTimeRemaining();
					timeField.setValue(msToString(timeRemaining));
				});
		} else {
			uiEventLogger.debug("+++ received {}, but announcer bar detached from UI", e);
			uiEventUnregister();
		}
	}

	public void doUpdateAnnouncerBar(Athlete athlete, Integer timeAllowed) {
		if (athlete != null) {
			lastName.setText(athlete.getLastName());
			firstName.setText(athlete.getFirstName());
			timeField.setValue(msToString(timeAllowed));
			Html newAttempt = new Html(
					"<h3>" + (athlete.getAttemptsDone() % 3 + 1) + "<sup>st</sup> att.</h3>");
			lifter.replace(attempt, newAttempt);
			attempt = newAttempt;
			weight.setText(athlete.getNextAttemptRequestedWeight() + "kg");
		} else {
			lastName.setText("\u2013");
			firstName.setText("");
			Html newAttempt = new Html("<span></span>");
			lifter.replace(attempt, newAttempt);
			attempt = newAttempt;
			weight.setText("");
		}
	}

	private String msToString(Integer millis) {
		long hours = TimeUnit.MILLISECONDS.toHours(millis);
		long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
		long fullHoursInMinutes = TimeUnit.HOURS.toMinutes(hours);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
		long fullMinutesInSeconds = TimeUnit.MINUTES.toSeconds(minutes);
		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours,
		      minutes - fullHoursInMinutes,
		      seconds - fullMinutesInSeconds);
		} else {
			return String.format("%02d:%02d",
			      minutes,
			      seconds - fullMinutesInSeconds);
		}
	}

	@Subscribe
	public void decisionReset(UIEvent.DecisionReset e) {
		uiEventLogger.info("received {}", e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.github.appreciated.app.layout.router.AppLayoutRouterLayout#onAttach(com.
	 * vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.trace("attaching {} to {}", attachEvent.getSource(), attachEvent.getUI());
		super.onAttach(attachEvent);
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doUpdateAnnouncerBar(fop.getCurAthlete(), fop.timeAllowed());
			
			// connect to bus for new updating events
			EventBus uiEventBus = fop.getUiEventBus();
			logger.debug("registering {} to {}", this, uiEventBus.identifier());
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.vaadin.flow.component.Component#onDetach(com.vaadin.flow.component.
	 * DetachEvent)
	 */
	@Override
	protected void onDetach(DetachEvent detachEvent) {
		logger.trace("detaching {} from {}", detachEvent.getSource(), detachEvent.getUI());
		super.onDetach(detachEvent);
		uiEventUnregister();
	}

	public void uiEventUnregister() {
		OwlcmsSession.withFop(fop -> {
			EventBus uiEventBus = fop.getUiEventBus();
			logger.debug("unregistering {} from {}", this, uiEventBus.identifier());
			try {
				uiEventBus.unregister(this);
			} catch (Exception ex) {
			}
		});
	}

}
