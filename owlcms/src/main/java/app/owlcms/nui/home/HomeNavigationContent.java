/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.nui.home;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import com.github.appreciated.css.grid.GridLayoutComponent.AutoFlow;
import com.github.appreciated.css.grid.GridLayoutComponent.Overflow;
import com.github.appreciated.css.grid.sizes.Flex;
import com.github.appreciated.css.grid.sizes.Length;
import com.github.appreciated.css.grid.sizes.MinMax;
import com.github.appreciated.css.grid.sizes.Repeat;
import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.DebugUtils;
import app.owlcms.i18n.Translator;
import app.owlcms.nui.displayselection.DisplayNavigationContent;
import app.owlcms.nui.displayselection.VideoNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import app.owlcms.nui.shared.BaseNavigationContent;
import app.owlcms.nui.shared.NavigationPage;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.utils.IPInterfaceUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * The Class HomeNavigationContent.
 */
/**
 * @author owlcms
 *
 */
@SuppressWarnings("serial")
@Route(value = "", layout = OwlcmsLayout.class)
public class HomeNavigationContent extends BaseNavigationContent implements NavigationPage, HasDynamicTitle {

	final private static Logger logger = (Logger) LoggerFactory.getLogger(HomeNavigationContent.class);
	static {
		logger.setLevel(Level.INFO);
	}

	/**
	 * Navigation crudGrid.
	 *
	 * @param items the items
	 * @return the flexible crudGrid layout
	 */
	public static FlexibleGridLayout navigationGrid(Component... items) {
		FlexibleGridLayout layout = new FlexibleGridLayout();
		layout.withColumns(Repeat.RepeatMode.AUTO_FILL, new MinMax(new Length("300px"), new Flex(1)))
		        .withAutoRows(new Length("1fr")).withItems(items)
		        .withOverflow(Overflow.AUTO).withAutoFlow(AutoFlow.ROW).withMargin(false).withPadding(true)
		        .withSpacing(false);
		layout.getContent().setGap(new Length("0.5em"), new Length("1.0em"));
		layout.setSizeUndefined();
		layout.setWidth("80%");
		layout.setBoxSizing(BoxSizing.BORDER_BOX);
		layout.setPadding(true);
		return layout;
	}

	String INFO = Translator.translate("About");
	String PREPARE_COMPETITION = Translator.translate("PrepareCompetition");
	String RESULT_DOCUMENTS = Translator.translate("Results");
	String RUN_LIFTING_GROUP = Translator.translate("RunLiftingGroup");
	String VIDEO_STREAMING = Translator.translate("VideoStreaming");

	String START_DISPLAYS = Translator.translate("StartDisplays");

	Map<String, List<String>> urlParameterMap = new HashMap<String, List<String>>();

	/**
	 * Instantiates a new main navigation content.
	 */
	public HomeNavigationContent() {
		VerticalLayout intro = buildIntro();
		intro.setSpacing(false);

		Button prepare = new Button(PREPARE_COMPETITION,
		        buttonClickEvent -> UI.getCurrent().navigate(PreparationNavigationContent.class));
		Button displays = new Button(START_DISPLAYS,
		        buttonClickEvent -> UI.getCurrent().navigate(DisplayNavigationContent.class));
		Button video = new Button(VIDEO_STREAMING,
		        buttonClickEvent -> UI.getCurrent().navigate(VideoNavigationContent.class));
		Button lifting = new Button(RUN_LIFTING_GROUP,
		        buttonClickEvent -> UI.getCurrent().navigate(LiftingNavigationContent.class));
		Button documents = new Button(RESULT_DOCUMENTS,
		        buttonClickEvent -> UI.getCurrent().navigate(ResultsNavigationContent.class));
		FlexibleGridLayout grid = HomeNavigationContent.navigationGrid(prepare, lifting, displays, video, documents);

		fillH(intro, this);
		fillH(grid, this);

		DebugUtils.gc();
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
		return getTranslation("OWLCMS_Top");
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#getPageTitle()
	 */
	@Override
	public String getPageTitle() {
		return getTranslation("OWLCMS_Top");
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {
		return urlParameterMap;
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#isIgnoreFopFromURL()
	 */
	@Override
	public boolean isIgnoreFopFromURL() {
		return true;
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

	private VerticalLayout buildIntro() {
		VerticalLayout intro = new VerticalLayout();
		intro.setSpacing(false);
		intro.setId("homeIntro");
		IPInterfaceUtils urlFinder = new IPInterfaceUtils();
		urlFinder.checkRequest();
		addP(intro, getTranslation("SystemURL"));
		UnorderedList ul = new UnorderedList();
		for (String url : urlFinder.getRecommended()) {
			ul.add(new ListItem(new Anchor(url, url)));
		}
		for (String url : urlFinder.getWired()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("Wired"))));
		}
		for (String url : urlFinder.getWireless()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("Wireless"))));
		}
		for (String url : urlFinder.getNetworking()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel("")));
		}
		for (String url : urlFinder.getLocalUrl()) {
			ul.add(new ListItem(new Anchor(url, url), new NativeLabel(getTranslation("LocalComputer"))));
		}
		intro.add(ul);
		Div div = new Div();
		intro.add(div);
		div.getStyle().set("margin-bottom", "1ex");
		Hr hr = new Hr();
		hr.getStyle().set("margin-bottom", "2ex");
		intro.add(hr);
		addP(intro,
		        getTranslation("LeftMenuNavigate")
		                + getTranslation("PrepareCompatition_description", PREPARE_COMPETITION)
		                + getTranslation("RunLiftingGroup_description", RUN_LIFTING_GROUP)
		                + getTranslation("StartDisplays_description", START_DISPLAYS)
		                + getTranslation("VideoStreaming_description", VIDEO_STREAMING)
		                + getTranslation("CompetitionDocuments_description", RESULT_DOCUMENTS)
		                + getTranslation("SeparateLaptops"));
		intro.getStyle().set("margin-bottom", "-1em");
		return intro;
	}

	/**
	 * @see app.owlcms.nui.shared.BaseNavigationContent#createMenuBarFopField(java.lang.String,
	 *      java.lang.String)
	 */
	@Override
	protected HorizontalLayout createMenuBarFopField(String label, String placeHolder) {
		return null;
	}
}