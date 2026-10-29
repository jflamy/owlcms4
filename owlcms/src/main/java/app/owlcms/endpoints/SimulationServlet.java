/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.endpoints;

import java.io.IOException;

import org.slf4j.LoggerFactory;

import app.owlcms.access.SameMachineProof;
import app.owlcms.audit.AuthAudit;
import app.owlcms.simulation.SimulationControl;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Scripted control of the competition simulation, for curl and test scripts. People use the {@code simulation} page.
 * <ul>
 * <li>{@code POST /simulation/start} with optional {@code skipDone}, {@code skipBefore}, {@code platforms},
 * {@code randomDeclarationJumps}</li>
 * <li>{@code POST /simulation/stop}</li>
 * <li>{@code GET /simulation/status}</li>
 * </ul>
 * Same proof as the control panel stop: the control panel token as {@code Authorization: Bearer} or {@code token},
 * required whenever OWLCMS has one.
 */
@SuppressWarnings("serial")
@WebServlet(urlPatterns = { SimulationServlet.START, SimulationServlet.STOP, SimulationServlet.STATUS })
public class SimulationServlet extends HttpServlet {

	static final String START = "/simulation/start";
	static final String STOP = "/simulation/stop";
	static final String STATUS = "/simulation/status";

	private static final Logger logger = (Logger) LoggerFactory.getLogger(SimulationServlet.class);

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!STATUS.equals(request.getServletPath())) {
			response.setHeader("Allow", "POST");
			reply(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "use POST");
			return;
		}
		if (authorize(request, response)) {
			reply(response, HttpServletResponse.SC_OK, SimulationControl.isRunning() ? "running" : "stopped");
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		if (!authorize(request, response)) {
			return;
		}
		switch (request.getServletPath()) {
			case START -> {
				SimulationControl.Result result = SimulationControl.start(isChecked(request, "skipDone"),
				        request.getParameter("skipBefore"), request.getParameter("platforms"),
				        isChecked(request, "randomDeclarationJumps"));
				if (result == SimulationControl.Result.STARTED) {
					reply(response, HttpServletResponse.SC_ACCEPTED, "started");
				} else {
					reply(response, HttpServletResponse.SC_CONFLICT, "already running");
				}
			}
			case STOP -> reply(response, HttpServletResponse.SC_OK,
			        SimulationControl.stop() == SimulationControl.Result.STOPPED ? "stopped" : "not running");
			default -> reply(response, HttpServletResponse.SC_NOT_FOUND, "not found");
		}
	}

	private boolean authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// use proxyutils because this is a plain servlet, not a Vaadin servlet
		String host = ProxyUtils.getClientIp(request);
		String refusal = SameMachineProof.refusal(request);
		if (refusal == null) {
			logger.info("{} authorized {}", host, request.getServletPath());
			return true;
		}
		logger./**/warn("{} denied {} reason={}", host, request.getServletPath(), refusal);
		AuthAudit.tokenRefused(host, request.getServletPath(), refusal, null, null);
		if ("not-local".equals(refusal)) {
			reply(response, HttpServletResponse.SC_FORBIDDEN, "only available from the computer running OWLCMS");
		} else {
			response.setHeader("WWW-Authenticate", "Bearer");
			reply(response, HttpServletResponse.SC_UNAUTHORIZED, "control panel token required");
		}
		return false;
	}

	private static void reply(HttpServletResponse response, int status, String text) throws IOException {
		response.setStatus(status);
		response.setContentType("text/plain;charset=UTF-8");
		response.getWriter().println(text);
		response.flushBuffer();
	}

	private static boolean isChecked(HttpServletRequest request, String name) {
		String value = request.getParameter(name);
		return "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
	}
}
