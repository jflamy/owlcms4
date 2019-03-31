package app.owlcms.displays.attemptboard;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.material.Material;

@SuppressWarnings("serial")
@Tag("attempt-board-template")
@HtmlImport("frontend://components/AttemptBoard.html")
@Route("displays/athleteFacing")
@Theme(value = Material.class, variant = Material.DARK)
public class AthleteFacingBoard extends AttemptBoard {
	
	public AthleteFacingBoard() {
		super();
		setPublicFacing(false);
	}
	
	public void setPublicFacing(boolean publicFacing) {
		getModel().setPublicFacing(publicFacing);
	}
	
	public boolean isPublicFacing() {
		return Boolean.TRUE.equals(getModel().isPublicFacing());
	}

	/* (non-Javadoc)
	 * @see app.owlcms.displays.attemptboard.AttemptBoard#onAttach(com.vaadin.flow.component.AttachEvent)
	 */
	@Override
	protected void onAttach(AttachEvent attachEvent) {
		super.onAttach(attachEvent);
		decisions.setPublicFacing(false);
	}
}
