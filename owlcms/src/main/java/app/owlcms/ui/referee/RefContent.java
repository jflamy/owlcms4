/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.ui.referee;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InitialPageSettings;
import com.vaadin.flow.server.PageConfigurator;

import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.UIEventProcessor;
import app.owlcms.ui.shared.RequireLogin;
import app.owlcms.ui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import app.owlcms.i18n.Translator;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "ref")
@Push
public class RefContent extends VerticalLayout implements FOPParameters, SafeEventBusRegistration,
        UIEventProcessor, HasDynamicTitle, RequireLogin, PageConfigurator, BeforeEnterListener {

    private static final String REF_INDEX = "num";
    final private static Logger logger = (Logger) LoggerFactory.getLogger(RefContent.class);
    final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
    static {
        logger.setLevel(Level.INFO);
        uiEventLogger.setLevel(Level.INFO);
    }

    private HorizontalLayout refVotingButtons;
    private VerticalLayout refVotingCenterHorizontally;
    private boolean redTouched;
    private Integer refIndex = null; // 1 2 or 3
    private boolean whiteTouched;
    private Icon good;
    private Icon bad;
    private Location location;
    private UI locationUI;
    private HashMap<String, List<String>> urlParams;
    private NumberField refField;
    private EventBus uiEventBus;

    public RefContent() {
        OwlcmsFactory.waitDBInitialized();
        init();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        RequireLogin.super.beforeEnter(event);
        UI.getCurrent().getPage().setTitle(getPageTitle());
    }

    @Override
    public void configurePage(InitialPageSettings settings) {
        settings.addMetaTag("mobile-web-app-capable", "yes");
        settings.addMetaTag("apple-mobile-web-app-capable", "yes");
        settings.addLink("shortcut icon", "frontend/images/owlcms.ico");
        settings.addFavIcon("icon", "frontend/images/logo.png", "96x96");
        settings.setViewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes");
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public UI getLocationUI() {
        return this.locationUI;
    }

    /**
     * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
     */
    @Override
    public String getPageTitle() {
        return Translator.translate("Referee") + (refIndex != null ? (" " + refIndex) : "");
    }

    @Override
    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public void setLocationUI(UI locationUI) {
        this.locationUI = locationUI;
    }

    /**
     * Parse the http query parameters
     *
     * Note: because we have the @Route, the parameters are parsed *before* our parent layout is created.
     *
     * @param event     Vaadin navigation event
     * @param parameter null in this case -- we don't want a vaadin "/" parameter. This allows us to add query
     *                  parameters instead.
     *
     * @see app.owlcms.apputils.queryparameters.FOPParameters#setParameter(com.vaadin.flow.router.BeforeEvent,
     *      java.lang.String)
     */
    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        location = event.getLocation();
        locationUI = event.getUI();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters(); // immutable
        urlParams = readParams(location, parametersMap);

        // get the referee number from query parameters, do not add value if num is not
        // defined
        List<String> nums = parametersMap.get(REF_INDEX);
        String num = null;
        if (nums != null) {
            num = nums.get(0);
            try {
                refIndex = Integer.parseInt(num);
                logger.debug("parsed {} parameter = {}", REF_INDEX, num);
                refField.setValue(refIndex.doubleValue());
            } catch (NumberFormatException e) {
                refIndex = null;
                num = null;
                LoggerUtils.logError(logger, e);
            }
        }

    }

    @Subscribe
    public void slaveDecision(UIEvent.Decision e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            good.getElement().setEnabled(false); // cannot grant after down has been given
            redTouched = false; // re-enable processing of red.
        });
    }

    /**
     * This must come from a timer on FieldOfPlay, because if we are using mobile devices there will not be a master
     * decision reset coming from the keypad-hosting device
     *
     * @param e
     */
    @Subscribe
    public void slaveDecisionReset(UIEvent.DecisionReset e) {
        logger.debug("received decision reset {}", refIndex);
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            resetRefVote();
        });
    }

    @Subscribe
    public void slaveDown(UIEvent.DownSignal e) {
        // if no decision, remind referee
    }

    @Subscribe
    public void slaveTimeStarted(UIEvent.StartTime e) {
        UIEventProcessor.uiAccess(this, uiEventBus, () -> {
            resetRefVote();
        });
    }

    protected ComboBox<FieldOfPlay> createFopSelect() {
        ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
        fopSelect.setPlaceholder(getTranslation("SelectPlatform"));
        fopSelect.setItems(OwlcmsFactory.getFOPs());
        fopSelect.setItemLabelGenerator(FieldOfPlay::getName);
        fopSelect.setWidth("10rem");
        return fopSelect;
    }

    protected void init() {
        this.setBoxSizing(BoxSizing.BORDER_BOX);
        this.setSizeFull();
        createContent(this);
    }

    /**
     * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        OwlcmsSession.withFop(fop -> {
            // we listen on uiEventBus.
            uiEventBus = uiEventBusRegister(this, fop);
        });
    }

    private Icon bigIcon(VaadinIcon iconDef, String color) {
        Icon icon = iconDef.create();
        icon.setSize("100%");
        icon.getStyle().set("color", color);
        return icon;
    }

    private void createContent(VerticalLayout refContainer) {
        HorizontalLayout topRow = new HorizontalLayout();
        Label juryLabel = new Label(getTranslation("Referee"));
        H3 labelWrapper = new H3(juryLabel);
        labelWrapper.getStyle().set("margin-top", "0");
        labelWrapper.getStyle().set("margin-bottom", "0");

        refField = new NumberField();
        refField.setStep(1.0D);
        refField.setMax(3.0D);
        refField.setMin(1.0D);
        refField.setValue(refIndex == null ? null : refIndex.doubleValue());
        refField.setPlaceholder(getTranslation("Number"));
        refField.setHasControls(true);
        refField.addValueChangeListener((e) -> {
            Double value = e.getValue();
            refIndex = value == null ? null : ((int) Math.round(value));
            setUrl(refIndex != null ? refIndex.toString() : null);
        });

        ComboBox<FieldOfPlay> fopSelect = createFopSelect();
        fopSelect.setValue(OwlcmsSession.getFop());
        fopSelect.addValueChangeListener((e) -> {
            OwlcmsSession.setFop(e.getValue());
        });

        topRow.add(labelWrapper, fopSelect, refField);
        topRow.setMargin(false);
        topRow.setAlignItems(Alignment.BASELINE);

        createRefVoting();
        resetRefVote();

        refContainer.setBoxSizing(BoxSizing.BORDER_BOX);
        refContainer.setMargin(false);
        refContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        refContainer.add(topRow);
        refContainer.setAlignSelf(Alignment.START, topRow);
        refContainer.add(refVotingCenterHorizontally);

    }

    private void createRefVoting() {
        // center buttons vertically, spread withing proper width
        refVotingButtons = new HorizontalLayout();
        refVotingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
        refVotingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
        refVotingButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        refVotingButtons.setHeight("60vh");
        refVotingButtons.setWidth("90%");
        refVotingButtons.getStyle().set("background-color", "black");
        refVotingButtons.setPadding(false);
        refVotingButtons.setMargin(false);

        // center the button cluster within page width
        refVotingCenterHorizontally = new VerticalLayout();
        refVotingCenterHorizontally.setWidthFull();
        refVotingCenterHorizontally.setBoxSizing(BoxSizing.BORDER_BOX);
        refVotingCenterHorizontally.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        refVotingCenterHorizontally.setPadding(true);
        refVotingCenterHorizontally.setMargin(true);
        refVotingCenterHorizontally.getStyle().set("background-color", "black");

        refVotingCenterHorizontally.add(refVotingButtons);
        return;
    }

    // private Key getBadKey(int i) {
    // switch (i) {
    // case 0:
    // return Key.DIGIT_2;
    // case 1:
    // return Key.DIGIT_4;
    // case 2:
    // return Key.DIGIT_6;
    // case 3:
    // return Key.DIGIT_8;
    // case 4:
    // return Key.DIGIT_0;
    // default:
    // return Key.UNIDENTIFIED;
    // }
    // }
    //
    // private Key getGoodKey(int i) {
    // switch (i) {
    // case 0:
    // return Key.DIGIT_1;
    // case 1:
    // return Key.DIGIT_3;
    // case 2:
    // return Key.DIGIT_5;
    // case 3:
    // return Key.DIGIT_7;
    // case 4:
    // return Key.DIGIT_9;
    // default:
    // return Key.UNIDENTIFIED;
    // }
    // }

    private void doRed() {
        OwlcmsSession.withFop(fop -> {
            fop.fopEventPost(new FOPEvent.DecisionUpdate(getOrigin(), refIndex - 1, false));
        });
        good.getStyle().set("color", "grey");
    }

    private void doWhite() {
        OwlcmsSession.withFop(fop -> {
            fop.fopEventPost(new FOPEvent.DecisionUpdate(getOrigin(), refIndex - 1, true));
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
        vibrate();
    }

    private void resetRefVote() {
        refVotingButtons.removeAll();
        good = bigIcon(VaadinIcon.CHECK_CIRCLE, "white");
        good.getElement().addEventListener("touchstart", (e) -> whiteTouched(e));
        good.getElement().addEventListener("click", (e) -> whiteClicked(e));
        bad = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
        bad.getElement().addEventListener("touchstart", (e) -> redTouched(e));
        bad.getElement().addEventListener("click", (e) -> redClicked(e));
        refVotingButtons.add(bad, good);
    }

    private void setUrl(String num) {
        if (num != null) {
            urlParams.put(REF_INDEX, Arrays.asList(num));
        } else {
            urlParams.remove(REF_INDEX);
        }
        // change the URL to reflect group
        Location location2 = new Location(location.getPath(), new QueryParameters(urlParams));
        locationUI.getPage().getHistory().replaceState(null, location2);
        logger.trace("changed location to {}", location2.getPathWithQueryParameters());
        UI.getCurrent().getPage().setTitle(getPageTitle());
    }

    private void vibrate() {
        UI.getCurrent().getPage().executeJs("window.navigator.vibrate", 200);
    }

    private void whiteClicked(DomEvent e) {
        if (!whiteTouched) {
            doWhite();
        }
        vibrate();
    }

    private void whiteTouched(DomEvent e) {
        whiteTouched = true;
        doWhite();
    }

}
