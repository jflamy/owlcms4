package app.owlcms.nui.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.FlexLayout.FlexDirection;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouterLink;

import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsFactory;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.DisplayNavigationContent;
import app.owlcms.nui.displays.VideoNavigationContent;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.home.InfoNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import ch.qos.logback.classic.Logger;

/**
 * The main view is a top-level placeholder for other views.
 */
@SuppressWarnings({ "serial", "deprecation" })
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
public class OwlcmsLayout extends AppLayout {

	public static final String LARGE = "text-l";
	public static final String NONE = "m-0";
	Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsLayout.class);
	private Component viewTitle;
	protected List<Component> navBarComponents;
	private DrawerToggle drawerToggle;
	private ComboBox<Locale> localeDropDown;
	private FlexLayout menuArea;
	private HorizontalLayout header;
	private boolean margin;

	public OwlcmsLayout() {
		OwlcmsFactory.waitDBInitialized();
		this.navBarComponents = new ArrayList<>();
		// create default empty components. Content will fill them in.
		populateHeader();
		// setPrimarySection(Section.DRAWER);
		addDrawerContent();
		setContent(null);
	}

	@Override
	public void addToNavbar(boolean touchOptimized, Component... components) {
		this.navBarComponents.addAll(Arrays.asList(components));
		super.addToNavbar(touchOptimized, components);
	}

	@Override
	public void addToNavbar(Component... components) {
		this.navBarComponents.addAll(Arrays.asList(components));
		super.addToNavbar(components);
	}

	public void closeDrawer() {
		setDrawerOpened(false);
	}

	public DrawerToggle getDrawerToggle() {
		return this.drawerToggle;
	}

	public ComboBox<Locale> getLocaleDropDown() {
		return this.localeDropDown;
	}

	public FlexLayout getMenuArea() {
		return this.menuArea;
	}

	/**
	 * @return the navBarComponents
	 */
	public List<Component> getNavBarComponents() {
		return this.navBarComponents;
	}

	public Component getViewTitle() {
		return this.viewTitle;
	}

	@Override
	public void remove(Component... components) {
		for (Component c : components) {
			if (this.navBarComponents.contains(c)) {
				this.navBarComponents.remove(c);
			}
		}
		super.remove(components);
	}

	public void setMenuArea(FlexLayout horizontalLayout) {
		this.menuArea = horizontalLayout;
	}

	public void setMenuTitle(Component topBarTitle) {
		Component curTitle = getViewTitle();
		remove(curTitle);
		setViewTitle(topBarTitle);
		updateHeader(this.margin);
	}

	public void setMenuTitle(String topBarTitle) {
		if (getViewTitle() instanceof NativeLabel) {
			((NativeLabel) getViewTitle()).setText(topBarTitle);
		} else {
			setMenuTitle(new NativeLabel("topBarTitle"));
		}
	}

	public void setMenuVisible(boolean hamburgerShown) {
		getDrawerToggle().setVisible(hamburgerShown);
	}

	/**
	 * @param navBarComponents the navBarComponents to set
	 */
	public void setNavBarComponents(List<Component> navBarComponents) {
		this.navBarComponents = navBarComponents;
	}

	public void showLocaleDropdown(boolean b) {
		getLocaleDropDown().getStyle().set("display", b ? "flex" : "none");
	}

	@Override
	public void showRouterLayoutContent(HasElement content) {
		if (content instanceof OwlcmsLayoutAware) {
			OwlcmsLayoutAware appContent = (OwlcmsLayoutAware) content;
			appContent.setPadding(false);
			appContent.getStyle().set("padding", "0 1em 0.5em");
			appContent.setRouterLayout(this);
			super.showRouterLayoutContent(content);
			appContent.setHeaderContent();
		} else {
			super.showRouterLayoutContent(content);
			populateHeader();
		}

	}

	public void updateHeader(boolean margin) {
		this.margin = margin;
		this.header.removeAll();
		this.header.setMargin(margin);
		this.header.setPadding(false);
		this.header.add(getDrawerToggle(), getViewTitle(), getMenuArea(), getLocaleDropDown());
		this.header.setFlexGrow(1.0D, getMenuArea());
		this.header.setWidth("100%");
		this.header.setAlignItems(Alignment.CENTER);

		clearNavBar();
		addToNavbar(false, this.header);
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		setMenuTitle(getCurrentPageTitle());
	}

	/**
	 * Create the top bar.
	 *
	 * Note: the top bar is created before the content.
	 *
	 * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
	 *
	 * @param header
	 */
	protected FlexLayout createMenuArea() {
		FlexLayout hLayout = new FlexLayout();
		hLayout.setFlexDirection(FlexDirection.ROW);
		return hLayout;
	}

	protected void populateHeader() {
		this.header = new HorizontalLayout();
		this.header.setPadding(false);
		this.header.setSpacing(false);

		setDrawerToggle(new DrawerToggle());
		getDrawerToggle().getElement().setAttribute("aria-label", "Menu drawerToggle");
		getDrawerToggle().setSizeUndefined();

		setViewTitle(new NativeLabel());
		Style style = getViewTitle().getElement().getStyle();
		style.set("font-size", "large");
		style.set("padding-right", "1em");

		setMenuArea(createMenuArea());

		setLocaleDropDown(createLocaleDropdown());

		updateHeader(true);
	}

	private void addDrawerContent() {
		Tabs tabs = getTabs();
		addToDrawer(tabs);
	}

	private void clearNavBar() {
		for (Component c : getNavBarComponents()) {
			try {
				super.remove(c);
			} catch (Exception e) {
				this.logger.error("removing component not present.");
			}
		}
		this.navBarComponents.clear();
	}

	private ComboBox<Locale> createLocaleDropdown() {
		ComboBox<Locale> sessionLocaleField = new ComboBox<>();
		// sessionLocaleField.setWidth("24ch");
		sessionLocaleField.setClearButtonVisible(true);
		List<Locale> usefulLocales = Translator.getUsefulLocales();
		sessionLocaleField.setItems(new ListDataProvider<>(usefulLocales));
		sessionLocaleField.setItemLabelGenerator((locale) -> locale.getDisplayName(locale));
		sessionLocaleField.setValue(Translator.getLocaleSupplier().get());
		sessionLocaleField.addValueChangeListener(e -> {
			OwlcmsSession.getCurrent().setLocale(e.getValue());
			UI.getCurrent().getPage().reload();
		});
		return sessionLocaleField;
	}

	@SuppressWarnings("deprecation")
	private Tab createTab(Icon viewIcon, String viewName, Class<? extends Component> viewClass) {
		viewIcon.getStyle().set("box-sizing", "border-box")
		        .set("margin-inline-end", "var(--lumo-space-m)")
		        .set("padding", "var(--lumo-space-xs)");

		RouterLink link = new RouterLink();
		link.add(viewIcon, new Span(viewName));
		link.setRoute(viewClass);
		link.setTabIndex(-1);

		return new Tab(link);
	}

	@SuppressWarnings("deprecation")
	private Tab createTab(Icon viewIcon, String viewName,
	        String docOpener) {
		Anchor a = new Anchor();
		a.setHref(docOpener);
		a.add(viewIcon);
		// copied from router-link
		viewIcon.getElement().setAttribute("style",
		        "padding: var(--lumo-space-xs); margin-inline-end: var(--lumo-space-m); box-sizing: border-box;");
		a.add(new Span(viewName));
		a.getElement().setAttribute("router-link", true);

		return new Tab(a);
	}

	private String getCurrentPageTitle() {
		PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
		return title == null ? "" : title.value();
	}

	private Tabs getTabs() {
		Tabs tabs = new Tabs();
		String docOpener = "javascript:window.open('https://jflamy.github.io/owlcms4/#/index','_blank')";
		// boolean tv = new OwlcmsLicense().isFeatureAllowed("tv");
		tabs.add(
		        createTab(new Icon(VaadinIcon.HOME),
		                Translator.translate("Home"),
		                HomeNavigationContent.class),
		        createTab(new Icon(VaadinIcon.GROUP),
		                Translator.translate("PrepareCompetition"),
		                PreparationNavigationContent.class),
		        createTab(new Icon(VaadinIcon.MICROPHONE),
		                Translator.translate("RunLiftingGroup"),
		                LiftingNavigationContent.class),
		        createTab(new Icon(VaadinIcon.DESKTOP),
		                Translator.translate("StartDisplays"),
		                DisplayNavigationContent.class));
		// if (tv) {
		tabs.add(
		        createTab(new Icon(VaadinIcon.MOVIE),
		                Translator.translate("VideoStreaming"),
		                VideoNavigationContent.class));
		// }
		tabs.add(
		        createTab(new Icon(VaadinIcon.PRINT),
		                Translator.translate("Results"),
		                ResultsNavigationContent.class),
		        createTab(new Icon(VaadinIcon.QUESTION_CIRCLE),
		                Translator.translate("Documentation_Menu"),
		                docOpener),
		        createTab(new Icon(VaadinIcon.INFO_CIRCLE_O),
		                Translator.translate("About"),
		                InfoNavigationContent.class));

		Translator.translate("RunLiftingGroup");
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
		return tabs;
	}

	private void setDrawerToggle(DrawerToggle drawerToggle) {
		this.drawerToggle = drawerToggle;
	}

	private void setLocaleDropDown(ComboBox<Locale> localeDropDown) {
		this.localeDropDown = localeDropDown;
	}

	private void setViewTitle(Component topBarTitle) {
		this.viewTitle = topBarTitle;
	}

}
