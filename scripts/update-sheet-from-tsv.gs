/**
 * OWLCMS Translation Sheet Updater
 *
 * HOW TO INSTALL
 * 1. Open the OWLCMS translation Google Sheet.
 * 2. Extensions → Apps Script.
 * 3. Paste this entire file, replacing any existing content.
 * 4. Save (Ctrl+S). Close the script tab.
 * 5. Reload the spreadsheet. A new menu "OWLCMS" will appear.
 *
 * HOW TO USE
 * 1. OWLCMS → Apply TSV Translation Updates
 * 2. Paste the contents of the proposed .tsv file into the text area.
 * 3. Click "Preview" first to see what will change without writing anything.
 * 4. If the preview looks correct, click "Apply".
 *
 * DUPLICATE KEYS
 * The updater only updates the first row of a key that appears more than once.
 * Run OWLCMS → Remove Duplicate Keys first: it lists what it will do and asks
 * for confirmation. For each duplicated key it keeps the last row (the one
 * OWLCMS uses), fills its empty cells from the earlier rows (latest non-empty
 * value wins, as when OWLCMS loads the file), then deletes the earlier rows.
 *
 * SAFETY
 * - Only cells whose current value differs from the proposed value are updated.
 * - Keys not found in the sheet are appended as new rows on Apply.
 * - Formula cells are skipped and reported.
 * - The active sheet (the tab selected when you open the menu) is used.
 *   Make sure you are on the correct tab before opening the menu.
 */

// ---------------------------------------------------------------------------
// Menu
// ---------------------------------------------------------------------------

function onOpen() {
  SpreadsheetApp.getUi()
    .createMenu('OWLCMS')
    .addItem('Apply TSV Translation Updates', 'showTsvUpdateDialog')
    .addItem('Remove Duplicate Keys', 'removeDuplicateKeys')
    .addToUi();
}

// ---------------------------------------------------------------------------
// Duplicate keys
// ---------------------------------------------------------------------------

function removeDuplicateKeys() {
  var ui = SpreadsheetApp.getUi();
  var sheet = SpreadsheetApp.getActiveSheet();
  var plan = planDuplicateKeyRemoval(sheet);

  if (plan.groups.length === 0) {
    ui.alert('Remove Duplicate Keys', 'No duplicate keys in sheet "' + sheet.getName() + '".', ui.ButtonSet.OK);
    return;
  }

  var lines = [];
  lines.push('Sheet: ' + sheet.getName());
  lines.push(plan.groups.length + ' duplicated key(s), ' + plan.rowsToDelete.length + ' row(s) to delete, '
      + plan.fills.length + ' empty cell(s) to fill in the kept rows.');
  if (plan.deletedFormulaCells > 0) {
    lines.push(plan.deletedFormulaCells + ' formula cell(s) are in the rows to delete.');
  }
  if (plan.referencingCells.length > 0) {
    lines.push('WARNING: formulas in other rows refer to rows that will be deleted:');
    plan.referencingCells.slice(0, 20).forEach(function(ref) { lines.push('  ' + ref); });
    if (plan.referencingCells.length > 20) {
      lines.push('  ... and ' + (plan.referencingCells.length - 20) + ' more');
    }
  }
  lines.push('');
  for (var g = 0; g < plan.groups.length; g++) {
    var group = plan.groups[g];
    lines.push(group.key + ': keep row ' + group.keepRow + ', delete row(s) ' + group.deleteRows.join(', '));
  }
  lines.push('');
  lines.push('Proceed?');

  var answer = ui.alert('Remove Duplicate Keys', lines.join('\n'), ui.ButtonSet.YES_NO);
  if (answer !== ui.Button.YES) {
    return;
  }

  for (var f = 0; f < plan.fills.length; f++) {
    var fill = plan.fills[f];
    sheet.getRange(fill.row1, fill.col1).setValue(fill.value);
  }
  // bottom-up so the remaining row numbers stay valid
  var rowsToDelete = plan.rowsToDelete.slice().sort(function(a, b) { return b - a; });
  for (var d = 0; d < rowsToDelete.length; d++) {
    sheet.deleteRow(rowsToDelete[d]);
  }
  ui.alert('Remove Duplicate Keys',
      'Done: ' + rowsToDelete.length + ' row(s) deleted, ' + plan.fills.length + ' cell(s) filled.',
      ui.ButtonSet.OK);
}

