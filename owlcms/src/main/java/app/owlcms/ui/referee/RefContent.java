/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.referee;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;

import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.QueryParameterReader;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "ref")
public class RefContent extends VerticalLayout implements QueryParameterReader, SafeEventBusRegistration, UIEventProcessor, HasDynamicTitle, RequireLogin, PageConfigurator {

    final private static Logger logger = (Logger) LoggerFactory.getLogger(RefContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName()); //$NON-NLS-1$
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private HorizontalLayout refVotingButtons;
    private VerticalLayout refVotingCenterHorizontally;
    private FieldOfPlay fop;
    private boolean redTouched;
    private Object refIndex;
    private boolean whiteTouched;
    private Icon good;
    private Icon bad;


    public RefContent() {
        init();
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addMetaTag("mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addLink("shortcut icon", "frontend/images/owlcms.ico");
        settings.addFavIcon("icon", "frontend/images/logo.png", "96x96");
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return getTranslation("Referee")+(refIndex == null ? "" : refIndex); //$NON-NLS-1$
    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccess(this, fop.getUiEventBus(), () -> {
            good.getElement().setEnabled(false); // cannot grant after down has been given
            redTouched = false; // re-enable processing of red.
        });
    }

    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        UIEventProcessor.uiAccess(this, fop.getUiEventBus(), () -> {
            resetRefVote();
        });
    }

    @Subscribe
    public void slaveDown(UIEvent.DownSignal e) {
        //TODO if no decision, tell referee
    }

    @Subscribe
    public void slaveTimeStarted(UIEvent.StartTime e) {
        UIEventProcessor.uiAccess(this, fop.getUiEventBus(), () -> {
            refVotingButtons.removeAll();
            resetRefVote();
            // referee decisions handle reset on their own, nothing to do.
        });
    }

    protected void init() {
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setSizeFull();
        fop = OwlcmsSession.getFop();
        buildRefBox(this);
    }

    private Icon bigIcon(VaadinIcon iconDef, String color) {
        Icon icon = iconDef.create();
        icon.setSize("100%"); //$NON-NLS-1$
        icon.getStyle().set("color", color); //$NON-NLS-1$
        return icon;
    }

    private void buildRefBox(VerticalLayout refContainer) {
        HorizontalLayout topRow = new HorizontalLayout();
        Label juryLabel = new Label(getTranslation("Referee")); //$NON-NLS-1$
        H3 labelWrapper = new H3(juryLabel);
        labelWrapper.getStyle().set("margin-top", "0");
        labelWrapper.getStyle().set("margin-bottom", "0");
        labelWrapper.setWidth("15em"); //$NON-NLS-1$
        Label spacer = new Label();
        spacer.setWidth("3em"); //$NON-NLS-1$
        topRow.add(labelWrapper,spacer);
        topRow.setMargin(true);

        buildRefVoting();
        resetRefVote();

        refContainer.setBoxSizing(BoxSizing.BORDER_BOX);
        refContainer.setMargin(false);
        refContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        refContainer.add(topRow);
        refContainer.setAlignSelf(Alignment.START, topRow);
        refContainer.add(refVotingCenterHorizontally);

    }

    private void buildRefVoting() {
        // center buttons vertically, spread withing proper width
        refVotingButtons = new HorizontalLayout();
        refVotingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
        refVotingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
        refVotingButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        refVotingButtons.setHeight("65vh"); //$NON-NLS-1$
        refVotingButtons.setWidth("90%"); //$NON-NLS-1$
        refVotingButtons.getStyle().set("background-color", "black"); //$NON-NLS-1$ //$NON-NLS-2$
        refVotingButtons.setPadding(false);
        refVotingButtons.setMargin(false);

        // center the button cluster within page width
        refVotingCenterHorizontally = new VerticalLayout();
        refVotingCenterHorizontally.setWidthFull();
        refVotingCenterHorizontally.setBoxSizing(BoxSizing.BORDER_BOX);
        refVotingCenterHorizontally.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        refVotingCenterHorizontally.setPadding(true);
        refVotingCenterHorizontally.setMargin(true);
        refVotingCenterHorizontally.getStyle().set("background-color", "black"); //$NON-NLS-1$ //$NON-NLS-2$

        refVotingCenterHorizontally.add(refVotingButtons);
        return;
    }

    private void doRed() {
        OwlcmsSession.withFop(fop -> {
            fop.getFopEventBus().post(new FOPEvent.RefereeIndividualUpdate(getOrigin(), refIndex, false));
        });
        good.getStyle().set("color", "grey");
    }

    private void doWhite() {
        OwlcmsSession.withFop(fop -> {
            fop.getFopEventBus().post(new FOPEvent.RefereeIndividualUpdate(getOrigin(), refIndex, false));
        });
        bad.getStyle().set("color", "grey");
    }

    private Object getOrigin() {
        return this;
    }


    private void redClicked(DomEvent e) {
        if (!redTouched) {
            doRed();
        }
    }

    private void redTouched(DomEvent e) {
        redTouched = true;
        doRed();
        UI.getCurrent().getPage().executeJavaScript("window.navigator.vibrate",200);
    }


    private void resetRefVote() {
        refVotingButtons.removeAll();
        good = bigIcon(VaadinIcon.CHECK_CIRCLE, "white"); //$NON-NLS-1$
        good.getElement().addEventListener("touchstart", (e) -> whiteTouched(e));
        good.getElement().addEventListener("click", (e) -> whiteClicked(e));
        bad = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red"); //$NON-NLS-1$
        bad.getElement().addEventListener("touchstart", (e) -> redTouched(e));
        bad.getElement().addEventListener("click", (e) -> redClicked(e));
        refVotingButtons.add(good, bad);
    }


    //	private Key getBadKey(int i) {
    //		switch (i) {
    //		case 0:
    //			return Key.DIGIT_2;
    //		case 1:
    //			return Key.DIGIT_4;
    //		case 2:
    //			return Key.DIGIT_6;
    //		case 3:
    //			return Key.DIGIT_8;
    //		case 4:
    //			return Key.DIGIT_0;
    //		default:
    //			return Key.UNIDENTIFIED;
    //		}
    //	}
    //
    //	private Key getGoodKey(int i) {
    //		switch (i) {
    //		case 0:
    //			return Key.DIGIT_1;
    //		case 1:
    //			return Key.DIGIT_3;
    //		case 2:
    //			return Key.DIGIT_5;
    //		case 3:
    //			return Key.DIGIT_7;
    //		case 4:
    //			return Key.DIGIT_9;
    //		default:
    //			return Key.UNIDENTIFIED;
    //		}
    //	}


    private void whiteClicked(DomEvent e) {
        if (!whiteTouched) {
            doWhite();
        }
        UI.getCurrent().getPage().executeJavaScript("window.navigator.vibrate",200);
    }

    private void whiteTouched(DomEvent e) {
        whiteTouched = true;
        doWhite();
    }


}
