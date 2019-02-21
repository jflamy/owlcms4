/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.init.OwlcmsSession;
import org.ledocte.owlcms.state.FieldOfPlayState;
import org.ledocte.owlcms.state.UIEvent;
import org.ledocte.owlcms.ui.home.MainNavigationLayout;
import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Html;
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
@Theme(Lumo.class)
@Push
public class AnnouncerLayout extends MainNavigationLayout implements UIEventListener {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerLayout.class);
	static {
		logger.setLevel(Level.DEBUG);
	}

	private H2 lastName;
	private H3 firstName;
	private Html attempt;
	private H3 weight;
	private TextField timeField;
	private HorizontalLayout announcerBar;
	private HorizontalLayout lifter;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.home.MainNavigationLayout#createAppLayoutInstance()
	 */
	@Override
	public AppLayout createAppLayoutInstance() {

		AppLayout appLayout = super.createAppLayoutInstance();
		this.announcerBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();

		createAnnouncerBar(announcerBar);

		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px");

		FieldOfPlayState fop = (FieldOfPlayState) OwlcmsSession.getAttribute("fop");
		if (fop != null) {
			EventBus uiEventBus = listenToUIEvents(fop);
			logger.debug("registered {} on {}", appLayout, uiEventBus);
		}

		return appLayout;
	}



	protected void createAnnouncerBar(HorizontalLayout announcerBar) {
		lastName = new H2();
		lastName.setText("\u2013");
		lastName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		firstName = new H3("\u2013");
		firstName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		Div div = new Div(
				lastName,
				firstName);

		attempt = new Html("<h3>? att.</h3>");
		weight = new H3();
		weight.setText("?kg");
		lifter = new HorizontalLayout(
				attempt,
				weight);
		lifter.setAlignItems(FlexComponent.Alignment.STRETCH);

		timeField = new TextField();
		timeField.setValue("0:00");
		timeField.setWidth("4em");
		HorizontalLayout buttons = new HorizontalLayout(
				timeField,
				new Button("announce"),
				new Button("start"),
				new Button("stop"),
				new Button("1 min"),
				new Button("2 min"));
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		HorizontalLayout decisions = new HorizontalLayout(
				new Button("good"),
				new Button("bad"));

		decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		announcerBar.getElement()
			.getStyle()
			.set("flex", "100 1");
		announcerBar.removeAll();
		announcerBar.add(div, lifter, buttons, decisions);
		announcerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		announcerBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	@Subscribe
	public void updateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
		if (this.getUI().isPresent()) {
			logger.trace("received {}", e);
			lastName.setText(e.getAthlete()
				.getLastName());
			firstName.setText(e.getAthlete()
				.getFirstName());
			Html newAttempt = new Html("<h3>" + (e.getAthlete()
				.getAttemptsDone() % 3 + 1) + "<sup>st</sup> att.</h3>");
			lifter.replace(attempt, newAttempt);
			attempt = newAttempt;
			weight.setText(e.getAthlete()
				.getNextAttemptRequestedWeight() + "kg");
		} else {
			logger.warn("received {}, no UI, but listener still registered", e);
		}
	}

	@Subscribe
	public void decisionReset(UIEvent.DecisionReset e) {
		logger.warn("received {}", e);
	}

}
