/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.ui.group;

import org.slf4j.LoggerFactory;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;
import com.github.appreciated.app.layout.behaviour.AppLayout;
import com.github.appreciated.app.layout.behaviour.Behaviour;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.components.appLayout.AppLayoutContent;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.attemptboard.TimerElement;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.state.UIEvent;
import app.owlcms.ui.home.MainNavigationLayout;
import app.owlcms.ui.home.SafeEventBusRegistration;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerLayout.
 */
@SuppressWarnings("serial")
@HtmlImport("frontend://bower_components/vaadin-lumo-styles/presets/compact.html")
@HtmlImport("frontend://styles/shared-styles.html")
@Theme(Lumo.class)
@Push
public abstract class BaseLayout extends MainNavigationLayout implements SafeEventBusRegistration, UIEventProcessor {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(BaseLayout.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI"+logger.getName());

	protected H3 title;
	protected H1 lastName;
	protected H2 firstName;
	protected Html attempt;
	protected H2 weight;
	protected TimerElement timeField;
	protected HorizontalLayout announcerBar;
	//protected HorizontalLayout request;
	protected EventBus uiEventBus;

	protected ComboBox<Group> groupSelect;
	
	/**
	 * gridGroupFilter points to a hidden field on the top bar of the grid.
	 * Changing groupSelect changes this slave filter; this allows us to use the filtering
	 * logic used everywhere else to change what is shown in the grid.
	 */
	protected ComboBox<Group> gridGroupFilter;

	
	public BaseLayout() {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	@Subscribe
	public void setTime(UIEvent.SetTime e) {
		UIEventProcessor.uiAccess(announcerBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);//timeField.setValue(msToString(timeRemaining));
		});
	}

