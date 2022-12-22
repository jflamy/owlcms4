/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.AthleteTimerElement;
import app.owlcms.components.elements.JuryDisplayDecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "nlifting/marshall", layout = OwlcmsLayout.class)
public class MarshallContent extends AthleteGridContent implements HasDynamicTitle {

    // @SuppressWarnings("unused")
    final private static Logger logger = (Logger) LoggerFactory.getLogger(MarshallContent.class);
    static {
        logger.setLevel(Level.INFO);
    }

    private HorizontalLayout decisionLights;

    public MarshallContent() {
        super();
    }

    /**
     * Use lifting order instead of display order
     *
     * @see app.owlcms.nui.shared.AthleteGridContent#findAll()
     */
    @Override
    public Collection<Athlete> findAll() {
        FieldOfPlay fop = OwlcmsSession.getFop();
        if (fop != null) {
            logger.trace("{}findAll {} {} {}", fop.getLoggingName(),
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
        return getTranslation("Marshall") + OwlcmsSession.getFopNameIfMultiple();
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
     * @see app.owlcms.nui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
     */
    @Override
    protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
        createStopTimeButton();
        HorizontalLayout buttons = new HorizontalLayout(stopTimeButton);
        buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
        return buttons;
    }
    

    @Override
    public String getMenuTitle() {
        return getPageTitle();
    }

    /**
     * @see app.owlcms.nui.shared.AthleteGridContent#createTopBar()
     */
    @Override
    public FlexLayout createMenuArea() {
        topBar = new FlexLayout();
        topBar.setClassName("athleteGridTopBar");
        initialBar = false;

        HorizontalLayout topBarLeft = createTopBarLeft();

        lastName = new H2();
        lastName.setText("\u2013");
        lastName.getStyle().set("margin", "0px 0px 0px 0px");

        setFirstNameWrapper(new H3(""));
        getFirstNameWrapper().getStyle().set("margin", "0px 0px 0px 0px");
        firstName = new Span("");
        firstName.getStyle().set("margin", "0px 0px 0px 0px");
        startNumber = new Span("");
        Style style = startNumber.getStyle();
        style.set("margin", "0px 0px 0px 1em");
        style.set("padding", "0px 0px 0px 0px");
        style.set("border", "2px solid var(--lumo-primary-color)");
        style.set("font-size", "90%");
        style.set("width", "1.4em");
        style.set("text-align", "center");
        style.set("display", "inline-block");
        startNumber.setVisible(false);
        getFirstNameWrapper().add(firstName, startNumber);
        Div fullName = new Div(lastName, getFirstNameWrapper());

        attempt = new H2();
        weight = new H2();
        weight.setText("");
        if (timer == null) {
            timer = new AthleteTimerElement(this);
        }
        timer.setSilenced(this.isSilenced());
        H1 time = new H1(timer);
        clearVerticalMargins(attempt);
        clearVerticalMargins(time);
        clearVerticalMargins(weight);

        buttons = announcerButtons(topBar);
        breaks = breakButtons(topBar);
        decisions = decisionButtons(topBar);
        decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

        topBar.setSizeFull();
        topBar.add(topBarLeft, fullName, attempt, weight, time);
        if (buttons != null) {
            topBar.add(buttons);
        }
        if (breaks != null) {
            topBar.add(breaks);
        }
        if (decisions != null) {
            topBar.add(decisions);
        }

        topBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
        topBar.setAlignItems(FlexComponent.Alignment.CENTER);
        topBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
        topBar.setFlexGrow(0.5, fullName);
        topBar.setFlexGrow(0.0, topBarLeft);
        return topBar;
    }

    /**
     * @see app.owlcms.nui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
     */
    @Override
    protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
        HorizontalLayout decisions = new HorizontalLayout();
        return decisions;
    }

    protected void displayLiveDecisions() {
        if (decisionLights == null) {
            getTopBarLeft().removeAll();
            createDecisionLights();
            getTopBarLeft().add(decisionLights);
        }
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
