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
import app.owlcms.ui.group.UIEventProcessor;
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
	final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
	final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
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
			.set("flex", "0 1 0px");
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
		title.setText("Edit Registered Athletes");
		title.add();
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");
		
		Button drawLots = new Button("Draw Lot Numbers", (e) -> {
			drawLots();
		});

		JXLSStartingList startingListWriter = new JXLSStartingList();
		StreamResource href = new StreamResource("startingList.xls", startingListWriter);
		startingList = new Anchor(href, "");
		startingListButton = new Button("Starting List", new Icon(VaadinIcon.DOWNLOAD_ALT));
		startingList.add(startingListButton);
		startingListButton.setEnabled(true);
		
		Button deleteAthletes = new Button("Delete Athletes", (e) -> {
			new ConfirmationDialog(
				"Delete Athletes", 
				"This will delete the athletes currently displayed from the database.<br>Are you sure?", 
				"Done.",
				() -> {deleteAthletes();}
				).open();
			
		});
		deleteAthletes.getElement().setAttribute("title", "Delete Athletes Currently Listed");
		
		Button clearLifts = new Button("Clear Lifts", (e) -> {
			new ConfirmationDialog(
				"Clear Lifts", 
				"This will clear all lifting data for the athletes currently displayed except for initial declarations.<br>Are you sure?", 
				"Lifts cleared",
				() -> {clearLifts();}
				).open();
		});
		deleteAthletes.getElement().setAttribute("title", "Clear Lifts for Athletes Currently Listed");

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
			.set("flex", "100 1");
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
