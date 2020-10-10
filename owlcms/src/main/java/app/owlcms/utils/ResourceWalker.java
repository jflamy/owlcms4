/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.utils;

import java.io.FileNotFoundException;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;

import org.slf4j.LoggerFactory;

import app.owlcms.init.OwlcmsSession;
import app.owlcms.ui.results.Resource;
import ch.qos.logback.classic.Logger;

/**
 * Get resource paths recursively from jar or classpath and assign them a short display name for selection by user.
 *
 * @author owlcms
 *
 */
public class ResourceWalker {

    static Logger logger = (Logger) LoggerFactory.getLogger(ResourceWalker.class);

    public static InputStream getLocalizedResourceAsStream(String resourceName) {
        int extensionPos = resourceName.lastIndexOf('.');
        String extension = resourceName.substring(extensionPos);
        String baseName = resourceName.substring(0, extensionPos);

        Locale locale = OwlcmsSession.getLocale();
        logger.warn("getLocalizedResourceAsStream {}",locale);
        
        String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
        InputStream result = ResourceWalker.class.getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
        result = ResourceWalker.class.getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_" + locale.getLanguage();
        result = ResourceWalker.class.getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_en";
        result = ResourceWalker.class.getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "";
        result = ResourceWalker.class.getResourceAsStream(baseName + suffix + extension);
        return result;
    }

    public static String getLocalizedResourceName(String rawName) throws FileNotFoundException {
        int extensionPos = rawName.lastIndexOf('.');
        String extension = rawName.substring(extensionPos);
        String baseName = rawName.substring(0, extensionPos);

        Locale locale = OwlcmsSession.getLocale();
        logger.warn("getLocalizedResourceAsStream {}",locale);
        
        String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
        String name = baseName + suffix + extension;
        InputStream result = ResourceWalker.class.getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
        name = baseName + suffix + extension;
        result = ResourceWalker.class.getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_" + locale.getLanguage();
        name = baseName + suffix + extension;
        result = ResourceWalker.class.getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_en";
        name = baseName + suffix + extension;
        result = ResourceWalker.class.getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "";
        name = baseName + suffix + extension;
        result = ResourceWalker.class.getResourceAsStream(name);
        if (result != null) {
            return name;
        } else {
            throw new FileNotFoundException(rawName);
        }
    }

    /**
     * open the file system for locating resources.
     *
     * @param absoluteRootPath
     * @return an open file system (intentionnaly not closed)
     */
    public static FileSystem openFileSystem(String absoluteRootPath) {
        URL resources = ResourceWalker.class.getResource(absoluteRootPath);
        try {
            URI resourcesURI = resources.toURI();
            FileSystem fileSystem = (resourcesURI.getScheme().equals("jar")
                    ? FileSystems.newFileSystem(resourcesURI, Collections.<String, Object>emptyMap())
                    : null);
            logger.trace("resources for URI {} found in {}", resourcesURI,
                    (fileSystem != null ? "jar" : "classpath folders"));
            return fileSystem;
        } catch (URISyntaxException | IOException e) {
            logger.error(LoggerUtils.stackTrace(e));
            throw new RuntimeException(e);
        } catch (Throwable t) {
            logger.error(LoggerUtils.stackTrace(t));
            throw t;
        }
    }

    public static String relativeName(Path filePath, Path rootPath) {
        return filePath.toString().substring(rootPath.toString().length() + 1);
    }

    /**
     * Walk a resource tree and return the entries. The paths can be inside a jar or classpath folder. A function is
     * called on the name in order to generate a display name.
     *
     * Assumes that the jar has been opened as a file system (see {@link #openFileSystem(String)}
     *
     * @param absoluteRoot a starting point (absolute resource name starts with a /)
     * @param generateName a function that takes the current path and the starting path and returns a (unique) display
     *                     name.
     * @return a list of <display name, file path> entries
     * @throws IOException
     * @throws URISyntaxException
     */
    public List<Resource> getResourceList(String absoluteRoot, BiFunction<Path, Path, String> generateName,
            String startsWith) {
        try {
            URL resources = getClass().getResource(absoluteRoot);
            URI resourcesURI = resources.toURI();
            List<Resource> localeNames = new ArrayList<>();
            List<Resource> englishNames = new ArrayList<>();
            List<Resource> otherNames = new ArrayList<>();
            Path rootPath = Paths.get(resourcesURI);
            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String generatedName = generateName.apply(filePath, rootPath);
                    if (startsWith != null) {
//                        String baseName = FilenameUtils.getBaseName(filePath.toString());
                        if (!generatedName.startsWith(startsWith)) {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    if (matchesLocale(filePath, OwlcmsSession.getLocale())) {
                        localeNames.add(new Resource(generatedName, filePath));
                    } else if (matchesLocale(filePath, Locale.ENGLISH)) {
                        englishNames.add(new Resource(generatedName, filePath));
                    } else {
                        otherNames.add(new Resource(generatedName, filePath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            localeNames.addAll(englishNames);
            //localeNames.addAll(otherNames);

            return localeNames;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean matchesLocale(Path filePath, Locale locale) {
        logger.warn("matching {} with {}",filePath,locale);
        String resourceName = filePath.toString();
        int extensionPos = resourceName.lastIndexOf('.');
        String extension = resourceName.substring(extensionPos);
        
        if (locale == Locale.ENGLISH && !resourceName.contains("_")) {
            // no suffix is assumed to be ENGLISH
            return true;
        }

        boolean result;
        String suffix;
        if (!locale.getVariant().isEmpty()) {
            suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant() + extension;
            result = resourceName.endsWith(suffix);
            if (result) {
                return true;
            }
        }

        if (!locale.getCountry().isEmpty()) {
            suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + extension;
            result = resourceName.endsWith(suffix);
            if (result) {
                return true;
            }
        }

        suffix = "_" + locale.getLanguage() + extension;
        result = resourceName.endsWith(suffix);
        if (result) {
            return true;
        }

        return false;
    }

}
