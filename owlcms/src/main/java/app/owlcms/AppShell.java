package app.owlcms;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;

/**
 * Use the @PWA annotation make the application installable on phones, tablets
 * and some desktop browsers.
 */
@SuppressWarnings("serial")
@PWA(name = "Project Base for Vaadin", shortName = "Project Base")
@Push
public class AppShell implements AppShellConfigurator {
}
