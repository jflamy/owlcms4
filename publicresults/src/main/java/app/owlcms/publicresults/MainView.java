package app.owlcms.publicresults;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
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
    private UI ui;

    public MainView() {
        text = new Text("Waiting...");
        add(text);
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
        ui = null;
    }
    
    @Subscribe
    public void update(UpdateEvent e) {
        if (ui == null) return;
        
        String leaders = e.getLeaders();
        logger.warn("updating {}", leaders);
        logger.warn("current {}", ui);
        ui.access(() -> {
            text.setText(leaders);
            logger.warn("updated {}", leaders);
        });
    }

}
