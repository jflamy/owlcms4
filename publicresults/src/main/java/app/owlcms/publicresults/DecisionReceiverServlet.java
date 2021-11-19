/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import app.owlcms.uievents.DecisionEvent;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

@WebServlet("/decision")
public class DecisionReceiverServlet extends HttpServlet {

    private static String defaultFopName;
    static EventBus eventBus = new AsyncEventBus(Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return eventBus;
    }

    Logger logger = (Logger) LoggerFactory.getLogger(DecisionReceiverServlet.class);

    private String secret = StartupUtils.getStringParam("updateKey");

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
//        if (StartupUtils.isDebugSetting()) {
//            Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
//            logger./**/warn("++++ decision received from {}", ProxyUtils.getClientIp(req));
//            for (Entry<String, String[]> pair : pairs) {
//                logger./**/warn("{} = {}", pair.getKey(), pair.getValue()[0]);
//            }
//        }

        String updateKey = req.getParameter("updateKey");
        if (updateKey == null || !updateKey.equals(secret)) {
            logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, updateKey);
            resp.sendError(401, "Denied, wrong credentials");
            return;
        }

        DecisionEvent decisionEvent = new DecisionEvent();

        String eventTypeString = req.getParameter("eventType");
        DecisionEventType eventType = null;
        try {
            eventType = DecisionEventType.valueOf(eventTypeString);
            decisionEvent.setEventType(eventType);
        } catch (Exception e) {
            String message = MessageFormat.format("unknown event type {0}", eventTypeString);
            logger.error(message);
            resp.sendError(400, message);
            return;
        }

        String ds = req.getParameter("d1");
        decisionEvent.setDecisionLight1(ds != null ? Boolean.valueOf(ds) : null);
        ds = req.getParameter("d2");
        decisionEvent.setDecisionLight2(ds != null ? Boolean.valueOf(ds) : null);
        ds = req.getParameter("d3");
        decisionEvent.setDecisionLight3(ds != null ? Boolean.valueOf(ds) : null);
        decisionEvent.setDecisionLightsVisible(Boolean.valueOf(req.getParameter("decisionsVisible")));
        decisionEvent.setDown(Boolean.valueOf(req.getParameter("down")));
        decisionEvent.setFopName(req.getParameter("fop"));

        String fopName = decisionEvent.getFopName();

        eventBus.post(decisionEvent);

        if (defaultFopName == null) {
            defaultFopName = fopName;
        }
    }

}