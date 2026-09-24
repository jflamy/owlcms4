#!/usr/bin/env node
// Copies each local image a page uses into <dir>/img/<page>/ (Typora convention) and points the link there.
// Originals are never removed; clean them up afterwards.

const fs = require("fs");
const path = require("path");
const crypto = require("crypto");

const IMAGE = /\.(png|jpe?g|svg|gif|webp)$/i;
const SKIPPED_DIRECTORIES = new Set([".git", "node_modules", "obsolete"]);
const MODES = ["--dry-run", "--apply", "--check"];
const USAGE = `Usage: typora-normalize [--dry-run | --apply | --check] [--verbose] [directory]

  --dry-run  report the copies and link changes without changing anything (default)
  --apply    copy images into img/<page>/ and update the links; nothing is deleted
  --check    list image links that are broken or not in the page's img/<page>/ folder
  --verbose  list every copy and link change
  -h, --help show this message`;

const LINK_PATTERNS = [
  /(\]\(\s*)(<[^>\n]+>|[^)\s]+)/g,
  /(\bsrc\s*=\s*["'])([^"']+)/gi,
  /(^[ \t]*\[[^\]\n]+\]:[ \t]*)(<[^>\n]+>|\S+)/gm,
];

const args = process.argv.slice(2);
if (args.includes("-h") || args.includes("--help")) {
  console.log(USAGE);
  process.exit(0);
}
const unknownOption = args.find(a => a.startsWith("-") && ![...MODES, "--verbose"].includes(a));
const modes = args.filter(a => MODES.includes(a));
const positionalArgs = args.filter(a => !a.startsWith("-"));
if (unknownOption || modes.length > 1 || positionalArgs.length > 1) {
  console.log(`${unknownOption ? `Unknown option: ${unknownOption}\n\n` : ""}${USAGE}`);
  process.exit(1);
}
const mode = modes[0] ?? "--dry-run";
const verbose = args.includes("--verbose");
const root = path.resolve(positionalArgs[0] ?? process.cwd());
if (!fs.existsSync(root) || !fs.statSync(root).isDirectory()) {
  console.log(`Not a directory: ${root}`);
  process.exit(1);
}

const rel = p => path.relative(root, p).split(path.sep).join("/");
const hash = p => crypto.createHash("sha1").update(fs.readFileSync(p)).digest("hex");
const isFile = p => fs.existsSync(p) && fs.statSync(p).isFile();

function walk(dir, out = []) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) {
      if (!SKIPPED_DIRECTORIES.has(e.name)) walk(p, out);
    } else if (e.isFile() && e.name.toLowerCase().endsWith(".md")) {
      out.push(p);
    }
  }
  return out.sort();
}

function decode(value) {
  try {
    return decodeURI(value);
  } catch {
    return value;
  }
}

