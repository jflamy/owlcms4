/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */

package app.owlcms.ui.lifting;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.components.elements.DecisionElement;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.fieldofplay.FOPEvent;
import app.owlcms.fieldofplay.UIEvent;
import app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.lifting.BreakDialog.BreakType;
import app.owlcms.ui.shared.AthleteGridContent;
import app.owlcms.ui.shared.AthleteGridLayout;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Class AnnouncerContent.
 */
@SuppressWarnings("serial")
@Route(value = "lifting/jury", layout = AthleteGridLayout.class)
public class JuryContent extends AthleteGridContent implements HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(JuryContent.class);
	final private static Logger uiEventLogger = (Logger) LoggerFactory.getLogger("UI" + logger.getName());
	private static final String BUTTON_WIDTH = "10em";
	static {
		logger.setLevel(Level.INFO);
		uiEventLogger.setLevel(Level.INFO);
	}

	private DecisionElement decisions;
	private HorizontalLayout juryVotingButtons;
	private Icon[] juryIcons;
	private Boolean[] juryVotes;
	private int nbJurors;
	private VerticalLayout juryVotingContainer;

	public JuryContent() {
		//  we don't actually inherit behaviour from the superclass because
		// all this does is call init() -- which we override.
		super();
	}


	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#add(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete add(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#delete(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public void delete(Athlete Athlete) {
		// do nothing;
	}

	/**
	 * @see com.vaadin.flow.router.HasDynamicTitle#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return "Jury";
	}

	@Subscribe
	public void slaveDown(UIEvent.DownSignal e) {
		// Ignore down signal
	}

	@Override
	@Subscribe
	public void slaveGroupDone(UIEvent.GroupDone e) {
		uiEventLogger.debug("### {} {} {} {}", this.getClass().getSimpleName(), e.getClass().getSimpleName(),
				this.getOrigin(), e.getOrigin());
		OwlcmsSession.withFop((fop) -> doUpdateTopBar(fop.getCurAthlete(), 0));
	}
	
	@Subscribe
	public void slaveTimeStarted(UIEvent.StartTime e) {
		juryVotingButtons.removeAll();
		resetTop();
		resetJuryVoting();
	}

	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#update(app.owlcms.data.athlete.Athlete)
	 */
	@Override
	public Athlete update(Athlete athlete) {
		// do nothing
		return athlete;
	}

	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#updateGrid(app.owlcms.fieldofplay.UIEvent.LiftingOrderUpdated)
	 */
	@Override
	public void updateGrid(LiftingOrderUpdated e) {
		// ignore
	}


	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#announcerButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout announcerButtons(HorizontalLayout announcerBar) {
		// moved down to the jury section
		return new HorizontalLayout(); // juryDeliberationButtons();
	}


	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#createTopBar()
	 */
	@Override
	protected void createTopBar() {
		super.createTopBar();
		// this hides the back arrow
		getAppLayout().setMenuVisible(false);
	}

	/**
	 * @see app.owlcms.ui.shared.AthleteGridContent#decisionButtons(com.vaadin.flow.component.orderedlayout.HorizontalLayout)
	 */
	@Override
	protected HorizontalLayout decisionButtons(HorizontalLayout announcerBar) {
		// moved down to the jury section
		return new HorizontalLayout(); // juryDecisionButtons();
	}


	@Override
	protected void init() {
		this.setBoxSizing(BoxSizing.BORDER_BOX);
		this.setSizeFull();
		setTopBarTitle("Jury");
		
		nbJurors = 3;
		buildJuryBox(this);
		buildRefereeBox(this);
	}


	private Icon bigIcon(VaadinIcon iconDef, String color) {
		Icon icon = iconDef.create();
		icon.setSize("80%");
		icon.getStyle().set("color", color);
		return icon;
	}

	private void buildJuryBox(VerticalLayout juryContainer) {
		HorizontalLayout topRow = new HorizontalLayout();
		Label juryLabel = new Label("Jury Decisions");
		H3 labelWrapper = new H3(juryLabel);
		labelWrapper.setWidth("15em");
		topRow.add(labelWrapper, juryDeliberationButtons(), juryDecisionButtons());
		topRow.setAlignItems(Alignment.CENTER);
		juryContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		juryContainer.setMargin(false);
		juryContainer.add(topRow);
		
		buildJuryVoting();
		resetJuryVoting();
		juryContainer.add(juryVotingContainer);
		juryContainer.setAlignSelf(Alignment.CENTER, juryVotingContainer);
	}

	private void buildJuryVoting() {
		juryVotingButtons = new HorizontalLayout();
		juryVotingButtons.setBoxSizing(BoxSizing.BORDER_BOX);
		juryVotingButtons.setJustifyContentMode(JustifyContentMode.EVENLY);
		juryVotingButtons.setAlignItems(Alignment.CENTER);
		juryVotingButtons.setWidth(nbJurors == 3 ? "50%" : "85%");
		juryVotingButtons.getStyle().set("background-color", "black");
		juryVotingButtons.setPadding(false);
		juryVotingButtons.setMargin(false);
		
		juryVotingContainer = new VerticalLayout();
		juryVotingContainer.setHeight("35vh");
		juryVotingContainer.setWidthFull();
		juryVotingContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		juryVotingContainer.setAlignItems(Alignment.CENTER);
		juryVotingContainer.setPadding(true);
		juryVotingContainer.setMargin(true);
		juryVotingContainer.getStyle().set("background-color", "black");
		
		juryVotingContainer.add(juryVotingButtons);
		juryVotingContainer.setAlignSelf(Alignment.CENTER, juryVotingButtons);
		return;
	}

	private void buildRefereeBox(VerticalLayout container) {
		Label label = new Label("Referee Decisions");
		H3 labelWrapper = new H3(label);
		labelWrapper.setHeight("5%");
		container.add(labelWrapper);

		decisions = new DecisionElement();
		decisions.setJury(true);

		Div decisionWrapper = new Div(decisions);
		decisionWrapper.getStyle().set("width", "50%");
		decisionWrapper.getStyle().set("height", "30vh");

		HorizontalLayout refContainer = new HorizontalLayout(decisionWrapper);
		refContainer.setBoxSizing(BoxSizing.BORDER_BOX);
		refContainer.setJustifyContentMode(JustifyContentMode.CENTER);
		refContainer.getStyle().set("background-color", "black");
		refContainer.setHeight("35vh");
		refContainer.setWidthFull();
		refContainer.setPadding(true);
		refContainer.setMargin(true);

		container.add(refContainer);
		container.setAlignSelf(Alignment.CENTER, refContainer);
	}

	private void checkAllVoted() {
		boolean allVoted = true;
		for (int i = 0; i < juryVotes.length; i++) {
			if (juryVotes[i] == null) {
				allVoted = false;
				break;
			}
		}
		
		if (allVoted) {
			for (int i = 0; i < juryVotes.length; i++) {
				if (juryVotes[i]) {
					Icon fullSizeIcon = bigIcon(VaadinIcon.CHECK_CIRCLE, "white");
					juryVotingButtons.replace(juryIcons[i],fullSizeIcon);
					juryIcons[i] = fullSizeIcon;
				} else {
					Icon fullSizeIcon = bigIcon(VaadinIcon.CLOSE_CIRCLE, "red");
					juryVotingButtons.replace(juryIcons[i],fullSizeIcon);
					juryIcons[i] = fullSizeIcon;
				}
			}
		}
	}

	private Key getBadKey(int i) {
		switch (i) {
		case 0:
			return Key.DIGIT_2;
		case 1:
			return Key.DIGIT_4;
		case 2:
			return Key.DIGIT_6;
		case 3:
			return Key.DIGIT_8;
		case 4:
			return Key.DIGIT_0;
		default:
			return Key.UNIDENTIFIED;
		}
	}

	private Key getGoodKey(int i) {
		switch (i) {
		case 0:
			return Key.DIGIT_1;
		case 1:
			return Key.DIGIT_3;
		case 2:
			return Key.DIGIT_5;
		case 3:
			return Key.DIGIT_7;
		case 4:
			return Key.DIGIT_9;
		default:
			return Key.UNIDENTIFIED;
		}
	}

	private HorizontalLayout juryDecisionButtons() {
		Button good = new Button(IronIcons.DONE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.JuryDecision(fop.getCurAthlete(), this.getOrigin(), true));
			});
		});
		good.getElement().setAttribute("theme", "success");
		good.setWidth(BUTTON_WIDTH);

		Button bad = new Button(IronIcons.CLOSE.create(), (e) -> {
			OwlcmsSession.withFop(fop -> {
				fop.getFopEventBus().post(new FOPEvent.JuryDecision(fop.getCurAthlete(), this.getOrigin(), false));
			});
		});
		bad.getElement().setAttribute("theme", "error");
		bad.setWidth(BUTTON_WIDTH);

		HorizontalLayout decisions = new HorizontalLayout(good, bad);
		return decisions;
	}


	private HorizontalLayout juryDeliberationButtons() {
		Button stopCompetition = new Button("Stop Competition", (e) -> {
			OwlcmsSession.withFop(
					fop -> fop.getFopEventBus().post(new FOPEvent.BreakStarted(BreakType.JURY, 0, this.getOrigin())));
		});
		stopCompetition.getElement().setAttribute("theme", "secondary");
		stopCompetition.setWidth(BUTTON_WIDTH);

		Button resumeCompetition = new Button("Resume Competition", (e) -> {
			OwlcmsSession.withFop(fop -> fop.getFopEventBus().post(new FOPEvent.StartLifting(this.getOrigin())));
		});
		resumeCompetition.getElement().setAttribute("theme", "secondary");
		resumeCompetition.setWidth(BUTTON_WIDTH);

		Button breakButton = new Button(IronIcons.ALARM.create(), (e) -> {
			(new BreakDialog(this)).open();
		});
		breakButton.getElement().setAttribute("theme", "icon");
		breakButton.getElement().setAttribute("title", "Break Timer");
		HorizontalLayout buttons = new HorizontalLayout(stopCompetition, resumeCompetition);
		buttons.setAlignItems(FlexComponent.Alignment.BASELINE);
		return buttons;
	}

	private void resetJuryVoting() {
		juryIcons = new Icon[nbJurors];
		juryVotes = new Boolean[nbJurors];
		for (int i = 0; i < nbJurors; i++) {
			final int ix = i;
			Icon nonVotedIcon = bigIcon(VaadinIcon.CIRCLE_THIN, "gray");
			juryIcons[ix] = nonVotedIcon;
			juryVotes[ix] = null;
			juryVotingButtons.add(juryIcons[ix], nonVotedIcon);
			UI.getCurrent().addShortcutListener(() -> {
				Icon votedIcon = bigIcon(VaadinIcon.CIRCLE, "gray");
				juryVotingButtons.replace(juryIcons[ix], votedIcon);
				juryIcons[ix] = votedIcon;
				juryVotes[ix] = true;
				checkAllVoted();
			}, getGoodKey(i));
			UI.getCurrent().addShortcutListener(() -> {
				Icon votedIcon = bigIcon(VaadinIcon.CIRCLE, "gray");
				juryVotingButtons.replace(juryIcons[ix], votedIcon);
				juryIcons[ix] = votedIcon;
				juryVotes[ix] = false;
				checkAllVoted();
			}, getBadKey(i));
		}
	}

	private void resetTop() {
		// TODO Auto-generated method stub
	}

}
