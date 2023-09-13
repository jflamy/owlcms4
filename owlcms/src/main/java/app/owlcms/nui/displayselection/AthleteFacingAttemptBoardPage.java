package app.owlcms.nui.displayselection;

import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.AthleteFacingAttemptBoard;

@SuppressWarnings("serial")
@Route("displays/athleteFacingAttempt")

public class AthleteFacingAttemptBoardPage extends AttemptBoardPage {
	
	public AthleteFacingAttemptBoardPage() {
		var board = new AthleteFacingAttemptBoard(this);
		this.addComponent(board);
	}
	
	@Override
	public boolean isSilencedByDefault() {
		return false;
	}


}
