package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/athleteFacingAttempt")

public class AthleteFacingAttemptBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(AthleteFacingAttemptBoardPage.class);
	
	public AthleteFacingAttemptBoardPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
				ContentParameters.SILENT, "false",
				ContentParameters.DOWNSILENT, "false")));
		
		var board = new AttemptBoard(this);
		board.setPublicFacing(false);
		this.addComponent(board);

		logger.warn("**** athlete attempt pf={} {}",board.isPublicFacing(), board.getDecisions().isPublicFacing());
	}
	
	@Override
	public String getPageTitle() {
		return getTranslation("AttemptAF") + OwlcmsSession.getFopNameIfMultiple();
	}

	
}
