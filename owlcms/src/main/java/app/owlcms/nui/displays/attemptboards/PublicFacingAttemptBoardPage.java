package app.owlcms.nui.displays.attemptboards;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.PublicFacingAttemptBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/attemptBoard")

public class PublicFacingAttemptBoardPage extends AbstractAttemptBoardPage {
	
	Logger logger = (Logger) LoggerFactory.getLogger(PublicFacingAttemptBoardPage.class);
	
	public PublicFacingAttemptBoardPage() {
		var board = new PublicFacingAttemptBoard(this);
		this.addComponent(board);
		
		// set the default options for the dialog and propagate to the board
		this.setSilenced(true);
		this.setDownSilenced(true);
	}

}
