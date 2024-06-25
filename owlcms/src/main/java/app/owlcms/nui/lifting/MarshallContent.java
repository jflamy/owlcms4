/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/

package app.owlcms.nui.lifting;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
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
@Route(value = "lifting/marshall", layout = OwlcmsLayout.class)
public class MarshallContent extends AthleteGridContent implements HasDynamicTitle {

	// @SuppressWarnings("unused")
	final private static Logger logger = (Logger) LoggerFactory.getLogger(MarshallContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();

	public MarshallContent() {
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "true",
		        SoundParameters.DOWNSILENT, "true",
		        SoundParameters.IMMEDIATE, "true",
		        SoundParameters.SINGLEREF, "false")));
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
			logger.trace("{}findAll {} {} {}", FieldOfPlay.getLoggingName(fop),
			        fop.getGroup() == null ? null : fop.getGroup().getName(),
			        LoggerUtils.whereFrom());
			final String filterValue;
			if (this.lastNameFilter.getValue() != null) {
				filterValue = this.lastNameFilter.getValue().toLowerCase();
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

	@Override
	public String getMenuTitle() {
		return getPageTitle();
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return Translator.translate("Marshall") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Subscribe
	public void slaveRefereeDecision(UIEvent.Decision e) {
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
			hideLiveDecisions();

			int d = e.decision ? 1 : 0;
			String text = Translator.translate("NoLift_GoodLift", d, e.getAthlete().getFullName());

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
		UIEventProcessor.uiAccess(this, this.uiEventBus, e, () -> {
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
		HorizontalLayout buttons = new HorizontalLayout(this.stopTimeButton);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	/**
	 * @see app.owlcms.nui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(FlexLayout announcerBar) {
		HorizontalLayout decisions = new HorizontalLayout();
		return decisions;
	}

}
