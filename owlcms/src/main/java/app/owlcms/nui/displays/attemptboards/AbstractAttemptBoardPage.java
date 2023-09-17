package app.owlcms.nui.displays.attemptboards;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;

import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.SoundEntries;
import app.owlcms.nui.shared.SafeEventBusRegistration;
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
        implements SoundEntries, DisplayParametersReader, HasDynamicTitle, SafeEventBusRegistration {

	Logger logger = (Logger) LoggerFactory.getLogger(AbstractAttemptBoardPage.class);
	
	public AbstractAttemptBoardPage() {
		// intentionally empty; superclass will invoke init() as required.
	}

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
		addSoundEntries(vl, page, (DisplayParametersReader) page);
	}

}
