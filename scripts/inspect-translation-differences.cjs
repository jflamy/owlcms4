#!/usr/bin/env node

const fs = require('fs');

const DEFAULT_KEYS = new Set([
  'Records.manageNewRecords',
  'AgeGroups.UploadCustom',
  'NoCollarsUnder',
  'CategoryAssignment.Warning',
  'ManageOwlcms.RestartWarning',
  'DropZone'
]);

function usage() {
  console.error(`Usage: node ${process.argv[1]} <local.csv> <remote.csv> [key ...]`);
  process.exit(2);
}

function parseCsv(text) {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;

  for (let index = 0; index < text.length; index += 1) {
    const character = text[index];
    if (quoted) {
      if (character === '"' && text[index + 1] === '"') {
        field += '"';
        index += 1;
      } else if (character === '"') {
        quoted = false;
      } else {
        field += character;
      }
    } else if (character === '"') {
      quoted = true;
    } else if (character === ',') {
      row.push(field);
      field = '';
    } else if (character === '\n') {
      row.push(field);
      rows.push(row);
      row = [];
      field = '';
    } else if (character !== '\r') {
      field += character;
    }
  }

  if (quoted) {
    throw new Error('CSV ends inside a quoted field');
  }
  if (field !== '' || row.length > 0) {
    row.push(field);
    rows.push(row);
  }
  return rows;
}

function readCsv(path) {
  const text = fs.readFileSync(path, 'utf8').replace(/^\uFEFF/, '');
  const rows = parseCsv(text);
  if (rows.length === 0) {
    throw new Error(`${path} is empty`);
  }
  const header = rows[0];
  const valuesByKey = new Map();
  for (const row of rows.slice(1)) {
    if (row[0]) {
      valuesByKey.set(row[0], row);
    }
  }
  return { header, valuesByKey };
}

function codePoints(value) {
  return Array.from(value, (character) => {
    const codePoint = character.codePointAt(0).toString(16).toUpperCase().padStart(4, '0');
    return `U+${codePoint}`;
  }).join(' ');
}

function describe(value) {
  return {
    json: JSON.stringify(value),
    utf16Length: value.length,
    codePointCount: Array.from(value).length,
    codePoints: codePoints(value),
    nfc: value.normalize('NFC') === value,
    nfd: value.normalize('NFD') === value,
    trimChanged: value.trim() !== value
  };
}

function firstDifference(left, right) {
  const leftPoints = Array.from(left);
  const rightPoints = Array.from(right);
  const limit = Math.min(leftPoints.length, rightPoints.length);
  for (let index = 0; index < limit; index += 1) {
    if (leftPoints[index] !== rightPoints[index]) {
      return index;
    }
  }
  return limit;
}

function differenceWindow(value, index) {
  const points = Array.from(value);
  const start = Math.max(0, index - 12);
  const end = Math.min(points.length, index + 13);
  return points.slice(start, end).join('');
}

if (process.argv.length < 4) {
  usage();
}

const [localPath, remotePath, ...requestedKeys] = process.argv.slice(2);
const keys = requestedKeys.length > 0 ? new Set(requestedKeys) : DEFAULT_KEYS;
const local = readCsv(localPath);
const remote = readCsv(remotePath);

let differences = 0;
for (const key of keys) {
  const localRow = local.valuesByKey.get(key);
  const remoteRow = remote.valuesByKey.get(key);
  if (!localRow || !remoteRow) {
    console.log(`${key}: missing from ${!localRow ? 'local' : 'remote'} CSV`);
    differences += 1;
    continue;
  }

  const width = Math.max(localRow.length, remoteRow.length, local.header.length, remote.header.length);
  for (let column = 1; column < width; column += 1) {
    const localValue = localRow[column] ?? '';
    const remoteValue = remoteRow[column] ?? '';
    if (localValue === remoteValue) {
      continue;
    }

    const language = local.header[column] || remote.header[column] || `col${column}`;
    const localDescription = describe(localValue);
    const remoteDescription = describe(remoteValue);
    const differenceIndex = firstDifference(localValue, remoteValue);
    const localWindow = differenceWindow(localValue, differenceIndex);
    const remoteWindow = differenceWindow(remoteValue, differenceIndex);
    console.log(`\n${key} [${language}]`);
    console.log(`  equal after NFC: ${localValue.normalize('NFC') === remoteValue.normalize('NFC')}`);
    console.log(`  first differing code point: ${differenceIndex}`);
    console.log(`  local:  length=${localDescription.utf16Length}, codePoints=${localDescription.codePointCount}, nfc=${localDescription.nfc}, nfd=${localDescription.nfd}, trimChanged=${localDescription.trimChanged}`);
    console.log(`    window: ${JSON.stringify(localWindow)}`);
    console.log(`    points: ${codePoints(localWindow)}`);
    console.log(`  remote: length=${remoteDescription.utf16Length}, codePoints=${remoteDescription.codePointCount}, nfc=${remoteDescription.nfc}, nfd=${remoteDescription.nfd}, trimChanged=${remoteDescription.trimChanged}`);
    console.log(`    window: ${JSON.stringify(remoteWindow)}`);
    console.log(`    points: ${codePoints(remoteWindow)}`);
    differences += 1;
  }
}

console.log(`\nDifferences inspected: ${differences}`);