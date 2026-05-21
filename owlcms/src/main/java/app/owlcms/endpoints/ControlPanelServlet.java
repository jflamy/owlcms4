package app.owlcms.endpoints;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.LoggerFactory;

import app.owlcms.Main;
import app.owlcms.apputils.AccessUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class ControlPanelServlet extends HttpServlet {

	private static final Logger logger = (Logger) LoggerFactory.getLogger(ControlPanelServlet.class);
	private static final long EXIT_DELAY_MS = 250L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		String socketHost = request.getRemoteAddr();
		String host = ProxyUtils.getClientIp(request);
		if (!AccessUtils.isLocalhost(socketHost) && !AccessUtils.checkBackdoor(host)) {
			logger.warn("{} denied control panel stop request", host);
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			response.setContentType("text/plain;charset=UTF-8");
			response.getWriter().println("OWLCMS stop requires localhost or backdoor whitelist access");
			response.flushBuffer();
			return;
		}

		logger.info("{} requested control panel stop", host);
		response.setStatus(HttpServletResponse.SC_ACCEPTED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/plain;charset=UTF-8");
		response.getWriter().println("OWLCMS stop requested");
		response.flushBuffer();

		Thread stopThread = new Thread(() -> {
			try {
				Thread.sleep(EXIT_DELAY_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			Main.prepareForExit();
			System.exit(0);
		}, "controlpanel-stop");
		stopThread.setDaemon(false);
		stopThread.start();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		response.flushBuffer();
	}
}