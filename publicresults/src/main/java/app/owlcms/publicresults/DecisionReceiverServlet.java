/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import app.owlcms.uievents.DecisionEvent;
import app.owlcms.uievents.DecisionEventType;
import app.owlcms.utils.ProxyUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/decision")
public class DecisionReceiverServlet extends HttpServlet implements Traceable {

    private static String defaultFopName;
//    static EventBus eventBus = new AsyncEventBus(DecisionReceiverServlet.class.getSimpleName(),
//            Executors.newCachedThreadPool());

    public static EventBus getEventBus() {
        return UpdateReceiverServlet.getEventBus();
    }

    private Logger logger = (Logger) LoggerFactory.getLogger(DecisionReceiverServlet.class);

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
        if (StartupUtils.isDebugSetting()) {
            Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
            this.getLogger()./**/warn("++++ decision update received from {}", ProxyUtils.getClientIp(req));
            tracePairs(pairs);
        }

        String updateKey = req.getParameter("updateKey");
        if (updateKey == null || !updateKey.equals(this.secret)) {
            this.getLogger().error("denying access from {} expected {} got {} ", req.getRemoteHost(), this.secret,
                    updateKey);
            resp.sendError(401, "Denied, wrong credentials");
            return;
        }

        DecisionEvent decisionEvent = new DecisionEvent();

        String eventTypeString = req.getParameter("decisionEventType");
        DecisionEventType eventType = null;
        try {
            eventType = DecisionEventType.valueOf(eventTypeString);
            decisionEvent.setEventType(eventType);
        } catch (Exception e) {
            String message = MessageFormat.format("unknown decision event type {0}", eventTypeString);
            this.getLogger().error(message);
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
        decisionEvent.setRecordKind(req.getParameter("recordKind"));
        decisionEvent.setRecordMessage(req.getParameter("recordMessage"));
        decisionEvent.setMode(req.getParameter("mode"));

        String fopName = decisionEvent.getFopName();

        getEventBus().post(decisionEvent);

        if (defaultFopName == null) {
            defaultFopName = fopName;
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