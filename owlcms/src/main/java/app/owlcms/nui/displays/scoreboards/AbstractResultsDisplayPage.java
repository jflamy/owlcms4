package app.owlcms.nui.displays.scoreboards;

import java.text.DecimalFormat;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasDynamicTitle;

import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.apputils.queryparameters.DisplayParametersReader;
import app.owlcms.nui.displays.AbstractDisplayPage;
import app.owlcms.nui.displays.SoundEntries;
import ch.qos.logback.classic.Logger;

/**
 * Wrapper class to wrap a board as navigable page, to store the board display options, and to present an option editing
 * dialog.
 *
 * @author jflamy
 *
 */
@SuppressWarnings("serial")

public abstract class AbstractResultsDisplayPage extends AbstractDisplayPage
        implements SoundEntries, DisplayParametersReader, HasDynamicTitle {

	Logger logger = (Logger) LoggerFactory.getLogger(AbstractResultsDisplayPage.class);
	public Double emFontSize;
	public Double teamWidth;
	private final DecimalFormat df = new DecimalFormat("0.000");
	protected DisplayParameters board;

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
		addSoundEntries(vl, page, (DisplayParametersReader) page);
	}

	@Override
	public Double getEmFontSize() {
		if (emFontSize == null) {
			return 1.2;
		}
		return emFontSize;
	}

	@Override
	public Double getTeamWidth() {
		if (teamWidth == null) {
			return 12.0D;
		}
		return teamWidth;
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		logger.warn("AbstractAttemptBoardPage onAttach");
		super.onAttach(attachEvent);
		openDialog(getDialog());
	}
	
	@Override
	public void doChangeEmSize() {
		String formattedEm = null;
		if (emFontSize != null) {
			formattedEm = df.format(emFontSize);
			getBoard().getElement().setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "rem;");
		}
	}

	public void doChangeTeamWidth() {
		String formattedTW = null;
		if (teamWidth != null) {
			formattedTW = df.format(teamWidth);
			getBoard().getElement().setProperty("twOverride", "--nameWidth: 1fr; --clubWidth:" + formattedTW + "em;");
		}
	}

}
