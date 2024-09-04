package app.owlcms;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.LoadingIndicatorConfiguration;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.ErrorHandler;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.IndexHtmlRequestListener;
import com.vaadin.flow.server.communication.IndexHtmlResponse;
import com.vaadin.flow.theme.Theme;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.servlet.StopProcessingException;
import app.owlcms.utils.LoggerUtils;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Use the @PWA annotation make the application installable on phones, tablets and some desktop browsers.
 */
@SuppressWarnings("serial")
// @PWA(name = "Olympic Weightlifting Competition Management System", shortName = "owlcms")
@Push
@Theme(value = "owlcms")
public class AppShell implements AppShellConfigurator, VaadinServiceInitListener, IndexHtmlRequestListener {

	/**
	 * @see com.vaadin.flow.component.page.AppShellConfigurator#configurePage(com.vaadin.flow.server.AppShellSettings)
	 */
	@Override
	public void configurePage(AppShellSettings settings) {
		HttpServletResponse response = VaadinServletResponse.getCurrent().getHttpServletResponse();
		response.addHeader("Content-Language", getCurrentUserLanguage());
		// not a recommended practice
		// settings.addInlineWithContents("<meta http-equiv='content-language'
		// content='"+getCurrentUserLanguage()+"'>", null);
		settings.addLink("shortcut icon", "icons/owlcms.ico");
		settings.addFavIcon("icon", "icons/owlcms.png", "96x96");
	}

	/**
	 * @see com.vaadin.flow.server.communication.IndexHtmlRequestListener#modifyIndexHtmlResponse(com.vaadin.flow.server.communication.IndexHtmlResponse)
	 */
	@Override
	public void modifyIndexHtmlResponse(IndexHtmlResponse indexHtmlResponse) {
		indexHtmlResponse.getDocument().getElementsByTag("html").attr("lang", getCurrentUserLanguage());
	}

	/**
	 * @see com.vaadin.flow.server.VaadinServiceInitListener#serviceInit(com.vaadin.flow.server.ServiceInitEvent)
	 */
	@Override
	public void serviceInit(ServiceInitEvent serviceInitEvent) {
		serviceInitEvent.getSource().addUIInitListener(uiInitEvent -> {
			LoadingIndicatorConfiguration conf = uiInitEvent.getUI().getLoadingIndicatorConfiguration();

			// disable default theme on loading indicator -> loading indicator isn't shown
			// conf.setApplyDefaultTheme(false);

			/*
			 * Delay for showing the indicator and setting the 'first' class name.
			 */
			conf.setFirstDelay(2000); // 300ms is the default

			/* Delay for setting the 'second' class name */
			conf.setSecondDelay(3500); // 1500ms is the default

			/* Delay for setting the 'third' class name */
			conf.setThirdDelay(5000); // 5000ms is the default
		});
		serviceInitEvent.addIndexHtmlRequestListener(this);

		serviceInitEvent.getSource().addSessionInitListener(sessionInitEvent -> {
			VaadinSession session = sessionInitEvent.getSession();
			ErrorHandler handler = new ErrorHandler() {
				@Override
				public void error(ErrorEvent errorEvent) {
					Throwable t = errorEvent.getThrowable();
					if (!(t instanceof StopProcessingException)) {
						LoggerFactory.getLogger("app.owlcms.errorHandler").warn("{} {}", t.toString(), t instanceof NullPointerException ? LoggerUtils.stackTrace() : "");
					}
				}
			};
			session.setErrorHandler(handler);
		});
	}

	private String getCurrentUserLanguage() {
		return OwlcmsSession.getLocale().toString().replace("_", "-");
	}
}
