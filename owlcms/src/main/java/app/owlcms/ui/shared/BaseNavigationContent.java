/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.shared;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
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

import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class NavigationContent
 *
 */
@SuppressWarnings("serial")
public abstract class BaseNavigationContent extends VerticalLayout
implements OwlcmsContent, QueryParameterReader, SafeEventBusRegistration, UIEventProcessor {

	// @SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(BaseNavigationContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName()); //$NON-NLS-1$
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	protected Location location;
	protected UI locationUI;
	protected EventBus uiEventBus;

	/**
	 * Top part content
	 */
	private ComboBox<Group> groupSelect;
	protected OwlcmsRouterLayout routerLayout;

	/**
	 * Instantiates a new announcer content.
	 * Content is created in {@link #setParameter(BeforeEvent, String)} after URL parameters are parsed.
	 */
	public BaseNavigationContent() {
	}

	/**
	 * Process URL parameters, including query parameters
	 * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
	 */
	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		QueryParameterReader.super.setParameter(event, parameter);
		location = event.getLocation();
		locationUI = event.getUI();
	}

	/**
	 * Update URL location.
	 * This method is called when we set the group explicitly via a dropdown.
	 *
	 * @param ui the ui
	 * @param location the location
	 * @param newGroup the new group
	 */
	public void updateURLLocation(UI ui, Location location, Group newGroup) {
		// change the URL to reflect fop group
		HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
		params.put("fop",Arrays.asList(OwlcmsSession.getFop().getName())); //$NON-NLS-1$
		if (newGroup != null && !isIgnoreGroupFromURL()) {
			params.put("group",Arrays.asList(newGroup.getName())); //$NON-NLS-1$
		} else {
			params.remove("group"); //$NON-NLS-1$
		}
		ui.getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(params)));
	}

	/* (non-Javadoc)
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsSession.withFop(fop -> {
			// create the top bar, now that we know the group and fop
			String title = getTitle();
			logger.debug("createTopBar {}",title); //$NON-NLS-1$
			createTopBar(title);
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);

		});
	}

	protected abstract String getTitle();

	/**
	 * The top bar is logically is the master part of a master-detail
	 * In the current implementation, the most convenient place to put it is in the top bar
	 * which is managed by the layout, but this could change. So we change the surrounding layout
	 * from this class.  In this way, only one class (the content) listens for events.
	 * Doing it the other way around would require multiple layouts, which breaks the idea of
	 * a single page app.
	 */
	protected void createTopBar(String title) {
		configureTopBar();
		configureTopBarTitle(title);
		HorizontalLayout fopField = createTopBarFopField(getTranslation("BaseNavigationContent.0"), getTranslation("BaseNavigationContent.1")); //$NON-NLS-1$ //$NON-NLS-2$
		createAppBar(fopField, null); //, groupField
	}

	public void configureTopBar() {
		HorizontalLayout topBar = getAppLayout().getAppBarElementWrapper();
		topBar.setSizeFull();
		topBar.setJustifyContentMode(JustifyContentMode.START);
	}

	/**
	 * The left part of the top bar.
	 * @param topBarTitle
	 * @param appLayoutComponent
	 */
	protected void configureTopBarTitle(String topBarTitle) {
		AbstractLeftAppLayoutBase appLayout = (AbstractLeftAppLayoutBase) getRouterLayout().getAppLayout();
		appLayout.getTitleWrapper().getElement()
		.getStyle()
		.set("flex", "0 1 20em"); //$NON-NLS-1$ //$NON-NLS-2$
		Label label = new Label(topBarTitle);
		appLayout.setTitleComponent(label);
	}
	/**
	 * The middle part of the top bar.
	 * @param fopField
	 * @param groupField
	 * @param appLayoutComponent
	 */
	protected void createAppBar(HorizontalLayout fopField, HorizontalLayout groupField) {
		HorizontalLayout appBar = new HorizontalLayout();
		if (fopField != null) {
			appBar.add(fopField);
		}
		if (groupField != null) {
			appBar.add(groupField);
		}
		appBar.setSpacing(true);
		appBar.setAlignItems(FlexComponent.Alignment.CENTER);
		getAppLayout().setAppBar(appBar);
	}

	protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
		Label fopLabel = new Label(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
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

	protected ComboBox<FieldOfPlay> createFopSelect(String placeHolder) {
		ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
		fopSelect.setPlaceholder(placeHolder);
		fopSelect.setItems(OwlcmsFactory.getFOPs());
		fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
		fopSelect.setWidth("10rem"); //$NON-NLS-1$
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
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder(placeHolder);
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setWidth("10rem"); //$NON-NLS-1$
		return groupSelect;
	}

	protected void formatLabel(Label label) {
		label.getStyle().set("font-size", "small"); //$NON-NLS-1$ //$NON-NLS-2$
		label.getStyle().set("text-align", "right"); //$NON-NLS-1$ //$NON-NLS-2$
		label.getStyle().set("width", "12em"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	protected void switchGroup(Group group2,FieldOfPlay fop) {
		Group group = group2;
		Group currentGroup = fop.getGroup();
		if (group == null) {
			fop.switchGroup(null, this.getOrigin());
			if (groupSelect != null) {
				groupSelect.setValue(null);
			}
		} else if (!group.equals(currentGroup)) {
			fop.switchGroup(group, this.getOrigin());
			if (groupSelect != null) {
				groupSelect.setValue(group);
			}
		}
	}

	protected Object getOrigin() {
		return this;
	}
	
	@Override
	final public OwlcmsRouterLayout getRouterLayout() {
		return routerLayout;
	}

	@Override
	final public void setRouterLayout(OwlcmsRouterLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
