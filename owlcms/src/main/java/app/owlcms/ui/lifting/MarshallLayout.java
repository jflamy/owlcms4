/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.state.FOPEvent;
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
public class MarshallLayout extends BaseLayout {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(MarshallLayout.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("owlcms.uiEventLogger");
	
	public MarshallLayout() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.TimeStoppedManually(announcerBar.getUI().get()));
		});
		stop.getElement().setAttribute("theme", "primary icon");
		HorizontalLayout buttons = new HorizontalLayout(
				stop);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.lifting.BaseLayout#createTopBar(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected void createTopBar(HorizontalLayout announcerBar) {
		super.createTopBar(announcerBar);
		title.setText("Marshall");
		groupSelect.setReadOnly(true);
	}
	
	
}
