/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.publicresults;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.DiskFileItemFactory.Builder;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import app.owlcms.utils.LoggerUtils;
import app.owlcms.utils.ResourceWalker;
import app.owlcms.utils.StartupUtils;
import ch.qos.logback.classic.Logger;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet("/config")
public class ConfigReceiverServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(ConfigReceiverServlet.class);

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
        try {
            handleUploads(req, resp);
        } catch (NumberFormatException | FileUploadException e) {
            logger.error(LoggerUtils.stackTrace(e));
        }
    }

    public void handleUploads(HttpServletRequest req, HttpServletResponse resp)
            throws FileUploadException, IOException {

        // Create a factory for disk-based file items
        Builder builder = DiskFileItemFactory.builder();
        String tmpDir = System.getProperty("java.io.tmpdir");
        DiskFileItemFactory f = builder.setPath(tmpDir).setBufferSize(-1).get();

        // Configure a repository (to ensure a secure temp location is used)
        JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload = new JakartaServletFileUpload<>(f);
        boolean authenticated = false;
        
        // Parse the request
        List<DiskFileItem> items = upload.parseRequest(req);
        // Process the uploaded items
        Iterator<DiskFileItem> iter = items.iterator();
        while (iter.hasNext()) {
            DiskFileItem item = iter.next();
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
                logger.info("receiving {} {}", item, item.getContentType());
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

    /* 24.1
    public void handleUploads(HttpServletRequest req, HttpServletResponse resp)
            throws FileUploadException, IOException {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
        factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        factory.setFileCleaningTracker(null);

        // Configure a repository (to ensure a secure temp location is used)
        JakSrvltFileUpload upload = new JakSrvltFileUpload(factory);
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
                logger.info("receiving {} {}", item, item.getContentType());
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
    */

    private void copyFile(FileItem<?> item) throws IOException {
        Path localDirPath = ResourceWalker.getLocalDirPath();
        if (localDirPath == null) {
            localDirPath = ResourceWalker.createLocalDir();
        }
        Path name = localDirPath.resolve("styles/" + item.getName());
        Files.createDirectories(name.getParent());
        try (InputStream uploadedStream = item.getInputStream();
                OutputStream out = Files.newOutputStream(name)) {
            logger.debug("copying to {}", name.toAbsolutePath());
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