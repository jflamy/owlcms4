package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.SoundParameters;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.data.config.Config;
import app.owlcms.displays.attemptboard.AbstractAttemptBoard;
import app.owlcms.displays.attemptboard.DecisionBoard;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")

public class AthleteFacingDecisionBoardPage extends AbstractAttemptBoardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingDecisionBoardPage.class);

	public AthleteFacingDecisionBoardPage() {
		// intentionally empty; superclass will invoke init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Decision_AF_") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	protected void init() {
		setBoard(new DecisionBoard(this));
		
		// when navigating to the page, Vaadin will call setParameter+readParameters
		// these parameters will be applied.
		setDefaultParameters(QueryParameters.simple(Map.of(
		        SoundParameters.SILENT, "false",
		        SoundParameters.DOWNSILENT, "false",
		        DisplayParameters.DARK, "true",
		        DisplayParameters.LEADERS, "false",
		        DisplayParameters.RECORDS, "false",
		        DisplayParameters.VIDEO, "false",
		        DisplayParameters.PUBLIC, "false",
		        SoundParameters.SINGLEREF, "false",
		        DisplayParameters.ABBREVIATED,
		        Boolean.toString(Config.getCurrent().featureSwitch("shortScoreboardNames")))));

		AbstractAttemptBoard board = (AbstractAttemptBoard) getBoard();
		board.getDecisions().setDontReset(false);
		board.setPublicFacing(false);
		board.setShowBarbell(false);
		setSilenced(false);
		setDownSilenced(false);
		this.addComponent(board);
	}

}
