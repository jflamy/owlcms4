/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import app.owlcms.data.config.Config;
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

    private static boolean initializedLocalDir = false;

    private static Path localDirPath = null;

    /**
     * Fetch a named file content. First looking in a local override directory structure, and if not found, as a
     * resource on the classpath.
     *
     * @param name
     * @return an input stream with the requested content, null if not found.
     */
    public static InputStream getFileOrResource(String name) {
        InputStream is = null;
        String relativeName;
        if (name.startsWith("/")) {
            relativeName = name.substring(1);
        } else {
            relativeName = name;
        }
        Path localDirPath2 = getLocalDirPath();
        Path target = null;
        if (localDirPath2 != null) {
            target = localDirPath2.resolve(relativeName);
        }
        logger.trace("checking override {} {}", localDirPath2, target);
        if (target != null && Files.exists(target)) {
            try {
                File file = target.toFile();
                logger.debug("found overridden resource {} at {} {}", name, file.getAbsolutePath(),
                        LoggerUtils.whereFrom(1));
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                // can't happen, we tested that Files.exists()...
                throw new RuntimeException("can't happen", e);
            }
        } else {
            is = ResourceWalker.class.getResourceAsStream(name);
            if (is != null) {
                logger.debug("found classpath resource {} {}", name, LoggerUtils.whereFrom(1));
            }
        }
        return is;
    }

    public static Path getLocalDirPath() {
        if (!initializedLocalDir) {
            initLocalDir();
        }
        return localDirPath;
    }

    public static InputStream getLocalizedResourceAsStream(String resourceName) {
        int extensionPos = resourceName.lastIndexOf('.');
        String extension = resourceName.substring(extensionPos);
        String baseName = resourceName.substring(0, extensionPos);

        Locale locale = OwlcmsSession.getLocale();

        String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
        InputStream result = getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
        result = getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_" + locale.getLanguage();
        result = getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "_en";
        result = getResourceAsStream(baseName + suffix + extension);
        if (result != null) {
            return result;
        }

        suffix = "";
        result = getResourceAsStream(baseName + suffix + extension);
        return result;
    }

    public static String getLocalizedResourceName(String rawName) throws FileNotFoundException {
        int extensionPos = rawName.lastIndexOf('.');
        String extension = rawName.substring(extensionPos);
        String baseName = rawName.substring(0, extensionPos);

        Locale locale = OwlcmsSession.getLocale();

        String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
        String name = baseName + suffix + extension;
        InputStream result = getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
        name = baseName + suffix + extension;
        result = getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_" + locale.getLanguage();
        name = baseName + suffix + extension;
        result = getResourceAsStream(name);
        if (result != null) {
            return name;
        }

        suffix = "_en";
        name = baseName + suffix + extension;
        result = getResourceAsStream(name);
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

    public static InputStream getResourceAsStream(String name) {
        return getFileOrResource(name);
    }

    public static void initLocalDir() {
        logger.trace("initializeLocalDir from {}", LoggerUtils.whereFrom());
        setInitializedLocalDir(true);
        byte[] localContent2 = Config.getCurrent().getLocalOverride();
        if (localContent2 != null && localContent2.length > 0) {
            logger.trace("override zip blob found");
            try {
                unzipBlobToTemp(localContent2);
            } catch (Exception e) {
                checkForLocalOverrideDirectory();
            }
        } else {
            checkForLocalOverrideDirectory();
        }
    }

    public static boolean isInitializedLocalDir() {
        return initializedLocalDir;
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
            logger.debug("resources for URI {} found in {}", resourcesURI,
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

    public static void setLocalDirPath(Path curDir) {
        localDirPath = curDir;
    }

    public static void checkForLocalOverrideDirectory() {
        Path curDir = Paths.get(".", "local");
        curDir = curDir.normalize();
        if (Files.exists(curDir)) {
            logger.info("local override directory = {}", curDir.toAbsolutePath());
            ResourceWalker.setLocalDirPath(curDir);
        } else {
            logger.info("no override directory {}", curDir.toAbsolutePath());
            ResourceWalker.setLocalDirPath(null);
        }
    }

    private static void setInitializedLocalDir(boolean checkedLocalDir) {
        initializedLocalDir = checkedLocalDir;
        logger.trace("initializedLocalDir = {}", checkedLocalDir);
    }

    public static void unzipBlobToTemp(byte[] localContent2) throws Exception {
        Path f = null;
        try {
            f = Files.createTempDirectory("owlcms");
            logger.trace("created temp directory " + f);
        } catch (IOException e) {
            throw new Exception("cannot create directory ", e);
        }
        try {
            ZipUtils.unzip(new ByteArrayInputStream(localContent2), f.toFile());
            setLocalDirPath(f);
            logger.info("new local override path {}", getLocalDirPath().normalize());
        } catch (IOException e) {
            throw new Exception("cannot unzip", e);
        }
    }

    /**
     * Walk a local file system resource tree and return the entries. Used for overriding the classpath resources. When
     * developing or running on a laptop the override directory will typically be ./local When running on the cloud, the
     * override files are stored in a blob in the database and extracted to a temporary directory.
     *
     * @param root          a starting point (if start with / will removed and made relative)
     * @param nameGenerator a function that takes the current path and the starting path and returns a (unique) display
     *                      name.
     * @return a list of <display name, file path> entries
     * @throws IOException
     * @throws URISyntaxException
     */
    // Path rootPath = null;
    public List<Resource> getLocalOverrideResourceList(String root,
            BiFunction<Path, Path, String> nameGenerator,
            String startsWith, Locale locale) {
        if (root.startsWith("/")) {
            root = root.substring(1);
        }
        Path basePath = ResourceWalker.getLocalDirPath();
        if (basePath != null) {
            basePath = basePath.normalize().toAbsolutePath();
            basePath = basePath.resolve(root);
            // what is in the path does not necessarily have an override
            if (Files.exists(basePath)) {
                List<Resource> resourceListFromPath = getResourceListFromPath(nameGenerator, startsWith, basePath,
                        locale);
                logger.trace("local override resources {}", resourceListFromPath);
                return resourceListFromPath;
            } else {
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Find all available files that start with a given prefix, either in a local file structure or on the classpath.
     *
     * For each file a display name suitable for a menu is returned. The file retrieval function will use the same logic
     * (look in the local files, then on the classpath).
     *
     * @param absoluteRoot  a starting point (absolute resource name starts with a /)
     * @param nameGenerator a function that takes the current file path and the starting path and returns a (unique)
     *                      display name.
     * @param locale2 
     * @return a list of <display name, file path> entries
     * @throws IOException
     * @throws URISyntaxException
     */
    // Path rootPath = null;
    public List<Resource> getResourceList(String absoluteRoot, BiFunction<Path, Path, String> nameGenerator,
            String startsWith, Locale locale) {
        List<Resource> classPathResources = getResourceListFromPath(nameGenerator, startsWith,
                getResourcesPath(absoluteRoot), locale);
        List<Resource> overrideResources = getLocalOverrideResourceList(absoluteRoot, nameGenerator, startsWith,
                locale);
        TreeSet<Resource> resourceSet = new TreeSet<>(overrideResources);
        resourceSet.addAll(classPathResources);

        return new ArrayList<Resource>(resourceSet);
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
        String regex = "^.*?_([a-zA-Z]{2}(_[a-zA-Z]{2})?(_[a-zA-Z]+)?)$";
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
                return result;
            } else if (!locale.getCountry().isEmpty()) {
                // accept all variants for country
                localeString = locale.getLanguage() + "_" + locale.getCountry();
                result = resourceSuffix.startsWith(localeString);
                if (result) {
                    return true;
                }
                localeString = locale.getLanguage();
                result = resourceSuffix.contentEquals(localeString);
                return result;
            } else if (!locale.getLanguage().isEmpty()) {
                localeString = locale.getLanguage();
                result = resourceSuffix.contentEquals(localeString);
                return result;
            } else {
                return true;
            }
        } catch (IllegalStateException e) {
            resourceSuffix = "";
            logger.trace("{} resourceSuffix({}) empty", resourceName, noExtension);
            return true;
        }
    }

    /**
     * Walk down a file system, gathering resources that match a locale. The file system is either be a real file
     * system, or a ZipFileSystem built from a jar.
     *
     * @param nameGenerator
     * @param startsWith
     * @param rootPath
     * @param locale        if null return files with no locale suffix, else return files that match the locale
     * @return
     */
    private List<Resource> getResourceListFromPath(BiFunction<Path, Path, String> nameGenerator, String startsWith,
            Path rootPath, Locale locale) {
        try {
            List<Resource> localeNames = new ArrayList<>();
            List<Resource> englishNames = new ArrayList<>();
            List<Resource> otherNames = new ArrayList<>();

            Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    String generatedName = nameGenerator.apply(filePath, rootPath);
                    String baseName = filePath.getFileName().toString();
                    logger.trace("visiting {} {}", filePath, locale);
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
            logger.error(LoggerUtils.stackTrace(e));
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
            // this will either return a file or a jar URI, depending on
            // expanded classpath (development) or jar classpath (production)
            resourcesURI = resources.toURI();
        } catch (URISyntaxException e1) {
            logger.error(e1.getReason());
            throw new RuntimeException(e1);
        }
        try {
            rootPath = Paths.get(resourcesURI);
        } catch (FileSystemNotFoundException e) {
            // if we are here, the resource is in the jar, and Vaadin has not already
            // loaded the ZipFileSystem so we do it. Normally Vaadin loads the jar
            // file system first so we never get here.
            openClassPathFileSystem("/agegroups"); // any resource we know is in the jar, but not in any previous jar on classpath
            rootPath = Paths.get(resourcesURI);
        }
        return rootPath;
    }

}
