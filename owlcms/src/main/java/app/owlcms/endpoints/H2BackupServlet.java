/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.endpoints;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;

import org.hibernate.Session;
import org.slf4j.LoggerFactory;

import app.owlcms.apputils.AccessUtils;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet that provides H2 database backup functionality.
 * Only works when the application is running with an H2 database.
 * 
 * @author Jean-François Lamy
 */
@SuppressWarnings("serial")
@WebServlet("/competition/h2")
public class H2BackupServlet extends HttpServlet {

	private static Logger logger = (Logger) LoggerFactory.getLogger(H2BackupServlet.class);

	/**
	 * Process GET request.
	 *
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		processRequest(request, response);
	}

	/**
	 * Process HEAD request.
	 *
	 * @see HttpServlet#doHead(HttpServletRequest, HttpServletResponse).
	 */
	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
	        throws ServletException, IOException {
		// For HEAD, just check access and database type
		String host = ProxyUtils.getClientIp(request);
		boolean bd = AccessUtils.checkBackdoor(host);
		if (!bd) {
			logger./**/warn("Access denied from {} - not in backdoor list", LoggerUtils.whereFrom());
			response.setStatus(403);
			return;
		}

		if (!JPAService.isLocalDb()) {
			logger./**/warn("H2 backup not available - not using H2 database {}", LoggerUtils.whereFrom());
			response.setStatus(404);
			return;
		}

		response.setStatus(200);
		response.setContentType("application/sql");
	}

	/**
	 * Process the actual request.
	 *
	 * @param request  The request to be processed.
	 * @param response The response to be created.
	 * @throws IOException If something fails at I/O level.
	 */
	private void processRequest(HttpServletRequest request, HttpServletResponse response)
	        throws IOException {
		logger.info("processing H2 backup request");
		
		// Check access control
		String host = ProxyUtils.getClientIp(request);
		boolean bd = AccessUtils.checkBackdoor(host);
		if (!bd) {
			logger./**/warn("{} not in backdoor list, denied H2 backup access {}", host, LoggerUtils.whereFrom());
			response.setStatus(403);
			response.flushBuffer();
			return;
		}
		logger.info("{} authorized H2 backup access", host);

		// Check if we're using H2
		if (!JPAService.isLocalDb()) {
			logger./**/warn("H2 backup not available - application is not using H2 database {}", LoggerUtils.whereFrom());
			response.setStatus(404);
			response.getWriter().write("H2 backup is only available when using H2 database");
			response.flushBuffer();
			return;
		}

		// Create a temporary file for the backup
		Path tempBackupFile = null;
		OutputStream output = null;
		InputStream inputStream = null;

		try {
			// Create temporary file
			tempBackupFile = Files.createTempFile("owlcms-h2-backup-", ".zip");
			logger.info("Creating H2 backup to temporary file: {}", tempBackupFile);

			// Execute H2 BACKUP TO command
			Path finalTempBackupFile = tempBackupFile;
			JPAService.runInTransaction(em -> {
				try {
					// Get the underlying JDBC connection
					Session session = em.unwrap(Session.class);
					session.doWork(connection -> {
						try (Statement stmt = connection.createStatement()) {
							// Use H2 BACKUP TO command - creates a zip file
							String backupPath = finalTempBackupFile.toString().replace("\\", "/");
							String sql = "BACKUP TO '" + backupPath + "'";
							logger.info("Executing: {}", sql);
							stmt.execute(sql);
							logger.info("H2 backup completed successfully");
						} catch (Exception e) {
							logger.error("Error executing H2 backup: {}", LoggerUtils.stackTrace(e));
							throw new RuntimeException("H2 backup failed", e);
						}
					});
					return null;
				} catch (Exception e) {
					logger.error("Error in backup transaction: {}", LoggerUtils.stackTrace(e));
					throw new RuntimeException("H2 backup transaction failed", e);
				}
			});

			// Prepare response
			response.reset();
			response.setContentType("application/zip");
			response.setCharacterEncoding("UTF-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"owlcms-h2-backup.zip\"");

			// Stream the backup file to the response
			output = response.getOutputStream();
			inputStream = new BufferedInputStream(Files.newInputStream(tempBackupFile));
			
			inputStream.transferTo(output);
			output.flush();

			response.setStatus(200);
			response.flushBuffer();

		} catch (Throwable t) {
			logger.error("H2 backup failed: {}", LoggerUtils.stackTrace(t));
			response.setStatus(500);
		} finally {
			// Clean up
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
			// Delete the temporary backup file
			if (tempBackupFile != null) {
				try {
					Files.deleteIfExists(tempBackupFile);
					logger.info("Deleted temporary backup file: {}", tempBackupFile);
				} catch (IOException e) {
					logger./**/warn("Could not delete temporary backup file: {} {}", tempBackupFile, LoggerUtils.whereFrom());
				}
			}
		}
	}
}
