/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.referee;

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
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
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

import app.owlcms.apputils.queryparameters.BaseContent;
import app.owlcms.apputils.queryparameters.FOPParametersReader;
import app.owlcms.data.competition.Competition;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.AuthorizationDispatch;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.JuryDeliberationEventType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings({ "serial", "deprecation" })
@Route(value = "jury")
@CssImport(value = "./styles/shared-styles.css")
public class JuryMobileContent extends BaseContent implements FOPParametersReader, SafeEventBusRegistration,
        UIEventProcessor, HasDynamicTitle, AuthorizationDispatch, BeforeEnterListener {

	private static final int MAX_JURY_MEMBERS = 5;
	private static final String JURY_INDEX = "num";
	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryMobileContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());

	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private Icon bad;
	private Icon good;
	private IntegerField juryField;
	private Integer juryMemberNumber;
	private Location location;
	private UI locationUI;
	private boolean redTouched;
	private HorizontalLayout topRow;
	private EventBus uiEventBus;
	private Map<String, List<String>> urlParams;
	private VerticalLayout votingCenterHorizontally;
	private HorizontalLayout votingButtons;
	private boolean whiteTouched;
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public JuryMobileContent() {
		OwlcmsFactory.waitDBInitialized();
		init();
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		AuthorizationDispatch.super.beforeEnter(event);
		UI.getCurrent().getPage().setTitle(getPageTitle());
	}

	@Override
	public String getPageTitle() {
		return getJuryLabel() + FieldOfPlay.getFopNameIfMultiple(getFop())
		        + (getJuryMemberNumber() != null ? (" " + getJuryMemberNumber()) : "");
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
		this.location = event.getLocation();
		this.locationUI = event.getUI();
		QueryParameters queryParameters = this.location.getQueryParameters();
		Map<String, List<String>> parametersMap = queryParameters.getParameters();
		this.urlParams = readParams(this.location, parametersMap);

		List<String> nums = parametersMap.get(JURY_INDEX);
		String num = null;
		if (nums != null) {
			num = nums.get(0);
			try {
				setJuryMemberNumber(sanitizeJuryMember(Integer.parseInt(num)));
				logger.debug("parsed {} parameter = {}", JURY_INDEX, num);
				this.juryField.setValue(getJuryMemberNumber() == null ? null : getJuryMemberNumber().intValue());
			} catch (NumberFormatException e) {
				setJuryMemberNumber(null);
				num = null;
				LoggerUtils.logError(logger, e);
			}
		}
	}

	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
	}

	@Subscribe
	public void slaveJuryNotification(UIEvent.JuryNotification e) {
		JuryDeliberationEventType deliberationType = e.getDeliberationEventType();
		if (deliberationType == JuryDeliberationEventType.START_DELIBERATION
		        || deliberationType == JuryDeliberationEventType.CHALLENGE) {
			UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetVote);
		}
	}

	@Subscribe
	public void slaveDecisionReset(UIEvent.DecisionReset e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetVote);
	}

	@Subscribe
	public void slaveJuryUpdate(UIEvent.JuryUpdate e) {
		if (getJuryMemberNumber() == null) {
			return;
		}
		UIEventProcessor.uiAccessIgnoreIfSelfOrigin(this, this.uiEventBus, e, this, () -> {
			int juryIndex = getJuryMemberNumber() - 1;
			Boolean[] decisions = e.getJuryMemberDecision();
			if (decisions == null || juryIndex < 0 || juryIndex >= decisions.length || decisions[juryIndex] == null) {
				return;
			}
			if (decisions[juryIndex]) {
				doWhiteColor();
			} else {
				doRedColor();
			}
		});
	}

	@Subscribe
	public void slaveStartLifting(UIEvent.StartLifting e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetVote);
	}

	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, this::resetVote);
	}

	protected ComboBox<FieldOfPlay> createFopSelect() {
		ComboBox<FieldOfPlay> fopSelect = new ComboBox<>();
		fopSelect.setPlaceholder(Translator.translate("SelectPlatform"));
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		getElement().executeJs("document.querySelector('html').setAttribute('theme', 'dark');");
		OwlcmsSession.withFop(this::bindToFop);
	}

	private void bindToFop(FieldOfPlay fop) {
		if (fop == null) {
			return;
		}
		setFop(fop);
		this.uiEventBus = uiEventBusRegister(this, fop);
	}

	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("70%");
		icon.getStyle().set("color", color);
		return icon;
	}

	private void createContent(VerticalLayout juryContainer) {
		NativeLabel juryLabel = new NativeLabel(getJuryLabel());
		var labelWrapper = new H2(juryLabel);
		labelWrapper.getStyle().set("margin-top", "0");
		labelWrapper.getStyle().set("margin-bottom", "0");

		this.juryField = new IntegerField();
		this.juryField.setStep(1);
		this.juryField.setMax(getMaximumJuryMembers());
		this.juryField.setMin(1);
		this.juryField.setValue(getJuryMemberNumber() == null ? null : getJuryMemberNumber().intValue());
		this.juryField.setPlaceholder(Translator.translate("Number"));
		this.juryField.setStepButtonsVisible(true);
		this.juryField.addValueChangeListener((e) -> {
			Integer sanitized = sanitizeJuryMember(e.getValue());
			if (e.getValue() != null && !e.getValue().equals(sanitized)) {
				this.juryField.setValue(sanitized);
				return;
			}
			setJuryMemberNumber(sanitized);
			setUrl(getJuryMemberNumber() != null ? getJuryMemberNumber().toString() : null);
		});

		ComboBox<FieldOfPlay> fopSelect = createFopSelect();
		fopSelect.setValue(OwlcmsSession.getFop());
		fopSelect.addValueChangeListener((e) -> {
			OwlcmsSession.setFop(e.getValue());
			bindToFop(e.getValue());
		});

		this.topRow = new HorizontalLayout();
		this.topRow.add(labelWrapper, fopSelect, this.juryField);
		this.topRow.setMargin(false);
		this.topRow.setSpacing(true);
		this.topRow.setAlignItems(Alignment.BASELINE);

		createVoting();
		resetVote();

		juryContainer.setId("juryMobileContainer");
		juryContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		juryContainer.setMargin(false);
		juryContainer.getClassNames().add("dark");
		juryContainer.setHeight("100%");
		juryContainer.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		juryContainer.add(this.topRow);
		juryContainer.setAlignSelf(Alignment.START, this.topRow);
		this.votingCenterHorizontally.setId("juryVotingCenterHorizontally");
		this.setId("top");
		juryContainer.add(this.votingCenterHorizontally);
	}

	private void createVoting() {
		this.votingButtons = new HorizontalLayout();
		this.votingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
		this.votingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
		this.votingButtons.setDefaultVerticalComponentAlignment(Alignment.CENTER);
		this.votingButtons.setHeight("100%");
		this.votingButtons.setWidth("100%");
		this.votingButtons.getStyle().set("background-color", "black");
		this.votingButtons.setPadding(false);
		this.votingButtons.setMargin(false);
		this.votingButtons.setSpacing(true);

		this.votingCenterHorizontally = new VerticalLayout();
		this.votingCenterHorizontally.setSizeFull();
		this.votingCenterHorizontally.setBoxSizing(BoxSizing.BORDER_BOX);
		this.votingCenterHorizontally.setDefaultHorizontalComponentAlignment(Alignment.CENTER);
		this.votingCenterHorizontally.setPadding(true);
		this.votingCenterHorizontally.setMargin(true);
		this.votingCenterHorizontally.add(this.votingButtons);
	}

	private void doRed() {
		OwlcmsSession.withFop(fop -> {
			if (getJuryMemberNumber() == null) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(getOrigin(), getJuryMemberNumber() - 1, false));
		});
		doRedColor();
	}

	private void doRedColor() {
		this.good.getStyle().set("color", "DarkSlateGrey");
		this.good.getStyle().set("outline-color", "white");
		this.bad.getStyle().set("color", "red");
	}

	private void doWhite() {
		OwlcmsSession.withFop(fop -> {
			if (getJuryMemberNumber() == null) {
				return;
			}
			fop.fopEventPost(new FOPEvent.JuryMemberDecisionUpdate(getOrigin(), getJuryMemberNumber() - 1, true));
		});
		doWhiteColor();
	}

	private void doWhiteColor() {
		this.bad.getStyle().set("color", "DarkSlateGrey");
		this.bad.getStyle().set("outline-color", "white");
		this.good.getStyle().set("color", "white");
	}

	private Integer getJuryMemberNumber() {
		return this.juryMemberNumber;
	}

	private String getJuryLabel() {
		if (getJuryMemberNumber() != null && getJuryMemberNumber() >= 1 && getJuryMemberNumber() <= MAX_JURY_MEMBERS) {
			return Translator.translate("Jury" + getJuryMemberNumber());
		}
		return Translator.translate("Jury");
	}

	private int getMaximumJuryMembers() {
		return Math.min(MAX_JURY_MEMBERS, Competition.getCurrent().getJurySize());
	}

	private Object getOrigin() {
		return this;
	}

	private void redClicked(DomEvent e) {
		if (!this.redTouched) {
			doRed();
		}
	}

	private void redTouched(DomEvent e) {
		this.redTouched = true;
		doRed();
		vibrate();
	}

	private void resetVote() {
		this.redTouched = false;
		this.whiteTouched = false;
		this.votingButtons.removeAll();
		this.good = bigIcon(VaadinIcon.CHECK_CIRCLE, "white");
		this.good.getElement().addEventListener("touchstart", this::whiteTouched);
		this.good.getElement().addEventListener("click", this::whiteClicked);
		this.bad = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
		this.bad.getElement().addEventListener("touchstart", this::redTouched);
		this.bad.getElement().addEventListener("click", this::redClicked);
		this.votingButtons.add(this.bad, this.good);
	}

	private Integer sanitizeJuryMember(Integer value) {
		if (value == null || value < 1 || value > getMaximumJuryMembers()) {
			return null;
		}
		return value;
	}

	private void setJuryMemberNumber(Integer juryMemberNumber) {
		this.juryMemberNumber = juryMemberNumber;
	}

	private void setUrl(String num) {
		if (num != null) {
			this.urlParams.put(JURY_INDEX, Arrays.asList(num));
		} else {
			this.urlParams.remove(JURY_INDEX);
		}
		Location location2 = new Location(this.location.getPath(), new QueryParameters(this.urlParams));
		URLUtils.replaceState(this.locationUI.getPage().getHistory(), null, location2, this.location);
		logger.trace("changed location to {}", location2.getPathWithQueryParameters());
		UI.getCurrent().getPage().setTitle(getPageTitle());
	}

	private void vibrate() {
		UI.getCurrent().getPage().executeJs("window.navigator.vibrate", 200);
	}

	private void whiteClicked(DomEvent e) {
		if (!this.whiteTouched) {
			doWhite();
		}
		vibrate();
	}

	private void whiteTouched(DomEvent e) {
		this.whiteTouched = true;
		doWhite();
	}
}