package app.owlcms;

import javax.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import app.owlcms.init.OwlcmsSession;

/**
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SuppressWarnings("serial")
@PWA(name = "Olympic Weightlifting Competition Management System", shortName = "owlcms")
@Push
@Theme(themeClass=Lumo.class)
public class AppShell implements AppShellConfigurator, VaadinServiceInitListener  {
    
    /**
     * @see com.vaadin.flow.component.page.AppShellConfigurator#configurePage(com.vaadin.flow.server.AppShellSettings)
     */
    @Override
    public void configurePage(AppShellSettings settings) {
        HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();
        response.addHeader("Content-Language",  OwlcmsSession.getLocale().toString().replace("_", "*"));
        settings.addInlineWithContents("<meta http-equiv='content-language' content='"+OwlcmsSession.getLocale().toString().replace("_", "-")+"'>",  null);
        settings.addLink("shortcut icon", "icons/owlcms.ico");
        settings.addFavIcon("icon", "icons/owlcms.png", "96x96");
    }
    
    /**
     * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow.server.ServiceInitEvent)
     */
    @Override
    public void serviceInit(ServiceInitEvent serviceInitEvent) {
        serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
            LoadingIndicatorConfiguration conf = uiInitEvent.getUI().getLoadingIndicatorConfiguration();

            // disable default theme on loading indicator -> loading indicator isn't shown
//            conf.setApplyDefaultTheme(false);

            /*
             * Delay for showing the indicator and setting the 'first' class name.
             */
            conf.setFirstDelay(2000); // 300ms is the default

            /* Delay for setting the 'second' class name */
            conf.setSecondDelay(3500); // 1500ms is the default

            /* Delay for setting the 'third' class name */
            conf.setThirdDelay(5000); // 5000ms is the default
        });
    }
}
