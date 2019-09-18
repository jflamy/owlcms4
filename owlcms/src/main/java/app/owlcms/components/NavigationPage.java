/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 * 
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)  
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.components;

import javax.servlet.http.HttpServletRequest;

import com.github.appreciated.layout.FlexibleGridLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServletRequest;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.shared.OwlcmsContent;
import app.owlcms.utils.URLUtils;

/**
 * Utility methods for creating the texts and buttons on navigation pages.
 * 
 * @author Jean-François Lamy
 *
 */
public interface NavigationPage extends OwlcmsContent {

    
    public default <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass, String label, String parameter) {
        Button button = new Button(label);
        button.getElement().setAttribute("onClick", getWindowOpenerFromClass(targetClass, parameter));
        return button;
    }
    
    public default <T extends Component & HasUrlParameter<String>> Button openInNewTab(Class<T> targetClass, String label) {
        return openInNewTab(targetClass, label, null);
    }
    
    public default <T extends Component & HasUrlParameter<String>> String getWindowOpenerFromClass(Class<T> targetClass, String parameter) {
        FieldOfPlay fop = OwlcmsSession.getFop();
        String name = fop == null ? "" : "_" + fop.getName();
        return "window.open('" +
                getUrlFromTargetClass(targetClass, parameter) + "','" +
                targetClass.getSimpleName() + name + "')";
    }

    public default <T extends Component & HasUrlParameter<String>> String getUrlFromTargetClass(Class<T> class1, String parameter) {
        RouteConfiguration routeResolver = RouteConfiguration.forApplicationScope();
        String relativeURL;
        if (parameter == null) {
            relativeURL = routeResolver.getUrl(class1);
        } else {
            relativeURL = routeResolver.<String,T>getUrl(class1, parameter);
        }
        String absoluteURL = buildAbsoluteURL(VaadinServletRequest.getCurrent(), relativeURL);
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
        paragraph.getStyle().set("margin-bottom", "0");
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

    public default void doGroup(String label, VerticalLayout intro, FlexibleGridLayout grid1, VerticalLayout wrapper) {
        VerticalLayout content1 = new VerticalLayout();
        content1.add(new Label(label));
        content1.add(intro);
        content1.getStyle().set("margin-bottom", "-2ex");
        fillH(content1, wrapper);
        fillH(grid1, wrapper);
    }

    public default String buildAbsoluteURL(VaadinServletRequest vRequest, String resourcePath) {
        HttpServletRequest request = vRequest.getHttpServletRequest();
        int port = URLUtils.getServerPort(request);
        String scheme = URLUtils.getScheme(request);
        StringBuilder result = new StringBuilder();
        result.append(scheme).append("://")
                .append(URLUtils.getServerName(request));
        if ((scheme.equals("http") && port != 80)
                || (request.getScheme().equals("https") && port != 443)) {
            result.append(':').append(port);
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