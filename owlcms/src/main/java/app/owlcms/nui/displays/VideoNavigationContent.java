/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.vaadin.addons.tatu.ColorPicker;
import org.vaadin.addons.tatu.ColorPicker.ColorPreset;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.components.GroupCategorySelectionMenu;
import app.owlcms.data.category.Category;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.video.StreamingEventMonitor;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.OBSMonitor;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingDecisionBoardPage;
import app.owlcms.nui.displays.scoreboards.CurrentAthletePage;
import app.owlcms.nui.displays.scoreboards.JuryDecisionsPage;
import app.owlcms.nui.displays.scoreboards.MedalsPage;
import app.owlcms.nui.displays.scoreboards.NCurrentAthletePage;
import app.owlcms.nui.displays.scoreboards.PublicRankingOrderPage;
import app.owlcms.nui.displays.scoreboards.PublicStartListPage;
import app.owlcms.nui.displays.scoreboards.WarmupMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.WarmupScoreboardPage;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireDisplayLogin;
import app.owlcms.uievents.UIEvent;
import app.owlcms.utils.NaturalOrderComparator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class DisplayNavigationContent.
 */
@SuppressWarnings("serial")
@Route(value = "video=true", layout = OwlcmsLayout.class)
public class VideoNavigationContent extends BaseNavigationContent
        implements NavigationPage, HasDynamicTitle, RequireDisplayLogin {

	final static Logger logger = (Logger) LoggerFactory.getLogger(VideoNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<>();
	private Category medalCategory;
	private Group medalGroup;

	/**
	 * Instantiates a new display navigation content.
	 */
	public VideoNavigationContent() {
		VerticalLayout intro = new VerticalLayout();
		intro.setSpacing(false);
		addP(intro, Translator.translate("VideoStreaming.Intro"));
		addP(intro, Translator.translate("Button_Open_Display"));
		intro.getStyle().set("margin-bottom", "0");
		fillH(intro, this);

		colorOverride();

		attemptBoard();
		startList();
		scoreboards();
		decisions();
		rankings();
		monitoring();

		DebugUtils.gc();
	}

	public void monitoring() {
		Button obsMonitor = openInNewTab(OBSMonitor.class, Translator.translate("OBS.MonitoringButton"));
		Button eventMonitor = openInNewTabWithFopQueryParameters(StreamingEventMonitor.class,
		        Translator.translate("Video.EventMonitoringButton"),
		        "video=true");
		VerticalLayout intro4 = new VerticalLayout();
		addP(intro4, Translator.translate("Video.EventMonitoringExplanation", Translator.translate("Video.EventMonitoringButton")));
		addP(intro4, Translator.translate("OBS.MonitoringExplanation", Translator.translate("OBS.MonitoringButton")));
		FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(eventMonitor, obsMonitor);
		doGroup(Translator.translate("OBS.MonitoringButton"), intro4, grid4, this);
	}

	public void rankings() {
		List<Group> groups = GroupRepository.findAll();
		// more recent group first, else reverse order.
		groups.sort((g1, g2) -> {
			int compare = -ObjectUtils.compare(g1.getCompetitionTime(), g2.getCompetitionTime(), true);
			if (compare != 0) {
				return compare;
			}
			compare = -(new NaturalOrderComparator<Group>().compare(g1, g2));
			return compare;
		});
		// Get FOP with fallbacks - getFop() may be null during construction before URL params are processed
		FieldOfPlay curFop = getFop();
		if (curFop == null) {
			curFop = OwlcmsSession.getFop();
		}
		if (curFop == null) {
			curFop = OwlcmsFactory.getDefaultFOP();
		}
		GroupCategorySelectionMenu groupCategorySelectionMenu = new GroupCategorySelectionMenu(groups, curFop,
		        // group has been selected
		        (g1, c1, fop1) -> selectVideoContext(g1, c1, fop1),
		        // no group
		        (g1, c1, fop1) -> selectVideoContext(null, c1, fop1));
		Checkbox includeNotCompleted = new Checkbox();
		includeNotCompleted.addValueChangeListener(e -> {
			groupCategorySelectionMenu.setIncludeNotCompleted(e.getValue());
			groupCategorySelectionMenu.recompute();
		});
		includeNotCompleted.setLabel(Translator.translate("Video.includeNotCompleted"));
		HorizontalLayout hl = new HorizontalLayout();
		hl.add(groupCategorySelectionMenu, includeNotCompleted);
		Button medals = new Button(Translator.translate("CeremonyType.MEDALS"));
		Button rankings = new Button(Translator.translate("Scoreboard.RANKING"));
		medals.addClickListener((e) -> {
			Class<MedalsPage> class1 = MedalsPage.class;
			openInNewTabWithResultsQueryParameters(class1);
		});
		rankings.addClickListener((e) -> {
			Class<PublicRankingOrderPage> class1 = PublicRankingOrderPage.class;
			openInNewTabWithResultsQueryParameters(class1, true);
		});
		VerticalLayout intro1a = new VerticalLayout();
		// addP(intro1, Translator.translate("darkModeSelect"));
		intro1a.add(hl);
		FlexibleGridLayout grid1a = HomeNavigationContent.navigationGrid(medals, rankings);
		doGroup(Translator.translate("Scoreboard.RankingOrder"), intro1a, grid1a, this);
	}

	public void decisions() {
		Button publicDecisions = openInNewTabWithFopQueryParameters(PublicFacingDecisionBoardPage.class,
		        Translator.translate("RefereeDecisions"), "video=true");
		Button juryDecisions = openInNewTabWithFopQueryParameters(JuryDecisionsPage.class,
		        Translator.translate("JuryDecisions.Title"), "video=true");
		FlexibleGridLayout grid31 = HomeNavigationContent.navigationGrid(publicDecisions, juryDecisions);
		doGroup(Translator.translate("RefereeDecisions"), grid31, this);
	}

	public void scoreboards() {
		Button scoreboard = openInNewTabWithFopQueryParameters(WarmupNoLeadersPage.class,
		        Translator.translate("Scoreboard"), "video=true&currentAttempt=false");
		Button scoreboardWLeaders = openInNewTabWithFopQueryParameters(WarmupScoreboardPage.class,
		        Translator.translate("ScoreboardWLeadersButton"), "video=true&currentAttempt=false");
		scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
		Button scoreboardMultiRanks = openInNewTabWithFopQueryParameters(WarmupMultiRanksPage.class,
		        Translator.translate("ScoreboardMultiRanksButton"), "video=true&currentAttempt=false");
		Button scoreboardRankings = openInNewTabWithFopQueryParameters(PublicRankingOrderPage.class,
		        Translator.translate("Scoreboard.RankingOrderButton"), "video=true&currentAttempt=false&showMedals=true");

		VerticalLayout intro1 = new VerticalLayout();
		// addP(intro1, Translator.translate("darkModeSelect"));
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
		        scoreboardRankings,
		        scoreboardMultiRanks);
		doGroup(Translator.translate("Scoreboards"), intro1, grid1, this);
	}

	public void startList() {
		Button startList = openInNewTabWithFopQueryParameters(PublicStartListPage.class,
		        Translator.translate("Scoreboard.StartList"), "video=true");
		FlexibleGridLayout gridIntro = HomeNavigationContent.navigationGrid(startList);
		doGroup(Translator.translate("CeremonyType.INTRODUCTION"), gridIntro, this);
	}

	public void attemptBoard() {
		FlexibleGridLayout grid3;
		Button attempt = openInNewTabWithFopQueryParameters(PublicFacingAttemptBoardPage.class,
		        Translator.translate("AttemptBoard"), "video=true");
		if (Config.getCurrent().featureSwitch("iwfLook")) {
			Button nCurrentAthlete = openInNewTabWithFopQueryParameters(NCurrentAthletePage.class,
			        Translator.translate("CurrentAthleteTitle") + " (New)", "video=true");
			grid3 = HomeNavigationContent.navigationGrid(nCurrentAthlete, attempt);
		} else {
			Button currentAthlete = openInNewTabWithFopQueryParameters(CurrentAthletePage.class,
			        Translator.translate("CurrentAthleteTitle"), "video=true");
			grid3 = HomeNavigationContent.navigationGrid(currentAthlete, attempt);
		}
		doGroup(Translator.translate("AttemptBoard"), grid3, this);
	}

	public void colorOverride() {
		boolean enableColorOverrides = Config.getCurrent().getEnableColorOverrides();
		Checkbox enableColorOverrideCheckbox = new Checkbox(enableColorOverrides);
		enableColorOverrideCheckbox.setMaxWidth("40%");
		ColorPicker colorPicker = new ColorPicker();
		colorPicker.setEnabled(enableColorOverrides);

		enableColorOverrideCheckbox.addClickListener(event -> {
			boolean selected = Boolean.TRUE.equals(enableColorOverrideCheckbox.getValue());
			Config.getCurrent().setEnableColorOverrides(selected);
			colorPicker.setEnabled(selected);
			logger.debug("selected {}", selected);
		});
		enableColorOverrideCheckbox.setLabel(Translator.translate("ColorSelection.EnabledLabel"));
		enableColorOverrideCheckbox.setHelperText(Translator.translate("ColorSelection.EnabledHelperText"));

		colorPicker.setLabel(Translator.translate("ColorSelection.Label"));
		colorPicker.setMaxWidth("40%");
		colorPicker.setHelperText(Translator.translate("ColorSelection.Helper"));
		colorPicker
		        .setPresets(Arrays.asList(
		                new ColorPreset("#000000", "Black"),
		                new ColorPreset("#696969", "Dim Grey"),
		                new ColorPreset("#8b0000", "Dark Red"),
		                new ColorPreset("#006400", "Dark Green"),
		                new ColorPreset("#00008b", "Dark Blue")));

		colorPicker.addValueChangeListener(event -> {
			Config.getCurrent().setVideoColorOverrides("--videoHeaderBackgroundColor: " + event.getValue());
			Notification.show(event.getValue());
		});

		VerticalLayout intro5 = new VerticalLayout();
		intro5.setSpacing(false);
		intro5.add(new Div(Translator.translate("ColorSelection.Intro")));
		intro5.setMargin(false);
		intro5.setPadding(false);
		HorizontalLayout horizontalLayout = new HorizontalLayout(enableColorOverrideCheckbox, colorPicker);
		horizontalLayout.setMargin(false);
		horizontalLayout.setAlignItems(Alignment.CENTER);
		intro5.add(horizontalLayout);

		doGroup(Translator.translate("ColorSelection"), intro5, new FlexibleGridLayout(), this);

	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		RequireDisplayLogin.super.beforeEnter(event);
	}

	@Override
	public String getMenuTitle() {
		return Translator.translate("VideoStreaming");
	}

	// private void setMedalAgeGroup(AgeGroup ag) {
	// this.medalAgeGroup = ag;
	// }

	@Override
	public String getPageTitle() {
		String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
		return Translator.translate("VideoStreaming") + suffix;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang. String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		NativeLabel fopLabel = new NativeLabel(label);
		formatLabel(fopLabel);

		ComboBox<FieldOfPlay> fopSelect = createFopSelect(placeHolder);
		fopSelect.setValue(getFop());
		fopSelect.addValueChangeListener(e -> {
			setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}

	private Category getMedalCategory() {
		return this.medalCategory;
	}

	@SuppressWarnings("unused")
	private Group getMedalGroup() {
		return this.medalGroup;
	}

	private void openInNewTabWithResultsQueryParameters(Class<?> class1) {
		openInNewTabWithResultsQueryParameters(class1, false);
	}

	private void openInNewTabWithResultsQueryParameters(Class<?> class1, boolean showMedals) {
		Map<String, String> params = new TreeMap<>();
		Category medalCategory2 = getMedalCategory();
		Group groupToUse = null;
		
		// Determine which group to use
		if (medalCategory2 != null) {
			params.put("cat", medalCategory2.getCode().toString());
		} else {
			// Try medal group first (set via menu selection)
			groupToUse = getMedalGroup();
			
			// If no medal group, try FOP's video group
			if (groupToUse == null) {
				FieldOfPlay fop = getFop();
				if (fop != null) {
					groupToUse = fop.getVideoGroup();
					// If no video group, fall back to FOP's current group
					if (groupToUse == null) {
						groupToUse = fop.getGroup();
					}
				}
			}
			
			if (groupToUse != null) {
				params.put("group", groupToUse.getName());
			}
		}
		
		FieldOfPlay fop = getFop();
		if (fop != null) {
			params.put("fop", fop.getName());
		}
		params.put("video", "true");
		if (showMedals) {
			params.put("showMedals", "true");
		}
		QueryParameters qp = QueryParameters.simple(params);
		doOpenInNewTab(class1,
		        Translator.translate("CeremonyType.MEDALS"),
		        null,
		        qp);
	}

	private void selectVideoContext(Group g, Category c, FieldOfPlay fop) {
		Competition.getCurrent().computeMedals(g);
		fop.setVideoGroup(g);
		fop.setVideoCategory(c);
		setMedalGroup(g);
		setMedalCategory(c);
		logger.info("switching {} video to {} {}", fop, g != null ? g.getName() : null, c != null ? c.getNameWithAgeGroup() : "");
		fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c, fop));
	}

	private void setMedalCategory(Category c) {
		this.medalCategory = c;
	}

	private void setMedalGroup(Group g) {
		this.medalGroup = g;
	}
}
