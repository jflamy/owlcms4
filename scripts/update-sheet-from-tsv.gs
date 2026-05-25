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
 * 2. Paste the contents of es-gender-terms-proposed.tsv into the text area.
 * 3. Click "Preview" first to see what will change without writing anything.
 * 4. If the preview looks correct, click "Apply".
 *
 * SAFETY
 * - Only cells whose current value differs from the proposed value are updated.
 * - Formula cells are skipped and reported.
 * - Keys not found in the sheet are reported but do not cause errors.
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
    .addToUi();
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
  var missingKeys = [];
  var missingCols = {};
  var formulaSkips = [];

  for (var tr = 1; tr < rows.length; tr++) {
    var tsvRow = rows[tr];
    var key = tsvRow[0];
    if (!key) continue;

    if (!rowByKey.hasOwnProperty(key)) {
      missingKeys.push(key);
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

  // Build report
  var lines = [];
  lines.push((apply ? 'Applied' : 'Preview') + ': ' + updates.length + ' cell(s) would be updated');
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

  if (missingKeys.length > 0) {
    lines.push('Keys not found in sheet (' + missingKeys.length + '):');
    for (var mk = 0; mk < missingKeys.length; mk++) {
      lines.push('  ' + missingKeys[mk]);
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

// ---------------------------------------------------------------------------
// TSV parser  (handles optional RFC-4180-style quoted fields)
// ---------------------------------------------------------------------------

function parseTsv(text) {
  text = String(text || '').replace(/^\uFEFF/, '');
  var rows = [];
  var row = [];
  var value = '';
  var inQuotes = false;

  for (var i = 0; i < text.length; i++) {
    var ch = text[i];
    var nx = text[i + 1];

    if (ch === '"') {
      if (inQuotes && nx === '"') { value += '"'; i++; }
      else { inQuotes = !inQuotes; }
      continue;
    }
    if (!inQuotes && ch === '\t') { row.push(value); value = ''; continue; }
    if (!inQuotes && (ch === '\n' || ch === '\r')) {
      if (ch === '\r' && nx === '\n') i++;
      row.push(value);
      rows.push(row);
      row = []; value = '';
      continue;
    }
    value += ch;
  }
  if (value.length > 0 || row.length > 0) { row.push(value); rows.push(row); }
  return rows.filter(function(r) { return r.some(function(c) { return c !== ''; }); });
}
