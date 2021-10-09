/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
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

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.init.OwlcmsSession;
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
    private Anchor startingWeights;
    private Button startingWeightsButton;
    private Anchor cards;
    private Button cardsButton;
    private Anchor jury;
    private Button juryButton;

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

        JXLSWeighInSheet startingWeightsWriter = new JXLSWeighInSheet(true, UI.getCurrent());
        StreamResource href = new StreamResource("startingWeights.xls", startingWeightsWriter);
        startingWeights = new Anchor(href, "");

        JXLSCards cardsWriter = new JXLSCards(true, UI.getCurrent());
        StreamResource href1 = new StreamResource("athleteCards.xls", cardsWriter);
        cards = new Anchor(href1, "");

        JXLSJurySheet juryWriter = new JXLSJurySheet(UI.getCurrent());
        StreamResource href2 = new StreamResource("jury.xls", juryWriter);
        jury = new Anchor(href2, "");

        OwlcmsSession.withFop((fop) -> {
            groupSelect.setValue(null);
            startingWeights.getElement().setAttribute("download",
                    "startingWeights" + (group != null ? "_" + group : "_all") + ".xls");
            cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls");
        });
        groupSelect.addValueChangeListener(e -> {
            setContentGroup(e);

            cardsWriter.setGroup(e.getValue());
            startingWeightsWriter.setGroup(e.getValue());
            juryWriter.setGroup(e.getValue());

            startingWeightsButton.setEnabled(e.getValue() != null);
            juryButton.setEnabled(e.getValue() != null);
            startingWeights.getElement().setAttribute("download",
                    "startingWeights" + (group != null ? "_" + group : "_all") + ".xls");
            cards.getElement().setAttribute("download", "cards" + (group != null ? "_" + group : "_all") + ".xls");
            jury.getElement().setAttribute("download", "jury" + (group != null ? "_" + group : "_all") + ".xls");
        });

        Button start = new Button(getTranslation("GenerateStartNumbers"), (e) -> {
            generateStartNumbers();
        });
        Button clear = new Button(getTranslation("ClearStartNumbers"), (e) -> {
            clearStartNumbers();
        });

        String startingWeightsSheetTranslation = getTranslation("StartingWeightsSheet");
        startingWeightsButton = new Button(startingWeightsSheetTranslation, new Icon(VaadinIcon.DOWNLOAD_ALT));
        startingWeights.add(startingWeightsButton);

        cardsButton = new Button(getTranslation("AthleteCards"), new Icon(VaadinIcon.DOWNLOAD_ALT));
        cards.add(cardsButton);
        cardsButton.setEnabled(true);

        juryButton = new Button(getTranslation("Jury"), new Icon(VaadinIcon.DOWNLOAD_ALT));
        jury.add(juryButton);
        juryButton.setEnabled(true);

        HorizontalLayout buttons = new HorizontalLayout(start, clear, startingWeights, cards, jury);
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
