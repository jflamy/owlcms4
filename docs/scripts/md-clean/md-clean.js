#!/usr/bin/env node

const fs = require("fs");
const path = require("path");
const os = require("os");

const TYPORA_TEMP = path.join(
  os.homedir(),
  "AppData",
  "Roaming",
  "Typora",
  "typora-user-images"
);

function cleanup(mdFile) {
  mdFile = path.resolve(mdFile);

  const mdDir = path.dirname(mdFile);
  const mdName = path.basename(mdFile, ".md");

  const imgDir = path.join(mdDir, "img", mdName);
  fs.mkdirSync(imgDir, { recursive: true });

  const content = fs.readFileSync(mdFile, "utf8");

  const regex = new RegExp(`(?:\\./)?img/${mdName}/([^\\)]+\\.png)`, "g");
  const referenced = new Set();
  let match;

  while ((match = regex.exec(content)) !== null) {
    referenced.add(match[1]);
  }

  if (fs.existsSync(TYPORA_TEMP)) {
    for (const f of fs.readdirSync(TYPORA_TEMP)) {
      if (f.toLowerCase().endsWith(".png")) {
        const src = path.join(TYPORA_TEMP, f);
        const dst = path.join(imgDir, f);
        if (!fs.existsSync(dst)) {
          fs.copyFileSync(src, dst);
          console.log("Copied:", f);
        }
      }
    }
  }

  const allFiles = new Set(
    fs.readdirSync(imgDir).filter(f => f.toLowerCase().endsWith(".png"))
  );

  const unused = [...allFiles].filter(f => !referenced.has(f));

  for (const f of unused) {
    const filePath = path.join(imgDir, f);
    console.log("Deleting unused:", f);
    fs.unlinkSync(filePath);
  }

  if (fs.existsSync(TYPORA_TEMP)) {
    for (const f of fs.readdirSync(TYPORA_TEMP)) {
      try {
        fs.unlinkSync(path.join(TYPORA_TEMP, f));
      } catch {}
    }
    console.log("Typora temp folder cleaned.");
  }

  console.log("Done.");
}

if (process.argv.length < 3) {
  console.log("Usage: md-clean <Markdown file>");
  process.exit(1);
}

cleanup(process.argv[2]);