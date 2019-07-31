/***
 * Copyright (c) 2009-2019 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * Get resource paths recursively from jar or classpath and assign them a short
 * display name for selection by user.
 * 
 * @author owlcms
 *
 */
public class ResourceWalker {

    public static void main(String[] args) {
        try {
            new ResourceWalker().walk("/templates/protocol", (filePath, rootPath) -> {
                String displayName = relativeName(filePath, rootPath);
                InputStream is;
                try {
                    is = Files.newInputStream(filePath);
                    byte[] a = IOUtils.toByteArray(is);
                    System.out.println(displayName + " " + filePath + " " + a.length + "bytes");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return displayName;
            });
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String relativeName(Path filePath, Path rootPath) {
        return filePath.toString().substring(rootPath.toString().length() + 1);
    }

    Logger logger = (Logger) LoggerFactory.getLogger(ResourceWalker.class);

    /**
     * Walk a resource tree and return the entries. The paths can be inside a jar or
     * classpath folder. A function is called on the name in order to generate a
     * display name.
     *
     * @param absoluteRoot a starting point (absolute resource name starts with a /)
     * @param generateName a function that takes the current path and the starting
     *                     path and returns a (unique) display name.
     * @return a list of <display name, file path> entries
     * @throws IOException
     * @throws URISyntaxException
     */
    public Map<String, Path> walk(String absoluteRoot, BiFunction<Path, Path, String> generateName)
            throws IOException, URISyntaxException {
        URL resources = getClass().getResource(absoluteRoot);
        URI uri = resources.toURI();
        Map<String, Path> processedNames = new TreeMap<>();
        try (FileSystem fileSystem = (uri.getScheme().equals("jar")
                ? FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap())
                : null)) {
            Path rootPath = Paths.get(uri);
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String processedName = generateName.apply(filePath, rootPath);
                    processedNames.put(processedName, filePath);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return processedNames;
    }
}
