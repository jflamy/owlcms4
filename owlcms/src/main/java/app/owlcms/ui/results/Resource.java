package app.owlcms.ui.results;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

public class Resource {
    
    String fileName;
    Path filePath;
    
    public Resource(String fileName, Path filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public InputStream getStream() throws IOException {
        return Files.newInputStream(filePath);
    }

    public byte[] getByteArray() throws IOException {
        return IOUtils.toByteArray(getStream());
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public Path getFilePath( ) {
        return filePath;
    }
    
    @Override
    public String toString() {
        return getFileName();
    }
}
