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

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.data.group.Group;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.ui.home.BaseNavigationContent;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.home.OwlcmsRouterLayout;
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
		addParagraph(intro, "Use the dropdown to select the platform where the display is located.");
		addParagraph(intro, "At the beginning of each competition group, select the group. "+
				"Changing the group changes it for all displays and screens connected to this platform "+
				"(announcer, timekeeper, marshall, results, attempt board, jury, etc.");
		addParagraph(intro, "Use one of the buttons below to start one of the technical official screens.");
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

		
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(
			announcer,
			marshall,
			timekeeper
			);
		
		fillH(intro, this);
		fillH(grid, this);
		logger.trace("GroupNavigationContent constructor stop");
	}
	
	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.BaseNavigationContent#configureTopBar(java.lang.String, com.github.appreciated.app.layout.behaviour.AppLayout)
	 */
	@Override
	protected void createTopBar(String title) {
		super.createTopBar("Run Lifting Group");
	}

	@Override
	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		Label fopLabel = new Label(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlayState> fopSelect = createFopSelect(placeHolder);
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			OwlcmsSession.withFop((fop) -> {
				Group group = e.getValue().getGroup();
				Group currentGroup = fop.getGroup();
				if (group == null) {
					fop.switchGroup(null, this.getOrigin());
				} else if (!group.equals(currentGroup)) {
					fop.switchGroup(group, this.getOrigin());
				}
			});
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}
}
