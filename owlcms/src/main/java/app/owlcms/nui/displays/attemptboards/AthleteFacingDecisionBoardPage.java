package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.DecisionBoard;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")

public class AthleteFacingDecisionBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingDecisionBoardPage.class);
	
	public AthleteFacingDecisionBoardPage() {
		logger.warn("af decision board constructor");
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "false",
				ContentParameters.DOWNSILENT, "false")));
		
		var board = new DecisionBoard(this);
		board.getDecisions().setDontReset(false);
		board.setPublicFacing(false);
		board.setShowBarbell(false);
		setSilenced(false);
		setDownSilenced(false);
		this.addComponent(board);
		
		logger.warn("***** af decision pf={} {}",board.isPublicFacing(), board.getDecisions().isPublicFacing());
	}
	
	@Override
	public String getPageTitle() {
		return getTranslation("Decision_AF_") + OwlcmsSession.getFopNameIfMultiple();
	}
	
}
