/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.lifting;

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

import app.owlcms.components.DownloadButtonFactory;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.ui.shared.OwlcmsRouterLayout;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@SuppressWarnings("serial")
public class WeighinLayout extends OwlcmsRouterLayout implements SafeEventBusRegistration, UIEventProcessor {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(WeighinLayout.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private FlexLayout topBar;
    private ComboBox<Group> gridGroupFilter;
    private AppLayout appLayout;
    private ComboBox<Group> groupSelect;
    private Group group;
    private Button startingWeightsButton;
    private Button cardsButton;
    private Button juryButton;

    public ComboBox<Group> getGroupSelect() {
        return groupSelect;
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
        WeighinContent weighinContent = (WeighinContent) getLayoutComponentContent();
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
        title.setText(getTranslation("WeighIn"));
        title.add();
        title.getStyle().set("margin", "0px 0px 0px 0px").set("font-weight", "normal");

        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(getTranslation("Group"));
        groupSelect.setItems(GroupRepository.findAll());
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setClearButtonVisible(true);
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);
        });

        JXLSCards cardsWriter = new JXLSCards();
        JXLSJurySheet juryWriter = new JXLSJurySheet();
        JXLSWeighInSheet startingWeightsWriter = new JXLSWeighInSheet();

        cardsButton = createCardsButton(cardsWriter);
        startingWeightsButton = createStartingWeightsButton(startingWeightsWriter);
        juryButton = createJuryButton(juryWriter);

        Button start = new Button(getTranslation("GenerateStartNumbers"), (e) -> {
            generateStartNumbers();
        });
        Button clear = new Button(getTranslation("ClearStartNumbers"), (e) -> {
            clearStartNumbers();
        });

        HorizontalLayout buttons = new HorizontalLayout(start, clear, startingWeightsButton, cardsButton, juryButton);
        buttons.setPadding(true);
        buttons.setSpacing(true);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.getElement().getStyle().set("flex", "100 1");
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
        appLayout.getTitleWrapper().getElement().getStyle().set("flex", "0 1 0px");
        return appLayout;
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
            List<Athlete> currentGroupAthletes = AthleteRepository.doFindAllByGroupAndWeighIn(em, group, null,
                    (Gender) null);
            for (Athlete a : currentGroupAthletes) {
                a.setStartNumber(0);
            }
            return currentGroupAthletes;
        });
        ((WeighinContent) getLayoutComponentContent()).refresh();
    }

    private Button createCardsButton(JXLSCards cardsWriter) {
        String resourceDirectoryLocation = "/templates/cards";
        String title = Translator.translate("AthleteCards");
        String downloadedFilePrefix = "cards";
        DownloadButtonFactory cardsButtonFactory = new DownloadButtonFactory(cardsWriter,
                () -> {
                    JXLSCards rs = new JXLSCards();
                    rs.setGroup(group);
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

    private Button createJuryButton(JXLSJurySheet juryWriter) {
        String resourceDirectoryLocation = "/templates/jury";
        String title = Translator.translate("Jury");
        String downloadedFilePrefix = "jury";

        DownloadButtonFactory juryButton = new DownloadButtonFactory(juryWriter,
                () -> {
                    JXLSJurySheet rs = new JXLSJurySheet();
                    rs.setGroup(group);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedJuryTemplateFileName,
                Competition::setJuryTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return juryButton.createTopBarDownloadButton();
    }

    private Button createStartingWeightsButton(JXLSWeighInSheet weighinListWriter) {
        String resourceDirectoryLocation = "/templates/weighin";
        String title = Translator.translate("StartingWeightsSheet");
        String downloadedFilePrefix = "startingWeights";

        DownloadButtonFactory startingWeightsButton = new DownloadButtonFactory(weighinListWriter,
                () -> {
                    JXLSWeighInSheet rs = new JXLSWeighInSheet();
                    rs.setGroup(group);
                    return rs;
                },
                resourceDirectoryLocation,
                Competition::getComputedStartingWeightsSheetTemplateFileName,
                Competition::setStartingWeightsSheetTemplateFileName,
                title,
                downloadedFilePrefix,
                Translator.translate("Download"));
        return startingWeightsButton.createTopBarDownloadButton();
    }

    private void generateStartNumbers() {
        Group group = groupSelect.getValue();
        if (group == null) {
            errorNotification();
            return;
        }
        AthleteRepository.assignStartNumbers(group);
        ((WeighinContent) getLayoutComponentContent()).refresh();
    }
}
