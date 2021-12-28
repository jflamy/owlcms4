/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.lifting;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.flowingcode.vaadin.addons.ironicons.PlacesIcons;
import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPError;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import app.owlcms.ui.shared.BreakDialog;
import app.owlcms.ui.shared.BreakManagement.CountdownType;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */

@SuppressWarnings("serial")
@Route(value = "lifting/announcer", layout = AthleteGridLayout.class)
@CssImport(value = "./styles/shared-styles.css")
@CssImport(value = "./styles/notification-theme.css", themeFor = "vaadin-notification-card")
@CssImport(value = "./styles/text-field-theme.css", themeFor = "vaadin-text-field")
public class AnnouncerContent extends AthleteGridContent implements HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private HorizontalLayout timerButtons;
    private HorizontalLayout decisionLights;

    private long previousGoodMillis = 0L;
    private long previousBadMillis = 0L;

    public AnnouncerContent() {
        super();
        defineFilters(crudGrid);
        setTopBarTitle(getTranslation("Announcer"));
    }

    /**
     * Use lifting order instead of display order
     *
     * @see app.owlcms.ui.shared.AthleteGridContent#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop != null) {
            logger.trace("{}findAll {} {}", fop.getLoggingName(),
                    fop.getGroup() == null ? null : fop.getGroup().getName(),
                    LoggerUtils.whereFrom());
            final String filterValue;
            if (lastNameFilter.getValue() != null) {
                filterValue = lastNameFilter.getValue().toLowerCase();
                return fop.getLiftingOrder().stream().filter(a -> a.getLastName().toLowerCase().startsWith(filterValue))
                        .collect(Collectors.toList());
            } else {
                return fop.getLiftingOrder();
            }
        } else {
            // no field of play, no group, empty list
            logger.debug("findAll fop==null");
            return ImmutableList.of();
        }
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Announcer") + OwlcmsSession.getFopNameIfMultiple();
    }

    /**
     * The URL contains the group, contrary to other screens.
     *
     * Normally there is only one announcer. If we have to restart the program the announcer screen will have the URL
     * correctly set. if there is no current group in the FOP, the announcer will (exceptionally set it)
     *
     * @see app.owlcms.ui.shared.AthleteGridContent#isIgnoreGroupFromURL()
     */
    @Override
    public boolean isIgnoreGroupFromURL() {
        return false;
    }

    @Subscribe
    public void slaveNotification(UIEvent.Notification e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            Notification n = new Notification();
            n.setText(FOPError.translateMessage(e.getFopStateString(), e.getFopEventString()));
            n.setPosition(Position.MIDDLE);
            n.setDuration(3000);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            n.open();
        });
    }

    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            hideLiveDecisions();

            int d = e.decision ? 1 : 0;
            String text = getTranslation("NoLift_GoodLift", d, e.getAthlete().getFullName());

            Notification n = new Notification();
            String themeName = e.decision ? "success" : "error";
            n.getElement().getThemeList().add(themeName);

            Div label = new Div();
            label.add(text);
            label.addClickListener((event) -> n.close());
            label.setSizeFull();
            label.getStyle().set("font-size", "large");
            n.add(label);
            n.setPosition(Position.TOP_START);
            n.setDuration(5000);
            n.open();
        });
    }

    @Subscribe
    public void slaveStartTime(UIEvent.StartTime e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            buttonsTimeStarted();
            displayLiveDecisions();
        });
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
     */
    @Override
    protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
        createStartTimeButton();
        createStopTimeButton();
        create1minButton();
        create2MinButton();

        timerButtons = new HorizontalLayout(
                startTimeButton, stopTimeButton, _1min, _2min);
        timerButtons.setAlignItems(FlexComponent.Alignment.BASELINE);
        return timerButtons;
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createInitialBar()
     */
    @Override
    protected void createInitialBar() {
        logger.debug("AnnouncerContent creating top bar {}", LoggerUtils.whereFrom());
        topBar = getAppLayout().getAppBarElementWrapper();
        topBar.removeAll();
        initialBar = true;

        createTopBarGroupSelect();
        createTopBarLeft();

        introCountdownButton = new Button(getTranslation("introCountdown"), AvIcons.AV_TIMER.create(),
                (e) -> {
                    OwlcmsSession.withFop(fop -> {
//                        fop.fopEventPost(
//                            new FOPEvent.BreakStarted(BreakType.BEFORE_INTRODUCTION, CountdownType.INDEFINITE, null, null, this));
                        BreakDialog dialog = new BreakDialog(this, BreakType.BEFORE_INTRODUCTION, CountdownType.TARGET);
                        dialog.open();
                    });
                });
        introCountdownButton.getElement().setAttribute("theme", "primary contrast");

        startLiftingButton = new Button(getTranslation("startLifting"), PlacesIcons.FITNESS_CENTER.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                UI.getCurrent().access(() -> createTopBar());
                fop.fopEventPost(new FOPEvent.StartLifting(this));
            });
        });
        startLiftingButton.getThemeNames().add("success primary");

        showResultsButton = new Button(getTranslation("ShowResults"), PlacesIcons.FITNESS_CENTER.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                UI.getCurrent().access(() -> createTopBar());
                fop.fopEventPost(
                        new FOPEvent.BreakStarted(BreakType.GROUP_DONE, CountdownType.INDEFINITE, null, null, this));
            });
        });
        showResultsButton.getThemeNames().add("success primary");
        showResultsButton.setVisible(false);

        warning = new H3();
        warning.getStyle().set("margin-top", "0").set("margin-bottom", "0");
        HorizontalLayout topBarRight = new HorizontalLayout();
        topBarRight.add(warning, introCountdownButton, startLiftingButton, showResultsButton);
        topBarRight.setSpacing(true);
        topBarRight.setPadding(true);
        topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);

        topBar.removeAll();
        topBar.setSizeFull();
        topBar.add(getTopBarLeft(), topBarRight);

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setFlexGrow(0.0, getTopBarLeft());
        topBar.setFlexGrow(1.0, topBarRight);
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createReset()
     */
    @Override
    protected Component createReset() {
        reset = new Button(getTranslation("RefreshList"), IronIcons.REFRESH.create(),
                (e) -> OwlcmsSession.withFop((fop) -> {
                    Group group = fop.getGroup();
                    logger.info("resetting {} from database", group);
                    // fop.loadGroup(group, this, true);
                    fop.fopEventPost(new FOPEvent.SwitchGroup(group, this));
                    syncWithFOP(true); // loadgroup does not refresh grid, true=ask for refresh
                }));

        reset.getElement().setAttribute("title", getTranslation("Reload_group"));
        reset.getElement().setAttribute("theme", "secondary contrast small icon");
        return reset;
    }

    /**
     * Add key shortcuts to parent
     * @see app.owlcms.ui.shared.AthleteGridContent#createStartTimeButton()
     */
    @Override
    protected void createStartTimeButton() {
        super.createStartTimeButton();
        UI.getCurrent().addShortcutListener(() -> doStartTime(), Key.COMMA);
    }
    
    /**
     * Add key shortcuts to parent
     * @see app.owlcms.ui.shared.AthleteGridContent#createStartTimeButton()
     */
    @Override
    protected void createStopTimeButton() {
        super.createStopTimeButton();
        UI.getCurrent().addShortcutListener(() -> doStopTime(), Key.PERIOD);
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
     */
    @Override
    protected void createTopBar() {
        super.createTopBar();
        // this hides the back arrow
        getAppLayout().setMenuVisible(false);
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createTopBarGroupSelect()
     */
    @Override
    protected void createTopBarGroupSelect() {
        // there is already all the SQL filtering logic for the group attached
        // hidden field in the crudGrid part of the page so we just set that
        // filter.
        super.createTopBarGroupSelect();
        topBarGroupSelect.setReadOnly(false);
        // topBarGroupSelect.setWidth("12ch");
        topBarGroupSelect.setClearButtonVisible(true);
        topBarGroupSelect.getStyle().set("--vaadin-combo-box-overlay-width", "40ch");
        OwlcmsSession.withFop((fop) -> {
            Group group = fop.getGroup();
            logger.trace("initial setting group to {} {}", group, LoggerUtils.whereFrom());
            topBarGroupSelect.setValue(group);
            getGroupFilter().setValue(group);
        });
        topBarGroupSelect.addValueChangeListener(e -> {
            Group group = e.getValue();
            logger.trace("##### select setting filter group to {} {}", group, LoggerUtils.whereFrom());
            getGroupFilter().setValue(group);
        });
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
     */
    @Override
    protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
        Button good = new Button(IronIcons.DONE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                long now = System.currentTimeMillis();
                long timeElapsed = now - previousGoodMillis;
                if (timeElapsed > 5000) {
                    // no reason to give two goods within one second...
                    fop.fopEventPost(
                            new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), true, true, true,
                                    true));
                }
                previousGoodMillis = now;
            });
        });
        good.getElement().setAttribute("theme", "success icon");

        Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                long now = System.currentTimeMillis();
                long timeElapsed = now - previousBadMillis;
                if (timeElapsed > 5000) {
                    // no reason to give two goods within one second...
                    fop.fopEventPost(new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), false,
                            false, false, false));
                }
                previousBadMillis = now;
            });
        });
        bad.getElement().setAttribute("theme", "error icon");

        HorizontalLayout decisions = new HorizontalLayout(good, bad);
        return decisions;
    }

    protected void displayLiveDecisions() {
        if (decisionLights == null) {
            getTopBarLeft().removeAll();
            createDecisionLights();
            getTopBarLeft().add(decisionLights);
        }
    }

    @Override
    protected void fillTopBarLeft() {
        super.fillTopBarLeft();
        getTopBarLeft().addClassName("announcerLeft");
        // getTopBarLeft().setWidth("12em");
    }

    private void createDecisionLights() {
        JuryDisplayDecisionElement decisionDisplay = new JuryDisplayDecisionElement();
//        Icon silenceIcon = AvIcons.MIC_OFF.create();
        decisionLights = new HorizontalLayout(decisionDisplay);
        decisionLights.addClassName("announcerLeft");
        // decisionLights.setWidth("12em");
        decisionLights.getStyle().set("line-height", "2em");
    }

    private void hideLiveDecisions() {
        getTopBarLeft().removeAll();
        fillTopBarLeft();
        decisionLights = null;
    }
}
