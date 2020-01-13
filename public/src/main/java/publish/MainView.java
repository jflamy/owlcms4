package publish;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;

import ch.qos.logback.classic.Logger;

@Route
@Push
public class MainView extends VerticalLayout {

    static Text text;
    private static Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);

    public MainView() {
        text = new Text("Waiting.");
        add(text);
    }

    @Subscribe
    public static void update(UpdateEvent e) {
        String leaders = e.getLeaders();
        logger.warn("updating {}", leaders);
        UI.getCurrent().access(() -> {
            text.setText(leaders);
            logger.warn("updated {}", leaders);
        });
    }

}
