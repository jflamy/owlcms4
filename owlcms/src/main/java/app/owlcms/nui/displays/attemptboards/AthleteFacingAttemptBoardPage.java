package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingAttempt")

public class AthleteFacingAttemptBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingAttemptBoardPage.class);
	
	public AthleteFacingAttemptBoardPage() {
		var board = new AthleteFacingAttemptBoard(this);
		this.addComponent(board);
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "false",
				ContentParameters.DOWNSILENT, "false")));
	}
	
}
