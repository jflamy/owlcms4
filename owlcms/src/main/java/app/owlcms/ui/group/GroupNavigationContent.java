/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.displays.results.ResultsBoard;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class GroupNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "group", layout = OwlcmsRouterLayout.class)
public class GroupNavigationContent extends BaseNavigationContent implements NavigationPage {

	final private static Logger logger = (Logger)LoggerFactory.getLogger(GroupNavigationContent.class);
	static { logger.setLevel(Level.INFO);}
	
	/**
	 * Instantiates a new lifting navigation content.
	 */
	public GroupNavigationContent() {
		logger.trace("GroupNavigationContent constructor start");
		VerticalLayout intro = new VerticalLayout();
		addP(intro,
				"The <b><i>Announcer</i></b> selects the current group for the platform.<br> " +
				"This changes the current group for all displays and screens associated with the platform "+
				"(announcer, timekeeper, marshall, result boards, attempt board, jury, etc.)");
		intro.getElement().getStyle().set("margin-bottom", "0");
		
		Button announcer = new Button(
				"Announcer",
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(AnnouncerContent.class)));
		Button marshall = new Button(
				"Marshall",
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(MarshallContent.class)));
		Button timekeeper = new Button(
				"Timekeeper",
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(TimekeeperContent.class)));
		Button jury = new Button(
			"Jury Display",
			buttonClickEvent -> UI.getCurrent().getPage()
				.executeJavaScript(getWindowOpener(ResultsBoard.class)));

		
		fillH(intro, this);
		
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
			announcer,
			marshall,
			timekeeper,
			jury
			);
		doGroup("Technical Official Screens (select the correct platform above before clicking)", grid1, this);
		jury.setEnabled(false);
		
		Button weighIn = new Button("Weigh-In and Start Numbers",
			buttonClickEvent -> UI.getCurrent()
				.navigate(WeighinContent.class));
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(
			weighIn);
		doGroup("Weigh-in", grid3, this);
		logger.trace("GroupNavigationContent constructor stop");
	}


	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#createTopBarFopField(java.lang.String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		Label fopLabel = new Label(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}


	@Override
	protected String getTitle() {
		return "Run Lifting Group";
	}
}
