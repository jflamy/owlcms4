/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.components.elements.AthleteTimerElementPR;
import app.owlcms.components.elements.BreakTimerElementPR;
import app.owlcms.prutils.CountdownTimer;
import app.owlcms.uievents.BreakTimerEvent;
import app.owlcms.uievents.TimerEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/timer")
public class TimerReceiverServlet extends HttpServlet implements Traceable {

    private static String defaultFopName;
    private static Map<String, CountdownTimer> athleteTimerCache = new HashMap<>();
    private static Map<String, CountdownTimer> breakTimerCache = new HashMap<>();

    public static EventBus getEventBus() {
        return UpdateReceiverServlet.getEventBus();
    }

    private String secret = StartupUtils.getStringParam("updateKey");

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL. Standard says there shouldn't be a 405 on a
        // get,
        // but "disallowed" is what makes most sense as a return code.
        resp.sendError(405);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            resp.setCharacterEncoding("UTF-8");
            if (StartupUtils.isTraceSetting()) {
                Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
                getLogger()./**/warn("---- timer update received from {}", ProxyUtils.getClientIp(req));
                tracePairs(pairs);
            }

            String updateKey = req.getParameter("updateKey");
            if (updateKey == null || !updateKey.equals(this.secret)) {
                getLogger().error("denying access from {} expected {} got {} ", req.getRemoteHost(),
                        this.secret,
                        updateKey);
                resp.sendError(401, "Denied, wrong credentials");
                return;
            }

            String fopName = processTimerReq(req, resp, getLogger());

