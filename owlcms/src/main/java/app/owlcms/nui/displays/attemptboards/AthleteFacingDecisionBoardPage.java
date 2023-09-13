package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")
public class AthleteFacingDecisionBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingDecisionBoardPage.class);
	
	public AthleteFacingDecisionBoardPage() {
		logger.warn("decision board constructor");
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "false",
				ContentParameters.DOWNSILENT, "false")));
		var board = new AthleteFacingDecisionBoard(this);
		this.addComponent(board);
	}	

}
