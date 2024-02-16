package app.owlcms.nui.displays.attemptboards;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.router.Route;

import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/brandingBoard")

public class BrandingAttemptBoardPage extends Div {

	Logger logger = (Logger) LoggerFactory.getLogger(BrandingAttemptBoardPage.class);

	public BrandingAttemptBoardPage() {
		IFrame iframe = new IFrame("displays/attemptBoard");
		this.setSizeFull();
		this.getStyle().set("overflow", "hidden");
		iframe.setSizeFull();
		this.addComponentAsFirst(iframe);
	}

}