            if (defaultFopName == null) {
                defaultFopName = fopName;
            }
        } catch (NumberFormatException | IOException e) {
            getLogger().error(LoggerUtils.stackTrace(e));
        }
    }

    public static String processTimerReq(HttpServletRequest req, HttpServletResponse resp, Logger logger) throws IOException {
        TimerEvent timerEvent = null;
        BreakTimerEvent breakTimerEvent = null;

        String athleteTimerEventTypeString = req.getParameter("athleteTimerEventType");
        String breakTimerEventTypeString = req.getParameter("breakTimerEventType");
        String fopName = req.getParameter("fopName");
        
        logger.debug("processing timer request {} {} {}", fopName, athleteTimerEventTypeString, breakTimerEventTypeString);

        int athleteMillis = computeAthleteTargetDuration(req);
        int breakMillis = computeBreakTargetDuration(req);

        String indefiniteString = req.getParameter("timerIndefiniteBreak");
        boolean indefinite = indefiniteString != null ? Boolean.valueOf(indefiniteString) : false;
        String silentString = req.getParameter("silent");
        boolean silent = silentString != null ? Boolean.valueOf(silentString) : false;

        if (athleteTimerEventTypeString != null) {
            CountdownTimer athleteTimer = getAthleteTimerFromCache(fopName);
            if (athleteTimerEventTypeString.equals("SetTime")) {
                athleteTimer.set(athleteMillis);
                timerEvent = new TimerEvent.SetTime(athleteMillis);
            } else if (athleteTimerEventTypeString.equals("StopTime")) {
                athleteTimer.set(athleteMillis);
                timerEvent = new TimerEvent.StopTime(athleteMillis);
            } else if (athleteTimerEventTypeString.equals("StartTime")) {
                athleteTimer.restartAtValue(athleteMillis);
                timerEvent = new TimerEvent.StartTime(athleteMillis, silent);
            } else {
                String message = MessageFormat.format("**** unknown athlete timer event type {0}", athleteTimerEventTypeString);
                logger.error(message);
                if (resp != null) {
                    resp.sendError(400, message);
                }
            }
        }
        if (breakTimerEventTypeString != null) {
            CountdownTimer breakTimer = getBreakTimerFromCache(fopName);
            if (breakTimerEventTypeString.equals("BreakPaused")) {
                breakTimer.stop();
                breakTimerEvent = new BreakTimerEvent.BreakPaused(breakMillis);
            } else if (breakTimerEventTypeString.equals("BreakStarted")) {
                breakTimer.restartAtValue(indefinite ? -1 : breakMillis);
                breakTimerEvent = new BreakTimerEvent.BreakStart(breakMillis, indefinite);
            } else if (breakTimerEventTypeString.equals("BreakDone")) {
                breakTimer.stop();
                breakTimerEvent = new BreakTimerEvent.BreakDone(null);
            } else if (breakTimerEventTypeString.equals("BreakSetTime")) {
                breakTimer.set(indefinite ? -1 : athleteMillis);
                breakTimerEvent = new BreakTimerEvent.BreakSetTime(breakMillis, indefinite);
            } else {
                String message = MessageFormat.format("**** unknown break timer event type {0}", breakTimerEventTypeString);
                logger.error(message);
                if (resp != null) {
                    resp.sendError(400, message);
                }
            }
        }
        if (timerEvent != null) {
            timerEvent.setFopName(fopName);
            getEventBus().post(timerEvent);
        }
        if (breakTimerEvent != null) {
            breakTimerEvent.setFopName(fopName);
            String mode = req.getParameter("mode");
            breakTimerEvent.setMode(mode);
            getEventBus().post(breakTimerEvent);
        }
        return fopName;
    }

    private static CountdownTimer getAthleteTimerFromCache(String fopName) {
        CountdownTimer t = athleteTimerCache.get(fopName);
        if (t == null) {
            t = new CountdownTimer(-1);
            athleteTimerCache.put(fopName, t);
        }
        return t;
    }
    
    private static CountdownTimer getBreakTimerFromCache(String fopName) {
        CountdownTimer t = breakTimerCache.get(fopName);
        if (t == null) {
            t = new CountdownTimer(0);
            breakTimerCache.put(fopName, t);
        }
        return t;
    }

    private static int computeAthleteTargetDuration(HttpServletRequest req) {
        String startTimeMillisString = req.getParameter("athleteStartTimeMillis");
        String secondsString = req.getParameter("athleteMillisRemaining");
        if (startTimeMillisString == null) {
            // relative time
            int deltaMillis = secondsString != null ? Integer.valueOf(secondsString) : 0;
            return deltaMillis;
        } else {
            long startTimeMillis = secondsString != null ? Long.valueOf(startTimeMillisString)
                    : System.currentTimeMillis();
            int deltaMillis = secondsString != null ? Integer.valueOf(secondsString) : 0;
            long targetMillis = startTimeMillis + deltaMillis;
            int milliSeconds = (int) (targetMillis - System.currentTimeMillis());
            return milliSeconds;
        }
    }

    private static int computeBreakTargetDuration(HttpServletRequest req) {
        String startTimeMillisString = req.getParameter("breakStartTimeMillis");
        String secondsString = req.getParameter("breakMillisRemaining");
        long startTimeMillis = secondsString != null ? Long.valueOf(startTimeMillisString) : System.currentTimeMillis();
        int deltaMillis = secondsString != null ? Integer.valueOf(secondsString) : 0;
        long targetMillis = startTimeMillis + deltaMillis;
        int milliSeconds = (int) (targetMillis - System.currentTimeMillis());
        return milliSeconds;
    }

    @Override
    public Logger getLogger() {
        return (Logger) LoggerFactory.getLogger(TimerReceiverServlet.class);
    }

    public static void syncAthleteTimer(String fopName2, AthleteTimerElementPR timer) {
        CountdownTimer t = getAthleteTimerFromCache(fopName2);
        long athleteMillis = t.getTimeRemaining();
        TimerEvent timerEvent = null;
        if (athleteMillis >= 0) {
            if (t.isRunning()) {
                timerEvent = new TimerEvent.StartTime((int) athleteMillis, true);
                
            } else {
                timerEvent = new TimerEvent.SetTime((int) athleteMillis);
            }
        }
        if (timerEvent != null) {
            timerEvent.setFopName(fopName2);
            getEventBus().post(timerEvent);
        } 
    }

    public static void syncBreakTimer(String fopName2, BreakTimerElementPR breakTimer) {
        CountdownTimer t = getBreakTimerFromCache(fopName2);
        @SuppressWarnings("unused")
        var logger = (Logger) LoggerFactory.getLogger(TimerReceiverServlet.class);
        //logger.debug("getBreakTimerFromCache {} {}",System.identityHashCode(t),t.getTimeRemaining());
        long athleteMillis = t.getTimeRemaining();
        BreakTimerEvent timerEvent = null;
        if (athleteMillis >= 0) {
            //logger.debug("t.isRunning() {}",t.isRunning());
            if (t.isRunning()) {
                timerEvent = new BreakTimerEvent.BreakStart((int) athleteMillis, t.isIndefinite());    
            } else {
                timerEvent = new BreakTimerEvent.BreakSetTime((int) athleteMillis, t.isIndefinite());
            }
        }
        if (timerEvent != null) {
            timerEvent.setFopName(fopName2);
            getEventBus().post(timerEvent);
        } 
    }

}