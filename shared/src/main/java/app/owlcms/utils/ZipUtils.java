/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.utils.IOUtils;
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

	@SuppressWarnings("deprecation")
	public static void extractZip(InputStream inputStream, Path target) throws IOException {
		try {
			ArchiveStreamFactory archiveStreamFactory = new ArchiveStreamFactory();
			ArchiveInputStream<ArchiveEntry> archiveInputStream = archiveStreamFactory
			        .createArchiveInputStream(ArchiveStreamFactory.ZIP, inputStream);
			ArchiveEntry archiveEntry = null;
			while ((archiveEntry = archiveInputStream.getNextEntry()) != null) {
				String name = archiveEntry.getName();
				// logger.debug("reading {}", name);
				// ignore directory entries, only process files.
				if (!name.endsWith("/")) {
					final String prefix = "local/";
					if (name.startsWith(prefix)) {
						name = name.substring(prefix.length());
					}
					Path outputfilePath = target.resolve(name);
					Files.createDirectories(outputfilePath.getParent());
					Files.createFile(outputfilePath);
					logger.trace("writing {}", outputfilePath);
					// write file
					try (OutputStream targetStream = Files.newOutputStream(outputfilePath)) {
						IOUtils.copy(archiveInputStream, targetStream);
					}
					logger.trace("written {}", outputfilePath);
				}
			}
		} catch (EOFException e) {
			// ignore
		}catch (ArchiveException e) {
			throw new IOException(e);
		}
	}

//    private static void copy(final InputStream source, final OutputStream target) throws IOException {
//        final int bufferSize = 4 * 1024;
//        final byte[] buffer = new byte[bufferSize];
//
//        int nextCount;
//        while ((nextCount = source.read(buffer)) >= 0) {
//            target.write(buffer, 0, nextCount);
//        }
//    }

	public static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
		if (fileToZip.isHidden()) {
			return;
		}
		if (fileToZip.isDirectory()) {
			zipOut.putNextEntry(new ZipEntry(fileName + (fileName.endsWith("/") ? "" : "/")));
			zipOut.closeEntry();
			File[] children = fileToZip.listFiles();
			for (File childFile : children) {
				zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
			}
			return;
		}
		FileInputStream fis = new FileInputStream(fileToZip);
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = fis.read(bytes)) >= 0) {
			zipOut.write(bytes, 0, length);
		}
		fis.close();
	}
	
	public static void zipStream(InputStream streamToZip, String fileName, boolean createDir, ZipOutputStream zipOut) throws IOException {
		if (createDir) {
			// pretend that a new directory is entered
			String dirName = new File(fileName).getParent();
			if (dirName != null) {
				zipOut.putNextEntry(new ZipEntry(dirName + (dirName.endsWith("/") ? "" : "/")));
			} else {
				zipOut.putNextEntry(new ZipEntry("/"));
			}
			zipOut.closeEntry();
		}
		ZipEntry zipEntry = new ZipEntry(fileName);
		zipOut.putNextEntry(zipEntry);
		byte[] bytes = new byte[1024];
		int length;
		while ((length = streamToZip.read(bytes)) >= 0) {
			//logger.debug("{} writing {} bytes", fileName, length);
			zipOut.write(bytes, 0, length);
		}
		zipOut.closeEntry();
		zipOut.flush();
		streamToZip.close();
	}
	
    public static void deleteDirectoryRecursively(Path dir) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }


}