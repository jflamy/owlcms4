package app.owlcms.ui.appLayout;

import com.github.appreciated.app.layout.router.AppLayoutRouterLayout;

public interface AppLayoutContent {

	void setParentLayout(AppLayoutRouterLayout parentLayout);

	AppLayoutRouterLayout getParentLayout();

}
