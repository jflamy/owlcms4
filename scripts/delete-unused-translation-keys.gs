/**
 * OWLCMS: delete unused translation keys (temporary script).
 *
 * The keys below are used by no source tree sharing the sheet
 * (see scripts/find-unused-translation-keys.py).
 *
 * It has no onOpen, so it can sit next to update-sheet-from-tsv.gs.
 * Remove it from the Apps Script project afterwards.
 *
 * PROJECT ATTACHED TO THE SHEET (opened with Extensions → Apps Script in the sheet)
 * 1. Select the translation tab in the sheet.
 * 2. In the editor, select deleteUnusedTranslationKeys and click Run.
 * 3. Go back to the sheet: a dialog lists the rows to delete and asks for confirmation.
 *
 * STANDALONE PROJECT (script.google.com)
 * 1. Set SPREADSHEET_ID below to the id in the sheet URL (.../spreadsheets/d/<id>/edit).
 *    The tab whose cell A1 is "key" is used; set SHEET_NAME if there is more than one.
 * 2. Run deleteUnusedTranslationKeys: dry run, the rows to delete are in the Execution log.
 * 3. Run deleteUnusedTranslationKeysNow to delete them.
 */

var SPREADSHEET_ID = '';
var SHEET_NAME = '';


var UNUSED_TRANSLATION_KEYS = [
  'Access.Login.TemporaryAdminPassword',
  'Admin.IwfIsOff',
  'Admin.IwfIsOn',
  'Admin.IwfNeedsToken',
  'Admin.IwfOffAtRestart',
  'Admin.IwfOnAtRestart',
  'Admin.IwfStartupRequiresToken',
  'Admin.IwfTurnOff',
  'Admin.IwfTurnOffConfirm',
  'Admin.IwfTurnOffHelp',
  'Admin.IwfTurnOn',
  'Admin.IwfTurnOnConfirm',
  'Admin.IwfTurnOnHelp',
  'Admin.LockedByIwf',
  'Admin.Security',
  'Admin.SetAtStartup',
  'Admin.Translations',
  'AuditIntegrity.ActiveKey',
  'AuditIntegrity.CheckPending',
  'AuditIntegrity.ForcedByKey',
  'AuditIntegrity.Generating',
  'AuditIntegrity.KeyFile',
  'AuditIntegrity.Later',
  'AuditIntegrity.Off',
  'AuditIntegrity.PendingInvalid',
  'AuditIntegrity.PrepareDescription',
  'AuditIntegrity.ReplaceConfirmation',
  'AuditSnapshot.Download',
  'AuditSnapshot.LOGS',
  'AuditSnapshot.LOGS_AND_EXPORT',
  'AuditSnapshot.LOGS_EXPORT_AND_LOCAL',
  'AuditSnapshot.Title',
  'CannotEditRegistration',
  'Competition.DefaultScoringMedalingRulesTab',
  'Competition.MedalsAndTeamsIntro',
  'Competition.teamPointsTitle',
  'Config.EventPublishingDestination1',
  'Config.EventPublishingDestination2',
  'Config.EventPublishingURL',
  'ImportR.LocalRestart',
  'JuryKeypad.ChairOverride',
  'JuryKeypad.Recall',
  'JuryKeypad.RestartVote',
  'JuryKeypad.VotesReceived',
  'JuryKeypad.WaitingForVotes',
  'ScoringSystems.Explanation',
  'TechnicalOfficial.OfficialRole',
  'TechnicalOfficials.OfficialRole',
  'TechnicalSupport',
  'Timetable.SelectCSVFile',
  'Timetable.UploadFailed',
  '\u00df'
];

function deleteUnusedTranslationKeys() {
  var bound = SpreadsheetApp.getActiveSpreadsheet();
  if (!bound) {
    var plan = planUnusedKeyDeletion(standaloneSheet());
    Logger.log('DRY RUN, nothing deleted. Run deleteUnusedTranslationKeysNow to delete.\n' + plan.lines.join('\n'));
    return;
  }
  var ui = SpreadsheetApp.getUi();
  var plan2 = planUnusedKeyDeletion(bound.getActiveSheet());
  if (plan2.error) {
    ui.alert('Delete Unused Keys', plan2.error, ui.ButtonSet.OK);
    return;
  }
  if (ui.alert('Delete Unused Keys', plan2.lines.join('\n') + '\n\nProceed?', ui.ButtonSet.YES_NO) !== ui.Button.YES) {
    return;
  }
  ui.alert('Delete Unused Keys', deleteRows(plan2), ui.ButtonSet.OK);
}

