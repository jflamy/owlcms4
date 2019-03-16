/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.lifting;

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
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.ui.appLayout.AppLayoutContent;
import app.owlcms.ui.home.MainNavigationLayout;
import app.owlcms.ui.home.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@SuppressWarnings("serial")
public class WeighinLayout extends MainNavigationLayout implements SafeEventBusRegistration, UIEventProcessor {

	private final static Logger logger = (Logger)LoggerFactory.getLogger(WeighinLayout.class);
	static {logger.setLevel(Level.DEBUG);}
	
	private HorizontalLayout topBar;
	private ComboBox<Group> gridGroupFilter;
	private AppLayout appLayout;
	private ComboBox<Group> groupSelect;
	private Group group;
	private Button download;
	private Anchor startingWeights;

	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
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
	 * the content using {@link #getLayoutContent()} and the content can refer to us via
	 * {@link AppLayoutContent#getParentLayout()}
	 * 
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		WeighinContent weighinContent = (WeighinContent) getLayoutContent();
		weighinContent.setParentLayout(this);
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
		groupSelect.setValue(null);
		groupSelect.addValueChangeListener(e -> {
			setContentGroup(e);
			download.setEnabled(e.getValue() != null);
			startingWeights.getElement().setAttribute("download", "startingWeights_"+group+".xls");
		});


		Button start = new Button("Generate Start Numbers", (e) -> {
			generateStartNumbers();
		});
		Button clear = new Button("Clear Start Numbers", (e) -> {
			clearStartNumbers();
		});
		
		JXLSWeighInSheet writer = new JXLSWeighInSheet(group, true);
		StreamResource href = new StreamResource("startingWeights.xls", writer);
		startingWeights = new Anchor(href, "");
		download = new Button("Starting Weights Sheet",new Icon(VaadinIcon.DOWNLOAD_ALT));
		download.addClickListener((e) -> {
			writer.setGroup(group);
		});
		startingWeights.add(download);
		download.setEnabled(false);
			
		HorizontalLayout buttons = new HorizontalLayout(
				start,
				clear,
				startingWeights);
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
		((WeighinContent)getLayoutContent()).refresh();
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
		((WeighinContent)getLayoutContent()).refresh();
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