// mapTarget receives the link path (no brackets, query or hash) and returns a replacement path or null.
function rewriteLinks(content, mapTarget) {
  return LINK_PATTERNS.reduce(
    (text, pattern) =>
      text.replace(pattern, (whole, lead, raw) => {
        const bracketed = raw.startsWith("<") && raw.endsWith(">");
        const inner = bracketed ? raw.slice(1, -1) : raw;
        const cut = inner.search(/[?#]/);
        const target = cut < 0 ? inner : inner.slice(0, cut);
        const suffix = cut < 0 ? "" : inner.slice(cut);
        if (/^[a-z][a-z0-9+.-]*:/i.test(target) || !IMAGE.test(target)) return whole;

        const replacement = mapTarget(target);
        if (replacement == null || replacement === target) return whole;
        return bracketed
          ? `${lead}<${replacement}${suffix}>`
          : `${lead}${replacement.replaceAll(" ", "%20")}${suffix}`;
      }),
    content
  );
}

// Tried relative to the page first, then relative to the docs root (Docsify).
function resolveTarget(mdFile, target) {
  const cleaned = decode(target).replaceAll("\\", "/").replace(/\/{2,}/g, "/");
  const candidates = cleaned.startsWith("/")
    ? [path.join(root, cleaned)]
    : [path.resolve(path.dirname(mdFile), cleaned), path.resolve(root, cleaned)];
  return candidates.find(isFile) ?? null;
}

const pageName = mdFile => path.basename(mdFile, path.extname(mdFile));
const ownImageDir = mdFile => path.join(path.dirname(mdFile), "img", pageName(mdFile));
const mdFiles = walk(root);

function check() {
  let ok = 0;
  const broken = [];
  const notLocal = [];

  for (const mdFile of mdFiles) {
    rewriteLinks(fs.readFileSync(mdFile, "utf8"), target => {
      const resolved = path.resolve(path.dirname(mdFile), decode(target));
      if (!isFile(resolved)) broken.push(`${rel(mdFile)}: ${target}`);
      else if (path.dirname(resolved) !== ownImageDir(mdFile)) notLocal.push(`${rel(mdFile)}: ${target}`);
      else ok++;
      return null;
    });
  }

  console.log(`Pages: ${mdFiles.length}`);
  console.log(`Image links in img/<page>/: ${ok}`);
  console.log(`Image links outside img/<page>/: ${notLocal.length}`);
  notLocal.forEach(l => console.log(`  ${l}`));
  console.log(`Broken image links: ${broken.length}`);
  broken.forEach(l => console.log(`  ${l}`));
  process.exit(broken.length + notLocal.length === 0 ? 0 : 1);
}

function normalize(apply) {
  let local = 0;
  let copies = 0;
  let linkChanges = 0;
  const changedPages = new Set();
  const actions = [];
  const missing = [];
  const conflicts = [];

  for (const mdFile of mdFiles) {
    const ownDir = ownImageDir(mdFile);
    const plannedCopies = new Map();
    const content = fs.readFileSync(mdFile, "utf8");

    const updated = rewriteLinks(content, target => {
      const source = resolveTarget(mdFile, target);
      if (!source) {
        missing.push(`${rel(mdFile)}: ${target}`);
        return null;
      }

      const fileName = path.basename(source);
      const destination = path.join(ownDir, fileName);
      const canonical = `img/${pageName(mdFile)}/${fileName}`;
      if (source === destination && decode(target) === canonical) {
        local++;
        return null;
      }

      if (source !== destination) {
        const planned = plannedCopies.get(destination);
        const existing = planned ?? (isFile(destination) ? destination : null);
        if (existing && existing !== source && hash(existing) !== hash(source)) {
          conflicts.push(`${rel(mdFile)}: ${target} -> ${rel(destination)} already holds a different image`);
          return null;
        }
        if (!existing) {
          plannedCopies.set(destination, source);
          copies++;
          actions.push(`COPY ${rel(source)} -> ${rel(destination)}`);
          if (apply) {
            fs.mkdirSync(ownDir, { recursive: true });
            fs.copyFileSync(source, destination);
          }
        }
      }

      linkChanges++;
      changedPages.add(mdFile);
      actions.push(`LINK ${rel(mdFile)}: ${target} -> ${canonical}`);
      return canonical;
    });

    if (apply && updated !== content) fs.writeFileSync(mdFile, updated);
  }

  const verb = apply ? "" : "would be ";
  console.log(`${apply ? "Applied" : "Dry run"}: ${root}`);
  console.log(`Pages: ${mdFiles.length}`);
  console.log(`Image links already in img/<page>/: ${local}`);
  console.log(`Image links ${verb}changed: ${linkChanges} (in ${changedPages.size} pages)`);
  console.log(`Images ${verb}copied: ${copies}`);
  if (verbose) actions.forEach(a => console.log(`  ${a}`));
  console.log(`Links to missing images (left unchanged): ${missing.length}`);
  missing.forEach(m => console.log(`  ${m}`));
  console.log(`Conflicts (left unchanged): ${conflicts.length}`);
  conflicts.forEach(c => console.log(`  ${c}`));
}

if (mode === "--check") check();
else normalize(mode === "--apply");
