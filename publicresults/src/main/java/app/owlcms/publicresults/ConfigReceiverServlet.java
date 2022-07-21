/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.util.Utf8Appendable.NotUtf8Exception;
import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
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
            handleUploads(req, resp);
        } catch (NumberFormatException | NotUtf8Exception | FileUploadException e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
    }

    public void handleUploads(HttpServletRequest req, HttpServletResponse resp)
            throws FileUploadException, IOException {


        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
        factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        factory.setFileCleaningTracker(null);

        // Configure a repository (to ensure a secure temp location is used)
        ServletFileUpload upload = new ServletFileUpload(factory);
        boolean authenticated = false;
        // Parse the request
        List<FileItem> items = upload.parseRequest(req);
        // Process the uploaded items
        Iterator<FileItem> iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = iter.next();
            String fieldName = item.getFieldName();
            if (item.isFormField()) {
                String string = item.getString();
                // updateKey should come first
                authenticated = checkUpdateKey(req, resp, authenticated, fieldName, string);
            } else {
                if (!authenticated) {
                    deny(req, resp, null);
                    return;
                }
                if (!item.getContentType().contains("zip")) {
                    copyFile(item);
                } else {
                    ResourceWalker.unzipBlobToTemp(item.getInputStream());
                }
            }
        }
        if (!authenticated) {
            deny(req, resp, null);
        }
        return;
    }

    private void copyFile(FileItem item) throws IOException {
        Path localDirPath = ResourceWalker.getLocalDirPath();
        if (localDirPath == null) {
            localDirPath = ResourceWalker.createLocalDir();
        }
        Path name = localDirPath.resolve("styles/"+item.getName());
        Files.createDirectories(name.getParent());
        try (InputStream uploadedStream = item.getInputStream();
                OutputStream out = Files.newOutputStream(name)) {
            logger.warn("copying to abs {}",name.toAbsolutePath());
            IOUtils.copy(uploadedStream, out);
            out.close();
        }
    }

    private boolean checkUpdateKey(HttpServletRequest req, HttpServletResponse resp, boolean authenticated,
            String fieldName, String string) throws IOException {
        if ("updateKey".contentEquals(fieldName)) {
            if (string != null && string.equals(secret)) {
                authenticated = true;
            } else {
                deny(req, resp, string);
            }
        }
        return authenticated;
    }

    private void deny(HttpServletRequest req, HttpServletResponse resp, String string) throws IOException {
        logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), secret, string);
        resp.sendError(401, "Denied, wrong credentials");
    }

}