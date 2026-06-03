#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const IMAGE_EXTENSION_PATTERN = "(?:png|jpe?g|svg)";

function isManagedImageFile(fileName) {
  return new RegExp(`\\.${IMAGE_EXTENSION_PATTERN}$`, "i").test(fileName);
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
  if (path.isAbsolute(mdFile)) {
    return path.resolve(mdFile);
  }

  const resolvedRelativePath = path.resolve(mdFile);
  if (fs.existsSync(resolvedRelativePath)) {
    return resolvedRelativePath;
  }

  const repoRoot = findRepoRoot();
  if (!repoRoot) {
    return resolvedRelativePath;
  }

  const docsRoot = path.join(repoRoot, "docs");
  const docsRelativePath = mdFile.replace(/^docs[\\/]/, "");
  return path.resolve(docsRoot, docsRelativePath);
}

function cleanup(mdFile, { dryRun = false } = {}) {
  mdFile = resolveMarkdownFile(mdFile);

  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, ".md");

  const imgDir = path.join(mdDir, "img", mdName);
  if (!fs.existsSync(imgDir)) {
    if (!dryRun) {
      fs.mkdirSync(imgDir, { recursive: true });
    }
    console.log("Done.");
    return;
  }

  const content = fs.readFileSync(mdFile, "utf8");

  const regex = new RegExp(
    `(?:\\./)?img/${mdName}/([^\\)]+\\.${IMAGE_EXTENSION_PATTERN})`,
    "gi"
  );
  const referenced = new Set();
  let match;

  while ((match = regex.exec(content)) !== null) {
    referenced.add(match[1]);
  }

  const allFiles = new Set(fs.readdirSync(imgDir).filter(isManagedImageFile));

  const unused = [...allFiles].filter(f => !referenced.has(f));

  for (const f of unused) {
    const filePath = path.join(imgDir, f);
    if (dryRun) {
      console.log("Would delete unused:", f);
    } else {
      console.log("Deleting unused:", f);
      fs.unlinkSync(filePath);
    }
  }

  console.log("Done.");
}

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const positionalArgs = args.filter(arg => arg !== "--dry-run");

if (positionalArgs.length !== 1) {
  console.log("Usage: md-clean [--dry-run] <Markdown file>");
  process.exit(1);
}

cleanup(positionalArgs[0], { dryRun });