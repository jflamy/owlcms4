const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');

const root = path.resolve(__dirname, '..');
const plan = JSON.parse(fs.readFileSync(path.join(__dirname, 'rename-map.json'), 'utf8'));
const mode = process.argv[2] || '--dry-run';
const routeMap = Object.fromEntries(Object.entries(plan.files).map(([source, destination]) => [source.replace(/\.md$/, ''), destination.replace(/\.md$/, '')]));
const routeScript = `window.owlcmsDocRoutes = ${JSON.stringify(routeMap, null, 2)};\n`;
if (mode === '--write-routes') {
  fs.writeFileSync(path.join(root, 'route-map.js'), routeScript);
  console.log(`Generated ${Object.keys(routeMap).length} legacy documentation routes.`);
  return;
}
assert(['--dry-run', '--apply'].includes(mode), 'Use --dry-run, --apply or --write-routes');
require('./check-rename-map.cjs');

function walk(directory, result = []) {
  for (const entry of fs.readdirSync(path.join(root, directory), { withFileTypes: true })) {
    const relative = path.posix.join(directory, entry.name);
    if (entry.isDirectory() && entry.name !== 'scripts') walk(relative, result);
    else if (entry.isFile()) result.push(relative);
  }
  return result.sort();
}

function mapped(source) {
  if (plan.files[source]) return plan.files[source];
  for (const [directory, destination] of Object.entries(plan.directories)) {
    if (source.startsWith(`${directory}/`)) return destination + source.slice(directory.length);
  }
  return source;
}

const originals = walk('');
const oldFiles = new Set(originals);
const newFiles = new Set(originals.map(mapped));
assert.equal(oldFiles.size, newFiles.size, 'File destination collision');
const changes = [];
const missing = new Set();
let references = 0;
let rewritten = 0;

function decode(target) {
  try { return decodeURI(target); } catch { return target; }
}

