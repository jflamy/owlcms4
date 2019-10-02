/**
 * 
 */
package app.owlcms.displays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.ui.shared.QueryParameterReader;

/**
 * @author owlcms
 *
 */
public interface DisplayParameters extends QueryParameterReader {
    
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

}
