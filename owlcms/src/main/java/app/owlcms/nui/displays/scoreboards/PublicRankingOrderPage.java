package app.owlcms.nui.displays.scoreboards;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.router.Route;

import app.owlcms.init.OwlcmsSession;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@Route("displays/publicRankingOrder")

public class PublicRankingOrderPage extends PublicScoreboardPage {

	Logger logger = (Logger) LoggerFactory.getLogger(PublicRankingOrderPage.class);

	public PublicRankingOrderPage() {
		// intentionally empty. superclass will call init() as required.
	}

	@Override
	public String getPageTitle() {
		return getTranslation("Scoreboard.RankingOrder") + OwlcmsSession.getFopNameIfMultiple();
	}

	@Override
	public void setDefaultParameters() {
		super.setDefaultParameters();
	}
}
