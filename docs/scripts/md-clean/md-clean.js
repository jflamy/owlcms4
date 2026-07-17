#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const IMAGE_EXTENSION_PATTERN = "(?:png|jpe?g|svg)";
const DOCS_ROOT = path.resolve(__dirname, "..", "..");

function isManagedImageFile(fileName) {
  return new RegExp(`^image-.*\\.${IMAGE_EXTENSION_PATTERN}$`, "i").test(fileName);
}

function findRepoRoot(startDir = process.cwd()) {
  let currentDir = path.resolve(startDir);

  while (true) {
    if (fs.existsSync(path.join(currentDir, ".git"))) {
      return currentDir;
    }

    const parentDir = path.dirname(currentDir);
    if (parentDir === currentDir) {
      return null;
    }

    currentDir = parentDir;
  }
}

function resolveMarkdownFile(mdFile) {
  if (fs.existsSync(mdFile)) {
    return path.resolve(mdFile);
  }

  let candidate = mdFile;
  if (!mdFile.toLowerCase().endsWith(".md")) {
    candidate = mdFile + ".md";
    if (fs.existsSync(candidate)) {
      return path.resolve(candidate);
    }
  }

  if (path.isAbsolute(mdFile)) {
    return path.resolve(candidate);
  }

  const repoRoot = findRepoRoot();
  if (!repoRoot) {
    return path.resolve(candidate);
  }

  const docsRoot = path.join(repoRoot, "docs");
  const docsRelativePath = candidate.replace(/^docs[\\/]/, "");
  return path.resolve(docsRoot, docsRelativePath);
}

function hasManagedImageDirectory(mdFile) {
  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, path.extname(mdFile));
  const imgDir = path.join(mdDir, "img", mdName);

  return fs.existsSync(imgDir) && fs.statSync(imgDir).isDirectory();
}

function findMarkdownFiles(docsRoot) {
  const markdownFiles = [];

  function visit(directory) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const entryPath = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        visit(entryPath);
      } else if (entry.isFile() && entry.name.toLowerCase().endsWith(".md") && hasManagedImageDirectory(entryPath)) {
        markdownFiles.push(entryPath);
      }
    }
  }

  visit(docsRoot);
  return markdownFiles.sort();
}

function findMarkdownFilesWithManagedImages(docsRoot) {
  return findMarkdownFiles(docsRoot).filter(hasManagedImageDirectory);
}

function escapeRegularExpression(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function findReferencedImages(markdownFiles, imgDir) {
  const relativeImgDir = path.relative(DOCS_ROOT, imgDir).split(path.sep).join("/");
  const imgDirPattern = escapeRegularExpression(relativeImgDir).replaceAll("/", "[\\\\/]");
  const regex = new RegExp(
    `(?:\\./)?${imgDirPattern}[\\\\/]([^\\)]+\\.${IMAGE_EXTENSION_PATTERN})`,
    "gi"
  );
  const referenced = new Set();

  for (const markdownFile of markdownFiles) {
    const content = fs.readFileSync(markdownFile, "utf8");
    let match;
    while ((match = regex.exec(content)) !== null) {
      referenced.add(match[1]);
    }
    regex.lastIndex = 0;
  }

  return referenced;
}

function displayImageDirectory(imgDir) {
  const relativePath = path.relative(DOCS_ROOT, imgDir);

  return relativePath.startsWith("..") ? imgDir : relativePath;
}

function cleanup(mdFile, { dryRun = false, markdownFiles } = {}) {
  mdFile = resolveMarkdownFile(mdFile);

  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, ".md");

  const imgDir = path.join(mdDir, "img", mdName);
  console.log(`Checking: ${displayImageDirectory(imgDir)}`);
  if (!fs.existsSync(imgDir)) {
    if (!dryRun) {
      fs.mkdirSync(imgDir, { recursive: true });
      console.log("  Created managed image directory.");
    } else {
      console.log("  No managed image directory.");
    }
    return;
  }

  const referenced = findReferencedImages(markdownFiles, imgDir);

  const allFiles = new Set(fs.readdirSync(imgDir).filter(isManagedImageFile));

  const unused = [...allFiles].filter(f => !referenced.has(f));

  if (unused.length === 0) {
    console.log("  No unused managed images.");
  }

  for (const f of unused) {
    const filePath = path.join(imgDir, f);
    if (dryRun) {
      console.log("  Would delete unused:", f);
    } else {
      console.log("  Deleting unused:", f);
      fs.unlinkSync(filePath);
    }
  }
}

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const all = args.includes("--all");
const positionalArgs = args.filter(arg => arg !== "--dry-run" && arg !== "--all");

if (all && positionalArgs.length !== 0) {
  console.log("Usage: md-clean [--dry-run] (--all | <Markdown file>)");
  process.exit(1);
}

if (!all && positionalArgs.length !== 1) {
  console.log("Usage: md-clean [--dry-run] (--all | <Markdown file>)");
  process.exit(1);
}

if (all) {
  const markdownFiles = findMarkdownFiles(DOCS_ROOT);
  const managedMarkdownFiles = markdownFiles.filter(hasManagedImageDirectory);
  for (const mdFile of managedMarkdownFiles) {
    cleanup(mdFile, { dryRun, markdownFiles });
  }
} else {
  cleanup(positionalArgs[0], { dryRun, markdownFiles: findMarkdownFiles(DOCS_ROOT) });
}