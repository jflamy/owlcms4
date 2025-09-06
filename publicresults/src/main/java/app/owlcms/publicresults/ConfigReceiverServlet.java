/*******************************************************************************
 * Copyright © 2009-present Jean-François Lamy
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collection;

import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.Part;
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
@MultipartConfig
public class ConfigReceiverServlet extends HttpServlet {

    Logger logger = (Logger) LoggerFactory.getLogger(ConfigReceiverServlet.class);

    private String secret = StartupUtils.getStringParam("updateKey");

    public void handleUploads(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        boolean authenticated = false;

        // Read all parts. First pass: check form fields for updateKey
        Collection<Part> parts = req.getParts();
        for (Part part : parts) {
            String fieldName = part.getName();
            String fileName = part.getSubmittedFileName();
            if (fileName == null) { // form field
                try (InputStream in = part.getInputStream()) {
                    String value = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    authenticated = checkUpdateKey(req, resp, authenticated, fieldName, value);
                }
            }
        }

        if (!authenticated) {
            // No valid updateKey found
            deny(req, resp, null);
            return;
        }

        // Second pass: process file parts
        for (Part part : parts) {
            String fileName = part.getSubmittedFileName();
            if (fileName != null) {
                String contentType = part.getContentType();
                this.logger.info("receiving {} {}", fileName, contentType);
                if (contentType != null && contentType.contains("zip")) {
                    ResourceWalker.unzipBlobToTemp(part.getInputStream());
                } else {
                    copyFile(part);
                }
            }
        }
    }

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
        } catch (Exception e) {
            this.logger.error(LoggerUtils.stackTrace(e));
        }
    }

    /*
     * 24.1
     * public void handleUploads(HttpServletRequest req, HttpServletResponse resp)
     * throws FileUploadException, IOException {
     * 
     * // Create a factory for disk-based file items
     * DiskFileItemFactory factory = new DiskFileItemFactory();
     * factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
     * factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
     * factory.setFileCleaningTracker(null);
     * 
     * // Configure a repository (to ensure a secure temp location is used)
     * JakSrvltFileUpload upload = new JakSrvltFileUpload(factory);
     * boolean authenticated = false;
     * // Parse the request
     * List<FileItem> items = upload.parseRequest(req);
     * // Process the uploaded items
     * Iterator<FileItem> iter = items.iterator();
     * while (iter.hasNext()) {
     * FileItem item = iter.next();
     * String fieldName = item.getFieldName();
     * if (item.isFormField()) {
     * String string = item.getString();
     * // updateKey should come first
     * authenticated = checkUpdateKey(req, resp, authenticated, fieldName, string);
     * } else {
     * if (!authenticated) {
     * deny(req, resp, null);
     * return;
     * }
     * logger.info("receiving {} {}", item, item.getContentType());
     * if (!item.getContentType().contains("zip")) {
     * copyFile(item);
     * } else {
     * ResourceWalker.unzipBlobToTemp(item.getInputStream());
     * }
     * }
     * }
     * if (!authenticated) {
     * deny(req, resp, null);
     * }
     * return;
     * }
     */

    private boolean checkUpdateKey(HttpServletRequest req, HttpServletResponse resp, boolean authenticated,
            String fieldName, String string) throws IOException {
        if ("updateKey".contentEquals(fieldName)) {
            if (string != null && string.equals(this.secret)) {
                authenticated = true;
            } else {
                deny(req, resp, string);
            }
        }
        return authenticated;
    }

    private void copyFile(Part part) throws IOException {
        Path localDirPath = ResourceWalker.getLocalDirPath();
        if (localDirPath == null) {
            localDirPath = ResourceWalker.createLocalDir();
        }
        String submitted = part.getSubmittedFileName();
        String simpleName = submitted == null ? "" : Paths.get(submitted).getFileName().toString();
        Path name = localDirPath.resolve("styles/" + simpleName);
        Files.createDirectories(name.getParent());
        try (InputStream uploadedStream = part.getInputStream(); OutputStream out = Files.newOutputStream(name)) {
            this.logger.debug("copying to {}", name.toAbsolutePath());
            uploadedStream.transferTo(out);
        }
    }

    private void deny(HttpServletRequest req, HttpServletResponse resp, String string) throws IOException {
        this.logger.error("denying access from {} expected {} got {} ", req.getRemoteHost(), this.secret, string);
        resp.sendError(401, "Denied, wrong credentials");
    }

}