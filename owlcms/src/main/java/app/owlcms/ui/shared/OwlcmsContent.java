package app.owlcms.ui.shared;

import com.vaadin.flow.router.HasDynamicTitle;

public interface OwlcmsContent extends ContentWrapping, AppLayoutAware, HasDynamicTitle, SafeEventBusRegistration, RequireLogin {

}
