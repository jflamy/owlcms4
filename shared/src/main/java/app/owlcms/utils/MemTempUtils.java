package app.owlcms.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MemTempUtils {

//    private static FileSystem memFs = Jimfs.newFileSystem(Configuration.unix());
//
//    private static Path tempDir =  memFs.getRootDirectories().iterator().next();

    public static Path createTempDirectory() throws IOException {
        //return Files.createTempDirectory(tempDir, Long.toString(System.currentTimeMillis()));
        return Files.createTempDirectory(Long.toString(System.currentTimeMillis()));
    }

    public static Path createTempDirectory(String name) throws IOException {
        //return Files.createTempDirectory(tempDir, name);
        return Files.createTempDirectory(name);
    }


    public static Path createTempFile(String prefix, String suffix) {
        try {
            //return Files.createTempFile(tempDir, prefix, suffix);
            return Files.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Path createTempFile(Path parentDir, String prefix, String suffix) {
        try {
            //Path memDir = tempDir.resolve(parentDir);
            //return Files.createTempFile(memDir, prefix, suffix);
            return Files.createTempFile(parentDir, prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
