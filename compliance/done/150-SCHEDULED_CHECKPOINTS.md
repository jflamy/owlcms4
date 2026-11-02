# Scheduled Recovery Checkpoints

Status: done 2026-11-01. OWLCMS checkpoint endpoint and audit/log handling, tracker
checkpoint plugin, and control panel launch from checkpoint are implemented.
Supersedes the 2026-10-17 design (a tracker-core change retaining the WebSocket
`database_zip` bytes).

## Goal

Keep recent, loadable copies of the competition off the OWLCMS machine, with up-to-date
copies of the logs, so that a lost machine can be replaced by starting OWLCMS from the latest
copy and the audit trail survives with it.

## Design

Each checkpoint is a backup in **best-effort logs** mode: the database export plus the log
files that changed since the previous checkpoint, copied as they are. It never forces a seal:
no rollover, no waiting for blocks to be sealed. The logs it carries are a convenience, not
the sealed record; the sealed record is produced by the Backup page (which rolls the logs) and
by the audit files closing on their own.

- The database export is written first, with its `export.json` audit line (checksum), then
  `audit.snapshot` is recorded, as for the Backup page.
- Nothing is forced: audit blocks are sealed on their usual limits (500 records, 1 MB, five
  minutes), files close at 10 MB (`AuditSealLimits.fileBytes`) and when OWLCMS stops, and the
  application logs rotate under the logging configuration. Forcing a seal or a rollover every
  few minutes would fragment the audit trail.
- Log files, audit and application alike, are included when modified since `since`, closed
  or still open. An open file is copied as it is at that moment, possibly ending in the
  middle of a record: its sealed blocks verify, and the integrity check reports the rest as
  partial. The next checkpoint carries
  a newer copy of the same file, which supersedes it; a file stops appearing once it is
  closed and unchanged.

The Backup page is unchanged: it rolls the logs and zips the whole `logs/` tree.

### OWLCMS: new audit files at each start

Today, the first record of each stream after a start is appended to that stream's latest
`_sealed_` file when it is under 10 MB (`AuditFileStore.append`, through `latest(prefix)`), so
a file closed when OWLCMS stopped is reopened by the next run. Instead, each start opens new
sealed and plain files for every stream, as the unsealed files already do
(`UnsealedAuditFiles`: each run starts a new pair). The new file is a new run root, as an
appended run is today; nothing links it to the previous file.

No detection is needed, and the same rule covers every way a run ends:

- **Clean stop:** the file ends with its `FINAL` marker and stays closed.
- **Crash:** the file ends where the crash cut it; the integrity check reports the unsealed
  tail as partial, and the next run no longer writes after a cut record.
- **Restore** (logs copied back by hand, if ever): the copied files end where the last
  checkpoint cut them, and are left
  exactly as copied.

So "closed" means "not written by the running OWLCMS", which `AuditLog` already knows, and a
closed file never changes again. Restarts produce more, smaller files; the 10 MB limit still
applies within a run.

Application logs: `owlcms.log` rolls daily and at 10 MB, the same limit as audit files
(`SizeAndTimeBasedRollingPolicy`, `owlcms_<date>.<index>.log`). A busy competition day
writes about 60 MB of application log (USAW Nationals 2024), so without the size limit every
checkpoint would copy the whole day so far; with it, the active file costs at most about
90 ms to zip and 1.5 MB to send. Until it rolls, `owlcms.log` is appended to by name, so each
newer copy contains the older content. Logback picks the next free index at startup, so a
restored `owlcms_<date>.<index>.log` is never overwritten.

### OWLCMS: token-accessible checkpoint backup

- New servlet `/competition/checkpoint` (`/admin/backup` is the Vaadin page).
  - `since` (optional): the timestamp in the name of a previous checkpoint zip; without it,
    every log file is included (first checkpoint into an empty folder). Files modified at or
    after `since` are included (inclusive, so a file modified at the boundary is sent again
    rather than missed).
  - The zip is named with the OWLCMS time taken before anything is selected, in UTC with
    milliseconds and without colons so it is valid on Windows, as audit file names are
    (`2026-10-31T20-37-42.123Z.zip`), and sent as the `Content-Disposition` file name. The
    caller stores it under that name and passes the name back as the next `since` (an ISO
    8601 instant is also accepted; anything else is refused with 400), so the selection uses
    the OWLCMS clock only. A file modified while the zip is built is sent again next time.
