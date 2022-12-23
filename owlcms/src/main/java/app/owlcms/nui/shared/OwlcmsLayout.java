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
import app.owlcms.nui.home.InfoNavigationContent;
import app.owlcms.nui.lifting.LiftingNavigationContent;
import app.owlcms.nui.preparation.PreparationNavigationContent;
import app.owlcms.nui.results.ResultsNavigationContent;
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
    private FlexLayout menuArea;
    private HorizontalLayout header;

    public OwlcmsLayout() {
        navBarComponents = new ArrayList<>();
        // create default empty components. Content will fill them in.
        populateHeader();
        // setPrimarySection(Section.DRAWER);
        addDrawerContent();
        setContent(null);
    }

    @Override
    public void addToNavbar(boolean touchOptimized, Component... components) {
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(touchOptimized, components);
    }

    @Override
    public void addToNavbar(Component... components) {
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(components);
    }

    public void closeDrawer() {
        setDrawerOpened(false);
    }

    public DrawerToggle getDrawerToggle() {
        return drawerToggle;
    }

    public ComboBox<Locale> getLocaleDropDown() {
        return localeDropDown;
    }

    public FlexLayout getMenuArea() {
        return menuArea;
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

    public void setMenuArea(FlexLayout horizontalLayout) {
        this.menuArea = horizontalLayout;
    }

    public void setMenuTitle(String topBarTitle) {
        getViewTitle().setText(topBarTitle);
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
            appContent.setRouterLayout(this);
            super.showRouterLayoutContent(content);
            appContent.setHeaderContent();
        } else {

            super.showRouterLayoutContent(content);
            populateHeader();
        }

    }

    public void updateHeader() {
        header.removeAll();
        header.setMargin(true);
        header.add(getDrawerToggle(), getViewTitle(), getMenuArea(), getLocaleDropDown());
        header.setFlexGrow(1.0D, getMenuArea());
        header.setWidth("100%");
        header.setAlignItems(Alignment.CENTER);

        clearNavBar();
        addToNavbar(false, header);
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
        header = new HorizontalLayout();
        header.setPadding(false);
        header.setSpacing(false);

        setDrawerToggle(new DrawerToggle());
        getDrawerToggle().getElement().setAttribute("aria-label", "Menu drawerToggle");
        getDrawerToggle().setSizeUndefined();

        setViewTitle(new Label());
        Style style = getViewTitle().getElement().getStyle();
        style.set("font-size", "large");
        style.set("padding-right", "1em");

        setMenuArea(createMenuArea());

        setLocaleDropDown(createLocaleDropdown());

        updateHeader();
    }

    private void addDrawerContent() {
        Tabs tabs = getTabs();
        addToDrawer(tabs);
    }

    private void clearNavBar() {
        for (Component c : getNavBarComponents()) {
            super.remove(c);
        }
        navBarComponents.clear();
    }

    private ComboBox<Locale> createLocaleDropdown() {
        ComboBox<Locale> sessionLocaleField = new ComboBox<>();
        sessionLocaleField.setWidth("24ch");
        sessionLocaleField.setClearButtonVisible(true);
        List<Locale> usefulLocales = Translator.getUsefulLocales();
//        Locale curLocale = OwlcmsSession.getLocale();
//        usefulLocales.sort((a,b) -> {
//            return a.getDisplayName(a).compareTo(b.getDisplayName(b));
//        });
        sessionLocaleField.setDataProvider(new ListDataProvider<>(usefulLocales));
        sessionLocaleField.setItemLabelGenerator((locale) -> locale.getDisplayName(locale));
        sessionLocaleField.setValue(Translator.getLocaleSupplier().get());
        sessionLocaleField.addValueChangeListener(e -> {
            OwlcmsSession.getCurrent().setLocale(e.getValue());
            UI.getCurrent().getPage().reload();
        });
        return sessionLocaleField;
    }

    private Tab createTab(IronIcon viewIcon, String viewName, Class<? extends Component> viewClass) {
        viewIcon.getStyle().set("box-sizing", "border-box")
                .set("margin-inline-end", "var(--lumo-space-m)")
                .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(viewIcon, new Span(viewName));
        link.setRoute(viewClass);
        link.setTabIndex(-1);

        return new Tab(link);
    }

    private Tab createTab(IronIcon viewIcon, String viewName,
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
                        docOpener),
                createTab(IronIcons.INFO_OUTLINE.create(),
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

    private void setViewTitle(Label viewTitle) {
        this.viewTitle = viewTitle;
    }

}
