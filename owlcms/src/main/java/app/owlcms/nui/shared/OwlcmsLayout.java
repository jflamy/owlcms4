package app.owlcms.nui.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.flowingcode.vaadin.addons.ironicons.HardwareIcons;
import com.flowingcode.vaadin.addons.ironicons.IronIcons;
import com.flowingcode.vaadin.addons.ironicons.MapsIcons;
import com.flowingcode.vaadin.addons.ironicons.PlacesIcons;
import com.flowingcode.vaadin.addons.ironicons.SocialIcons;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.IronIcon;
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
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displayselection.DisplayNavigationContent;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * The main view is a top-level placeholder for other views.
 */
@SuppressWarnings("serial")
public class OwlcmsLayout extends AppLayout {

    public static final String LARGE = "text-l";
    public static final String NONE = "m-0";
    Logger logger = (Logger) LoggerFactory.getLogger(OwlcmsLayout.class);
    private Label viewTitle;

    protected List<Component> navBarComponents;

    private DrawerToggle drawerToggle;
    private ComboBox<Locale> localeDropDown;
    private FlexLayout buttonArea;

    public OwlcmsLayout() {
        logger.warn("***** creating layout");
        navBarComponents = new ArrayList<>();
        populateNavBar();

        // setPrimarySection(Section.DRAWER);
        addDrawerContent();

    }

    @Override
    public void addToNavbar(boolean touchOptimized, Component... components) {
        logger.warn("***** adding1 {} from {}", components, LoggerUtils.whereFrom());
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(touchOptimized, components);
    }

    @Override
    public void addToNavbar(Component... components) {
        logger.warn("***** adding2 {} from {}", components, LoggerUtils.whereFrom());
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(components);
    }

    public void closeDrawer() {
        setDrawerOpened(false);
    }

//    public FlexLayout getButtonArea() {
//        // FIXME
//        if (navBarComponents.size() <= 2) {
//            navBarComponents.add(new FlexLayout());
//        }
//        return (FlexLayout) navBarComponents.get(navBarComponents.size() - 1);
//    }

    public FlexLayout getButtonArea() {
        return buttonArea;
    }

    public DrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public ComboBox<Locale> getLocaleDropDown() {
        return localeDropDown;
    }

    /**
     * @return the navBarComponents
     */
    public List<Component> getNavBarComponents() {
        return navBarComponents;
    }

    public Label getViewTitle() {
        return viewTitle;
    }

    @Override
    public void remove(Component... components) {
        for (Component c : components) {
            if (navBarComponents.contains(c)) {
                navBarComponents.remove(c);
            }
        }
        super.remove(components);
    }

    public void setMenuVisible(boolean hamburgerShown) {
        // FIXME no way to hide hamburger?
    }

    /**
     * @param navBarComponents the navBarComponents to set
     */
    public void setNavBarComponents(List<Component> navBarComponents) {
        this.navBarComponents = navBarComponents;
    }

    public void setTopBarTitle(String topBarTitle) {
        getViewTitle().setText(topBarTitle);
    }

    public void showLocaleDropdown(boolean b) {
        getLocaleDropDown().getStyle().set("display", b ? "block" : "none");
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        if (content instanceof OwlcmsLayoutAware) {
            logger.warn("***** aware showRouterLayoutContent {}", content);
            OwlcmsLayoutAware appContent = (OwlcmsLayoutAware) content;
            appContent.setRouterLayout(this);
            super.showRouterLayoutContent(content);
            appContent.setHeaderContent();
        } else {
            logger.warn("***** NOT aware showRouterLayoutContent {}", content);
            super.showRouterLayoutContent(content);
            populateNavBar();
        }

    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        setTopBarTitle(getCurrentPageTitle());
    }

    protected void populateNavBar() {
        HorizontalLayout topBar = new HorizontalLayout();
        setDrawerToggle(new DrawerToggle());
        getDrawerToggle().getElement().setAttribute("aria-label", "Menu drawerToggle");
        setViewTitle(new Label());
        Style style = getViewTitle().getElement().getStyle();
        style.set("font-size", "large");
        style.set("margin-left", "0");
        setButtonArea(createButtonArea());
        setLocaleDropDown(createLocaleDropdown());
        topBar.setMargin(true);
        topBar.add(getDrawerToggle(), getViewTitle(), getButtonArea(), getLocaleDropDown());
        topBar.setFlexGrow(1.0D, getButtonArea());
        topBar.setWidth("100%");
        topBar.setAlignItems(Alignment.CENTER);

        logger.warn("***** OwlcmsLayout set HeaderContent from {}", LoggerUtils.whereFrom());
        clearNavBar();
        addToNavbar(false, topBar);
    }

