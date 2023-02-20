/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.displayselection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.components.GroupCategorySelectionMenu;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.displays.attemptboard.PublicFacingDecisionBoard;
import app.owlcms.displays.monitor.OBSMonitor;
import app.owlcms.displays.scoreboard.CurrentAthlete;
import app.owlcms.displays.scoreboard.Results;
import app.owlcms.displays.scoreboard.ResultsLeadersRanks;
import app.owlcms.displays.scoreboard.ResultsMedals;
import app.owlcms.displays.scoreboard.ResultsNoLeaders;
import app.owlcms.displays.scoreboard.ResultsRankings;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.RequireDisplayLogin;
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
		Button currentAthlete = openInNewTab(CurrentAthlete.class, getTranslation("CurrentAthleteTitle"), "video");
		Button attempt = openInNewTab(AttemptBoard.class, getTranslation("AttemptBoard"), "video");
		Button publicDecisions = openInNewTab(PublicFacingDecisionBoard.class, getTranslation("RefereeDecisions"),
		        "video");

		fillH(intro, this);
		FlexibleGridLayout grid3 = HomeNavigationContent.navigationGrid(attempt, currentAthlete, publicDecisions);
		doGroup(getTranslation("AttemptBoard"), grid3, this);

		Button scoreboard;
		Button scoreboardWLeaders;
		Button scoreboardMultiRanks;
		scoreboard = openInNewTab(ResultsNoLeaders.class, getTranslation("Scoreboard"), "video");
		scoreboardWLeaders = openInNewTab(Results.class, getTranslation("ScoreboardWLeadersButton"), "video");
		scoreboardWLeaders.getElement().setAttribute("title", getTranslation("ScoreboardWLeadersMouseOver"));
		scoreboardMultiRanks = openInNewTab(ResultsLeadersRanks.class,
		        getTranslation("ScoreboardMultiRanksButton"), "video");

		List<Group> groups = GroupRepository.findAll();
		groups.sort(new NaturalOrderComparator<Group>());
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

		Button medals = new Button(getTranslation("CeremonyType.MEDALS"));
		medals.addClickListener((e) -> {
			Class<ResultsMedals> class1 = ResultsMedals.class;
	        openClass(class1);
		});
		Button rankings = new Button(getTranslation("Scoreboard.RANKING"));
		rankings.addClickListener((e) -> {
			Class<ResultsRankings> class1 = ResultsRankings.class;
	        openClass(class1);
		});

		VerticalLayout intro1 = new VerticalLayout();
		// addP(intro1, getTranslation("darkModeSelect"));
		FlexibleGridLayout grid1 = HomeNavigationContent.navigationGrid(scoreboard, scoreboardWLeaders,
		        scoreboardMultiRanks);
		doGroup(getTranslation("Scoreboards"), intro1, grid1, this);

		VerticalLayout intro1a = new VerticalLayout();
		// addP(intro1, getTranslation("darkModeSelect"));
		intro1a.add(hl);
		FlexibleGridLayout grid1a = HomeNavigationContent.navigationGrid(medals, rankings);
		doGroup(getTranslation("Scoreboard.RANKINGS"), intro1a, grid1a, this);

		Button obsMonitor = openInNewTab(OBSMonitor.class, getTranslation("OBS.MonitoringButton"));
		VerticalLayout intro4 = new VerticalLayout();
		addP(intro4, getTranslation("OBS.MonitoringExplanation"));
		FlexibleGridLayout grid4 = HomeNavigationContent.navigationGrid(obsMonitor);
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
		fop.setVideoGroup(g);
		fop.setVideoCategory(c);
		setMedalGroup(g);
		setMedalCategory(c);
	}

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
	public Location getLocation() {
		return this.location;
	}

	@Override
	public UI getLocationUI() {
		return this.locationUI;
	}

	@Override
	public String getMenuTitle() {
		return getTranslation("VideoStreaming");
	}

	@Override
	public String getPageTitle() {
		return getTranslation("VideoStreaming") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	@Override
	public void setLocation(Location location) {
		this.location = location;
	}

	@Override
	public void setLocationUI(UI locationUI) {
		this.locationUI = locationUI;
	}

	@Override
	public void setUrlParameterMap(Map<String, List<String>> newParameterMap) {
		this.urlParameterMap = newParameterMap;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * app.owlcms.nui.home.BaseNavigationContent#createTopBarFopField(java.lang.
	 * String, java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		Label fopLabel = new Label(label);
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
