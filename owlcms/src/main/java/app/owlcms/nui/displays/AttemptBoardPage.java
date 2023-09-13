package app.owlcms.nui.displays;

import java.util.List;
import java.util.TreeMap;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.utils.URLUtils;

/**
 * Wrapper class to wrap a board as navigable page, to store the board display options, and to present an option editing dialog.
 * 
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")
public abstract class AttemptBoardPage extends AbstractDisplayPage implements SoundEntries {

	@Override
	public void addDialogContent(Component unused, VerticalLayout vl) {
		addSoundEntries(this, this, this);
	}

	@Override
	public AttemptBoardPage getWrapper() {
		return this;
	}


	@Override
	public boolean isDownSilenced() {
		return super.isDownSilenced();
	}
	
	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#updateURLLocation(com.vaadin.flow.component.UI,
	 *      com.vaadin.flow.router.Location, java.lang.String, java.lang.String)
	 */
	@Override
	public void updateURLLocation(UI ui, Location location, String parameter, String mode) {
		TreeMap<String, List<String>> parametersMap = new TreeMap<>(location.getQueryParameters().getParameters());
		updateParam(parametersMap, DARK, null);
		updateParam(parametersMap, parameter, mode);
		FieldOfPlay fop = OwlcmsSession.getFop();
		updateParam(parametersMap, "fop", fop != null ? fop.getName() : null);
		setUrlParameterMap(parametersMap);
		Location location2 = new Location(location.getPath(), new QueryParameters(URLUtils.cleanParams(parametersMap)));
		ui.getPage().getHistory().replaceState(null, location2);
		setLocation(location2);
	}

}