	@Subscribe
	public void startTime(UIEvent.StartTime e) {
		UIEventProcessor.uiAccess(announcerBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);//timeField.setValue(msToString(timeRemaining));
		});
	}

	@Subscribe
	public void stopTime(UIEvent.StopTime e) {
		UIEventProcessor.uiAccess(announcerBar, uiEventBus, e, () -> {
			Integer timeRemaining = e.getTimeRemaining();
			timeField.setTimeRemaining(timeRemaining);//timeField.setValue(msToString(timeRemaining));
		});
	}

	@Subscribe
	public void updateAnnouncerBar(UIEvent.LiftingOrderUpdated e) {
		UIEventProcessor.uiAccess(announcerBar, uiEventBus, e, () -> {
			Athlete athlete = e.getAthlete();
			Integer timeAllowed = e.getTimeAllowed();
			doUpdateAnnouncerBar(athlete, timeAllowed);
		});
	}
	
	protected void doUpdateAnnouncerBar(Athlete athlete, Integer timeAllowed) {
		syncWithFOP();
		if (athlete != null) {
			lastName.setText(athlete.getLastName());
			firstName.setText(athlete.getFirstName());
			timeField.setTimeRemaining(timeAllowed);//timeField.setValue(msToString(timeAllowed));
			Html newAttempt = new Html(
					"<h2>" + athlete.getAttemptNumber() + "<sup>st</sup> att.</h2>");
			announcerBar.replace(attempt, newAttempt);
			attempt = newAttempt;
			Integer nextAttemptRequestedWeight = athlete.getNextAttemptRequestedWeight();
			weight.setText((nextAttemptRequestedWeight != null ? nextAttemptRequestedWeight.toString() :"\u2013")+ "kg");
		} else {
			lastName.setText("\u2013");
			firstName.setText("");
			Html newAttempt = new Html("<h2><span></span></h2>");
			announcerBar.replace(attempt, newAttempt);
			attempt = newAttempt;
			weight.setText("");
		}
	}

	public void syncWithFOP() {
		OwlcmsSession.withFop((fop) -> {
			Group fopGroup = fop.getGroup();
			Group displayedGroup = groupSelect.getValue();
			if (fopGroup == null && displayedGroup == null) return;
			if (fopGroup != null && ! fopGroup.equals(displayedGroup)) {
				groupSelect.setValue(fopGroup);
			} else if (fopGroup == null) {
				groupSelect.setValue(null);
			}
		});
	}
	
	protected EventBus getFopEventBus() {
		return OwlcmsSession.getFop().getEventBus();
	}
	

	protected void createTopBar(HorizontalLayout announcerBar) {	
		title = new H3();
		title.setText("Announcer");
		title.getStyle()
			.set("margin", "0px 0px 0px 0px")
			.set("font-weight", "normal");
		
		groupSelect = new ComboBox<>();
		groupSelect.setPlaceholder("Select Group");
		groupSelect.setItems(GroupRepository.findAll());
		groupSelect.setItemLabelGenerator(Group::getName);
		groupSelect.setWidth("8rem");
		
		syncWithFOP();
//		OwlcmsSession.withFop((fop) -> {
//			groupSelect.setValue(fop.getGroup());
//		});
		groupSelect.addValueChangeListener(e -> {
			gridGroupFilter.setValue(e.getValue());
		});
		
		lastName = new H1();
		lastName.setText("\u2013");
		lastName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		firstName = new H2("");
		firstName.getStyle()
			.set("margin", "0px 0px 0px 0px");
		Div fullName = new Div(
				lastName,
				firstName);

		attempt = new Html("<h2><span></span></h2");
		weight = new H2();
		weight.setText("");

		timeField = new TimerElement();
		timeField.setTimeRemaining(0);
		H1 time = new H1(timeField);
		
		HorizontalLayout buttons = announcerButtons(announcerBar);

		HorizontalLayout decisions = decisionButtons(announcerBar);

		decisions.setAlignItems(FlexComponent.Alignment.BASELINE);

		announcerBar
			.getElement()
			.getStyle()
			.set("flex", "100 1");
		announcerBar.removeAll();
		announcerBar.add(title, groupSelect, fullName, attempt, weight, time, buttons, decisions);
		announcerBar.setJustifyContentMode(FlexComponent.JustifyContentMode.AROUND);
		announcerBar.setAlignItems(FlexComponent.Alignment.CENTER);
		announcerBar.setAlignSelf(Alignment.CENTER, attempt, weight, time);
		announcerBar.setFlexGrow(0.5, fullName);
	}



	/* (non-Javadoc)
	 * @see app.owlcms.ui.home.MainNavigationLayout#getLayoutConfiguration(com.github.appreciated.app.layout.behaviour.Behaviour)
	 */
	@Override
	protected AppLayout getLayoutConfiguration(Behaviour variant) {
		AppLayout appLayout = super.getLayoutConfiguration(variant);
		this.announcerBar = ((AbstractLeftAppLayoutBase) appLayout).getAppBarElementWrapper();
		createTopBar(announcerBar);
		appLayout.getTitleWrapper()
			.getElement()
			.getStyle()
			.set("flex", "0 1 0px");
		return appLayout;
	}
	
	/**
	 * The layout is created before the content. This routine has created the content, we can refer to
	 * the content using {@link #getLayoutContent()} and the content can refer to us via
	 * {@link AppLayoutContent#getParentLayout()}
	 * 
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayoutBase#showRouterLayoutContent(com.vaadin.flow.component.HasElement)
	 */
	@Override
	public void showRouterLayoutContent(HasElement content) {
		super.showRouterLayoutContent(content);
		BaseContent baseContent = (BaseContent) getLayoutContent();
		baseContent.setParentLayout(this);
		gridGroupFilter = baseContent.getGroupFilter();
	}

	/* (non-Javadoc)
	 * @see com.github.appreciated.app.layout.router.AppLayoutRouterLayout#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		OwlcmsSession.withFop(fop -> {
			// sync with current status of FOP
			doUpdateAnnouncerBar(fop.getCurAthlete(), fop.getTimeAllowed());
			// connect to bus for new updating events
			uiEventBus = uiEventBusRegister(this, fop);
		});
	}

	protected abstract HorizontalLayout announcerButtons(HorizontalLayout announcerBar);

	protected abstract HorizontalLayout decisionButtons(HorizontalLayout announcerBar);
}
