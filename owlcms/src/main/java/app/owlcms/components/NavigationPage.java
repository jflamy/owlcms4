package app.owlcms.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;

/**
 * Utility methods for creating the texts and buttons on navigation pages.
 * 
 * @author Jean-François Lamy
 *
 */
public interface NavigationPage {

	public default String getWindowOpener(Class<? extends Component> targetClass) {
		FieldOfPlay fop = OwlcmsSession.getFop();
		String name = fop == null ? "" : "_"+fop.getName();
		return "window.open('"+
				getUrlFromTarget(targetClass)+
				"','"+
				targetClass.getSimpleName()+
				name+
				"')";
	}

	public default String getUrlFromTarget(Class<? extends Component> targetClass) {
		RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
		String relativeURL = routeResolver.getUrl(targetClass);
		String absoluteURL = buildAbsoluteURL(VaadinServletRequest.getCurrent(),relativeURL);
		return absoluteURL;
	}

	public default Paragraph addParagraph(HasComponents intro, String text) {
		Paragraph paragraph = new Paragraph(text);
		paragraph.getElement().getStyle().set("margin-bottom", "0");
		intro.add(paragraph);
		return paragraph;
	}
	
	public default String buildAbsoluteURL(VaadinServletRequest request, String resourcePath) {
		int port = request.getServerPort();
		StringBuilder result = new StringBuilder();
		result.append(request.getScheme())
			.append("://")
			.append(request.getServerName());
		if ((request.getScheme().equals("http") && port != 80)
				|| (request.getScheme().equals("https") && port != 443)) {
			result.append(':')
				.append(port);
		}
		result.append(request.getContextPath());
		if (resourcePath != null && resourcePath.length() > 0) {
			if (!resourcePath.startsWith("/")) {
				result.append("/");
			}
			result.append(resourcePath);
		}
		return result.toString();
	}

}