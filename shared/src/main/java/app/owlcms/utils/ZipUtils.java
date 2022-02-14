/*******************************************************************************
 * Copyright (c) 2009-2022 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

public class ZipUtils {
    
    public static class NoCloseInputStream extends ZipInputStream {

        public NoCloseInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() throws IOException {
        }

        public void doClose() throws IOException {
            super.close();
        }
    }

    final static Logger logger = (Logger) LoggerFactory.getLogger(ZipUtils.class);

    /**
     * @param source zip stream
     * @param target target directory
     * @throws IOException extraction failed
     */
    public static void unzip(InputStream source, File target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            // only extract files
            if (!name.endsWith("/")) {
                String prefix = "local/";
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }
                final File nextFile = new File(target, name);
                logger.debug("unzipping {}", nextFile.getAbsolutePath());

                // create directories
                final File parent = nextFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }

                // write file
                try (OutputStream targetStream = new FileOutputStream(nextFile)) {
                    copy(zipStream, targetStream);
                }
            }
        }
    }

    /**
     * @param source zip stream
     * @param target target directory
     * @throws IOException extraction failed
     */
    public static void unzip(InputStream source, Path target) throws IOException {
        final ZipInputStream zipStream = new ZipInputStream(source);
        ZipEntry nextEntry;
        while ((nextEntry = zipStream.getNextEntry()) != null) {
            String name = nextEntry.getName();
            // only extract files
            if (!name.endsWith("/")) {
                String prefix = "local/";
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                }
                Path outputfilePath = target.resolve(name);
                Files.createDirectories(outputfilePath.getParent());
                Files.createFile(outputfilePath);

                // logger.debug("unzipping {}", outputfilePath);

                // write file
                try (OutputStream targetStream = Files.newOutputStream(outputfilePath)) {
                    copy(zipStream, targetStream);
                }
            }
        }
    }

    public static void copy(final InputStream source, final OutputStream target) throws IOException {
        final int bufferSize = 4 * 1024;
        final byte[] buffer = new byte[bufferSize];

        int nextCount;
        while ((nextCount = source.read(buffer)) >= 0) {
            target.write(buffer, 0, nextCount);
        }
    }
}