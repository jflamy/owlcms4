/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 *
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */

package app.owlcms.ui.home;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;
import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.components.appLayout.AppLayoutContent;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.FieldOfPlayState;
import app.owlcms.ui.group.UIEventProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class NavigationContent
 * 
 * Defi
 */
@SuppressWarnings("serial")
public abstract class BaseNavigationContent extends VerticalLayout
implements QueryParameterReader, ContentWrapping, SafeEventBusRegistration, UIEventProcessor, AppLayoutContent {

	// @SuppressWarnings("unused")
	final private Logger logger = (Logger) LoggerFactory.getLogger(BaseNavigationContent.class);
	final private Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());
	private void initLoggers() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.DEBUG);
	}

	protected Location location;
	protected UI locationUI;
	protected EventBus uiEventBus;
	protected NavigationLayout parentLayout;

	protected ComboBox<Group> groupFilter = new ComboBox<>();

	/**
	 * Instantiates a new announcer content.
	 * Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public BaseNavigationContent() {
		initLoggers();
	}

	/**
	 * Process URL parameters, including query parameters
	 * @see app.owlcms.ui.home.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		QueryParameterReader.super.setParameter(event, parameter);
		location = event.getLocation();
		locationUI = event.getUI();
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsSession.withFop(fop -> {
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}


	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
		params.put("fop",Arrays.asList(OwlcmsSession.getFop().getName()));
		if (newGroup != null && !isIgnoreGroup()) {
			params.put("group",Arrays.asList(newGroup.getName()));
		} else {
			params.remove("group");
		}
		ui.getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}


	/**
	 * if true, the group is shown on the URL and will be restored on a refresh.
	 * if false, the group is not shown on the URL, and will be set to the current group for the FOP
	 * @see app.owlcms.ui.home.QueryParameterReader#isIgnoreGroup()
	 */
	@Override
	public boolean isIgnoreGroup() {
		return false;
	}

	/**
	 * We want to use the same layout class for all navigation screens, so that the framework only
	 * changes the slot where the content is shown.  The content will then update the top bar using
	 * this method.
	 * @param title TODO
	 * @param appLayout
	 */
	protected void configureTopBar(String title, AppLayout appLayout) {
		createTopBar(appLayout);
		configureTopBarTitle(appLayout, title);
		HorizontalLayout fopField = createTopBarFopField("Competition Platform", "Select Platform");
		HorizontalLayout groupField = createTopBarGroupField("Group", "Select Group");
		configureAppBar(appLayout, fopField, groupField);
	}

	public void createTopBar(AppLayout appLayout) {
		HorizontalLayout topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		topBar
		.getElement()
		.getStyle()
		.set("flex", "100 1");
		topBar.setJustifyContentMode(JustifyContentMode.START);
	}

	/**
	 * The left part of the top bar.
	 * @param appLayout
	 * @param topBarTitle
	 */
	protected void configureTopBarTitle(AppLayout appLayout, String topBarTitle) {
		appLayout.getTitleWrapper()
		.getElement()
		.getStyle()
		.set("flex", "0 1 20em");
		Label label = new Label(topBarTitle);
		appLayout.setTitleComponent(label);
	}
	/**
	 * The middle part of the top bar.
	 *
	 * @param appLayout
	 * @param fopField
	 * @param groupField
	 */
	protected void configureAppBar(AppLayout appLayout, HorizontalLayout fopField, HorizontalLayout groupField) {
		HorizontalLayout appBar = new HorizontalLayout();
		if (fopField != null) {
			appBar.add(fopField);
		}
		if (groupField != null) {
			appBar.add(groupField);
		}
		appBar.setSpacing(true);
		appBar.setAlignItems(FlexComponent.Alignment.CENTER);
		appLayout.setAppBar(appBar);
	}

	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		Label fopLabel = new Label(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlayState> fopSelect = createFopSelect(placeHolder);
		OwlcmsSession.withFop((fop1) -> {
			fopSelect.setValue(fop1);
		});
		fopSelect.addValueChangeListener(e -> {
			// by default, we do NOT switch the group -- only the competition group lifting page
			// does by overriding this method.
			OwlcmsSession.setFop(e.getValue());
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}

	protected ComboBox<FieldOfPlayState> createFopSelect(String placeHolder) {
		ComboBox<FieldOfPlayState> fopSelect = new ComboBox<>();
		fopSelect.setPlaceholder(placeHolder);
		fopSelect.setItems(OwlcmsFactory.getFOPs());
		fopSelect.setItemLabelGenerator(FieldOfPlayState::getName);
		fopSelect.setWidth("10rem");
		return fopSelect;
	}

	protected HorizontalLayout createTopBarGroupField(String label, String placeHolder) {
		Label groupLabel = new Label(label);
		formatLabel(groupLabel);

		ComboBox<Group> groupSelect = createGroupSelect(placeHolder);
		OwlcmsSession.withFop((fop) -> {
			groupSelect.setValue(fop.getGroup());
		});
		groupSelect.addValueChangeListener(e -> {
			OwlcmsSession.withFop((fop) -> {
				switchGroup(e.getValue(), fop);
			});
		});

		HorizontalLayout groupField = new HorizontalLayout(groupLabel, groupSelect);
		groupField.setAlignItems(Alignment.CENTER);
		return groupField;
	}

	public ComboBox<Group> createGroupSelect(String placeHolder) {
		ComboBox<Group> groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder(placeHolder);
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setWidth("10rem");
		return groupSelect;
	}

	protected void formatLabel(Label label) {
		label.getStyle().set("font-size", "small");
		label.getStyle().set("text-align", "right");
		label.getStyle().set("width", "12em");
	}

	protected void switchGroup(Group group2,FieldOfPlayState fop) {
		Group group = group2;
		Group currentGroup = fop.getGroup();
		if (group == null) {
			fop.switchGroup(null);
			if (parentLayout.groupSelect != null) {
				parentLayout.groupSelect.setValue(null);
			}
		} else if (!group.equals(currentGroup)) {
			fop.switchGroup(group);
			if (parentLayout.groupSelect != null) {
				parentLayout.groupSelect.setValue(group);
			}
		}
	}

	/* (non-Javadoc)
	 * @see app.owlcms.components.appLayout.AppLayoutContent#getParentLayout()
	 */
	@Override
	public AppLayoutRouterLayout getParentLayout() {
		return parentLayout;
	}

	/* (non-Javadoc)
	 * @see app.owlcms.components.appLayout.AppLayoutContent#setParentLayout(com.github.appreciated.app.layout.router.AppLayoutRouterLayout)
	 */
	@Override
	public void setParentLayout(AppLayoutRouterLayout parentLayout) {
		this.parentLayout = (NavigationLayout) parentLayout;
	}

}
