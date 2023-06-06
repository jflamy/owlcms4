/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
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

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.LoggerFactory;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.simulation.CompetitionSimulator;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;

/**
 * A file servlet supporting resume of downloads and client-side caching and
 * GZIP of text content. This servlet can also be used for images, client-side
 * caching would become more efficient. This servlet can also be used for text
 * files, GZIP would decrease network bandwidth.
 *
 * @author BalusC
 * @link http://balusc.blogspot.com/2009/02/fileservlet-supporting-resume-and.html
 */
@SuppressWarnings("serial")
/**
 * Modified to fetch files under the ./local directory relative to the startup
 * directory and, failing that, as a resource on the classpath.
 *
 * @author Jean-François Lamy
 *
 */
@WebServlet("/simulation/*")
public class SimulationServlet extends HttpServlet {

	// Helpers (can be refactored to public utility class)
	// ----------------------------------------

	private static Logger logger = (Logger) LoggerFactory.getLogger(SimulationServlet.class);
//    { logger.setLevel(Level.DEBUG); }

	// Inner classes
	// ------------------------------------------------------------------------------

	/**
	 * Process the actual request.
	 *
	 * @param request  The request to be processed.
	 * @param response The response to be created.
	 * @param content  Whether the request body should be written (GET) or not
	 *                 (HEAD).
	 * @throws IOException If something fails at I/O level.
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response, boolean content)
	        throws IOException {
		logger.info("processing simulation request");
		// use proxyutils because this is a plain servlet, not a Vaadin servlet
		String host = ProxyUtils.getClientIp(request);
		boolean bd = AccessUtils.checkBackdoor(host);
		if (!bd) {
			logger.error("{} not in backdoor list, denied simulation", host);
			response.setStatus(403);
			response.flushBuffer();
			return;
		} else {
			logger.info("{} authorized simulation", host);
		}

		// Get requested file by path info.
		String requestedFileName = request.getPathInfo();
		logger.debug("requested file = {}", requestedFileName);

		// Prepare and initialize response
		// --------------------------------------------------------

		// Initialize response.
		response.reset();

		// Prepare streams.
		OutputStream output = null;

		try {
			// Open streams.
			output = response.getOutputStream();
			PrintWriter pw = new PrintWriter(output, true, StandardCharsets.UTF_8);
			pw.write("Starting simulation");
			pw.flush();
			output.flush();
			response.setStatus(200);
			response.flushBuffer();
			new CompetitionSimulator().runSimulation();
		} catch (Throwable t) {
			logger.error("{}", LoggerUtils.stackTrace(t));
			response.setStatus(500);
		} finally {
			if (output != null) {
				output.close();
			}
		}
	}

	/**
	 * Process GET request.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		// Process request with content.
		processRequest(request, response, true);
	}

	/**
	 * Process HEAD request. This returns the same headers as GET request, but
	 * without content.
	 *
	 * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		// Process request without content.
		processRequest(request, response, false);
	}

}
