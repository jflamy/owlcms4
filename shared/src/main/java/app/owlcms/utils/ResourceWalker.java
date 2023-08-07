/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.utils;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Get resource paths recursively from jar or classpath and assign them a short
 * display name for selection by user.
 *
 * @author owlcms
 *
 */
public class ResourceWalker {

	static Logger logger = (Logger) LoggerFactory.getLogger(ResourceWalker.class);

	private static boolean initializedLocalDir = false;

	private static Path localDirPath = null;

	private static Supplier<byte[]> localZipBlobSupplier;

	private static Supplier<Locale> localeSupplier;

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

	public static synchronized Path createLocalDir() {
		Path f = null;
		try {
			f = MemTempUtils.createTempDirectory("owlcmsOverride");
			logger.trace("created temp directory " + f);
			setLocalDirPath(f);
			setInitializedLocalDir(true);
			logger.info("new in-memory override path {}", getLocalDirPath().normalize());
			return f;
		} catch (IOException e) {
			throw new RuntimeException("cannot create directory ", e);
		}
	}

	public static synchronized Path createLocalRealDir() {
		Path f = null;
		try {
			f = Files.createTempDirectory("config");
			logger.trace("created temp directory " + f);
			setLocalDirPath(f);
			setInitializedLocalDir(true);
			logger.info("new temporary directory {}", getLocalDirPath().normalize());
			return f;
		} catch (IOException e) {
			throw new RuntimeException("cannot create directory ", e);
		}
	}

