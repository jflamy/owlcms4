/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import app.owlcms.uievents.BreakType;
import app.owlcms.uievents.UpdateEvent;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/update")
public class UpdateReceiverServlet extends HttpServlet implements Traceable {

    private static String defaultFopName;
    static EventBus eventBus = new AsyncEventBus(UpdateReceiverServlet.class.getSimpleName(),
            Executors.newCachedThreadPool());
    private static Map<String, UpdateEvent> updateCache = new HashMap<>();
    static long lastUpdate = 0;

    public static EventBus getEventBus() {
        return eventBus;
    }

    public static Map<String, UpdateEvent> getUpdateCache() {
        return updateCache;
    }

    public static void setUpdateCache(Map<String, UpdateEvent> updateCache) {
        UpdateReceiverServlet.updateCache = updateCache;
    }

    public static UpdateEvent sync(String fopName) {
        if (fopName == null) {
            fopName = defaultFopName;
        }
        UpdateEvent updateEvent = updateCache.get(fopName);
        if (updateEvent != null) {
            return updateEvent;
        }
        return null;
    }

    private Logger logger = (Logger) LoggerFactory.getLogger(UpdateReceiverServlet.class);

    private String secret = StartupUtils.getStringParam("updateKey");

    public UpdateReceiverServlet() {
        this.getLogger().setLevel(Level.DEBUG);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL. Standard says there shouldn't be a 405 on a
        // get. Sue me.
        resp.sendError(405);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        // TODO create timer and decision events
        // TODO compute checksum of update, timer, decision, issue bus events if changed
        try {
            String updateKey = req.getParameter("updateKey");
            if (updateKey == null || !updateKey.equals(this.secret)) {
                this.getLogger().error("denying access from {} expected {} got {} ", req.getRemoteHost(), this.secret,
                        updateKey);
                resp.sendError(401, "Denied, wrong credentials");
                return;
            }

            if (ResourceWalker.getLocalDirPath() == null) {
                String message = "Local override directory not present: requesting remote configuration files.";
                this.getLogger().info(message);
                this.getLogger().info("requesting customization");
                resp.sendError(412, "Missing configuration files.");
                return;
            }

            if (StartupUtils.isDebugSetting()) {
                this.getLogger().setLevel(Level.TRACE);
                Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
                if (StartupUtils.isTraceSetting()) {
                    this.getLogger()./**/trace("update received from {}", ProxyUtils.getClientIp(req));
                    tracePairs(pairs);
                }
            }

            UpdateEvent updateEvent = new UpdateEvent();

            updateEvent.setCompetitionName(req.getParameter("competitionName"));
            updateEvent.setFopName(req.getParameter("fop"));
            updateEvent.setFopState(req.getParameter("fopState"));
            updateEvent.setStylesDir(req.getParameter("stylesDir"));

            updateEvent.setAttempt(req.getParameter("attempt"));
            updateEvent.setCategoryName(req.getParameter("categoryName"));
            updateEvent.setFullName(req.getParameter("fullName"));
            updateEvent.setGroupName(req.getParameter("groupName"));
            updateEvent.setGroupInfo(req.getParameter("groupInfo"));

            updateEvent.setHidden(Boolean.valueOf(req.getParameter("hidden")));
            String startNumber = req.getParameter("startNumber");
            updateEvent.setStartNumber(startNumber != null ? Integer.parseInt(startNumber) : 0);
            updateEvent.setTeamName(req.getParameter("teamName"));
            String weight = req.getParameter("weight");
            updateEvent.setWeight(weight != null ? Integer.parseInt(weight) : null);

            updateEvent.setShowLiftRanks(Boolean.parseBoolean(req.getParameter("showLiftRanks")));
            updateEvent.setShowTotalRank(Boolean.parseBoolean(req.getParameter("showTotalRank")));
            updateEvent.setShowSinclair(Boolean.parseBoolean(req.getParameter("showSinclair")));
            updateEvent.setShowSinclairRank(Boolean.parseBoolean(req.getParameter("showSinclairRank")));
            
            updateEvent.setAthletes(req.getParameter("groupAthletes"));
            updateEvent.setLiftingOrderAthletes(req.getParameter("liftingOrderAthletes"));
            updateEvent.setLeaders(req.getParameter("leaders"));

            updateEvent.setRecords(req.getParameter("records"));
            updateEvent.setRecordKind(req.getParameter("recordKind"));
            updateEvent.setRecordMessage(req.getParameter("recordMessage"));
            updateEvent.setLiftsDone(req.getParameter("liftsDone"));

            updateEvent.setWideTeamNames(Boolean.parseBoolean(req.getParameter("wideTeamNames")));
            String timeAllowed = req.getParameter("timeAllowed");
            updateEvent.setTimeAllowed(timeAllowed != null ? Integer.parseInt(req.getParameter("timeAllowed")) : null);

            updateEvent.setTranslationMap(req.getParameter("translationMap"));

            String mode = req.getParameter("mode");
            updateEvent.setMode(mode);
            
            TimerReceiverServlet.processTimerReq(req, null, getLogger());

            String breakTypeString = req.getParameter("breakType");
            updateEvent.setBreak("true".equalsIgnoreCase(req.getParameter("break")));
            if (breakTypeString == BreakType.GROUP_DONE.name()) {
                updateEvent.setRecords(null);
                updateEvent.setRecordKind("none");
                updateEvent.setRecordMessage("");
                updateEvent.setDone(true);
            }
            updateEvent.setCeremonyType(req.getParameter("ceremonyType"));
            updateEvent.setBreakType(req.getParameter("breakType"));
            
            String sinclairMeetString = req.getParameter("sinclairMeet");
            updateEvent.setSinclairMeet(Boolean.parseBoolean(sinclairMeetString));

            String fopName = updateEvent.getFopName();
            // put in the cache first so events can know which FOPs are active;

            long now = System.currentTimeMillis();

            // the computed hashcode is not included in the hashcode
            // this avoids every servlet recomputing it.
            updateEvent.setHashCode(updateEvent.hashCode());
            if (now - lastUpdate < 500) {
                // short time range, is this a duplicate?
                UpdateEvent prevUpdate = updateCache.get(fopName);
                if (prevUpdate != null && updateEvent.getHashCode() == prevUpdate.getHashCode()) {
                    this.getLogger()./**/warn("duplicate event ignored");
                } else {
                    updateCache.put(fopName, updateEvent);
                    eventBus.post(updateEvent);
                }
            } else {
                updateCache.put(fopName, updateEvent);
                eventBus.post(updateEvent);
            }

            if (defaultFopName == null) {
                defaultFopName = fopName;
            }

            // TODO create timer and decision objects as well.
            resp.sendError(200);
        } catch (Exception e) {
            this.getLogger().error(LoggerUtils.stackTrace(e));
        }
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    void setLogger(Logger logger) {
        this.logger = logger;
    }
    
}