/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.config.Config;
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.FOPState;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class TCContent
 *
 * Technical Controller / Plates loading information.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/testing", layout = OwlcmsLayout.class)
public class TestingContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(TestingContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}
	private OwlcmsCrudFormFactory<Athlete> crudFormFactory;
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private UI ui;
	private Pre eventsLog;
	private StringBuilder eventsText;
	private String line;
	private Pre lineLog;
	private Button startTesting;
	private Button resumeCompetition;

	public TestingContent() {
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true")));
	}

	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	public FlexLayout createMenuArea() {
		FlexLayout fl = super.createMenuArea();
		// this hides the back arrow
		getAppLayout().setMenuVisible(true);
		return fl;
	}

	@Override
	public void delete(Athlete Athlete) {
		// do nothing;
	}

	public OwlcmsCrudFormFactory<Athlete> getCrudFormFactory() {
		return this.crudFormFactory;
	}

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("TestButtons.Title") + OwlcmsSession.getFopNameIfMultiple();
	}

	public void setCrudFormFactory(OwlcmsCrudFormFactory<Athlete> crudFormFactory) {
		this.crudFormFactory = crudFormFactory;
	}

	@Override
	@Subscribe
	public void slaveUpdateGrid(UIEvent.Decision e) {
	}

	@Override
	@Subscribe
	public void slaveUpdateGrid(UIEvent.LiftingOrderUpdated e) {
		ui.access(() -> {
			startTesting.setEnabled(true);
			resumeCompetition.setEnabled(false);
		});
	}
	
	@Override
	@Subscribe
	public void slaveBreakDone(UIEvent.BreakDone e) {
		ui.access(() -> {
			startTesting.setEnabled(true);
			resumeCompetition.setEnabled(false);
		});
	}
	
	@Override
	@Subscribe
	public void slaveBreakStart(UIEvent.BreakStarted e) {
		ui.access(() -> {
			resumeCompetition.setEnabled(true);
		});
	}

	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	@Override
	protected HorizontalLayout announcerButtons(FlexLayout announcerBar) {
		return null;
	}

	@Override
	protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

	@Subscribe
	public void slaveButtonTest(UIEvent.ButtonTest bt) {
		ui.access(() -> {
			line = MessageFormat.format(bt.format, bt.infos);
			lineLog.setText(line);
			eventsLog.setText(eventsText.toString());
			var prev = eventsText;
			eventsText = new StringBuilder(line);
			eventsText.append("\n");
			eventsText.append(prev);

		});
	}

	@Override
	protected void init() {
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		ui = UI.getCurrent();
		this.removeAll();
		
		setCrudFormFactory(createFormFactory());
		HorizontalLayout bts = createInterruptionButtons();
		lineLog = new Pre("\n");
		lineLog.setWidthFull();
		lineLog.getStyle().set("border-style", "solid");
		lineLog.getStyle().set("border-width", "2px");
		lineLog.getStyle().set("margin-bottom", "2px");
		lineLog.getStyle().set("font-weight", "bold");
		lineLog.getStyle().set("font-size", "115%");
				
		eventsLog = new Pre();
		eventsLog.setSizeFull();
		eventsLog.getStyle().set("border-style", "solid");
		eventsLog.getStyle().set("border-width", "2px");
		eventsLog.getStyle().set("overflow", "auto");
		eventsLog.getStyle().set("margin-top", "0px");

		eventsText = new StringBuilder();
		FlexLayout events = new FlexLayout(
		        new H4(Translator.translate("TestButtons.Title")),
		        bts,
		        lineLog,
		        eventsLog);
		events.setSizeUndefined();
		events.setHeight("100%");
		events.getStyle().set("margin-top", "1em");
		events.getStyle().set("flex-direction", "column");
		events.setWidth("95vw");
		fillHW(events, this);

	}

	private HorizontalLayout createInterruptionButtons() {
		startTesting = new Button(Translator.translate("TestButtons.StartTesting"), new Icon(VaadinIcon.EXCLAMATION),
		        (e) -> {
			        fop.fopEventPost(new FOPEvent.BreakStarted(
			                BreakType.TEST_BUTTONS,
			                CountdownType.INDEFINITE,
			                null,
			                null,
			                true,
			                this.getOrigin()));
		        });
		startTesting.getElement().setAttribute("theme", "primary contrast");
		startTesting.getElement().setAttribute("title", Translator.translate("StopCompetition"));


		resumeCompetition = new Button(Translator.translate("ResumeCompetition"), new Icon(VaadinIcon.MICROPHONE),
		        (e) -> {
			        fop.fopEventPost(new FOPEvent.StartLifting(this));
		        });
		resumeCompetition.getElement().setAttribute("theme", "primary success");
		resumeCompetition.getElement().setAttribute("title", Translator.translate("ResumeCompetition"));
		
		startTesting.addClickListener(e -> {
			resumeCompetition.setEnabled(true);
			startTesting.setEnabled(false);
		});
		resumeCompetition.addClickListener(e -> {
			resumeCompetition.setEnabled(false);
			startTesting.setEnabled(true);
		});
		
		boolean inBreak = getFop().getState() == FOPState.BREAK;
		startTesting.setEnabled(true);
		resumeCompetition.setEnabled(inBreak);
		
		HorizontalLayout buttons = new HorizontalLayout();
		if (fop != null && Config.getCurrent().featureSwitch("mqttDownSignal")) {
			var testDown = new Button(Translator.translate("TestButtons.TestDownSignal"), new Icon(VaadinIcon.ARROW_DOWN),
			        (e) -> {
				        new Thread(() -> {
							try {
								fop.getMqttMonitor().testDownSignal();
							} catch (MqttException e1) {
								LoggerUtils.logError(logger, e1);
							}
						}).start();
			        });
			//testDown.getElement().setAttribute("theme", "primary");
			testDown.getElement().setAttribute("title", Translator.translate("ResumeCompetition"));
			buttons.add(startTesting, resumeCompetition, testDown);
		} else {
			buttons.add(startTesting, resumeCompetition);
		}
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		return buttons;
	}

	
}
