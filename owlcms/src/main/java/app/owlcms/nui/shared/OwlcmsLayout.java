package app.owlcms.nui.shared;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;

/**
 * The main view is a top-level placeholder for other views.
 */
@SuppressWarnings("serial")
public class OwlcmsLayout extends AppLayout {

    public H2 viewTitle;
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
        navBarComponents = new ArrayList<>();
        viewTitle = new H2();

        //setPrimarySection(Section.DRAWER);
        addDrawerContent();

    }

    protected void setHeaderContent() {
        drawerToggle = new DrawerToggle();
        drawerToggle.getElement().setAttribute("aria-label", "Menu drawerToggle");

        viewTitle = new H2();
        viewTitle.addClassNames(LARGE, NONE);

        addToNavbar(true, drawerToggle, viewTitle);
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
        super.showRouterLayoutContent(content);
        if (content instanceof AppLayoutAware) {
            ((AppLayoutAware) content).setRouterLayout(this);
        }
        // this can call methods from the page content
        setHeaderContent();
    }

    private void addDrawerContent() {
        H1 appName = new H1("TBD");
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
        if (navBarComponents.size() == 2) {
            navBarComponents.add(new FlexLayout());
        }
        return (FlexLayout)navBarComponents.get(2);
    }
    
    public void setMenuVisible(boolean hamburgerShown) {
        //FIXME no way to hide hamburger?
    }

    public void setViewTitle(String topBarTitle) {
        viewTitle.setText(topBarTitle);
    }
}
