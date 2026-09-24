#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

const IMAGE_EXTENSION_PATTERN = "(?:png|jpe?g|svg)";
const SKIPPED_DIRECTORIES = new Set([".git", "node_modules"]);
const USAGE = `Usage: md-clean [--dry-run] (-r [directory] | <Markdown file>)

Deletes images in <dir>/img/<name>/ that <dir>/<name>.md does not reference.

  <Markdown file>        clean the image folder of one file (folder created if missing)
  -r, --recursive [dir]  clean the image folders of every Markdown file under dir (default: current directory)
  --dry-run              report what would be deleted without deleting
  -h, --help             show this message`;

function isManagedImageFile(fileName) {
  return new RegExp(`^.+\\.${IMAGE_EXTENSION_PATTERN}$`, "i").test(fileName);
}

function resolveMarkdownFile(mdFile) {
  if (fs.existsSync(mdFile) || mdFile.toLowerCase().endsWith(".md")) {
    return path.resolve(mdFile);
  }

  return path.resolve(mdFile + ".md");
}

function hasManagedImageDirectory(mdFile) {
  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, path.extname(mdFile));
  const imgDir = path.join(mdDir, "img", mdName);

  return fs.existsSync(imgDir) && fs.statSync(imgDir).isDirectory();
}

function findMarkdownFiles(rootDir) {
  const markdownFiles = [];

  function visit(directory) {
    for (const entry of fs.readdirSync(directory, { withFileTypes: true })) {
      const entryPath = path.join(directory, entry.name);
      if (entry.isDirectory()) {
        if (!SKIPPED_DIRECTORIES.has(entry.name)) {
          visit(entryPath);
        }
      } else if (entry.isFile() && entry.name.toLowerCase().endsWith(".md")) {
        markdownFiles.push(entryPath);
      }
    }
  }

  visit(rootDir);
  return markdownFiles.sort();
}

function decodeReference(reference) {
  try {
    return decodeURI(reference);
  } catch {
    return reference;
  }
}

// Match on the img/<name>/ suffix so links work whether relative to the file or to a site root.
function findReferencedImages(mdFile, mdName) {
  const bracketedRegex = new RegExp(`<([^<>\\n]+?\\.${IMAGE_EXTENSION_PATTERN})>`, "gi");
  const tokenRegex = new RegExp(
    `([^\\s"'<>()\\[\\]]+?\\.${IMAGE_EXTENSION_PATTERN})(?![\\w-]|\\.\\w)`,
    "gi"
  );
  const prefix = `img/${mdName}/`;
  const content = fs.readFileSync(mdFile, "utf8");
  const referenced = new Set();

  for (const regex of [bracketedRegex, tokenRegex]) {
    for (const match of content.matchAll(regex)) {
      const reference = decodeReference(match[1]).replaceAll("\\", "/");
      const index = reference.lastIndexOf(prefix);
      if (index === 0 || (index > 0 && reference[index - 1] === "/")) {
        referenced.add(reference.slice(index + prefix.length));
      }
    }
  }

  return referenced;
}

function cleanup(mdFile, dryRun) {
  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, ".md");

  const imgDir = path.join(mdDir, "img", mdName);
  const displayDir = path.relative(process.cwd(), imgDir) || imgDir;
  if (!fs.existsSync(imgDir)) {
    if (!dryRun) {
      fs.mkdirSync(imgDir, { recursive: true });
      console.log(`${displayDir}: created managed image directory.`);
    } else {
      console.log(`${displayDir}: no managed image directory.`);
    }
    return;
  }

  const referenced = findReferencedImages(mdFile, mdName);

  const allFiles = fs.readdirSync(imgDir).filter(isManagedImageFile);

  const unused = allFiles.filter(f => !referenced.has(f));

  if (unused.length > 0) {
    console.log(`${displayDir}:`);
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

function cleanupTree(rootDir, dryRun) {
  for (const mdFile of findMarkdownFiles(rootDir).filter(hasManagedImageDirectory)) {
    cleanup(mdFile, dryRun);
  }
}

const args = process.argv.slice(2);
const dryRun = args.includes("--dry-run");
const recursive = args.includes("-r") || args.includes("--recursive");
const positionalArgs = args.filter(arg => !["--dry-run", "-r", "--recursive"].includes(arg));

if (args.includes("-h") || args.includes("--help")) {
  console.log(USAGE);
  process.exit(0);
}

const unknownOption = positionalArgs.find(arg => arg.startsWith("-"));
if (unknownOption) {
  console.log(`Unknown option: ${unknownOption}\n\n${USAGE}`);
  process.exit(1);
}

const validArguments = recursive ? positionalArgs.length <= 1 : positionalArgs.length === 1;

if (!validArguments) {
  console.log(USAGE);
  process.exit(1);
}

if (recursive) {
  const scanRoot = path.resolve(positionalArgs[0] ?? process.cwd());
  if (!fs.existsSync(scanRoot) || !fs.statSync(scanRoot).isDirectory()) {
    console.log(`Not a directory: ${scanRoot}`);
    process.exit(1);
  }
  cleanupTree(scanRoot, dryRun);
} else {
  const mdFile = resolveMarkdownFile(positionalArgs[0]);
  if (!fs.existsSync(mdFile)) {
    console.log(`Markdown file not found: ${mdFile}`);
    process.exit(1);
  }
  cleanup(mdFile, dryRun);
}