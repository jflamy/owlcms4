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
import app.owlcms.fieldofplay.CountdownType;
import app.owlcms.fieldofplay.FOPEvent;
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
		var stopCompetition = new Button(Translator.translate("TestButtons.StartTesting"), new Icon(VaadinIcon.EXCLAMATION),
		        (e) -> {
			        fop.fopEventPost(new FOPEvent.BreakStarted(
			                BreakType.TEST_BUTTONS,
			                CountdownType.INDEFINITE,
			                null,
			                null,
			                true,
			                this.getOrigin()));
		        });
		stopCompetition.getElement().setAttribute("theme", "primary contrast");
		stopCompetition.getElement().setAttribute("title", Translator.translate("StopCompetition"));

		var endInterruption = new Button(Translator.translate("ResumeCompetition"), new Icon(VaadinIcon.MICROPHONE),
		        (e) -> {
			        fop.fopEventPost(new FOPEvent.StartLifting(this));
		        });
		endInterruption.getElement().setAttribute("theme", "primary success");
		endInterruption.getElement().setAttribute("title", Translator.translate("ResumeCompetition"));
		
		var testDown = new Button(Translator.translate("TestButtons.DownSignal"), new Icon(VaadinIcon.DOWNLOAD_ALT),
		        (e) -> {
			        new Thread(() -> {
						try {
							fop.getMqttMonitor().testDownSignal();
						} catch (MqttException e1) {
							LoggerUtils.logError(logger, e1);
						}
					}).start();
		        });
		testDown.getElement().setAttribute("theme", "primary");
		testDown.getElement().setAttribute("title", Translator.translate("ResumeCompetition"));

		HorizontalLayout buttons = new HorizontalLayout();
		if (fop != null && fop.getMqttMonitor().isActive()) {
			buttons.add(stopCompetition, endInterruption, testDown);
		} else {
			buttons.add(stopCompetition, endInterruption);
		}
		buttons.setSpacing(true);
		buttons.setMargin(false);
		buttons.setPadding(false);
		return buttons;
	}

}
