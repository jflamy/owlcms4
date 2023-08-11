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
public class UpdateReceiverServlet extends HttpServlet {

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

    Logger logger = (Logger) LoggerFactory.getLogger(UpdateReceiverServlet.class);

    private String secret = StartupUtils.getStringParam("updateKey");

    /**
     * @see jakarta.servlet.http.HttpServlet#doGet(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // get makes no sense on this URL. Standard says there shouldn't be a 405 on a get. Sue me.
        resp.sendError(405);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String updateKey = req.getParameter("updateKey");
            if (updateKey == null || !updateKey.equals(secret)) {
                logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, updateKey);
                resp.sendError(401, "Denied, wrong credentials");
                return;
            }
            
            if (ResourceWalker.getLocalDirPath() == null) {
                String message = "Local override directory not present: requesting remote configuration files.";
                logger.info(message);
                logger.info("requesting customization");
                resp.sendError(412, "Missing configuration files.");
                return;
            }

            try {
                ResourceWalker.getFileOrResource("styles/results.css");
            } catch (Exception e) {
                logger.error("styles directory not found");
            }

            if (StartupUtils.isDebugSetting()) {
                logger.setLevel(Level.DEBUG);
                Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
                logger./**/debug("update received from {}", ProxyUtils.getClientIp(req));
                if (StartupUtils.isTraceSetting()) {
                    for (Entry<String, String[]> pair : pairs) {
                        logger./**/debug("    {} = {}", pair.getKey(), pair.getValue()[0]);
                    }
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

            updateEvent.setHidden(Boolean.valueOf(req.getParameter("hidden")));
            String startNumber = req.getParameter("startNumber");
            updateEvent.setStartNumber(startNumber != null ? Integer.parseInt(startNumber) : 0);
            updateEvent.setTeamName(req.getParameter("teamName"));
            String weight = req.getParameter("weight");
            updateEvent.setWeight(weight != null ? Integer.parseInt(weight) : null);

            updateEvent.setNoLiftRanks(req.getParameter("noLiftRanks"));
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

            String breakString = req.getParameter("break");
            String breakTypeString = req.getParameter("breakType");
            String breakRemainingString = req.getParameter("breakRemaining");
            String breakIsIndefiniteString = req.getParameter("breakIsIndefinite");
            updateEvent.setBreak(breakString != null ? Boolean.valueOf(breakString) : null);
            BreakType bt = breakTypeString != null ? BreakType.valueOf(breakTypeString) : null;
            updateEvent.setBreakType(bt);
            updateEvent.setBreakRemaining(breakRemainingString != null ? Integer.parseInt(breakRemainingString) : null);
            updateEvent.setIndefinite(Boolean.parseBoolean(breakIsIndefiniteString));
            
            String sinclairMeetString = req.getParameter("sinclairMeet");
            updateEvent.setSinclairMeet(Boolean.parseBoolean(sinclairMeetString));

            if (bt == BreakType.GROUP_DONE) {
                updateEvent.setRecords(null);
                updateEvent.setRecordKind("none");
                updateEvent.setRecordMessage("");
            }

            String fopName = updateEvent.getFopName();
            // put in the cache first so events can know which FOPs are active;

            long now = System.currentTimeMillis();
            if (now - lastUpdate < 500) {
                // short time range, is this a duplicate?
                UpdateEvent prevUpdate = updateCache.get(fopName);
                if (prevUpdate != null && updateEvent.hashCode() == prevUpdate.hashCode()) {
                    logger./**/warn("duplicate event ignored");
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
            resp.sendError(200);
        } catch (Exception e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
    }

}