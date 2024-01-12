// vite.generated.ts
import path from "path";
import { existsSync as existsSync5, mkdirSync as mkdirSync2, readdirSync as readdirSync2, readFileSync as readFileSync4, writeFileSync as writeFileSync2 } from "fs";
import { createHash } from "crypto";
import * as net from "net";

// target/plugins/application-theme-plugin/theme-handle.js
import { existsSync as existsSync3, readFileSync as readFileSync2 } from "fs";
import { resolve as resolve3 } from "path";

// target/plugins/application-theme-plugin/theme-generator.js
import { globSync as globSync2 } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/glob/dist/mjs/index.js";
import { resolve as resolve2, basename as basename2 } from "path";
import { existsSync as existsSync2, readFileSync, writeFileSync } from "fs";

// target/plugins/application-theme-plugin/theme-copy.js
import { readdirSync, statSync, mkdirSync, existsSync, copyFileSync } from "fs";
import { resolve, basename, relative, extname } from "path";
import { globSync } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/glob/dist/mjs/index.js";
var ignoredFileExtensions = [".css", ".js", ".json"];
function copyThemeResources(themeFolder2, projectStaticAssetsOutputFolder, logger) {
  const staticAssetsThemeFolder = resolve(projectStaticAssetsOutputFolder, "themes", basename(themeFolder2));
  const collection = collectFolders(themeFolder2, logger);
  if (collection.files.length > 0) {
    mkdirSync(staticAssetsThemeFolder, { recursive: true });
    collection.directories.forEach((directory) => {
      const relativeDirectory = relative(themeFolder2, directory);
      const targetDirectory = resolve(staticAssetsThemeFolder, relativeDirectory);
      mkdirSync(targetDirectory, { recursive: true });
    });
    collection.files.forEach((file) => {
      const relativeFile = relative(themeFolder2, file);
      const targetFile = resolve(staticAssetsThemeFolder, relativeFile);
      copyFileIfAbsentOrNewer(file, targetFile, logger);
    });
  }
}
function collectFolders(folderToCopy, logger) {
  const collection = { directories: [], files: [] };
  logger.trace("files in directory", readdirSync(folderToCopy));
  readdirSync(folderToCopy).forEach((file) => {
    const fileToCopy = resolve(folderToCopy, file);
    try {
      if (statSync(fileToCopy).isDirectory()) {
        logger.debug("Going through directory", fileToCopy);
        const result = collectFolders(fileToCopy, logger);
        if (result.files.length > 0) {
          collection.directories.push(fileToCopy);
          logger.debug("Adding directory", fileToCopy);
          collection.directories.push.apply(collection.directories, result.directories);
          collection.files.push.apply(collection.files, result.files);
        }
      } else if (!ignoredFileExtensions.includes(extname(fileToCopy))) {
        logger.debug("Adding file", fileToCopy);
        collection.files.push(fileToCopy);
      }
    } catch (error) {
      handleNoSuchFileError(fileToCopy, error, logger);
    }
  });
  return collection;
}
function copyStaticAssets(themeName, themeProperties, projectStaticAssetsOutputFolder, logger) {
  const assets = themeProperties["assets"];
  if (!assets) {
    logger.debug("no assets to handle no static assets were copied");
    return;
  }
  mkdirSync(projectStaticAssetsOutputFolder, {
    recursive: true
  });
  const missingModules = checkModules(Object.keys(assets));
  if (missingModules.length > 0) {
    throw Error(
      "Missing npm modules '" + missingModules.join("', '") + "' for assets marked in 'theme.json'.\nInstall package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm/bun i'"
    );
  }
  Object.keys(assets).forEach((module) => {
    const copyRules = assets[module];
    Object.keys(copyRules).forEach((copyRule) => {
      const nodeSources = resolve("node_modules/", module, copyRule);
      const files = globSync(nodeSources, { nodir: true });
      const targetFolder = resolve(projectStaticAssetsOutputFolder, "themes", themeName, copyRules[copyRule]);
      mkdirSync(targetFolder, {
        recursive: true
      });
      files.forEach((file) => {
        const copyTarget = resolve(targetFolder, basename(file));
        copyFileIfAbsentOrNewer(file, copyTarget, logger);
      });
    });
  });
}
function checkModules(modules) {
  const missing = [];
  modules.forEach((module) => {
    if (!existsSync(resolve("node_modules/", module))) {
      missing.push(module);
    }
  });
  return missing;
}
function copyFileIfAbsentOrNewer(fileToCopy, copyTarget, logger) {
  try {
    if (!existsSync(copyTarget) || statSync(copyTarget).mtime < statSync(fileToCopy).mtime) {
      logger.trace("Copying: ", fileToCopy, "=>", copyTarget);
      copyFileSync(fileToCopy, copyTarget);
    }
  } catch (error) {
    handleNoSuchFileError(fileToCopy, error, logger);
  }
}
function handleNoSuchFileError(file, error, logger) {
  if (error.code === "ENOENT") {
    logger.warn("Ignoring not existing file " + file + ". File may have been deleted during theme processing.");
  } else {
    throw error;
  }
}

// target/plugins/application-theme-plugin/theme-generator.js
var themeComponentsFolder = "components";
var documentCssFilename = "document.css";
var stylesCssFilename = "styles.css";
var CSSIMPORT_COMMENT = "CSSImport end";
var headerImport = `import 'construct-style-sheets-polyfill';
`;
function writeThemeFiles(themeFolder2, themeName, themeProperties, options) {
  const productionMode = !options.devMode;
  const useDevServerOrInProductionMode = !options.useDevBundle;
  const outputFolder = options.frontendGeneratedFolder;
  const styles = resolve2(themeFolder2, stylesCssFilename);
  const documentCssFile = resolve2(themeFolder2, documentCssFilename);
  const autoInjectComponents = themeProperties.autoInjectComponents ?? true;
  const globalFilename = "theme-" + themeName + ".global.generated.js";
  const componentsFilename = "theme-" + themeName + ".components.generated.js";
  const themeFilename = "theme-" + themeName + ".generated.js";
  let themeFileContent = headerImport;
  let globalImportContent = "// When this file is imported, global styles are automatically applied\n";
  let componentsFileContent = "";
  var componentsFiles;
  if (autoInjectComponents) {
    componentsFiles = globSync2("*.css", {
      cwd: resolve2(themeFolder2, themeComponentsFolder),
      nodir: true
    });
    if (componentsFiles.length > 0) {
      componentsFileContent += "import { unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';\n";
    }
  }
  if (themeProperties.parent) {
    themeFileContent += `import { applyTheme as applyBaseTheme } from './theme-${themeProperties.parent}.generated.js';
`;
  }
  themeFileContent += `import { injectGlobalCss } from 'Frontend/generated/jar-resources/theme-util.js';
`;
  themeFileContent += `import './${componentsFilename}';
`;
  themeFileContent += `let needsReloadOnChanges = false;
`;
  const imports = [];
  const componentCssImports = [];
  const globalFileContent = [];
  const globalCssCode = [];
  const shadowOnlyCss = [];
  const componentCssCode = [];
  const parentTheme = themeProperties.parent ? "applyBaseTheme(target);\n" : "";
  const parentThemeGlobalImport = themeProperties.parent ? `import './theme-${themeProperties.parent}.global.generated.js';
` : "";
  const themeIdentifier = "_vaadintheme_" + themeName + "_";
  const lumoCssFlag = "_vaadinthemelumoimports_";
  const globalCssFlag = themeIdentifier + "globalCss";
  const componentCssFlag = themeIdentifier + "componentCss";
  if (!existsSync2(styles)) {
    if (productionMode) {
      throw new Error(`styles.css file is missing and is needed for '${themeName}' in folder '${themeFolder2}'`);
    }
    writeFileSync(
      styles,
      "/* Import your application global css files here or add the styles directly to this file */",
      "utf8"
    );
  }
  let filename = basename2(styles);
  let variable = camelCase(filename);
  const lumoImports = themeProperties.lumoImports || ["color", "typography"];
  if (lumoImports) {
    lumoImports.forEach((lumoImport) => {
      imports.push(`import { ${lumoImport} } from '@vaadin/vaadin-lumo-styles/${lumoImport}.js';
`);
      if (lumoImport === "utility" || lumoImport === "badge" || lumoImport === "typography" || lumoImport === "color") {
        imports.push(`import '@vaadin/vaadin-lumo-styles/${lumoImport}-global.js';
`);
      }
    });
    lumoImports.forEach((lumoImport) => {
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${lumoImport}.cssText, '', target, true));
`);
    });
  }
  if (useDevServerOrInProductionMode) {
    globalFileContent.push(parentThemeGlobalImport);
    globalFileContent.push(`import 'themes/${themeName}/${filename}';
`);
    imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';
`);
    shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable}.toString(), '', target));
    `);
  }
  if (existsSync2(documentCssFile)) {
    filename = basename2(documentCssFile);
    variable = camelCase(filename);
    if (useDevServerOrInProductionMode) {
      globalFileContent.push(`import 'themes/${themeName}/${filename}';
`);
      imports.push(`import ${variable} from 'themes/${themeName}/${filename}?inline';
`);
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable}.toString(),'', document));
    `);
    }
  }
  let i = 0;
  if (themeProperties.documentCss) {
    const missingModules = checkModules(themeProperties.documentCss);
    if (missingModules.length > 0) {
      throw Error(
        "Missing npm modules or files '" + missingModules.join("', '") + "' for documentCss marked in 'theme.json'.\nInstall or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm/bun i'"
      );
    }
    themeProperties.documentCss.forEach((cssImport) => {
      const variable2 = "module" + i++;
      imports.push(`import ${variable2} from '${cssImport}?inline';
`);
      globalCssCode.push(`if(target !== document) {
        removers.push(injectGlobalCss(${variable2}.toString(), '', target));
    }
    `);
      globalCssCode.push(
        `removers.push(injectGlobalCss(${variable2}.toString(), '${CSSIMPORT_COMMENT}', document));
    `
      );
    });
  }
  if (themeProperties.importCss) {
    const missingModules = checkModules(themeProperties.importCss);
    if (missingModules.length > 0) {
      throw Error(
        "Missing npm modules or files '" + missingModules.join("', '") + "' for importCss marked in 'theme.json'.\nInstall or update package(s) by adding a @NpmPackage annotation or install it using 'npm/pnpm/bun i'"
      );
    }
    themeProperties.importCss.forEach((cssPath) => {
      const variable2 = "module" + i++;
      globalFileContent.push(`import '${cssPath}';
`);
      imports.push(`import ${variable2} from '${cssPath}?inline';
`);
      shadowOnlyCss.push(`removers.push(injectGlobalCss(${variable2}.toString(), '${CSSIMPORT_COMMENT}', target));
`);
    });
  }
  if (autoInjectComponents) {
    componentsFiles.forEach((componentCss) => {
      const filename2 = basename2(componentCss);
      const tag = filename2.replace(".css", "");
      const variable2 = camelCase(filename2);
      componentCssImports.push(
        `import ${variable2} from 'themes/${themeName}/${themeComponentsFolder}/${filename2}?inline';
`
      );
      const componentString = `registerStyles(
        '${tag}',
        unsafeCSS(${variable2}.toString())
      );
      `;
      componentCssCode.push(componentString);
    });
  }
  themeFileContent += imports.join("");
  const themeFileApply = `
  let themeRemovers = new WeakMap();
  let targets = [];

  export const applyTheme = (target) => {
    const removers = [];
    if (target !== document) {
      ${shadowOnlyCss.join("")}
    }
    ${parentTheme}
    ${globalCssCode.join("")}

    if (import.meta.hot) {
      targets.push(new WeakRef(target));
      themeRemovers.set(target, removers);
    }

  }
  
`;
  componentsFileContent += `
${componentCssImports.join("")}

if (!document['${componentCssFlag}']) {
  ${componentCssCode.join("")}
  document['${componentCssFlag}'] = true;
}

if (import.meta.hot) {
  import.meta.hot.accept((module) => {
    window.location.reload();
  });
}

`;
  themeFileContent += themeFileApply;
  themeFileContent += `
if (import.meta.hot) {
  import.meta.hot.accept((module) => {

    if (needsReloadOnChanges) {
      window.location.reload();
    } else {
      targets.forEach(targetRef => {
        const target = targetRef.deref();
        if (target) {
          themeRemovers.get(target).forEach(remover => remover())
          module.applyTheme(target);
        }
      })
    }
  });

  import.meta.hot.on('vite:afterUpdate', (update) => {
    document.dispatchEvent(new CustomEvent('vaadin-theme-updated', { detail: update }));
  });
}

`;
  globalImportContent += `
${globalFileContent.join("")}
`;
  writeIfChanged(resolve2(outputFolder, globalFilename), globalImportContent);
  writeIfChanged(resolve2(outputFolder, themeFilename), themeFileContent);
  writeIfChanged(resolve2(outputFolder, componentsFilename), componentsFileContent);
}
function writeIfChanged(file, data) {
  if (!existsSync2(file) || readFileSync(file, { encoding: "utf-8" }) !== data) {
    writeFileSync(file, data);
  }
}
function camelCase(str) {
  return str.replace(/(?:^\w|[A-Z]|\b\w)/g, function(word, index) {
    return index === 0 ? word.toLowerCase() : word.toUpperCase();
  }).replace(/\s+/g, "").replace(/\.|\-/g, "");
}

// target/plugins/application-theme-plugin/theme-handle.js
var nameRegex = /theme-(.*)\.generated\.js/;
var prevThemeName = void 0;
var firstThemeName = void 0;
function processThemeResources(options, logger) {
  const themeName = extractThemeName(options.frontendGeneratedFolder);
  if (themeName) {
    if (!prevThemeName && !firstThemeName) {
      firstThemeName = themeName;
    } else if (prevThemeName && prevThemeName !== themeName && firstThemeName !== themeName || !prevThemeName && firstThemeName !== themeName) {
      const warning = `Attention: Active theme is switched to '${themeName}'.`;
      const description = `
      Note that adding new style sheet files to '/themes/${themeName}/components', 
      may not be taken into effect until the next application restart.
      Changes to already existing style sheet files are being reloaded as before.`;
      logger.warn("*******************************************************************");
      logger.warn(warning);
      logger.warn(description);
      logger.warn("*******************************************************************");
    }
    prevThemeName = themeName;
    findThemeFolderAndHandleTheme(themeName, options, logger);
  } else {
    prevThemeName = void 0;
    logger.debug("Skipping Vaadin application theme handling.");
    logger.trace("Most likely no @Theme annotation for application or only themeClass used.");
  }
}
function findThemeFolderAndHandleTheme(themeName, options, logger) {
  let themeFound = false;
  for (let i = 0; i < options.themeProjectFolders.length; i++) {
    const themeProjectFolder = options.themeProjectFolders[i];
    if (existsSync3(themeProjectFolder)) {
      logger.debug("Searching themes folder '" + themeProjectFolder + "' for theme '" + themeName + "'");
      const handled = handleThemes(themeName, themeProjectFolder, options, logger);
      if (handled) {
        if (themeFound) {
          throw new Error(
            "Found theme files in '" + themeProjectFolder + "' and '" + themeFound + "'. Theme should only be available in one folder"
          );
        }
        logger.debug("Found theme files from '" + themeProjectFolder + "'");
        themeFound = themeProjectFolder;
      }
    }
  }
  if (existsSync3(options.themeResourceFolder)) {
    if (themeFound && existsSync3(resolve3(options.themeResourceFolder, themeName))) {
      throw new Error(
        "Theme '" + themeName + `'should not exist inside a jar and in the project at the same time
Extending another theme is possible by adding { "parent": "my-parent-theme" } entry to the theme.json file inside your theme folder.`
      );
    }
    logger.debug(
      "Searching theme jar resource folder '" + options.themeResourceFolder + "' for theme '" + themeName + "'"
    );
    handleThemes(themeName, options.themeResourceFolder, options, logger);
    themeFound = true;
  }
  return themeFound;
}
function handleThemes(themeName, themesFolder, options, logger) {
  const themeFolder2 = resolve3(themesFolder, themeName);
  if (existsSync3(themeFolder2)) {
    logger.debug("Found theme ", themeName, " in folder ", themeFolder2);
    const themeProperties = getThemeProperties(themeFolder2);
    if (themeProperties.parent) {
      const found = findThemeFolderAndHandleTheme(themeProperties.parent, options, logger);
      if (!found) {
        throw new Error(
          "Could not locate files for defined parent theme '" + themeProperties.parent + "'.\nPlease verify that dependency is added or theme folder exists."
        );
      }
    }
    copyStaticAssets(themeName, themeProperties, options.projectStaticAssetsOutputFolder, logger);
    copyThemeResources(themeFolder2, options.projectStaticAssetsOutputFolder, logger);
    writeThemeFiles(themeFolder2, themeName, themeProperties, options);
    return true;
  }
  return false;
}
function getThemeProperties(themeFolder2) {
  const themePropertyFile = resolve3(themeFolder2, "theme.json");
  if (!existsSync3(themePropertyFile)) {
    return {};
  }
  const themePropertyFileAsString = readFileSync2(themePropertyFile);
  if (themePropertyFileAsString.length === 0) {
    return {};
  }
  return JSON.parse(themePropertyFileAsString);
}
function extractThemeName(frontendGeneratedFolder) {
  if (!frontendGeneratedFolder) {
    throw new Error(
      "Couldn't extract theme name from 'theme.js', because the path to folder containing this file is empty. Please set the a correct folder path in ApplicationThemePlugin constructor parameters."
    );
  }
  const generatedThemeFile = resolve3(frontendGeneratedFolder, "theme.js");
  if (existsSync3(generatedThemeFile)) {
    const themeName = nameRegex.exec(readFileSync2(generatedThemeFile, { encoding: "utf8" }))[1];
    if (!themeName) {
      throw new Error("Couldn't parse theme name from '" + generatedThemeFile + "'.");
    }
    return themeName;
  } else {
    return "";
  }
}

