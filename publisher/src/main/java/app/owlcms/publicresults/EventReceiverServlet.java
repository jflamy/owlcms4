package app.owlcms.publicresults;

import java.io.IOException;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

@WebServlet("/update")
public class EventReceiverServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(EventReceiverServlet.class);
    private String secret = StartupUtils.getStringParam("UPDATEKEY");
    static EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return eventBus;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL.  Standard says there shouldn't be a 405 on a get. Sue me.
        resp.sendError(405);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
//        Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
//        for (Entry<String, String[]> pair : pairs) {
//            logger.warn("{} = {}", pair.getKey(), pair.getValue()[0]);
//        }
        
        String updateKey = req.getParameter("updateKey");
        if (updateKey == null || !updateKey.equals(secret)) {
            logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, updateKey);
            resp.sendError(401,"Denied, wrong credentials");
            return;
        }

        UpdateEvent updateEvent = new UpdateEvent();

        updateEvent.setAttempt(req.getParameter("attempt"));
        updateEvent.setCategoryName(req.getParameter("categoryName"));
        updateEvent.setFullName(req.getParameter("fullName"));
        updateEvent.setGroupName(req.getParameter("groupName"));

        updateEvent.setHidden(Boolean.valueOf(req.getParameter("hidden")));
        String startNumber = req.getParameter("startNumber");
        updateEvent.setStartNumber(startNumber != null ? Integer.parseInt(startNumber) : 0);
        updateEvent.setTeamName(req.getParameter("teamName"));
        String weight = req.getParameter("weight");
        updateEvent.setWeight(weight != null ? Integer.parseInt(weight) : null);

        updateEvent.setAthletes(req.getParameter("groupAthletes"));
        updateEvent.setLeaders(req.getParameter("leaders"));
        updateEvent.setLiftsDone(req.getParameter("liftsDone"));
        updateEvent.setTranslationMap(req.getParameter("translationMap"));
        updateEvent.setWideTeamNames(Boolean.parseBoolean(req.getParameter("wideTeamNames")));
        String timeAllowed = req.getParameter("timeAllowed");
        updateEvent.setTimeAllowed(timeAllowed != null ? Integer.parseInt(req.getParameter("timeAllowed")) : null);

        eventBus.post(updateEvent);
    }

}