- Authorized by `ServletTokenGuard.authorize(..., Role.COMPETITION_SUPERVISOR, "checkpoint")`:
  an account token whose owner is Competition Supervisor or admin; in Simple Access mode,
  the officials PIN (`token` or Bearer) from any address the officials access list allows,
  or the local network when no officials PIN is set, as for all guarded servlets
  (`/competition/export`, `/competition/h2`). Stricter than the exports and the H2
  backup, which require `RESULTS`: the checkpoint carries the audit and application logs, as
  the Backup page does, and `RESULTS` is also held by Competition Director and `EXTERNAL`
  accounts. The token stored in the tracker configuration grants no UI login: the login form
  never accepts an account token (`AccountAuthenticator`), servlets check it per request and
  create no session (`ServletTokenGuard`), and on pages it is honoured only on display output
  pages, bound to that tab, never setting the session principal (`TokenNavigation`,
  `not-token-page` on every other page).
- Built by `AuditSnapshot.writeCheckpoint` (not a `Scope` value, so the Backup page's choices
  are unchanged; no rollover, no seal wait, modified since `since`); audited
  (`audit.snapshot`, `scope=CHECKPOINT`, `channel=http`, with `since`) with the token's actor.

### Tracker: checkpoint plugin

A tracker plugin; tracker-core is not modified.

- Category: `checkpoints`, added to `DEFAULT_LANDING_PAGE_CATEGORIES`
  (`src/lib/server/landing-page-categories.js`) as "Checkpoints", after
  "Video Overlays".
