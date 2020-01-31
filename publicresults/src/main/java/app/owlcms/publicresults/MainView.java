package app.owlcms.publicresults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;

import app.owlcms.displays.scoreboard.ScoreWithLeaders;
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

@Route
@Push
public class MainView extends VerticalLayout {

    static Text text;

    private static Logger logger = (Logger) LoggerFactory.getLogger(MainView.class);
    private UI ui;

    public MainView() {

        text = new Text("Waiting for updates from competition site.");
        buildHomePage();
    }

    private void buildHomePage() {
        Set<String> fopNames = EventReceiverServlet.updateCache.keySet();
        if (fopNames.size() == 0) {
            removeAll();
            add(text);
        } else {
            createButtons(fopNames);
        }
    }

    private void createButtons(Set<String> fopNames) {
        removeAll();
        fopNames.stream().sorted().forEach(fopName -> {
            Button fopButton = new Button(getTranslation("Platform") + " " + fopName,
                    buttonClickEvent -> {
                        String url = URLUtils.getRelativeURLFromTargetClass(ScoreWithLeaders.class);
                        HashMap<String, List<String>> params = new HashMap<>();
                        params.put("fop", Arrays.asList(fopName));
                        QueryParameters parameters = new QueryParameters(params);
                        UI.getCurrent().navigate(url, parameters);
                    });
            add(fopButton);
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = UI.getCurrent();
        EventReceiverServlet.getEventBus().register(this);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        EventReceiverServlet.getEventBus().unregister(this);
    }
    
    @Subscribe
    public void update(UpdateEvent e) {
        if (ui == null) {
            logger.error("ui is null!?");
            return;
        }
       ui.access(() -> {
            buildHomePage();
        });
    }

}
