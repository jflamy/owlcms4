const fs = require('fs');
const path = require('path');

const args = new Set(process.argv.slice(2));
const write = args.has('--write');
const rootArg = [...args].find((arg) => arg.startsWith('--root='));
const rootDir = rootArg ? path.resolve(rootArg.slice('--root='.length)) : path.resolve(__dirname, '..');

const skipDirs = new Set([
  '.git',
  '.idea',
  '.vscode',
  'node_modules',
  'target',
  'build',
  'dist',
  'coverage',
  'out',
]);

const loggerCallPattern = /\b(?:this\.)?(?:logger|[A-Za-z_][A-Za-z0-9_]*Logger|getLogger\(\))(?:\s*\/\*\*\/\s*)?\.\s*[A-Za-z_][A-Za-z0-9_]*\s*\(/;
const leadingMarkerPattern = /^([*+=%&])\1+\s*/;

function cleanupLiteralContent(content) {
  let prefix = '';
  let remainder = content;

  if (remainder.startsWith('{}')) {
    prefix = '{}';
    remainder = remainder.slice(2);
  }

  const markerMatch = remainder.match(leadingMarkerPattern);
  if (!markerMatch) {
    return content;
  }

  remainder = remainder.slice(markerMatch[0].length);
  if (prefix && remainder.startsWith('{')) {
    return `${prefix} ${remainder}`;
  }

  return `${prefix}${remainder}`;
}

function processLine(line) {
  const loggerMatch = loggerCallPattern.exec(line);
  if (!loggerMatch) {
    return line;
  }

  const quoteIndex = line.indexOf('"', loggerMatch.index + loggerMatch[0].length);
  if (quoteIndex === -1) {
    return line;
  }

  const content = line.slice(quoteIndex + 1);
  const cleaned = cleanupLiteralContent(content);
  if (cleaned === content) {
    return line;
  }

  return line.slice(0, quoteIndex + 1) + cleaned;
}

function processFile(filePath) {
  const original = fs.readFileSync(filePath, 'utf8');
  const eol = original.includes('\r\n') ? '\r\n' : '\n';
  const lines = original.split(/\r?\n/);
  let changed = 0;

  const updatedLines = lines.map((line) => {
    const updated = processLine(line);
    if (updated !== line) {
      changed += 1;
    }
    return updated;
  });

  if (changed === 0) {
    return null;
  }

  const updated = updatedLines.join(eol);
  if (write) {
    fs.writeFileSync(filePath, updated, 'utf8');
  }

  return { filePath, changed };
}

function walk(dir, results) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.isDirectory()) {
      if (!skipDirs.has(entry.name)) {
        walk(path.join(dir, entry.name), results);
      }
      continue;
    }

    if (entry.isFile() && entry.name.endsWith('.java')) {
      results.push(path.join(dir, entry.name));
    }
  }
}

function main() {
  const javaFiles = [];
  walk(rootDir, javaFiles);

  const changes = [];
  for (const filePath of javaFiles) {
    const result = processFile(filePath);
    if (result) {
      changes.push(result);
    }
  }

  for (const change of changes) {
    console.log(`${write ? 'updated' : 'would update'} ${path.relative(rootDir, change.filePath)} lines=${change.changed}`);
  }

  console.log(`files=${changes.length} mode=${write ? 'write' : 'dry-run'} root=${rootDir}`);
}

main();