- **Page:** the plugin's page is where checkpoints are set up and run. Like the tracker's
  other control pages (OBS), it is not protected: the tracker is expected on a trusted
  network.
  - **Folder:** a picker that browses the folders of the tracker machine (served by the
    tracker, so a remote browser sees the tracker's disk, not its own), with **New Folder**
    to create a folder inside the one being browsed. The chosen folder is shown with what it
    holds: the number of zips and the newest one.
  - **Interval:** minutes between pulls, default 5.
  - **OWLCMS:** base URL and secret (an account token, or the officials PIN in Simple Access
    mode; sent as an `Authorization: Bearer` header).
  - **Start / Stop:** Start begins polling into the chosen folder: one pull right away, then
    one every interval. Stop ends polling and changes nothing else. Start is refused
    without a folder or a URL; while started, the settings cannot be changed.
  - **Status:** started or stopped, last checkpoint and its time, next pull, last error.
  - The settings and the started state are kept by the tracker (`config-override.json5`),
    so a tracker restart resumes polling if it was started.
- **Folder choice:** an empty folder gets a full first pull (no `since`). A folder that
  already holds checkpoints continues from its newest zip, and Start names that zip, so the
  frozen folder of a lost machine is not fed by mistake; after a failover, New Folder is the
  way to checkpoint the new main.
- **While started:** on `database:received` (OWLCMS started, reconnected or sent the
  database; cached reloads included), pull right away and restart the interval. Pull at
  every interval, with `since`: the tracker does not see everything that adds to the logs
  (weigh-in, registration, settings, application log), so there is no activity filter; a
  quiet interval costs only the export.
- **OWLCMS not answering:** when OWLCMS starts or reconnects it sends its database, and the
  hub emits `database:received`, which triggers a pull right away. The plugin listens with
  `competitionHub.on(...)`, as the OBS plugin does for `decision`, `timer` and `fop_update`,
  registering on Start (and on load when resuming) and re-registering if the hub was
  recreated. Retries cover the rest, typically network failures: when a pull gets no
  response, a 5xx, or an incomplete zip, the plugin tries again once per minute until a pull
  succeeds, then returns to the interval. The status shows "waiting for OWLCMS" with the
  time of the last attempt. Start does not need OWLCMS to be up. After a failover this is
  what keeps the lost machine's folder frozen: the pulls to it keep failing.
- **Refused:** a 401, 403 or 429 is not retried: polling stops and the status shows the
  error. A wrong PIN retried every minute would keep the officials PIN locked out for
  everyone (the servlet counts failures with the PIN logins); the operator fixes the secret
  and presses Start again.
- **Storage:** see the checkpoint folder below. Download to a `.part` name, reject entries
  outside the checkpoint tree (absolute paths, `..`) and extract once into a hidden pending
  folder. If extraction fails, discard it. Move the new database into the checkpoint folder
  first, then merge the changed logs, remove older databases last, and only then rename the
  ZIP to the name OWLCMS sent. Until that final rename, the cursor does not advance; after a
  tracker restart the pending ZIP is completed before another request is made.

### Checkpoint folder: a ready image

```text
<checkpoint folder>/
  database_v2_2026-10-31_16h37.json    latest export only
  logs/                                latest copy of every log file
  zips/2026-10-31T20-37-42.123Z.zip    every checkpoint, as received
```

- **Unpack:** each received ZIP is extracted once into a hidden pending folder. Its new
  database is moved into the checkpoint folder first, then its changed logs are moved over
  older copies; unchanged logs remain where they are. Older `database_v2_*.json` files are
  deleted only after the log merge. An interrupted merge may temporarily leave both the old
  and new complete exports, and is repeated from the pending ZIP; after completion the folder
  has one export and the newest copy of every log file.
- **Stash:** the zips stay in `zips/`, kept as received, so any earlier state can be
  recovered and the image can be rebuilt by unpacking them in name order.
  `OWLCMS_LOADJSON` lists only the files directly in the folder, so it never sees them.
- **Cursor:** `since` is the timestamp in the name of the newest completed zip in `zips/`.
  A failed transfer or merge does not complete its `.part` ZIP, so the cursor does not move
  and nothing is skipped. The cursor
  survives OWLCMS restarts: files closed when OWLCMS stopped are newer than it, so the first
  checkpoint after the restart brings them. Only the first call into an empty folder has no
  `since`.
- **Closed files:** a closed file (audit file closed at 10 MB, by a Backup page rollover, by
  an OWLCMS stop or cut by a crash; rotated application log) never changes again, so it is in
  the first checkpoint after it closed and in no later one, and its copy in `logs/` is final.
- **Logs:** `logs/` is checked with the audit integrity check; files still open at the last
  checkpoint end in a partial block. Once OWLCMS has stopped and the next checkpoint is
  stored, every file is closed and the record is complete.

## Recovery

The checkpoint folder is ready to launch: `OWLCMS_LOADJSON` set to it loads its only
`.json`, the latest export. After that, the folder is the frozen record of the lost machine:
nothing is added to it. The replacement OWLCMS starts with its own `logs/` in its own
installation; logs are not copied at launch. Old and new logs are consolidated afterwards
from the two places: the checkpoint folder's `logs/` (the lost machine up to its last
checkpoint, files open at that time ending in a partial block) and the replacement's
`logs/`. Consolidation itself is not designed yet.

The control panel never restarts a launch with `OWLCMS_LOADJSON` set, except on exit code
75, without the load ([160](done/160-DIRECT_JSON_BOOT.md)). A stored zip can also be loaded
directly: `FormatDetector` imports the first `.json` entry of a ZIP, which is the export
(written first).

### Failover example

`owlcms.local` runs OWLCMS, `tracker.local` runs the tracker and the plugin, with its
checkpoint folder `/checkpoints`. Only `tracker.local` has a checkpoint folder. Launch from
Checkpoint is used only once `owlcms.local` has died.

```text
While owlcms.local runs:
  owlcms.local:  <install>/<version>/logs/        OWLCMS writes here
  tracker.local: /checkpoints/
                   database_v2_…json              latest export from owlcms.local
                   logs/                          latest copy of owlcms.local's logs
                   zips/                          every checkpoint from owlcms.local

After owlcms.local dies and tracker.local launches from /checkpoints:
  tracker.local: <install>/<version>/logs/        the new OWLCMS writes here, own logs
                 /checkpoints/                    unchanged: owlcms.local's record
```