// target/plugins/theme-loader/theme-loader-utils.js
import { existsSync as existsSync4, readFileSync as readFileSync3 } from "fs";
import { resolve as resolve4, basename as basename3 } from "path";
import { globSync as globSync3 } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/glob/dist/mjs/index.js";
var urlMatcher = /(url\(\s*)(\'|\")?(\.\/|\.\.\/)(\S*)(\2\s*\))/g;
function assetsContains(fileUrl, themeFolder2, logger) {
  const themeProperties = getThemeProperties2(themeFolder2);
  if (!themeProperties) {
    logger.debug("No theme properties found.");
    return false;
  }
  const assets = themeProperties["assets"];
  if (!assets) {
    logger.debug("No defined assets in theme properties");
    return false;
  }
  for (let module of Object.keys(assets)) {
    const copyRules = assets[module];
    for (let copyRule of Object.keys(copyRules)) {
      if (fileUrl.startsWith(copyRules[copyRule])) {
        const targetFile = fileUrl.replace(copyRules[copyRule], "");
        const files = globSync3(resolve4("node_modules/", module, copyRule), { nodir: true });
        for (let file of files) {
          if (file.endsWith(targetFile))
            return true;
        }
      }
    }
  }
  return false;
}
function getThemeProperties2(themeFolder2) {
  const themePropertyFile = resolve4(themeFolder2, "theme.json");
  if (!existsSync4(themePropertyFile)) {
    return {};
  }
  const themePropertyFileAsString = readFileSync3(themePropertyFile);
  if (themePropertyFileAsString.length === 0) {
    return {};
  }
  return JSON.parse(themePropertyFileAsString);
}
function rewriteCssUrls(source, handledResourceFolder, themeFolder2, logger, options) {
  source = source.replace(urlMatcher, function(match, url, quoteMark, replace2, fileUrl, endString) {
    let absolutePath = resolve4(handledResourceFolder, replace2, fileUrl);
    const existingThemeResource = absolutePath.startsWith(themeFolder2) && existsSync4(absolutePath);
    if (existingThemeResource || assetsContains(fileUrl, themeFolder2, logger)) {
      const replacement = options.devMode ? "./" : "../static/";
      const skipLoader = existingThemeResource ? "" : replacement;
      const frontendThemeFolder = skipLoader + "themes/" + basename3(themeFolder2);
      logger.debug(
        "Updating url for file",
        "'" + replace2 + fileUrl + "'",
        "to use",
        "'" + frontendThemeFolder + "/" + fileUrl + "'"
      );
      const pathResolved = absolutePath.substring(themeFolder2.length).replace(/\\/g, "/");
      return url + (quoteMark ?? "") + frontendThemeFolder + pathResolved + endString;
    } else if (options.devMode) {
      logger.log("No rewrite for '", match, "' as the file was not found.");
    } else {
      return url + (quoteMark ?? "") + "../../" + fileUrl + endString;
    }
    return match;
  });
  return source;
}

// target/vaadin-dev-server-settings.json
var vaadin_dev_server_settings_default = {
  frontendFolder: "C:/Users/lamyj/git/owlcms4/owlcms/frontend",
  themeFolder: "themes",
  themeResourceFolder: "C:/Users/lamyj/git/owlcms4/owlcms/frontend/generated/jar-resources",
  staticOutput: "C:/Users/lamyj/git/owlcms4/owlcms/target/classes/META-INF/VAADIN/webapp/VAADIN/static",
  generatedFolder: "generated",
  statsOutput: "C:\\Users\\lamyj\\git\\owlcms4\\owlcms\\target\\classes\\META-INF\\VAADIN\\config",
  frontendBundleOutput: "C:\\Users\\lamyj\\git\\owlcms4\\owlcms\\target\\classes\\META-INF\\VAADIN\\webapp",
  devBundleOutput: "C:/Users/lamyj/git/owlcms4/owlcms/target/dev-bundle/webapp",
  devBundleStatsOutput: "C:/Users/lamyj/git/owlcms4/owlcms/target/dev-bundle/config",
  jarResourcesFolder: "C:/Users/lamyj/git/owlcms4/owlcms/frontend/generated/jar-resources",
  themeName: "owlcms",
  clientServiceWorkerSource: "C:\\Users\\lamyj\\git\\owlcms4\\owlcms\\target\\sw.ts",
  pwaEnabled: false,
  offlineEnabled: false,
  offlinePath: "'offline.html'"
};

// vite.generated.ts
import {
  defineConfig,
  mergeConfig
} from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/vite/dist/node/index.js";
import { getManifest } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/workbox-build/build/index.js";
import * as rollup from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/rollup/dist/es/rollup.js";
import brotli from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/rollup-plugin-brotli/lib/index.cjs.js";
import replace from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/@rollup/plugin-replace/dist/es/index.js";
import checker from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/vite-plugin-checker/dist/esm/main.js";

// target/plugins/rollup-plugin-postcss-lit-custom/rollup-plugin-postcss-lit.js
import { createFilter } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/@rollup/pluginutils/dist/es/index.js";
import transformAst from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/transform-ast/index.js";
var assetUrlRE = /__VITE_ASSET__([\w$]+)__(?:\$_(.*?)__)?/g;
var escape = (str) => str.replace(assetUrlRE, '${unsafeCSSTag("__VITE_ASSET__$1__$2")}').replace(/`/g, "\\`").replace(/\\(?!`)/g, "\\\\");
function postcssLit(options = {}) {
  const defaultOptions = {
    include: "**/*.{css,sss,pcss,styl,stylus,sass,scss,less}",
    exclude: null,
    importPackage: "lit"
  };
  const opts = { ...defaultOptions, ...options };
  const filter = createFilter(opts.include, opts.exclude);
  return {
    name: "postcss-lit",
    enforce: "post",
    transform(code, id) {
      if (!filter(id))
        return;
      const ast = this.parse(code, {});
      let defaultExportName;
      let isDeclarationLiteral = false;
      const magicString = transformAst(code, { ast }, (node) => {
        if (node.type === "ExportDefaultDeclaration") {
          defaultExportName = node.declaration.name;
          isDeclarationLiteral = node.declaration.type === "Literal";
        }
      });
      if (!defaultExportName && !isDeclarationLiteral) {
        return;
      }
      magicString.walk((node) => {
        if (defaultExportName && node.type === "VariableDeclaration") {
          const exportedVar = node.declarations.find((d) => d.id.name === defaultExportName);
          if (exportedVar) {
            exportedVar.init.edit.update(`cssTag\`${escape(exportedVar.init.value)}\``);
          }
        }
        if (isDeclarationLiteral && node.type === "ExportDefaultDeclaration") {
          node.declaration.edit.update(`cssTag\`${escape(node.declaration.value)}\``);
        }
      });
      magicString.prepend(`import {css as cssTag, unsafeCSS as unsafeCSSTag} from '${opts.importPackage}';
`);
      return {
        code: magicString.toString(),
        map: magicString.generateMap({
          hires: true
        })
      };
    }
  };
}

// vite.generated.ts
import { createRequire } from "module";
import { visualizer } from "file:///C:/Users/lamyj/git/owlcms4/owlcms/node_modules/rollup-plugin-visualizer/dist/plugin/index.js";
var __vite_injected_original_dirname = "C:\\Users\\lamyj\\git\\owlcms4\\owlcms";
var __vite_injected_original_import_meta_url = "file:///C:/Users/lamyj/git/owlcms4/owlcms/vite.generated.ts";
var require2 = createRequire(__vite_injected_original_import_meta_url);
var appShellUrl = ".";
var frontendFolder = path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.frontendFolder);
var themeFolder = path.resolve(frontendFolder, vaadin_dev_server_settings_default.themeFolder);
var frontendBundleFolder = path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.frontendBundleOutput);
var devBundleFolder = path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.devBundleOutput);
var devBundle = !!process.env.devBundle;
var jarResourcesFolder = path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.jarResourcesFolder);
var themeResourceFolder = path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.themeResourceFolder);
var projectPackageJsonFile = path.resolve(__vite_injected_original_dirname, "package.json");
var buildOutputFolder = devBundle ? devBundleFolder : frontendBundleFolder;
var statsFolder = path.resolve(__vite_injected_original_dirname, devBundle ? vaadin_dev_server_settings_default.devBundleStatsOutput : vaadin_dev_server_settings_default.statsOutput);
var statsFile = path.resolve(statsFolder, "stats.json");
var bundleSizeFile = path.resolve(statsFolder, "bundle-size.html");
var nodeModulesFolder = path.resolve(__vite_injected_original_dirname, "node_modules");
var webComponentTags = "";
var projectIndexHtml = path.resolve(frontendFolder, "index.html");
var projectStaticAssetsFolders = [
  path.resolve(__vite_injected_original_dirname, "src", "main", "resources", "META-INF", "resources"),
  path.resolve(__vite_injected_original_dirname, "src", "main", "resources", "static"),
  frontendFolder
];
var themeProjectFolders = projectStaticAssetsFolders.map((folder) => path.resolve(folder, vaadin_dev_server_settings_default.themeFolder));
var themeOptions = {
  devMode: false,
  useDevBundle: devBundle,
  // The following matches folder 'frontend/generated/themes/'
  // (not 'frontend/themes') for theme in JAR that is copied there
  themeResourceFolder: path.resolve(themeResourceFolder, vaadin_dev_server_settings_default.themeFolder),
  themeProjectFolders,
  projectStaticAssetsOutputFolder: devBundle ? path.resolve(devBundleFolder, "../assets") : path.resolve(__vite_injected_original_dirname, vaadin_dev_server_settings_default.staticOutput),
  frontendGeneratedFolder: path.resolve(frontendFolder, vaadin_dev_server_settings_default.generatedFolder)
};
var hasExportedWebComponents = existsSync5(path.resolve(frontendFolder, "web-component.html"));
console.trace = () => {
};
console.debug = () => {
};
function injectManifestToSWPlugin() {
  const rewriteManifestIndexHtmlUrl = (manifest) => {
    const indexEntry = manifest.find((entry) => entry.url === "index.html");
    if (indexEntry) {
      indexEntry.url = appShellUrl;
    }
    return { manifest, warnings: [] };
  };
  return {
    name: "vaadin:inject-manifest-to-sw",
    async transform(code, id) {
      if (/sw\.(ts|js)$/.test(id)) {
        const { manifestEntries } = await getManifest({
          globDirectory: buildOutputFolder,
          globPatterns: ["**/*"],
          globIgnores: ["**/*.br"],
          manifestTransforms: [rewriteManifestIndexHtmlUrl],
          maximumFileSizeToCacheInBytes: 100 * 1024 * 1024
          // 100mb,
        });
        return code.replace("self.__WB_MANIFEST", JSON.stringify(manifestEntries));
      }
    }
  };
}
function buildSWPlugin(opts) {
  let config;
  const devMode = opts.devMode;
  const swObj = {};
  async function build(action, additionalPlugins = []) {
    const includedPluginNames = [
      "vite:esbuild",
      "rollup-plugin-dynamic-import-variables",
      "vite:esbuild-transpile",
      "vite:terser"
    ];
    const plugins = config.plugins.filter((p) => {
      return includedPluginNames.includes(p.name);
    });
    const resolver = config.createResolver();
    const resolvePlugin = {
      name: "resolver",
      resolveId(source, importer, _options) {
        return resolver(source, importer);
      }
    };
    plugins.unshift(resolvePlugin);
    plugins.push(
      replace({
        values: {
          "process.env.NODE_ENV": JSON.stringify(config.mode),
          ...config.define
        },
        preventAssignment: true
      })
    );
    if (additionalPlugins) {
      plugins.push(...additionalPlugins);
    }
    const bundle = await rollup.rollup({
      input: path.resolve(vaadin_dev_server_settings_default.clientServiceWorkerSource),
      plugins
    });
    try {
      return await bundle[action]({
        file: path.resolve(buildOutputFolder, "sw.js"),
        format: "es",
        exports: "none",
        sourcemap: config.command === "serve" || config.build.sourcemap,
        inlineDynamicImports: true
      });
    } finally {
      await bundle.close();
    }
  }
  return {
    name: "vaadin:build-sw",
    enforce: "post",
    async configResolved(resolvedConfig) {
      config = resolvedConfig;
    },
    async buildStart() {
      if (devMode) {
        const { output } = await build("generate");
        swObj.code = output[0].code;
        swObj.map = output[0].map;
      }
    },
    async load(id) {
      if (id.endsWith("sw.js")) {
        return "";
      }
    },
    async transform(_code, id) {
      if (id.endsWith("sw.js")) {
        return swObj;
      }
    },
    async closeBundle() {
      if (!devMode) {
        await build("write", [injectManifestToSWPlugin(), brotli()]);
      }
    }
  };
}
function statsExtracterPlugin() {
  function collectThemeJsonsInFrontend(themeJsonContents, themeName) {
    const themeJson = path.resolve(frontendFolder, vaadin_dev_server_settings_default.themeFolder, themeName, "theme.json");
    if (existsSync5(themeJson)) {
      const themeJsonContent = readFileSync4(themeJson, { encoding: "utf-8" }).replace(/\r\n/g, "\n");
      themeJsonContents[themeName] = themeJsonContent;
      const themeJsonObject = JSON.parse(themeJsonContent);
      if (themeJsonObject.parent) {
        collectThemeJsonsInFrontend(themeJsonContents, themeJsonObject.parent);
      }
    }
  }
  return {
    name: "vaadin:stats",
    enforce: "post",
    async writeBundle(options, bundle) {
      const modules = Object.values(bundle).flatMap((b) => b.modules ? Object.keys(b.modules) : []);
      const nodeModulesFolders = modules.map((id) => id.replace(/\\/g, "/")).filter((id) => id.startsWith(nodeModulesFolder.replace(/\\/g, "/"))).map((id) => id.substring(nodeModulesFolder.length + 1));
      const npmModules = nodeModulesFolders.map((id) => id.replace(/\\/g, "/")).map((id) => {
        const parts = id.split("/");
        if (id.startsWith("@")) {
          return parts[0] + "/" + parts[1];
        } else {
          return parts[0];
        }
      }).sort().filter((value, index, self) => self.indexOf(value) === index);
      const npmModuleAndVersion = Object.fromEntries(npmModules.map((module) => [module, getVersion(module)]));
      const cvdls = Object.fromEntries(
        npmModules.filter((module) => getCvdlName(module) != null).map((module) => [module, { name: getCvdlName(module), version: getVersion(module) }])
      );
      mkdirSync2(path.dirname(statsFile), { recursive: true });
      const projectPackageJson = JSON.parse(readFileSync4(projectPackageJsonFile, { encoding: "utf-8" }));
      const entryScripts = Object.values(bundle).filter((bundle2) => bundle2.isEntry).map((bundle2) => bundle2.fileName);
      const generatedIndexHtml = path.resolve(buildOutputFolder, "index.html");
      const customIndexData = readFileSync4(projectIndexHtml, { encoding: "utf-8" });
      const generatedIndexData = readFileSync4(generatedIndexHtml, {
        encoding: "utf-8"
      });
      const customIndexRows = new Set(customIndexData.split(/[\r\n]/).filter((row) => row.trim() !== ""));
      const generatedIndexRows = generatedIndexData.split(/[\r\n]/).filter((row) => row.trim() !== "");
      const rowsGenerated = [];
      generatedIndexRows.forEach((row) => {
        if (!customIndexRows.has(row)) {
          rowsGenerated.push(row);
        }
      });
      const parseImports = (filename, result) => {
        const content = readFileSync4(filename, { encoding: "utf-8" });
        const lines = content.split("\n");
        const staticImports = lines.filter((line) => line.startsWith("import ")).map((line) => line.substring(line.indexOf("'") + 1, line.lastIndexOf("'"))).map((line) => line.includes("?") ? line.substring(0, line.lastIndexOf("?")) : line);
        const dynamicImports = lines.filter((line) => line.includes("import(")).map((line) => line.replace(/.*import\(/, "")).map((line) => line.split(/'/)[1]).map((line) => line.includes("?") ? line.substring(0, line.lastIndexOf("?")) : line);
        staticImports.forEach((staticImport) => result.add(staticImport));
        dynamicImports.map((dynamicImport) => {
          const importedFile = path.resolve(path.dirname(filename), dynamicImport);
          parseImports(importedFile, result);
        });
      };
      const generatedImportsSet = /* @__PURE__ */ new Set();
      parseImports(
        path.resolve(themeOptions.frontendGeneratedFolder, "flow", "generated-flow-imports.js"),
        generatedImportsSet
      );
      const generatedImports = Array.from(generatedImportsSet).sort();
      const frontendFiles = {};
      const projectFileExtensions = [".js", ".js.map", ".ts", ".ts.map", ".tsx", ".tsx.map", ".css", ".css.map"];
      const isThemeComponentsResource = (id) => id.startsWith(themeOptions.frontendGeneratedFolder.replace(/\\/g, "/")) && id.match(/.*\/jar-resources\/themes\/[^\/]+\/components\//);
      modules.map((id) => id.replace(/\\/g, "/")).filter((id) => id.startsWith(frontendFolder.replace(/\\/g, "/"))).filter((id) => !id.startsWith(themeOptions.frontendGeneratedFolder.replace(/\\/g, "/")) || isThemeComponentsResource(id)).map((id) => id.substring(frontendFolder.length + 1)).map((line) => line.includes("?") ? line.substring(0, line.lastIndexOf("?")) : line).forEach((line) => {
        const filePath = path.resolve(frontendFolder, line);
        if (projectFileExtensions.includes(path.extname(filePath))) {
          const fileBuffer = readFileSync4(filePath, { encoding: "utf-8" }).replace(/\r\n/g, "\n");
          frontendFiles[line] = createHash("sha256").update(fileBuffer, "utf8").digest("hex");
        }
      });
      generatedImports.filter((line) => line.includes("generated/jar-resources")).forEach((line) => {
        let filename = line.substring(line.indexOf("generated"));
        const fileBuffer = readFileSync4(path.resolve(frontendFolder, filename), { encoding: "utf-8" }).replace(
          /\r\n/g,
          "\n"
        );
        const hash = createHash("sha256").update(fileBuffer, "utf8").digest("hex");
        const fileKey = line.substring(line.indexOf("jar-resources/") + 14);
        frontendFiles[fileKey] = hash;
      });
      if (existsSync5(path.resolve(frontendFolder, "index.ts"))) {
        const fileBuffer = readFileSync4(path.resolve(frontendFolder, "index.ts"), { encoding: "utf-8" }).replace(
          /\r\n/g,
          "\n"
        );
        frontendFiles[`index.ts`] = createHash("sha256").update(fileBuffer, "utf8").digest("hex");
      }
      const themeJsonContents = {};
      const themesFolder = path.resolve(jarResourcesFolder, "themes");
      if (existsSync5(themesFolder)) {
        readdirSync2(themesFolder).forEach((themeFolder2) => {
          const themeJson = path.resolve(themesFolder, themeFolder2, "theme.json");
          if (existsSync5(themeJson)) {
            themeJsonContents[path.basename(themeFolder2)] = readFileSync4(themeJson, { encoding: "utf-8" }).replace(
              /\r\n/g,
              "\n"
            );
          }
        });
      }
      collectThemeJsonsInFrontend(themeJsonContents, vaadin_dev_server_settings_default.themeName);
      let webComponents = [];
      if (webComponentTags) {
        webComponents = webComponentTags.split(";");
      }
      const stats = {
        packageJsonDependencies: projectPackageJson.dependencies,
        npmModules: npmModuleAndVersion,
        bundleImports: generatedImports,
        frontendHashes: frontendFiles,
        themeJsonContents,
        entryScripts,
        webComponents,
        cvdlModules: cvdls,
        packageJsonHash: projectPackageJson?.vaadin?.hash,
        indexHtmlGenerated: rowsGenerated
      };
      writeFileSync2(statsFile, JSON.stringify(stats, null, 1));
    }
  };
}
function vaadinBundlesPlugin() {
  const disabledMessage = "Vaadin component dependency bundles are disabled.";
  const modulesDirectory = nodeModulesFolder.replace(/\\/g, "/");
  let vaadinBundleJson;
  function parseModuleId(id) {
    const [scope, scopedPackageName] = id.split("/", 3);
    const packageName = scope.startsWith("@") ? `${scope}/${scopedPackageName}` : scope;
    const modulePath = `.${id.substring(packageName.length)}`;
    return {
      packageName,
      modulePath
    };
  }
  function getExports(id) {
    const { packageName, modulePath } = parseModuleId(id);
    const packageInfo = vaadinBundleJson.packages[packageName];
    if (!packageInfo)
      return;
    const exposeInfo = packageInfo.exposes[modulePath];
    if (!exposeInfo)
      return;
    const exportsSet = /* @__PURE__ */ new Set();
    for (const e of exposeInfo.exports) {
      if (typeof e === "string") {
        exportsSet.add(e);
      } else {
        const { namespace, source } = e;
        if (namespace) {
          exportsSet.add(namespace);
        } else {
          const sourceExports = getExports(source);
          if (sourceExports) {
            sourceExports.forEach((e2) => exportsSet.add(e2));
          }
        }
      }
    }
    return Array.from(exportsSet);
  }
  function getExportBinding(binding) {
    return binding === "default" ? "_default as default" : binding;
  }
  function getImportAssigment(binding) {
    return binding === "default" ? "default: _default" : binding;
  }
  return {
    name: "vaadin:bundles",
    enforce: "pre",
    apply(config, { command }) {
      if (command !== "serve")
        return false;
      try {
        const vaadinBundleJsonPath = require2.resolve("@vaadin/bundles/vaadin-bundle.json");
        vaadinBundleJson = JSON.parse(readFileSync4(vaadinBundleJsonPath, { encoding: "utf8" }));
      } catch (e) {
        if (typeof e === "object" && e.code === "MODULE_NOT_FOUND") {
          vaadinBundleJson = { packages: {} };
          console.info(`@vaadin/bundles npm package is not found, ${disabledMessage}`);
          return false;
        } else {
          throw e;
        }
      }
      const versionMismatches = [];
      for (const [name, packageInfo] of Object.entries(vaadinBundleJson.packages)) {
        let installedVersion = void 0;
        try {
          const { version: bundledVersion } = packageInfo;
          const installedPackageJsonFile = path.resolve(modulesDirectory, name, "package.json");
          const packageJson = JSON.parse(readFileSync4(installedPackageJsonFile, { encoding: "utf8" }));
          installedVersion = packageJson.version;
          if (installedVersion && installedVersion !== bundledVersion) {
            versionMismatches.push({
              name,
              bundledVersion,
              installedVersion
            });
          }
        } catch (_) {
        }
      }
      if (versionMismatches.length) {
        console.info(`@vaadin/bundles has version mismatches with installed packages, ${disabledMessage}`);
        console.info(`Packages with version mismatches: ${JSON.stringify(versionMismatches, void 0, 2)}`);
        vaadinBundleJson = { packages: {} };
        return false;
      }
      return true;
    },
    async config(config) {
      return mergeConfig(
        {
          optimizeDeps: {
            exclude: [
              // Vaadin bundle
              "@vaadin/bundles",
              ...Object.keys(vaadinBundleJson.packages),
              "@vaadin/vaadin-material-styles"
            ]
          }
        },
        config
      );
    },
    load(rawId) {
      const [path2, params] = rawId.split("?");
      if (!path2.startsWith(modulesDirectory))
        return;
      const id = path2.substring(modulesDirectory.length + 1);
      const bindings = getExports(id);
      if (bindings === void 0)
        return;
      const cacheSuffix = params ? `?${params}` : "";
      const bundlePath = `@vaadin/bundles/vaadin.js${cacheSuffix}`;
      return `import { init as VaadinBundleInit, get as VaadinBundleGet } from '${bundlePath}';
await VaadinBundleInit('default');
const { ${bindings.map(getImportAssigment).join(", ")} } = (await VaadinBundleGet('./node_modules/${id}'))();
export { ${bindings.map(getExportBinding).join(", ")} };`;
    }
  };
}
function themePlugin(opts) {
  const fullThemeOptions = { ...themeOptions, devMode: opts.devMode };
  return {
    name: "vaadin:theme",
    config() {
      processThemeResources(fullThemeOptions, console);
    },
    configureServer(server) {
      function handleThemeFileCreateDelete(themeFile, stats) {
        if (themeFile.startsWith(themeFolder)) {
          const changed = path.relative(themeFolder, themeFile);
          console.debug("Theme file " + (!!stats ? "created" : "deleted"), changed);
          processThemeResources(fullThemeOptions, console);
        }
      }
      server.watcher.on("add", handleThemeFileCreateDelete);
      server.watcher.on("unlink", handleThemeFileCreateDelete);
    },
    handleHotUpdate(context) {
      const contextPath = path.resolve(context.file);
      const themePath = path.resolve(themeFolder);
      if (contextPath.startsWith(themePath)) {
        const changed = path.relative(themePath, contextPath);
        console.debug("Theme file changed", changed);
        if (changed.startsWith(vaadin_dev_server_settings_default.themeName)) {
          processThemeResources(fullThemeOptions, console);
        }
      }
    },
    async resolveId(id, importer) {
      if (path.resolve(themeOptions.frontendGeneratedFolder, "theme.js") === importer && !existsSync5(path.resolve(themeOptions.frontendGeneratedFolder, id))) {
        console.debug("Generate theme file " + id + " not existing. Processing theme resource");
        processThemeResources(fullThemeOptions, console);
        return;
      }
      if (!id.startsWith(vaadin_dev_server_settings_default.themeFolder)) {
        return;
      }
      for (const location of [themeResourceFolder, frontendFolder]) {
        const result = await this.resolve(path.resolve(location, id));
        if (result) {
          return result;
        }
      }
    },
    async transform(raw, id, options) {
      const [bareId, query] = id.split("?");
      if (!bareId?.startsWith(themeFolder) && !bareId?.startsWith(themeOptions.themeResourceFolder) || !bareId?.endsWith(".css")) {
        return;
      }
      const [themeName] = bareId.substring(themeFolder.length + 1).split("/");
      return rewriteCssUrls(raw, path.dirname(bareId), path.resolve(themeFolder, themeName), console, opts);
    }
  };
}
function runWatchDog(watchDogPort, watchDogHost) {
  const client = net.Socket();
  client.setEncoding("utf8");
  client.on("error", function(err) {
    console.log("Watchdog connection error. Terminating vite process...", err);
    client.destroy();
    process.exit(0);
  });
  client.on("close", function() {
    client.destroy();
    runWatchDog(watchDogPort, watchDogHost);
  });
  client.connect(watchDogPort, watchDogHost || "localhost");
}
var spaMiddlewareForceRemoved = false;
var allowedFrontendFolders = [frontendFolder, nodeModulesFolder];
function showRecompileReason() {
  return {
    name: "vaadin:why-you-compile",
    handleHotUpdate(context) {
      console.log("Recompiling because", context.file, "changed");
    }
  };
}
var DEV_MODE_START_REGEXP = /\/\*[\*!]\s+vaadin-dev-mode:start/;
var DEV_MODE_CODE_REGEXP = /\/\*[\*!]\s+vaadin-dev-mode:start([\s\S]*)vaadin-dev-mode:end\s+\*\*\//i;
function preserveUsageStats() {
  return {
    name: "vaadin:preserve-usage-stats",
    transform(src, id) {
      if (id.includes("vaadin-usage-statistics")) {
        if (src.includes("vaadin-dev-mode:start")) {
          const newSrc = src.replace(DEV_MODE_START_REGEXP, "/*! vaadin-dev-mode:start");
          if (newSrc === src) {
            console.error("Comment replacement failed to change anything");
          } else if (!newSrc.match(DEV_MODE_CODE_REGEXP)) {
            console.error("New comment fails to match original regexp");
          } else {
            return { code: newSrc };
          }
        }
      }
      return { code: src };
    }
  };
}
var vaadinConfig = (env) => {
  const devMode = env.mode === "development";
  const productionMode = !devMode && !devBundle;
  if (devMode && process.env.watchDogPort) {
    runWatchDog(process.env.watchDogPort, process.env.watchDogHost);
  }
  return {
    root: frontendFolder,
    base: "",
    publicDir: false,
    resolve: {
      alias: {
        "@vaadin/flow-frontend": jarResourcesFolder,
        Frontend: frontendFolder
      },
      preserveSymlinks: true
    },
    define: {
      OFFLINE_PATH: vaadin_dev_server_settings_default.offlinePath,
      VITE_ENABLED: "true"
    },
    server: {
      host: "127.0.0.1",
      strictPort: true,
      fs: {
        allow: allowedFrontendFolders
      }
    },
    build: {
      outDir: buildOutputFolder,
      emptyOutDir: devBundle,
      assetsDir: "VAADIN/build",
      rollupOptions: {
        input: {
          indexhtml: projectIndexHtml,
          ...hasExportedWebComponents ? { webcomponenthtml: path.resolve(frontendFolder, "web-component.html") } : {}
        },
        onwarn: (warning, defaultHandler) => {
          const ignoreEvalWarning = [
            "generated/jar-resources/FlowClient.js",
            "generated/jar-resources/vaadin-spreadsheet/spreadsheet-export.js",
            "@vaadin/charts/src/helpers.js"
          ];
          if (warning.code === "EVAL" && warning.id && !!ignoreEvalWarning.find((id) => warning.id.endsWith(id))) {
            return;
          }
          defaultHandler(warning);
        }
      }
    },
    optimizeDeps: {
      entries: [
        // Pre-scan entrypoints in Vite to avoid reloading on first open
        "generated/vaadin.ts"
      ],
      exclude: [
        "@vaadin/router",
        "@vaadin/vaadin-license-checker",
        "@vaadin/vaadin-usage-statistics",
        "workbox-core",
        "workbox-precaching",
        "workbox-routing",
        "workbox-strategies"
      ]
    },
    plugins: [
      productionMode && brotli(),
      devMode && vaadinBundlesPlugin(),
      devMode && showRecompileReason(),
      vaadin_dev_server_settings_default.offlineEnabled && buildSWPlugin({ devMode }),
      !devMode && statsExtracterPlugin(),
      devBundle && preserveUsageStats(),
      themePlugin({ devMode }),
      postcssLit({
        include: ["**/*.css", /.*\/.*\.css\?.*/],
        exclude: [
          `${themeFolder}/**/*.css`,
          new RegExp(`${themeFolder}/.*/.*\\.css\\?.*`),
          `${themeResourceFolder}/**/*.css`,
          new RegExp(`${themeResourceFolder}/.*/.*\\.css\\?.*`),
          new RegExp(".*/.*\\?html-proxy.*")
        ]
      }),
      {
        name: "vaadin:force-remove-html-middleware",
        transformIndexHtml: {
          order: "pre",
          handler(_html, { server }) {
            if (server && !spaMiddlewareForceRemoved) {
              server.middlewares.stack = server.middlewares.stack.filter((mw) => {
                const handleName = "" + mw.handle;
                return !handleName.includes("viteHtmlFallbackMiddleware");
              });
              spaMiddlewareForceRemoved = true;
            }
          }
        }
      },
      hasExportedWebComponents && {
        name: "vaadin:inject-entrypoints-to-web-component-html",
        transformIndexHtml: {
          order: "pre",
          handler(_html, { path: path2, server }) {
            if (path2 !== "/web-component.html") {
              return;
            }
            return [
              {
                tag: "script",
                attrs: { type: "module", src: `/generated/vaadin-web-component.ts` },
                injectTo: "head"
              }
            ];
          }
        }
      },
      {
        name: "vaadin:inject-entrypoints-to-index-html",
        transformIndexHtml: {
          order: "pre",
          handler(_html, { path: path2, server }) {
            if (path2 !== "/index.html") {
              return;
            }
            const scripts = [];
            if (devMode) {
              scripts.push({
                tag: "script",
                attrs: { type: "module", src: `/generated/vite-devmode.ts` },
                injectTo: "head"
              });
            }
            scripts.push({
              tag: "script",
              attrs: { type: "module", src: "/generated/vaadin.ts" },
              injectTo: "head"
            });
            return scripts;
          }
        }
      },
      checker({
        typescript: true
      }),
      productionMode && visualizer({ brotliSize: true, filename: bundleSizeFile })
    ]
  };
};
var overrideVaadinConfig = (customConfig2) => {
  return defineConfig((env) => mergeConfig(vaadinConfig(env), customConfig2(env)));
};
function getVersion(module) {
  const packageJson = path.resolve(nodeModulesFolder, module, "package.json");
  return JSON.parse(readFileSync4(packageJson, { encoding: "utf-8" })).version;
}
function getCvdlName(module) {
  const packageJson = path.resolve(nodeModulesFolder, module, "package.json");
  return JSON.parse(readFileSync4(packageJson, { encoding: "utf-8" })).cvdlName;
}

// vite.config.ts
var customConfig = (env) => ({
  // Here you can add custom Vite parameters
  // https://vitejs.dev/config/
});
var vite_config_default = overrideVaadinConfig(customConfig);
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5nZW5lcmF0ZWQudHMiLCAidGFyZ2V0L3BsdWdpbnMvYXBwbGljYXRpb24tdGhlbWUtcGx1Z2luL3RoZW1lLWhhbmRsZS5qcyIsICJ0YXJnZXQvcGx1Z2lucy9hcHBsaWNhdGlvbi10aGVtZS1wbHVnaW4vdGhlbWUtZ2VuZXJhdG9yLmpzIiwgInRhcmdldC9wbHVnaW5zL2FwcGxpY2F0aW9uLXRoZW1lLXBsdWdpbi90aGVtZS1jb3B5LmpzIiwgInRhcmdldC9wbHVnaW5zL3RoZW1lLWxvYWRlci90aGVtZS1sb2FkZXItdXRpbHMuanMiLCAidGFyZ2V0L3ZhYWRpbi1kZXYtc2VydmVyLXNldHRpbmdzLmpzb24iLCAidGFyZ2V0L3BsdWdpbnMvcm9sbHVwLXBsdWdpbi1wb3N0Y3NzLWxpdC1jdXN0b20vcm9sbHVwLXBsdWdpbi1wb3N0Y3NzLWxpdC5qcyIsICJ2aXRlLmNvbmZpZy50cyJdLAogICJzb3VyY2VzQ29udGVudCI6IFsiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHZpdGUuZ2VuZXJhdGVkLnRzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9sYW15ai9naXQvb3dsY21zNC9vd2xjbXMvdml0ZS5nZW5lcmF0ZWQudHNcIjsvKipcbiAqIE5PVElDRTogdGhpcyBpcyBhbiBhdXRvLWdlbmVyYXRlZCBmaWxlXG4gKlxuICogVGhpcyBmaWxlIGhhcyBiZWVuIGdlbmVyYXRlZCBieSB0aGUgYGZsb3c6cHJlcGFyZS1mcm9udGVuZGAgbWF2ZW4gZ29hbC5cbiAqIFRoaXMgZmlsZSB3aWxsIGJlIG92ZXJ3cml0dGVuIG9uIGV2ZXJ5IHJ1bi4gQW55IGN1c3RvbSBjaGFuZ2VzIHNob3VsZCBiZSBtYWRlIHRvIHZpdGUuY29uZmlnLnRzXG4gKi9cbmltcG9ydCBwYXRoIGZyb20gJ3BhdGgnO1xuaW1wb3J0IHsgZXhpc3RzU3luYywgbWtkaXJTeW5jLCByZWFkZGlyU3luYywgcmVhZEZpbGVTeW5jLCB3cml0ZUZpbGVTeW5jIH0gZnJvbSAnZnMnO1xuaW1wb3J0IHsgY3JlYXRlSGFzaCB9IGZyb20gJ2NyeXB0byc7XG5pbXBvcnQgKiBhcyBuZXQgZnJvbSAnbmV0JztcblxuaW1wb3J0IHsgcHJvY2Vzc1RoZW1lUmVzb3VyY2VzIH0gZnJvbSAnLi90YXJnZXQvcGx1Z2lucy9hcHBsaWNhdGlvbi10aGVtZS1wbHVnaW4vdGhlbWUtaGFuZGxlLmpzJztcbmltcG9ydCB7IHJld3JpdGVDc3NVcmxzIH0gZnJvbSAnLi90YXJnZXQvcGx1Z2lucy90aGVtZS1sb2FkZXIvdGhlbWUtbG9hZGVyLXV0aWxzLmpzJztcbmltcG9ydCBzZXR0aW5ncyBmcm9tICcuL3RhcmdldC92YWFkaW4tZGV2LXNlcnZlci1zZXR0aW5ncy5qc29uJztcbmltcG9ydCB7XG4gIEFzc2V0SW5mbyxcbiAgQ2h1bmtJbmZvLFxuICBkZWZpbmVDb25maWcsXG4gIG1lcmdlQ29uZmlnLFxuICBPdXRwdXRPcHRpb25zLFxuICBQbHVnaW5PcHRpb24sXG4gIFJlc29sdmVkQ29uZmlnLFxuICBVc2VyQ29uZmlnRm5cbn0gZnJvbSAndml0ZSc7XG5pbXBvcnQgeyBnZXRNYW5pZmVzdCB9IGZyb20gJ3dvcmtib3gtYnVpbGQnO1xuXG5pbXBvcnQgKiBhcyByb2xsdXAgZnJvbSAncm9sbHVwJztcbmltcG9ydCBicm90bGkgZnJvbSAncm9sbHVwLXBsdWdpbi1icm90bGknO1xuaW1wb3J0IHJlcGxhY2UgZnJvbSAnQHJvbGx1cC9wbHVnaW4tcmVwbGFjZSc7XG5pbXBvcnQgY2hlY2tlciBmcm9tICd2aXRlLXBsdWdpbi1jaGVja2VyJztcbmltcG9ydCBwb3N0Y3NzTGl0IGZyb20gJy4vdGFyZ2V0L3BsdWdpbnMvcm9sbHVwLXBsdWdpbi1wb3N0Y3NzLWxpdC1jdXN0b20vcm9sbHVwLXBsdWdpbi1wb3N0Y3NzLWxpdC5qcyc7XG5cbmltcG9ydCB7IGNyZWF0ZVJlcXVpcmUgfSBmcm9tICdtb2R1bGUnO1xuXG5pbXBvcnQgeyB2aXN1YWxpemVyIH0gZnJvbSAncm9sbHVwLXBsdWdpbi12aXN1YWxpemVyJztcblxuLy8gTWFrZSBgcmVxdWlyZWAgY29tcGF0aWJsZSB3aXRoIEVTIG1vZHVsZXNcbmNvbnN0IHJlcXVpcmUgPSBjcmVhdGVSZXF1aXJlKGltcG9ydC5tZXRhLnVybCk7XG5cbmNvbnN0IGFwcFNoZWxsVXJsID0gJy4nO1xuXG5jb25zdCBmcm9udGVuZEZvbGRlciA9IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIHNldHRpbmdzLmZyb250ZW5kRm9sZGVyKTtcbmNvbnN0IHRoZW1lRm9sZGVyID0gcGF0aC5yZXNvbHZlKGZyb250ZW5kRm9sZGVyLCBzZXR0aW5ncy50aGVtZUZvbGRlcik7XG5jb25zdCBmcm9udGVuZEJ1bmRsZUZvbGRlciA9IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIHNldHRpbmdzLmZyb250ZW5kQnVuZGxlT3V0cHV0KTtcbmNvbnN0IGRldkJ1bmRsZUZvbGRlciA9IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIHNldHRpbmdzLmRldkJ1bmRsZU91dHB1dCk7XG5jb25zdCBkZXZCdW5kbGUgPSAhIXByb2Nlc3MuZW52LmRldkJ1bmRsZTtcbmNvbnN0IGphclJlc291cmNlc0ZvbGRlciA9IHBhdGgucmVzb2x2ZShfX2Rpcm5hbWUsIHNldHRpbmdzLmphclJlc291cmNlc0ZvbGRlcik7XG5jb25zdCB0aGVtZVJlc291cmNlRm9sZGVyID0gcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgc2V0dGluZ3MudGhlbWVSZXNvdXJjZUZvbGRlcik7XG5jb25zdCBwcm9qZWN0UGFja2FnZUpzb25GaWxlID0gcGF0aC5yZXNvbHZlKF9fZGlybmFtZSwgJ3BhY2thZ2UuanNvbicpO1xuXG5jb25zdCBidWlsZE91dHB1dEZvbGRlciA9IGRldkJ1bmRsZSA/IGRldkJ1bmRsZUZvbGRlciA6IGZyb250ZW5kQnVuZGxlRm9sZGVyO1xuY29uc3Qgc3RhdHNGb2xkZXIgPSBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCBkZXZCdW5kbGUgPyBzZXR0aW5ncy5kZXZCdW5kbGVTdGF0c091dHB1dCA6IHNldHRpbmdzLnN0YXRzT3V0cHV0KTtcbmNvbnN0IHN0YXRzRmlsZSA9IHBhdGgucmVzb2x2ZShzdGF0c0ZvbGRlciwgJ3N0YXRzLmpzb24nKTtcbmNvbnN0IGJ1bmRsZVNpemVGaWxlID0gcGF0aC5yZXNvbHZlKHN0YXRzRm9sZGVyLCAnYnVuZGxlLXNpemUuaHRtbCcpO1xuY29uc3Qgbm9kZU1vZHVsZXNGb2xkZXIgPSBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnbm9kZV9tb2R1bGVzJyk7XG5jb25zdCB3ZWJDb21wb25lbnRUYWdzID0gJyc7XG5cbmNvbnN0IHByb2plY3RJbmRleEh0bWwgPSBwYXRoLnJlc29sdmUoZnJvbnRlbmRGb2xkZXIsICdpbmRleC5odG1sJyk7XG5cbmNvbnN0IHByb2plY3RTdGF0aWNBc3NldHNGb2xkZXJzID0gW1xuICBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnc3JjJywgJ21haW4nLCAncmVzb3VyY2VzJywgJ01FVEEtSU5GJywgJ3Jlc291cmNlcycpLFxuICBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCAnc3JjJywgJ21haW4nLCAncmVzb3VyY2VzJywgJ3N0YXRpYycpLFxuICBmcm9udGVuZEZvbGRlclxuXTtcblxuLy8gRm9sZGVycyBpbiB0aGUgcHJvamVjdCB3aGljaCBjYW4gY29udGFpbiBhcHBsaWNhdGlvbiB0aGVtZXNcbmNvbnN0IHRoZW1lUHJvamVjdEZvbGRlcnMgPSBwcm9qZWN0U3RhdGljQXNzZXRzRm9sZGVycy5tYXAoKGZvbGRlcikgPT4gcGF0aC5yZXNvbHZlKGZvbGRlciwgc2V0dGluZ3MudGhlbWVGb2xkZXIpKTtcblxuY29uc3QgdGhlbWVPcHRpb25zID0ge1xuICBkZXZNb2RlOiBmYWxzZSxcbiAgdXNlRGV2QnVuZGxlOiBkZXZCdW5kbGUsXG4gIC8vIFRoZSBmb2xsb3dpbmcgbWF0Y2hlcyBmb2xkZXIgJ2Zyb250ZW5kL2dlbmVyYXRlZC90aGVtZXMvJ1xuICAvLyAobm90ICdmcm9udGVuZC90aGVtZXMnKSBmb3IgdGhlbWUgaW4gSkFSIHRoYXQgaXMgY29waWVkIHRoZXJlXG4gIHRoZW1lUmVzb3VyY2VGb2xkZXI6IHBhdGgucmVzb2x2ZSh0aGVtZVJlc291cmNlRm9sZGVyLCBzZXR0aW5ncy50aGVtZUZvbGRlciksXG4gIHRoZW1lUHJvamVjdEZvbGRlcnM6IHRoZW1lUHJvamVjdEZvbGRlcnMsXG4gIHByb2plY3RTdGF0aWNBc3NldHNPdXRwdXRGb2xkZXI6IGRldkJ1bmRsZVxuICAgID8gcGF0aC5yZXNvbHZlKGRldkJ1bmRsZUZvbGRlciwgJy4uL2Fzc2V0cycpXG4gICAgOiBwYXRoLnJlc29sdmUoX19kaXJuYW1lLCBzZXR0aW5ncy5zdGF0aWNPdXRwdXQpLFxuICBmcm9udGVuZEdlbmVyYXRlZEZvbGRlcjogcGF0aC5yZXNvbHZlKGZyb250ZW5kRm9sZGVyLCBzZXR0aW5ncy5nZW5lcmF0ZWRGb2xkZXIpXG59O1xuXG5jb25zdCBoYXNFeHBvcnRlZFdlYkNvbXBvbmVudHMgPSBleGlzdHNTeW5jKHBhdGgucmVzb2x2ZShmcm9udGVuZEZvbGRlciwgJ3dlYi1jb21wb25lbnQuaHRtbCcpKTtcblxuLy8gQmxvY2sgZGVidWcgYW5kIHRyYWNlIGxvZ3MuXG5jb25zb2xlLnRyYWNlID0gKCkgPT4ge307XG5jb25zb2xlLmRlYnVnID0gKCkgPT4ge307XG5cbmZ1bmN0aW9uIGluamVjdE1hbmlmZXN0VG9TV1BsdWdpbigpOiByb2xsdXAuUGx1Z2luIHtcbiAgY29uc3QgcmV3cml0ZU1hbmlmZXN0SW5kZXhIdG1sVXJsID0gKG1hbmlmZXN0KSA9PiB7XG4gICAgY29uc3QgaW5kZXhFbnRyeSA9IG1hbmlmZXN0LmZpbmQoKGVudHJ5KSA9PiBlbnRyeS51cmwgPT09ICdpbmRleC5odG1sJyk7XG4gICAgaWYgKGluZGV4RW50cnkpIHtcbiAgICAgIGluZGV4RW50cnkudXJsID0gYXBwU2hlbGxVcmw7XG4gICAgfVxuXG4gICAgcmV0dXJuIHsgbWFuaWZlc3QsIHdhcm5pbmdzOiBbXSB9O1xuICB9O1xuXG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3ZhYWRpbjppbmplY3QtbWFuaWZlc3QtdG8tc3cnLFxuICAgIGFzeW5jIHRyYW5zZm9ybShjb2RlLCBpZCkge1xuICAgICAgaWYgKC9zd1xcLih0c3xqcykkLy50ZXN0KGlkKSkge1xuICAgICAgICBjb25zdCB7IG1hbmlmZXN0RW50cmllcyB9ID0gYXdhaXQgZ2V0TWFuaWZlc3Qoe1xuICAgICAgICAgIGdsb2JEaXJlY3Rvcnk6IGJ1aWxkT3V0cHV0Rm9sZGVyLFxuICAgICAgICAgIGdsb2JQYXR0ZXJuczogWycqKi8qJ10sXG4gICAgICAgICAgZ2xvYklnbm9yZXM6IFsnKiovKi5iciddLFxuICAgICAgICAgIG1hbmlmZXN0VHJhbnNmb3JtczogW3Jld3JpdGVNYW5pZmVzdEluZGV4SHRtbFVybF0sXG4gICAgICAgICAgbWF4aW11bUZpbGVTaXplVG9DYWNoZUluQnl0ZXM6IDEwMCAqIDEwMjQgKiAxMDI0IC8vIDEwMG1iLFxuICAgICAgICB9KTtcblxuICAgICAgICByZXR1cm4gY29kZS5yZXBsYWNlKCdzZWxmLl9fV0JfTUFOSUZFU1QnLCBKU09OLnN0cmluZ2lmeShtYW5pZmVzdEVudHJpZXMpKTtcbiAgICAgIH1cbiAgICB9XG4gIH07XG59XG5cbmZ1bmN0aW9uIGJ1aWxkU1dQbHVnaW4ob3B0cyk6IFBsdWdpbk9wdGlvbiB7XG4gIGxldCBjb25maWc6IFJlc29sdmVkQ29uZmlnO1xuICBjb25zdCBkZXZNb2RlID0gb3B0cy5kZXZNb2RlO1xuXG4gIGNvbnN0IHN3T2JqID0ge307XG5cbiAgYXN5bmMgZnVuY3Rpb24gYnVpbGQoYWN0aW9uOiAnZ2VuZXJhdGUnIHwgJ3dyaXRlJywgYWRkaXRpb25hbFBsdWdpbnM6IHJvbGx1cC5QbHVnaW5bXSA9IFtdKSB7XG4gICAgY29uc3QgaW5jbHVkZWRQbHVnaW5OYW1lcyA9IFtcbiAgICAgICd2aXRlOmVzYnVpbGQnLFxuICAgICAgJ3JvbGx1cC1wbHVnaW4tZHluYW1pYy1pbXBvcnQtdmFyaWFibGVzJyxcbiAgICAgICd2aXRlOmVzYnVpbGQtdHJhbnNwaWxlJyxcbiAgICAgICd2aXRlOnRlcnNlcidcbiAgICBdO1xuICAgIGNvbnN0IHBsdWdpbnM6IHJvbGx1cC5QbHVnaW5bXSA9IGNvbmZpZy5wbHVnaW5zLmZpbHRlcigocCkgPT4ge1xuICAgICAgcmV0dXJuIGluY2x1ZGVkUGx1Z2luTmFtZXMuaW5jbHVkZXMocC5uYW1lKTtcbiAgICB9KTtcbiAgICBjb25zdCByZXNvbHZlciA9IGNvbmZpZy5jcmVhdGVSZXNvbHZlcigpO1xuICAgIGNvbnN0IHJlc29sdmVQbHVnaW46IHJvbGx1cC5QbHVnaW4gPSB7XG4gICAgICBuYW1lOiAncmVzb2x2ZXInLFxuICAgICAgcmVzb2x2ZUlkKHNvdXJjZSwgaW1wb3J0ZXIsIF9vcHRpb25zKSB7XG4gICAgICAgIHJldHVybiByZXNvbHZlcihzb3VyY2UsIGltcG9ydGVyKTtcbiAgICAgIH1cbiAgICB9O1xuICAgIHBsdWdpbnMudW5zaGlmdChyZXNvbHZlUGx1Z2luKTsgLy8gUHV0IHJlc29sdmUgZmlyc3RcbiAgICBwbHVnaW5zLnB1c2goXG4gICAgICByZXBsYWNlKHtcbiAgICAgICAgdmFsdWVzOiB7XG4gICAgICAgICAgJ3Byb2Nlc3MuZW52Lk5PREVfRU5WJzogSlNPTi5zdHJpbmdpZnkoY29uZmlnLm1vZGUpLFxuICAgICAgICAgIC4uLmNvbmZpZy5kZWZpbmVcbiAgICAgICAgfSxcbiAgICAgICAgcHJldmVudEFzc2lnbm1lbnQ6IHRydWVcbiAgICAgIH0pXG4gICAgKTtcbiAgICBpZiAoYWRkaXRpb25hbFBsdWdpbnMpIHtcbiAgICAgIHBsdWdpbnMucHVzaCguLi5hZGRpdGlvbmFsUGx1Z2lucyk7XG4gICAgfVxuICAgIGNvbnN0IGJ1bmRsZSA9IGF3YWl0IHJvbGx1cC5yb2xsdXAoe1xuICAgICAgaW5wdXQ6IHBhdGgucmVzb2x2ZShzZXR0aW5ncy5jbGllbnRTZXJ2aWNlV29ya2VyU291cmNlKSxcbiAgICAgIHBsdWdpbnNcbiAgICB9KTtcblxuICAgIHRyeSB7XG4gICAgICByZXR1cm4gYXdhaXQgYnVuZGxlW2FjdGlvbl0oe1xuICAgICAgICBmaWxlOiBwYXRoLnJlc29sdmUoYnVpbGRPdXRwdXRGb2xkZXIsICdzdy5qcycpLFxuICAgICAgICBmb3JtYXQ6ICdlcycsXG4gICAgICAgIGV4cG9ydHM6ICdub25lJyxcbiAgICAgICAgc291cmNlbWFwOiBjb25maWcuY29tbWFuZCA9PT0gJ3NlcnZlJyB8fCBjb25maWcuYnVpbGQuc291cmNlbWFwLFxuICAgICAgICBpbmxpbmVEeW5hbWljSW1wb3J0czogdHJ1ZVxuICAgICAgfSk7XG4gICAgfSBmaW5hbGx5IHtcbiAgICAgIGF3YWl0IGJ1bmRsZS5jbG9zZSgpO1xuICAgIH1cbiAgfVxuXG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3ZhYWRpbjpidWlsZC1zdycsXG4gICAgZW5mb3JjZTogJ3Bvc3QnLFxuICAgIGFzeW5jIGNvbmZpZ1Jlc29sdmVkKHJlc29sdmVkQ29uZmlnKSB7XG4gICAgICBjb25maWcgPSByZXNvbHZlZENvbmZpZztcbiAgICB9LFxuICAgIGFzeW5jIGJ1aWxkU3RhcnQoKSB7XG4gICAgICBpZiAoZGV2TW9kZSkge1xuICAgICAgICBjb25zdCB7IG91dHB1dCB9ID0gYXdhaXQgYnVpbGQoJ2dlbmVyYXRlJyk7XG4gICAgICAgIHN3T2JqLmNvZGUgPSBvdXRwdXRbMF0uY29kZTtcbiAgICAgICAgc3dPYmoubWFwID0gb3V0cHV0WzBdLm1hcDtcbiAgICAgIH1cbiAgICB9LFxuICAgIGFzeW5jIGxvYWQoaWQpIHtcbiAgICAgIGlmIChpZC5lbmRzV2l0aCgnc3cuanMnKSkge1xuICAgICAgICByZXR1cm4gJyc7XG4gICAgICB9XG4gICAgfSxcbiAgICBhc3luYyB0cmFuc2Zvcm0oX2NvZGUsIGlkKSB7XG4gICAgICBpZiAoaWQuZW5kc1dpdGgoJ3N3LmpzJykpIHtcbiAgICAgICAgcmV0dXJuIHN3T2JqO1xuICAgICAgfVxuICAgIH0sXG4gICAgYXN5bmMgY2xvc2VCdW5kbGUoKSB7XG4gICAgICBpZiAoIWRldk1vZGUpIHtcbiAgICAgICAgYXdhaXQgYnVpbGQoJ3dyaXRlJywgW2luamVjdE1hbmlmZXN0VG9TV1BsdWdpbigpLCBicm90bGkoKV0pO1xuICAgICAgfVxuICAgIH1cbiAgfTtcbn1cblxuZnVuY3Rpb24gc3RhdHNFeHRyYWN0ZXJQbHVnaW4oKTogUGx1Z2luT3B0aW9uIHtcbiAgZnVuY3Rpb24gY29sbGVjdFRoZW1lSnNvbnNJbkZyb250ZW5kKHRoZW1lSnNvbkNvbnRlbnRzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+LCB0aGVtZU5hbWU6IHN0cmluZykge1xuICAgIGNvbnN0IHRoZW1lSnNvbiA9IHBhdGgucmVzb2x2ZShmcm9udGVuZEZvbGRlciwgc2V0dGluZ3MudGhlbWVGb2xkZXIsIHRoZW1lTmFtZSwgJ3RoZW1lLmpzb24nKTtcbiAgICBpZiAoZXhpc3RzU3luYyh0aGVtZUpzb24pKSB7XG4gICAgICBjb25zdCB0aGVtZUpzb25Db250ZW50ID0gcmVhZEZpbGVTeW5jKHRoZW1lSnNvbiwgeyBlbmNvZGluZzogJ3V0Zi04JyB9KS5yZXBsYWNlKC9cXHJcXG4vZywgJ1xcbicpO1xuICAgICAgdGhlbWVKc29uQ29udGVudHNbdGhlbWVOYW1lXSA9IHRoZW1lSnNvbkNvbnRlbnQ7XG4gICAgICBjb25zdCB0aGVtZUpzb25PYmplY3QgPSBKU09OLnBhcnNlKHRoZW1lSnNvbkNvbnRlbnQpO1xuICAgICAgaWYgKHRoZW1lSnNvbk9iamVjdC5wYXJlbnQpIHtcbiAgICAgICAgY29sbGVjdFRoZW1lSnNvbnNJbkZyb250ZW5kKHRoZW1lSnNvbkNvbnRlbnRzLCB0aGVtZUpzb25PYmplY3QucGFyZW50KTtcbiAgICAgIH1cbiAgICB9XG4gIH1cblxuICByZXR1cm4ge1xuICAgIG5hbWU6ICd2YWFkaW46c3RhdHMnLFxuICAgIGVuZm9yY2U6ICdwb3N0JyxcbiAgICBhc3luYyB3cml0ZUJ1bmRsZShvcHRpb25zOiBPdXRwdXRPcHRpb25zLCBidW5kbGU6IHsgW2ZpbGVOYW1lOiBzdHJpbmddOiBBc3NldEluZm8gfCBDaHVua0luZm8gfSkge1xuICAgICAgY29uc3QgbW9kdWxlcyA9IE9iamVjdC52YWx1ZXMoYnVuZGxlKS5mbGF0TWFwKChiKSA9PiAoYi5tb2R1bGVzID8gT2JqZWN0LmtleXMoYi5tb2R1bGVzKSA6IFtdKSk7XG4gICAgICBjb25zdCBub2RlTW9kdWxlc0ZvbGRlcnMgPSBtb2R1bGVzXG4gICAgICAgIC5tYXAoKGlkKSA9PiBpZC5yZXBsYWNlKC9cXFxcL2csICcvJykpXG4gICAgICAgIC5maWx0ZXIoKGlkKSA9PiBpZC5zdGFydHNXaXRoKG5vZGVNb2R1bGVzRm9sZGVyLnJlcGxhY2UoL1xcXFwvZywgJy8nKSkpXG4gICAgICAgIC5tYXAoKGlkKSA9PiBpZC5zdWJzdHJpbmcobm9kZU1vZHVsZXNGb2xkZXIubGVuZ3RoICsgMSkpO1xuICAgICAgY29uc3QgbnBtTW9kdWxlcyA9IG5vZGVNb2R1bGVzRm9sZGVyc1xuICAgICAgICAubWFwKChpZCkgPT4gaWQucmVwbGFjZSgvXFxcXC9nLCAnLycpKVxuICAgICAgICAubWFwKChpZCkgPT4ge1xuICAgICAgICAgIGNvbnN0IHBhcnRzID0gaWQuc3BsaXQoJy8nKTtcbiAgICAgICAgICBpZiAoaWQuc3RhcnRzV2l0aCgnQCcpKSB7XG4gICAgICAgICAgICByZXR1cm4gcGFydHNbMF0gKyAnLycgKyBwYXJ0c1sxXTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgcmV0dXJuIHBhcnRzWzBdO1xuICAgICAgICAgIH1cbiAgICAgICAgfSlcbiAgICAgICAgLnNvcnQoKVxuICAgICAgICAuZmlsdGVyKCh2YWx1ZSwgaW5kZXgsIHNlbGYpID0+IHNlbGYuaW5kZXhPZih2YWx1ZSkgPT09IGluZGV4KTtcbiAgICAgIGNvbnN0IG5wbU1vZHVsZUFuZFZlcnNpb24gPSBPYmplY3QuZnJvbUVudHJpZXMobnBtTW9kdWxlcy5tYXAoKG1vZHVsZSkgPT4gW21vZHVsZSwgZ2V0VmVyc2lvbihtb2R1bGUpXSkpO1xuICAgICAgY29uc3QgY3ZkbHMgPSBPYmplY3QuZnJvbUVudHJpZXMoXG4gICAgICAgIG5wbU1vZHVsZXNcbiAgICAgICAgICAuZmlsdGVyKChtb2R1bGUpID0+IGdldEN2ZGxOYW1lKG1vZHVsZSkgIT0gbnVsbClcbiAgICAgICAgICAubWFwKChtb2R1bGUpID0+IFttb2R1bGUsIHsgbmFtZTogZ2V0Q3ZkbE5hbWUobW9kdWxlKSwgdmVyc2lvbjogZ2V0VmVyc2lvbihtb2R1bGUpIH1dKVxuICAgICAgKTtcblxuICAgICAgbWtkaXJTeW5jKHBhdGguZGlybmFtZShzdGF0c0ZpbGUpLCB7IHJlY3Vyc2l2ZTogdHJ1ZSB9KTtcbiAgICAgIGNvbnN0IHByb2plY3RQYWNrYWdlSnNvbiA9IEpTT04ucGFyc2UocmVhZEZpbGVTeW5jKHByb2plY3RQYWNrYWdlSnNvbkZpbGUsIHsgZW5jb2Rpbmc6ICd1dGYtOCcgfSkpO1xuXG4gICAgICBjb25zdCBlbnRyeVNjcmlwdHMgPSBPYmplY3QudmFsdWVzKGJ1bmRsZSlcbiAgICAgICAgLmZpbHRlcigoYnVuZGxlKSA9PiBidW5kbGUuaXNFbnRyeSlcbiAgICAgICAgLm1hcCgoYnVuZGxlKSA9PiBidW5kbGUuZmlsZU5hbWUpO1xuXG4gICAgICBjb25zdCBnZW5lcmF0ZWRJbmRleEh0bWwgPSBwYXRoLnJlc29sdmUoYnVpbGRPdXRwdXRGb2xkZXIsICdpbmRleC5odG1sJyk7XG4gICAgICBjb25zdCBjdXN0b21JbmRleERhdGE6IHN0cmluZyA9IHJlYWRGaWxlU3luYyhwcm9qZWN0SW5kZXhIdG1sLCB7IGVuY29kaW5nOiAndXRmLTgnIH0pO1xuICAgICAgY29uc3QgZ2VuZXJhdGVkSW5kZXhEYXRhOiBzdHJpbmcgPSByZWFkRmlsZVN5bmMoZ2VuZXJhdGVkSW5kZXhIdG1sLCB7XG4gICAgICAgIGVuY29kaW5nOiAndXRmLTgnXG4gICAgICB9KTtcblxuICAgICAgY29uc3QgY3VzdG9tSW5kZXhSb3dzID0gbmV3IFNldChjdXN0b21JbmRleERhdGEuc3BsaXQoL1tcXHJcXG5dLykuZmlsdGVyKChyb3cpID0+IHJvdy50cmltKCkgIT09ICcnKSk7XG4gICAgICBjb25zdCBnZW5lcmF0ZWRJbmRleFJvd3MgPSBnZW5lcmF0ZWRJbmRleERhdGEuc3BsaXQoL1tcXHJcXG5dLykuZmlsdGVyKChyb3cpID0+IHJvdy50cmltKCkgIT09ICcnKTtcblxuICAgICAgY29uc3Qgcm93c0dlbmVyYXRlZDogc3RyaW5nW10gPSBbXTtcbiAgICAgIGdlbmVyYXRlZEluZGV4Um93cy5mb3JFYWNoKChyb3cpID0+IHtcbiAgICAgICAgaWYgKCFjdXN0b21JbmRleFJvd3MuaGFzKHJvdykpIHtcbiAgICAgICAgICByb3dzR2VuZXJhdGVkLnB1c2gocm93KTtcbiAgICAgICAgfVxuICAgICAgfSk7XG5cbiAgICAgIC8vQWZ0ZXIgZGV2LWJ1bmRsZSBidWlsZCBhZGQgdXNlZCBGbG93IGZyb250ZW5kIGltcG9ydHMgSnNNb2R1bGUvSmF2YVNjcmlwdC9Dc3NJbXBvcnRcblxuICAgICAgY29uc3QgcGFyc2VJbXBvcnRzID0gKGZpbGVuYW1lOiBzdHJpbmcsIHJlc3VsdDogU2V0PHN0cmluZz4pOiB2b2lkID0+IHtcbiAgICAgICAgY29uc3QgY29udGVudDogc3RyaW5nID0gcmVhZEZpbGVTeW5jKGZpbGVuYW1lLCB7IGVuY29kaW5nOiAndXRmLTgnIH0pO1xuICAgICAgICBjb25zdCBsaW5lcyA9IGNvbnRlbnQuc3BsaXQoJ1xcbicpO1xuICAgICAgICBjb25zdCBzdGF0aWNJbXBvcnRzID0gbGluZXNcbiAgICAgICAgICAuZmlsdGVyKChsaW5lKSA9PiBsaW5lLnN0YXJ0c1dpdGgoJ2ltcG9ydCAnKSlcbiAgICAgICAgICAubWFwKChsaW5lKSA9PiBsaW5lLnN1YnN0cmluZyhsaW5lLmluZGV4T2YoXCInXCIpICsgMSwgbGluZS5sYXN0SW5kZXhPZihcIidcIikpKVxuICAgICAgICAgIC5tYXAoKGxpbmUpID0+IChsaW5lLmluY2x1ZGVzKCc/JykgPyBsaW5lLnN1YnN0cmluZygwLCBsaW5lLmxhc3RJbmRleE9mKCc/JykpIDogbGluZSkpO1xuICAgICAgICBjb25zdCBkeW5hbWljSW1wb3J0cyA9IGxpbmVzXG4gICAgICAgICAgLmZpbHRlcigobGluZSkgPT4gbGluZS5pbmNsdWRlcygnaW1wb3J0KCcpKVxuICAgICAgICAgIC5tYXAoKGxpbmUpID0+IGxpbmUucmVwbGFjZSgvLippbXBvcnRcXCgvLCAnJykpXG4gICAgICAgICAgLm1hcCgobGluZSkgPT4gbGluZS5zcGxpdCgvJy8pWzFdKVxuICAgICAgICAgIC5tYXAoKGxpbmUpID0+IChsaW5lLmluY2x1ZGVzKCc/JykgPyBsaW5lLnN1YnN0cmluZygwLCBsaW5lLmxhc3RJbmRleE9mKCc/JykpIDogbGluZSkpO1xuXG4gICAgICAgIHN0YXRpY0ltcG9ydHMuZm9yRWFjaCgoc3RhdGljSW1wb3J0KSA9PiByZXN1bHQuYWRkKHN0YXRpY0ltcG9ydCkpO1xuXG4gICAgICAgIGR5bmFtaWNJbXBvcnRzLm1hcCgoZHluYW1pY0ltcG9ydCkgPT4ge1xuICAgICAgICAgIGNvbnN0IGltcG9ydGVkRmlsZSA9IHBhdGgucmVzb2x2ZShwYXRoLmRpcm5hbWUoZmlsZW5hbWUpLCBkeW5hbWljSW1wb3J0KTtcbiAgICAgICAgICBwYXJzZUltcG9ydHMoaW1wb3J0ZWRGaWxlLCByZXN1bHQpO1xuICAgICAgICB9KTtcbiAgICAgIH07XG5cbiAgICAgIGNvbnN0IGdlbmVyYXRlZEltcG9ydHNTZXQgPSBuZXcgU2V0PHN0cmluZz4oKTtcbiAgICAgIHBhcnNlSW1wb3J0cyhcbiAgICAgICAgcGF0aC5yZXNvbHZlKHRoZW1lT3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlciwgJ2Zsb3cnLCAnZ2VuZXJhdGVkLWZsb3ctaW1wb3J0cy5qcycpLFxuICAgICAgICBnZW5lcmF0ZWRJbXBvcnRzU2V0XG4gICAgICApO1xuICAgICAgY29uc3QgZ2VuZXJhdGVkSW1wb3J0cyA9IEFycmF5LmZyb20oZ2VuZXJhdGVkSW1wb3J0c1NldCkuc29ydCgpO1xuXG4gICAgICBjb25zdCBmcm9udGVuZEZpbGVzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge307XG5cbiAgICAgIGNvbnN0IHByb2plY3RGaWxlRXh0ZW5zaW9ucyA9IFsnLmpzJywgJy5qcy5tYXAnLCAnLnRzJywgJy50cy5tYXAnLCAnLnRzeCcsICcudHN4Lm1hcCcsICcuY3NzJywgJy5jc3MubWFwJ107XG5cbiAgICAgIGNvbnN0IGlzVGhlbWVDb21wb25lbnRzUmVzb3VyY2UgPSAoaWQ6IHN0cmluZykgPT5cbiAgICAgICAgICBpZC5zdGFydHNXaXRoKHRoZW1lT3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlci5yZXBsYWNlKC9cXFxcL2csICcvJykpXG4gICAgICAgICAgICAgICYmIGlkLm1hdGNoKC8uKlxcL2phci1yZXNvdXJjZXNcXC90aGVtZXNcXC9bXlxcL10rXFwvY29tcG9uZW50c1xcLy8pO1xuXG4gICAgICAvLyBjb2xsZWN0cyBwcm9qZWN0J3MgZnJvbnRlbmQgcmVzb3VyY2VzIGluIGZyb250ZW5kIGZvbGRlciwgZXhjbHVkaW5nXG4gICAgICAvLyAnZ2VuZXJhdGVkJyBzdWItZm9sZGVyLCBleGNlcHQgZm9yIGxlZ2FjeSBzaGFkb3cgRE9NIHN0eWxlc2hlZXRzXG4gICAgICAvLyBwYWNrYWdlZCBpbiBgdGhlbWUvY29tcG9uZW50cy9gIGZvbGRlci5cbiAgICAgIG1vZHVsZXNcbiAgICAgICAgLm1hcCgoaWQpID0+IGlkLnJlcGxhY2UoL1xcXFwvZywgJy8nKSlcbiAgICAgICAgLmZpbHRlcigoaWQpID0+IGlkLnN0YXJ0c1dpdGgoZnJvbnRlbmRGb2xkZXIucmVwbGFjZSgvXFxcXC9nLCAnLycpKSlcbiAgICAgICAgLmZpbHRlcigoaWQpID0+ICFpZC5zdGFydHNXaXRoKHRoZW1lT3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlci5yZXBsYWNlKC9cXFxcL2csICcvJykpIHx8IGlzVGhlbWVDb21wb25lbnRzUmVzb3VyY2UoaWQpKVxuICAgICAgICAubWFwKChpZCkgPT4gaWQuc3Vic3RyaW5nKGZyb250ZW5kRm9sZGVyLmxlbmd0aCArIDEpKVxuICAgICAgICAubWFwKChsaW5lOiBzdHJpbmcpID0+IChsaW5lLmluY2x1ZGVzKCc/JykgPyBsaW5lLnN1YnN0cmluZygwLCBsaW5lLmxhc3RJbmRleE9mKCc/JykpIDogbGluZSkpXG4gICAgICAgIC5mb3JFYWNoKChsaW5lOiBzdHJpbmcpID0+IHtcbiAgICAgICAgICAvLyBcXHJcXG4gZnJvbSB3aW5kb3dzIG1hZGUgZmlsZXMgbWF5IGJlIHVzZWQgc28gY2hhbmdlIHRvIFxcblxuICAgICAgICAgIGNvbnN0IGZpbGVQYXRoID0gcGF0aC5yZXNvbHZlKGZyb250ZW5kRm9sZGVyLCBsaW5lKTtcbiAgICAgICAgICBpZiAocHJvamVjdEZpbGVFeHRlbnNpb25zLmluY2x1ZGVzKHBhdGguZXh0bmFtZShmaWxlUGF0aCkpKSB7XG4gICAgICAgICAgICBjb25zdCBmaWxlQnVmZmVyID0gcmVhZEZpbGVTeW5jKGZpbGVQYXRoLCB7IGVuY29kaW5nOiAndXRmLTgnIH0pLnJlcGxhY2UoL1xcclxcbi9nLCAnXFxuJyk7XG4gICAgICAgICAgICBmcm9udGVuZEZpbGVzW2xpbmVdID0gY3JlYXRlSGFzaCgnc2hhMjU2JykudXBkYXRlKGZpbGVCdWZmZXIsICd1dGY4JykuZGlnZXN0KCdoZXgnKTtcbiAgICAgICAgICB9XG4gICAgICAgIH0pO1xuXG4gICAgICAvLyBjb2xsZWN0cyBmcm9udGVuZCByZXNvdXJjZXMgZnJvbSB0aGUgSkFSc1xuICAgICAgZ2VuZXJhdGVkSW1wb3J0c1xuICAgICAgICAuZmlsdGVyKChsaW5lOiBzdHJpbmcpID0+IGxpbmUuaW5jbHVkZXMoJ2dlbmVyYXRlZC9qYXItcmVzb3VyY2VzJykpXG4gICAgICAgIC5mb3JFYWNoKChsaW5lOiBzdHJpbmcpID0+IHtcbiAgICAgICAgICBsZXQgZmlsZW5hbWUgPSBsaW5lLnN1YnN0cmluZyhsaW5lLmluZGV4T2YoJ2dlbmVyYXRlZCcpKTtcbiAgICAgICAgICAvLyBcXHJcXG4gZnJvbSB3aW5kb3dzIG1hZGUgZmlsZXMgbWF5IGJlIHVzZWQgcm8gcmVtb3ZlIHRvIGJlIG9ubHkgXFxuXG4gICAgICAgICAgY29uc3QgZmlsZUJ1ZmZlciA9IHJlYWRGaWxlU3luYyhwYXRoLnJlc29sdmUoZnJvbnRlbmRGb2xkZXIsIGZpbGVuYW1lKSwgeyBlbmNvZGluZzogJ3V0Zi04JyB9KS5yZXBsYWNlKFxuICAgICAgICAgICAgL1xcclxcbi9nLFxuICAgICAgICAgICAgJ1xcbidcbiAgICAgICAgICApO1xuICAgICAgICAgIGNvbnN0IGhhc2ggPSBjcmVhdGVIYXNoKCdzaGEyNTYnKS51cGRhdGUoZmlsZUJ1ZmZlciwgJ3V0ZjgnKS5kaWdlc3QoJ2hleCcpO1xuXG4gICAgICAgICAgY29uc3QgZmlsZUtleSA9IGxpbmUuc3Vic3RyaW5nKGxpbmUuaW5kZXhPZignamFyLXJlc291cmNlcy8nKSArIDE0KTtcbiAgICAgICAgICBmcm9udGVuZEZpbGVzW2ZpbGVLZXldID0gaGFzaDtcbiAgICAgICAgfSk7XG4gICAgICAvLyBJZiBhIGluZGV4LnRzIGV4aXN0cyBoYXNoIGl0IHRvIGJlIGFibGUgdG8gc2VlIGlmIGl0IGNoYW5nZXMuXG4gICAgICBpZiAoZXhpc3RzU3luYyhwYXRoLnJlc29sdmUoZnJvbnRlbmRGb2xkZXIsICdpbmRleC50cycpKSkge1xuICAgICAgICBjb25zdCBmaWxlQnVmZmVyID0gcmVhZEZpbGVTeW5jKHBhdGgucmVzb2x2ZShmcm9udGVuZEZvbGRlciwgJ2luZGV4LnRzJyksIHsgZW5jb2Rpbmc6ICd1dGYtOCcgfSkucmVwbGFjZShcbiAgICAgICAgICAvXFxyXFxuL2csXG4gICAgICAgICAgJ1xcbidcbiAgICAgICAgKTtcbiAgICAgICAgZnJvbnRlbmRGaWxlc1tgaW5kZXgudHNgXSA9IGNyZWF0ZUhhc2goJ3NoYTI1NicpLnVwZGF0ZShmaWxlQnVmZmVyLCAndXRmOCcpLmRpZ2VzdCgnaGV4Jyk7XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IHRoZW1lSnNvbkNvbnRlbnRzOiBSZWNvcmQ8c3RyaW5nLCBzdHJpbmc+ID0ge307XG4gICAgICBjb25zdCB0aGVtZXNGb2xkZXIgPSBwYXRoLnJlc29sdmUoamFyUmVzb3VyY2VzRm9sZGVyLCAndGhlbWVzJyk7XG4gICAgICBpZiAoZXhpc3RzU3luYyh0aGVtZXNGb2xkZXIpKSB7XG4gICAgICAgIHJlYWRkaXJTeW5jKHRoZW1lc0ZvbGRlcikuZm9yRWFjaCgodGhlbWVGb2xkZXIpID0+IHtcbiAgICAgICAgICBjb25zdCB0aGVtZUpzb24gPSBwYXRoLnJlc29sdmUodGhlbWVzRm9sZGVyLCB0aGVtZUZvbGRlciwgJ3RoZW1lLmpzb24nKTtcbiAgICAgICAgICBpZiAoZXhpc3RzU3luYyh0aGVtZUpzb24pKSB7XG4gICAgICAgICAgICB0aGVtZUpzb25Db250ZW50c1twYXRoLmJhc2VuYW1lKHRoZW1lRm9sZGVyKV0gPSByZWFkRmlsZVN5bmModGhlbWVKc29uLCB7IGVuY29kaW5nOiAndXRmLTgnIH0pLnJlcGxhY2UoXG4gICAgICAgICAgICAgIC9cXHJcXG4vZyxcbiAgICAgICAgICAgICAgJ1xcbidcbiAgICAgICAgICAgICk7XG4gICAgICAgICAgfVxuICAgICAgICB9KTtcbiAgICAgIH1cblxuICAgICAgY29sbGVjdFRoZW1lSnNvbnNJbkZyb250ZW5kKHRoZW1lSnNvbkNvbnRlbnRzLCBzZXR0aW5ncy50aGVtZU5hbWUpO1xuXG4gICAgICBsZXQgd2ViQ29tcG9uZW50czogc3RyaW5nW10gPSBbXTtcbiAgICAgIGlmICh3ZWJDb21wb25lbnRUYWdzKSB7XG4gICAgICAgIHdlYkNvbXBvbmVudHMgPSB3ZWJDb21wb25lbnRUYWdzLnNwbGl0KCc7Jyk7XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IHN0YXRzID0ge1xuICAgICAgICBwYWNrYWdlSnNvbkRlcGVuZGVuY2llczogcHJvamVjdFBhY2thZ2VKc29uLmRlcGVuZGVuY2llcyxcbiAgICAgICAgbnBtTW9kdWxlczogbnBtTW9kdWxlQW5kVmVyc2lvbixcbiAgICAgICAgYnVuZGxlSW1wb3J0czogZ2VuZXJhdGVkSW1wb3J0cyxcbiAgICAgICAgZnJvbnRlbmRIYXNoZXM6IGZyb250ZW5kRmlsZXMsXG4gICAgICAgIHRoZW1lSnNvbkNvbnRlbnRzOiB0aGVtZUpzb25Db250ZW50cyxcbiAgICAgICAgZW50cnlTY3JpcHRzLFxuICAgICAgICB3ZWJDb21wb25lbnRzLFxuICAgICAgICBjdmRsTW9kdWxlczogY3ZkbHMsXG4gICAgICAgIHBhY2thZ2VKc29uSGFzaDogcHJvamVjdFBhY2thZ2VKc29uPy52YWFkaW4/Lmhhc2gsXG4gICAgICAgIGluZGV4SHRtbEdlbmVyYXRlZDogcm93c0dlbmVyYXRlZFxuICAgICAgfTtcbiAgICAgIHdyaXRlRmlsZVN5bmMoc3RhdHNGaWxlLCBKU09OLnN0cmluZ2lmeShzdGF0cywgbnVsbCwgMSkpO1xuICAgIH1cbiAgfTtcbn1cbmZ1bmN0aW9uIHZhYWRpbkJ1bmRsZXNQbHVnaW4oKTogUGx1Z2luT3B0aW9uIHtcbiAgdHlwZSBFeHBvcnRJbmZvID1cbiAgICB8IHN0cmluZ1xuICAgIHwge1xuICAgICAgICBuYW1lc3BhY2U/OiBzdHJpbmc7XG4gICAgICAgIHNvdXJjZTogc3RyaW5nO1xuICAgICAgfTtcblxuICB0eXBlIEV4cG9zZUluZm8gPSB7XG4gICAgZXhwb3J0czogRXhwb3J0SW5mb1tdO1xuICB9O1xuXG4gIHR5cGUgUGFja2FnZUluZm8gPSB7XG4gICAgdmVyc2lvbjogc3RyaW5nO1xuICAgIGV4cG9zZXM6IFJlY29yZDxzdHJpbmcsIEV4cG9zZUluZm8+O1xuICB9O1xuXG4gIHR5cGUgQnVuZGxlSnNvbiA9IHtcbiAgICBwYWNrYWdlczogUmVjb3JkPHN0cmluZywgUGFja2FnZUluZm8+O1xuICB9O1xuXG4gIGNvbnN0IGRpc2FibGVkTWVzc2FnZSA9ICdWYWFkaW4gY29tcG9uZW50IGRlcGVuZGVuY3kgYnVuZGxlcyBhcmUgZGlzYWJsZWQuJztcblxuICBjb25zdCBtb2R1bGVzRGlyZWN0b3J5ID0gbm9kZU1vZHVsZXNGb2xkZXIucmVwbGFjZSgvXFxcXC9nLCAnLycpO1xuXG4gIGxldCB2YWFkaW5CdW5kbGVKc29uOiBCdW5kbGVKc29uO1xuXG4gIGZ1bmN0aW9uIHBhcnNlTW9kdWxlSWQoaWQ6IHN0cmluZyk6IHsgcGFja2FnZU5hbWU6IHN0cmluZzsgbW9kdWxlUGF0aDogc3RyaW5nIH0ge1xuICAgIGNvbnN0IFtzY29wZSwgc2NvcGVkUGFja2FnZU5hbWVdID0gaWQuc3BsaXQoJy8nLCAzKTtcbiAgICBjb25zdCBwYWNrYWdlTmFtZSA9IHNjb3BlLnN0YXJ0c1dpdGgoJ0AnKSA/IGAke3Njb3BlfS8ke3Njb3BlZFBhY2thZ2VOYW1lfWAgOiBzY29wZTtcbiAgICBjb25zdCBtb2R1bGVQYXRoID0gYC4ke2lkLnN1YnN0cmluZyhwYWNrYWdlTmFtZS5sZW5ndGgpfWA7XG4gICAgcmV0dXJuIHtcbiAgICAgIHBhY2thZ2VOYW1lLFxuICAgICAgbW9kdWxlUGF0aFxuICAgIH07XG4gIH1cblxuICBmdW5jdGlvbiBnZXRFeHBvcnRzKGlkOiBzdHJpbmcpOiBzdHJpbmdbXSB8IHVuZGVmaW5lZCB7XG4gICAgY29uc3QgeyBwYWNrYWdlTmFtZSwgbW9kdWxlUGF0aCB9ID0gcGFyc2VNb2R1bGVJZChpZCk7XG4gICAgY29uc3QgcGFja2FnZUluZm8gPSB2YWFkaW5CdW5kbGVKc29uLnBhY2thZ2VzW3BhY2thZ2VOYW1lXTtcblxuICAgIGlmICghcGFja2FnZUluZm8pIHJldHVybjtcblxuICAgIGNvbnN0IGV4cG9zZUluZm86IEV4cG9zZUluZm8gPSBwYWNrYWdlSW5mby5leHBvc2VzW21vZHVsZVBhdGhdO1xuICAgIGlmICghZXhwb3NlSW5mbykgcmV0dXJuO1xuXG4gICAgY29uc3QgZXhwb3J0c1NldCA9IG5ldyBTZXQ8c3RyaW5nPigpO1xuICAgIGZvciAoY29uc3QgZSBvZiBleHBvc2VJbmZvLmV4cG9ydHMpIHtcbiAgICAgIGlmICh0eXBlb2YgZSA9PT0gJ3N0cmluZycpIHtcbiAgICAgICAgZXhwb3J0c1NldC5hZGQoZSk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBjb25zdCB7IG5hbWVzcGFjZSwgc291cmNlIH0gPSBlO1xuICAgICAgICBpZiAobmFtZXNwYWNlKSB7XG4gICAgICAgICAgZXhwb3J0c1NldC5hZGQobmFtZXNwYWNlKTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICBjb25zdCBzb3VyY2VFeHBvcnRzID0gZ2V0RXhwb3J0cyhzb3VyY2UpO1xuICAgICAgICAgIGlmIChzb3VyY2VFeHBvcnRzKSB7XG4gICAgICAgICAgICBzb3VyY2VFeHBvcnRzLmZvckVhY2goKGUpID0+IGV4cG9ydHNTZXQuYWRkKGUpKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIEFycmF5LmZyb20oZXhwb3J0c1NldCk7XG4gIH1cblxuICBmdW5jdGlvbiBnZXRFeHBvcnRCaW5kaW5nKGJpbmRpbmc6IHN0cmluZykge1xuICAgIHJldHVybiBiaW5kaW5nID09PSAnZGVmYXVsdCcgPyAnX2RlZmF1bHQgYXMgZGVmYXVsdCcgOiBiaW5kaW5nO1xuICB9XG5cbiAgZnVuY3Rpb24gZ2V0SW1wb3J0QXNzaWdtZW50KGJpbmRpbmc6IHN0cmluZykge1xuICAgIHJldHVybiBiaW5kaW5nID09PSAnZGVmYXVsdCcgPyAnZGVmYXVsdDogX2RlZmF1bHQnIDogYmluZGluZztcbiAgfVxuXG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3ZhYWRpbjpidW5kbGVzJyxcbiAgICBlbmZvcmNlOiAncHJlJyxcbiAgICBhcHBseShjb25maWcsIHsgY29tbWFuZCB9KSB7XG4gICAgICBpZiAoY29tbWFuZCAhPT0gJ3NlcnZlJykgcmV0dXJuIGZhbHNlO1xuXG4gICAgICB0cnkge1xuICAgICAgICBjb25zdCB2YWFkaW5CdW5kbGVKc29uUGF0aCA9IHJlcXVpcmUucmVzb2x2ZSgnQHZhYWRpbi9idW5kbGVzL3ZhYWRpbi1idW5kbGUuanNvbicpO1xuICAgICAgICB2YWFkaW5CdW5kbGVKc29uID0gSlNPTi5wYXJzZShyZWFkRmlsZVN5bmModmFhZGluQnVuZGxlSnNvblBhdGgsIHsgZW5jb2Rpbmc6ICd1dGY4JyB9KSk7XG4gICAgICB9IGNhdGNoIChlOiB1bmtub3duKSB7XG4gICAgICAgIGlmICh0eXBlb2YgZSA9PT0gJ29iamVjdCcgJiYgKGUgYXMgeyBjb2RlOiBzdHJpbmcgfSkuY29kZSA9PT0gJ01PRFVMRV9OT1RfRk9VTkQnKSB7XG4gICAgICAgICAgdmFhZGluQnVuZGxlSnNvbiA9IHsgcGFja2FnZXM6IHt9IH07XG4gICAgICAgICAgY29uc29sZS5pbmZvKGBAdmFhZGluL2J1bmRsZXMgbnBtIHBhY2thZ2UgaXMgbm90IGZvdW5kLCAke2Rpc2FibGVkTWVzc2FnZX1gKTtcbiAgICAgICAgICByZXR1cm4gZmFsc2U7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgdGhyb3cgZTtcbiAgICAgICAgfVxuICAgICAgfVxuXG4gICAgICBjb25zdCB2ZXJzaW9uTWlzbWF0Y2hlczogQXJyYXk8eyBuYW1lOiBzdHJpbmc7IGJ1bmRsZWRWZXJzaW9uOiBzdHJpbmc7IGluc3RhbGxlZFZlcnNpb246IHN0cmluZyB9PiA9IFtdO1xuICAgICAgZm9yIChjb25zdCBbbmFtZSwgcGFja2FnZUluZm9dIG9mIE9iamVjdC5lbnRyaWVzKHZhYWRpbkJ1bmRsZUpzb24ucGFja2FnZXMpKSB7XG4gICAgICAgIGxldCBpbnN0YWxsZWRWZXJzaW9uOiBzdHJpbmcgfCB1bmRlZmluZWQgPSB1bmRlZmluZWQ7XG4gICAgICAgIHRyeSB7XG4gICAgICAgICAgY29uc3QgeyB2ZXJzaW9uOiBidW5kbGVkVmVyc2lvbiB9ID0gcGFja2FnZUluZm87XG4gICAgICAgICAgY29uc3QgaW5zdGFsbGVkUGFja2FnZUpzb25GaWxlID0gcGF0aC5yZXNvbHZlKG1vZHVsZXNEaXJlY3RvcnksIG5hbWUsICdwYWNrYWdlLmpzb24nKTtcbiAgICAgICAgICBjb25zdCBwYWNrYWdlSnNvbiA9IEpTT04ucGFyc2UocmVhZEZpbGVTeW5jKGluc3RhbGxlZFBhY2thZ2VKc29uRmlsZSwgeyBlbmNvZGluZzogJ3V0ZjgnIH0pKTtcbiAgICAgICAgICBpbnN0YWxsZWRWZXJzaW9uID0gcGFja2FnZUpzb24udmVyc2lvbjtcbiAgICAgICAgICBpZiAoaW5zdGFsbGVkVmVyc2lvbiAmJiBpbnN0YWxsZWRWZXJzaW9uICE9PSBidW5kbGVkVmVyc2lvbikge1xuICAgICAgICAgICAgdmVyc2lvbk1pc21hdGNoZXMucHVzaCh7XG4gICAgICAgICAgICAgIG5hbWUsXG4gICAgICAgICAgICAgIGJ1bmRsZWRWZXJzaW9uLFxuICAgICAgICAgICAgICBpbnN0YWxsZWRWZXJzaW9uXG4gICAgICAgICAgICB9KTtcbiAgICAgICAgICB9XG4gICAgICAgIH0gY2F0Y2ggKF8pIHtcbiAgICAgICAgICAvLyBpZ25vcmUgcGFja2FnZSBub3QgZm91bmRcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgaWYgKHZlcnNpb25NaXNtYXRjaGVzLmxlbmd0aCkge1xuICAgICAgICBjb25zb2xlLmluZm8oYEB2YWFkaW4vYnVuZGxlcyBoYXMgdmVyc2lvbiBtaXNtYXRjaGVzIHdpdGggaW5zdGFsbGVkIHBhY2thZ2VzLCAke2Rpc2FibGVkTWVzc2FnZX1gKTtcbiAgICAgICAgY29uc29sZS5pbmZvKGBQYWNrYWdlcyB3aXRoIHZlcnNpb24gbWlzbWF0Y2hlczogJHtKU09OLnN0cmluZ2lmeSh2ZXJzaW9uTWlzbWF0Y2hlcywgdW5kZWZpbmVkLCAyKX1gKTtcbiAgICAgICAgdmFhZGluQnVuZGxlSnNvbiA9IHsgcGFja2FnZXM6IHt9IH07XG4gICAgICAgIHJldHVybiBmYWxzZTtcbiAgICAgIH1cblxuICAgICAgcmV0dXJuIHRydWU7XG4gICAgfSxcbiAgICBhc3luYyBjb25maWcoY29uZmlnKSB7XG4gICAgICByZXR1cm4gbWVyZ2VDb25maWcoXG4gICAgICAgIHtcbiAgICAgICAgICBvcHRpbWl6ZURlcHM6IHtcbiAgICAgICAgICAgIGV4Y2x1ZGU6IFtcbiAgICAgICAgICAgICAgLy8gVmFhZGluIGJ1bmRsZVxuICAgICAgICAgICAgICAnQHZhYWRpbi9idW5kbGVzJyxcbiAgICAgICAgICAgICAgLi4uT2JqZWN0LmtleXModmFhZGluQnVuZGxlSnNvbi5wYWNrYWdlcyksXG4gICAgICAgICAgICAgICdAdmFhZGluL3ZhYWRpbi1tYXRlcmlhbC1zdHlsZXMnXG4gICAgICAgICAgICBdXG4gICAgICAgICAgfVxuICAgICAgICB9LFxuICAgICAgICBjb25maWdcbiAgICAgICk7XG4gICAgfSxcbiAgICBsb2FkKHJhd0lkKSB7XG4gICAgICBjb25zdCBbcGF0aCwgcGFyYW1zXSA9IHJhd0lkLnNwbGl0KCc/Jyk7XG4gICAgICBpZiAoIXBhdGguc3RhcnRzV2l0aChtb2R1bGVzRGlyZWN0b3J5KSkgcmV0dXJuO1xuXG4gICAgICBjb25zdCBpZCA9IHBhdGguc3Vic3RyaW5nKG1vZHVsZXNEaXJlY3RvcnkubGVuZ3RoICsgMSk7XG4gICAgICBjb25zdCBiaW5kaW5ncyA9IGdldEV4cG9ydHMoaWQpO1xuICAgICAgaWYgKGJpbmRpbmdzID09PSB1bmRlZmluZWQpIHJldHVybjtcblxuICAgICAgY29uc3QgY2FjaGVTdWZmaXggPSBwYXJhbXMgPyBgPyR7cGFyYW1zfWAgOiAnJztcbiAgICAgIGNvbnN0IGJ1bmRsZVBhdGggPSBgQHZhYWRpbi9idW5kbGVzL3ZhYWRpbi5qcyR7Y2FjaGVTdWZmaXh9YDtcblxuICAgICAgcmV0dXJuIGBpbXBvcnQgeyBpbml0IGFzIFZhYWRpbkJ1bmRsZUluaXQsIGdldCBhcyBWYWFkaW5CdW5kbGVHZXQgfSBmcm9tICcke2J1bmRsZVBhdGh9JztcbmF3YWl0IFZhYWRpbkJ1bmRsZUluaXQoJ2RlZmF1bHQnKTtcbmNvbnN0IHsgJHtiaW5kaW5ncy5tYXAoZ2V0SW1wb3J0QXNzaWdtZW50KS5qb2luKCcsICcpfSB9ID0gKGF3YWl0IFZhYWRpbkJ1bmRsZUdldCgnLi9ub2RlX21vZHVsZXMvJHtpZH0nKSkoKTtcbmV4cG9ydCB7ICR7YmluZGluZ3MubWFwKGdldEV4cG9ydEJpbmRpbmcpLmpvaW4oJywgJyl9IH07YDtcbiAgICB9XG4gIH07XG59XG5cbmZ1bmN0aW9uIHRoZW1lUGx1Z2luKG9wdHMpOiBQbHVnaW5PcHRpb24ge1xuICBjb25zdCBmdWxsVGhlbWVPcHRpb25zID0geyAuLi50aGVtZU9wdGlvbnMsIGRldk1vZGU6IG9wdHMuZGV2TW9kZSB9O1xuICByZXR1cm4ge1xuICAgIG5hbWU6ICd2YWFkaW46dGhlbWUnLFxuICAgIGNvbmZpZygpIHtcbiAgICAgIHByb2Nlc3NUaGVtZVJlc291cmNlcyhmdWxsVGhlbWVPcHRpb25zLCBjb25zb2xlKTtcbiAgICB9LFxuICAgIGNvbmZpZ3VyZVNlcnZlcihzZXJ2ZXIpIHtcbiAgICAgIGZ1bmN0aW9uIGhhbmRsZVRoZW1lRmlsZUNyZWF0ZURlbGV0ZSh0aGVtZUZpbGUsIHN0YXRzKSB7XG4gICAgICAgIGlmICh0aGVtZUZpbGUuc3RhcnRzV2l0aCh0aGVtZUZvbGRlcikpIHtcbiAgICAgICAgICBjb25zdCBjaGFuZ2VkID0gcGF0aC5yZWxhdGl2ZSh0aGVtZUZvbGRlciwgdGhlbWVGaWxlKTtcbiAgICAgICAgICBjb25zb2xlLmRlYnVnKCdUaGVtZSBmaWxlICcgKyAoISFzdGF0cyA/ICdjcmVhdGVkJyA6ICdkZWxldGVkJyksIGNoYW5nZWQpO1xuICAgICAgICAgIHByb2Nlc3NUaGVtZVJlc291cmNlcyhmdWxsVGhlbWVPcHRpb25zLCBjb25zb2xlKTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgc2VydmVyLndhdGNoZXIub24oJ2FkZCcsIGhhbmRsZVRoZW1lRmlsZUNyZWF0ZURlbGV0ZSk7XG4gICAgICBzZXJ2ZXIud2F0Y2hlci5vbigndW5saW5rJywgaGFuZGxlVGhlbWVGaWxlQ3JlYXRlRGVsZXRlKTtcbiAgICB9LFxuICAgIGhhbmRsZUhvdFVwZGF0ZShjb250ZXh0KSB7XG4gICAgICBjb25zdCBjb250ZXh0UGF0aCA9IHBhdGgucmVzb2x2ZShjb250ZXh0LmZpbGUpO1xuICAgICAgY29uc3QgdGhlbWVQYXRoID0gcGF0aC5yZXNvbHZlKHRoZW1lRm9sZGVyKTtcbiAgICAgIGlmIChjb250ZXh0UGF0aC5zdGFydHNXaXRoKHRoZW1lUGF0aCkpIHtcbiAgICAgICAgY29uc3QgY2hhbmdlZCA9IHBhdGgucmVsYXRpdmUodGhlbWVQYXRoLCBjb250ZXh0UGF0aCk7XG5cbiAgICAgICAgY29uc29sZS5kZWJ1ZygnVGhlbWUgZmlsZSBjaGFuZ2VkJywgY2hhbmdlZCk7XG5cbiAgICAgICAgaWYgKGNoYW5nZWQuc3RhcnRzV2l0aChzZXR0aW5ncy50aGVtZU5hbWUpKSB7XG4gICAgICAgICAgcHJvY2Vzc1RoZW1lUmVzb3VyY2VzKGZ1bGxUaGVtZU9wdGlvbnMsIGNvbnNvbGUpO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfSxcbiAgICBhc3luYyByZXNvbHZlSWQoaWQsIGltcG9ydGVyKSB7XG4gICAgICAvLyBmb3JjZSB0aGVtZSBnZW5lcmF0aW9uIGlmIGdlbmVyYXRlZCB0aGVtZSBzb3VyY2VzIGRvZXMgbm90IHlldCBleGlzdFxuICAgICAgLy8gdGhpcyBtYXkgaGFwcGVuIGZvciBleGFtcGxlIGR1cmluZyBKYXZhIGhvdCByZWxvYWQgd2hlbiB1cGRhdGluZ1xuICAgICAgLy8gQFRoZW1lIGFubm90YXRpb24gdmFsdWVcbiAgICAgIGlmIChcbiAgICAgICAgcGF0aC5yZXNvbHZlKHRoZW1lT3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlciwgJ3RoZW1lLmpzJykgPT09IGltcG9ydGVyICYmXG4gICAgICAgICFleGlzdHNTeW5jKHBhdGgucmVzb2x2ZSh0aGVtZU9wdGlvbnMuZnJvbnRlbmRHZW5lcmF0ZWRGb2xkZXIsIGlkKSlcbiAgICAgICkge1xuICAgICAgICBjb25zb2xlLmRlYnVnKCdHZW5lcmF0ZSB0aGVtZSBmaWxlICcgKyBpZCArICcgbm90IGV4aXN0aW5nLiBQcm9jZXNzaW5nIHRoZW1lIHJlc291cmNlJyk7XG4gICAgICAgIHByb2Nlc3NUaGVtZVJlc291cmNlcyhmdWxsVGhlbWVPcHRpb25zLCBjb25zb2xlKTtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgaWYgKCFpZC5zdGFydHNXaXRoKHNldHRpbmdzLnRoZW1lRm9sZGVyKSkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGZvciAoY29uc3QgbG9jYXRpb24gb2YgW3RoZW1lUmVzb3VyY2VGb2xkZXIsIGZyb250ZW5kRm9sZGVyXSkge1xuICAgICAgICBjb25zdCByZXN1bHQgPSBhd2FpdCB0aGlzLnJlc29sdmUocGF0aC5yZXNvbHZlKGxvY2F0aW9uLCBpZCkpO1xuICAgICAgICBpZiAocmVzdWx0KSB7XG4gICAgICAgICAgcmV0dXJuIHJlc3VsdDtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH0sXG4gICAgYXN5bmMgdHJhbnNmb3JtKHJhdywgaWQsIG9wdGlvbnMpIHtcbiAgICAgIC8vIHJld3JpdGUgdXJscyBmb3IgdGhlIGFwcGxpY2F0aW9uIHRoZW1lIGNzcyBmaWxlc1xuICAgICAgY29uc3QgW2JhcmVJZCwgcXVlcnldID0gaWQuc3BsaXQoJz8nKTtcbiAgICAgIGlmIChcbiAgICAgICAgKCFiYXJlSWQ/LnN0YXJ0c1dpdGgodGhlbWVGb2xkZXIpICYmICFiYXJlSWQ/LnN0YXJ0c1dpdGgodGhlbWVPcHRpb25zLnRoZW1lUmVzb3VyY2VGb2xkZXIpKSB8fFxuICAgICAgICAhYmFyZUlkPy5lbmRzV2l0aCgnLmNzcycpXG4gICAgICApIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgY29uc3QgW3RoZW1lTmFtZV0gPSBiYXJlSWQuc3Vic3RyaW5nKHRoZW1lRm9sZGVyLmxlbmd0aCArIDEpLnNwbGl0KCcvJyk7XG4gICAgICByZXR1cm4gcmV3cml0ZUNzc1VybHMocmF3LCBwYXRoLmRpcm5hbWUoYmFyZUlkKSwgcGF0aC5yZXNvbHZlKHRoZW1lRm9sZGVyLCB0aGVtZU5hbWUpLCBjb25zb2xlLCBvcHRzKTtcbiAgICB9XG4gIH07XG59XG5cbmZ1bmN0aW9uIHJ1bldhdGNoRG9nKHdhdGNoRG9nUG9ydCwgd2F0Y2hEb2dIb3N0KSB7XG4gIGNvbnN0IGNsaWVudCA9IG5ldC5Tb2NrZXQoKTtcbiAgY2xpZW50LnNldEVuY29kaW5nKCd1dGY4Jyk7XG4gIGNsaWVudC5vbignZXJyb3InLCBmdW5jdGlvbiAoZXJyKSB7XG4gICAgY29uc29sZS5sb2coJ1dhdGNoZG9nIGNvbm5lY3Rpb24gZXJyb3IuIFRlcm1pbmF0aW5nIHZpdGUgcHJvY2Vzcy4uLicsIGVycik7XG4gICAgY2xpZW50LmRlc3Ryb3koKTtcbiAgICBwcm9jZXNzLmV4aXQoMCk7XG4gIH0pO1xuICBjbGllbnQub24oJ2Nsb3NlJywgZnVuY3Rpb24gKCkge1xuICAgIGNsaWVudC5kZXN0cm95KCk7XG4gICAgcnVuV2F0Y2hEb2cod2F0Y2hEb2dQb3J0LCB3YXRjaERvZ0hvc3QpO1xuICB9KTtcblxuICBjbGllbnQuY29ubmVjdCh3YXRjaERvZ1BvcnQsIHdhdGNoRG9nSG9zdCB8fCAnbG9jYWxob3N0Jyk7XG59XG5cbmxldCBzcGFNaWRkbGV3YXJlRm9yY2VSZW1vdmVkID0gZmFsc2U7XG5cbmNvbnN0IGFsbG93ZWRGcm9udGVuZEZvbGRlcnMgPSBbZnJvbnRlbmRGb2xkZXIsIG5vZGVNb2R1bGVzRm9sZGVyXTtcblxuZnVuY3Rpb24gc2hvd1JlY29tcGlsZVJlYXNvbigpOiBQbHVnaW5PcHRpb24ge1xuICByZXR1cm4ge1xuICAgIG5hbWU6ICd2YWFkaW46d2h5LXlvdS1jb21waWxlJyxcbiAgICBoYW5kbGVIb3RVcGRhdGUoY29udGV4dCkge1xuICAgICAgY29uc29sZS5sb2coJ1JlY29tcGlsaW5nIGJlY2F1c2UnLCBjb250ZXh0LmZpbGUsICdjaGFuZ2VkJyk7XG4gICAgfVxuICB9O1xufVxuXG5jb25zdCBERVZfTU9ERV9TVEFSVF9SRUdFWFAgPSAvXFwvXFwqW1xcKiFdXFxzK3ZhYWRpbi1kZXYtbW9kZTpzdGFydC87XG5jb25zdCBERVZfTU9ERV9DT0RFX1JFR0VYUCA9IC9cXC9cXCpbXFwqIV1cXHMrdmFhZGluLWRldi1tb2RlOnN0YXJ0KFtcXHNcXFNdKil2YWFkaW4tZGV2LW1vZGU6ZW5kXFxzK1xcKlxcKlxcLy9pO1xuXG5mdW5jdGlvbiBwcmVzZXJ2ZVVzYWdlU3RhdHMoKSB7XG4gIHJldHVybiB7XG4gICAgbmFtZTogJ3ZhYWRpbjpwcmVzZXJ2ZS11c2FnZS1zdGF0cycsXG5cbiAgICB0cmFuc2Zvcm0oc3JjOiBzdHJpbmcsIGlkOiBzdHJpbmcpIHtcbiAgICAgIGlmIChpZC5pbmNsdWRlcygndmFhZGluLXVzYWdlLXN0YXRpc3RpY3MnKSkge1xuICAgICAgICBpZiAoc3JjLmluY2x1ZGVzKCd2YWFkaW4tZGV2LW1vZGU6c3RhcnQnKSkge1xuICAgICAgICAgIGNvbnN0IG5ld1NyYyA9IHNyYy5yZXBsYWNlKERFVl9NT0RFX1NUQVJUX1JFR0VYUCwgJy8qISB2YWFkaW4tZGV2LW1vZGU6c3RhcnQnKTtcbiAgICAgICAgICBpZiAobmV3U3JjID09PSBzcmMpIHtcbiAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoJ0NvbW1lbnQgcmVwbGFjZW1lbnQgZmFpbGVkIHRvIGNoYW5nZSBhbnl0aGluZycpO1xuICAgICAgICAgIH0gZWxzZSBpZiAoIW5ld1NyYy5tYXRjaChERVZfTU9ERV9DT0RFX1JFR0VYUCkpIHtcbiAgICAgICAgICAgIGNvbnNvbGUuZXJyb3IoJ05ldyBjb21tZW50IGZhaWxzIHRvIG1hdGNoIG9yaWdpbmFsIHJlZ2V4cCcpO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICByZXR1cm4geyBjb2RlOiBuZXdTcmMgfTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgcmV0dXJuIHsgY29kZTogc3JjIH07XG4gICAgfVxuICB9O1xufVxuXG5leHBvcnQgY29uc3QgdmFhZGluQ29uZmlnOiBVc2VyQ29uZmlnRm4gPSAoZW52KSA9PiB7XG4gIGNvbnN0IGRldk1vZGUgPSBlbnYubW9kZSA9PT0gJ2RldmVsb3BtZW50JztcbiAgY29uc3QgcHJvZHVjdGlvbk1vZGUgPSAhZGV2TW9kZSAmJiAhZGV2QnVuZGxlXG5cbiAgaWYgKGRldk1vZGUgJiYgcHJvY2Vzcy5lbnYud2F0Y2hEb2dQb3J0KSB7XG4gICAgLy8gT3BlbiBhIGNvbm5lY3Rpb24gd2l0aCB0aGUgSmF2YSBkZXYtbW9kZSBoYW5kbGVyIGluIG9yZGVyIHRvIGZpbmlzaFxuICAgIC8vIHZpdGUgd2hlbiBpdCBleGl0cyBvciBjcmFzaGVzLlxuICAgIHJ1bldhdGNoRG9nKHByb2Nlc3MuZW52LndhdGNoRG9nUG9ydCwgcHJvY2Vzcy5lbnYud2F0Y2hEb2dIb3N0KTtcbiAgfVxuXG4gIHJldHVybiB7XG4gICAgcm9vdDogZnJvbnRlbmRGb2xkZXIsXG4gICAgYmFzZTogJycsXG4gICAgcHVibGljRGlyOiBmYWxzZSxcbiAgICByZXNvbHZlOiB7XG4gICAgICBhbGlhczoge1xuICAgICAgICAnQHZhYWRpbi9mbG93LWZyb250ZW5kJzogamFyUmVzb3VyY2VzRm9sZGVyLFxuICAgICAgICBGcm9udGVuZDogZnJvbnRlbmRGb2xkZXJcbiAgICAgIH0sXG4gICAgICBwcmVzZXJ2ZVN5bWxpbmtzOiB0cnVlXG4gICAgfSxcbiAgICBkZWZpbmU6IHtcbiAgICAgIE9GRkxJTkVfUEFUSDogc2V0dGluZ3Mub2ZmbGluZVBhdGgsXG4gICAgICBWSVRFX0VOQUJMRUQ6ICd0cnVlJ1xuICAgIH0sXG4gICAgc2VydmVyOiB7XG4gICAgICBob3N0OiAnMTI3LjAuMC4xJyxcbiAgICAgIHN0cmljdFBvcnQ6IHRydWUsXG4gICAgICBmczoge1xuICAgICAgICBhbGxvdzogYWxsb3dlZEZyb250ZW5kRm9sZGVyc1xuICAgICAgfVxuICAgIH0sXG4gICAgYnVpbGQ6IHtcbiAgICAgIG91dERpcjogYnVpbGRPdXRwdXRGb2xkZXIsXG4gICAgICBlbXB0eU91dERpcjogZGV2QnVuZGxlLFxuICAgICAgYXNzZXRzRGlyOiAnVkFBRElOL2J1aWxkJyxcbiAgICAgIHJvbGx1cE9wdGlvbnM6IHtcbiAgICAgICAgaW5wdXQ6IHtcbiAgICAgICAgICBpbmRleGh0bWw6IHByb2plY3RJbmRleEh0bWwsXG5cbiAgICAgICAgICAuLi4oaGFzRXhwb3J0ZWRXZWJDb21wb25lbnRzID8geyB3ZWJjb21wb25lbnRodG1sOiBwYXRoLnJlc29sdmUoZnJvbnRlbmRGb2xkZXIsICd3ZWItY29tcG9uZW50Lmh0bWwnKSB9IDoge30pXG4gICAgICAgIH0sXG4gICAgICAgIG9ud2FybjogKHdhcm5pbmc6IHJvbGx1cC5Sb2xsdXBXYXJuaW5nLCBkZWZhdWx0SGFuZGxlcjogcm9sbHVwLldhcm5pbmdIYW5kbGVyKSA9PiB7XG4gICAgICAgICAgY29uc3QgaWdub3JlRXZhbFdhcm5pbmcgPSBbXG4gICAgICAgICAgICAnZ2VuZXJhdGVkL2phci1yZXNvdXJjZXMvRmxvd0NsaWVudC5qcycsXG4gICAgICAgICAgICAnZ2VuZXJhdGVkL2phci1yZXNvdXJjZXMvdmFhZGluLXNwcmVhZHNoZWV0L3NwcmVhZHNoZWV0LWV4cG9ydC5qcycsXG4gICAgICAgICAgICAnQHZhYWRpbi9jaGFydHMvc3JjL2hlbHBlcnMuanMnXG4gICAgICAgICAgXTtcbiAgICAgICAgICBpZiAod2FybmluZy5jb2RlID09PSAnRVZBTCcgJiYgd2FybmluZy5pZCAmJiAhIWlnbm9yZUV2YWxXYXJuaW5nLmZpbmQoKGlkKSA9PiB3YXJuaW5nLmlkLmVuZHNXaXRoKGlkKSkpIHtcbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICB9XG4gICAgICAgICAgZGVmYXVsdEhhbmRsZXIod2FybmluZyk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9LFxuICAgIG9wdGltaXplRGVwczoge1xuICAgICAgZW50cmllczogW1xuICAgICAgICAvLyBQcmUtc2NhbiBlbnRyeXBvaW50cyBpbiBWaXRlIHRvIGF2b2lkIHJlbG9hZGluZyBvbiBmaXJzdCBvcGVuXG4gICAgICAgICdnZW5lcmF0ZWQvdmFhZGluLnRzJ1xuICAgICAgXSxcbiAgICAgIGV4Y2x1ZGU6IFtcbiAgICAgICAgJ0B2YWFkaW4vcm91dGVyJyxcbiAgICAgICAgJ0B2YWFkaW4vdmFhZGluLWxpY2Vuc2UtY2hlY2tlcicsXG4gICAgICAgICdAdmFhZGluL3ZhYWRpbi11c2FnZS1zdGF0aXN0aWNzJyxcbiAgICAgICAgJ3dvcmtib3gtY29yZScsXG4gICAgICAgICd3b3JrYm94LXByZWNhY2hpbmcnLFxuICAgICAgICAnd29ya2JveC1yb3V0aW5nJyxcbiAgICAgICAgJ3dvcmtib3gtc3RyYXRlZ2llcydcbiAgICAgIF1cbiAgICB9LFxuICAgIHBsdWdpbnM6IFtcbiAgICAgIHByb2R1Y3Rpb25Nb2RlICYmIGJyb3RsaSgpLFxuICAgICAgZGV2TW9kZSAmJiB2YWFkaW5CdW5kbGVzUGx1Z2luKCksXG4gICAgICBkZXZNb2RlICYmIHNob3dSZWNvbXBpbGVSZWFzb24oKSxcbiAgICAgIHNldHRpbmdzLm9mZmxpbmVFbmFibGVkICYmIGJ1aWxkU1dQbHVnaW4oeyBkZXZNb2RlIH0pLFxuICAgICAgIWRldk1vZGUgJiYgc3RhdHNFeHRyYWN0ZXJQbHVnaW4oKSxcbiAgICAgIGRldkJ1bmRsZSAmJiBwcmVzZXJ2ZVVzYWdlU3RhdHMoKSxcbiAgICAgIHRoZW1lUGx1Z2luKHsgZGV2TW9kZSB9KSxcbiAgICAgIHBvc3Rjc3NMaXQoe1xuICAgICAgICBpbmNsdWRlOiBbJyoqLyouY3NzJywgLy4qXFwvLipcXC5jc3NcXD8uKi9dLFxuICAgICAgICBleGNsdWRlOiBbXG4gICAgICAgICAgYCR7dGhlbWVGb2xkZXJ9LyoqLyouY3NzYCxcbiAgICAgICAgICBuZXcgUmVnRXhwKGAke3RoZW1lRm9sZGVyfS8uKi8uKlxcXFwuY3NzXFxcXD8uKmApLFxuICAgICAgICAgIGAke3RoZW1lUmVzb3VyY2VGb2xkZXJ9LyoqLyouY3NzYCxcbiAgICAgICAgICBuZXcgUmVnRXhwKGAke3RoZW1lUmVzb3VyY2VGb2xkZXJ9Ly4qLy4qXFxcXC5jc3NcXFxcPy4qYCksXG4gICAgICAgICAgbmV3IFJlZ0V4cCgnLiovLipcXFxcP2h0bWwtcHJveHkuKicpXG4gICAgICAgIF1cbiAgICAgIH0pLFxuICAgICAge1xuICAgICAgICBuYW1lOiAndmFhZGluOmZvcmNlLXJlbW92ZS1odG1sLW1pZGRsZXdhcmUnLFxuICAgICAgICB0cmFuc2Zvcm1JbmRleEh0bWw6IHtcbiAgICAgICAgICBvcmRlcjogJ3ByZScsXG4gICAgICAgICAgaGFuZGxlcihfaHRtbCwgeyBzZXJ2ZXIgfSkge1xuICAgICAgICAgICAgaWYgKHNlcnZlciAmJiAhc3BhTWlkZGxld2FyZUZvcmNlUmVtb3ZlZCkge1xuICAgICAgICAgICAgICBzZXJ2ZXIubWlkZGxld2FyZXMuc3RhY2sgPSBzZXJ2ZXIubWlkZGxld2FyZXMuc3RhY2suZmlsdGVyKChtdykgPT4ge1xuICAgICAgICAgICAgICAgIGNvbnN0IGhhbmRsZU5hbWUgPSAnJyArIG13LmhhbmRsZTtcbiAgICAgICAgICAgICAgICByZXR1cm4gIWhhbmRsZU5hbWUuaW5jbHVkZXMoJ3ZpdGVIdG1sRmFsbGJhY2tNaWRkbGV3YXJlJyk7XG4gICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgICBzcGFNaWRkbGV3YXJlRm9yY2VSZW1vdmVkID0gdHJ1ZTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgICBoYXNFeHBvcnRlZFdlYkNvbXBvbmVudHMgJiYge1xuICAgICAgICBuYW1lOiAndmFhZGluOmluamVjdC1lbnRyeXBvaW50cy10by13ZWItY29tcG9uZW50LWh0bWwnLFxuICAgICAgICB0cmFuc2Zvcm1JbmRleEh0bWw6IHtcbiAgICAgICAgICBvcmRlcjogJ3ByZScsXG4gICAgICAgICAgaGFuZGxlcihfaHRtbCwgeyBwYXRoLCBzZXJ2ZXIgfSkge1xuICAgICAgICAgICAgaWYgKHBhdGggIT09ICcvd2ViLWNvbXBvbmVudC5odG1sJykge1xuICAgICAgICAgICAgICByZXR1cm47XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIHJldHVybiBbXG4gICAgICAgICAgICAgIHtcbiAgICAgICAgICAgICAgICB0YWc6ICdzY3JpcHQnLFxuICAgICAgICAgICAgICAgIGF0dHJzOiB7IHR5cGU6ICdtb2R1bGUnLCBzcmM6IGAvZ2VuZXJhdGVkL3ZhYWRpbi13ZWItY29tcG9uZW50LnRzYCB9LFxuICAgICAgICAgICAgICAgIGluamVjdFRvOiAnaGVhZCdcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgXTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgICB7XG4gICAgICAgIG5hbWU6ICd2YWFkaW46aW5qZWN0LWVudHJ5cG9pbnRzLXRvLWluZGV4LWh0bWwnLFxuICAgICAgICB0cmFuc2Zvcm1JbmRleEh0bWw6IHtcbiAgICAgICAgICBvcmRlcjogJ3ByZScsXG4gICAgICAgICAgaGFuZGxlcihfaHRtbCwgeyBwYXRoLCBzZXJ2ZXIgfSkge1xuICAgICAgICAgICAgaWYgKHBhdGggIT09ICcvaW5kZXguaHRtbCcpIHtcbiAgICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICBjb25zdCBzY3JpcHRzID0gW107XG5cbiAgICAgICAgICAgIGlmIChkZXZNb2RlKSB7XG4gICAgICAgICAgICAgIHNjcmlwdHMucHVzaCh7XG4gICAgICAgICAgICAgICAgdGFnOiAnc2NyaXB0JyxcbiAgICAgICAgICAgICAgICBhdHRyczogeyB0eXBlOiAnbW9kdWxlJywgc3JjOiBgL2dlbmVyYXRlZC92aXRlLWRldm1vZGUudHNgIH0sXG4gICAgICAgICAgICAgICAgaW5qZWN0VG86ICdoZWFkJ1xuICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHNjcmlwdHMucHVzaCh7XG4gICAgICAgICAgICAgIHRhZzogJ3NjcmlwdCcsXG4gICAgICAgICAgICAgIGF0dHJzOiB7IHR5cGU6ICdtb2R1bGUnLCBzcmM6ICcvZ2VuZXJhdGVkL3ZhYWRpbi50cycgfSxcbiAgICAgICAgICAgICAgaW5qZWN0VG86ICdoZWFkJ1xuICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICByZXR1cm4gc2NyaXB0cztcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgICBjaGVja2VyKHtcbiAgICAgICAgdHlwZXNjcmlwdDogdHJ1ZVxuICAgICAgfSksXG4gICAgICBwcm9kdWN0aW9uTW9kZSAmJiB2aXN1YWxpemVyKHsgYnJvdGxpU2l6ZTogdHJ1ZSwgZmlsZW5hbWU6IGJ1bmRsZVNpemVGaWxlIH0pXG4gICAgXVxuICB9O1xufTtcblxuZXhwb3J0IGNvbnN0IG92ZXJyaWRlVmFhZGluQ29uZmlnID0gKGN1c3RvbUNvbmZpZzogVXNlckNvbmZpZ0ZuKSA9PiB7XG4gIHJldHVybiBkZWZpbmVDb25maWcoKGVudikgPT4gbWVyZ2VDb25maWcodmFhZGluQ29uZmlnKGVudiksIGN1c3RvbUNvbmZpZyhlbnYpKSk7XG59O1xuZnVuY3Rpb24gZ2V0VmVyc2lvbihtb2R1bGU6IHN0cmluZyk6IHN0cmluZyB7XG4gIGNvbnN0IHBhY2thZ2VKc29uID0gcGF0aC5yZXNvbHZlKG5vZGVNb2R1bGVzRm9sZGVyLCBtb2R1bGUsICdwYWNrYWdlLmpzb24nKTtcbiAgcmV0dXJuIEpTT04ucGFyc2UocmVhZEZpbGVTeW5jKHBhY2thZ2VKc29uLCB7IGVuY29kaW5nOiAndXRmLTgnIH0pKS52ZXJzaW9uO1xufVxuZnVuY3Rpb24gZ2V0Q3ZkbE5hbWUobW9kdWxlOiBzdHJpbmcpOiBzdHJpbmcge1xuICBjb25zdCBwYWNrYWdlSnNvbiA9IHBhdGgucmVzb2x2ZShub2RlTW9kdWxlc0ZvbGRlciwgbW9kdWxlLCAncGFja2FnZS5qc29uJyk7XG4gIHJldHVybiBKU09OLnBhcnNlKHJlYWRGaWxlU3luYyhwYWNrYWdlSnNvbiwgeyBlbmNvZGluZzogJ3V0Zi04JyB9KSkuY3ZkbE5hbWU7XG59XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxccGx1Z2luc1xcXFxhcHBsaWNhdGlvbi10aGVtZS1wbHVnaW5cIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxccGx1Z2luc1xcXFxhcHBsaWNhdGlvbi10aGVtZS1wbHVnaW5cXFxcdGhlbWUtaGFuZGxlLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9sYW15ai9naXQvb3dsY21zNC9vd2xjbXMvdGFyZ2V0L3BsdWdpbnMvYXBwbGljYXRpb24tdGhlbWUtcGx1Z2luL3RoZW1lLWhhbmRsZS5qc1wiOy8qXG4gKiBDb3B5cmlnaHQgMjAwMC0yMDIzIFZhYWRpbiBMdGQuXG4gKlxuICogTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTsgeW91IG1heSBub3RcbiAqIHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS4gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mXG4gKiB0aGUgTGljZW5zZSBhdFxuICpcbiAqIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuICpcbiAqIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbiAqIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUywgV0lUSE9VVFxuICogV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLiBTZWUgdGhlXG4gKiBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZCBsaW1pdGF0aW9ucyB1bmRlclxuICogdGhlIExpY2Vuc2UuXG4gKi9cblxuLyoqXG4gKiBUaGlzIGZpbGUgY29udGFpbnMgZnVuY3Rpb25zIGZvciBsb29rIHVwIGFuZCBoYW5kbGUgdGhlIHRoZW1lIHJlc291cmNlc1xuICogZm9yIGFwcGxpY2F0aW9uIHRoZW1lIHBsdWdpbi5cbiAqL1xuaW1wb3J0IHsgZXhpc3RzU3luYywgcmVhZEZpbGVTeW5jIH0gZnJvbSAnZnMnO1xuaW1wb3J0IHsgcmVzb2x2ZSB9IGZyb20gJ3BhdGgnO1xuaW1wb3J0IHsgd3JpdGVUaGVtZUZpbGVzIH0gZnJvbSAnLi90aGVtZS1nZW5lcmF0b3IuanMnO1xuaW1wb3J0IHsgY29weVN0YXRpY0Fzc2V0cywgY29weVRoZW1lUmVzb3VyY2VzIH0gZnJvbSAnLi90aGVtZS1jb3B5LmpzJztcblxuLy8gbWF0Y2hlcyB0aGVtZSBuYW1lIGluICcuL3RoZW1lLW15LXRoZW1lLmdlbmVyYXRlZC5qcydcbmNvbnN0IG5hbWVSZWdleCA9IC90aGVtZS0oLiopXFwuZ2VuZXJhdGVkXFwuanMvO1xuXG5sZXQgcHJldlRoZW1lTmFtZSA9IHVuZGVmaW5lZDtcbmxldCBmaXJzdFRoZW1lTmFtZSA9IHVuZGVmaW5lZDtcblxuLyoqXG4gKiBMb29rcyB1cCBmb3IgYSB0aGVtZSByZXNvdXJjZXMgaW4gYSBjdXJyZW50IHByb2plY3QgYW5kIGluIGphciBkZXBlbmRlbmNpZXMsXG4gKiBjb3BpZXMgdGhlIGZvdW5kIHJlc291cmNlcyBhbmQgZ2VuZXJhdGVzL3VwZGF0ZXMgbWV0YSBkYXRhIGZvciB3ZWJwYWNrXG4gKiBjb21waWxhdGlvbi5cbiAqXG4gKiBAcGFyYW0ge29iamVjdH0gb3B0aW9ucyBhcHBsaWNhdGlvbiB0aGVtZSBwbHVnaW4gbWFuZGF0b3J5IG9wdGlvbnMsXG4gKiBAc2VlIHtAbGluayBBcHBsaWNhdGlvblRoZW1lUGx1Z2lufVxuICpcbiAqIEBwYXJhbSBsb2dnZXIgYXBwbGljYXRpb24gdGhlbWUgcGx1Z2luIGxvZ2dlclxuICovXG5mdW5jdGlvbiBwcm9jZXNzVGhlbWVSZXNvdXJjZXMob3B0aW9ucywgbG9nZ2VyKSB7XG4gIGNvbnN0IHRoZW1lTmFtZSA9IGV4dHJhY3RUaGVtZU5hbWUob3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlcik7XG4gIGlmICh0aGVtZU5hbWUpIHtcbiAgICBpZiAoIXByZXZUaGVtZU5hbWUgJiYgIWZpcnN0VGhlbWVOYW1lKSB7XG4gICAgICBmaXJzdFRoZW1lTmFtZSA9IHRoZW1lTmFtZTtcbiAgICB9IGVsc2UgaWYgKFxuICAgICAgKHByZXZUaGVtZU5hbWUgJiYgcHJldlRoZW1lTmFtZSAhPT0gdGhlbWVOYW1lICYmIGZpcnN0VGhlbWVOYW1lICE9PSB0aGVtZU5hbWUpIHx8XG4gICAgICAoIXByZXZUaGVtZU5hbWUgJiYgZmlyc3RUaGVtZU5hbWUgIT09IHRoZW1lTmFtZSlcbiAgICApIHtcbiAgICAgIC8vIFdhcm5pbmcgbWVzc2FnZSBpcyBzaG93biB0byB0aGUgZGV2ZWxvcGVyIHdoZW46XG4gICAgICAvLyAxLiBIZSBpcyBzd2l0Y2hpbmcgdG8gYW55IHRoZW1lLCB3aGljaCBpcyBkaWZmZXIgZnJvbSBvbmUgYmVpbmcgc2V0IHVwXG4gICAgICAvLyBvbiBhcHBsaWNhdGlvbiBzdGFydHVwLCBieSBjaGFuZ2luZyB0aGVtZSBuYW1lIGluIGBAVGhlbWUoKWBcbiAgICAgIC8vIDIuIEhlIHJlbW92ZXMgb3IgY29tbWVudHMgb3V0IGBAVGhlbWUoKWAgdG8gc2VlIGhvdyB0aGUgYXBwXG4gICAgICAvLyBsb29rcyBsaWtlIHdpdGhvdXQgdGhlbWluZywgYW5kIHRoZW4gYWdhaW4gYnJpbmdzIGBAVGhlbWUoKWAgYmFja1xuICAgICAgLy8gd2l0aCBhIHRoZW1lTmFtZSB3aGljaCBpcyBkaWZmZXIgZnJvbSBvbmUgYmVpbmcgc2V0IHVwIG9uIGFwcGxpY2F0aW9uXG4gICAgICAvLyBzdGFydHVwLlxuICAgICAgY29uc3Qgd2FybmluZyA9IGBBdHRlbnRpb246IEFjdGl2ZSB0aGVtZSBpcyBzd2l0Y2hlZCB0byAnJHt0aGVtZU5hbWV9Jy5gO1xuICAgICAgY29uc3QgZGVzY3JpcHRpb24gPSBgXG4gICAgICBOb3RlIHRoYXQgYWRkaW5nIG5ldyBzdHlsZSBzaGVldCBmaWxlcyB0byAnL3RoZW1lcy8ke3RoZW1lTmFtZX0vY29tcG9uZW50cycsIFxuICAgICAgbWF5IG5vdCBiZSB0YWtlbiBpbnRvIGVmZmVjdCB1bnRpbCB0aGUgbmV4dCBhcHBsaWNhdGlvbiByZXN0YXJ0LlxuICAgICAgQ2hhbmdlcyB0byBhbHJlYWR5IGV4aXN0aW5nIHN0eWxlIHNoZWV0IGZpbGVzIGFyZSBiZWluZyByZWxvYWRlZCBhcyBiZWZvcmUuYDtcbiAgICAgIGxvZ2dlci53YXJuKCcqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqJyk7XG4gICAgICBsb2dnZXIud2Fybih3YXJuaW5nKTtcbiAgICAgIGxvZ2dlci53YXJuKGRlc2NyaXB0aW9uKTtcbiAgICAgIGxvZ2dlci53YXJuKCcqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqKioqJyk7XG4gICAgfVxuICAgIHByZXZUaGVtZU5hbWUgPSB0aGVtZU5hbWU7XG5cbiAgICBmaW5kVGhlbWVGb2xkZXJBbmRIYW5kbGVUaGVtZSh0aGVtZU5hbWUsIG9wdGlvbnMsIGxvZ2dlcik7XG4gIH0gZWxzZSB7XG4gICAgLy8gVGhpcyBpcyBuZWVkZWQgaW4gdGhlIHNpdHVhdGlvbiB0aGF0IHRoZSB1c2VyIGRlY2lkZXMgdG8gY29tbWVudCBvclxuICAgIC8vIHJlbW92ZSB0aGUgQFRoZW1lKC4uLikgY29tcGxldGVseSB0byBzZWUgaG93IHRoZSBhcHBsaWNhdGlvbiBsb29rc1xuICAgIC8vIHdpdGhvdXQgYW55IHRoZW1lLiBUaGVuIHdoZW4gdGhlIHVzZXIgYnJpbmdzIGJhY2sgb25lIG9mIHRoZSB0aGVtZXMsXG4gICAgLy8gdGhlIHByZXZpb3VzIHRoZW1lIHNob3VsZCBiZSB1bmRlZmluZWQgdG8gZW5hYmxlIHVzIHRvIGRldGVjdCB0aGUgY2hhbmdlLlxuICAgIHByZXZUaGVtZU5hbWUgPSB1bmRlZmluZWQ7XG4gICAgbG9nZ2VyLmRlYnVnKCdTa2lwcGluZyBWYWFkaW4gYXBwbGljYXRpb24gdGhlbWUgaGFuZGxpbmcuJyk7XG4gICAgbG9nZ2VyLnRyYWNlKCdNb3N0IGxpa2VseSBubyBAVGhlbWUgYW5ub3RhdGlvbiBmb3IgYXBwbGljYXRpb24gb3Igb25seSB0aGVtZUNsYXNzIHVzZWQuJyk7XG4gIH1cbn1cblxuLyoqXG4gKiBTZWFyY2ggZm9yIHRoZSBnaXZlbiB0aGVtZSBpbiB0aGUgcHJvamVjdCBhbmQgcmVzb3VyY2UgZm9sZGVycy5cbiAqXG4gKiBAcGFyYW0ge3N0cmluZ30gdGhlbWVOYW1lIG5hbWUgb2YgdGhlbWUgdG8gZmluZFxuICogQHBhcmFtIHtvYmplY3R9IG9wdGlvbnMgYXBwbGljYXRpb24gdGhlbWUgcGx1Z2luIG1hbmRhdG9yeSBvcHRpb25zLFxuICogQHNlZSB7QGxpbmsgQXBwbGljYXRpb25UaGVtZVBsdWdpbn1cbiAqIEBwYXJhbSBsb2dnZXIgYXBwbGljYXRpb24gdGhlbWUgcGx1Z2luIGxvZ2dlclxuICogQHJldHVybiB0cnVlIG9yIGZhbHNlIGZvciBpZiB0aGVtZSB3YXMgZm91bmRcbiAqL1xuZnVuY3Rpb24gZmluZFRoZW1lRm9sZGVyQW5kSGFuZGxlVGhlbWUodGhlbWVOYW1lLCBvcHRpb25zLCBsb2dnZXIpIHtcbiAgbGV0IHRoZW1lRm91bmQgPSBmYWxzZTtcbiAgZm9yIChsZXQgaSA9IDA7IGkgPCBvcHRpb25zLnRoZW1lUHJvamVjdEZvbGRlcnMubGVuZ3RoOyBpKyspIHtcbiAgICBjb25zdCB0aGVtZVByb2plY3RGb2xkZXIgPSBvcHRpb25zLnRoZW1lUHJvamVjdEZvbGRlcnNbaV07XG4gICAgaWYgKGV4aXN0c1N5bmModGhlbWVQcm9qZWN0Rm9sZGVyKSkge1xuICAgICAgbG9nZ2VyLmRlYnVnKFwiU2VhcmNoaW5nIHRoZW1lcyBmb2xkZXIgJ1wiICsgdGhlbWVQcm9qZWN0Rm9sZGVyICsgXCInIGZvciB0aGVtZSAnXCIgKyB0aGVtZU5hbWUgKyBcIidcIik7XG4gICAgICBjb25zdCBoYW5kbGVkID0gaGFuZGxlVGhlbWVzKHRoZW1lTmFtZSwgdGhlbWVQcm9qZWN0Rm9sZGVyLCBvcHRpb25zLCBsb2dnZXIpO1xuICAgICAgaWYgKGhhbmRsZWQpIHtcbiAgICAgICAgaWYgKHRoZW1lRm91bmQpIHtcbiAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoXG4gICAgICAgICAgICBcIkZvdW5kIHRoZW1lIGZpbGVzIGluICdcIiArXG4gICAgICAgICAgICAgIHRoZW1lUHJvamVjdEZvbGRlciArXG4gICAgICAgICAgICAgIFwiJyBhbmQgJ1wiICtcbiAgICAgICAgICAgICAgdGhlbWVGb3VuZCArXG4gICAgICAgICAgICAgIFwiJy4gVGhlbWUgc2hvdWxkIG9ubHkgYmUgYXZhaWxhYmxlIGluIG9uZSBmb2xkZXJcIlxuICAgICAgICAgICk7XG4gICAgICAgIH1cbiAgICAgICAgbG9nZ2VyLmRlYnVnKFwiRm91bmQgdGhlbWUgZmlsZXMgZnJvbSAnXCIgKyB0aGVtZVByb2plY3RGb2xkZXIgKyBcIidcIik7XG4gICAgICAgIHRoZW1lRm91bmQgPSB0aGVtZVByb2plY3RGb2xkZXI7XG4gICAgICB9XG4gICAgfVxuICB9XG5cbiAgaWYgKGV4aXN0c1N5bmMob3B0aW9ucy50aGVtZVJlc291cmNlRm9sZGVyKSkge1xuICAgIGlmICh0aGVtZUZvdW5kICYmIGV4aXN0c1N5bmMocmVzb2x2ZShvcHRpb25zLnRoZW1lUmVzb3VyY2VGb2xkZXIsIHRoZW1lTmFtZSkpKSB7XG4gICAgICB0aHJvdyBuZXcgRXJyb3IoXG4gICAgICAgIFwiVGhlbWUgJ1wiICtcbiAgICAgICAgICB0aGVtZU5hbWUgK1xuICAgICAgICAgIFwiJ3Nob3VsZCBub3QgZXhpc3QgaW5zaWRlIGEgamFyIGFuZCBpbiB0aGUgcHJvamVjdCBhdCB0aGUgc2FtZSB0aW1lXFxuXCIgK1xuICAgICAgICAgICdFeHRlbmRpbmcgYW5vdGhlciB0aGVtZSBpcyBwb3NzaWJsZSBieSBhZGRpbmcgeyBcInBhcmVudFwiOiBcIm15LXBhcmVudC10aGVtZVwiIH0gZW50cnkgdG8gdGhlIHRoZW1lLmpzb24gZmlsZSBpbnNpZGUgeW91ciB0aGVtZSBmb2xkZXIuJ1xuICAgICAgKTtcbiAgICB9XG4gICAgbG9nZ2VyLmRlYnVnKFxuICAgICAgXCJTZWFyY2hpbmcgdGhlbWUgamFyIHJlc291cmNlIGZvbGRlciAnXCIgKyBvcHRpb25zLnRoZW1lUmVzb3VyY2VGb2xkZXIgKyBcIicgZm9yIHRoZW1lICdcIiArIHRoZW1lTmFtZSArIFwiJ1wiXG4gICAgKTtcbiAgICBoYW5kbGVUaGVtZXModGhlbWVOYW1lLCBvcHRpb25zLnRoZW1lUmVzb3VyY2VGb2xkZXIsIG9wdGlvbnMsIGxvZ2dlcik7XG4gICAgdGhlbWVGb3VuZCA9IHRydWU7XG4gIH1cbiAgcmV0dXJuIHRoZW1lRm91bmQ7XG59XG5cbi8qKlxuICogQ29waWVzIHN0YXRpYyByZXNvdXJjZXMgZm9yIHRoZW1lIGFuZCBnZW5lcmF0ZXMvd3JpdGVzIHRoZVxuICogW3RoZW1lLW5hbWVdLmdlbmVyYXRlZC5qcyBmb3Igd2VicGFjayB0byBoYW5kbGUuXG4gKlxuICogTm90ZSEgSWYgYSBwYXJlbnQgdGhlbWUgaXMgZGVmaW5lZCBpdCB3aWxsIGFsc28gYmUgaGFuZGxlZCBoZXJlIHNvIHRoYXQgdGhlIHBhcmVudCB0aGVtZSBnZW5lcmF0ZWQgZmlsZSBpc1xuICogZ2VuZXJhdGVkIGluIGFkdmFuY2Ugb2YgdGhlIHRoZW1lIGdlbmVyYXRlZCBmaWxlLlxuICpcbiAqIEBwYXJhbSB7c3RyaW5nfSB0aGVtZU5hbWUgbmFtZSBvZiB0aGVtZSB0byBoYW5kbGVcbiAqIEBwYXJhbSB7c3RyaW5nfSB0aGVtZXNGb2xkZXIgZm9sZGVyIGNvbnRhaW5pbmcgYXBwbGljYXRpb24gdGhlbWUgZm9sZGVyc1xuICogQHBhcmFtIHtvYmplY3R9IG9wdGlvbnMgYXBwbGljYXRpb24gdGhlbWUgcGx1Z2luIG1hbmRhdG9yeSBvcHRpb25zLFxuICogQHNlZSB7QGxpbmsgQXBwbGljYXRpb25UaGVtZVBsdWdpbn1cbiAqIEBwYXJhbSB7b2JqZWN0fSBsb2dnZXIgcGx1Z2luIGxvZ2dlciBpbnN0YW5jZVxuICpcbiAqIEB0aHJvd3MgRXJyb3IgaWYgcGFyZW50IHRoZW1lIGRlZmluZWQsIGJ1dCBjYW4ndCBsb2NhdGUgcGFyZW50IHRoZW1lXG4gKlxuICogQHJldHVybnMgdHJ1ZSBpZiB0aGVtZSB3YXMgZm91bmQgZWxzZSBmYWxzZS5cbiAqL1xuZnVuY3Rpb24gaGFuZGxlVGhlbWVzKHRoZW1lTmFtZSwgdGhlbWVzRm9sZGVyLCBvcHRpb25zLCBsb2dnZXIpIHtcbiAgY29uc3QgdGhlbWVGb2xkZXIgPSByZXNvbHZlKHRoZW1lc0ZvbGRlciwgdGhlbWVOYW1lKTtcbiAgaWYgKGV4aXN0c1N5bmModGhlbWVGb2xkZXIpKSB7XG4gICAgbG9nZ2VyLmRlYnVnKCdGb3VuZCB0aGVtZSAnLCB0aGVtZU5hbWUsICcgaW4gZm9sZGVyICcsIHRoZW1lRm9sZGVyKTtcblxuICAgIGNvbnN0IHRoZW1lUHJvcGVydGllcyA9IGdldFRoZW1lUHJvcGVydGllcyh0aGVtZUZvbGRlcik7XG5cbiAgICAvLyBJZiB0aGVtZSBoYXMgcGFyZW50IGhhbmRsZSBwYXJlbnQgdGhlbWUgaW1tZWRpYXRlbHkuXG4gICAgaWYgKHRoZW1lUHJvcGVydGllcy5wYXJlbnQpIHtcbiAgICAgIGNvbnN0IGZvdW5kID0gZmluZFRoZW1lRm9sZGVyQW5kSGFuZGxlVGhlbWUodGhlbWVQcm9wZXJ0aWVzLnBhcmVudCwgb3B0aW9ucywgbG9nZ2VyKTtcbiAgICAgIGlmICghZm91bmQpIHtcbiAgICAgICAgdGhyb3cgbmV3IEVycm9yKFxuICAgICAgICAgIFwiQ291bGQgbm90IGxvY2F0ZSBmaWxlcyBmb3IgZGVmaW5lZCBwYXJlbnQgdGhlbWUgJ1wiICtcbiAgICAgICAgICAgIHRoZW1lUHJvcGVydGllcy5wYXJlbnQgK1xuICAgICAgICAgICAgXCInLlxcblwiICtcbiAgICAgICAgICAgICdQbGVhc2UgdmVyaWZ5IHRoYXQgZGVwZW5kZW5jeSBpcyBhZGRlZCBvciB0aGVtZSBmb2xkZXIgZXhpc3RzLidcbiAgICAgICAgKTtcbiAgICAgIH1cbiAgICB9XG4gICAgY29weVN0YXRpY0Fzc2V0cyh0aGVtZU5hbWUsIHRoZW1lUHJvcGVydGllcywgb3B0aW9ucy5wcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyLCBsb2dnZXIpO1xuICAgIGNvcHlUaGVtZVJlc291cmNlcyh0aGVtZUZvbGRlciwgb3B0aW9ucy5wcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyLCBsb2dnZXIpO1xuXG4gICAgd3JpdGVUaGVtZUZpbGVzKHRoZW1lRm9sZGVyLCB0aGVtZU5hbWUsIHRoZW1lUHJvcGVydGllcywgb3B0aW9ucyk7XG4gICAgcmV0dXJuIHRydWU7XG4gIH1cbiAgcmV0dXJuIGZhbHNlO1xufVxuXG5mdW5jdGlvbiBnZXRUaGVtZVByb3BlcnRpZXModGhlbWVGb2xkZXIpIHtcbiAgY29uc3QgdGhlbWVQcm9wZXJ0eUZpbGUgPSByZXNvbHZlKHRoZW1lRm9sZGVyLCAndGhlbWUuanNvbicpO1xuICBpZiAoIWV4aXN0c1N5bmModGhlbWVQcm9wZXJ0eUZpbGUpKSB7XG4gICAgcmV0dXJuIHt9O1xuICB9XG4gIGNvbnN0IHRoZW1lUHJvcGVydHlGaWxlQXNTdHJpbmcgPSByZWFkRmlsZVN5bmModGhlbWVQcm9wZXJ0eUZpbGUpO1xuICBpZiAodGhlbWVQcm9wZXJ0eUZpbGVBc1N0cmluZy5sZW5ndGggPT09IDApIHtcbiAgICByZXR1cm4ge307XG4gIH1cbiAgcmV0dXJuIEpTT04ucGFyc2UodGhlbWVQcm9wZXJ0eUZpbGVBc1N0cmluZyk7XG59XG5cbi8qKlxuICogRXh0cmFjdHMgY3VycmVudCB0aGVtZSBuYW1lIGZyb20gYXV0by1nZW5lcmF0ZWQgJ3RoZW1lLmpzJyBmaWxlIGxvY2F0ZWQgb24gYVxuICogZ2l2ZW4gZm9sZGVyLlxuICogQHBhcmFtIGZyb250ZW5kR2VuZXJhdGVkRm9sZGVyIGZvbGRlciBpbiBwcm9qZWN0IGNvbnRhaW5pbmcgJ3RoZW1lLmpzJyBmaWxlXG4gKiBAcmV0dXJucyB7c3RyaW5nfSBjdXJyZW50IHRoZW1lIG5hbWVcbiAqL1xuZnVuY3Rpb24gZXh0cmFjdFRoZW1lTmFtZShmcm9udGVuZEdlbmVyYXRlZEZvbGRlcikge1xuICBpZiAoIWZyb250ZW5kR2VuZXJhdGVkRm9sZGVyKSB7XG4gICAgdGhyb3cgbmV3IEVycm9yKFxuICAgICAgXCJDb3VsZG4ndCBleHRyYWN0IHRoZW1lIG5hbWUgZnJvbSAndGhlbWUuanMnLFwiICtcbiAgICAgICAgJyBiZWNhdXNlIHRoZSBwYXRoIHRvIGZvbGRlciBjb250YWluaW5nIHRoaXMgZmlsZSBpcyBlbXB0eS4gUGxlYXNlIHNldCcgK1xuICAgICAgICAnIHRoZSBhIGNvcnJlY3QgZm9sZGVyIHBhdGggaW4gQXBwbGljYXRpb25UaGVtZVBsdWdpbiBjb25zdHJ1Y3RvcicgK1xuICAgICAgICAnIHBhcmFtZXRlcnMuJ1xuICAgICk7XG4gIH1cbiAgY29uc3QgZ2VuZXJhdGVkVGhlbWVGaWxlID0gcmVzb2x2ZShmcm9udGVuZEdlbmVyYXRlZEZvbGRlciwgJ3RoZW1lLmpzJyk7XG4gIGlmIChleGlzdHNTeW5jKGdlbmVyYXRlZFRoZW1lRmlsZSkpIHtcbiAgICAvLyByZWFkIHRoZW1lIG5hbWUgZnJvbSB0aGUgJ2dlbmVyYXRlZC90aGVtZS5qcycgYXMgdGhlcmUgd2UgYWx3YXlzXG4gICAgLy8gbWFyayB0aGUgdXNlZCB0aGVtZSBmb3Igd2VicGFjayB0byBoYW5kbGUuXG4gICAgY29uc3QgdGhlbWVOYW1lID0gbmFtZVJlZ2V4LmV4ZWMocmVhZEZpbGVTeW5jKGdlbmVyYXRlZFRoZW1lRmlsZSwgeyBlbmNvZGluZzogJ3V0ZjgnIH0pKVsxXTtcbiAgICBpZiAoIXRoZW1lTmFtZSkge1xuICAgICAgdGhyb3cgbmV3IEVycm9yKFwiQ291bGRuJ3QgcGFyc2UgdGhlbWUgbmFtZSBmcm9tICdcIiArIGdlbmVyYXRlZFRoZW1lRmlsZSArIFwiJy5cIik7XG4gICAgfVxuICAgIHJldHVybiB0aGVtZU5hbWU7XG4gIH0gZWxzZSB7XG4gICAgcmV0dXJuICcnO1xuICB9XG59XG5cbi8qKlxuICogRmluZHMgYWxsIHRoZSBwYXJlbnQgdGhlbWVzIGxvY2F0ZWQgaW4gdGhlIHByb2plY3QgdGhlbWVzIGZvbGRlcnMgYW5kIGluXG4gKiB0aGUgSkFSIGRlcGVuZGVuY2llcyB3aXRoIHJlc3BlY3QgdG8gdGhlIGdpdmVuIGN1c3RvbSB0aGVtZSB3aXRoXG4gKiB7QGNvZGUgdGhlbWVOYW1lfS5cbiAqIEBwYXJhbSB7c3RyaW5nfSB0aGVtZU5hbWUgZ2l2ZW4gY3VzdG9tIHRoZW1lIG5hbWUgdG8gbG9vayBwYXJlbnRzIGZvclxuICogQHBhcmFtIHtvYmplY3R9IG9wdGlvbnMgYXBwbGljYXRpb24gdGhlbWUgcGx1Z2luIG1hbmRhdG9yeSBvcHRpb25zLFxuICogQHNlZSB7QGxpbmsgQXBwbGljYXRpb25UaGVtZVBsdWdpbn1cbiAqIEByZXR1cm5zIHtzdHJpbmdbXX0gYXJyYXkgb2YgcGF0aHMgdG8gZm91bmQgcGFyZW50IHRoZW1lcyB3aXRoIHJlc3BlY3QgdG8gdGhlXG4gKiBnaXZlbiBjdXN0b20gdGhlbWVcbiAqL1xuZnVuY3Rpb24gZmluZFBhcmVudFRoZW1lcyh0aGVtZU5hbWUsIG9wdGlvbnMpIHtcbiAgY29uc3QgZXhpc3RpbmdUaGVtZUZvbGRlcnMgPSBbb3B0aW9ucy50aGVtZVJlc291cmNlRm9sZGVyLCAuLi5vcHRpb25zLnRoZW1lUHJvamVjdEZvbGRlcnNdLmZpbHRlcigoZm9sZGVyKSA9PlxuICAgIGV4aXN0c1N5bmMoZm9sZGVyKVxuICApO1xuICByZXR1cm4gY29sbGVjdFBhcmVudFRoZW1lcyh0aGVtZU5hbWUsIGV4aXN0aW5nVGhlbWVGb2xkZXJzLCBmYWxzZSk7XG59XG5cbmZ1bmN0aW9uIGNvbGxlY3RQYXJlbnRUaGVtZXModGhlbWVOYW1lLCB0aGVtZUZvbGRlcnMsIGlzUGFyZW50KSB7XG4gIGxldCBmb3VuZFBhcmVudFRoZW1lcyA9IFtdO1xuICB0aGVtZUZvbGRlcnMuZm9yRWFjaCgoZm9sZGVyKSA9PiB7XG4gICAgY29uc3QgdGhlbWVGb2xkZXIgPSByZXNvbHZlKGZvbGRlciwgdGhlbWVOYW1lKTtcbiAgICBpZiAoZXhpc3RzU3luYyh0aGVtZUZvbGRlcikpIHtcbiAgICAgIGNvbnN0IHRoZW1lUHJvcGVydGllcyA9IGdldFRoZW1lUHJvcGVydGllcyh0aGVtZUZvbGRlcik7XG5cbiAgICAgIGlmICh0aGVtZVByb3BlcnRpZXMucGFyZW50KSB7XG4gICAgICAgIGZvdW5kUGFyZW50VGhlbWVzLnB1c2goLi4uY29sbGVjdFBhcmVudFRoZW1lcyh0aGVtZVByb3BlcnRpZXMucGFyZW50LCB0aGVtZUZvbGRlcnMsIHRydWUpKTtcbiAgICAgICAgaWYgKCFmb3VuZFBhcmVudFRoZW1lcy5sZW5ndGgpIHtcbiAgICAgICAgICB0aHJvdyBuZXcgRXJyb3IoXG4gICAgICAgICAgICBcIkNvdWxkIG5vdCBsb2NhdGUgZmlsZXMgZm9yIGRlZmluZWQgcGFyZW50IHRoZW1lICdcIiArXG4gICAgICAgICAgICAgIHRoZW1lUHJvcGVydGllcy5wYXJlbnQgK1xuICAgICAgICAgICAgICBcIicuXFxuXCIgK1xuICAgICAgICAgICAgICAnUGxlYXNlIHZlcmlmeSB0aGF0IGRlcGVuZGVuY3kgaXMgYWRkZWQgb3IgdGhlbWUgZm9sZGVyIGV4aXN0cy4nXG4gICAgICAgICAgKTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgICAgLy8gQWRkIGEgdGhlbWUgcGF0aCB0byByZXN1bHQgY29sbGVjdGlvbiBvbmx5IGlmIGEgZ2l2ZW4gdGhlbWVOYW1lXG4gICAgICAvLyBpcyBzdXBwb3NlZCB0byBiZSBhIHBhcmVudCB0aGVtZVxuICAgICAgaWYgKGlzUGFyZW50KSB7XG4gICAgICAgIGZvdW5kUGFyZW50VGhlbWVzLnB1c2godGhlbWVGb2xkZXIpO1xuICAgICAgfVxuICAgIH1cbiAgfSk7XG4gIHJldHVybiBmb3VuZFBhcmVudFRoZW1lcztcbn1cblxuZXhwb3J0IHsgcHJvY2Vzc1RoZW1lUmVzb3VyY2VzLCBleHRyYWN0VGhlbWVOYW1lLCBmaW5kUGFyZW50VGhlbWVzIH07XG4iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxccGx1Z2luc1xcXFxhcHBsaWNhdGlvbi10aGVtZS1wbHVnaW5cIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxccGx1Z2luc1xcXFxhcHBsaWNhdGlvbi10aGVtZS1wbHVnaW5cXFxcdGhlbWUtZ2VuZXJhdG9yLmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9sYW15ai9naXQvb3dsY21zNC9vd2xjbXMvdGFyZ2V0L3BsdWdpbnMvYXBwbGljYXRpb24tdGhlbWUtcGx1Z2luL3RoZW1lLWdlbmVyYXRvci5qc1wiOy8qXG4gKiBDb3B5cmlnaHQgMjAwMC0yMDIzIFZhYWRpbiBMdGQuXG4gKlxuICogTGljZW5zZWQgdW5kZXIgdGhlIEFwYWNoZSBMaWNlbnNlLCBWZXJzaW9uIDIuMCAodGhlIFwiTGljZW5zZVwiKTsgeW91IG1heSBub3RcbiAqIHVzZSB0aGlzIGZpbGUgZXhjZXB0IGluIGNvbXBsaWFuY2Ugd2l0aCB0aGUgTGljZW5zZS4gWW91IG1heSBvYnRhaW4gYSBjb3B5IG9mXG4gKiB0aGUgTGljZW5zZSBhdFxuICpcbiAqIGh0dHA6Ly93d3cuYXBhY2hlLm9yZy9saWNlbnNlcy9MSUNFTlNFLTIuMFxuICpcbiAqIFVubGVzcyByZXF1aXJlZCBieSBhcHBsaWNhYmxlIGxhdyBvciBhZ3JlZWQgdG8gaW4gd3JpdGluZywgc29mdHdhcmVcbiAqIGRpc3RyaWJ1dGVkIHVuZGVyIHRoZSBMaWNlbnNlIGlzIGRpc3RyaWJ1dGVkIG9uIGFuIFwiQVMgSVNcIiBCQVNJUywgV0lUSE9VVFxuICogV0FSUkFOVElFUyBPUiBDT05ESVRJT05TIE9GIEFOWSBLSU5ELCBlaXRoZXIgZXhwcmVzcyBvciBpbXBsaWVkLiBTZWUgdGhlXG4gKiBMaWNlbnNlIGZvciB0aGUgc3BlY2lmaWMgbGFuZ3VhZ2UgZ292ZXJuaW5nIHBlcm1pc3Npb25zIGFuZCBsaW1pdGF0aW9ucyB1bmRlclxuICogdGhlIExpY2Vuc2UuXG4gKi9cblxuLyoqXG4gKiBUaGlzIGZpbGUgaGFuZGxlcyB0aGUgZ2VuZXJhdGlvbiBvZiB0aGUgJ1t0aGVtZS1uYW1lXS5qcycgdG9cbiAqIHRoZSB0aGVtZXMvW3RoZW1lLW5hbWVdIGZvbGRlciBhY2NvcmRpbmcgdG8gcHJvcGVydGllcyBmcm9tICd0aGVtZS5qc29uJy5cbiAqL1xuaW1wb3J0IHsgZ2xvYlN5bmMgfSBmcm9tICdnbG9iJztcbmltcG9ydCB7IHJlc29sdmUsIGJhc2VuYW1lIH0gZnJvbSAncGF0aCc7XG5pbXBvcnQgeyBleGlzdHNTeW5jLCByZWFkRmlsZVN5bmMsIHdyaXRlRmlsZVN5bmMgfSBmcm9tICdmcyc7XG5pbXBvcnQgeyBjaGVja01vZHVsZXMgfSBmcm9tICcuL3RoZW1lLWNvcHkuanMnO1xuXG4vLyBTcGVjaWFsIGZvbGRlciBpbnNpZGUgYSB0aGVtZSBmb3IgY29tcG9uZW50IHRoZW1lcyB0aGF0IGdvIGluc2lkZSB0aGUgY29tcG9uZW50IHNoYWRvdyByb290XG5jb25zdCB0aGVtZUNvbXBvbmVudHNGb2xkZXIgPSAnY29tcG9uZW50cyc7XG4vLyBUaGUgY29udGVudHMgb2YgYSBnbG9iYWwgQ1NTIGZpbGUgd2l0aCB0aGlzIG5hbWUgaW4gYSB0aGVtZSBpcyBhbHdheXMgYWRkZWQgdG9cbi8vIHRoZSBkb2N1bWVudC4gRS5nLiBAZm9udC1mYWNlIG11c3QgYmUgaW4gdGhpc1xuY29uc3QgZG9jdW1lbnRDc3NGaWxlbmFtZSA9ICdkb2N1bWVudC5jc3MnO1xuLy8gc3R5bGVzLmNzcyBpcyB0aGUgb25seSBlbnRyeXBvaW50IGNzcyBmaWxlIHdpdGggZG9jdW1lbnQuY3NzLiBFdmVyeXRoaW5nIGVsc2Ugc2hvdWxkIGJlIGltcG9ydGVkIHVzaW5nIGNzcyBAaW1wb3J0XG5jb25zdCBzdHlsZXNDc3NGaWxlbmFtZSA9ICdzdHlsZXMuY3NzJztcblxuY29uc3QgQ1NTSU1QT1JUX0NPTU1FTlQgPSAnQ1NTSW1wb3J0IGVuZCc7XG5jb25zdCBoZWFkZXJJbXBvcnQgPSBgaW1wb3J0ICdjb25zdHJ1Y3Qtc3R5bGUtc2hlZXRzLXBvbHlmaWxsJztcbmA7XG5cbi8qKlxuICogR2VuZXJhdGUgdGhlIFt0aGVtZU5hbWVdLmpzIGZpbGUgZm9yIHRoZW1lRm9sZGVyIHdoaWNoIGNvbGxlY3RzIGFsbCByZXF1aXJlZCBpbmZvcm1hdGlvbiBmcm9tIHRoZSBmb2xkZXIuXG4gKlxuICogQHBhcmFtIHtzdHJpbmd9IHRoZW1lRm9sZGVyIGZvbGRlciBvZiB0aGUgdGhlbWVcbiAqIEBwYXJhbSB7c3RyaW5nfSB0aGVtZU5hbWUgbmFtZSBvZiB0aGUgaGFuZGxlZCB0aGVtZVxuICogQHBhcmFtIHtKU09OfSB0aGVtZVByb3BlcnRpZXMgY29udGVudCBvZiB0aGVtZS5qc29uXG4gKiBAcGFyYW0ge09iamVjdH0gb3B0aW9ucyBidWlsZCBvcHRpb25zIChlLmcuIHByb2Qgb3IgZGV2IG1vZGUpXG4gKiBAcmV0dXJucyB7c3RyaW5nfSB0aGVtZSBmaWxlIGNvbnRlbnRcbiAqL1xuZnVuY3Rpb24gd3JpdGVUaGVtZUZpbGVzKHRoZW1lRm9sZGVyLCB0aGVtZU5hbWUsIHRoZW1lUHJvcGVydGllcywgb3B0aW9ucykge1xuICBjb25zdCBwcm9kdWN0aW9uTW9kZSA9ICFvcHRpb25zLmRldk1vZGU7XG4gIGNvbnN0IHVzZURldlNlcnZlck9ySW5Qcm9kdWN0aW9uTW9kZSA9ICFvcHRpb25zLnVzZURldkJ1bmRsZTtcbiAgY29uc3Qgb3V0cHV0Rm9sZGVyID0gb3B0aW9ucy5mcm9udGVuZEdlbmVyYXRlZEZvbGRlcjtcbiAgY29uc3Qgc3R5bGVzID0gcmVzb2x2ZSh0aGVtZUZvbGRlciwgc3R5bGVzQ3NzRmlsZW5hbWUpO1xuICBjb25zdCBkb2N1bWVudENzc0ZpbGUgPSByZXNvbHZlKHRoZW1lRm9sZGVyLCBkb2N1bWVudENzc0ZpbGVuYW1lKTtcbiAgY29uc3QgYXV0b0luamVjdENvbXBvbmVudHMgPSB0aGVtZVByb3BlcnRpZXMuYXV0b0luamVjdENvbXBvbmVudHMgPz8gdHJ1ZTtcbiAgY29uc3QgZ2xvYmFsRmlsZW5hbWUgPSAndGhlbWUtJyArIHRoZW1lTmFtZSArICcuZ2xvYmFsLmdlbmVyYXRlZC5qcyc7XG4gIGNvbnN0IGNvbXBvbmVudHNGaWxlbmFtZSA9ICd0aGVtZS0nICsgdGhlbWVOYW1lICsgJy5jb21wb25lbnRzLmdlbmVyYXRlZC5qcyc7XG4gIGNvbnN0IHRoZW1lRmlsZW5hbWUgPSAndGhlbWUtJyArIHRoZW1lTmFtZSArICcuZ2VuZXJhdGVkLmpzJztcblxuICBsZXQgdGhlbWVGaWxlQ29udGVudCA9IGhlYWRlckltcG9ydDtcbiAgbGV0IGdsb2JhbEltcG9ydENvbnRlbnQgPSAnLy8gV2hlbiB0aGlzIGZpbGUgaXMgaW1wb3J0ZWQsIGdsb2JhbCBzdHlsZXMgYXJlIGF1dG9tYXRpY2FsbHkgYXBwbGllZFxcbic7XG4gIGxldCBjb21wb25lbnRzRmlsZUNvbnRlbnQgPSAnJztcbiAgdmFyIGNvbXBvbmVudHNGaWxlcztcblxuICBpZiAoYXV0b0luamVjdENvbXBvbmVudHMpIHtcbiAgICBjb21wb25lbnRzRmlsZXMgPSBnbG9iU3luYygnKi5jc3MnLCB7XG4gICAgICBjd2Q6IHJlc29sdmUodGhlbWVGb2xkZXIsIHRoZW1lQ29tcG9uZW50c0ZvbGRlciksXG4gICAgICBub2RpcjogdHJ1ZVxuICAgIH0pO1xuXG4gICAgaWYgKGNvbXBvbmVudHNGaWxlcy5sZW5ndGggPiAwKSB7XG4gICAgICBjb21wb25lbnRzRmlsZUNvbnRlbnQgKz1cbiAgICAgICAgXCJpbXBvcnQgeyB1bnNhZmVDU1MsIHJlZ2lzdGVyU3R5bGVzIH0gZnJvbSAnQHZhYWRpbi92YWFkaW4tdGhlbWFibGUtbWl4aW4vcmVnaXN0ZXItc3R5bGVzJztcXG5cIjtcbiAgICB9XG4gIH1cblxuICBpZiAodGhlbWVQcm9wZXJ0aWVzLnBhcmVudCkge1xuICAgIHRoZW1lRmlsZUNvbnRlbnQgKz0gYGltcG9ydCB7IGFwcGx5VGhlbWUgYXMgYXBwbHlCYXNlVGhlbWUgfSBmcm9tICcuL3RoZW1lLSR7dGhlbWVQcm9wZXJ0aWVzLnBhcmVudH0uZ2VuZXJhdGVkLmpzJztcXG5gO1xuICB9XG5cbiAgdGhlbWVGaWxlQ29udGVudCArPSBgaW1wb3J0IHsgaW5qZWN0R2xvYmFsQ3NzIH0gZnJvbSAnRnJvbnRlbmQvZ2VuZXJhdGVkL2phci1yZXNvdXJjZXMvdGhlbWUtdXRpbC5qcyc7XFxuYDtcbiAgdGhlbWVGaWxlQ29udGVudCArPSBgaW1wb3J0ICcuLyR7Y29tcG9uZW50c0ZpbGVuYW1lfSc7XFxuYDtcblxuICB0aGVtZUZpbGVDb250ZW50ICs9IGBsZXQgbmVlZHNSZWxvYWRPbkNoYW5nZXMgPSBmYWxzZTtcXG5gO1xuICBjb25zdCBpbXBvcnRzID0gW107XG4gIGNvbnN0IGNvbXBvbmVudENzc0ltcG9ydHMgPSBbXTtcbiAgY29uc3QgZ2xvYmFsRmlsZUNvbnRlbnQgPSBbXTtcbiAgY29uc3QgZ2xvYmFsQ3NzQ29kZSA9IFtdO1xuICBjb25zdCBzaGFkb3dPbmx5Q3NzID0gW107XG4gIGNvbnN0IGNvbXBvbmVudENzc0NvZGUgPSBbXTtcbiAgY29uc3QgcGFyZW50VGhlbWUgPSB0aGVtZVByb3BlcnRpZXMucGFyZW50ID8gJ2FwcGx5QmFzZVRoZW1lKHRhcmdldCk7XFxuJyA6ICcnO1xuICBjb25zdCBwYXJlbnRUaGVtZUdsb2JhbEltcG9ydCA9IHRoZW1lUHJvcGVydGllcy5wYXJlbnRcbiAgICA/IGBpbXBvcnQgJy4vdGhlbWUtJHt0aGVtZVByb3BlcnRpZXMucGFyZW50fS5nbG9iYWwuZ2VuZXJhdGVkLmpzJztcXG5gXG4gICAgOiAnJztcblxuICBjb25zdCB0aGVtZUlkZW50aWZpZXIgPSAnX3ZhYWRpbnRoZW1lXycgKyB0aGVtZU5hbWUgKyAnXyc7XG4gIGNvbnN0IGx1bW9Dc3NGbGFnID0gJ192YWFkaW50aGVtZWx1bW9pbXBvcnRzXyc7XG4gIGNvbnN0IGdsb2JhbENzc0ZsYWcgPSB0aGVtZUlkZW50aWZpZXIgKyAnZ2xvYmFsQ3NzJztcbiAgY29uc3QgY29tcG9uZW50Q3NzRmxhZyA9IHRoZW1lSWRlbnRpZmllciArICdjb21wb25lbnRDc3MnO1xuXG4gIGlmICghZXhpc3RzU3luYyhzdHlsZXMpKSB7XG4gICAgaWYgKHByb2R1Y3Rpb25Nb2RlKSB7XG4gICAgICB0aHJvdyBuZXcgRXJyb3IoYHN0eWxlcy5jc3MgZmlsZSBpcyBtaXNzaW5nIGFuZCBpcyBuZWVkZWQgZm9yICcke3RoZW1lTmFtZX0nIGluIGZvbGRlciAnJHt0aGVtZUZvbGRlcn0nYCk7XG4gICAgfVxuICAgIHdyaXRlRmlsZVN5bmMoXG4gICAgICBzdHlsZXMsXG4gICAgICAnLyogSW1wb3J0IHlvdXIgYXBwbGljYXRpb24gZ2xvYmFsIGNzcyBmaWxlcyBoZXJlIG9yIGFkZCB0aGUgc3R5bGVzIGRpcmVjdGx5IHRvIHRoaXMgZmlsZSAqLycsXG4gICAgICAndXRmOCdcbiAgICApO1xuICB9XG5cbiAgLy8gc3R5bGVzLmNzcyB3aWxsIGFsd2F5cyBiZSBhdmFpbGFibGUgYXMgd2Ugd3JpdGUgb25lIGlmIGl0IGRvZXNuJ3QgZXhpc3QuXG4gIGxldCBmaWxlbmFtZSA9IGJhc2VuYW1lKHN0eWxlcyk7XG4gIGxldCB2YXJpYWJsZSA9IGNhbWVsQ2FzZShmaWxlbmFtZSk7XG5cbiAgLyogTFVNTyAqL1xuICBjb25zdCBsdW1vSW1wb3J0cyA9IHRoZW1lUHJvcGVydGllcy5sdW1vSW1wb3J0cyB8fCBbJ2NvbG9yJywgJ3R5cG9ncmFwaHknXTtcbiAgaWYgKGx1bW9JbXBvcnRzKSB7XG4gICAgbHVtb0ltcG9ydHMuZm9yRWFjaCgobHVtb0ltcG9ydCkgPT4ge1xuICAgICAgaW1wb3J0cy5wdXNoKGBpbXBvcnQgeyAke2x1bW9JbXBvcnR9IH0gZnJvbSAnQHZhYWRpbi92YWFkaW4tbHVtby1zdHlsZXMvJHtsdW1vSW1wb3J0fS5qcyc7XFxuYCk7XG4gICAgICBpZiAobHVtb0ltcG9ydCA9PT0gJ3V0aWxpdHknIHx8IGx1bW9JbXBvcnQgPT09ICdiYWRnZScgfHwgbHVtb0ltcG9ydCA9PT0gJ3R5cG9ncmFwaHknIHx8IGx1bW9JbXBvcnQgPT09ICdjb2xvcicpIHtcbiAgICAgICAgLy8gSW5qZWN0IGludG8gbWFpbiBkb2N1bWVudCB0aGUgc2FtZSB3YXkgYXMgb3RoZXIgTHVtbyBzdHlsZXMgYXJlIGluamVjdGVkXG4gICAgICAgIGltcG9ydHMucHVzaChgaW1wb3J0ICdAdmFhZGluL3ZhYWRpbi1sdW1vLXN0eWxlcy8ke2x1bW9JbXBvcnR9LWdsb2JhbC5qcyc7XFxuYCk7XG4gICAgICB9XG4gICAgfSk7XG5cbiAgICBsdW1vSW1wb3J0cy5mb3JFYWNoKChsdW1vSW1wb3J0KSA9PiB7XG4gICAgICAvLyBMdW1vIGlzIGluamVjdGVkIHRvIHRoZSBkb2N1bWVudCBieSBMdW1vIGl0c2VsZlxuICAgICAgc2hhZG93T25seUNzcy5wdXNoKGByZW1vdmVycy5wdXNoKGluamVjdEdsb2JhbENzcygke2x1bW9JbXBvcnR9LmNzc1RleHQsICcnLCB0YXJnZXQsIHRydWUpKTtcXG5gKTtcbiAgICB9KTtcbiAgfVxuXG4gIC8qIFRoZW1lICovXG4gIGlmICh1c2VEZXZTZXJ2ZXJPckluUHJvZHVjdGlvbk1vZGUpIHtcbiAgICBnbG9iYWxGaWxlQ29udGVudC5wdXNoKHBhcmVudFRoZW1lR2xvYmFsSW1wb3J0KTtcbiAgICBnbG9iYWxGaWxlQ29udGVudC5wdXNoKGBpbXBvcnQgJ3RoZW1lcy8ke3RoZW1lTmFtZX0vJHtmaWxlbmFtZX0nO1xcbmApO1xuXG4gICAgaW1wb3J0cy5wdXNoKGBpbXBvcnQgJHt2YXJpYWJsZX0gZnJvbSAndGhlbWVzLyR7dGhlbWVOYW1lfS8ke2ZpbGVuYW1lfT9pbmxpbmUnO1xcbmApO1xuICAgIHNoYWRvd09ubHlDc3MucHVzaChgcmVtb3ZlcnMucHVzaChpbmplY3RHbG9iYWxDc3MoJHt2YXJpYWJsZX0udG9TdHJpbmcoKSwgJycsIHRhcmdldCkpO1xcbiAgICBgKTtcbiAgfVxuICBpZiAoZXhpc3RzU3luYyhkb2N1bWVudENzc0ZpbGUpKSB7XG4gICAgZmlsZW5hbWUgPSBiYXNlbmFtZShkb2N1bWVudENzc0ZpbGUpO1xuICAgIHZhcmlhYmxlID0gY2FtZWxDYXNlKGZpbGVuYW1lKTtcblxuICAgIGlmICh1c2VEZXZTZXJ2ZXJPckluUHJvZHVjdGlvbk1vZGUpIHtcbiAgICAgIGdsb2JhbEZpbGVDb250ZW50LnB1c2goYGltcG9ydCAndGhlbWVzLyR7dGhlbWVOYW1lfS8ke2ZpbGVuYW1lfSc7XFxuYCk7XG5cbiAgICAgIGltcG9ydHMucHVzaChgaW1wb3J0ICR7dmFyaWFibGV9IGZyb20gJ3RoZW1lcy8ke3RoZW1lTmFtZX0vJHtmaWxlbmFtZX0/aW5saW5lJztcXG5gKTtcbiAgICAgIHNoYWRvd09ubHlDc3MucHVzaChgcmVtb3ZlcnMucHVzaChpbmplY3RHbG9iYWxDc3MoJHt2YXJpYWJsZX0udG9TdHJpbmcoKSwnJywgZG9jdW1lbnQpKTtcXG4gICAgYCk7XG4gICAgfVxuICB9XG5cbiAgbGV0IGkgPSAwO1xuICBpZiAodGhlbWVQcm9wZXJ0aWVzLmRvY3VtZW50Q3NzKSB7XG4gICAgY29uc3QgbWlzc2luZ01vZHVsZXMgPSBjaGVja01vZHVsZXModGhlbWVQcm9wZXJ0aWVzLmRvY3VtZW50Q3NzKTtcbiAgICBpZiAobWlzc2luZ01vZHVsZXMubGVuZ3RoID4gMCkge1xuICAgICAgdGhyb3cgRXJyb3IoXG4gICAgICAgIFwiTWlzc2luZyBucG0gbW9kdWxlcyBvciBmaWxlcyAnXCIgK1xuICAgICAgICAgIG1pc3NpbmdNb2R1bGVzLmpvaW4oXCInLCAnXCIpICtcbiAgICAgICAgICBcIicgZm9yIGRvY3VtZW50Q3NzIG1hcmtlZCBpbiAndGhlbWUuanNvbicuXFxuXCIgK1xuICAgICAgICAgIFwiSW5zdGFsbCBvciB1cGRhdGUgcGFja2FnZShzKSBieSBhZGRpbmcgYSBATnBtUGFja2FnZSBhbm5vdGF0aW9uIG9yIGluc3RhbGwgaXQgdXNpbmcgJ25wbS9wbnBtL2J1biBpJ1wiXG4gICAgICApO1xuICAgIH1cbiAgICB0aGVtZVByb3BlcnRpZXMuZG9jdW1lbnRDc3MuZm9yRWFjaCgoY3NzSW1wb3J0KSA9PiB7XG4gICAgICBjb25zdCB2YXJpYWJsZSA9ICdtb2R1bGUnICsgaSsrO1xuICAgICAgaW1wb3J0cy5wdXNoKGBpbXBvcnQgJHt2YXJpYWJsZX0gZnJvbSAnJHtjc3NJbXBvcnR9P2lubGluZSc7XFxuYCk7XG4gICAgICAvLyBEdWUgdG8gY2hyb21lIGJ1ZyBodHRwczovL2J1Z3MuY2hyb21pdW0ub3JnL3AvY2hyb21pdW0vaXNzdWVzL2RldGFpbD9pZD0zMzY4NzYgZm9udC1mYWNlIHdpbGwgbm90IHdvcmtcbiAgICAgIC8vIGluc2lkZSBzaGFkb3dSb290IHNvIHdlIG5lZWQgdG8gaW5qZWN0IGl0IHRoZXJlIGFsc28uXG4gICAgICBnbG9iYWxDc3NDb2RlLnB1c2goYGlmKHRhcmdldCAhPT0gZG9jdW1lbnQpIHtcbiAgICAgICAgcmVtb3ZlcnMucHVzaChpbmplY3RHbG9iYWxDc3MoJHt2YXJpYWJsZX0udG9TdHJpbmcoKSwgJycsIHRhcmdldCkpO1xuICAgIH1cXG4gICAgYCk7XG4gICAgICBnbG9iYWxDc3NDb2RlLnB1c2goXG4gICAgICAgIGByZW1vdmVycy5wdXNoKGluamVjdEdsb2JhbENzcygke3ZhcmlhYmxlfS50b1N0cmluZygpLCAnJHtDU1NJTVBPUlRfQ09NTUVOVH0nLCBkb2N1bWVudCkpO1xcbiAgICBgXG4gICAgICApO1xuICAgIH0pO1xuICB9XG4gIGlmICh0aGVtZVByb3BlcnRpZXMuaW1wb3J0Q3NzKSB7XG4gICAgY29uc3QgbWlzc2luZ01vZHVsZXMgPSBjaGVja01vZHVsZXModGhlbWVQcm9wZXJ0aWVzLmltcG9ydENzcyk7XG4gICAgaWYgKG1pc3NpbmdNb2R1bGVzLmxlbmd0aCA+IDApIHtcbiAgICAgIHRocm93IEVycm9yKFxuICAgICAgICBcIk1pc3NpbmcgbnBtIG1vZHVsZXMgb3IgZmlsZXMgJ1wiICtcbiAgICAgICAgICBtaXNzaW5nTW9kdWxlcy5qb2luKFwiJywgJ1wiKSArXG4gICAgICAgICAgXCInIGZvciBpbXBvcnRDc3MgbWFya2VkIGluICd0aGVtZS5qc29uJy5cXG5cIiArXG4gICAgICAgICAgXCJJbnN0YWxsIG9yIHVwZGF0ZSBwYWNrYWdlKHMpIGJ5IGFkZGluZyBhIEBOcG1QYWNrYWdlIGFubm90YXRpb24gb3IgaW5zdGFsbCBpdCB1c2luZyAnbnBtL3BucG0vYnVuIGknXCJcbiAgICAgICk7XG4gICAgfVxuICAgIHRoZW1lUHJvcGVydGllcy5pbXBvcnRDc3MuZm9yRWFjaCgoY3NzUGF0aCkgPT4ge1xuICAgICAgY29uc3QgdmFyaWFibGUgPSAnbW9kdWxlJyArIGkrKztcbiAgICAgIGdsb2JhbEZpbGVDb250ZW50LnB1c2goYGltcG9ydCAnJHtjc3NQYXRofSc7XFxuYCk7XG4gICAgICBpbXBvcnRzLnB1c2goYGltcG9ydCAke3ZhcmlhYmxlfSBmcm9tICcke2Nzc1BhdGh9P2lubGluZSc7XFxuYCk7XG4gICAgICBzaGFkb3dPbmx5Q3NzLnB1c2goYHJlbW92ZXJzLnB1c2goaW5qZWN0R2xvYmFsQ3NzKCR7dmFyaWFibGV9LnRvU3RyaW5nKCksICcke0NTU0lNUE9SVF9DT01NRU5UfScsIHRhcmdldCkpO1xcbmApO1xuICAgIH0pO1xuICB9XG5cbiAgaWYgKGF1dG9JbmplY3RDb21wb25lbnRzKSB7XG4gICAgY29tcG9uZW50c0ZpbGVzLmZvckVhY2goKGNvbXBvbmVudENzcykgPT4ge1xuICAgICAgY29uc3QgZmlsZW5hbWUgPSBiYXNlbmFtZShjb21wb25lbnRDc3MpO1xuICAgICAgY29uc3QgdGFnID0gZmlsZW5hbWUucmVwbGFjZSgnLmNzcycsICcnKTtcbiAgICAgIGNvbnN0IHZhcmlhYmxlID0gY2FtZWxDYXNlKGZpbGVuYW1lKTtcbiAgICAgIGNvbXBvbmVudENzc0ltcG9ydHMucHVzaChcbiAgICAgICAgYGltcG9ydCAke3ZhcmlhYmxlfSBmcm9tICd0aGVtZXMvJHt0aGVtZU5hbWV9LyR7dGhlbWVDb21wb25lbnRzRm9sZGVyfS8ke2ZpbGVuYW1lfT9pbmxpbmUnO1xcbmBcbiAgICAgICk7XG4gICAgICAvLyBEb24ndCBmb3JtYXQgYXMgdGhlIGdlbmVyYXRlZCBmaWxlIGZvcm1hdHRpbmcgd2lsbCBnZXQgd29ua3khXG4gICAgICBjb25zdCBjb21wb25lbnRTdHJpbmcgPSBgcmVnaXN0ZXJTdHlsZXMoXG4gICAgICAgICcke3RhZ30nLFxuICAgICAgICB1bnNhZmVDU1MoJHt2YXJpYWJsZX0udG9TdHJpbmcoKSlcbiAgICAgICk7XG4gICAgICBgO1xuICAgICAgY29tcG9uZW50Q3NzQ29kZS5wdXNoKGNvbXBvbmVudFN0cmluZyk7XG4gICAgfSk7XG4gIH1cblxuICB0aGVtZUZpbGVDb250ZW50ICs9IGltcG9ydHMuam9pbignJyk7XG5cbiAgLy8gRG9uJ3QgZm9ybWF0IGFzIHRoZSBnZW5lcmF0ZWQgZmlsZSBmb3JtYXR0aW5nIHdpbGwgZ2V0IHdvbmt5IVxuICAvLyBJZiB0YXJnZXRzIGNoZWNrIHRoYXQgd2Ugb25seSByZWdpc3RlciB0aGUgc3R5bGUgcGFydHMgb25jZSwgY2hlY2tzIGV4aXN0IGZvciBnbG9iYWwgY3NzIGFuZCBjb21wb25lbnQgY3NzXG4gIGNvbnN0IHRoZW1lRmlsZUFwcGx5ID0gYFxuICBsZXQgdGhlbWVSZW1vdmVycyA9IG5ldyBXZWFrTWFwKCk7XG4gIGxldCB0YXJnZXRzID0gW107XG5cbiAgZXhwb3J0IGNvbnN0IGFwcGx5VGhlbWUgPSAodGFyZ2V0KSA9PiB7XG4gICAgY29uc3QgcmVtb3ZlcnMgPSBbXTtcbiAgICBpZiAodGFyZ2V0ICE9PSBkb2N1bWVudCkge1xuICAgICAgJHtzaGFkb3dPbmx5Q3NzLmpvaW4oJycpfVxuICAgIH1cbiAgICAke3BhcmVudFRoZW1lfVxuICAgICR7Z2xvYmFsQ3NzQ29kZS5qb2luKCcnKX1cblxuICAgIGlmIChpbXBvcnQubWV0YS5ob3QpIHtcbiAgICAgIHRhcmdldHMucHVzaChuZXcgV2Vha1JlZih0YXJnZXQpKTtcbiAgICAgIHRoZW1lUmVtb3ZlcnMuc2V0KHRhcmdldCwgcmVtb3ZlcnMpO1xuICAgIH1cblxuICB9XG4gIFxuYDtcbiAgY29tcG9uZW50c0ZpbGVDb250ZW50ICs9IGBcbiR7Y29tcG9uZW50Q3NzSW1wb3J0cy5qb2luKCcnKX1cblxuaWYgKCFkb2N1bWVudFsnJHtjb21wb25lbnRDc3NGbGFnfSddKSB7XG4gICR7Y29tcG9uZW50Q3NzQ29kZS5qb2luKCcnKX1cbiAgZG9jdW1lbnRbJyR7Y29tcG9uZW50Q3NzRmxhZ30nXSA9IHRydWU7XG59XG5cbmlmIChpbXBvcnQubWV0YS5ob3QpIHtcbiAgaW1wb3J0Lm1ldGEuaG90LmFjY2VwdCgobW9kdWxlKSA9PiB7XG4gICAgd2luZG93LmxvY2F0aW9uLnJlbG9hZCgpO1xuICB9KTtcbn1cblxuYDtcblxuICB0aGVtZUZpbGVDb250ZW50ICs9IHRoZW1lRmlsZUFwcGx5O1xuICB0aGVtZUZpbGVDb250ZW50ICs9IGBcbmlmIChpbXBvcnQubWV0YS5ob3QpIHtcbiAgaW1wb3J0Lm1ldGEuaG90LmFjY2VwdCgobW9kdWxlKSA9PiB7XG5cbiAgICBpZiAobmVlZHNSZWxvYWRPbkNoYW5nZXMpIHtcbiAgICAgIHdpbmRvdy5sb2NhdGlvbi5yZWxvYWQoKTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGFyZ2V0cy5mb3JFYWNoKHRhcmdldFJlZiA9PiB7XG4gICAgICAgIGNvbnN0IHRhcmdldCA9IHRhcmdldFJlZi5kZXJlZigpO1xuICAgICAgICBpZiAodGFyZ2V0KSB7XG4gICAgICAgICAgdGhlbWVSZW1vdmVycy5nZXQodGFyZ2V0KS5mb3JFYWNoKHJlbW92ZXIgPT4gcmVtb3ZlcigpKVxuICAgICAgICAgIG1vZHVsZS5hcHBseVRoZW1lKHRhcmdldCk7XG4gICAgICAgIH1cbiAgICAgIH0pXG4gICAgfVxuICB9KTtcblxuICBpbXBvcnQubWV0YS5ob3Qub24oJ3ZpdGU6YWZ0ZXJVcGRhdGUnLCAodXBkYXRlKSA9PiB7XG4gICAgZG9jdW1lbnQuZGlzcGF0Y2hFdmVudChuZXcgQ3VzdG9tRXZlbnQoJ3ZhYWRpbi10aGVtZS11cGRhdGVkJywgeyBkZXRhaWw6IHVwZGF0ZSB9KSk7XG4gIH0pO1xufVxuXG5gO1xuXG4gIGdsb2JhbEltcG9ydENvbnRlbnQgKz0gYFxuJHtnbG9iYWxGaWxlQ29udGVudC5qb2luKCcnKX1cbmA7XG5cbiAgd3JpdGVJZkNoYW5nZWQocmVzb2x2ZShvdXRwdXRGb2xkZXIsIGdsb2JhbEZpbGVuYW1lKSwgZ2xvYmFsSW1wb3J0Q29udGVudCk7XG4gIHdyaXRlSWZDaGFuZ2VkKHJlc29sdmUob3V0cHV0Rm9sZGVyLCB0aGVtZUZpbGVuYW1lKSwgdGhlbWVGaWxlQ29udGVudCk7XG4gIHdyaXRlSWZDaGFuZ2VkKHJlc29sdmUob3V0cHV0Rm9sZGVyLCBjb21wb25lbnRzRmlsZW5hbWUpLCBjb21wb25lbnRzRmlsZUNvbnRlbnQpO1xufVxuXG5mdW5jdGlvbiB3cml0ZUlmQ2hhbmdlZChmaWxlLCBkYXRhKSB7XG4gIGlmICghZXhpc3RzU3luYyhmaWxlKSB8fCByZWFkRmlsZVN5bmMoZmlsZSwgeyBlbmNvZGluZzogJ3V0Zi04JyB9KSAhPT0gZGF0YSkge1xuICAgIHdyaXRlRmlsZVN5bmMoZmlsZSwgZGF0YSk7XG4gIH1cbn1cblxuLyoqXG4gKiBNYWtlIGdpdmVuIHN0cmluZyBpbnRvIGNhbWVsQ2FzZS5cbiAqXG4gKiBAcGFyYW0ge3N0cmluZ30gc3RyIHN0cmluZyB0byBtYWtlIGludG8gY2FtZUNhc2VcbiAqIEByZXR1cm5zIHtzdHJpbmd9IGNhbWVsQ2FzZWQgdmVyc2lvblxuICovXG5mdW5jdGlvbiBjYW1lbENhc2Uoc3RyKSB7XG4gIHJldHVybiBzdHJcbiAgICAucmVwbGFjZSgvKD86Xlxcd3xbQS1aXXxcXGJcXHcpL2csIGZ1bmN0aW9uICh3b3JkLCBpbmRleCkge1xuICAgICAgcmV0dXJuIGluZGV4ID09PSAwID8gd29yZC50b0xvd2VyQ2FzZSgpIDogd29yZC50b1VwcGVyQ2FzZSgpO1xuICAgIH0pXG4gICAgLnJlcGxhY2UoL1xccysvZywgJycpXG4gICAgLnJlcGxhY2UoL1xcLnxcXC0vZywgJycpO1xufVxuXG5leHBvcnQgeyB3cml0ZVRoZW1lRmlsZXMgfTtcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxwbHVnaW5zXFxcXGFwcGxpY2F0aW9uLXRoZW1lLXBsdWdpblwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxwbHVnaW5zXFxcXGFwcGxpY2F0aW9uLXRoZW1lLXBsdWdpblxcXFx0aGVtZS1jb3B5LmpzXCI7Y29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2ltcG9ydF9tZXRhX3VybCA9IFwiZmlsZTovLy9DOi9Vc2Vycy9sYW15ai9naXQvb3dsY21zNC9vd2xjbXMvdGFyZ2V0L3BsdWdpbnMvYXBwbGljYXRpb24tdGhlbWUtcGx1Z2luL3RoZW1lLWNvcHkuanNcIjsvKlxuICogQ29weXJpZ2h0IDIwMDAtMjAyMyBWYWFkaW4gTHRkLlxuICpcbiAqIExpY2Vuc2VkIHVuZGVyIHRoZSBBcGFjaGUgTGljZW5zZSwgVmVyc2lvbiAyLjAgKHRoZSBcIkxpY2Vuc2VcIik7IHlvdSBtYXkgbm90XG4gKiB1c2UgdGhpcyBmaWxlIGV4Y2VwdCBpbiBjb21wbGlhbmNlIHdpdGggdGhlIExpY2Vuc2UuIFlvdSBtYXkgb2J0YWluIGEgY29weSBvZlxuICogdGhlIExpY2Vuc2UgYXRcbiAqXG4gKiBodHRwOi8vd3d3LmFwYWNoZS5vcmcvbGljZW5zZXMvTElDRU5TRS0yLjBcbiAqXG4gKiBVbmxlc3MgcmVxdWlyZWQgYnkgYXBwbGljYWJsZSBsYXcgb3IgYWdyZWVkIHRvIGluIHdyaXRpbmcsIHNvZnR3YXJlXG4gKiBkaXN0cmlidXRlZCB1bmRlciB0aGUgTGljZW5zZSBpcyBkaXN0cmlidXRlZCBvbiBhbiBcIkFTIElTXCIgQkFTSVMsIFdJVEhPVVRcbiAqIFdBUlJBTlRJRVMgT1IgQ09ORElUSU9OUyBPRiBBTlkgS0lORCwgZWl0aGVyIGV4cHJlc3Mgb3IgaW1wbGllZC4gU2VlIHRoZVxuICogTGljZW5zZSBmb3IgdGhlIHNwZWNpZmljIGxhbmd1YWdlIGdvdmVybmluZyBwZXJtaXNzaW9ucyBhbmQgbGltaXRhdGlvbnMgdW5kZXJcbiAqIHRoZSBMaWNlbnNlLlxuICovXG5cbi8qKlxuICogVGhpcyBjb250YWlucyBmdW5jdGlvbnMgYW5kIGZlYXR1cmVzIHVzZWQgdG8gY29weSB0aGVtZSBmaWxlcy5cbiAqL1xuXG5pbXBvcnQgeyByZWFkZGlyU3luYywgc3RhdFN5bmMsIG1rZGlyU3luYywgZXhpc3RzU3luYywgY29weUZpbGVTeW5jIH0gZnJvbSAnZnMnO1xuaW1wb3J0IHsgcmVzb2x2ZSwgYmFzZW5hbWUsIHJlbGF0aXZlLCBleHRuYW1lIH0gZnJvbSAncGF0aCc7XG5pbXBvcnQgeyBnbG9iU3luYyB9IGZyb20gJ2dsb2InO1xuXG5jb25zdCBpZ25vcmVkRmlsZUV4dGVuc2lvbnMgPSBbJy5jc3MnLCAnLmpzJywgJy5qc29uJ107XG5cbi8qKlxuICogQ29weSB0aGVtZSBzdGF0aWMgcmVzb3VyY2VzIHRvIHN0YXRpYyBhc3NldHMgZm9sZGVyLiBBbGwgZmlsZXMgaW4gdGhlIHRoZW1lXG4gKiBmb2xkZXIgd2lsbCBiZSBjb3BpZWQgZXhjbHVkaW5nIGNzcywganMgYW5kIGpzb24gZmlsZXMgdGhhdCB3aWxsIGJlXG4gKiBoYW5kbGVkIGJ5IHdlYnBhY2sgYW5kIG5vdCBiZSBzaGFyZWQgYXMgc3RhdGljIGZpbGVzLlxuICpcbiAqIEBwYXJhbSB7c3RyaW5nfSB0aGVtZUZvbGRlciBGb2xkZXIgd2l0aCB0aGVtZSBmaWxlXG4gKiBAcGFyYW0ge3N0cmluZ30gcHJvamVjdFN0YXRpY0Fzc2V0c091dHB1dEZvbGRlciByZXNvdXJjZXMgb3V0cHV0IGZvbGRlclxuICogQHBhcmFtIHtvYmplY3R9IGxvZ2dlciBwbHVnaW4gbG9nZ2VyXG4gKi9cbmZ1bmN0aW9uIGNvcHlUaGVtZVJlc291cmNlcyh0aGVtZUZvbGRlciwgcHJvamVjdFN0YXRpY0Fzc2V0c091dHB1dEZvbGRlciwgbG9nZ2VyKSB7XG4gIGNvbnN0IHN0YXRpY0Fzc2V0c1RoZW1lRm9sZGVyID0gcmVzb2x2ZShwcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyLCAndGhlbWVzJywgYmFzZW5hbWUodGhlbWVGb2xkZXIpKTtcbiAgY29uc3QgY29sbGVjdGlvbiA9IGNvbGxlY3RGb2xkZXJzKHRoZW1lRm9sZGVyLCBsb2dnZXIpO1xuXG4gIC8vIE9ubHkgY3JlYXRlIGFzc2V0cyBmb2xkZXIgaWYgdGhlcmUgYXJlIGZpbGVzIHRvIGNvcHkuXG4gIGlmIChjb2xsZWN0aW9uLmZpbGVzLmxlbmd0aCA+IDApIHtcbiAgICBta2RpclN5bmMoc3RhdGljQXNzZXRzVGhlbWVGb2xkZXIsIHsgcmVjdXJzaXZlOiB0cnVlIH0pO1xuICAgIC8vIGNyZWF0ZSBmb2xkZXJzIHdpdGhcbiAgICBjb2xsZWN0aW9uLmRpcmVjdG9yaWVzLmZvckVhY2goKGRpcmVjdG9yeSkgPT4ge1xuICAgICAgY29uc3QgcmVsYXRpdmVEaXJlY3RvcnkgPSByZWxhdGl2ZSh0aGVtZUZvbGRlciwgZGlyZWN0b3J5KTtcbiAgICAgIGNvbnN0IHRhcmdldERpcmVjdG9yeSA9IHJlc29sdmUoc3RhdGljQXNzZXRzVGhlbWVGb2xkZXIsIHJlbGF0aXZlRGlyZWN0b3J5KTtcblxuICAgICAgbWtkaXJTeW5jKHRhcmdldERpcmVjdG9yeSwgeyByZWN1cnNpdmU6IHRydWUgfSk7XG4gICAgfSk7XG5cbiAgICBjb2xsZWN0aW9uLmZpbGVzLmZvckVhY2goKGZpbGUpID0+IHtcbiAgICAgIGNvbnN0IHJlbGF0aXZlRmlsZSA9IHJlbGF0aXZlKHRoZW1lRm9sZGVyLCBmaWxlKTtcbiAgICAgIGNvbnN0IHRhcmdldEZpbGUgPSByZXNvbHZlKHN0YXRpY0Fzc2V0c1RoZW1lRm9sZGVyLCByZWxhdGl2ZUZpbGUpO1xuICAgICAgY29weUZpbGVJZkFic2VudE9yTmV3ZXIoZmlsZSwgdGFyZ2V0RmlsZSwgbG9nZ2VyKTtcbiAgICB9KTtcbiAgfVxufVxuXG4vKipcbiAqIENvbGxlY3QgYWxsIGZvbGRlcnMgd2l0aCBjb3B5YWJsZSBmaWxlcyBhbmQgYWxsIGZpbGVzIHRvIGJlIGNvcGllZC5cbiAqIEZvbGVkIHdpbGwgbm90IGJlIGFkZGVkIGlmIG5vIGZpbGVzIGluIGZvbGRlciBvciBzdWJmb2xkZXJzLlxuICpcbiAqIEZpbGVzIHdpbGwgbm90IGNvbnRhaW4gZmlsZXMgd2l0aCBpZ25vcmVkIGV4dGVuc2lvbnMgYW5kIGZvbGRlcnMgb25seSBjb250YWluaW5nIGlnbm9yZWQgZmlsZXMgd2lsbCBub3QgYmUgYWRkZWQuXG4gKlxuICogQHBhcmFtIGZvbGRlclRvQ29weSBmb2xkZXIgd2Ugd2lsbCBjb3B5IGZpbGVzIGZyb21cbiAqIEBwYXJhbSBsb2dnZXIgcGx1Z2luIGxvZ2dlclxuICogQHJldHVybiB7e2RpcmVjdG9yaWVzOiBbXSwgZmlsZXM6IFtdfX0gb2JqZWN0IGNvbnRhaW5pbmcgZGlyZWN0b3JpZXMgdG8gY3JlYXRlIGFuZCBmaWxlcyB0byBjb3B5XG4gKi9cbmZ1bmN0aW9uIGNvbGxlY3RGb2xkZXJzKGZvbGRlclRvQ29weSwgbG9nZ2VyKSB7XG4gIGNvbnN0IGNvbGxlY3Rpb24gPSB7IGRpcmVjdG9yaWVzOiBbXSwgZmlsZXM6IFtdIH07XG4gIGxvZ2dlci50cmFjZSgnZmlsZXMgaW4gZGlyZWN0b3J5JywgcmVhZGRpclN5bmMoZm9sZGVyVG9Db3B5KSk7XG4gIHJlYWRkaXJTeW5jKGZvbGRlclRvQ29weSkuZm9yRWFjaCgoZmlsZSkgPT4ge1xuICAgIGNvbnN0IGZpbGVUb0NvcHkgPSByZXNvbHZlKGZvbGRlclRvQ29weSwgZmlsZSk7XG4gICAgdHJ5IHtcbiAgICAgIGlmIChzdGF0U3luYyhmaWxlVG9Db3B5KS5pc0RpcmVjdG9yeSgpKSB7XG4gICAgICAgIGxvZ2dlci5kZWJ1ZygnR29pbmcgdGhyb3VnaCBkaXJlY3RvcnknLCBmaWxlVG9Db3B5KTtcbiAgICAgICAgY29uc3QgcmVzdWx0ID0gY29sbGVjdEZvbGRlcnMoZmlsZVRvQ29weSwgbG9nZ2VyKTtcbiAgICAgICAgaWYgKHJlc3VsdC5maWxlcy5sZW5ndGggPiAwKSB7XG4gICAgICAgICAgY29sbGVjdGlvbi5kaXJlY3Rvcmllcy5wdXNoKGZpbGVUb0NvcHkpO1xuICAgICAgICAgIGxvZ2dlci5kZWJ1ZygnQWRkaW5nIGRpcmVjdG9yeScsIGZpbGVUb0NvcHkpO1xuICAgICAgICAgIGNvbGxlY3Rpb24uZGlyZWN0b3JpZXMucHVzaC5hcHBseShjb2xsZWN0aW9uLmRpcmVjdG9yaWVzLCByZXN1bHQuZGlyZWN0b3JpZXMpO1xuICAgICAgICAgIGNvbGxlY3Rpb24uZmlsZXMucHVzaC5hcHBseShjb2xsZWN0aW9uLmZpbGVzLCByZXN1bHQuZmlsZXMpO1xuICAgICAgICB9XG4gICAgICB9IGVsc2UgaWYgKCFpZ25vcmVkRmlsZUV4dGVuc2lvbnMuaW5jbHVkZXMoZXh0bmFtZShmaWxlVG9Db3B5KSkpIHtcbiAgICAgICAgbG9nZ2VyLmRlYnVnKCdBZGRpbmcgZmlsZScsIGZpbGVUb0NvcHkpO1xuICAgICAgICBjb2xsZWN0aW9uLmZpbGVzLnB1c2goZmlsZVRvQ29weSk7XG4gICAgICB9XG4gICAgfSBjYXRjaCAoZXJyb3IpIHtcbiAgICAgIGhhbmRsZU5vU3VjaEZpbGVFcnJvcihmaWxlVG9Db3B5LCBlcnJvciwgbG9nZ2VyKTtcbiAgICB9XG4gIH0pO1xuICByZXR1cm4gY29sbGVjdGlvbjtcbn1cblxuLyoqXG4gKiBDb3B5IGFueSBzdGF0aWMgbm9kZV9tb2R1bGVzIGFzc2V0cyBtYXJrZWQgaW4gdGhlbWUuanNvbiB0b1xuICogcHJvamVjdCBzdGF0aWMgYXNzZXRzIGZvbGRlci5cbiAqXG4gKiBUaGUgdGhlbWUuanNvbiBjb250ZW50IGZvciBhc3NldHMgaXMgc2V0IHVwIGFzOlxuICoge1xuICogICBhc3NldHM6IHtcbiAqICAgICBcIm5vZGVfbW9kdWxlIGlkZW50aWZpZXJcIjoge1xuICogICAgICAgXCJjb3B5LXJ1bGVcIjogXCJ0YXJnZXQvZm9sZGVyXCIsXG4gKiAgICAgfVxuICogICB9XG4gKiB9XG4gKlxuICogVGhpcyB3b3VsZCBtZWFuIHRoYXQgYW4gYXNzZXQgd291bGQgYmUgYnVpbHQgYXM6XG4gKiBcIkBmb3J0YXdlc29tZS9mb250YXdlc29tZS1mcmVlXCI6IHtcbiAqICAgXCJzdmdzL3JlZ3VsYXIvKipcIjogXCJmb3J0YXdlc29tZS9pY29uc1wiXG4gKiB9XG4gKiBXaGVyZSAnQGZvcnRhd2Vzb21lL2ZvbnRhd2Vzb21lLWZyZWUnIGlzIHRoZSBucG0gcGFja2FnZSwgJ3N2Z3MvcmVndWxhci8qKicgaXMgd2hhdCBzaG91bGQgYmUgY29waWVkXG4gKiBhbmQgJ2ZvcnRhd2Vzb21lL2ljb25zJyBpcyB0aGUgdGFyZ2V0IGRpcmVjdG9yeSB1bmRlciBwcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyIHdoZXJlIHRoaW5nc1xuICogd2lsbCBnZXQgY29waWVkIHRvLlxuICpcbiAqIE5vdGUhIHRoZXJlIGNhbiBiZSBtdWx0aXBsZSBjb3B5LXJ1bGVzIHdpdGggdGFyZ2V0IGZvbGRlcnMgZm9yIG9uZSBucG0gcGFja2FnZSBhc3NldC5cbiAqXG4gKiBAcGFyYW0ge3N0cmluZ30gdGhlbWVOYW1lIG5hbWUgb2YgdGhlIHRoZW1lIHdlIGFyZSBjb3B5aW5nIGFzc2V0cyBmb3JcbiAqIEBwYXJhbSB7anNvbn0gdGhlbWVQcm9wZXJ0aWVzIHRoZW1lIHByb3BlcnRpZXMganNvbiB3aXRoIGRhdGEgb24gYXNzZXRzXG4gKiBAcGFyYW0ge3N0cmluZ30gcHJvamVjdFN0YXRpY0Fzc2V0c091dHB1dEZvbGRlciBwcm9qZWN0IG91dHB1dCBmb2xkZXIgd2hlcmUgd2UgY29weSBhc3NldHMgdG8gdW5kZXIgdGhlbWUvW3RoZW1lTmFtZV1cbiAqIEBwYXJhbSB7b2JqZWN0fSBsb2dnZXIgcGx1Z2luIGxvZ2dlclxuICovXG5mdW5jdGlvbiBjb3B5U3RhdGljQXNzZXRzKHRoZW1lTmFtZSwgdGhlbWVQcm9wZXJ0aWVzLCBwcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyLCBsb2dnZXIpIHtcbiAgY29uc3QgYXNzZXRzID0gdGhlbWVQcm9wZXJ0aWVzWydhc3NldHMnXTtcbiAgaWYgKCFhc3NldHMpIHtcbiAgICBsb2dnZXIuZGVidWcoJ25vIGFzc2V0cyB0byBoYW5kbGUgbm8gc3RhdGljIGFzc2V0cyB3ZXJlIGNvcGllZCcpO1xuICAgIHJldHVybjtcbiAgfVxuXG4gIG1rZGlyU3luYyhwcm9qZWN0U3RhdGljQXNzZXRzT3V0cHV0Rm9sZGVyLCB7XG4gICAgcmVjdXJzaXZlOiB0cnVlXG4gIH0pO1xuICBjb25zdCBtaXNzaW5nTW9kdWxlcyA9IGNoZWNrTW9kdWxlcyhPYmplY3Qua2V5cyhhc3NldHMpKTtcbiAgaWYgKG1pc3NpbmdNb2R1bGVzLmxlbmd0aCA+IDApIHtcbiAgICB0aHJvdyBFcnJvcihcbiAgICAgIFwiTWlzc2luZyBucG0gbW9kdWxlcyAnXCIgK1xuICAgICAgICBtaXNzaW5nTW9kdWxlcy5qb2luKFwiJywgJ1wiKSArXG4gICAgICAgIFwiJyBmb3IgYXNzZXRzIG1hcmtlZCBpbiAndGhlbWUuanNvbicuXFxuXCIgK1xuICAgICAgICBcIkluc3RhbGwgcGFja2FnZShzKSBieSBhZGRpbmcgYSBATnBtUGFja2FnZSBhbm5vdGF0aW9uIG9yIGluc3RhbGwgaXQgdXNpbmcgJ25wbS9wbnBtL2J1biBpJ1wiXG4gICAgKTtcbiAgfVxuICBPYmplY3Qua2V5cyhhc3NldHMpLmZvckVhY2goKG1vZHVsZSkgPT4ge1xuICAgIGNvbnN0IGNvcHlSdWxlcyA9IGFzc2V0c1ttb2R1bGVdO1xuICAgIE9iamVjdC5rZXlzKGNvcHlSdWxlcykuZm9yRWFjaCgoY29weVJ1bGUpID0+IHtcbiAgICAgIGNvbnN0IG5vZGVTb3VyY2VzID0gcmVzb2x2ZSgnbm9kZV9tb2R1bGVzLycsIG1vZHVsZSwgY29weVJ1bGUpO1xuICAgICAgY29uc3QgZmlsZXMgPSBnbG9iU3luYyhub2RlU291cmNlcywgeyBub2RpcjogdHJ1ZSB9KTtcbiAgICAgIGNvbnN0IHRhcmdldEZvbGRlciA9IHJlc29sdmUocHJvamVjdFN0YXRpY0Fzc2V0c091dHB1dEZvbGRlciwgJ3RoZW1lcycsIHRoZW1lTmFtZSwgY29weVJ1bGVzW2NvcHlSdWxlXSk7XG5cbiAgICAgIG1rZGlyU3luYyh0YXJnZXRGb2xkZXIsIHtcbiAgICAgICAgcmVjdXJzaXZlOiB0cnVlXG4gICAgICB9KTtcbiAgICAgIGZpbGVzLmZvckVhY2goKGZpbGUpID0+IHtcbiAgICAgICAgY29uc3QgY29weVRhcmdldCA9IHJlc29sdmUodGFyZ2V0Rm9sZGVyLCBiYXNlbmFtZShmaWxlKSk7XG4gICAgICAgIGNvcHlGaWxlSWZBYnNlbnRPck5ld2VyKGZpbGUsIGNvcHlUYXJnZXQsIGxvZ2dlcik7XG4gICAgICB9KTtcbiAgICB9KTtcbiAgfSk7XG59XG5cbmZ1bmN0aW9uIGNoZWNrTW9kdWxlcyhtb2R1bGVzKSB7XG4gIGNvbnN0IG1pc3NpbmcgPSBbXTtcblxuICBtb2R1bGVzLmZvckVhY2goKG1vZHVsZSkgPT4ge1xuICAgIGlmICghZXhpc3RzU3luYyhyZXNvbHZlKCdub2RlX21vZHVsZXMvJywgbW9kdWxlKSkpIHtcbiAgICAgIG1pc3NpbmcucHVzaChtb2R1bGUpO1xuICAgIH1cbiAgfSk7XG5cbiAgcmV0dXJuIG1pc3Npbmc7XG59XG5cbi8qKlxuICogQ29waWVzIGdpdmVuIGZpbGUgdG8gYSBnaXZlbiB0YXJnZXQgcGF0aCwgaWYgdGFyZ2V0IGZpbGUgZG9lc24ndCBleGlzdCBvciBpZlxuICogZmlsZSB0byBjb3B5IGlzIG5ld2VyLlxuICogQHBhcmFtIHtzdHJpbmd9IGZpbGVUb0NvcHkgcGF0aCBvZiB0aGUgZmlsZSB0byBjb3B5XG4gKiBAcGFyYW0ge3N0cmluZ30gY29weVRhcmdldCBwYXRoIG9mIHRoZSB0YXJnZXQgZmlsZVxuICogQHBhcmFtIHtvYmplY3R9IGxvZ2dlciBwbHVnaW4gbG9nZ2VyXG4gKi9cbmZ1bmN0aW9uIGNvcHlGaWxlSWZBYnNlbnRPck5ld2VyKGZpbGVUb0NvcHksIGNvcHlUYXJnZXQsIGxvZ2dlcikge1xuICB0cnkge1xuICAgIGlmICghZXhpc3RzU3luYyhjb3B5VGFyZ2V0KSB8fCBzdGF0U3luYyhjb3B5VGFyZ2V0KS5tdGltZSA8IHN0YXRTeW5jKGZpbGVUb0NvcHkpLm10aW1lKSB7XG4gICAgICBsb2dnZXIudHJhY2UoJ0NvcHlpbmc6ICcsIGZpbGVUb0NvcHksICc9PicsIGNvcHlUYXJnZXQpO1xuICAgICAgY29weUZpbGVTeW5jKGZpbGVUb0NvcHksIGNvcHlUYXJnZXQpO1xuICAgIH1cbiAgfSBjYXRjaCAoZXJyb3IpIHtcbiAgICBoYW5kbGVOb1N1Y2hGaWxlRXJyb3IoZmlsZVRvQ29weSwgZXJyb3IsIGxvZ2dlcik7XG4gIH1cbn1cblxuLy8gSWdub3JlcyBlcnJvcnMgZHVlIHRvIGZpbGUgbWlzc2luZyBkdXJpbmcgdGhlbWUgcHJvY2Vzc2luZ1xuLy8gVGhpcyBtYXkgaGFwcGVuIGZvciBleGFtcGxlIHdoZW4gYW4gSURFIGNyZWF0ZXMgYSB0ZW1wb3JhcnkgZmlsZVxuLy8gYW5kIHRoZW4gaW1tZWRpYXRlbHkgZGVsZXRlcyBpdFxuZnVuY3Rpb24gaGFuZGxlTm9TdWNoRmlsZUVycm9yKGZpbGUsIGVycm9yLCBsb2dnZXIpIHtcbiAgaWYgKGVycm9yLmNvZGUgPT09ICdFTk9FTlQnKSB7XG4gICAgbG9nZ2VyLndhcm4oJ0lnbm9yaW5nIG5vdCBleGlzdGluZyBmaWxlICcgKyBmaWxlICsgJy4gRmlsZSBtYXkgaGF2ZSBiZWVuIGRlbGV0ZWQgZHVyaW5nIHRoZW1lIHByb2Nlc3NpbmcuJyk7XG4gIH0gZWxzZSB7XG4gICAgdGhyb3cgZXJyb3I7XG4gIH1cbn1cblxuZXhwb3J0IHsgY2hlY2tNb2R1bGVzLCBjb3B5U3RhdGljQXNzZXRzLCBjb3B5VGhlbWVSZXNvdXJjZXMgfTtcbiIsICJjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZGlybmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxwbHVnaW5zXFxcXHRoZW1lLWxvYWRlclwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxwbHVnaW5zXFxcXHRoZW1lLWxvYWRlclxcXFx0aGVtZS1sb2FkZXItdXRpbHMuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0M6L1VzZXJzL2xhbXlqL2dpdC9vd2xjbXM0L293bGNtcy90YXJnZXQvcGx1Z2lucy90aGVtZS1sb2FkZXIvdGhlbWUtbG9hZGVyLXV0aWxzLmpzXCI7aW1wb3J0IHsgZXhpc3RzU3luYywgcmVhZEZpbGVTeW5jIH0gZnJvbSAnZnMnO1xuaW1wb3J0IHsgcmVzb2x2ZSwgYmFzZW5hbWUgfSBmcm9tICdwYXRoJztcbmltcG9ydCB7IGdsb2JTeW5jIH0gZnJvbSAnZ2xvYic7XG5cbi8vIENvbGxlY3QgZ3JvdXBzIFt1cmwoXSBbJ3xcIl1vcHRpb25hbCAnLi98Li4vJywgZmlsZSBwYXJ0IGFuZCBlbmQgb2YgdXJsXG5jb25zdCB1cmxNYXRjaGVyID0gLyh1cmxcXChcXHMqKShcXCd8XFxcIik/KFxcLlxcL3xcXC5cXC5cXC8pKFxcUyopKFxcMlxccypcXCkpL2c7XG5cbmZ1bmN0aW9uIGFzc2V0c0NvbnRhaW5zKGZpbGVVcmwsIHRoZW1lRm9sZGVyLCBsb2dnZXIpIHtcbiAgY29uc3QgdGhlbWVQcm9wZXJ0aWVzID0gZ2V0VGhlbWVQcm9wZXJ0aWVzKHRoZW1lRm9sZGVyKTtcbiAgaWYgKCF0aGVtZVByb3BlcnRpZXMpIHtcbiAgICBsb2dnZXIuZGVidWcoJ05vIHRoZW1lIHByb3BlcnRpZXMgZm91bmQuJyk7XG4gICAgcmV0dXJuIGZhbHNlO1xuICB9XG4gIGNvbnN0IGFzc2V0cyA9IHRoZW1lUHJvcGVydGllc1snYXNzZXRzJ107XG4gIGlmICghYXNzZXRzKSB7XG4gICAgbG9nZ2VyLmRlYnVnKCdObyBkZWZpbmVkIGFzc2V0cyBpbiB0aGVtZSBwcm9wZXJ0aWVzJyk7XG4gICAgcmV0dXJuIGZhbHNlO1xuICB9XG4gIC8vIEdvIHRocm91Z2ggZWFjaCBhc3NldCBtb2R1bGVcbiAgZm9yIChsZXQgbW9kdWxlIG9mIE9iamVjdC5rZXlzKGFzc2V0cykpIHtcbiAgICBjb25zdCBjb3B5UnVsZXMgPSBhc3NldHNbbW9kdWxlXTtcbiAgICAvLyBHbyB0aHJvdWdoIGVhY2ggY29weSBydWxlXG4gICAgZm9yIChsZXQgY29weVJ1bGUgb2YgT2JqZWN0LmtleXMoY29weVJ1bGVzKSkge1xuICAgICAgLy8gaWYgZmlsZSBzdGFydHMgd2l0aCBjb3B5UnVsZSB0YXJnZXQgY2hlY2sgaWYgZmlsZSB3aXRoIHBhdGggYWZ0ZXIgY29weSB0YXJnZXQgY2FuIGJlIGZvdW5kXG4gICAgICBpZiAoZmlsZVVybC5zdGFydHNXaXRoKGNvcHlSdWxlc1tjb3B5UnVsZV0pKSB7XG4gICAgICAgIGNvbnN0IHRhcmdldEZpbGUgPSBmaWxlVXJsLnJlcGxhY2UoY29weVJ1bGVzW2NvcHlSdWxlXSwgJycpO1xuICAgICAgICBjb25zdCBmaWxlcyA9IGdsb2JTeW5jKHJlc29sdmUoJ25vZGVfbW9kdWxlcy8nLCBtb2R1bGUsIGNvcHlSdWxlKSwgeyBub2RpcjogdHJ1ZSB9KTtcblxuICAgICAgICBmb3IgKGxldCBmaWxlIG9mIGZpbGVzKSB7XG4gICAgICAgICAgaWYgKGZpbGUuZW5kc1dpdGgodGFyZ2V0RmlsZSkpIHJldHVybiB0cnVlO1xuICAgICAgICB9XG4gICAgICB9XG4gICAgfVxuICB9XG4gIHJldHVybiBmYWxzZTtcbn1cblxuZnVuY3Rpb24gZ2V0VGhlbWVQcm9wZXJ0aWVzKHRoZW1lRm9sZGVyKSB7XG4gIGNvbnN0IHRoZW1lUHJvcGVydHlGaWxlID0gcmVzb2x2ZSh0aGVtZUZvbGRlciwgJ3RoZW1lLmpzb24nKTtcbiAgaWYgKCFleGlzdHNTeW5jKHRoZW1lUHJvcGVydHlGaWxlKSkge1xuICAgIHJldHVybiB7fTtcbiAgfVxuICBjb25zdCB0aGVtZVByb3BlcnR5RmlsZUFzU3RyaW5nID0gcmVhZEZpbGVTeW5jKHRoZW1lUHJvcGVydHlGaWxlKTtcbiAgaWYgKHRoZW1lUHJvcGVydHlGaWxlQXNTdHJpbmcubGVuZ3RoID09PSAwKSB7XG4gICAgcmV0dXJuIHt9O1xuICB9XG4gIHJldHVybiBKU09OLnBhcnNlKHRoZW1lUHJvcGVydHlGaWxlQXNTdHJpbmcpO1xufVxuXG5mdW5jdGlvbiByZXdyaXRlQ3NzVXJscyhzb3VyY2UsIGhhbmRsZWRSZXNvdXJjZUZvbGRlciwgdGhlbWVGb2xkZXIsIGxvZ2dlciwgb3B0aW9ucykge1xuICBzb3VyY2UgPSBzb3VyY2UucmVwbGFjZSh1cmxNYXRjaGVyLCBmdW5jdGlvbiAobWF0Y2gsIHVybCwgcXVvdGVNYXJrLCByZXBsYWNlLCBmaWxlVXJsLCBlbmRTdHJpbmcpIHtcbiAgICBsZXQgYWJzb2x1dGVQYXRoID0gcmVzb2x2ZShoYW5kbGVkUmVzb3VyY2VGb2xkZXIsIHJlcGxhY2UsIGZpbGVVcmwpO1xuICAgIGNvbnN0IGV4aXN0aW5nVGhlbWVSZXNvdXJjZSA9IGFic29sdXRlUGF0aC5zdGFydHNXaXRoKHRoZW1lRm9sZGVyKSAmJiBleGlzdHNTeW5jKGFic29sdXRlUGF0aCk7XG4gICAgaWYgKGV4aXN0aW5nVGhlbWVSZXNvdXJjZSB8fCBhc3NldHNDb250YWlucyhmaWxlVXJsLCB0aGVtZUZvbGRlciwgbG9nZ2VyKSkge1xuICAgICAgLy8gQWRkaW5nIC4vIHdpbGwgc2tpcCBjc3MtbG9hZGVyLCB3aGljaCBzaG91bGQgYmUgZG9uZSBmb3IgYXNzZXQgZmlsZXNcbiAgICAgIC8vIEluIGEgcHJvZHVjdGlvbiBidWlsZCwgdGhlIGNzcyBmaWxlIGlzIGluIFZBQURJTi9idWlsZCBhbmQgc3RhdGljIGZpbGVzIGFyZSBpbiBWQUFESU4vc3RhdGljLCBzbyAuLi9zdGF0aWMgbmVlZHMgdG8gYmUgYWRkZWRcbiAgICAgIGNvbnN0IHJlcGxhY2VtZW50ID0gb3B0aW9ucy5kZXZNb2RlID8gJy4vJyA6ICcuLi9zdGF0aWMvJztcblxuICAgICAgY29uc3Qgc2tpcExvYWRlciA9IGV4aXN0aW5nVGhlbWVSZXNvdXJjZSA/ICcnIDogcmVwbGFjZW1lbnQ7XG4gICAgICBjb25zdCBmcm9udGVuZFRoZW1lRm9sZGVyID0gc2tpcExvYWRlciArICd0aGVtZXMvJyArIGJhc2VuYW1lKHRoZW1lRm9sZGVyKTtcbiAgICAgIGxvZ2dlci5kZWJ1ZyhcbiAgICAgICAgJ1VwZGF0aW5nIHVybCBmb3IgZmlsZScsXG4gICAgICAgIFwiJ1wiICsgcmVwbGFjZSArIGZpbGVVcmwgKyBcIidcIixcbiAgICAgICAgJ3RvIHVzZScsXG4gICAgICAgIFwiJ1wiICsgZnJvbnRlbmRUaGVtZUZvbGRlciArICcvJyArIGZpbGVVcmwgKyBcIidcIlxuICAgICAgKTtcbiAgICAgIGNvbnN0IHBhdGhSZXNvbHZlZCA9IGFic29sdXRlUGF0aC5zdWJzdHJpbmcodGhlbWVGb2xkZXIubGVuZ3RoKS5yZXBsYWNlKC9cXFxcL2csICcvJyk7XG5cbiAgICAgIC8vIGtlZXAgdGhlIHVybCB0aGUgc2FtZSBleGNlcHQgcmVwbGFjZSB0aGUgLi8gb3IgLi4vIHRvIHRoZW1lcy9bdGhlbWVGb2xkZXJdXG4gICAgICByZXR1cm4gdXJsICsgKHF1b3RlTWFyayA/PyAnJykgKyBmcm9udGVuZFRoZW1lRm9sZGVyICsgcGF0aFJlc29sdmVkICsgZW5kU3RyaW5nO1xuICAgIH0gZWxzZSBpZiAob3B0aW9ucy5kZXZNb2RlKSB7XG4gICAgICBsb2dnZXIubG9nKFwiTm8gcmV3cml0ZSBmb3IgJ1wiLCBtYXRjaCwgXCInIGFzIHRoZSBmaWxlIHdhcyBub3QgZm91bmQuXCIpO1xuICAgIH0gZWxzZSB7XG4gICAgICAvLyBJbiBwcm9kdWN0aW9uLCB0aGUgY3NzIGlzIGluIFZBQURJTi9idWlsZCBidXQgdGhlIHRoZW1lIGZpbGVzIGFyZSBpbiAuXG4gICAgICByZXR1cm4gdXJsICsgKHF1b3RlTWFyayA/PyAnJykgKyAnLi4vLi4vJyArIGZpbGVVcmwgKyBlbmRTdHJpbmc7XG4gICAgfVxuICAgIHJldHVybiBtYXRjaDtcbiAgfSk7XG4gIHJldHVybiBzb3VyY2U7XG59XG5cbmV4cG9ydCB7IHJld3JpdGVDc3NVcmxzIH07XG4iLCAie1xuICBcImZyb250ZW5kRm9sZGVyXCI6IFwiQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL2Zyb250ZW5kXCIsXG4gIFwidGhlbWVGb2xkZXJcIjogXCJ0aGVtZXNcIixcbiAgXCJ0aGVtZVJlc291cmNlRm9sZGVyXCI6IFwiQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL2Zyb250ZW5kL2dlbmVyYXRlZC9qYXItcmVzb3VyY2VzXCIsXG4gIFwic3RhdGljT3V0cHV0XCI6IFwiQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL3RhcmdldC9jbGFzc2VzL01FVEEtSU5GL1ZBQURJTi93ZWJhcHAvVkFBRElOL3N0YXRpY1wiLFxuICBcImdlbmVyYXRlZEZvbGRlclwiOiBcImdlbmVyYXRlZFwiLFxuICBcInN0YXRzT3V0cHV0XCI6IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxjbGFzc2VzXFxcXE1FVEEtSU5GXFxcXFZBQURJTlxcXFxjb25maWdcIixcbiAgXCJmcm9udGVuZEJ1bmRsZU91dHB1dFwiOiBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxcY2xhc3Nlc1xcXFxNRVRBLUlORlxcXFxWQUFESU5cXFxcd2ViYXBwXCIsXG4gIFwiZGV2QnVuZGxlT3V0cHV0XCI6IFwiQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL3RhcmdldC9kZXYtYnVuZGxlL3dlYmFwcFwiLFxuICBcImRldkJ1bmRsZVN0YXRzT3V0cHV0XCI6IFwiQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL3RhcmdldC9kZXYtYnVuZGxlL2NvbmZpZ1wiLFxuICBcImphclJlc291cmNlc0ZvbGRlclwiOiBcIkM6L1VzZXJzL2xhbXlqL2dpdC9vd2xjbXM0L293bGNtcy9mcm9udGVuZC9nZW5lcmF0ZWQvamFyLXJlc291cmNlc1wiLFxuICBcInRoZW1lTmFtZVwiOiBcIm93bGNtc1wiLFxuICBcImNsaWVudFNlcnZpY2VXb3JrZXJTb3VyY2VcIjogXCJDOlxcXFxVc2Vyc1xcXFxsYW15alxcXFxnaXRcXFxcb3dsY21zNFxcXFxvd2xjbXNcXFxcdGFyZ2V0XFxcXHN3LnRzXCIsXG4gIFwicHdhRW5hYmxlZFwiOiBmYWxzZSxcbiAgXCJvZmZsaW5lRW5hYmxlZFwiOiBmYWxzZSxcbiAgXCJvZmZsaW5lUGF0aFwiOiBcIidvZmZsaW5lLmh0bWwnXCJcbn0iLCAiY29uc3QgX192aXRlX2luamVjdGVkX29yaWdpbmFsX2Rpcm5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx0YXJnZXRcXFxccGx1Z2luc1xcXFxyb2xsdXAtcGx1Z2luLXBvc3Rjc3MtbGl0LWN1c3RvbVwiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9maWxlbmFtZSA9IFwiQzpcXFxcVXNlcnNcXFxcbGFteWpcXFxcZ2l0XFxcXG93bGNtczRcXFxcb3dsY21zXFxcXHRhcmdldFxcXFxwbHVnaW5zXFxcXHJvbGx1cC1wbHVnaW4tcG9zdGNzcy1saXQtY3VzdG9tXFxcXHJvbGx1cC1wbHVnaW4tcG9zdGNzcy1saXQuanNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfaW1wb3J0X21ldGFfdXJsID0gXCJmaWxlOi8vL0M6L1VzZXJzL2xhbXlqL2dpdC9vd2xjbXM0L293bGNtcy90YXJnZXQvcGx1Z2lucy9yb2xsdXAtcGx1Z2luLXBvc3Rjc3MtbGl0LWN1c3RvbS9yb2xsdXAtcGx1Z2luLXBvc3Rjc3MtbGl0LmpzXCI7LyoqXG4gKiBNSVQgTGljZW5zZVxuXG5Db3B5cmlnaHQgKGMpIDIwMTkgVW1iZXJ0byBQZXBhdG9cblxuUGVybWlzc2lvbiBpcyBoZXJlYnkgZ3JhbnRlZCwgZnJlZSBvZiBjaGFyZ2UsIHRvIGFueSBwZXJzb24gb2J0YWluaW5nIGEgY29weVxub2YgdGhpcyBzb2Z0d2FyZSBhbmQgYXNzb2NpYXRlZCBkb2N1bWVudGF0aW9uIGZpbGVzICh0aGUgXCJTb2Z0d2FyZVwiKSwgdG8gZGVhbFxuaW4gdGhlIFNvZnR3YXJlIHdpdGhvdXQgcmVzdHJpY3Rpb24sIGluY2x1ZGluZyB3aXRob3V0IGxpbWl0YXRpb24gdGhlIHJpZ2h0c1xudG8gdXNlLCBjb3B5LCBtb2RpZnksIG1lcmdlLCBwdWJsaXNoLCBkaXN0cmlidXRlLCBzdWJsaWNlbnNlLCBhbmQvb3Igc2VsbFxuY29waWVzIG9mIHRoZSBTb2Z0d2FyZSwgYW5kIHRvIHBlcm1pdCBwZXJzb25zIHRvIHdob20gdGhlIFNvZnR3YXJlIGlzXG5mdXJuaXNoZWQgdG8gZG8gc28sIHN1YmplY3QgdG8gdGhlIGZvbGxvd2luZyBjb25kaXRpb25zOlxuXG5UaGUgYWJvdmUgY29weXJpZ2h0IG5vdGljZSBhbmQgdGhpcyBwZXJtaXNzaW9uIG5vdGljZSBzaGFsbCBiZSBpbmNsdWRlZCBpbiBhbGxcbmNvcGllcyBvciBzdWJzdGFudGlhbCBwb3J0aW9ucyBvZiB0aGUgU29mdHdhcmUuXG5cblRIRSBTT0ZUV0FSRSBJUyBQUk9WSURFRCBcIkFTIElTXCIsIFdJVEhPVVQgV0FSUkFOVFkgT0YgQU5ZIEtJTkQsIEVYUFJFU1MgT1JcbklNUExJRUQsIElOQ0xVRElORyBCVVQgTk9UIExJTUlURUQgVE8gVEhFIFdBUlJBTlRJRVMgT0YgTUVSQ0hBTlRBQklMSVRZLFxuRklUTkVTUyBGT1IgQSBQQVJUSUNVTEFSIFBVUlBPU0UgQU5EIE5PTklORlJJTkdFTUVOVC4gSU4gTk8gRVZFTlQgU0hBTEwgVEhFXG5BVVRIT1JTIE9SIENPUFlSSUdIVCBIT0xERVJTIEJFIExJQUJMRSBGT1IgQU5ZIENMQUlNLCBEQU1BR0VTIE9SIE9USEVSXG5MSUFCSUxJVFksIFdIRVRIRVIgSU4gQU4gQUNUSU9OIE9GIENPTlRSQUNULCBUT1JUIE9SIE9USEVSV0lTRSwgQVJJU0lORyBGUk9NLFxuT1VUIE9GIE9SIElOIENPTk5FQ1RJT04gV0lUSCBUSEUgU09GVFdBUkUgT1IgVEhFIFVTRSBPUiBPVEhFUiBERUFMSU5HUyBJTiBUSEVcblNPRlRXQVJFLlxuICovXG4vLyBUaGlzIGlzIGh0dHBzOi8vZ2l0aHViLmNvbS91bWJvcGVwYXRvL3JvbGx1cC1wbHVnaW4tcG9zdGNzcy1saXQgMi4wLjAgKyBodHRwczovL2dpdGh1Yi5jb20vdW1ib3BlcGF0by9yb2xsdXAtcGx1Z2luLXBvc3Rjc3MtbGl0L3B1bGwvNTRcbi8vIHRvIG1ha2UgaXQgd29yayB3aXRoIFZpdGUgM1xuLy8gT25jZSAvIGlmIGh0dHBzOi8vZ2l0aHViLmNvbS91bWJvcGVwYXRvL3JvbGx1cC1wbHVnaW4tcG9zdGNzcy1saXQvcHVsbC81NCBpcyBtZXJnZWQgdGhpcyBzaG91bGQgYmUgcmVtb3ZlZCBhbmQgcm9sbHVwLXBsdWdpbi1wb3N0Y3NzLWxpdCBzaG91bGQgYmUgdXNlZCBpbnN0ZWFkXG5cbmltcG9ydCB7IGNyZWF0ZUZpbHRlciB9IGZyb20gJ0Byb2xsdXAvcGx1Z2ludXRpbHMnO1xuaW1wb3J0IHRyYW5zZm9ybUFzdCBmcm9tICd0cmFuc2Zvcm0tYXN0JztcblxuY29uc3QgYXNzZXRVcmxSRSA9IC9fX1ZJVEVfQVNTRVRfXyhbXFx3JF0rKV9fKD86XFwkXyguKj8pX18pPy9nXG5cbmNvbnN0IGVzY2FwZSA9IChzdHIpID0+XG4gIHN0clxuICAgIC5yZXBsYWNlKGFzc2V0VXJsUkUsICcke3Vuc2FmZUNTU1RhZyhcIl9fVklURV9BU1NFVF9fJDFfXyQyXCIpfScpXG4gICAgLnJlcGxhY2UoL2AvZywgJ1xcXFxgJylcbiAgICAucmVwbGFjZSgvXFxcXCg/IWApL2csICdcXFxcXFxcXCcpO1xuXG5leHBvcnQgZGVmYXVsdCBmdW5jdGlvbiBwb3N0Y3NzTGl0KG9wdGlvbnMgPSB7fSkge1xuICBjb25zdCBkZWZhdWx0T3B0aW9ucyA9IHtcbiAgICBpbmNsdWRlOiAnKiovKi57Y3NzLHNzcyxwY3NzLHN0eWwsc3R5bHVzLHNhc3Msc2NzcyxsZXNzfScsXG4gICAgZXhjbHVkZTogbnVsbCxcbiAgICBpbXBvcnRQYWNrYWdlOiAnbGl0J1xuICB9O1xuXG4gIGNvbnN0IG9wdHMgPSB7IC4uLmRlZmF1bHRPcHRpb25zLCAuLi5vcHRpb25zIH07XG4gIGNvbnN0IGZpbHRlciA9IGNyZWF0ZUZpbHRlcihvcHRzLmluY2x1ZGUsIG9wdHMuZXhjbHVkZSk7XG5cbiAgcmV0dXJuIHtcbiAgICBuYW1lOiAncG9zdGNzcy1saXQnLFxuICAgIGVuZm9yY2U6ICdwb3N0JyxcbiAgICB0cmFuc2Zvcm0oY29kZSwgaWQpIHtcbiAgICAgIGlmICghZmlsdGVyKGlkKSkgcmV0dXJuO1xuICAgICAgY29uc3QgYXN0ID0gdGhpcy5wYXJzZShjb2RlLCB7fSk7XG4gICAgICAvLyBleHBvcnQgZGVmYXVsdCBjb25zdCBjc3M7XG4gICAgICBsZXQgZGVmYXVsdEV4cG9ydE5hbWU7XG5cbiAgICAgIC8vIGV4cG9ydCBkZWZhdWx0ICcuLi4nO1xuICAgICAgbGV0IGlzRGVjbGFyYXRpb25MaXRlcmFsID0gZmFsc2U7XG4gICAgICBjb25zdCBtYWdpY1N0cmluZyA9IHRyYW5zZm9ybUFzdChjb2RlLCB7IGFzdDogYXN0IH0sIChub2RlKSA9PiB7XG4gICAgICAgIGlmIChub2RlLnR5cGUgPT09ICdFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24nKSB7XG4gICAgICAgICAgZGVmYXVsdEV4cG9ydE5hbWUgPSBub2RlLmRlY2xhcmF0aW9uLm5hbWU7XG5cbiAgICAgICAgICBpc0RlY2xhcmF0aW9uTGl0ZXJhbCA9IG5vZGUuZGVjbGFyYXRpb24udHlwZSA9PT0gJ0xpdGVyYWwnO1xuICAgICAgICB9XG4gICAgICB9KTtcblxuICAgICAgaWYgKCFkZWZhdWx0RXhwb3J0TmFtZSAmJiAhaXNEZWNsYXJhdGlvbkxpdGVyYWwpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuICAgICAgbWFnaWNTdHJpbmcud2Fsaygobm9kZSkgPT4ge1xuICAgICAgICBpZiAoZGVmYXVsdEV4cG9ydE5hbWUgJiYgbm9kZS50eXBlID09PSAnVmFyaWFibGVEZWNsYXJhdGlvbicpIHtcbiAgICAgICAgICBjb25zdCBleHBvcnRlZFZhciA9IG5vZGUuZGVjbGFyYXRpb25zLmZpbmQoKGQpID0+IGQuaWQubmFtZSA9PT0gZGVmYXVsdEV4cG9ydE5hbWUpO1xuICAgICAgICAgIGlmIChleHBvcnRlZFZhcikge1xuICAgICAgICAgICAgZXhwb3J0ZWRWYXIuaW5pdC5lZGl0LnVwZGF0ZShgY3NzVGFnXFxgJHtlc2NhcGUoZXhwb3J0ZWRWYXIuaW5pdC52YWx1ZSl9XFxgYCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKGlzRGVjbGFyYXRpb25MaXRlcmFsICYmIG5vZGUudHlwZSA9PT0gJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbicpIHtcbiAgICAgICAgICBub2RlLmRlY2xhcmF0aW9uLmVkaXQudXBkYXRlKGBjc3NUYWdcXGAke2VzY2FwZShub2RlLmRlY2xhcmF0aW9uLnZhbHVlKX1cXGBgKTtcbiAgICAgICAgfVxuICAgICAgfSk7XG4gICAgICBtYWdpY1N0cmluZy5wcmVwZW5kKGBpbXBvcnQge2NzcyBhcyBjc3NUYWcsIHVuc2FmZUNTUyBhcyB1bnNhZmVDU1NUYWd9IGZyb20gJyR7b3B0cy5pbXBvcnRQYWNrYWdlfSc7XFxuYCk7XG4gICAgICByZXR1cm4ge1xuICAgICAgICBjb2RlOiBtYWdpY1N0cmluZy50b1N0cmluZygpLFxuICAgICAgICBtYXA6IG1hZ2ljU3RyaW5nLmdlbmVyYXRlTWFwKHtcbiAgICAgICAgICBoaXJlczogdHJ1ZVxuICAgICAgICB9KVxuICAgICAgfTtcbiAgICB9XG4gIH07XG59O1xuIiwgImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJDOlxcXFxVc2Vyc1xcXFxsYW15alxcXFxnaXRcXFxcb3dsY21zNFxcXFxvd2xjbXNcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkM6XFxcXFVzZXJzXFxcXGxhbXlqXFxcXGdpdFxcXFxvd2xjbXM0XFxcXG93bGNtc1xcXFx2aXRlLmNvbmZpZy50c1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vQzovVXNlcnMvbGFteWovZ2l0L293bGNtczQvb3dsY21zL3ZpdGUuY29uZmlnLnRzXCI7aW1wb3J0IHsgVXNlckNvbmZpZ0ZuIH0gZnJvbSAndml0ZSc7XG5pbXBvcnQgeyBvdmVycmlkZVZhYWRpbkNvbmZpZyB9IGZyb20gJy4vdml0ZS5nZW5lcmF0ZWQnO1xuXG5jb25zdCBjdXN0b21Db25maWc6IFVzZXJDb25maWdGbiA9IChlbnYpID0+ICh7XG4gIC8vIEhlcmUgeW91IGNhbiBhZGQgY3VzdG9tIFZpdGUgcGFyYW1ldGVyc1xuICAvLyBodHRwczovL3ZpdGVqcy5kZXYvY29uZmlnL1xufSk7XG5cbmV4cG9ydCBkZWZhdWx0IG92ZXJyaWRlVmFhZGluQ29uZmlnKGN1c3RvbUNvbmZpZyk7XG4iXSwKICAibWFwcGluZ3MiOiAiO0FBTUEsT0FBTyxVQUFVO0FBQ2pCLFNBQVMsY0FBQUEsYUFBWSxhQUFBQyxZQUFXLGVBQUFDLGNBQWEsZ0JBQUFDLGVBQWMsaUJBQUFDLHNCQUFxQjtBQUNoRixTQUFTLGtCQUFrQjtBQUMzQixZQUFZLFNBQVM7OztBQ1dyQixTQUFTLGNBQUFDLGFBQVksZ0JBQUFDLHFCQUFvQjtBQUN6QyxTQUFTLFdBQUFDLGdCQUFlOzs7QUNEeEIsU0FBUyxZQUFBQyxpQkFBZ0I7QUFDekIsU0FBUyxXQUFBQyxVQUFTLFlBQUFDLGlCQUFnQjtBQUNsQyxTQUFTLGNBQUFDLGFBQVksY0FBYyxxQkFBcUI7OztBQ0Z4RCxTQUFTLGFBQWEsVUFBVSxXQUFXLFlBQVksb0JBQW9CO0FBQzNFLFNBQVMsU0FBUyxVQUFVLFVBQVUsZUFBZTtBQUNyRCxTQUFTLGdCQUFnQjtBQUV6QixJQUFNLHdCQUF3QixDQUFDLFFBQVEsT0FBTyxPQUFPO0FBV3JELFNBQVMsbUJBQW1CQyxjQUFhLGlDQUFpQyxRQUFRO0FBQ2hGLFFBQU0sMEJBQTBCLFFBQVEsaUNBQWlDLFVBQVUsU0FBU0EsWUFBVyxDQUFDO0FBQ3hHLFFBQU0sYUFBYSxlQUFlQSxjQUFhLE1BQU07QUFHckQsTUFBSSxXQUFXLE1BQU0sU0FBUyxHQUFH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QUFBLGNBQ0wsT0FBTyxFQUFFLE1BQU0sVUFBVSxLQUFLLHVCQUF1QjtBQUFBLGNBQ3JELFVBQVU7QUFBQSxZQUNaLENBQUM7QUFDRCxtQkFBTztBQUFBLFVBQ1Q7QUFBQSxRQUNGO0FBQUEsTUFDRjtBQUFBLE1BQ0EsUUFBUTtBQUFBLFFBQ04sWUFBWTtBQUFBLE1BQ2QsQ0FBQztBQUFBLE1BQ0Qsa0JBQWtCLFdBQVcsRUFBRSxZQUFZLE1BQU0sVUFBVSxlQUFlLENBQUM7QUFBQSxJQUM3RTtBQUFBLEVBQ0Y7QUFDRjtBQUVPLElBQU0sdUJBQXVCLENBQUNDLGtCQUErQjtBQUNsRSxTQUFPLGFBQWEsQ0FBQyxRQUFRLFlBQVksYUFBYSxHQUFHLEdBQUdBLGNBQWEsR0FBRyxDQUFDLENBQUM7QUFDaEY7QUFDQSxTQUFTLFdBQVcsUUFBd0I7QUFDMUMsUUFBTSxjQUFjLEtBQUssUUFBUSxtQkFBbUIsUUFBUSxjQUFjO0FBQzFFLFNBQU8sS0FBSyxNQUFNUixjQUFhLGFBQWEsRUFBRSxVQUFVLFFBQVEsQ0FBQyxDQUFDLEVBQUU7QUFDdEU7QUFDQSxTQUFTLFlBQVksUUFBd0I7QUFDM0MsUUFBTSxjQUFjLEtBQUssUUFBUSxtQkFBbUIsUUFBUSxjQUFjO0FBQzFFLFNBQU8sS0FBSyxNQUFNQSxjQUFhLGFBQWEsRUFBRSxVQUFVLFFBQVEsQ0FBQyxDQUFDLEVBQUU7QUFDdEU7OztBT3Z6QkEsSUFBTSxlQUE2QixDQUFDLFNBQVM7QUFBQTtBQUFBO0FBRzdDO0FBRUEsSUFBTyxzQkFBUSxxQkFBcUIsWUFBWTsiLAogICJuYW1lcyI6IFsiZXhpc3RzU3luYyIsICJta2RpclN5bmMiLCAicmVhZGRpclN5bmMiLCAicmVhZEZpbGVTeW5jIiwgIndyaXRlRmlsZVN5bmMiLCAiZXhpc3RzU3luYyIsICJyZWFkRmlsZVN5bmMiLCAicmVzb2x2ZSIsICJnbG9iU3luYyIsICJyZXNvbHZlIiwgImJhc2VuYW1lIiwgImV4aXN0c1N5bmMiLCAidGhlbWVGb2xkZXIiLCAidGhlbWVGb2xkZXIiLCAicmVzb2x2ZSIsICJnbG9iU3luYyIsICJleGlzdHNTeW5jIiwgImJhc2VuYW1lIiwgInZhcmlhYmxlIiwgImZpbGVuYW1lIiwgImV4aXN0c1N5bmMiLCAicmVzb2x2ZSIsICJ0aGVtZUZvbGRlciIsICJyZWFkRmlsZVN5bmMiLCAiZXhpc3RzU3luYyIsICJyZWFkRmlsZVN5bmMiLCAicmVzb2x2ZSIsICJiYXNlbmFtZSIsICJnbG9iU3luYyIsICJ0aGVtZUZvbGRlciIsICJnZXRUaGVtZVByb3BlcnRpZXMiLCAiZ2xvYlN5bmMiLCAicmVzb2x2ZSIsICJleGlzdHNTeW5jIiwgInJlYWRGaWxlU3luYyIsICJyZXBsYWNlIiwgImJhc2VuYW1lIiwgInJlcXVpcmUiLCAiZXhpc3RzU3luYyIsICJyZWFkRmlsZVN5bmMiLCAibWtkaXJTeW5jIiwgImJ1bmRsZSIsICJyZWFkZGlyU3luYyIsICJ0aGVtZUZvbGRlciIsICJ3cml0ZUZpbGVTeW5jIiwgImUiLCAicGF0aCIsICJjdXN0b21Db25maWciXQp9Cg==
