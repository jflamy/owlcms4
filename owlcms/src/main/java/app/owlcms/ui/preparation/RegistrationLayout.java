/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.ui.preparation;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.JXLSStartingList;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Athletes Registration Layout.
 */
@SuppressWarnings("serial")
public class RegistrationLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor {
	
	final private static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationLayout.class);
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine"); //$NON-NLS-1$
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag"); //$NON-NLS-1$
	static {
		logger.setLevel(Level.INFO);
		jexlLogger.setLevel(Level.ERROR);
		tagLogger.setLevel(Level.ERROR);
	}

	private AppLayout appLayout;
	private HorizontalLayout topBar;
	private Anchor startingList;
	private Button startingListButton;

	/* (non-Javadoc)
	 * 
	 * @see
	 * app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.
	 * behaviour.Behaviour) */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		variant = Behaviour.LEFT;
		appLayout = super.getLayoutConfiguration(variant);
		this.topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		createTopBar(topBar);
		appLayout.closeDrawer();
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px"); //$NON-NLS-1$ //$NON-NLS-2$
		return appLayout;
	}

	/**
	 * Create the top bar.
	 * 
	 * Note: the top bar is created before the content.
	 * 
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 * 
	 * @param topBar
	 */
	protected void createTopBar(HorizontalLayout topBar) {

		H3 title = new H3();
		title.setText(getTranslation("RegistrationLayout.0")); //$NON-NLS-1$
		title.add();
		title.getStyle()
			.set("margin", "0px 0px 0px 0px") //$NON-NLS-1$ //$NON-NLS-2$
			.set("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$
		
		Button drawLots = new Button(getTranslation("RegistrationLayout.1"), (e) -> { //$NON-NLS-1$
			drawLots();
		});

		JXLSStartingList startingListWriter = new JXLSStartingList();
		StreamResource href = new StreamResource("startingList.xls", startingListWriter); //$NON-NLS-1$
		startingList = new Anchor(href, ""); //$NON-NLS-1$
		startingListButton = new Button(getTranslation("RegistrationLayout.2"), new Icon(VaadinIcon.DOWNLOAD_ALT)); //$NON-NLS-1$
		startingList.add(startingListButton);
		startingListButton.setEnabled(true);
		
		Button deleteAthletes = new Button(getTranslation("RegistrationLayout.3"), (e) -> { //$NON-NLS-1$
			new ConfirmationDialog(
				getTranslation("RegistrationLayout.4"),  //$NON-NLS-1$
				getTranslation("RegistrationLayout.5"),  //$NON-NLS-1$
				getTranslation("RegistrationLayout.6"), //$NON-NLS-1$
				() -> {deleteAthletes();}
				).open();
			
		});
		deleteAthletes.getElement().setAttribute("title", getTranslation("RegistrationLayout.7")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Button clearLifts = new Button(getTranslation("RegistrationLayout.8"), (e) -> { //$NON-NLS-1$
			new ConfirmationDialog(
				getTranslation("RegistrationLayout.9"),  //$NON-NLS-1$
				getTranslation("RegistrationLayout.10"),  //$NON-NLS-1$
				getTranslation("RegistrationLayout.11"), //$NON-NLS-1$
				() -> {clearLifts();}
				).open();
		});
		deleteAthletes.getElement().setAttribute("title", getTranslation("RegistrationLayout.12")); //$NON-NLS-1$ //$NON-NLS-2$

		HorizontalLayout buttons = new HorizontalLayout(
				drawLots,
				startingList,
				deleteAthletes,
				clearLifts);
		buttons.setPadding(true);
		buttons.setSpacing(true);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar
			.getElement()
			.getStyle()
			.set("flex", "100 1"); //$NON-NLS-1$ //$NON-NLS-2$
		topBar.removeAll();
		topBar.add(title, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	private void clearLifts() {
		JPAService.runInTransaction(em -> {
			RegistrationContent content = (RegistrationContent) getLayoutComponentContent();
			List<Athlete> athletes = (List<Athlete>) content.doFindAll(em);
			for (Athlete a : athletes) {
				a.clearLifts();
				em.merge(a);
			}
			em.flush();
			return null;
		});
	}



	private void deleteAthletes() {
		RegistrationContent content = (RegistrationContent) getLayoutComponentContent();
		JPAService.runInTransaction(em -> {
			List<Athlete> athletes = (List<Athlete>) content.doFindAll(em);
			for (Athlete a: athletes) {
				em.remove(a);
			}
			em.flush();
			return null;
		});
		content.refreshCrudGrid();
	}

	private void drawLots() {
		RegistrationContent content = (RegistrationContent) getLayoutComponentContent();
		JPAService.runInTransaction(em -> {
			List<Athlete> toBeShuffled = AthleteRepository.doFindAll(em);
			AthleteSorter.drawLots(toBeShuffled);
			for (Athlete a: toBeShuffled) {
				em.merge(a);
			}
			em.flush();
			return null;
		});
		content.refreshCrudGrid();
	}
}
