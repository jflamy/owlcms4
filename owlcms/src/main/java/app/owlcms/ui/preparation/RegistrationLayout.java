/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.preparation;

import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.component.applayout.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.component.applayout.AppLayout;
import com.github.appreciated.app.layout.component.applayout.LeftLayouts;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import app.owlcms.components.ConfirmationDialog;
import app.owlcms.components.DownloadButtonFactory;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSStartingList;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.utils.NaturalOrderComparator;
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

    private AppLayout appLayout;
    private Button cardsButton;
    private ComboBox<Group> gridGroupFilter;
    private Group group;
    private ComboBox<Group> groupSelect;
    private Button startingListButton;
    private FlexLayout topBar;

    /**
     * @return the groupSelect
     */
    public ComboBox<Group> getGroupSelect() {
        return groupSelect;
    }

    /**
     * @param groupSelect the groupSelect to set
     */
    public void setGroupSelect(ComboBox<Group> groupSelect) {
        this.groupSelect = groupSelect;
    }

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
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        groupSelect.setItems(groups);
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setClearButtonVisible(true);

        groupSelect.setValue(null);
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);
        });

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

        JXLSCards cardsWriter = new JXLSCards();
        JXLSStartingList startingListWriter = new JXLSStartingList();

        cardsButton = createCardsButton(cardsWriter);
        startingListButton = createStartingListButton(startingListWriter);

        Button resetCats = new Button(getTranslation("ResetCategories.ResetAthletes"), (e) -> {
            new ConfirmationDialog(
                    getTranslation("ResetCategories.ResetCategories"),
                    getTranslation("ResetCategories.Warning_ResetCategories"),
                    getTranslation("ResetCategories.CategoriesReset"), () -> {
                        resetCategories();
                    }).open();
        });
        resetCats.getElement().setAttribute("title", getTranslation("ResetCategories.ResetCategoriesMouseOver"));

        HorizontalLayout buttons;
        if (Config.getCurrent().featureSwitch("preCompDocs", true)) {
            buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts,
                    resetCats);
        } else {
            buttons = new HorizontalLayout(drawLots, deleteAthletes, clearLifts, startingListButton,
                    cardsButton,
                    resetCats);
        }

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

    private Button createCardsButton(JXLSCards cardsWriter) {
        String resourceDirectoryLocation = "/templates/cards";
        String title = Translator.translate("AthleteCards");
        String downloadedFilePrefix = "cards";
        DownloadButtonFactory cardsButtonFactory = new DownloadButtonFactory(
                () -> {
                    JXLSCards rs = new JXLSCards();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedCardsTemplateFileName,
                Competition::setCardsTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return cardsButtonFactory.createTopBarDownloadButton();
    }

    private Button createStartingListButton(JXLSStartingList startingListWriter) {
        String resourceDirectoryLocation = "/templates/start";
        String title = Translator.translate("StartingList");
        String downloadedFilePrefix = "startingList";

        DownloadButtonFactory startingListFactory = new DownloadButtonFactory(
                () -> {
                    JXLSStartingList rs = new JXLSStartingList();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedStartListTemplateFileName,
                Competition::setStartListTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return startingListFactory.createTopBarDownloadButton();
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
