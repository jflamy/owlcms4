package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.AbstractAttemptBoard;
import app.owlcms.displays.attemptboard.DecisionBoard;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicFacingDecision")

public class PublicFacingDecisionBoardPage extends AbstractAttemptBoardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(PublicFacingDecisionBoardPage.class);

	public PublicFacingDecisionBoardPage() {
		logger.warn("pf decision board constructor");
		setDefaultParameters(QueryParameters.simple(Map.of(
		        ContentParameters.SILENT, "true",
		        ContentParameters.DOWNSILENT, "true")));

		setBoard(new DecisionBoard(this));
		
		AbstractAttemptBoard board = (AbstractAttemptBoard) getBoard();
		board.getDecisions().setDontReset(true);
		board.setPublicFacing(true);
		board.setShowBarbell(false);
		setSilenced(true);
		setDownSilenced(true);
		this.addComponent(board);

		logger.warn("***** pf decision pf={} {}", board.isPublicFacing(), board.getDecisions().isPublicFacing());
	}

	@Override
	public String getPageTitle() {
		return getTranslation("RefereeDecisions") + OwlcmsSession.getFopNameIfMultiple();
	}

}
