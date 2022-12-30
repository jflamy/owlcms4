/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.shared;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
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

    public ComboBox<Group> createGroupSelect(String placeHolder) {
        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(placeHolder);
        List<Group> groups = GroupRepository.findAll();
        groups.sort(new NaturalOrderComparator<Group>());
        groupSelect.setItems(groups);
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setWidth("10rem");
        return groupSelect;
    }

    /**
     * The top bar is logically is the master part of a master-detail In the current implementation, the most convenient
     * place to put it is in the top bar which is managed by the layout, but this could change. So we change the
     * surrounding layout from this class. In this way, only one class (the content) listens for events. Doing it the
     * other way around would require multiple layouts, which breaks the idea of a single page app.
     *
     * @return
     */
    @Override
    public FlexLayout createMenuArea() {
        HorizontalLayout fopField = createMenuBarFopField(getTranslation("CompetitionPlatform"),
                getTranslation("SelectPlatform"));
        HorizontalLayout menu = new HorizontalLayout();
        menu.setSizeFull();
        if (fopField != null) {
            menu.add(fopField);
        }
        Div spacer = new Div();
        spacer.setSizeFull();

        menu.add(spacer);
        menu.setSpacing(true);
        menu.setAlignItems(FlexComponent.Alignment.CENTER);
        menu.setFlexGrow(1.0, spacer);
        FlexLayout fl = new FlexLayout();
        fl.add(menu);
        fl.setFlexGrow(1.0, menu);
        return fl;
    }

    @Override
    final public OwlcmsLayout getRouterLayout() {
        return routerLayout;
    }

    @Override
    public void setHeaderContent() {
        Label label = new Label(getMenuTitle());
        label.getStyle().set("font-size", "var(--lumo-font-size-xl");
        Image image = new Image("icons/owlcms.png", "owlcms icon");
        image.getStyle().set("height", "7ex");
        image.getStyle().set("width", "auto");
        HorizontalLayout topBarTitle = new HorizontalLayout(image, label);
        topBarTitle.setAlignSelf(Alignment.CENTER, label);
        routerLayout.setMenuTitle(topBarTitle);
        routerLayout.setMenuArea(createMenuArea());
        routerLayout.showLocaleDropdown(true);
        routerLayout.setDrawerOpened(true);
        routerLayout.updateHeader(true);
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
        ui.getPage().getHistory().replaceState(null, new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(params))));
    }

    protected ComboBox<FieldOfPlay> createFopSelect(String placeHolder) {
        ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
        fopSelect.setPlaceholder(placeHolder);
        fopSelect.setItems(OwlcmsFactory.getFOPs());
        fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
        fopSelect.setWidth("10rem");
        return fopSelect;
    }

    protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
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
        OwlcmsSession.withFop(fop -> {
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

}