function deleteUnusedTranslationKeysNow() {
  var plan = planUnusedKeyDeletion(standaloneSheet());
  Logger.log(plan.lines.join('\n'));
  if (!plan.error) {
    Logger.log(deleteRows(plan));
  }
}

function standaloneSheet() {
  if (!SPREADSHEET_ID) {
    throw new Error('Set SPREADSHEET_ID at the top of this file (the id in the sheet URL).');
  }
  var spreadsheet = SpreadsheetApp.openById(SPREADSHEET_ID);
  if (SHEET_NAME) {
    var named = spreadsheet.getSheetByName(SHEET_NAME);
    if (!named) throw new Error('No tab named "' + SHEET_NAME + '".');
    return named;
  }
  var candidates = spreadsheet.getSheets().filter(function(sheet) {
    return String(sheet.getRange(1, 1).getValue()).trim() === 'key';
  });
  if (candidates.length !== 1) {
    throw new Error(candidates.length + ' tabs have "key" in A1. Set SHEET_NAME at the top of this file.');
  }
  return candidates[0];
}

function planUnusedKeyDeletion(sheet) {
  var range = sheet.getDataRange();
  var values = range.getValues();
  var formulas = range.getFormulas();
  var plan = { sheet: sheet, rows: [], lines: [], error: null };

  if (String(values[0][0]).trim() !== 'key') {
    plan.error = 'Sheet "' + sheet.getName() + '" does not start with a "key" column. Select the translation tab and run again.';
    plan.lines.push(plan.error);
    return plan;
  }

  var wanted = {};
  UNUSED_TRANSLATION_KEYS.forEach(function(key) { wanted[key] = true; });
  var found = {};
  for (var r = 1; r < values.length; r++) {
    var key = String(values[r][0] || '').trim();
    if (wanted[key]) {
      plan.rows.push(r + 1);
      found[key] = true;
    }
  }
  if (plan.rows.length === 0) {
    plan.error = 'None of the ' + UNUSED_TRANSLATION_KEYS.length + ' keys is in sheet "' + sheet.getName() + '".';
    plan.lines.push(plan.error);
    return plan;
  }
  var missing = UNUSED_TRANSLATION_KEYS.filter(function(key) { return !found[key]; });

  var deleted = {};
  plan.rows.forEach(function(row1) { deleted[row1] = true; });
  var referencing = [];
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
          referencing.push(values[0][fc] + ' row ' + (fr + 1) + ': ' + formula);
          break;
        }
      }
    }
  }

  var lines = plan.lines;
  lines.push('Sheet: ' + sheet.getName());
  lines.push(plan.rows.length + ' row(s) to delete for ' + Object.keys(found).length + ' of ' + UNUSED_TRANSLATION_KEYS.length + ' key(s).');
  if (missing.length > 0) {
    lines.push('Not in the sheet: ' + missing.join(', '));
  }
  if (referencing.length > 0) {
    lines.push('WARNING: formulas in other rows refer to rows that will be deleted:');
    referencing.slice(0, 20).forEach(function(ref) { lines.push('  ' + ref); });
    if (referencing.length > 20) {
      lines.push('  ... and ' + (referencing.length - 20) + ' more');
    }
  }
  lines.push('');
  plan.rows.forEach(function(row1) { lines.push('row ' + row1 + ': ' + values[row1 - 1][0]); });
  return plan;
}

function deleteRows(plan) {
  // bottom-up so the remaining row numbers stay valid
  plan.rows.slice().sort(function(a, b) { return b - a; }).forEach(function(row1) { plan.sheet.deleteRow(row1); });
  return 'Done: ' + plan.rows.length + ' row(s) deleted from sheet "' + plan.sheet.getName() + '".';
}
