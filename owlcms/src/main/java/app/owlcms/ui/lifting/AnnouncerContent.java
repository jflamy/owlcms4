/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.AvIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */

@SuppressWarnings("serial")
@Route(value = "lifting/announcer", layout = AthleteGridLayout.class)
public class AnnouncerContent extends AthleteGridContent implements HasDynamicTitle {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(AnnouncerContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private Button introCountdownButton;
    private Button startLiftingButton;

    public AnnouncerContent() {
        super();
        defineFilters(crudGrid);
        setTopBarTitle(getTranslation("Announcer"));
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createReset()
     */
    @Override
    protected Component createReset() {
        reset = new Button(IronIcons.REFRESH.create(), (e) -> OwlcmsSession.withFop((fop) -> {
            Group group = fop.getGroup();
            logger.debug("resetting {} from database", group);
            fop.loadGroup(group, this);
            syncWithFOP(false); // no need to reload grid, we just loaded.
        }));
        reset.getElement().setAttribute("title", getTranslation("Reload_group"));
        reset.getElement().setAttribute("theme", "secondary contrast small icon");
        return reset;
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
        OwlcmsSession.withFop((fop) -> {
            Group group = fop.getGroup();
            logger.trace("initial setting group to {} {}", group, LoggerUtils.whereFrom());
            topBarGroupSelect.setValue(group);
            getGroupFilter().setValue(group);
        });
        topBarGroupSelect.addValueChangeListener(e -> {
            Group group = e.getValue();
            logger.trace("select setting filter group to {}", group);
            getGroupFilter().setValue(group);
        });
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Announcer");
    }

    /**
     * The URL contains the group, contrary to other screens.
     *
     * Normally there is only one announcer. If we have to restart the program the
     * announcer screen will have the URL correctly set. if there is no current
     * group in the FOP, the announcer will (exceptionally set it)
     *
     * @see app.owlcms.ui.shared.AthleteGridContent#isIgnoreGroupFromURL()
     */
    @Override
    public boolean isIgnoreGroupFromURL() {
        return false;
    }

    @Subscribe
    public void slaveRefereeDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccess(this, uiEventBus, e, () -> {
            int d = e.decision ? 1 : 0;
            String text = getTranslation("NoLift_GoodLift", d, e.getAthlete().getFullName());

            Notification n = new Notification();
            // Notification theme styling is done in
            // META-INF/resources/frontend/styles/shared-styles.html
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

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.FlexLayout)
     */
    @Override
    protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
        Button start = new Button(AvIcons.PLAY_ARROW.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.TimeStarted(this.getOrigin()));
            });
        });
        start.getElement().setAttribute("theme", "primary icon");
        Button stop = new Button(AvIcons.PAUSE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.TimeStopped(this.getOrigin()));
            });
        });
        stop.getElement().setAttribute("theme", "primary icon");
        Button _1min = new Button("1:00", (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.ForceTime(60000, this.getOrigin()));
            });
        });
        _1min.getElement().setAttribute("theme", "icon");
        Button _2min = new Button("2:00", (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.ForceTime(120000, this.getOrigin()));
            });
        });
        _2min.getElement().setAttribute("theme", "icon");
        Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
            (new BreakDialog(this)).open();
        });
        breakButton.getElement().setAttribute("theme", "icon");
        breakButton.getElement().setAttribute("title", getTranslation("Countdown_BreakTimer"));

        HorizontalLayout buttons = new HorizontalLayout(
                // announce,
                start, stop, _1min, _2min, breakButton);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
        return buttons;
    }

    /**
     * @see app.owlcms.ui.shared.AthleteGridContent#createInitialBar()
     */
    @Override
    protected void createInitialBar() {
        logger.debug("AnnouncerContent creating top bar");
        topBar = getAppLayout().getAppBarElementWrapper();
        topBar.removeAll();

        createTopBarGroupSelect();
        HorizontalLayout topBarLeft = createTopBarLeft();

        introCountdownButton = new Button(getTranslation("introCountdown"), (e) -> {
            Notification.show("countdown dialog");
        });
        startLiftingButton = new Button(getTranslation("startLifting"), (e) -> {
            OwlcmsSession.withFop(fop -> {
                createTopBar();
                fop.getFopEventBus().post(new FOPEvent.StartLifting(this));
            });
        });
        warning = new H3();
        warning.getStyle().set("margin-top", "0").set("margin-bottom", "0");
        HorizontalLayout topBarRight = new HorizontalLayout();
        topBarRight.add(warning, introCountdownButton, startLiftingButton);
        topBarRight.setSpacing(true);
        topBarRight.setPadding(true);
        topBarRight.setAlignItems(FlexComponent.Alignment.CENTER);

        topBar.removeAll();
        topBar.setSizeFull();
        topBar.add(topBarLeft, topBarRight);

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setFlexGrow(0.0, topBarLeft);
        topBar.setFlexGrow(1.0, topBarRight);
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
     * @see app.owlcms.ui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
     */
    @Override
    protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
        Button good = new Button(IronIcons.DONE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(
                        new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), true, true, true, true));
            });
        });
        good.getElement().setAttribute("theme", "success icon");

        Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
            OwlcmsSession.withFop(fop -> {
                fop.getFopEventBus().post(new FOPEvent.ExplicitDecision(fop.getCurAthlete(), this.getOrigin(), false,
                        false, false, false));
            });
        });
        bad.getElement().setAttribute("theme", "error icon");

        HorizontalLayout decisions = new HorizontalLayout(good, bad);
        return decisions;
    }

    @Override
    protected void warn(Group group, String string) {
        String text = group == null ? "\u2013" : string;
        if (topBarPresent) {
            lastName.setText(text);
            firstName.setText("");
            timeField.getElement().getStyle().set("visibility", "hidden");
            attempt.setText("");
            weight.setText("");
            warning.setText(string);
        } else {
            introCountdownButton.setEnabled(false);
            startLiftingButton.setEnabled(false);
            warning.setText(string);
        }
    }

}
