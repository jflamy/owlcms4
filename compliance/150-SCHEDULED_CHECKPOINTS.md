# Scheduled Recovery Checkpoints

Status: redesigned 2026-10-31; not implemented. Supersedes the 2026-10-17 design (a
tracker-core change retaining the WebSocket `database_zip` bytes).

## Goal

Keep recent, loadable copies of the competition off the OWLCMS machine, with the audit
logs sealed alongside, so that a lost machine can be replaced by starting OWLCMS from the
latest copy.

## Design

Each checkpoint is an OWLCMS **backup** (database export and logs), the same zip as the
Backup page produces with "Database export and logs":

- the database export is written first, with its `export.json` audit line (checksum);
- the backup is audited (`audit.snapshot`);
- the audit logs are rolled over: every sealed file is closed and a new one started;
- the zip holds the export and the whole `logs/` tree, every audit file sealed and closed.

A plain export (HTTP, WebSocket or the Export button) does not do this: its audit line goes
into the open file (sealed with its block within about five minutes) and it carries no logs.

### OWLCMS: token-accessible backup

- New servlet `/competition/backup` (`/admin/backup` is the Vaadin page), `?scope=` one of
  `EXPORT`, `LOGS`, `LOGS_AND_EXPORT` (default), `LOGS_EXPORT_AND_LOCAL`.
- Authorized by `ServletTokenGuard.authorize(..., Role.COMPETITION_SUPERVISOR, "backup")`:
  an account token whose owner is Competition Supervisor or admin; in Simple Access mode,
  the local network, as for `/competition/export`.
- Streams `AuditSnapshot.writeTo` with the token's actor; audited as the Backup page is.
  File name `owlcms_backup_<yyyy-MM-dd_HHhmmss>.zip`.

### Tracker: checkpoint plugin

A tracker plugin; tracker-core is not modified.

- Configuration (`config.js`, server-wide `config-override.json5`, re-read at each tick):
  OWLCMS base URL, account token, checkpoint folder, interval (default 5 minutes; 0
  disables).
- **Baseline:** on `database:received` (OWLCMS started, reconnected or sent the database;
  cached reloads included), pull a backup and (re)start the timer.
- **Timer:** every interval, pull a backup if competition activity was seen since the last
  one (`decision`, `fop_update` other than timer and clock changes, `database:received`);
  skip otherwise. Optionally an unconditional pull every 30 minutes.
- **Storage:** write to a temporary name that does not end in `.json` or `.zip`, check that it
  is a ZIP with a `.json` entry, then rename to `owlcms_backup_<timestamp>.zip`. A failed or
  incomplete transfer never replaces the latest good copy. Keep all copies.
- Growth: each zip holds the whole log history. If size becomes a problem, add a `since`
  parameter on the OWLCMS side (files closed since the last backup) rather than unpacking
  in the tracker.

## Recovery

A backup zip loads directly: `OWLCMS_LOADJSON` set to the checkpoint folder loads the most
recent `.json`/`.zip` in it, and `FormatDetector` imports the first `.json` entry of a ZIP,
which is the export (written first). The control panel never restarts a launch with
`OWLCMS_LOADJSON` set, except on exit code 75, without the load
([160](done/160-DIRECT_JSON_BOOT.md)).

### Control panel: launch from checkpoint folder

Moved from 160. In the controlpanel repository: let the operator choose a checkpoint
folder (or a single export) and launch OWLCMS with `OWLCMS_LOADJSON` set to it. Remember the
last folder; carry the setting across installed-version upgrades. Today the variable must
be set by hand.

## Integrity

The export's SHA-256 is in its own `export.json` audit line, inside the sealed logs of the
same zip, so a checkpoint can be verified with the audit integrity check. Connection
credentials in the export are protected as described in
[140](done/140-EXPORT_CREDENTIAL_PROTECTION.md): the tracker stores the zip as received and
never decrypts anything.

## Acceptance

- `/competition/backup`: refused without a token or with a token lacking Competition
  Supervisor; each scope gives the expected entries; audited with the token's account.
- Plugin: baseline pull after OWLCMS starts; timer pulls only after activity; interval 0
  disables; failed transfer leaves the last good copy; copies are byte-identical to what
  OWLCMS sent.
- Recovery: launch from the checkpoint folder in the control panel loads the latest copy;
  the audit integrity check verifies its logs.
