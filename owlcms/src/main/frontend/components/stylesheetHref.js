const debugStylesheetVersion = new WeakMap();

function getVersionSuffix(component) {
  const autoVersion = component.autoversion ?? component.autoVersion ?? "";
  if (autoVersion) {
    return `${autoVersion}.css`;
  }

  let debugVersion = debugStylesheetVersion.get(component);
  if (!debugVersion) {
    debugVersion = `${Date.now()}`;
    debugStylesheetVersion.set(component, debugVersion);
  }

  return `.css?v=${debugVersion}`;
}

export function stylesheetHref(component, assetName) {
  const stylesDir = component.stylesDir ?? "";
  return `local/${stylesDir}/${assetName}${getVersionSuffix(component)}`;
}