    protected void setButtonArea(FlexLayout horizontalLayout) {
        this.buttonArea = horizontalLayout;
    }

    private void addDrawerContent() {
//        H3 appName = new H3("TBD");
//        appName.addClassNames(LARGE, NONE);
//        Header header = new Header(appName);
//        header.getStyle().set("margin-left", "1em");
//
//        VerticalLayout createNavigation = createNavigation();
//        createNavigation.setWidth("90%");
//        Scroller scroller = new Scroller(createNavigation);

        Tabs tabs = getTabs();

        addToDrawer(tabs);
//        addToDrawer(header, scroller, createFooter());
    }

    private void clearNavBar() {
        for (Component c : getNavBarComponents()) {
            super.remove(c);
        }
        navBarComponents.clear();
    }

//    private Footer createFooter() {
//        Footer layout = new Footer();
//
//        return layout;
//    }

    private ComboBox<Locale> createLocaleDropdown() {
        ComboBox<Locale> sessionLocaleField = new ComboBox<>();
        sessionLocaleField.setWidth("24ch");
        sessionLocaleField.setClearButtonVisible(true);
        sessionLocaleField.setDataProvider(new ListDataProvider<>(Translator.getUsefulLocales()));
        sessionLocaleField.setItemLabelGenerator((locale) -> locale.getDisplayName(locale));
        sessionLocaleField.setValue(Translator.getLocaleSupplier().get());
        sessionLocaleField.addValueChangeListener(e -> {
            OwlcmsSession.getCurrent().setLocale(e.getValue());
            UI.getCurrent().getPage().reload();
        });
        return sessionLocaleField;
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }

    private void setDrawerToggle(DrawerToggle drawerToggle) {
        this.drawerToggle = drawerToggle;
    }

    private void setLocaleDropDown(ComboBox<Locale> localeDropDown) {
        this.localeDropDown = localeDropDown;
    }

    private void setViewTitle(Label viewTitle) {
        this.viewTitle = viewTitle;
    }

    private Tabs getTabs() {
        Tabs tabs = new Tabs();
        String docOpener = "javascript:window.open('https://jflamy.github.io/owlcms4/#/index','_blank')";
        tabs.add(
                createTab(IronIcons.HOME.create(), 
                        Translator.translate("Home"), 
                        HomeNavigationContent.class),
                createTab(SocialIcons.GROUP_ADD.create(), 
                        Translator.translate("PrepareCompetition"),
                        PreparationNavigationContent.class),
                createTab(PlacesIcons.FITNESS_CENTER.create(), 
                        Translator.translate("RunLiftingGroup"),
                        LiftingNavigationContent.class),
                createTab(HardwareIcons.DESKTOP_WINDOWS.create(), 
                        Translator.translate("StartDisplays"),
                        DisplayNavigationContent.class),
                createTab(MapsIcons.LOCAL_PRINTSHOP.create(), 
                        Translator.translate("Results"),
                        ResultsNavigationContent.class),
                createTab(IronIcons.HELP.create(), 
                        Translator.translate("Documentation_Menu"),
                        docOpener
                        ),
                createTab(IronIcons.INFO_OUTLINE.create(), 
                        Translator.translate("About"),
                        PreparationNavigationContent.class)
//                createTab(IronIcons.SETTINGS.create(), 
//                        Translator.translate("Preferences"),
//                        PreparationNavigationContent.class)
                );

        Translator.translate("RunLiftingGroup");
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        return tabs;
    }

    private Tab createTab(IronIcon viewIcon, String viewName,
            String docOpener) {
        Anchor a = new Anchor();
        a.setHref(docOpener);
        a.add(viewIcon);
        // copied from router-link
        viewIcon.getElement().setAttribute("style", "padding: var(--lumo-space-xs); margin-inline-end: var(--lumo-space-m); box-sizing: border-box;");
        a.add(new Span(viewName));
        a.getElement().setAttribute("router-link", true);

        return new Tab(a);
    }

    private Tab createTab(IronIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        viewIcon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(viewIcon, new Span(viewName));
        // Demo has no routes
        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }

    /**
     * Create the top bar.
     *
     * Note: the top bar is created before the content.
     *
     * @see #showRouterLayoutContent(HasElement) for how to content to layout and vice-versa
     *
     * @param topBar
     */
    protected FlexLayout createButtonArea() {
        FlexLayout hLayout = new FlexLayout();
        hLayout.setFlexDirection(FlexDirection.ROW);
        return hLayout;
    }

}
