/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.NavigationPage;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.home.HomeNavigationContent;
import app.owlcms.ui.shared.BaseNavigationContent;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class LiftingNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting", layout = OwlcmsRouterLayout.class)
public class LiftingNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger)LoggerFactory.getLogger(LiftingNavigationContent.class);
	static { logger.setLevel(Level.INFO);}
	
	/**
	 * Instantiates a new lifting navigation content.
	 */
	public LiftingNavigationContent() {
		logger.trace("LiftingNavigationContent constructor start"); //$NON-NLS-1$
		VerticalLayout intro = new VerticalLayout();
		addP(intro,
				getTranslation("LiftingNavigationContent.0") + //$NON-NLS-1$
				getTranslation("LiftingNavigationContent.1")+ //$NON-NLS-1$
				getTranslation("LiftingNavigationContent.2")); //$NON-NLS-1$
		intro.getElement().getStyle().set("margin-bottom", "0"); //$NON-NLS-1$ //$NON-NLS-2$
		
		Button announcer = new Button(
				getTranslation("LiftingNavigationContent.3"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(AnnouncerContent.class)));
		Button marshall = new Button(
				getTranslation("LiftingNavigationContent.4"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(MarshallContent.class)));
		Button timekeeper = new Button(
				getTranslation("LiftingNavigationContent.5"), //$NON-NLS-1$
				buttonClickEvent -> UI.getCurrent().getPage()
					.executeJavaScript(getWindowOpener(TimekeeperContent.class)));
		Button jury = new Button(
			getTranslation("LiftingNavigationContent.6"), //$NON-NLS-1$
			buttonClickEvent -> UI.getCurrent().getPage()
				.executeJavaScript(getWindowOpener(JuryContent.class)));

		fillH(intro, this);
		
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(
			announcer,
			marshall,
			timekeeper,
			jury
			);
		doGroup(getTranslation("LiftingNavigationContent.7"), grid1, this); //$NON-NLS-1$
		
		Button weighIn = new Button(getTranslation("LiftingNavigationContent.8"), //$NON-NLS-1$
			buttonClickEvent -> UI.getCurrent()
				.navigate(WeighinContent.class));
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(
			weighIn);
		doGroup(getTranslation("LiftingNavigationContent.9"), grid3, this); //$NON-NLS-1$
		logger.trace("LiftingNavigationContent constructor stop"); //$NON-NLS-1$
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
		return getTranslation("LiftingNavigationContent.10"); //$NON-NLS-1$
	}


	@Override
	public String getPageTitle() {
		return getTranslation("LiftingNavigationContent.11"); //$NON-NLS-1$
	}
}
