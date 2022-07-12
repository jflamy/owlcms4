/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.Utf8Appendable.NotUtf8Exception;
import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ProxyUtils;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;

@WebServlet("/config")
public class ConfigReceiverServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(ConfigReceiverServlet.class);

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

        try {
            resp.setCharacterEncoding("UTF-8");
            if (StartupUtils.isTraceSetting()) {
                Set<Entry<String, String[]>> pairs = req.getParameterMap().entrySet();
                logger./**/warn("---- timer update received from {}", ProxyUtils.getClientIp(req));
                for (Entry<String, String[]> pair : pairs) {
                    logger./**/warn("    {} = {}", pair.getKey(), pair.getValue()[0]);
                }
            }

            String updateKey = req.getParameter("updateKey");
            if (updateKey == null || !updateKey.equals(secret)) {
                logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, updateKey);
                resp.sendError(401, "Denied, wrong credentials");
                return;
            }
            
            handleUpload(req);

        } catch (NumberFormatException | IOException | NotUtf8Exception e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
    }

    public String handleUpload(HttpServletRequest request) {
        logger.warn("handle upload");
        System.out.println(System.getProperty("java.io.tmpdir"));
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
        factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        factory.setFileCleaningTracker(null);
        // Configure a repository (to ensure a secure temp location is used)
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            // Parse the request
            List<FileItem> items = upload.parseRequest(request);
            // Process the uploaded items
            Iterator<FileItem> iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = iter.next();
                logger.warn("parse request {}", item.getName());
                if (!item.isFormField()) {
                    try (
                            InputStream uploadedStream = item.getInputStream();
                            OutputStream out = new FileOutputStream(item.getName())) {
                        IOUtils.copy(uploadedStream, out);
                        out.close();
                    }
                }
            }
            // Parse the request with Streaming API
            upload = new ServletFileUpload();
            FileItemIterator iterStream = upload.getItemIterator(request);
            while (iterStream.hasNext()) {
                FileItemStream item = iterStream.next();
                String fieldName = item.getFieldName();
                logger.warn("stream api fieldName = {}  name = {}",fieldName, item.getName());
                InputStream stream = item.openStream();
                if (!item.isFormField()) {
                    // Process the InputStream
                } else {
                    // process form fields
                    String formFieldValue = Streams.asString(stream);
                }
            }
            return "success!";
        } catch (IOException | FileUploadException ex) {
            return "failed: " + ex.getMessage();
        }
    }

}