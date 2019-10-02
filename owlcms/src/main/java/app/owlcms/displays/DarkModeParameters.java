/**
 * 
 */
package app.owlcms.displays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.ui.shared.QueryParameterReader;

/**
 * @author owlcms
 *
 */
public interface DarkModeParameters extends QueryParameterReader {
    
    public void setDarkMode(boolean dark);
    
    public boolean isDarkMode();
    
    /*
     * Process query parameters
     * @see app.owlcms.ui.group.URLParameter#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
     * @see app.owlcms.ui.shared.QueryParameterReader#setParameter(com.vaadin.flow.router.BeforeEvent, java.lang.String)
     */
    @Override
    public default void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        Location location = event.getLocation();
        QueryParameters queryParameters = location.getQueryParameters();
        Map<String, List<String>> parametersMap = queryParameters.getParameters();
        HashMap<String, List<String>> cleanParams = computeParams(location, parametersMap);
        
        List<String> darkParams = cleanParams.get("dark");
        // dark is the default.  dark=false or dark=no or ... will turn off dark mode.
        boolean darkMode = darkParams == null || darkParams.isEmpty() || darkParams.get(0).toLowerCase().equals("true");
        setDarkMode(darkMode);
        
        // change the URL to reflect retrieved parameters
        event.getUI().getPage().getHistory().replaceState(null, new Location(location.getPath(),new QueryParameters(cleanParams)));
    }

    public default void setDarkMode(Element element2, boolean dark, boolean notify) {
        element2.getClassList().set("dark", dark);
        element2.getClassList().set("light", !dark);
        setDarkMode(dark);

        if (notify) {
            doNotification(dark);
        }
    }

    public default void doNotification(boolean dark) {
        Notification n = new Notification();
        H2 h2 = new H2();
        h2.setText(h2.getTranslation("darkMode." + (dark ? "dark" : "light")));
        h2.getStyle().set("margin", "0");
        n.add(h2);
        n.setDuration(3000);
        n.setPosition(Position.MIDDLE);
        if (dark) {
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            h2.getStyle().set("color", "white");
        }
        n.getElement().getStyle().set("font-size", "x-large");
        n.open();
    }

    public default ContextMenu buildContextMenu(Component target) {
        ContextMenu contextMenu = new ContextMenu(target);
        contextMenu.addItem(contextMenu.getTranslation("dark"), e -> setDarkMode(target.getElement(),true, true));
        contextMenu.addItem(contextMenu.getTranslation("light"), e -> setDarkMode(target.getElement(),false, true));
        return contextMenu;
    }
    

}
