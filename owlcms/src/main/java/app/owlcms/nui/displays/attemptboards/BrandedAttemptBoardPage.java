package app.owlcms.nui.displays.attemptboards;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.attemptboard.BrandedAttemptBoard;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/brandedBoard")

public class BrandedAttemptBoardPage extends Div {

	Logger logger = (Logger) LoggerFactory.getLogger(BrandedAttemptBoardPage.class);

	public BrandedAttemptBoardPage() {
		this.addComponentAsFirst(new BrandedAttemptBoard());
	}

}
