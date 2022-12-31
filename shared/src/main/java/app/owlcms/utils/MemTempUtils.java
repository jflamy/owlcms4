/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

public class MemTempUtils {

    private static FileSystem memFs = Jimfs.newFileSystem(Configuration.unix());
    private static Path tempRoot = memFs.getRootDirectories().iterator().next();

    public static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(tempRoot, Long.toString(System.currentTimeMillis()));
    }

    public static Path createTempDirectory(String name) throws IOException {
        return Files.createTempDirectory(tempRoot, name);
    }

    public static Path createTempFile(Path parentDir, String prefix, String suffix) {
        try {
            Path memDir = tempRoot.resolve(parentDir);
            return Files.createTempFile(memDir, prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(tempRoot, prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
