package app.owlcms.ui.home;

import com.vaadin.flow.router.Route;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.ui.shared.OwlcmsRouterLayout;

@SuppressWarnings("serial")
@Route(value = DisplayLoginView.LOGIN, layout = OwlcmsRouterLayout.class)
public class DisplayLoginView extends LoginView {
    
    public static final String LOGIN = "displaylogin";

    @Override
    protected boolean checkAuthenticated(String value) {
        return !AccessUtils.checkDisplayAuthenticated(value);
    }
}
