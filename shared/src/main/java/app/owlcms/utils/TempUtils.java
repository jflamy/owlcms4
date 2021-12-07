package app.owlcms.utils;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.jimfs.Jimfs;

public class TempUtils {

    private static FileSystem memFs = Jimfs.newFileSystem();

    private static Path tempDir =  memFs.getRootDirectories().iterator().next();

    public static Path createTempDirectory() throws IOException {
        return Files.createTempDirectory(tempDir, Long.toString(System.currentTimeMillis()));
    }

    public static Path createTempDirectory(String name) throws IOException {
        Path temp = memFs.getPath(name);
        return Files.createTempDirectory(temp, name);
    }

    /**
     * @return the tempDir
     */
    public static Path getTempDir() {
        return tempDir;
    }

    public static Path createTempFile(String prefix, String suffix) {
        try {
            return Files.createTempFile(tempDir, prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
