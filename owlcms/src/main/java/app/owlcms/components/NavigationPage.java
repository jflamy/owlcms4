/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.ContentWrapping;

/**
 * Utility methods for creating the texts and buttons on navigation pages.
 * 
 * @author Jean-François Lamy
 *
 */
public interface NavigationPage extends ContentWrapping {

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

	/**
	 * Create a paragraph with HTML inside.
	 * 
	 * @param intro
	 * @param text
	 * @return the formatted paragraph
	 */
	public default Paragraph addP(HasComponents intro, String text) {
		Paragraph paragraph = new Paragraph();
		paragraph.getElement().setProperty("innerHTML", text);
		paragraph.getElement().getStyle().set("margin-bottom", "0");
		intro.add(paragraph);
		return paragraph;
	}
	
	public default void doGroup(String label, FlexibleGridLayout grid1, VerticalLayout wrapper) {
		VerticalLayout content1 = new VerticalLayout();
		content1.add(new Label(label));
		content1.getStyle().set("margin-bottom", "-2ex");
		fillH(content1, wrapper);
		fillH(grid1, wrapper);
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