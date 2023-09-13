package app.owlcms.nui.displayselection;

import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.PublicFacingAttemptBoard;

@SuppressWarnings("serial")
@Route("displays/athleteFacingAttempt")

public class PublicFacingAttemptBoardPage extends AttemptBoardPage {
	
	public PublicFacingAttemptBoardPage() {
		var board = new PublicFacingAttemptBoard(this);
		this.addComponent(board);
	}
	
	@Override
	public boolean isSilencedByDefault() {
		return true;
	}


}
