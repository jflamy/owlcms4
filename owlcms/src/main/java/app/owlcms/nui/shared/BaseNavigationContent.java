/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.shared;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.NaturalOrderComparator;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class NavigationContent
 *
 */
@SuppressWarnings("serial")
public abstract class BaseNavigationContent extends VerticalLayout
        implements OwlcmsContent, FOPParameters, SafeEventBusRegistration, UIEventProcessor {

    // @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(BaseNavigationContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    protected Location location;
    protected UI locationUI;
    protected OwlcmsLayout routerLayout;

    protected EventBus uiEventBus;
    /**
     * Top part content
     */
    private ComboBox<Group> groupSelect;

    /**
     * Instantiates a new announcer content. Content is created in {@link #setParameter(BeforeEvent, String)} after URL
     * parameters are parsed.
     */
    public BaseNavigationContent() {
    }

    public void configureTopBar() {
    }

    public void setHeaderContent() {
        routerLayout.setTopBarTitle(getPageTitle());
        routerLayout.showLocaleDropdown(true);
        routerLayout.setDrawerOpened(false);
    }

    public ComboBox<Group> createGroupSelect(String placeHolder) {
        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(placeHolder);
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        groupSelect.setItems(groups);
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setWidth("10rem");
        return groupSelect;
    }

    @Override
    final public OwlcmsLayout getRouterLayout() {
        return routerLayout;
    }

    /**
     * Process URL parameters, including query parameters
     *
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        FOPParameters.super.setParameter(event, parameter);
        location = event.getLocation();
        locationUI = event.getUI();
    }

    @Override
    final public void setRouterLayout(OwlcmsLayout routerLayout) {
        logger.warn("**** setting router layout {}", routerLayout);
        this.routerLayout = routerLayout;
    }

    /**
     * Update URL location. This method is called when we set the group explicitly via a dropdown.
     *
     * @param ui       the ui
     * @param location the location
     * @param newGroup the new group
     */
    public void updateURLLocation(UI ui, Location location, Group newGroup) {
        // change the URL to reflect fop group
        HashMap<String, List<String>> params = new HashMap<>(location.getQueryParameters().getParameters());
        params.put("fop", Arrays.asList(URLUtils.urlEncode(OwlcmsSession.getFop().getName())));
        if (newGroup != null && !isIgnoreGroupFromURL()) {
            params.put("group", Arrays.asList(URLUtils.urlEncode(newGroup.getName())));
        } else {
            params.remove("group");
        }
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(params)));
    }

    /**
     * The left part of the top bar.
     *
     * @param topBarTitle
     * @param appLayoutComponent
     */
    protected void configureTopBarTitle(String topBarTitle) {
        getAppLayout().setTopBarTitle(topBarTitle);
    }

    /**
     * The middle part of the top bar.
     *
     * @param fopField
     * @param groupField
     * @param appLayoutComponent
     */
    protected FlexLayout createAppBar(HorizontalLayout fopField, HorizontalLayout groupField) {
        HorizontalLayout appBar = new HorizontalLayout();
        appBar.setSizeFull();
        if (fopField != null) {
            appBar.add(fopField);
        }
        if (groupField != null) {
            appBar.add(groupField);
        }
        Div spacer = new Div();
        spacer.setSizeFull();
        appBar.add(spacer);
        
        appBar.setSpacing(true);
        appBar.setAlignItems(FlexComponent.Alignment.CENTER);
        appBar.setFlexGrow(1.0, spacer);
        FlexLayout appBarElementWrapper = getAppLayout().getButtonArea();
        appBarElementWrapper.removeAll();
        appBarElementWrapper.add(appBar);
        appBarElementWrapper.setFlexGrow(1.0, appBar);
        return appBarElementWrapper;
    }

    protected ComboBox<FieldOfPlay> createFopSelect(String placeHolder) {
        ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
        fopSelect.setPlaceholder(placeHolder);
        fopSelect.setItems(OwlcmsFactory.getFOPs());
        fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
        fopSelect.setWidth("10rem");
        return fopSelect;
    }

    /**
     * The top bar is logically is the master part of a master-detail In the current implementation, the most convenient
     * place to put it is in the top bar which is managed by the layout, but this could change. So we change the
     * surrounding layout from this class. In this way, only one class (the content) listens for events. Doing it the
     * other way around would require multiple layouts, which breaks the idea of a single page app.
     * @return 
     */
    public FlexLayout createTopBar(String title) {
        configureTopBar();
        configureTopBarTitle(title);
        HorizontalLayout fopField = createTopBarFopField(getTranslation("CompetitionPlatform"),
                getTranslation("SelectPlatform"));
        FlexLayout fl = createAppBar(fopField, null); // , groupField
        return fl;
    }

    protected HorizontalLayout createTopBarFopField(String label, String placeHolder) {
        Label fopLabel = new Label(label);
        formatLabel(fopLabel);

        ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
        OwlcmsSession.withFop((fop1) -> {
            fopSelect.setValue(fop1);
        });
        fopSelect.addValueChangeListener(e -> {
            // by default, we do NOT switch the group -- only the competition group lifting
            // page
            // does by overriding this method.
            OwlcmsSession.setFop(e.getValue());
        });

        HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
        fopField.setAlignItems(Alignment.CENTER);
        return fopField;
    }

    protected void formatLabel(Label label) {
        label.getStyle().set("font-size", "small");
        label.getStyle().set("text-align", "right");
        label.getStyle().set("width", "12em");
    }

    protected Object getOrigin() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component. AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        logger.warn("***** base navigation content onAttach\n{}", LoggerUtils.stackTrace());
        OwlcmsSession.withFop(fop -> {
            // create the top bar, now that we know the group and fop
            String title = getPageTitle();
            logger.debug("createTopBar {}", title);
            createTopBar(title);
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);

        });
    }

}
