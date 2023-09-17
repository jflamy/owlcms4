package app.owlcms.nui.displays.attemptboards;

import java.util.Map;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.apputils.queryparameters.ContentParameters;
import app.owlcms.displays.attemptboard.AbstractAttemptBoard;
import app.owlcms.displays.attemptboard.AttemptBoard;
import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/attemptBoard")

public class PublicFacingAttemptBoardPage extends AbstractAttemptBoardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(PublicFacingAttemptBoardPage.class);

	public PublicFacingAttemptBoardPage() {
		setDefaultParameters(QueryParameters.simple(Map.of(
		        ContentParameters.SILENT, "true",
		        ContentParameters.DOWNSILENT, "true")));

		setBoard(new AttemptBoard(this));
		
		AbstractAttemptBoard board = (AbstractAttemptBoard) getBoard();
		board.setPublicFacing(true);
		this.addComponent(board);
	}

	@Override
	public String getPageTitle() {
		return getTranslation("AttemptBoard") + OwlcmsSession.getFopNameIfMultiple();
	}

}
