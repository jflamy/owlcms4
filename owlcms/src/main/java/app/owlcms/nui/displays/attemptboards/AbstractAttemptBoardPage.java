package app.owlcms.nui.displays.attemptboards;

import java.util.List;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;

import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.init.OwlcmsSession;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.SoundEntries;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

/**
 * Wrapper class to wrap a board as navigable page, to store the board display options, and to present an option editing
 * dialog.
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractAttemptBoardPage extends AbstractDisplayPage
        implements SoundEntries, DisplayParametersReader, HasDynamicTitle {

	Logger logger = (Logger) LoggerFactory.getLogger(AbstractAttemptBoardPage.class);

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
		addSoundEntries(vl, page, (DisplayParametersReader) page);
	}

	/**
	 * @see app.owlcms.apputils.queryparameters.FOPParameters#updateURLLocation(com.vaadin.flow.component.UI,
	 *      com.vaadin.flow.router.Location, java.lang.String, java.lang.String)
	 */
	@Override
	public void updateURLLocation(UI ui, Location location, String parameter, String mode) {
		logger.warn("AttemptBoardPage updateURLLocation");
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

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.warn("AbstractAttemptBoardPage onAttach");
		super.onAttach(attachEvent);
		openDialog(getDialog());
	}

}
