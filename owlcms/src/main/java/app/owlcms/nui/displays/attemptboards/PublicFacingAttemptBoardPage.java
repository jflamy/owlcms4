package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.PublicFacingAttemptBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/attemptBoard")

public class PublicFacingAttemptBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(PublicFacingAttemptBoardPage.class);
	
	public PublicFacingAttemptBoardPage() {
		var board = new PublicFacingAttemptBoard(this);
		this.addComponent(board);
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "true",
				ContentParameters.DOWNSILENT, "true")));
	}

}
