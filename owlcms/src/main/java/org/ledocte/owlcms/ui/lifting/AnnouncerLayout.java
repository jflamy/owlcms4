/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package org.ledocte.owlcms.ui.lifting;

import org.ledocte.owlcms.ui.home.MainNavigationLayout;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings("serial")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@Theme(Lumo.class)
public class AnnouncerLayout extends MainNavigationLayout {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.ledocte.owlcms.ui.home.MainNavigationLayout#createAppLayoutInstance()
	 */
	@Override
	public AppLayout createAppLayoutInstance() {
		AppLayout appLayout = super.createAppLayoutInstance();
		HorizontalLayout appBarElementWrapper = ((AbstractLeftAppLayoutBase)appLayout).getAppBarElementWrapper();
		HorizontalLayout lifter = new HorizontalLayout(
				new Label("Beauchemin-De la Durantaye, Marie-Dominique"),
				new Label("2nd att."),
				new Label("110kg"));
		TextField timeField = new TextField("","2:00");
		timeField.setWidth("4em");
		HorizontalLayout buttons = new HorizontalLayout(
				timeField,
				new Button("announce"),
				new Button("start"),
				new Button("stop"),
				new Button("1 min"),
				new Button("2 min"));
		HorizontalLayout decisions = new HorizontalLayout(
				new Button("good"),
				new Button("bad"));
		appLayout.getTitleWrapper().getElement().getStyle().set("flex", "0 1 0px");
		appBarElementWrapper.getElement().getStyle().set("flex", "100 1");
		appBarElementWrapper.removeAll();
		appBarElementWrapper.setSpacing(true);
		appBarElementWrapper.add(lifter,buttons,decisions);
		appBarElementWrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.EVENLY);
		appBarElementWrapper.setAlignItems(FlexComponent.Alignment.CENTER);
		return appLayout;

	}

}
