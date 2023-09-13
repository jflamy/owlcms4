package app.owlcms.nui.displayselection;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.PublicFacingDecisionBoard;

@SuppressWarnings("serial")
@Route("displays/athleteFacingDecision")
public class PublicFacingDecisionBoardPage extends AttemptBoardPage {
	
	public PublicFacingDecisionBoardPage() {
		var board = new PublicFacingDecisionBoard(this);
		this.addComponent(board);
	}

	@Override
	public boolean isSilencedByDefault() {
		return true;
	}
	

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		//FIXME get the options
		//checkVideo(Config.getCurrent().getParamStylesDir() + "/video/decisionboard.css", routeParameter, this);
	}
}
