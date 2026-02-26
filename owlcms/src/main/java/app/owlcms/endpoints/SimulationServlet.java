/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
//
//  Copyright (C) 2009 BalusC
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Lesser General Public License as published by the Free Software Foundation, either version 3
//  of the License, or (at your option) any later version.
//
//  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
//  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//
//  You should have received a copy of the GNU Lesser General Public License along with this library.
//  If not, see <http://www.gnu.org/licenses/>.

package app.owlcms.endpoints;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.simulation.CompetitionSimulator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A file servlet supporting resume of downloads and client-side caching and GZIP of text content. This servlet can also be used for images, client-side caching
 * would become more efficient. This servlet can also be used for text files, GZIP would decrease network bandwidth.
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
@SuppressWarnings("serial")
/**
 * Modified to fetch files under the ./local directory relative to the startup directory and, failing that, as a resource on the classpath.
 *
 * @author Jean-François Lamy
 *
 */
@WebServlet("/simulation/*")
public class SimulationServlet extends HttpServlet {

	// Helpers (can be refactored to public utility class)
	// ----------------------------------------

	private static Logger logger = (Logger) LoggerFactory.getLogger(SimulationServlet.class);
	private static volatile Thread simulationThread;
	// { logger.setLevel(Level.DEBUG); }

	// Inner classes
	// ------------------------------------------------------------------------------

	/**
	 * Process GET request.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		processRequest(request, response, true);
	}

	/**
	 * Process HEAD request. This returns the same headers as GET request, but without content.
	 *
	 * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		processRequest(request, response, false);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		processRequest(request, response, true);
	}

	/**
	 * Process the actual request.
	 *
	 * @param request  The request to be processed.
	 * @param response The response to be created.
	 * @param content  Whether the request body should be written (GET) or not (HEAD).
	 * @throws IOException If something fails at I/O level.
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content)
	        throws IOException {
		logger.info("processing simulation request");
		if (!authorize(request, response)) {
			return;
		}

		String action = getAction(request);
		String message = null;
		if ("start".equalsIgnoreCase(action)) {
			message = startSimulation();
		} else if ("stop".equalsIgnoreCase(action)) {
			message = stopSimulation();
		}

		if (!content) {
			response.setStatus(200);
			response.flushBuffer();
			return;
		}

		writePage(response, message);
	}

	private static synchronized String startSimulation() {
		if (CompetitionSimulator.isRunning()) {
			return "Simulation is already running.";
		}
		simulationThread = new Thread(() -> {
			try {
				new CompetitionSimulator().runSimulation();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				logger./**/warn("simulation thread interrupted");
			} catch (Throwable t) {
				logger.error("{}", LoggerUtils.stackTrace(t));
			}
		}, "competition-simulation");
		simulationThread.setDaemon(true);
		simulationThread.start();
		return "Simulation start requested.";
	}

	private static synchronized String stopSimulation() {
		boolean wasRunning = CompetitionSimulator.isRunning();
		CompetitionSimulator.stopSimulation();
		if (simulationThread != null) {
			simulationThread.interrupt();
			simulationThread = null;
		}
		return wasRunning ? "Simulation stop requested." : "Simulation is not running.";
	}

	private boolean authorize(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// use proxyutils because this is a plain servlet, not a Vaadin servlet
		String host = ProxyUtils.getClientIp(request);
		boolean bd = AccessUtils.checkBackdoor(host);
		if (!bd) {
			logger.error("{} not in backdoor list, denied simulation", host);
			response.setStatus(403);
			response.flushBuffer();
			return false;
		} else {
			logger.info("{} authorized simulation", host);
		}
		return true;
	}

	private String getAction(HttpServletRequest request) {
		String action = request.getParameter("action");
		if (action != null && !action.isBlank()) {
			return action;
		}
		String pathInfo = request.getPathInfo();
		if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
			return null;
		}
		return pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
	}

	private void writePage(HttpServletResponse response, String message) throws IOException {
		response.reset();
		response.setStatus(200);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/html;charset=UTF-8");

		OutputStream output = response.getOutputStream();
		PrintWriter pw = new PrintWriter(output, true, StandardCharsets.UTF_8);
		pw.println("<!doctype html>");
		pw.println("<html><head><meta charset='utf-8'><title>Simulation</title></head><body>");
		pw.println("<h2>Simulation</h2>");
		pw.println("<p>Status: " + (CompetitionSimulator.isRunning() ? "running" : "stopped") + "</p>");
		if (message != null && !message.isBlank()) {
			pw.println("<p>" + message + "</p>");
		}
		pw.println("<form method='post' action=''>");
		pw.println("<button type='submit' name='action' value='start'>Start</button>");
		pw.println("<button type='submit' name='action' value='stop'>Stop</button>");
		pw.println("</form>");
		pw.println("</body></html>");
		pw.flush();
		output.flush();
		output.close();
	}

}
