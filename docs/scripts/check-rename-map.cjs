const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

const root = path.resolve(__dirname, '..');
const plan = JSON.parse(fs.readFileSync(path.join(__dirname, 'rename-map.json'), 'utf8'));
const destinations = new Set();
const exists = relative => fs.existsSync(path.join(root, relative));
const applied = process.argv.includes('--applied');
const originalPages = new Map(Object.entries(plan.files).map(([source, destination]) => [destination, source]));

for (const [kind, entries] of [['files', plan.files], ['directories', plan.directories]]) {
  for (const [source, destination] of Object.entries(entries)) {
    for (const relative of [source, destination]) {
      assert(!path.isAbsolute(relative) && !relative.split('/').includes('..'), `Outside docs: ${relative}`);
    }
    const current = applied ? destination : source;
    const absent = applied ? source : destination;
    assert(exists(current), `Missing ${applied ? 'destination' : 'source'}: ${current}`);
    const stats = fs.statSync(path.join(root, current));
    assert(kind === 'files' ? stats.isFile() : stats.isDirectory(), `Wrong source type: ${source}`);
    assert(!exists(absent), `Unexpected path exists: ${absent}`);
    assert(!destinations.has(destination.toLowerCase()), `Duplicate destination: ${destination}`);
    destinations.add(destination.toLowerCase());
  }
}

const pages = fs.readdirSync(root).filter(name => name.endsWith('.md'));
if (applied) pages.push(...Object.keys(plan.files));
for (const page of pages) {
  assert(plan.files[page] || plan.keep.includes(page), `Unmapped page: ${page}`);
}
for (const [source, destination] of Object.entries(plan.files)) {
  const oldStem = path.posix.basename(source, '.md');
  const newStem = path.posix.basename(destination, '.md');
  assert.equal(newStem.replace(/^\d+-/, ''), oldStem.replace(/^\d+/, ''), `Unexpected rename: ${source}`);
  const imageSource = `img/${oldStem}`;
  if (exists(imageSource) || plan.directories[imageSource]) {
    assert.equal(plan.directories[imageSource], `${path.posix.dirname(destination)}/img/${newStem}`, `Image mismatch: ${source}`);
  }
}
for (const entry of fs.readdirSync(path.join(root, 'img'), { withFileTypes: true })) {
  if (entry.isDirectory()) {
    const source = `img/${entry.name}`;
    assert(plan.directories[source] || plan.keep.includes(source), `Unmapped image folder: ${source}`);
  }
}

const sidebar = fs.readFileSync(path.join(root, '_sidebar.md'), 'utf8');
const sidebarPages = new Set();
const sectionOrder = [];
const sectionPages = new Map();
for (const match of sidebar.matchAll(/\]\(([^)]+)\)/g)) {
  const target = match[1];
  if (/^[a-z]+:/i.test(target)) continue;
  const stem = target.split(/[?#]/)[0].replace(/^\//, '').replace(/\.md$/, '');
  const page = `${stem}.md`;
  const source = applied ? originalPages.get(page) || page : page;
  if (sidebarPages.has(source)) continue;
  sidebarPages.add(source);
  if (plan.keep.includes(source)) continue;
  assert(plan.files[source], `Unmapped sidebar target: ${source}`);
  const destination = plan.files[source];
  const section = path.posix.dirname(destination);
  if (!sectionPages.has(section)) {
    sectionOrder.push(section);
    sectionPages.set(section, []);
  }
  sectionPages.get(section).push(destination);
}
for (const [index, section] of sectionOrder.entries()) {
  assert(section.startsWith(`${String((index + 1) * 10).padStart(3, '0')}-`), `Section order: ${section}`);
  for (const [pageIndex, destination] of sectionPages.get(section).entries()) {
    assert(path.posix.basename(destination).startsWith(`${String((pageIndex + 1) * 10).padStart(3, '0')}-`), `Page order: ${destination}`);
  }
}
const unlisted = Object.keys(plan.files).filter(source => !sidebarPages.has(source)).sort();
assert.deepEqual(unlisted, [...plan.unlistedPageProposals].sort(), 'Unlisted page proposals differ');
console.log(`Validated: ${Object.keys(plan.files).length} page renames, ${Object.keys(plan.directories).length} image-folder renames, ${sectionOrder.length} sections.`);
console.log(`All ${pages.length} Markdown files accounted for; ${unlisted.length} non-sidebar placements.`);
console.log(`Layout: ${applied ? 'after migration' : 'before migration'}.`);
console.log('Read-only check: no files moved or references edited.');