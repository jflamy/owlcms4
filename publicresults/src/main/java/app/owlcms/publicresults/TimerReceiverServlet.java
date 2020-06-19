package app.owlcms.publicresults;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.Set;
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
import app.owlcms.utils.URLUtils;
import ch.qos.logback.classic.Logger;

@WebServlet("/timer")
public class TimerReceiverServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(TimerReceiverServlet.class);
    private String secret = StartupUtils.getStringParam("updateKey");
    private static String defaultFopName;
    static EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return eventBus;
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL. Standard says there shouldn't be a 405 on a get,
        // but "disallowed" is what makes most sense as a return code.
        resp.sendError(405);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (StartupUtils.getBooleanParam("DEBUG")) {
            Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
            logger./**/warn("---- update received from {}", URLUtils.getClientIp(req));
            for (Entry<String, String[]> pair : pairs) {
                logger./**/warn("{} = {}", pair.getKey(), pair.getValue()[0]);
            }
        }

        String updateKey = req.getParameter("updateKey");
        if (updateKey == null || !updateKey.equals(secret)) {
            logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, updateKey);
            resp.sendError(401, "Denied, wrong credentials");
            return;
        }

        TimerEvent timerEvent = null;
        BreakTimerEvent breakTimerEvent = null;

        String eventTypeString = req.getParameter("eventType");
        Class<?> eventClass = null;

        try {
            eventClass = Class.forName(eventTypeString);
        } catch (Exception e1) {
            String message = MessageFormat.format("unknown event type {0}", eventTypeString);
            logger.error(message);
            resp.sendError(400, message);
            return;
        }

        String secondsString = req.getParameter("seconds");
        int seconds = secondsString != null ? Integer.valueOf(secondsString) : 0;
        if (eventClass.isAssignableFrom(TimerEvent.SetTime.class)) {
            timerEvent = new TimerEvent.SetTime(seconds, null);
        } else if (eventClass.isAssignableFrom(TimerEvent.StopTime.class)) {
            timerEvent = new TimerEvent.StopTime(seconds, null);
        } else if (eventClass.isAssignableFrom(TimerEvent.StartTime.class)) {
            String silentString = req.getParameter("silent");
            timerEvent = new TimerEvent.StartTime(seconds, null,
                    silentString != null ? Boolean.valueOf(silentString) : false);
        } else if (eventClass.isAssignableFrom(BreakTimerEvent.SetTime.class)) {
            timerEvent = new TimerEvent.SetTime(seconds, null);
        } else if (eventClass.isAssignableFrom(BreakTimerEvent.StopTime.class)) {
            timerEvent = new TimerEvent.StopTime(seconds, null);
        } else if (eventClass.isAssignableFrom(BreakTimerEvent.StartTime.class)) {
            String silentString = req.getParameter("silent");
            breakTimerEvent = new BreakTimerEvent.StartTime(seconds, null,
                    silentString != null ? Boolean.valueOf(silentString) : false);
        } else if (eventClass.isAssignableFrom(BreakTimerEvent.BreakStart.class)) {
            breakTimerEvent = new BreakTimerEvent.BreakStart(seconds, null);
        } else if (eventClass.isAssignableFrom(BreakTimerEvent.BreakDone.class)) {
            breakTimerEvent = new BreakTimerEvent.BreakDone(null);
        }
        String fopName = timerEvent.getFopName();

        if (timerEvent != null) {
            eventBus.post(timerEvent);
        }
        if (breakTimerEvent != null) {
            eventBus.post(breakTimerEvent);
        }

        if (defaultFopName == null) {
            defaultFopName = fopName;
        }
    }

}