- **Frozen without action:** the plugin still pulls from `owlcms.local`, which no longer
  answers, so nothing is stored and `/checkpoints` stays as it was. The plugin must not be
  pointed at the new OWLCMS with the same folder; checkpointing the new main would need
  another folder, and another machine to be useful.
- **Loss:** what `owlcms.local` wrote after its last stored checkpoint, at most one interval.

### Control panel: launch from checkpoint

Moved from 160. Implemented 2026-11-01 in the controlpanel repository:

- **Setting:** **Select Checkpoints Folder…** in the OWLCMS tab's Options menu opens the
  native folder chooser and saves the folder as `CONTROLPANEL_CHECKPOINT_FOLDER` in the
  shared OWLCMS `env.properties`, so it carries over to newly installed versions. Like the
  other `CONTROLPANEL_` settings, it is not passed to OWLCMS.
- **Offered when present:** whenever the version list is built (control panel start, after
  selecting the folder), the folder is checked for a `.json` directly inside it. If there is
  one, each installed version's Options menu (`createVersionOptionsButton`, next to Port
  Number and Tracker Connection) gets **Launch from Checkpoint**, which asks for
  confirmation and names the export that will replace the database; otherwise the entry is
  not shown.
- **One shot:** that launch alone gets `OWLCMS_LOADJSON` set to the folder, so OWLCMS loads
  the latest `.json` in it; the variable is added to the launch environment
  (`launchOwlcmsLoading`) and never written to `env.properties`, so the next ordinary
  Launch keeps the database. The existing restart rules apply (`startupimport.go`): a
  launch that loaded an export is never restarted with the load, and exit 75 restarts
  without it.

## Integrity

The export's SHA-256 is in its own `export.json` audit line. That line is in an audit file
still being written, so it is sealed with its block within about five minutes and reaches
the tracker in a later checkpoint's copy of that file; the export itself can be verified
once that copy has its block sealed. Connection
credentials in the export are protected as described in
[140](done/140-EXPORT_CREDENTIAL_PROTECTION.md): the tracker unpacks the export as it is and
never decrypts anything.

## Acceptance

- `/competition/checkpoint`: refused without a token or with a token lacking Competition
  Supervisor; in Simple Access mode with an officials PIN, refused without it or with a
  wrong one even from the local network, accepted with it from any address the officials
  access list allows; no rollover (audit file count unchanged); without `since` all log
  files, with `since` only those modified after it (open files included); audited with the
  token's account.
- Token: the checkpoint token is refused as a password on the login form, and on any page
  other than a display output page (`not-token-page`); a successful checkpoint request
  leaves no authenticated session.
- Plugin: nothing is pulled until Start, and nothing after Stop; Start without a folder or a
  URL is refused; New Folder creates the folder in the one being browsed; an empty folder
  gets a full first pull, a folder with checkpoints continues from its newest zip, which
  Start names; a tracker restart resumes polling only if it was started; pull on
  `database:received` and at every interval whether or not activity occurred; while OWLCMS
  does not answer, one attempt per minute until it does; a 401, 403 or 429 stops polling
  with the error shown; a corrupt pending ZIP is discarded and requested again from the
  previous cursor; an interrupted merge makes the new database available first and is
  completed before another request; completed zips are byte-identical to what OWLCMS sent;
  after each completed unpack the folder holds exactly one `.json`, the latest; a zip with
  an entry outside its tree is rejected.
- Logs: a file closed by size, by a Backup page rollover or by an OWLCMS stop reaches
  `logs/` with the next checkpoint and is not sent again; a failed transfer delays it to the
  following checkpoint without losing it; once OWLCMS has stopped and the next checkpoint is
  stored, `logs/` passes the audit integrity check.
- Startup: after a clean stop, a crash, or a restore of cut files, the next start creates new
  sealed and plain files for each stream and leaves every existing audit file byte-identical;
  the integrity check reports a cut file's tail as partial, not as a failure.
- Recovery: with `CHECKPOINT_FOLDER` set to a folder holding a `.json`, the control panel
  offers Launch from Checkpoint, which loads the latest export; without it, or when the
  folder is missing, the entry is absent; the next ordinary Launch does not load again.
