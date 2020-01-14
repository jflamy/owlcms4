package publish;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import ch.qos.logback.classic.Logger;

@WebServlet("/results")
public class ResultsListenerServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(ResultsListenerServlet.class);
    static EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return eventBus;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        PrintWriter osw = new PrintWriter(resp.getOutputStream());
        osw.print("Hello!");
        osw.flush();
        osw.close();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
        for (Entry<String, String[]> pair : pairs) {
            logger.warn("{} = {}", pair.getKey(), pair.getValue()[0]);
        }

        UpdateEvent updateEvent = new UpdateEvent();

        updateEvent.setAttempt(req.getParameter("attempt"));
        updateEvent.setCategoryName(req.getParameter("categoryName"));
        updateEvent.setFullName(req.getParameter("fullName"));
        updateEvent.setGroupName(req.getParameter("groupName"));

        updateEvent.setHidden(req.getParameter("hidden"));
        updateEvent.setStartNumber(req.getParameter("startNumber"));
        updateEvent.setTeamName(req.getParameter("teamName"));
        updateEvent.setWeight(req.getParameter("weight"));

        updateEvent.setAthletes(req.getParameter("groupAthletes"));
        updateEvent.setLeaders(req.getParameter("leaders"));
        updateEvent.setLiftsDone(req.getParameter("liftsDone"));
        updateEvent.setTranslationMap(req.getParameter("translationMap"));
        updateEvent.setWideTeamNames(Boolean.parseBoolean(req.getParameter("wideTeamNames")));

        eventBus.post(updateEvent);
    }

}