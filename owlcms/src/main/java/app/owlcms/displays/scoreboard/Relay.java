package app.owlcms.displays.scoreboard;

import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.ContextFreeDisplayParameters;
import app.owlcms.data.category.Category;
import app.owlcms.data.group.Group;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.lifting.UIEventProcessor;
import app.owlcms.nui.shared.SafeEventBusRegistration;
import app.owlcms.uievents.CeremonyType;
import app.owlcms.uievents.UIEvent;

/**
 * TODO: this class should take the URL Parameters as a route, and navigate to that route.
 * 
 * <p>.../relay/display/results/video?paramSet=1 should navigate to .../display/results/video?group=H1</p>
 * <p>paramSet 1 would be defined using the user interface to define group, cat, for, ag, em, darkMode, etc.</p>
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class Relay extends Component implements ContextFreeDisplayParameters, SafeEventBusRegistration {
	protected EventBus uiEventBus;
	private Group ceremonyGroup;

	@Subscribe
	public void slaveCeremonyStarted(UIEvent.CeremonyStarted e) {;
		UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			doCeremony(e);
		});
	}
	
	public void doCeremony(UIEvent.CeremonyStarted e) {
		ceremonyGroup = e.getCeremonyGroup();
		@SuppressWarnings("unused")
		Category ceremonyCategory = e.getCeremonyCategory();
		OwlcmsSession.withFop(fop -> UIEventProcessor.uiAccess(this, uiEventBus, () -> {
			if (e.getCeremonyType() == CeremonyType.MEDALS && this.isSwitchableDisplay() && ceremonyGroup != null) {
				UI.getCurrent().navigate("displays/resultsMedals", new QueryParameters(getUrlParameterMap()));
			}
		}));
	}

	@Override
	public void addDialogContent(Component target, VerticalLayout vl) {

		
	}

	@Override
	public Dialog getDialog() {

		return null;
	}

	@Override
	public String getRouteParameter() {

		return null;
	}

	@Override
	public boolean isDarkMode() {

		return false;
	}

	@Override
	public boolean isShowInitialDialog() {

		return false;
	}

	@Override
	public void setDarkMode(boolean dark) {
	}

	@Override
	public void setDialog(Dialog dialog) {

		
	}

	@Override
	public void setDialogTimer(Timer timer) {

		
	}

	@Override
	public void setRouteParameter(String routeParameter) {

		
	}

	@Override
	public void setShowInitialDialog(boolean b) {

		
	}

	@Override
	public Timer getDialogTimer() {

		return null;
	}

	@Override
	public boolean isSilenced() {

		return false;
	}

	@Override
	public void setSilenced(boolean silent) {

		
	}

	@Override
	public Location getLocation() {

		return null;
	}

	@Override
	public UI getLocationUI() {

		return null;
	}

	@Override
	public void setLocation(Location location) {

		
	}

	@Override
	public void setLocationUI(UI locationUI) {

		
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#setUrlParameterMap(java.util.Map)
	 */
	@Override
	public void setUrlParameterMap(Map<String, List<String>> parametersMap) {

		
	}

	@Override
	public Map<String, List<String>> getUrlParameterMap() {

		return null;
	}

	@Override
	public void setGroup(Group group) {

		
	}

	@Override
	public void setCategory(Category cat) {

		
	}

	@Override
	public void setFop(FieldOfPlay fop) {

		
	}

}
