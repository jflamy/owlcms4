/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
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
public class AnnouncerLayout extends BaseGridLayout {

	final private Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerLayout.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	public AnnouncerLayout() {
		super();
		logger.setLevel(Level.DEBUG);
		uiEventLogger.setLevel(Level.DEBUG);
	}

	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button announce = new Button(AvIcons.MIC.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.AthleteAnnounced(announcerBar.getUI().get()));
		});
		announce.getElement().setAttribute("theme", "primary icon");
		Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.TimeStartedManually(announcerBar.getUI().get()));
		});
		start.getElement().setAttribute("theme", "primary icon");
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.TimeStoppedManually(announcerBar.getUI().get()));
		});
		stop.getElement().setAttribute("theme", "primary icon");
		Button _1min = new Button("1:00", (e) -> {
			getFopEventBus().post(new FOPEvent.ForceTime(60000,announcerBar.getUI().get()));
		});
		_1min.getElement().setAttribute("theme", "icon");
		Button _2min = new Button("2:00", (e) -> {
			getFopEventBus().post(new FOPEvent.ForceTime(120000,announcerBar.getUI().get()));
		});
		_2min.getElement().setAttribute("theme", "icon");
		HorizontalLayout buttons = new HorizontalLayout(
				announce,
				start,
				stop,
				_1min,
				_2min);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.group.BaseGridLayout#createTopBar(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected void createTopBar(HorizontalLayout announcerBar) {
		super.createTopBar(announcerBar);
		title.setText("Announcer");
		groupSelect.setReadOnly(true);
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		Button good = new Button(IronIcons.DONE.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get() ,true, true, true, true));
			getFopEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
		});
		good.getElement().setAttribute("theme", "success icon");
		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			getFopEventBus().post(new FOPEvent.RefereeDecision(announcerBar.getUI().get(), false, false, false, false));
			getFopEventBus().post(new FOPEvent.DecisionReset(announcerBar.getUI().get()));
		});
		bad.getElement().setAttribute("theme", "error icon");
		HorizontalLayout decisions = new HorizontalLayout(
				good,
				bad);
		return decisions;
	}

}
