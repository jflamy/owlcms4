/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.ui.results;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;

public class Resource implements Comparable<Resource> {

    String fileName;
    Path filePath;

    public Resource(String fileName, Path filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public byte[] getByteArray() throws IOException {
        return IOUtils.toByteArray(getStream());
    }

    public String getFileName() {
        return fileName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public InputStream getStream() throws IOException {
        return Files.newInputStream(filePath);
    }

    @Override
    public String toString() {
        return getFileName();
    }

    @Override
    public int compareTo(Resource o) {
        return ObjectUtils.compare(toString(),o.toString());
    }
}
