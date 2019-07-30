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

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSStartingList;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@SuppressWarnings("serial")
public class RegistrationLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(RegistrationLayout.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private HorizontalLayout topBar;
    private ComboBox<Group> gridGroupFilter;
    private AppLayout appLayout;
    private ComboBox<Group> groupSelect;
    private Group group;
    private Anchor cards;
    private Button cardsButton;
    private Anchor startingList;
    private Button startingListButton;

    /*
     * (non-Javadoc)
     * 
     * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.
     * appreciated.app.layout.behaviour.Behaviour)
     */
    @Override
    protected AppLayout getLayoutConfiguration(Behaviour variant) {
        variant = Behaviour.LEFT;
        appLayout = super.getLayoutConfiguration(variant);
        appLayout.closeDrawer();
        this.topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
        createTopBar(topBar);
        appLayout.getTitleWrapper().getElement().getStyle().set("flex", "0 1 0px"); //$NON-NLS-1$ //$NON-NLS-2$
        return appLayout;
    }

    /**
     * The layout is created before the content. This routine has created the
     * content, we can refer to the content using
     * {@link #getLayoutComponentContent()} and the content can refer to us via
     * {@link AppLayoutContent#getParentLayout()}
     * 
     * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
     */
    @Override
    public void showRouterLayoutContent(HasElement content) {
        super.showRouterLayoutContent(content);
        RegistrationContent weighinContent = (RegistrationContent) getLayoutComponentContent();
        gridGroupFilter = weighinContent.getGroupFilter();
    }

    /**
     * Create the top bar.
     * 
     * Note: the top bar is created before the content.
     * 
     * @see #showRouterLayoutContent(HasElement) for how to content to layout and
     *      vice-versa
     * 
     * @param topBar
     */
    protected void createTopBar(HorizontalLayout topBar) {

        H3 title = new H3();
        title.setText(getTranslation("EditRegisteredAthletes")); //$NON-NLS-1$
        title.add();
        title.getStyle().set("margin", "0px 0px 0px 0px") //$NON-NLS-1$ //$NON-NLS-2$
                .set("font-weight", "normal"); //$NON-NLS-1$ //$NON-NLS-2$

        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(getTranslation("Group")); //$NON-NLS-1$
        groupSelect.setItems(GroupRepository.findAll());
        groupSelect.setItemLabelGenerator(Group::getName);

        JXLSCards cardsWriter = new JXLSCards(true);
        StreamResource href1 = new StreamResource("athleteCards.xls", cardsWriter); //$NON-NLS-1$
        cards = new Anchor(href1, ""); //$NON-NLS-1$

        groupSelect.setValue(null);   
        cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);
            cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
        });

        JXLSStartingList startingListWriter = new JXLSStartingList();
        StreamResource href2 = new StreamResource("startingList.xls", startingListWriter); //$NON-NLS-1$
        startingList = new Anchor(href2, ""); //$NON-NLS-1$
        startingListButton = new Button(getTranslation("StartingList"), new Icon(VaadinIcon.DOWNLOAD_ALT)); //$NON-NLS-1$
        startingList.add(startingListButton);
        startingListButton.setEnabled(true);

        Button drawLots = new Button(getTranslation("DrawLotNumbers"), (e) -> { //$NON-NLS-1$
            drawLots();
        });

        Button deleteAthletes = new Button(getTranslation("DeleteAthletes"), (e) -> { //$NON-NLS-1$
            new ConfirmationDialog(getTranslation("DeleteAthletes"), //$NON-NLS-1$
                    getTranslation("Warning_DeleteAthletes"), //$NON-NLS-1$
                    getTranslation("Done_period"), //$NON-NLS-1$
                    () -> {
                        deleteAthletes();
                    }).open();

        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("DeleteAthletes_forListed")); //$NON-NLS-1$ //$NON-NLS-2$

        Button clearLifts = new Button(getTranslation("ClearLifts"), (e) -> { //$NON-NLS-1$
            new ConfirmationDialog(getTranslation("ClearLifts"), //$NON-NLS-1$
                    getTranslation("Warning_ClearAthleteLifts"), //$NON-NLS-1$
                    getTranslation("LiftsCleared"), //$NON-NLS-1$
                    () -> {
                        clearLifts();
                    }).open();
        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("ClearLifts_forListed")); //$NON-NLS-1$ //$NON-NLS-2$

        cardsButton = new Button(getTranslation("AthleteCards"), new Icon(VaadinIcon.DOWNLOAD_ALT)); //$NON-NLS-1$
        cardsButton.addClickListener((e) -> {
            cardsWriter.setGroup(group);
        });
        cards.add(cardsButton);
        cardsButton.setEnabled(true);
        
        HorizontalLayout buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts, startingList, cards);
        buttons.setPadding(true);
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.getElement().getStyle().set("flex", "100 1"); //$NON-NLS-1$ //$NON-NLS-2$
        topBar.removeAll();
        topBar.add(title, groupSelect, buttons);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
    }

    protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
        group = e.getValue();
        gridGroupFilter.setValue(e.getValue());
    }

    protected void errorNotification() {
        Label content = new Label(getTranslation("Select_group_first")); //$NON-NLS-1$
        content.getElement().setAttribute("theme", "error"); //$NON-NLS-1$ //$NON-NLS-2$
        Button buttonInside = new Button(getTranslation("GotIt")); //$NON-NLS-1$
        buttonInside.getElement().setAttribute("theme", "error primary"); //$NON-NLS-1$ //$NON-NLS-2$
        VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
        verticalLayout.setAlignItems(Alignment.CENTER);
        Notification notification = new Notification(verticalLayout);
        notification.setDuration(3000);
        buttonInside.addClickListener(event -> notification.close());
        notification.setPosition(Position.MIDDLE);
        notification.open();
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
            for (Athlete a : athletes) {
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
            for (Athlete a : toBeShuffled) {
                em.merge(a);
            }
            em.flush();
            return null;
        });
        content.refreshCrudGrid();
    }
}
