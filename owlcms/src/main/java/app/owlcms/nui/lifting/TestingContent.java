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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.details.Details;
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
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.crudui.OwlcmsCrudFormFactory;
import app.owlcms.nui.shared.AthleteGridContent;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.util.Timer;
import java.util.TimerTask;


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

	@Override
	protected void onDetach(com.vaadin.flow.component.DetachEvent detachEvent) {
		super.onDetach(detachEvent);
		if (connectionsTimer != null) {
			try {
				connectionsTimer.cancel();
			} catch (Throwable t) {
				// ignore
			}
			connectionsTimer = null;
		}
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
 	 private Div devicesContainer;
 	 private Timer connectionsTimer;
    	private Button refreshClientsBtn;
    	private Details connectionsDetails;

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
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("TestButtons.Title") + suffix;
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
			// When a break starts, enable resume and disable start testing
			resumeCompetition.setEnabled(true);
			if (startTesting != null) startTesting.setEnabled(false);
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

		try {
			var all = app.owlcms.monitors.MQTTMonitor.getAllMonitorSummaries();
			var allPubs = app.owlcms.monitors.MQTTMonitor.getAllActivePublishers();
			if ((all == null || all.isEmpty()) && (allPubs == null || allPubs.isEmpty())) {
				logger.debug("No MQTT monitors configured");
			} else {
				for (var entry : all.entrySet()) {
					var name = entry.getKey();
					var summary = entry.getValue();
					var pubs = (allPubs != null && allPubs.containsKey(name)) ? allPubs.get(name) : java.util.List.of();
					logger.debug("MQTT monitor {} => {} publishers={}", name, summary, pubs);
				}
			}
		} catch (Exception e) {
			app.owlcms.utils.LoggerUtils.logError(logger, e);
		}
		
		setCrudFormFactory(createFormFactory());
		HorizontalLayout bts = createInterruptionButtons();
		// container that will hold device boxes displayed above the buttons
		devicesContainer = new Div();
	devicesContainer.getStyle().set("display", "flex");
	devicesContainer.getStyle().set("flex-wrap", "wrap");
	devicesContainer.getStyle().set("gap", "4px");
	// no outer frame; keep only spacing
	devicesContainer.getStyle().set("margin-bottom", "4px");
	devicesContainer.getStyle().set("padding", "2px");

	// Refresh button only (clear removed) — create it before the Details so it's not null
	refreshClientsBtn = new Button(Translator.translate("Refresh"), e -> {
			new Thread(() -> {
				try {
					var globalClients = app.owlcms.monitors.MQTTMonitor.getGlobalActiveClientIds();
					java.util.List<String> ids = getFilteredDeviceIds(globalClients);
					int cnt = ids.size();
						ui.access(() -> {
						populateDevicesContainer(ids);
						connectionsDetails.setSummaryText(Translator.translate("MQTT.ConnectedDevices", cnt));
					});
				} catch (Throwable t) {
					app.owlcms.utils.LoggerUtils.logError(logger, t);
				}
			}).start();
		});

	// Collapsible details that contains the device list and the refresh button; hidden by default
	String title = Translator.translate("MQTT.ConnectedDevices", countConnectedDevices());
	com.vaadin.flow.component.orderedlayout.VerticalLayout vl = new com.vaadin.flow.component.orderedlayout.VerticalLayout(devicesContainer, refreshClientsBtn);
	vl.setPadding(false);
	vl.setSpacing(false);
	vl.getStyle().set("padding", "2px 0 0 0");
	connectionsDetails = new Details(title, vl);
	connectionsDetails.setOpened(false);
	// make the details title bolder by applying a style to the summary element
	connectionsDetails.getElement().getStyle().set("font-weight", "600");
	// smaller spacing under the details section and left margin for alignment
	connectionsDetails.getStyle().set("margin-bottom", "6px");
	// apply left margin to revealed content (the vertical layout) instead of the header
	vl.getStyle().set("margin-left", "2em");

		// schedule periodic refresh of only the connected devices enumeration while attached
		connectionsTimer = new Timer("mqtt-connections-refresh", true);
		connectionsTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					var globalClients = app.owlcms.monitors.MQTTMonitor.getGlobalActiveClientIds();
					java.util.List<String> ids = getFilteredDeviceIds(globalClients);
					int cnt = ids.size();
					ui.access(() -> {
						populateDevicesContainer(ids);
						connectionsDetails.setSummaryText(Translator.translate("MQTT.ConnectedDevices", cnt));
					});
				} catch (Throwable t) {
					// defensive: log only, avoid throwing from timer
					app.owlcms.utils.LoggerUtils.logError(logger, t);
				}
			}
		}, 0, 5000);

        
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
			// expandable details section
			connectionsDetails,
			bts,
			lineLog,
			eventsLog);
		events.setSizeUndefined();
		events.setHeight("100%");
	events.getStyle().set("margin-top", "0.5em");
		events.getStyle().set("flex-direction", "column");
		events.setWidth("95vw");
		fillHW(events, this);

	}



	/** Return a sorted, numbered enumeration of connected devices from global client ids. */
	private java.util.List<String> getFilteredDeviceIds(java.util.Collection<?> globalClients) {
		java.util.List<String> list = new java.util.ArrayList<>();
		if (globalClients == null || globalClients.isEmpty()) return list;
		java.util.Map<String, String> descriptors = app.owlcms.monitors.MQTTMonitor.getConnectionDescriptorsSnapshot();
		for (Object o : globalClients) {
			if (o == null) continue;
			String s = o.toString();
			if (s.contains("_owlcms_") || s.toLowerCase().contains("configmonitor")) continue;
			// hide config connections: raw id starts with config_f OR descriptor starts with 'config'
			try {
				if (s.toLowerCase().startsWith("config_f")) continue;
				String desc = lookupDescriptorForId(descriptors, s);
				if (desc != null && desc.toLowerCase().startsWith("config")) continue;
			} catch (Throwable t) {
				// ignore and fall back to showing the id
			}
			list.add(s);
		}
		java.util.Collections.sort(list);
		return list;
	}

	private void populateDevicesContainer(java.util.List<String> ids) {
		devicesContainer.removeAll();
	// snapshot descriptors to avoid repeated lookups while rendering
	java.util.Map<String, String> descriptors = app.owlcms.monitors.MQTTMonitor.getConnectionDescriptorsSnapshot();
	java.util.Map<String, Long> lastSeen = app.owlcms.monitors.MQTTMonitor.getConnectionLastSeenSnapshot();
		for (int i = 0; i < ids.size(); i++) {
			String id = ids.get(i);
			// Prefer descriptor (friendly name) if available, otherwise fall back to id
			// no remote address available anymore
			String display = id;
			String desc = lookupDescriptorForId(descriptors, id);
			if (desc != null && !desc.isBlank()) {
				display = desc;
			}

			// append concise last-seen if available (use permissive lookup)
			Long ts = lookupLastSeenForId(lastSeen, id);
			if (ts != null) {
				String ago = formatAgo(ts);
				if (ago != null && !ago.isBlank()) display = display + " (" + ago + ")";
			}

			// no remote IP available; omit address
			Div box = new Div();
			box.getStyle().set("width", "30ch");
			box.getStyle().set("padding", "6px");
			box.getStyle().set("border", "none");
			box.getStyle().set("border-radius", "4px");
			box.getStyle().set("box-sizing", "border-box");
			box.getStyle().set("white-space", "nowrap");
			box.getStyle().set("overflow", "hidden");
			box.getStyle().set("text-overflow", "ellipsis");
			// If the display (descriptor) differs from the raw id, show descriptor then raw id on next line
			if (!display.equals(id)) {
				box.getStyle().set("white-space", "normal");
				box.setText((i+1) + ". " + display + "\n" + id);
				box.getStyle().set("font-size", "0.95em");
			} else {
				box.setText((i+1) + ". " + display);
			}
			// make connection name non-bold
			box.getStyle().set("font-weight", "normal");
			devicesContainer.add(box);
		}
	}

	// remote lookup removed

	/**
	 * Permissive lookup for descriptor: exact key, key startsWith id, id startsWith key, or key contains id.
	 */
	private String lookupDescriptorForId(java.util.Map<String, String> descriptors, String id) {
		if (descriptors == null || id == null) return null;
		if (descriptors.containsKey(id)) return descriptors.get(id);
		// prefer keys that start with the id (e.g., broker id variants)
		for (String k : descriptors.keySet()) {
			if (k != null && k.startsWith(id)) return descriptors.get(k);
		}
		// prefer keys that id starts with (id is a longer variant)
		for (String k : descriptors.keySet()) {
			if (k != null && id.startsWith(k)) return descriptors.get(k);
		}
		// fallback to contains
		for (String k : descriptors.keySet()) {
			if (k != null && k.contains(id)) return descriptors.get(k);
		}
		return null;
	}

	/**
	 * Permissive lookup for lastSeen timestamp using the same heuristic as descriptors.
	 */
	private Long lookupLastSeenForId(java.util.Map<String, Long> lastSeen, String id) {
		if (lastSeen == null || id == null) return null;
		if (lastSeen.containsKey(id)) return lastSeen.get(id);
		for (String k : lastSeen.keySet()) {
			if (k != null && k.startsWith(id)) return lastSeen.get(k);
		}
		for (String k : lastSeen.keySet()) {
			if (k != null && id.startsWith(k)) return lastSeen.get(k);
		}
		for (String k : lastSeen.keySet()) {
			if (k != null && k.contains(id)) return lastSeen.get(k);
		}
		return null;
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
			startTesting.setEnabled(false);
		});
		resumeCompetition.addClickListener(e -> {
			resumeCompetition.setEnabled(false);
			startTesting.setEnabled(true);
		});
		
		boolean inBreak = (getFop() != null && getFop().getState() == FOPState.BREAK);
		startTesting.setEnabled(!inBreak);
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
		// add a bit of space above this button row
		buttons.getStyle().set("margin-top", "0.6em");
		return buttons;
	}

	private int countConnectedDevices() {
		var globalClients = app.owlcms.monitors.MQTTMonitor.getGlobalActiveClientIds();
		if (globalClients == null || globalClients.isEmpty()) return 0;
		java.util.Map<String, String> descriptors = app.owlcms.monitors.MQTTMonitor.getConnectionDescriptorsSnapshot();
		int count = 0;
		for (Object o : globalClients) {
			if (o == null) continue;
			String s = o.toString();
			if (s.contains("_owlcms_") || s.toLowerCase().contains("configmonitor")) continue;
			try {
				if (s.toLowerCase().startsWith("config_f")) continue;
				String desc = lookupDescriptorForId(descriptors, s);
				if (desc != null && desc.toLowerCase().startsWith("config")) continue;
			} catch (Throwable t) {
				// ignore and count the client
			}
			count++;
		}
		return count;
	}

	private String formatAgo(Long ts) {
		if (ts == null) return "";
		long diff = System.currentTimeMillis() - ts;
		if (diff < 1000L) return "just now";
		long seconds = diff / 1000L;
		if (seconds < 60L) return seconds + "s ago";
		long minutes = seconds / 60L;
		if (minutes < 60L) return minutes + "m ago";
		long hours = minutes / 60L;
		return hours + "h ago";
	}

	
}
