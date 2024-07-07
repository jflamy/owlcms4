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
import app.owlcms.nui.displays.scoreboards.WarmupMultiRanksPage;
import app.owlcms.nui.displays.scoreboards.WarmupNoLeadersPage;
import app.owlcms.nui.displays.scoreboards.WarmupRankingOrderPage;
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

		Button currentAthlete = openInNewTabQueryParameters(CurrentAthletePage.class,
		        Translator.translate("CurrentAthleteTitle"), "video=true");
		Button attempt = openInNewTabQueryParameters(PublicFacingAttemptBoardPage.class,
		        Translator.translate("AttemptBoard"), "video=true");
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete);
		doGroup(Translator.translate("AttemptBoard"), grid3, this);

		Button publicDecisions = openInNewTabQueryParameters(PublicFacingDecisionBoardPage.class,
		        Translator.translate("RefereeDecisions"), "video=true");
		FlexibleGridLayout grid31 = HomeNavigationContent.navigationGrid(publicDecisions);
		doGroup(Translator.translate("RefereeDecisions"), grid31, this);

		Button scoreboard = openInNewTabQueryParameters(WarmupNoLeadersPage.class,
		        Translator.translate("Scoreboard"), "video=true");
		Button scoreboardWLeaders = openInNewTabQueryParameters(WarmupScoreboardPage.class,
		        Translator.translate("ScoreboardWLeadersButton"), "video=true");
		scoreboardWLeaders.getElement().setAttribute("title", Translator.translate("ScoreboardWLeadersMouseOver"));
		Button scoreboardMultiRanks = openInNewTabQueryParameters(WarmupMultiRanksPage.class,
		        Translator.translate("ScoreboardMultiRanksButton"), "video=true");
		Button scoreboardRankings = openInNewTabQueryParameters(WarmupRankingOrderPage.class,
		        Translator.translate("Scoreboard.RankingOrderButton"), "video=true");

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
		// addP(intro1, Translator.translate("darkModeSelect"));
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
		        scoreboardRankings,
		        scoreboardMultiRanks);
		doGroup(Translator.translate("Scoreboards"), intro1, grid1, this);

		Button medals = new Button(Translator.translate("CeremonyType.MEDALS"));
		Button rankings = new Button(Translator.translate("Scoreboard.RANKING"));
		medals.addClickListener((e) -> {
			Class<MedalsPage> class1 = MedalsPage.class;
			openInNewTabWithResultsQueryParameters(class1);
		});
		rankings.addClickListener((e) -> {
			Class<RankingsPage> class1 = RankingsPage.class;
			openInNewTabWithResultsQueryParameters(class1);
		});
		VerticalLayout intro1a = new VerticalLayout();
		// addP(intro1, Translator.translate("darkModeSelect"));
		intro1a.add(hl);
		FlexibleGridLayout grid1a = HomeNavigationContent.navigationGrid(medals, rankings);
		doGroup(Translator.translate("Scoreboard.RANKINGS"), intro1a, grid1a, this);

		Button obsMonitor = openInNewTab(OBSMonitor.class, Translator.translate("OBS.MonitoringButton"));
		Button eventMonitor = openInNewTabQueryParameters(StreamingEventMonitor.class,
		        Translator.translate("Video.EventMonitoringButton"),
		        "video=true");
		VerticalLayout intro4 = new VerticalLayout();
		addP(intro4, Translator.translate("Video.EventMonitoringExplanation", Translator.translate("Video.EventMonitoringButton")));
		addP(intro4, Translator.translate("OBS.MonitoringExplanation", Translator.translate("OBS.MonitoringButton")));
		FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(eventMonitor, obsMonitor);
		doGroup(Translator.translate("OBS.MonitoringButton"), intro4, grid4, this);

		DebugUtils.gc();
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
		return Translator.translate("VideoStreaming") + OwlcmsSession.getFopNameIfMultiple();
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

	private Category getMedalCategory() {
		return this.medalCategory;
	}

	@SuppressWarnings("unused")
	private Group getMedalGroup() {
		return this.medalGroup;
	}

	private void openInNewTabWithResultsQueryParameters(Class<?> class1) {
		Map<String, String> params = new TreeMap<>();
		Category medalCategory2 = getMedalCategory();
		if (medalCategory2 != null) {
			params.put("cat", medalCategory2.getCode().toString());
		} else if (getMedalGroup() != null) {
			params.put("group", getMedalGroup().toString());
		}
		params.put("video", "true");
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
		logger.info("switching to {} {}", g.getName(), c != null ? c.getNameWithAgeGroup() : "");
		fop.getUiEventBus().post(new UIEvent.VideoRefresh(this, g, c, getFop()));
	}

	private void setMedalCategory(Category c) {
		this.medalCategory = c;
	}

	private void setMedalGroup(Group g) {
		this.medalGroup = g;
	}
}
