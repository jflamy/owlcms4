package app.owlcms.ui.shared;

import com.github.appreciated.app.layout.behaviour.AbstractLeftAppLayoutBase;

public interface LayoutAware {
	public OwlcmsRouterLayout getRouterLayout();
	
	public void setRouterLayout(OwlcmsRouterLayout routerLayout);
	
	public default AbstractLeftAppLayoutBase getAppLayout() {
		return (AbstractLeftAppLayoutBase) getRouterLayout().getAppLayout();
	}

}
