package app.owlcms.nui.displays.scoreboards;

import org.slf4j.LoggerFactory;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import app.owlcms.access.RequiresRole;
import app.owlcms.access.Role;
import com.vaadin.flow.router.Route;
import app.owlcms.apputils.queryparameters.DisplayParameters;
import app.owlcms.displays.scoreboard.WodBoard;
import app.owlcms.fieldofplay.FieldOfPlay;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
@RequiresRole(value = Role.DISPLAYS, platformBound = true)
@Route("displays/wod")
public class WodPage extends WarmupScoreboardPage {
    Logger logger = (Logger) LoggerFactory.getLogger(WodPage.class);

    public WodPage() {
        // intentionally empty. superclass will call init() as required.
    }

    @Override
    public String getPageTitle() {
        String suffix = FieldOfPlay.getFopNameIfMultiple(getFop());
        return Translator.translate("WOD") + suffix;
    }

    @Override
    protected void init() {
        this.logger = (Logger) LoggerFactory.getLogger(WodPage.class);
        var board = new WodBoard();
        this.setBoard(board);
        // No special parameters for now
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        DisplayParameters board = (DisplayParameters) this.getBoard();
        board.setFop(this.getFop());
        this.addComponent((Component) board);
    }
}
