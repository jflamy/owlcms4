package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.PublicFacingDecisionBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicFacingDecision")
public class PublicFacingDecisionBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(PublicFacingDecisionBoardPage.class);
	
	public PublicFacingDecisionBoardPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "true",
				ContentParameters.DOWNSILENT, "true")));
		var board = new PublicFacingDecisionBoard(this);
		this.addComponent(board);

	}

}