	/**
	 * Fetch a named file content. First looking in a local override directory
	 * structure, and if not found, as a resource on the classpath.
	 *
	 * @param name
	 * @return an input stream with the requested content, null if not found.
	 * @throws FileNotFoundException
	 */
	public static InputStream getFileOrResource(String name) throws FileNotFoundException {
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
		if (target != null && Files.exists(target)) {
			try {
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("found overridden resource {} at {} {}", name, target.toAbsolutePath(),
					        LoggerUtils.whereFrom(1));
				}
				return Files.newInputStream(target);
			} catch (IOException e) {
				if (name.trim().contentEquals("/") || name.isBlank()) {
					// exists but is top level
					return null;
				} else {
					throw new RuntimeException("can't happen '" + name + "'", e);
				}
			}
		} else {
			is = ResourceWalker.class.getResourceAsStream(name.startsWith("/") ? name : "/" + name);
			if (is != null) {
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("found classpath resource {} {}", name, LoggerUtils.whereFrom(1));
				}
			} else {
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("not found {} {}", name, LoggerUtils.whereFrom(0));
				}
				throw new FileNotFoundException(name);
			}
		}
		return is;
	}

	/**
	 * Fetch a named file content. First looking in a local override directory
	 * structure, and if not found, as a resource on the classpath.
	 *
	 * @param name
	 * @return an input stream with the requested content, null if not found.
	 * @throws FileNotFoundException
	 */
	public static Path getFileOrResourcePath(String name) throws FileNotFoundException {
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
		if (target != null && Files.exists(target)) {
			if (logger.isEnabledFor(Level.DEBUG)) {
				logger.debug("found overridden resource {} at {} {}", name, target.toAbsolutePath(),
				        LoggerUtils.whereFrom(1));
			}
			return target;
		} else {
			String resName = "/" + relativeName;
			target = getResourcePath(resName);
			if (target != null) {
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("found classpath resource {} {}", name, LoggerUtils.whereFrom(1));
				}
			} else {
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("not found {} {}", target, resName);
				}
				throw new FileNotFoundException(name);
			}

		}
		return target;
	}

	public static Path getLocalDirPath() {
		if (!initializedLocalDir) {
			initLocalDir();
		}
		return localDirPath;
	}

	/**
	 * @return the localeSupplier
	 */
	public static Supplier<Locale> getLocaleSupplier() {
		return localeSupplier;
	}

	public static InputStream getLocalizedResourceAsStream(String resourceName) {
		int extensionPos = resourceName.lastIndexOf('.');
		String extension = resourceName.substring(extensionPos);
		String baseName = resourceName.substring(0, extensionPos);

		Locale locale = getLocaleSupplier().get();

		String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
		InputStream result;
		try {
			result = getResourceAsStream(baseName + suffix + extension);
			return result;
		} catch (FileNotFoundException e1) {
			// ignore
		}

		suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
		try {
			result = getResourceAsStream(baseName + suffix + extension);
			return result;
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "_" + locale.getLanguage();
		try {
			result = getResourceAsStream(baseName + suffix + extension);
			return result;
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "_en";
		try {
			result = getResourceAsStream(baseName + suffix + extension);
			return result;
		} catch (FileNotFoundException e) {
			// ignore
		}

		try {
			suffix = "";
			result = getResourceAsStream(baseName + suffix + extension);
			return result;
		} catch (FileNotFoundException e) {
			return null;
		}

	}

	public static String getLocalizedResourceName(String rawName) throws FileNotFoundException {
		int extensionPos = rawName.lastIndexOf('.');
		String extension = rawName.substring(extensionPos);
		String baseName = rawName.substring(0, extensionPos);

		Locale locale = getLocaleSupplier().get();

		String suffix = "_" + locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
		String name = baseName + suffix + extension;
		InputStream result;
		try {
			result = getResourceAsStream(name);
			if (result != null) {
				return name;
			}
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "_" + locale.getLanguage() + "_" + locale.getCountry();
		name = baseName + suffix + extension;
		try {
			result = getResourceAsStream(name);
			if (result != null) {
				return name;
			}
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "_" + locale.getLanguage();
		name = baseName + suffix + extension;
		try {
			result = getResourceAsStream(name);
			if (result != null) {
				return name;
			}
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "_en";
		name = baseName + suffix + extension;
		try {
			result = getResourceAsStream(name);
			if (result != null) {
				return name;
			}
		} catch (FileNotFoundException e) {
			// ignore
		}

		suffix = "";
		name = baseName + suffix + extension;
		result = getResourceAsStream(name);
		if (result != null) {
			return name;
		}
		return null;
	}

	public static Supplier<byte[]> getLocalZipBlobSupplier() {
		return localZipBlobSupplier;
	}

	public static InputStream getResourceAsStream(String name) throws FileNotFoundException {
		return getFileOrResource(name);
	}

	public static Path getResourcePath(String resourcePathString) {
		URL resourceURL = ResourceWalker.class.getResource(resourcePathString);
		if (resourceURL == null) {
			// logger.error(resourcePathString + " not found");
			// throw new RuntimeException(resourcePathString + " not found");
			return null;
		}
		Path resourcePath;
		URI resourcesURI;
		try {
			// this will either return a file or a jar URI, depending on
			// expanded classpath (development) or jar classpath (production)
			resourcesURI = resourceURL.toURI();
			logger.warn(resourcesURI.toString());
		} catch (URISyntaxException e1) {
			logger.error(e1.getReason());
			throw new RuntimeException(e1);
		}
		try {
			resourcePath = Paths.get(resourcesURI);
		} catch (FileSystemNotFoundException e) {
			// the normal classpath uses the default file system, which is always found.
			// if the file was in a jar, normally Vaadin has already loaded the zip file
			// system
			// so we should not get a not found either.

			// so the only way to get here is if the file is in a jar, and somehow Vaadin
			// has
			// not opened it yet. So we use a file that should be in the jar, and expect the
			// URI to be of the "jar" type.

			// beware: use a resource that is in the shared module
			openClassPathFileSystem("/i18n");
			resourcePath = Paths.get(resourcesURI);
			logger.debug("resourcePath: {} {}", resourcesURI, resourcePath);
		}
		return resourcePath;
	}

	public static synchronized void initLocalDir() {
		setInitializedLocalDir(true);
		byte[] blob = localZipBlobSupplier != null ? localZipBlobSupplier.get() : null;
		if (blob != null && blob.length > 0) {
			if (logger.isEnabledFor(Level.DEBUG)) {
				logger.debug("override zip blob found");
			}
			try {
				unzipBlobToTemp(blob);
			} catch (Exception e) {
				checkForLocalOverrideDirectory();
			}
		} else {
			logger.debug("checking for override.");
			checkForLocalOverrideDirectory();
		}
	}

	public static boolean isInitializedLocalDir() {
		return initializedLocalDir;
	}

	public static String relativeName(Path filePath, Path rootPath) {
		String substring;
		if (filePath.equals(rootPath)) {
			substring = filePath.getFileName().toString();
		} else {
			substring = filePath.toString().substring(rootPath.toString().length() + 1);
		}
		return substring;
	}

	public static String straightName(Path filePath, Path rootPath) {
		return filePath.toString();
	}

	public static void setLocalDirPath(Path curDir) {
		localDirPath = curDir;
	}

	/**
	 * @param localeSupplier the localeSupplier to set
	 */
	public static void setLocaleSupplier(Supplier<Locale> localeSupplier) {
		ResourceWalker.localeSupplier = localeSupplier;
	}

	public static void setLocalZipBlobSupplier(Supplier<byte[]> localOverrideSupplier) {
		ResourceWalker.localZipBlobSupplier = localOverrideSupplier;
	}

	public static void unzipBlobToTemp(byte[] localContent2) throws Exception {
		Path f = null;
		try {
			f = MemTempUtils.createTempDirectory("owlcmsOverride");
			if (logger.isEnabledFor(Level.DEBUG)) {
				logger.debug("created temp directory " + f);
			}
		} catch (IOException e) {
			throw new Exception("cannot create directory ", e);
		}
		ZipUtils.extractZip(new ByteArrayInputStream(localContent2), f);
		setLocalDirPath(f);
		setInitializedLocalDir(true);
		logger.info("new in-memory override path {}", getLocalDirPath().normalize());
	}

	public static void unzipBlobToTemp(InputStream in) throws IOException {
		Path f = null;
		f = MemTempUtils.createTempDirectory("owlcmsOverride");
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.debug("created temp directory " + f);
		}
		ZipUtils.extractZip(in, f);
		setLocalDirPath(f);
		setInitializedLocalDir(true);
		logger.info("new in-memory override path {}", getLocalDirPath().normalize());
	}

	/**
	 * Register an additional file system for the resources
	 *
	 * We use the classloader to return the URI where it found a resource. This will
	 * be either a jar (in production) or a regular file system (in development). If
	 * a jar, then we register a file system for the Jar's URI.
	 *
	 * @param absoluteRootPath
	 * @return an open file system (intentionnaly not closed)
	 */
	private static FileSystem openClassPathFileSystem(String absoluteRootPath) {
		URL resources = ResourceWalker.class.getResource(absoluteRootPath);
		try {
			URI resourcesURI = resources.toURI();
			FileSystem fileSystem = (resourcesURI.getScheme().equals("jar")
			        ? FileSystems.newFileSystem(resourcesURI, Collections.<String, Object>emptyMap())
			        : null);
			if (logger.isEnabledFor(Level.DEBUG)) {
				logger.debug("resources for URI {} found in {}", resourcesURI,
				        (fileSystem != null ? "jar" : "classpath folders"));
			}
			return fileSystem;
		} catch (FileSystemAlreadyExistsException fe) {
			throw fe;
		} catch (URISyntaxException | IOException e) {
			LoggerUtils.logError(logger, e);
			throw new RuntimeException(e);
		} catch (Throwable t) {
			LoggerUtils.logError(logger, t);
			throw t;
		}
	}

	private static void setInitializedLocalDir(boolean checkedLocalDir) {
		initializedLocalDir = checkedLocalDir;
		if (logger.isEnabledFor(Level.DEBUG)) {
			logger.debug("initializedLocalDir = {}", checkedLocalDir);
		}
	}

	/**
	 * Walk a local file system resource tree and return the entries. Used for
	 * overriding the classpath resources. When developing or running on a laptop
	 * the override directory will typically be ./local When running on the cloud,
	 * the override files are stored in a blob in the database and extracted to a
	 * temporary directory.
	 *
	 * @param root          a starting point (if start with / will removed and made
	 *                      relative)
	 * @param nameGenerator a function that takes the current path and the starting
	 *                      path and returns a (unique) display name.
	 * @return a list of <display name, file path> entries
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	// Path rootPath = null;
	public List<Resource> getLocalOverrideResourceList(String root,
	        BiFunction<Path, Path, String> nameGenerator,
	        Predicate<String> predicate, Locale locale) {
		if (root.startsWith("/")) {
			root = root.substring(1);
		}
		Path basePath = ResourceWalker.getLocalDirPath();
		List<Resource> list = doFetchFromDir(root, nameGenerator, predicate, locale, basePath);
		return list;
	}

	private List<Resource> doFetchFromDir(String root, BiFunction<Path, Path, String> nameGenerator,
	        Predicate<String> predicate, Locale locale, Path basePath) {
		if (basePath != null) {
			basePath = basePath.normalize().toAbsolutePath();
			basePath = basePath.resolve(root);
			// what is in the path does not necessarily have an override
			if (Files.exists(basePath)) {
				List<Resource> resourceListFromPath = getResourceListFromPath(nameGenerator, predicate, basePath,
				        locale);
				if (logger.isEnabledFor(Level.DEBUG)) {
					logger.debug("local override resources {}", resourceListFromPath);
				}
				return resourceListFromPath;
			} else {
				return new ArrayList<>();
			}
		} else {
			return new ArrayList<>();
		}
	}

	public List<Resource> getResourceList(String absoluteRoot, BiFunction<Path, Path, String> nameGenerator,
	        Predicate<String> startsWith, Locale locale) {
		return getResourceList(absoluteRoot, nameGenerator, startsWith, locale, false);
	}

	/**
	 * Find all available files that start with a given prefix, either in a local
	 * file structure or on the classpath.
	 *
	 * For each file a display name suitable for a menu is returned. The file
	 * retrieval function will use the same logic (look in the local files, then on
	 * the classpath).
	 *
	 * @param absoluteRoot  a starting point (absolute resource name starts with a
	 *                      /)
	 * @param nameGenerator a function that takes the current file path and the
	 *                      starting path and returns a (unique) display name.
	 * @param overridesOnly if true, do not include classpath resources - use only
	 *                      explicitly provided files
	 * @param locale2
	 * @return a list of <display name, file path> entries
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	// Path rootPath = null;
	public List<Resource> getResourceList(String absoluteRoot, BiFunction<Path, Path, String> nameGenerator,
	        Predicate<String> startsWith, Locale locale, boolean overridesOnly) {

		Map<String, Resource> classPathResourcesMap = overridesOnly ? Map.of()
		        : getResourceListFromPath(nameGenerator, startsWith,
		                getResourcePath(absoluteRoot), locale)
		                .stream()
		                .collect(Collectors.toMap(Resource::normalizedName, Function.identity()));

		Map<String, Resource> overrideResourcesMap = getLocalOverrideResourceList(absoluteRoot, nameGenerator,
		        startsWith, locale)
		        .stream()
		        .collect(Collectors.toMap(Resource::normalizedName, Function.identity()));

		// we want all the resource names from both lists. If a resource with a given
		// name is found in both lists,
		// we want the resource from the override list.

		Set<String> classPathResourceNames = classPathResourcesMap.keySet();
		Set<String> overrideResourceNames = overrideResourcesMap.keySet();
		// logger.debug("classpath resources {}", classPathResourceNames);
		// logger.debug("override resources {}", overrideResourceNames);
		TreeSet<String> allResourceNames = new TreeSet<>();
		allResourceNames.addAll(classPathResourceNames);
		allResourceNames.addAll(overrideResourceNames);

		List<Resource> resourceList = allResourceNames.stream().map(rn -> {
			Resource r = overrideResourcesMap.get(rn);
			return r != null ? r : classPathResourcesMap.get(rn);
		}).collect(Collectors.toList());
		// logger.trace("merged list {}", resourceList);
		return resourceList;
	}

	public Map<String, Resource> getPRResourceMap(Locale locale) {
		Map<String, Resource> resourceMap = new TreeMap<>();
		Predicate<String> startsWith = (s) -> true;

//		resourceMap.putAll(getResourceListFromPath(ResourceWalker::relativeName,
//		        (s) -> {
//			        logger.warn("checking {}", s);
//			        return !s.endsWith(".class");
//		        },
//		        getResourcePath("/"), locale)
//		        .stream()
//		        .collect(Collectors.toMap(Resource::normalizedName, Function.identity())));

		// during maven development, get default i18n and styles in case they are not in local.
		try {
			addToResourceMap(resourceMap, ResourceWalker::relativeName, startsWith, locale,
			        Paths.get("..", "shared", "src", "main", "resources", "i18n"), "i18n");
			addToResourceMap(resourceMap, ResourceWalker::relativeName, startsWith, locale,
			        Paths.get("..", "shared", "src", "main", "resources", "styles"), "styles");
		} catch (Exception e) {
			//ignore in production
		}
		
		//FIXME: resource override directory.
		try {
			addToResourceMap(resourceMap, ResourceWalker::relativeName, startsWith, locale,
			        getLocalDirPath() , null);
		} catch (Exception e) {
			// ignore in cloud mode.
		}
		


		for (Entry<String, Resource> n : resourceMap.entrySet()) {
			System.err.println(n.getKey() + " " + n.getValue().getFilePath().normalize().toAbsolutePath());
		}
		return resourceMap;
	}

	private void addToResourceMap(Map<String, Resource> classPathResourcesMap,
	        BiFunction<Path, Path, String> nameGenerator, Predicate<String> startsWith,
	        Locale locale, Path resourcePath, String prefix) {
		Map<String, Resource> classPathResourcesMap2 = getResourceListFromPath(
		        ResourceWalker::relativeName,
		        startsWith,
		        resourcePath,
		        locale)
		        .stream()
		        .collect(Collectors.toMap(Resource::normalizedName, Function.identity()));
		for (Entry<String, Resource> e : classPathResourcesMap2.entrySet()) {
			classPathResourcesMap.put((prefix != null ? (prefix + "/") : "") + e.getKey(), e.getValue());
		}
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
	 * As a consequence, for Spanish, if the generic spanish is chosen as locale,
	 * and the machine is running in Mexico, the locale will be es_MX and the SV, EC
	 * and ES specific templates will be ignored if present
	 *
	 *
	 * @param resourceName
	 * @param locale
	 * @return
	 */
	public boolean matchesLocale(String resourceName, Locale locale) {
		int extensionPos = resourceName.lastIndexOf('.');
		String noExtension = extensionPos == -1 ? resourceName : resourceName.substring(0, extensionPos);
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
			if (logger.isEnabledFor(Level.TRACE)) {
				logger.trace("resourceSuffix({}) : {}", noExtension, resourceSuffix);
			}

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
			if (logger.isEnabledFor(Level.TRACE)) {
				logger.trace("{} resourceSuffix({}) empty", resourceName, noExtension);
			}
			return true;
		}
	}

	/**
	 * Walk down a file system, gathering resources that match a locale. The file
	 * system is either be a real file system, or a ZipFileSystem built from a jar.
	 *
	 * @param nameGenerator
	 * @param startsWith
	 * @param rootPath
	 * @param locale        if null return files with no locale suffix, else return
	 *                      files that match the locale
	 * @return
	 */
	private List<Resource> getResourceListFromPath(BiFunction<Path, Path, String> nameGenerator,
	        Predicate<String> predicate,
	        Path rootPath, Locale locale) {
		try {
			List<Resource> localeNames = new ArrayList<>();
			List<Resource> englishNames = new ArrayList<>();
			List<Resource> otherNames = new ArrayList<>();

			Files.walkFileTree(rootPath, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,
			        new SimpleFileVisitor<Path>() {
				        @Override
				        public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
					        String generatedName = nameGenerator.apply(filePath, rootPath);
					        String baseName = filePath.getFileName().toString();
					        logger.trace("visiting {} {}", filePath, locale);
					        if (predicate != null) {
						        if (!predicate.test(baseName)) {
							        logger.trace("ignored {}", filePath);
							        return FileVisitResult.CONTINUE;
						        }
					        }

					        if (matchesLocale(baseName, locale)) {
						        if (logger.isEnabledFor(Level.TRACE)) {
							        logger.trace("kept {}, baseName={}, locale {}", filePath, baseName, locale);
						        }
						        localeNames.add(new Resource(generatedName, filePath));
					        } else if (matchesLocale(baseName, null)) {
						        if (logger.isEnabledFor(Level.TRACE)) {
							        logger.trace("kept_default {}, baseName={}, locale {}", filePath, baseName, locale);
						        }
						        englishNames.add(new Resource(generatedName, filePath));
					        } else {
						        if (logger.isEnabledFor(Level.TRACE)) {
							        logger.trace("ignored {}, baseName={}, wrong locale {}", filePath, baseName,
							                locale);
						        }
						        otherNames.add(new Resource(generatedName, filePath));
					        }
					        return FileVisitResult.CONTINUE;
				        }
			        });
			localeNames.addAll(englishNames);
			if (logger.isEnabledFor(Level.TRACE)) {
				logger.trace("resources: {}", localeNames);
			}

			return localeNames;
		} catch (IOException e) {
			LoggerUtils.logError(logger, e);
			throw new RuntimeException(e);
		}
	}

}