/**
 * Keeps the last row of each duplicated key. Its empty cells take the latest
 * non-empty displayed value from the earlier rows; the "ia" column and the
 * kept row's formula cells are left alone. Formulas elsewhere that refer to a
 * row being deleted are reported.
 */
function planDuplicateKeyRemoval(sheet) {
  var range = sheet.getDataRange();
  var values = range.getValues();
  var formulas = range.getFormulas();
  var header = values[0].map(String);
  var iaCol = header.indexOf('ia');

  var rowsByKey = {};
  var keyOrder = [];
  for (var r = 1; r < values.length; r++) {
    var key = String(values[r][0] || '').trim();
    if (!key) continue;
    if (!rowsByKey.hasOwnProperty(key)) {
      rowsByKey[key] = [];
      keyOrder.push(key);
    }
    rowsByKey[key].push(r);
  }

  var plan = { groups: [], rowsToDelete: [], fills: [], deletedFormulaCells: 0, referencingCells: [] };
  for (var k = 0; k < keyOrder.length; k++) {
    var key2 = keyOrder[k];
    var rows = rowsByKey[key2];
    if (rows.length < 2) continue;

    var keep = rows[rows.length - 1];
    var earlier = rows.slice(0, rows.length - 1);
    for (var c = 1; c < header.length; c++) {
      if (c === iaCol) continue;
      if (formulas[keep][c] !== '' || String(values[keep][c]) !== '') continue;
      var latest = '';
      for (var e = 0; e < earlier.length; e++) {
        var v = String(values[earlier[e]][c]);
        if (v.trim() !== '') latest = values[earlier[e]][c];
      }
      if (latest !== '') {
        plan.fills.push({ row1: keep + 1, col1: c + 1, value: latest });
      }
    }
    earlier.forEach(function(row) {
      plan.deletedFormulaCells += formulas[row].filter(function(cell) { return cell !== ''; }).length;
      plan.rowsToDelete.push(row + 1);
    });
    plan.groups.push({
      key: key2,
      keepRow: keep + 1,
      deleteRows: earlier.map(function(row) { return row + 1; })
    });
  }

  var deleted = {};
  plan.rowsToDelete.forEach(function(row1) { deleted[row1] = true; });
  var cellRef = /(^|[^A-Za-z0-9_])\$?[A-Za-z]{1,3}\$?(\d+)(?![\d(])/g;
  for (var fr = 1; fr < formulas.length; fr++) {
    if (deleted[fr + 1]) continue;
    for (var fc = 0; fc < formulas[fr].length; fc++) {
      var formula = formulas[fr][fc];
      if (!formula) continue;
      var match;
      cellRef.lastIndex = 0;
      while ((match = cellRef.exec(formula)) !== null) {
        if (deleted[Number(match[2])]) {
          plan.referencingCells.push(header[fc] + ' row ' + (fr + 1) + ': ' + formula);
          break;
        }
      }
    }
  }
  return plan;
}

// ---------------------------------------------------------------------------
// Dialog
// ---------------------------------------------------------------------------

function showTsvUpdateDialog() {
  const htmlBody = `<!DOCTYPE html>
<html>
<head>
  <style>
    body { font-family: Arial, sans-serif; margin: 12px; font-size: 13px; }
    textarea { width: 100%; height: 320px; font-family: monospace; font-size: 11px; }
    .buttons { margin-top: 10px; }
    button { margin-right: 8px; padding: 6px 16px; font-size: 13px; cursor: pointer; }
    pre#result { white-space: pre-wrap; margin-top: 12px; font-size: 12px;
                 background: #f5f5f5; padding: 8px; border: 1px solid #ddd; }
  </style>
</head>
<body>
  <p>Paste TSV content (must include a header row with <code>key</code> and the column name to update).</p>
  <textarea id="tsv" placeholder="key&#9;es&#10;Gender.Men&#9;Masculino"></textarea>
  <div class="buttons">
    <button onclick="run(false)">Preview (no changes)</button>
    <button onclick="run(true)"  style="background:#4caf50;color:#fff">Apply</button>
    <button onclick="google.script.host.close()">Cancel</button>
  </div>
  <pre id="result"></pre>
  <script>
    function run(apply) {
      var result = document.getElementById('result');
      result.textContent = apply ? 'Applying...' : 'Previewing...';
      google.script.run
        .withSuccessHandler(function(msg) { result.textContent = msg; })
        .withFailureHandler(function(err) { result.textContent = 'ERROR: ' + err.message; })
        .applyTsvTranslationUpdates(document.getElementById('tsv').value, apply);
    }
  </script>
</body>
</html>`;

  const html = HtmlService.createHtmlOutput(htmlBody)
    .setWidth(900)
    .setHeight(560);
  SpreadsheetApp.getUi().showModalDialog(html, 'Apply TSV Translation Updates');
}

// ---------------------------------------------------------------------------
// Server-side update logic
// ---------------------------------------------------------------------------

/**
 * @param {string} tsvText  Full TSV text including header row.
 * @param {boolean} apply   If false, only preview; if true, write to sheet.
 * @returns {string}        Human-readable summary.
 */
function applyTsvTranslationUpdates(tsvText, apply) {
  var SKIPPED_COLUMNS = { 'key': true, 'ia': true, '': true };

  var rows = parseTsv(tsvText);
  if (rows.length < 2) {
    throw new Error('TSV must contain a header row and at least one data row.');
  }

  var tsvHeader = rows[0];
  if (tsvHeader[0] !== 'key') {
    throw new Error('First TSV column must be "key". Got: ' + tsvHeader[0]);
  }

  var sheet = SpreadsheetApp.getActiveSheet();
  var dataRange = sheet.getDataRange();
  var values = dataRange.getValues();
  var formulas = dataRange.getFormulas();

  if (values.length < 1) {
    throw new Error('The active sheet appears to be empty.');
  }

  // Build index: column name → column index (0-based) from sheet header row
  var sheetHeader = values[0].map(String);
  var sheetColByName = {};
  for (var c = 0; c < sheetHeader.length; c++) {
    var name = sheetHeader[c];
    if (name && !sheetColByName.hasOwnProperty(name)) {
      sheetColByName[name] = c;
    }
  }

  // Build index: key → row index (0-based) from sheet data
  var rowByKey = {};
  for (var r = 1; r < values.length; r++) {
    var k = String(values[r][0] || '');
    if (k && !rowByKey.hasOwnProperty(k)) {
      rowByKey[k] = r;
    }
  }

  var updates = [];
  var addedRows = [];
  var addedRowByKey = {};
  var missingCols = {};
  var formulaSkips = [];

  for (var tr = 1; tr < rows.length; tr++) {
    var tsvRow = rows[tr];
    var key = tsvRow[0];
    if (!key) continue;

    if (!rowByKey.hasOwnProperty(key)) {
      var addedRow = addedRowByKey[key];
      if (!addedRow) {
        addedRow = {
          key: key,
          rowValues: buildNewSheetRow(key, sheetHeader.length),
          populatedColumns: []
        };
        addedRows.push(addedRow);
        addedRowByKey[key] = addedRow;
      }
      populateNewSheetRow(addedRow, tsvHeader, tsvRow, sheetColByName, SKIPPED_COLUMNS, missingCols);
      continue;
    }
    var sheetRowIdx = rowByKey[key];
    var sheetRow = values[sheetRowIdx];

    for (var tc = 0; tc < tsvHeader.length; tc++) {
      var colName = tsvHeader[tc];
      if (SKIPPED_COLUMNS[colName]) continue;

      if (!sheetColByName.hasOwnProperty(colName)) {
        missingCols[colName] = true;
        continue;
      }

      var sheetColIdx = sheetColByName[colName];
      var newVal = tc < tsvRow.length ? tsvRow[tc] : '';
      var oldVal = String(sheetRow[sheetColIdx] != null ? sheetRow[sheetColIdx] : '');
      var formula = formulas[sheetRowIdx][sheetColIdx];

      if (formula) {
        if (newVal !== formula) {
          formulaSkips.push(key + ' [' + colName + ']: ' + formula);
        }
        continue;
      }

      if (newVal !== oldVal) {
        updates.push({
          key: key,
          col: colName,
          row1: sheetRowIdx + 1,  // 1-based for Sheets API
          col1: sheetColIdx + 1,  // 1-based for Sheets API
          oldVal: oldVal,
          newVal: newVal
        });
      }
    }
  }

  // Block if any required columns are missing
  var missingColList = Object.keys(missingCols);
  if (missingColList.length > 0) {
    throw new Error('Columns not found in sheet header: ' + missingColList.join(', '));
  }

  // Apply
  if (apply && updates.length > 0) {
    for (var u = 0; u < updates.length; u++) {
      sheet.getRange(updates[u].row1, updates[u].col1).setValue(updates[u].newVal);
    }
  }
  if (apply && addedRows.length > 0) {
    var appendStartRow = sheet.getLastRow() + 1;
    sheet.getRange(appendStartRow, 1, addedRows.length, sheetHeader.length)
      .setValues(addedRows.map(function(addedRow) { return addedRow.rowValues; }));
  }

  // Build report
  var lines = [];
  if (apply) {
    lines.push('Applied: ' + updates.length + ' cell(s) updated, ' + addedRows.length + ' row(s) added');
  } else {
    lines.push('Preview: ' + updates.length + ' cell(s) would be updated, ' + addedRows.length + ' row(s) would be added');
  }
  lines.push('Active sheet: ' + sheet.getName());
  lines.push('');

  if (updates.length > 0) {
    lines.push('Changes:');
    for (var u2 = 0; u2 < updates.length; u2++) {
      var up = updates[u2];
      lines.push('  ' + up.key + ' [' + up.col + ']');
      lines.push('    old: ' + up.oldVal);
      lines.push('    new: ' + up.newVal);
    }
    lines.push('');
  }

  if (addedRows.length > 0) {
    lines.push((apply ? 'Rows added' : 'Rows to add') + ' (' + addedRows.length + '):');
    for (var ar = 0; ar < addedRows.length; ar++) {
      var rowToAdd = addedRows[ar];
      lines.push('  ' + rowToAdd.key);
      for (var pc = 0; pc < rowToAdd.populatedColumns.length; pc++) {
        var populatedColumn = rowToAdd.populatedColumns[pc];
        lines.push('    ' + populatedColumn.col + ': ' + populatedColumn.value);
      }
    }
    lines.push('');
  }

  if (formulaSkips.length > 0) {
    lines.push('Formula cells skipped (' + formulaSkips.length + '):');
    for (var fs = 0; fs < formulaSkips.length; fs++) {
      lines.push('  ' + formulaSkips[fs]);
    }
  }

  return lines.join('\n');
}

function buildNewSheetRow(key, columnCount) {
  var row = [];
  for (var c = 0; c < columnCount; c++) {
    row.push('');
  }
  row[0] = key;
  return row;
}

function populateNewSheetRow(addedRow, tsvHeader, tsvRow, sheetColByName, skippedColumns, missingCols) {
  var populatedColumnByName = {};
  for (var existing = 0; existing < addedRow.populatedColumns.length; existing++) {
    populatedColumnByName[addedRow.populatedColumns[existing].col] = addedRow.populatedColumns[existing];
  }

  for (var tc = 0; tc < tsvHeader.length; tc++) {
    var colName = tsvHeader[tc];
    if (skippedColumns[colName]) continue;

    if (!sheetColByName.hasOwnProperty(colName)) {
      missingCols[colName] = true;
      continue;
    }

    var sheetColIdx = sheetColByName[colName];
    var newVal = tc < tsvRow.length ? tsvRow[tc] : '';
    addedRow.rowValues[sheetColIdx] = newVal;

    if (newVal !== '') {
      if (!populatedColumnByName[colName]) {
        populatedColumnByName[colName] = { col: colName, value: newVal };
        addedRow.populatedColumns.push(populatedColumnByName[colName]);
      } else {
        populatedColumnByName[colName].value = newVal;
      }
    }
  }
}

// ---------------------------------------------------------------------------
// TSV parser: fields are plain tab-delimited text; double quotes are literal.
// ---------------------------------------------------------------------------

function parseTsv(text) {
  text = String(text || '').replace(/^\uFEFF/, '');
  return text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .split('\n')
    .map(function(line) { return line.split('\t'); })
    .filter(function(row) { return row.some(function(c) { return c !== ''; }); });
}
