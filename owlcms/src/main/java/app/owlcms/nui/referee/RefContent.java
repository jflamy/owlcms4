/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.referee;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.SoundUtils;
import app.owlcms.apputils.queryparameters.FOPParameters;
import app.owlcms.components.elements.BeepElement;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.RequireLogin;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings({ "serial", "deprecation" })
@Route(value = "ref")
@CssImport(value = "./styles/shared-styles.css")

public class RefContent extends VerticalLayout implements FOPParameters, SafeEventBusRegistration,
        UIEventProcessor, HasDynamicTitle, RequireLogin, BeforeEnterListener {

	private class DelayTimer {
		private final Timer t = new Timer();

		public TimerTask schedule(final Runnable r, long delay) {
			final TimerTask task = new TimerTask() {
				@Override
				public void run() {
					r.run();
				}
			};
			t.schedule(task, delay);
			return task;
		}
	}

	final private static Logger logger = (Logger) LoggerFactory.getLogger(RefContent.class);
	private static final String REF_INDEX = "num";
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	private Icon bad;
	private BeepElement beeper;
	private Icon good;
	private Location location;
	private UI locationUI;
	private boolean redTouched;
	private Integer ref13ix = null; // 1 2 or 3
	private IntegerField refField;
	private HorizontalLayout refVotingButtons;
	private VerticalLayout refVotingCenterHorizontally;
	private HorizontalLayout topRow;
	private EventBus uiEventBus;
	private HashMap<String, List<String>> urlParams;
	private HorizontalLayout warningRow;

	private boolean whiteTouched;

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

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
		return Translator.translate("Referee") + OwlcmsSession.getFopNameIfMultiple()
		        + (getRef13ix() != null ? (" " + getRef13ix()) : "");
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
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
	 * Note: because we have the @Route, the parameters are parsed *before* our
	 * parent layout is created.
	 *
	 * @param event     Vaadin navigation event
	 * @param parameter null in this case -- we don't want a vaadin "/" parameter.
	 *                  This allows us to add query parameters instead.
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
				setRef13ix(Integer.parseInt(num));
				logger.debug("parsed {} parameter = {}", REF_INDEX, num);
				refField.setValue(getRef13ix().intValue());
			} catch (NumberFormatException e) {
				setRef13ix(null);
				num = null;
				LoggerUtils.logError(logger, e);
			}
		}

	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	@Subscribe
	public void slaveDecision(UIEvent.Decision e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			good.getElement().setEnabled(false); // cannot grant after down has been given
			redTouched = false; // re-enable processing of red.
		});
	}

	/**
	 * This must come from a timer on FieldOfPlay, because if we are using mobile
	 * devices there will not be a master decision reset coming from the
	 * keypad-hosting device
	 *
	 * @param e
	 */
	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		logger.debug("received decision reset {}", getRef13ix());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			resetRefVote();
		});
	}

	@Subscribe
	public void slaveDown(UIEvent.DownSignal e) {
	}

	@Subscribe
	public void slaveRefereeUpdate(UIEvent.RefereeUpdate e) {
		// only used during simulations to show what the fake referees pressed.
		if (!Competition.getCurrent().isSimulation()) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, uiEventBus, e, this, () -> {
			switch (getRef13ix()) {
			case 1:
				if (e.ref1 != null) {
					if (e.ref1) {
						doWhiteColor();
					} else {
						doRedColor();
					}
				}
				break;
			case 2:
				if (e.ref2 != null) {
					if (e.ref2) {
						doWhiteColor();
					} else {
						doRedColor();
					}
				}
				break;
			case 3:
				if (e.ref3 != null) {
					if (e.ref3) {
						doWhiteColor();
					} else {
						doRedColor();
					}
				}
				break;
			default:
				break;
			}
		});

	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		logger.debug("received decision reset {}", getRef13ix());
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			resetRefVote();
		});
	}

	@Subscribe
	public void slaveSummonRef(UIEvent.SummonRef e) {
		if (getRef13ix() == null || (e.ref != 0 && e.ref != getRef13ix())) {
			return;
		}
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			warningRow.removeAll();
			warningRow.setVisible(true);
			warningRow.setWidth("100%");
			warningRow.setPadding(false);
			warningRow.setMargin(false);
			H3 h3 = new H3(Translator.translate("JuryNotification.PleaseSeeJury"));
			h3.getElement().setAttribute("style",
			        "background-color: red; width: 100%; color: white; text-align: center; padding: 0; margin-top:0.5em");
			h3.getClassNames().add("blink");
			h3.setWidth("100%");
			warningRow.add(h3);
			warningRow.getElement().setAttribute("style", "background-color: red; width: 100%;");
			topRow.setVisible(false);
			beeper.beep();
			UI currentUI = UI.getCurrent();
			new DelayTimer().schedule(() -> currentUI.access(() -> {
				beeper.beep();
			}), 1000);
			new DelayTimer().schedule(() -> currentUI.access(() -> {
				warningRow.setVisible(false);
				topRow.setVisible(true);
			}), 9000);
		});
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			resetRefVote();
		});
	}

	@Subscribe
	public void slaveWakeUpRef(UIEvent.WakeUpRef e) {
		if (getRef13ix() == null || e.ref != getRef13ix()) {
			return;
		}
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (e.on) {
				warningRow.removeAll();
				warningRow.setVisible(true);
				H3 h3 = new H3(Translator.translate("JuryNotification.PleaseEnterDecision"));
				h3.getElement().setAttribute("style",
				        "background-color: yellow; width: 100%; color: black; text-align: center; padding: 0; margin-top:0.5em");
				h3.getClassNames().add("blink");
				h3.setWidth("100%");
				warningRow.add(h3);
				warningRow.getElement().setAttribute("style", "background-color: yellow; width: 100%;");
				topRow.setVisible(false);
				beeper.beep();
			} else {
				warningRow.setVisible(false);
				topRow.setVisible(true);
			}
		});
	}

	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("100%");
		icon.getStyle().set("color", color);
		return icon;
	}

	private void createContent(VerticalLayout refContainer) {
		topRow = new HorizontalLayout();
		topRow.add(beeper);
		warningRow = new HorizontalLayout();
		warningRow.setPadding(false);
		warningRow.setMargin(false);
		warningRow.setVisible(false);

		NativeLabel refLabel = new NativeLabel(getTranslation("Referee"));
		H3 labelWrapper = new H3(refLabel);
		labelWrapper.getStyle().set("margin-top", "0");
		labelWrapper.getStyle().set("margin-bottom", "0");

		refField = new IntegerField();
		refField.setStep(1);
		refField.setMax(3);
		refField.setMin(1);
		refField.setValue(getRef13ix() == null ? null : getRef13ix().intValue());
		refField.setPlaceholder(getTranslation("Number"));
		//DIXME refField.setHasControls(true);
		refField.addValueChangeListener((e) -> {
			setRef13ix(e.getValue());
			setUrl(getRef13ix() != null ? getRef13ix().toString() : null);
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
		refContainer.add(topRow, warningRow);
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

	private void doRed() {
		OwlcmsSession.withFop(fop -> {
			fop.fopEventPost(new FOPEvent.DecisionUpdate(getOrigin(), getRef13ix() - 1, false));
		});
		doRedColor();
	}

	private void doRedColor() {
		good.getStyle().set("color", "DarkSlateGrey");
		good.getStyle().set("outline-color", "white");
		bad.getStyle().set("color", "red");
	}

	private void doWhite() {
		OwlcmsSession.withFop(fop -> {
			fop.fopEventPost(new FOPEvent.DecisionUpdate(getOrigin(), getRef13ix() - 1, true));
		});
		doWhiteColor();
	}

	private void doWhiteColor() {
		bad.getStyle().set("color", "DarkSlateGrey");
		bad.getStyle().set("outline-color", "white");
		good.getStyle().set("color", "white");
	}

	private Object getOrigin() {
		return this;
	}

	private Integer getRef13ix() {
		return ref13ix;
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
		topRow.setVisible(true);
		warningRow.setVisible(false);
	}

	private void setRef13ix(Integer ref13ix) {
		this.ref13ix = ref13ix;
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
		beeper = new BeepElement();
		createContent(this);
	}

	/**
	 * @see com.vaadin.flow.component.Component#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		// crude workaround -- randomly getting light or dark due to multiple themes
		// detected in app.
		getElement().executeJs("document.querySelector('html').setAttribute('theme', 'dark');");

		SoundUtils.enableAudioContextNotification(this.getElement(), true);
		OwlcmsSession.withFop(fop -> {
			// we listen on uiEventBus.
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}
}
