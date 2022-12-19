/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.lifting;

import java.util.Comparator;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;

import app.owlcms.components.DownloadButtonFactory;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.spreadsheet.JXLSCards;
import app.owlcms.spreadsheet.JXLSJurySheet;
import app.owlcms.spreadsheet.JXLSWeighInSheet;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Weigh-in page -- top bar.
 */
@Push
@SuppressWarnings("serial")
public class WeighinLayout extends OwlcmsLayout implements SafeEventBusRegistration, UIEventProcessor {

    private final static Logger logger = (Logger) LoggerFactory.getLogger(WeighinLayout.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private Button cardsButton;
    private ComboBox<Group> gridGroupFilter;
    private Group group;
    private ComboBox<Group> groupSelect;
    private Button juryButton;
    private Button startingWeightsButton;
    
    public ComboBox<Group> getGroupSelect() {
        return groupSelect;
    }


    @Override
    public void showRouterLayoutContent(HasElement content) {
        super.showRouterLayoutContent(content);
        WeighinContent weighinContent = (WeighinContent) getContent();
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
     * @return 
     */
    @Override
    protected FlexLayout createButtonArea() {

//        H3 title = new H3();
//        title.setText(getTranslation("WeighIn"));
//        title.add();
//        title.getStyle().set("margin", "0px 0px 0px 0px").set("font-weight", "normal");

        groupSelect = new ComboBox<>();
        groupSelect.setPlaceholder(getTranslation("Group"));
        List<Group> groups = GroupRepository.findAll();
        groups.sort((Comparator<Group>) new NaturalOrderComparator<Group>());
        groupSelect.setItems(groups);
        groupSelect.setItemLabelGenerator(Group::getName);
        groupSelect.setClearButtonVisible(true);
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);
        });

        JXLSCards cardsWriter = new JXLSCards();
        JXLSJurySheet juryWriter = new JXLSJurySheet();
        cardsButton = createCardsButton(cardsWriter);
        startingWeightsButton = createStartingWeightsButton();
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

        FlexLayout topBar = new FlexLayout();
        topBar .getElement().getStyle().set("flex", "100 1");
        topBar.removeAll();
        topBar.add(groupSelect, buttons);
        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        
        return topBar;
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
        ((WeighinContent) getContent()).refresh();
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

    private Button createJuryButton(JXLSJurySheet juryWriter) {
        String resourceDirectoryLocation = "/templates/jury";
        String title = Translator.translate("Jury");
        String downloadedFilePrefix = "jury";

        DownloadButtonFactory juryButton = new DownloadButtonFactory(
                () -> {
                    JXLSJurySheet rs = new JXLSJurySheet();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
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

    private Button createStartingWeightsButton() {
        String resourceDirectoryLocation = "/templates/weighin";
        String title = Translator.translate("StartingWeightsSheet");
        String downloadedFilePrefix = "startingWeights";

        DownloadButtonFactory startingWeightsButton = new DownloadButtonFactory(
                () -> {
                    JXLSWeighInSheet rs = new JXLSWeighInSheet();
                    // group may have been edited since the page was loaded
                    rs.setGroup(group != null ? GroupRepository.getById(group.getId()) : null);
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
        ((WeighinContent) getContent()).refresh();
    }
}
