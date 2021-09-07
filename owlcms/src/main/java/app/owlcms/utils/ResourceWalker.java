/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // FIXME should use config to check override first.
        int extensionPos = rawName.lastIndexOf('.');
        String extension = rawName.substring(extensionPos);
        String baseName = rawName.substring(0, extensionPos);

        Locale locale = OwlcmsSession.getLocale();

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
    public static FileSystem openClassPathFileSystem(String absoluteRootPath) {
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
     * Assumes that the jar has been opened as a file system (see {@link #openClassPathFileSystem(String)}
     *
     * @param absoluteRoot  a starting point (absolute resource name starts with a /)
     * @param nameGenerator a function that takes the current path and the starting path and returns a (unique) display
     *                      name.
     * @return a list of <display name, file path> entries
     * @throws IOException
     * @throws URISyntaxException
     */
    // Path rootPath = null;
    public List<Resource> getResourceList(String absoluteRoot, BiFunction<Path, Path, String> nameGenerator,
            String startsWith) {
        URL resources = getClass().getResource(absoluteRoot);
        if (resources == null) {
            logger.error(absoluteRoot + " not found");
            throw new RuntimeException(absoluteRoot + " not found");
        }
        Path classpathFileSystemPath;
        URI resourcesURI;
        try {
            resourcesURI = resources.toURI();
        } catch (URISyntaxException e1) {
            logger.error(e1.getReason());
            throw new RuntimeException(e1);
        }
        try {
            classpathFileSystemPath = Paths.get(resourcesURI);
        } catch (FileSystemNotFoundException e) {
            // workaround for breaking change in Vaadin 14.6.2
            // the name does not matter, we want something that is found in the jar if there is no real classpath
            openClassPathFileSystem("/templates");
            classpathFileSystemPath = Paths.get(resourcesURI);
        }
        return getResourceListFromPath(nameGenerator, startsWith, classpathFileSystemPath);
    }

    /**
     * Walk down a file system, gathering resources that match a locale.
     * The file system is either be a real file system, or a ZipFileSystem built from a jar.
     * 
     * @param nameGenerator
     * @param startsWith
     * @param rootPath
     * @return
     */
    private List<Resource> getResourceListFromPath(BiFunction<Path, Path, String> nameGenerator, String startsWith,
            Path rootPath) {
        try {
            Locale locale = OwlcmsSession.getLocale();
            List<Resource> localeNames = new ArrayList<>();
            List<Resource> englishNames = new ArrayList<>();
            List<Resource> otherNames = new ArrayList<>();

            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String generatedName = nameGenerator.apply(filePath, rootPath);
                    String baseName = filePath.getFileName().toString();
                    if (startsWith != null) {
                        if (!baseName.startsWith(startsWith)) {
                            logger.trace("ignored {}", filePath);
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    if (matchesLocale(baseName, locale)) {
                        logger.trace("kept {}, baseName={}, locale {}", filePath, baseName, locale);
                        localeNames.add(new Resource(generatedName, filePath));
                    } else if (matchesLocale(baseName, null)) {
                        logger.trace("kept_default {}, baseName={}, locale {}", filePath, baseName, locale);
                        englishNames.add(new Resource(generatedName, filePath));
                    } else {
                        logger.trace("ignored {}, baseName={}, wrong locale {}", filePath, baseName, locale);
                        otherNames.add(new Resource(generatedName, filePath));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            localeNames.addAll(englishNames);
            logger.trace("resources: {}", localeNames);
            // localeNames.addAll(otherNames);

            return localeNames;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getResourcesPath(String absoluteRoot) {
        URL resources = getClass().getResource(absoluteRoot);
        if (resources == null) {
            logger.error(absoluteRoot + " not found");
            throw new RuntimeException(absoluteRoot + " not found");
        }
        Path rootPath;
        URI resourcesURI;
        try {
            resourcesURI = resources.toURI();
        } catch (URISyntaxException e1) {
            logger.error(e1.getReason());
            throw new RuntimeException(e1);
        }
        try {
            rootPath = Paths.get(resourcesURI);
        } catch (FileSystemNotFoundException e) {
            // workaround for breaking change in Vaadin 14.6.2
            openClassPathFileSystem("/templates");
            rootPath = Paths.get(resourcesURI);
        }
        return rootPath;
    }

    /**
     * Check that a resource is relevant to the given language and country
     *
     * The caller is responsible for providing the country.
     *
     * Rules:
     *
     * - Protocol.xls no suffix, always relevant.
     *
     * - Protocol_en.xls always returned if locale has language en
     *
     * - Protocol_en_CA.xls returned only if locale has country = CA
     *
     * As a consequence, for Spanish, if the generic spanish is chosen as locale, and the machine is running in Mexico,
     * the locale will be es_MX and the SV, EC and ES specific templates will be ignored if present
     *
     *
     * @param resourceName
     * @param locale
     * @return
     */
    public boolean matchesLocale(String resourceName, Locale locale) {
        int extensionPos = resourceName.lastIndexOf('.');
        String noExtension = resourceName.substring(0, extensionPos);
        String resourceSuffix = null;

        if (locale == null) {
            return !resourceName.contains("_");
        }

        // name ends with _en or _en_CA or _en_CA_variant
        String regex = "^.*?_([a-zA-Z]{2,3}(_[a-zA-Z]{2,3})?(_[a-zA-Z]+)?)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(noExtension);

        try {
            // boolean test = Pattern.matches(regex, noExtension);
            // logger.trace("pattern match {}",test);
            // test = matcher.matches();
            // logger.trace("matcher match {}",test);
            matcher.matches();
            resourceSuffix = matcher.group(1);
            logger.trace("resourceSuffix({}) : {}", noExtension, resourceSuffix);

            boolean result;
            String localeString;
            if (!locale.getVariant().isEmpty()) {
                // a specific variant is asked for
                localeString = locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
                result = resourceSuffix.contentEquals(localeString);
                if (result) {
                    return true;
                }
                localeString = locale.getLanguage() + "_" + locale.getCountry();
                result = resourceSuffix.contentEquals(localeString);
                if (result) {
                    return true;
                }
                localeString = locale.getLanguage();
                result = resourceSuffix.contentEquals(localeString);
                if (result) {
                    return true;
                }
            } else if (!locale.getCountry().isEmpty()) {
                // accept all variants for country
                localeString = locale.getLanguage() + "_" + locale.getCountry();
                result = resourceSuffix.startsWith(localeString);
                if (result) {
                    return true;
                }
                localeString = locale.getLanguage();
                result = resourceSuffix.contentEquals(localeString);
                if (result) {
                    return true;
                }
            } else if (!locale.getLanguage().isEmpty()) {
                localeString = locale.getLanguage();
                result = resourceSuffix.contentEquals(localeString);
                if (result) {
                    return true;
                }
            } else {
                return true;
            }
        } catch (IllegalStateException e) {
            resourceSuffix = "";
            logger.trace("resourceSuffix({}) empty", noExtension);
        }

        return false;
    }

}
