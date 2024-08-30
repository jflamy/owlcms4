/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/expired")
public class ExpiredServlet extends HttpServlet implements Traceable {

    private Logger logger = (Logger) LoggerFactory.getLogger(ExpiredServlet.class);

    public ExpiredServlet() {
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
        //resp.sendError(405);
        doPost(req,resp);
    }

    /**
     * @see jakarta.servlet.http.HttpServlet#doPost(jakarta.servlet.http.HttpServletRequest,
     *      jakarta.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        String reloadTitle = req.getParameter("reloadTitle");
        String reloadText = req.getParameter("reloadText");
        String reloadLabel = req.getParameter("reloadLabel");
        String reloadUrl = req.getParameter("reloadUrl");
        // stylesheet is in web server root, as this is not served by Vaadin
        // EmbeddedJetty: webapp directory at top level of class/resources
        String page = """
                <html>
                <head>
                    <link rel="stylesheet" type="text/css" href="reload.css">
                </head>
                <body>
                    <div class="wrapper">
                        <h2>%s</h2>
                        <p>%s</p>
                        <div class="button-bar">
                            <button onclick="location.href='%s';" type="button">%s</button>
                        </div>
                    </div>
                <body>
                </html>
                """.formatted(reloadTitle, reloadText, reloadUrl, reloadLabel);
        try {
            resp.getWriter().append(page);
            resp.flushBuffer();
        } catch (IOException e) {
            LoggerUtils.logError(logger, e);
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