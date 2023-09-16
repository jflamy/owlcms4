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
	private final DecimalFormat df = new DecimalFormat("0.000");
	protected DisplayParameters board;

	@Override
	public void addDialogContent(Component page, VerticalLayout vl) {
		addSoundEntries(vl, page, (DisplayParametersReader) page);
	}

	@Override
	public Double getEmFontSize() {
		if (super.getEmFontSize() == null) {
			return 1.2;
		}
		return super.getEmFontSize();
	}

	@Override
	public Double getTeamWidth() {
		if (super.getTeamWidth() == null) {
			return 12.0D;
		}
		return super.getTeamWidth();
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
		if (getEmFontSize() != null) {
			formattedEm = df.format(getEmFontSize());
			getBoard().getElement().setProperty("sizeOverride", " --tableFontSize:" + formattedEm + "rem;");
		}
	}

	public void doChangeTeamWidth() {
		String formattedTW = null;
		if (getTeamWidth() != null) {
			formattedTW = df.format(getTeamWidth());
			getBoard().getElement().setProperty("twOverride", "--nameWidth: 1fr; --clubWidth:" + formattedTW + "em;");
		}
	}

}
