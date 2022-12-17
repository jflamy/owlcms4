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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;

import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;

/**
 * The main view is a top-level placeholder for other views.
 */
@SuppressWarnings("serial")
public class OwlcmsLayout extends AppLayout {

    Logger logger = (Logger)LoggerFactory.getLogger(OwlcmsLayout.class);
    public Label viewTitle;
    protected List<Component> navBarComponents;
    /**
     * @return the navBarComponents
     */
    public List<Component> getNavBarComponents() {
        return navBarComponents;
    }

    /**
     * @param navBarComponents the navBarComponents to set
     */
    public void setNavBarComponents(List<Component> navBarComponents) {
        this.navBarComponents = navBarComponents;
    }

    protected DrawerToggle drawerToggle;
    public static final String LARGE = "text-l";
    public static final String NONE = "m-0";

    public OwlcmsLayout() {
        logger.warn("***** creating layout");
        navBarComponents = new ArrayList<>();
        displayViewTitle("");

        //setPrimarySection(Section.DRAWER);
        addDrawerContent();

    }

    protected void setHeaderContent() {
        HorizontalLayout topBar = new HorizontalLayout();
        drawerToggle = new DrawerToggle();
        drawerToggle.getElement().setAttribute("aria-label", "Menu drawerToggle");
        viewTitle = new Label();
        Style style = viewTitle.getElement().getStyle();
        style.set("font-size", "large");
        style.set("margin-left", "0");
        Div buttonArea = new Div();
        Component languageDropDown = createLocaleDropdown();
        topBar.setMargin(true);
        topBar.add(drawerToggle, viewTitle, buttonArea, languageDropDown);
        topBar.setFlexGrow(1.0D, buttonArea);
        topBar.setWidth("100%");
        topBar.setAlignItems(Alignment.CENTER);
        
        logger.warn("***** OwlcmsLayout set HeaderContent from {}", LoggerUtils.whereFrom());
        clearNavBar();
        addToNavbar(false, topBar);
    }
    
    private ComboBox<Locale> createLocaleDropdown(){
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

    private void clearNavBar() {
        for (Component c: getNavBarComponents()) {
            super.remove(c);
        }
        navBarComponents.clear();
    }

    public void displayViewTitle(String title) {
        viewTitle = new Label();
        viewTitle.getElement().getStyle().set("font-size", "large");
    }
    
    @Override
    public void addToNavbar(boolean touchOptimized, Component... components) {
        logger.warn("***** adding1 {} from {}", (Object[])components, LoggerUtils.whereFrom());
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(touchOptimized, components);
    }
    
    @Override
    public void addToNavbar(Component... components) {
        logger.warn("***** adding2 {} from {}", (Object[])components, LoggerUtils.whereFrom());
        navBarComponents.addAll(Arrays.asList(components));
        super.addToNavbar(components);
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
    
    @Override
    public void showRouterLayoutContent(HasElement content) {
        if (content instanceof OwlcmsLayoutAware) {
            logger.warn("***** aware showRouterLayoutContent {}", content);
            OwlcmsLayoutAware appContent = (OwlcmsLayoutAware) content;
            appContent.setRouterLayout(this);
            super.showRouterLayoutContent(content);
            setHeaderContent();
            appContent.setHeaderContent();
        } else {
            logger.warn("***** NOT aware showRouterLayoutContent {}", content);
            super.showRouterLayoutContent(content);
            setHeaderContent();
        }

    }

    private void addDrawerContent() {
        H3 appName = new H3("TBD");
        appName.addClassNames(LARGE, NONE);
        Header header = new Header(appName);
        header.getStyle().set("margin-left", "1em");

        VerticalLayout createNavigation = createNavigation();
        createNavigation.setWidth("90%");
        Scroller scroller = new Scroller(createNavigation);
        addToDrawer(header, scroller, createFooter());
    }

    private VerticalLayout createNavigation() {
        return new VerticalLayout();
    }

    private Footer createFooter() {
        Footer layout = new Footer();

        return layout;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        setViewTitle(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
    
    public void closeDrawer() {
        setDrawerOpened(false);
    }
    
    public FlexLayout getAppBarElementWrapper() {
        //FIXME
        if (navBarComponents.size() <= 2) {
            navBarComponents.add(new FlexLayout());
        }
        return (FlexLayout)navBarComponents.get(navBarComponents.size()-1);
    }
    
    public void setMenuVisible(boolean hamburgerShown) {
        //FIXME no way to hide hamburger?
    }

    public void setViewTitle(String topBarTitle) {
        viewTitle.setText(topBarTitle);
    }

    public void showLocaleDropdown(boolean b) {
        // TODO Auto-generated method stub
        
    }

}
