/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.component.applayout.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
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
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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

    private FlexLayout topBar;
    private ComboBox<Group> gridGroupFilter;
    private AppLayout appLayout;
    private ComboBox<Group> groupSelect;
    private Group group;
    private Anchor cards;
    private Button cardsButton;
    private Anchor startingList;
    private Button startingListButton;

    /**
     * The layout is created before the content. This routine has created the content, we can refer to the content using
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
     * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
     *
     * @param topBar
     */
    protected void createTopBar(FlexLayout topBar) {

        H3 title = new H3();
        title.setText(getTranslation("EditRegisteredAthletes"));
        title.add();
        title.getStyle().set("margin", "0px 0px 0px 0px").set("font-weight", "normal");

        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(getTranslation("Group"));
        groupSelect.setItems(GroupRepository.findAll());
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setClearButtonVisible(true);

        JXLSCards cardsWriter = new JXLSCards(true, UI.getCurrent());
        StreamResource href1 = new StreamResource("athleteCards.xls", cardsWriter);
        cards = new Anchor(href1, "");

        groupSelect.setValue(null);
        cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls");
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);
            cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls");
        });

        JXLSStartingList startingListWriter = new JXLSStartingList(UI.getCurrent());
        StreamResource href2 = new StreamResource("startingList.xls", startingListWriter);
        startingList = new Anchor(href2, "");
        startingListButton = new Button(getTranslation("StartingList"), new Icon(VaadinIcon.DOWNLOAD_ALT));
        startingList.add(startingListButton);
        startingListButton.setEnabled(true);

        Button drawLots = new Button(getTranslation("DrawLotNumbers"), (e) -> {
            drawLots();
        });

        Button deleteAthletes = new Button(getTranslation("DeleteAthletes"), (e) -> {
            new ConfirmationDialog(getTranslation("DeleteAthletes"), getTranslation("Warning_DeleteAthletes"),
                    getTranslation("Done_period"), () -> {
                        deleteAthletes();
                    }).open();

        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("DeleteAthletes_forListed"));

        Button clearLifts = new Button(getTranslation("ClearLifts"), (e) -> {
            new ConfirmationDialog(getTranslation("ClearLifts"), getTranslation("Warning_ClearAthleteLifts"),
                    getTranslation("LiftsCleared"), () -> {
                        clearLifts();
                    }).open();
        });
        deleteAthletes.getElement().setAttribute("title", getTranslation("ClearLifts_forListed"));

        cardsButton = new Button(getTranslation("AthleteCards"), new Icon(VaadinIcon.DOWNLOAD_ALT));
        cardsButton.addClickListener((e) -> {
            cardsWriter.setGroup(group);
        });
        cards.add(cardsButton);
        cardsButton.setEnabled(true);

        Button resetCats = new Button(getTranslation("ResetCategories.ResetAthletes"), (e) -> {
            new ConfirmationDialog(
                    getTranslation("ResetCategories.ResetCategories"),
                    getTranslation("ResetCategories.Warning_ResetCategories"),
                    getTranslation("ResetCategories.CategoriesReset"), () -> {
                        resetCategories();
                    }).open();
        });
        resetCats.getElement().setAttribute("title", getTranslation("ResetCategories.ResetCategoriesMouseOver"));

        HorizontalLayout buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts, startingList, cards,
                resetCats);
        buttons.setPadding(true);
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.getStyle().set("flex", "100 1");
        topBar.removeAll();
        topBar.add(title, groupSelect, buttons);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
    }

    protected void errorNotification() {
        Label content = new Label(getTranslation("Select_group_first"));
        content.getElement().setAttribute("theme", "error");
        Button buttonInside = new Button(getTranslation("GotIt"));
        buttonInside.getElement().setAttribute("theme", "error primary");
        VerticalLayout verticalLayout = new VerticalLayout(content, buttonInside);
        verticalLayout.setAlignItems(Alignment.CENTER);
        Notification notification = new Notification(verticalLayout);
        notification.setDuration(3000);
        buttonInside.addClickListener(event -> notification.close());
        notification.setPosition(Position.MIDDLE);
        notification.open();
    }

    /*
     * (non-Javadoc)
     *
     * @see app.owlcms.ui.home.OwlcmsRouterLayout#getLayoutConfiguration(com.github.
     * appreciated.app.layout.behaviour.Behaviour)
     */
    @Override
    protected AppLayout getLayoutConfiguration(Class<? extends AppLayout> variant) {
        variant = LeftLayouts.Left.class;
        appLayout = super.getLayoutConfiguration(variant);
        appLayout.closeDrawer();
        this.topBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
        createTopBar(topBar);
        appLayout.getTitleWrapper().getStyle().set("flex", "0 1 0px");
        return appLayout;
    }

    protected void setContentGroup(ComponentValueChangeEvent<ComboBox<Group>, Group> e) {
        group = e.getValue();
        gridGroupFilter.setValue(e.getValue());
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

    private void resetCategories() {
        RegistrationContent content = (RegistrationContent) getLayoutComponentContent();
        AthleteRepository.resetParticipations();
        content.refreshCrudGrid();
    }
}
