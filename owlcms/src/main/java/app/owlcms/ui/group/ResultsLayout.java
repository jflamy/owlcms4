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

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
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

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.spreadsheet.JXLSResultSheet;
import app.owlcms.ui.home.OwlcmsRouterLayout;
import app.owlcms.ui.home.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Results Page -- top bar.
 */
@SuppressWarnings("serial")
public class ResultsLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor {

	private final Logger logger = (Logger)LoggerFactory.getLogger(ResultsLayout.class);
	protected void initLoggers() {
		logger.setLevel(Level.DEBUG);
	}
	
	private HorizontalLayout topBar;
	private ComboBox<Group> gridGroupFilter;
	private AppLayout appLayout;
	private ComboBox<Group> groupSelect;
	private Group layoutGroup;
	private Button download;
	private Anchor groupResults;
	
	public ResultsLayout() {
		initLoggers();
	}


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
	 * The layout is created before the content. This routine creates the content, and links Layout and
	 * Content together so we can refer to the content using {@link #getLayoutContent()} and the content can
	 * refer to us via {@link AppLayoutContent#getParentLayout()}
	 * 
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		ResultsContent ResultsContent = (ResultsContent) getLayoutContent();
		gridGroupFilter = ResultsContent.getGroupFilter();
		logger.debug("showing layout content {} {}", content, gridGroupFilter);
		setGroupSelectionListener();
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
		title.setText("Group Results");
		title.add();
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");
		
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setValue(null);
		groupSelect.setWidth("8em");

		JXLSResultSheet writer = new JXLSResultSheet();
		StreamResource href = new StreamResource("resultSheet.xls", writer);
		groupResults = new Anchor(href, "");
		download = new Button("Group Results",new Icon(VaadinIcon.DOWNLOAD_ALT));
		download.addClickListener((e) -> {
			writer.setGroup(layoutGroup);
		});
		groupResults.add(download);
			
		HorizontalLayout buttons = new HorizontalLayout(
				groupResults);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

		topBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		topBar.removeAll();
		topBar.add(title, groupSelect, buttons);
		topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
		topBar.setFlexGrow(0.2, title);
		topBar.setSpacing(true);
		topBar.setAlignItems(FlexComponent.Alignment.CENTER);
	}


	protected void setGroupSelectionListener() {
		groupSelect.setValue(getContentGroup());
		groupSelect.addValueChangeListener(e -> {
			setContentGroup(e.getValue());
			groupResults.getElement().setAttribute("download", "results"+(layoutGroup != null ? layoutGroup : "_all") +".xls");
		});
	}

	public void setContentGroup(Group group) {
		this.layoutGroup = group;
		gridGroupFilter.setValue(group);
	}
	
	public Group getContentGroup() {
		return gridGroupFilter.getValue();
	}


	protected void errorNotification() {
		Label content = new Label(
		        "Please select a Group first.");
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


	/**
	 * Set the top bar settings for the group.
	 * 
	 * Initialization proceeds as follows (1) The content class receives the routing information and the
	 * URL parameters (including the group), but does not create its user interface. 
	 * (2) then the parent layout (this class) is created with the top bar UI, then
	 * (3) the content UI (the grid and the filters) is created and added.
	 * 
	 * This method is used during stage 2 to populate information gathered at stage 1.
	 * 
	 * @param nGroup the group as obtained from the URL
	 */
	public void setLayoutGroup(Group nGroup) {
		this.layoutGroup = nGroup;
		this.groupSelect.setValue(nGroup);
		groupResults.getElement().setAttribute("download", "results"+(layoutGroup != null ? layoutGroup : "_all") +".xls");
		
	}
}
