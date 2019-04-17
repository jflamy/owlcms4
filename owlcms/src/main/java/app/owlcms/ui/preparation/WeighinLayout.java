/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/master/License.txt
 */
package app.owlcms.ui.preparation;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.ui.group.UIEventProcessor;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@SuppressWarnings("serial")
public class WeighinLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor {

	private final static Logger logger = (Logger)LoggerFactory.getLogger(WeighinLayout.class);
	static {logger.setLevel(Level.INFO);}
	
	private HorizontalLayout topBar;
	private ComboBox<Group> gridGroupFilter;
	private AppLayout appLayout;
	private ComboBox<Group> groupSelect;
	private Group group;
	private Anchor startingWeights;
	private Button startingWeightsButton;
	private Anchor cards;
	private Button cardsButton;


	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		appLayout = super.getLayoutConfiguration(variant);
		this.topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		createTopBar(topBar);
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px");
		return appLayout;
	}
	
	/**
	 * The layout is created before the content. This routine has created the content, we can refer to
	 * the content using {@link #getLayoutComponentContent()} and the content can refer to us via
	 * {@link AppLayoutContent#getParentLayout()}
	 * 
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		WeighinContent weighinContent = (WeighinContent) getLayoutComponentContent();
		gridGroupFilter = weighinContent.getGroupFilter();
	}
	
	/**
	 * Create the top bar.
	 * 
	 * Note: the top bar is created before the content.
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 * 
	 * @param topBar
	 */
	protected void createTopBar(HorizontalLayout topBar) {

		H3 title = new H3();
		title.setText("Weigh-In");
		title.add();
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");
		
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		OwlcmsSession.withFop((fop) -> {
			//TODO get group from URL, but not connected to FOP
			groupSelect.setValue(null);
		});
		groupSelect.addValueChangeListener(e -> {
			setContentGroup(e);
			startingWeightsButton.setEnabled(e.getValue() != null);
			startingWeights.getElement().setAttribute("startingWeightsButton", "startingWeights_"+group+".xls");
		});


		Button start = new Button("Generate Start Numbers", (e) -> {
			generateStartNumbers();
		});
		Button clear = new Button("Clear Start Numbers", (e) -> {
			clearStartNumbers();
		});
		
		JXLSWeighInSheet startingWeightsWriter = new JXLSWeighInSheet(true);
		StreamResource href = new StreamResource("startingWeights.xls", startingWeightsWriter);
		startingWeights = new Anchor(href, "");
		startingWeightsButton = new Button("Starting Weights Sheet",new Icon(VaadinIcon.DOWNLOAD_ALT));
		startingWeightsButton.addClickListener((e) -> {
			startingWeightsWriter.setGroup(group);
		});
		startingWeights.add(startingWeightsButton);
		startingWeightsButton.setEnabled(false);
		
		JXLSCards cardsWriter = new JXLSCards(true);
		StreamResource href1 = new StreamResource("startingWeights.xls", cardsWriter);
		cards = new Anchor(href1, "");
		cardsButton = new Button("Athlete Cards",new Icon(VaadinIcon.DOWNLOAD_ALT));
		cardsButton.addClickListener((e) -> {
			cardsWriter.setGroup(group);
		});
		cards.add(cardsButton);
		cardsButton.setEnabled(true);
			
		HorizontalLayout buttons = new HorizontalLayout(
				start,
				clear,
				startingWeights,
				cards
				);
		buttons.setPadding(true);
		buttons.setSpacing(true);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		topBar.removeAll();
		topBar.add(title, groupSelect, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}

	protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
		group = e.getValue();
		gridGroupFilter.setValue(e.getValue());
	}

	private void clearStartNumbers() {
		Group group = groupSelect.getValue();
		if (group == null) {
			errorNotification();
			return;
		}
		JPAService.runInTransaction((em) -> {
			List<Athlete> currentGroupAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em,group, false);
			for (Athlete a: currentGroupAthletes) {
				a.setStartNumber(0);
			}
			return currentGroupAthletes;
		});
		((WeighinContent)getLayoutComponentContent()).refresh();
	}
	
	private void generateStartNumbers() {
		Group group = groupSelect.getValue();
		if (group == null) {
			errorNotification();
			return;
		}
		JPAService.runInTransaction((em) -> {
			List<Athlete> currentGroupAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em,group, true);
			AthleteSorter.displayOrder(currentGroupAthletes);
			AthleteSorter.assignStartNumbers(currentGroupAthletes);
			return currentGroupAthletes;
		});
		((WeighinContent)getLayoutComponentContent()).refresh();
	}

	protected void errorNotification() {
		Label content = new Label(
		        "Please select a group first.");
		content.getElement().setAttribute("theme", "error");
		Button buttonInside = new Button("Got it.");
		buttonInside.getElement().setAttribute("theme","error primary");
		VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
		verticalLayout.setAlignItems(Alignment.CENTER);
		Notification notification = new Notification(verticalLayout);
		notification.setDuration(3000);
		buttonInside.addClickListener(event -> notification.close());
		notification.setPosition(Position.MIDDLE);
		notification.open();
	}
}
