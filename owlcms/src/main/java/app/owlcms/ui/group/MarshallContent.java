/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import app.owlcms.ui.shared.QueryParameterReader;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "group/marshall", layout = AthleteGridLayout.class)
public class MarshallContent extends AthleteGridContent implements QueryParameterReader {

	// @SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(MarshallContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	public MarshallContent() {
		super();
		setTopBarTitle("Marshall");
	}


	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus()
				.post(new FOPEvent.TimeStopped(this.getOrigin())));
		});
		stop.getElement().setAttribute("theme", "primary icon");
		HorizontalLayout buttons = new HorizontalLayout(
			stop);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar() */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}

	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}
}
