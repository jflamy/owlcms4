package app.owlcms.nui.displays;

import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.AthleteFacingDecisionBoard;

@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")
public class AthleteFacingDecisionBoardPage extends AttemptBoardPage {
	
	public AthleteFacingDecisionBoardPage() {
		var board = new AthleteFacingDecisionBoard(this);
		this.addComponent(board);
	}
	
	@Override
	public boolean isSilencedByDefault() {
		return false;
	}

}
