/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.NativeLabel;
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
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.video.StreamingEventMonitor;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.monitors.OBSMonitor;
import app.owlcms.nui.displays.attemptboards.PublicFacingAttemptBoardPage;
import app.owlcms.nui.displays.attemptboards.PublicFacingDecisionBoardPage;
import app.owlcms.nui.displays.scoreboards.CurrentAthletePage;
import app.owlcms.nui.displays.scoreboards.MedalsPage;
import app.owlcms.nui.displays.scoreboards.RankingsPage;
import app.owlcms.nui.displays.scoreboards.WarmupScoreboardPage;
import app.owlcms.nui.displays.scoreboards.WarmupMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.WarmupRankingOrderPage;
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
@Route(value = "video", layout = OwlcmsLayout.class)
public class VideoNavigationContent extends BaseNavigationContent
        implements NavigationPage, HasDynamicTitle, RequireDisplayLogin {

	final static Logger logger = (Logger) LoggerFactory.getLogger(VideoNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}
	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();
	private Category medalCategory;
	private Group medalGroup;

	/**
	 * Instantiates a new display navigation content.
	 */
	public VideoNavigationContent() {
		VerticalLayout intro = new VerticalLayout();
		intro.setSpacing(false);
		addP(intro, getTranslation("VideoStreaming.Intro"));
		addP(intro, getTranslation("Button_Open_Display"));
		intro.getStyle().set("margin-bottom", "0");
		fillH(intro, this);

		Button currentAthlete = openInNewTab(CurrentAthletePage.class, 
				getTranslation("CurrentAthleteTitle"), "video");
		Button attempt = openInNewTab(PublicFacingAttemptBoardPage.class, 
				getTranslation("AttemptBoard"), "video");
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete);
		doGroup(getTranslation("AttemptBoard"), grid3, this);

		Button publicDecisions = openInNewTab(PublicFacingDecisionBoardPage.class, 
				getTranslation("RefereeDecisions"), "video");
		FlexibleGridLayout grid31 = HomeNavigationContent.navigationGrid(publicDecisions);
		doGroup(getTranslation("RefereeDecisions"), grid31, this);

		Button scoreboard = openInNewTab(WarmupNoLeadersPage.class, getTranslation("Scoreboard"), "video");
		Button scoreboardWLeaders = openInNewTab(WarmupScoreboardPage.class, 
				getTranslation("ScoreboardWLeadersButton"), "video");
		scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
		Button scoreboardMultiRanks = openInNewTab(WarmupMultiRanksPage.class,
		        getTranslation("ScoreboardMultiRanksButton"), "video");
		Button scoreboardRankings = openInNewTab(WarmupRankingOrderPage.class,
		        getTranslation("Scoreboard.RankingOrderButton"), "video");

		List<Group> groups = GroupRepository.findAll();
		// more recent group first, else reverse order.
		groups.sort((g1, g2) -> {
			int compare = -ObjectUtils.compare(g1.getCompetitionTime(), g2.getCompetitionTime(), true);
			if (compare != 0)
				return compare;
			compare = -(new NaturalOrderComparator<Group>().compare(g1, g2));
			return compare;
		});
		FieldOfPlay curFop = OwlcmsSession.getFop();
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
		VerticalLayout intro1 = new VerticalLayout();
		// addP(intro1, getTranslation("darkModeSelect"));
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
		        scoreboardRankings,
		        scoreboardMultiRanks);
		doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

		Button medals = new Button(getTranslation("CeremonyType.MEDALS"));
		Button rankings = new Button(getTranslation("Scoreboard.RANKING"));
		medals.addClickListener((e) -> {
			Class<MedalsPage> class1 = MedalsPage.class;
			openClass(class1);
		});
		rankings.addClickListener((e) -> {
			Class<RankingsPage> class1 = RankingsPage.class;
			openClass(class1);
		});
		VerticalLayout intro1a = new VerticalLayout();
		// addP(intro1, getTranslation("darkModeSelect"));
		intro1a.add(hl);
		FlexibleGridLayout grid1a = HomeNavigationContent.navigationGrid(medals, rankings);
		doGroup(getTranslation("Scoreboard.RANKINGS"), intro1a, grid1a, this);

		Button obsMonitor = openInNewTab(OBSMonitor.class, getTranslation("OBS.MonitoringButton"));
		Button eventMonitor = openInNewTab(StreamingEventMonitor.class, getTranslation("Video.EventMonitoringButton"),
		        "video");
		VerticalLayout intro4 = new VerticalLayout();
		addP(intro4, getTranslation("Video.EventMonitoringExplanation", getTranslation("Video.EventMonitoringButton")));
		addP(intro4, getTranslation("OBS.MonitoringExplanation", getTranslation("OBS.MonitoringButton")));
		FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(eventMonitor, obsMonitor);
		doGroup(getTranslation("OBS.MonitoringButton"), intro4, grid4, this);

		DebugUtils.gc();
	}

	private void openClass(Class<?> class1) {
		Map<String, String> params = new TreeMap<>();
		Category medalCategory2 = getMedalCategory();
		if (medalCategory2 != null) {
			params.put("cat", medalCategory2.getCode().toString());
		} else if (getMedalGroup() != null) {
			params.put("group", getMedalGroup().toString());
		}
		QueryParameters qp = QueryParameters.simple(params);
		doOpenInNewTab(class1,
		        getTranslation("CeremonyType.MEDALS"),
		        "video",
		        qp);
	}

	private void selectVideoContext(Group g, Category c, FieldOfPlay fop) {
		Competition.getCurrent().computeMedals(g);
		fop.setVideoGroup(g);
		fop.setVideoCategory(c);
		setMedalGroup(g);
		setMedalCategory(c);
		logger.info("switching to {} {}", g.getName(), c != null ? c.getTranslatedName() : "");
		fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c));
	}

//	private void selectVideoContext(AgeGroup ag,Group g,  Category c, FieldOfPlay fop) {
//		Competition.getCurrent().computeMedals(g);
//		fop.setVideoAgeGroup(ag);
//		fop.setVideoGroup(g);
//		fop.setVideoCategory(c);
//		setMedalAgeGroup(ag);
//		setMedalCategory(c);
//		logger.info("switching to {} {} {}", ag.getName(), g.getName(), c != null ? c.getTranslatedName() : "");
//		fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c));
//	}
//
//	private void setMedalAgeGroup(AgeGroup ag) {
//		this.medalAgeGroup = ag;
//	}

	private void setMedalCategory(Category c) {
		this.medalCategory = c;
	}

	private void setMedalGroup(Group g) {
		this.medalGroup = g;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		RequireDisplayLogin.super.beforeEnter(event);
	}

	@Override
	public String getMenuTitle() {
		return getTranslation("VideoStreaming");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("VideoStreaming") + OwlcmsSession.getFopNameIfMultiple();
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
		OwlcmsSession.withFop((fop) -> {
			fopSelect.setValue(fop);
		});
		fopSelect.addValueChangeListener(e -> {
			OwlcmsSession.setFop(e.getValue());
			updateURLLocation(getLocationUI(), getLocation(), null);
		});

		HorizontalLayout fopField = new HorizontalLayout(fopLabel, fopSelect);
		fopField.setAlignItems(Alignment.CENTER);
		return fopField;
	}

	@SuppressWarnings("unused")
	private Group getMedalGroup() {
		return medalGroup;
	}

	private Category getMedalCategory() {
		return medalCategory;
	}
}