function splitTarget(raw) {
  const route = /^(?:\.\/)?#\//.test(raw);
  const target = route ? raw.replace(/^(?:\.\/)?#\//, '/') : raw;
  const offset = target.search(/[?#]/);
  return { route, pathname: decode(offset < 0 ? target : target.slice(0, offset)), suffix: offset < 0 ? '' : target.slice(offset) };
}

function resolve(source, pathname, files) {
  const candidate = pathname.startsWith('/')
    ? path.posix.normalize(pathname.slice(1))
    : path.posix.join(path.posix.dirname(source), pathname);
  if (files.has(candidate)) return candidate;
  if (!path.posix.extname(candidate) && files.has(`${candidate}.md`)) return `${candidate}.md`;
  return null;
}

function rewriteTarget(raw, source, destination) {
  if (/^(?:[a-z][a-z\d+.-]*:|\/\/)/i.test(raw) || !raw) return raw;
  if (raw.startsWith('#') && !raw.startsWith('#/')) return raw;
  const { route, pathname, suffix } = splitTarget(raw);
  if (!pathname) return raw;
  const target = resolve(source, pathname, oldFiles);
  if (!target) {
    missing.add(`${source}: ${raw}`);
    return raw;
  }
  const newTarget = mapped(target);
  let relative;
  if (route) relative = `#/${newTarget}`;
  else if (source === '_sidebar.md' || pathname.startsWith('/')) relative = `/${newTarget}`;
  else {
    relative = path.posix.relative(path.posix.dirname(destination), newTarget);
    if (newTarget.endsWith('.md') && !relative.startsWith('.')) relative = `./${relative}`;
  }
  if (!path.posix.extname(pathname) && target.endsWith('.md')) relative = relative.replace(/\.md$/, '');
  const replacement = relative.replaceAll(' ', '%20') + suffix;
  const resolved = resolve(destination, splitTarget(replacement).pathname, newFiles);
  assert.equal(resolved, newTarget, `Reference changed target: ${source}: ${raw}`);
  references++;
  if (replacement !== raw) rewritten++;
  return replacement;
}

function rewriteText(content, source, destination) {
  const protectedParts = [];
  let text = content;
  if (source.endsWith('.md')) {
    text = text.replace(/(^ {0,3}(`{3,}|~{3,})[^\n]*\n[\s\S]*?^ {0,3}\2[^\n]*(?:\n|$))|(`+)([^`]|(?!\3)`)*?\3|<!--[\s\S]*?-->/gm, whole => {
      protectedParts.push(whole);
      return `\u0000PROTECTED${protectedParts.length - 1}\u0000`;
    });
    text = text.replace(/(\]\(\s*|^[ \t]*\[[^\]\n]+\]:[ \t]*)(<[^>\n]+>|[^\s)]+(?:\([^)]*\)[^\s)]*)*)/gm, (whole, lead, raw) => {
      const bracketed = raw.startsWith('<') && raw.endsWith('>');
      const target = bracketed ? raw.slice(1, -1) : raw;
      const replacement = rewriteTarget(target, source, destination);
      return lead + (bracketed ? `<${replacement}>` : replacement);
    });
  }
  text = text.replace(/<(?:[a-z][\w:-]*)\b[^>]*>/gi, tag => tag.replace(/(\b(?:src|href|xlink:href|poster)\s*=\s*)(["'])(.*?)\2/gi, (whole, lead, quote, target) => {
    return lead + quote + rewriteTarget(target, source, destination) + quote;
  }));
  if (/\.(css|svg|html)$/i.test(source)) {
    text = text.replace(/(url\(\s*)(["']?)([^)'"\s]+)\2(\s*\))/gi, (whole, lead, quote, target, end) => {
      return lead + quote + rewriteTarget(target, source, destination) + quote + end;
    });
  }
  return text.replace(/\u0000PROTECTED(\d+)\u0000/g, (whole, index) => protectedParts[Number(index)]);
}

for (const source of originals) {
  if (!/\.(md|html|css|svg)$/i.test(source)) continue;
  const before = fs.readFileSync(path.join(root, source), 'utf8');
  const destination = mapped(source);
  const after = rewriteText(before, source, destination);
  if (after !== before) changes.push({ source, destination, before, after });
}

console.log(`${mode}: ${Object.keys(plan.files).length} pages and ${Object.keys(plan.directories).length} image folders.`);
console.log(`${references} resolved references checked; ${rewritten} rewrites in ${changes.length} files.`);
console.log(`Pre-existing unresolved local references (${missing.size}), left unchanged:`);
for (const reference of missing) console.log(`  ${reference}`);

if (mode === '--apply') {
  const hash = relative => crypto.createHash('sha256').update(fs.readFileSync(path.join(root, relative))).digest('hex');
  const hashes = new Map(originals.filter(source => mapped(source) !== source).map(source => [source, hash(source)]));
  for (const change of changes) assert.equal(fs.readFileSync(path.join(root, change.source), 'utf8'), change.before);
  for (const [source, destination] of Object.entries({ ...plan.files, ...plan.directories })) {
    fs.mkdirSync(path.dirname(path.join(root, destination)), { recursive: true });
    fs.renameSync(path.join(root, source), path.join(root, destination));
  }
  for (const [source, expected] of hashes) assert.equal(hash(mapped(source)), expected, `Move changed bytes: ${source}`);
  for (const change of changes) fs.writeFileSync(path.join(root, change.destination), change.after);
  for (const change of changes) assert.equal(fs.readFileSync(path.join(root, change.destination), 'utf8'), change.after);
  for (const destination of newFiles) assert(fs.existsSync(path.join(root, destination)), `Missing after move: ${destination}`);
  fs.writeFileSync(path.join(root, 'route-map.js'), routeScript);
  console.log('Moves verified byte-for-byte before reference rewrites; all planned destinations and rewritten contents verified.');
}