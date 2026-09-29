package app.owlcms.access;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;

import app.owlcms.i18n.Translator;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.home.HomeNavigationContent;
import app.owlcms.nui.shared.ContentWrapping;
import app.owlcms.nui.shared.OwlcmsLayout;
import app.owlcms.nui.shared.OwlcmsLayoutAware;

/** Shown when the principal may not open the requested page. */
@SuppressWarnings("serial")
@Route(value = "denied", layout = OwlcmsLayout.class)
@AuthenticatedPage
public class AccessDeniedView extends Composite<VerticalLayout>
        implements OwlcmsLayoutAware, ContentWrapping, HasDynamicTitle {

	private OwlcmsLayout routerLayout;

	public AccessDeniedView() {
		Principal principal = OwlcmsSession.getPrincipal();

		H3 title = new H3(Translator.translate("Access.Denied.Title"));
		title.getStyle().set("color", "var(--lumo-header-text-color)");
		title.getStyle().set("font-size", "var(--lumo-font-size-xl)");

		VerticalLayout form = new VerticalLayout();
		form.add(title, new Paragraph(Translator.translate("Access.Denied.Message")));
		if (principal != null && principal.username() != null && !"-".equals(principal.username())) {
			form.add(new Paragraph(principal.username()));
		}
		if (principal != null && principal.loginPlatform() != null) {
			form.add(new Paragraph(Translator.translate("Platform") + ": " + principal.loginPlatform()));
		}

		Button home = new Button(Translator.translate("Home"),
		        e -> UI.getCurrent().navigate(HomeNavigationContent.class));
		home.getThemeNames().add("primary");
		form.add(home);
		form.setWidth("30em");
		getContent().add(form);
	}

	@Override
	public FlexLayout createMenuArea() {
		return new FlexLayout();
	}

	public String getMenuTitle() {
		return Translator.translate("OWLCMS_Top");
	}

	@Override
	public String getPageTitle() {
		return Translator.translate("Access.Denied.Title");
	}

	@Override
	public OwlcmsLayout getRouterLayout() {
		return this.routerLayout;
	}

	@Override
	public void setHeaderContent() {
		NativeLabel label = new NativeLabel(getMenuTitle());
		label.getStyle().set("font-size", "var(--lumo-font-size-xl)");
		Image image = new Image("icons/owlcms.png", "owlcms icon");
		image.getStyle().set("height", "7ex");
		image.getStyle().set("width", "auto");
		HorizontalLayout topBarTitle = new HorizontalLayout(image, label);
		topBarTitle.setAlignSelf(Alignment.CENTER, label);
		this.routerLayout.setMenuTitle(topBarTitle);
		this.routerLayout.setMenuArea(createMenuArea());
		this.routerLayout.showLocaleDropdown(true);
		this.routerLayout.setDrawerOpened(true);
		this.routerLayout.updateHeader(true);
	}

	@Override
	public void setPadding(boolean b) {
		// not needed
	}

	@Override
	public void setRouterLayout(OwlcmsLayout routerLayout) {
		this.routerLayout = routerLayout;
	}
}
