# Direct JSON Boot - Remaining Work

Checked against the implementation on 2026-10-25. Completed startup-import
work is omitted. This is a backlog, not a description of available features.

Implement [PEM-based account mode](005-PEM_ACCOUNT_MODE.md) first. That page
covers normal and JSON startup mode selection, admin setup and the admin's
persistent PIN override.

## 1. Finish launch-with-file support

- **Control panel restart rule:** in the controlpanel repository, never restart
  a child launched with `OWLCMS_LOADJSON` set, whether import fails or the
  application crashes later. Reloading would replace competition work.
  Decide from the child's launch environment, not its exit code.
  Normal launches and UI-import restarts remain unchanged.
- **VS Code launch options:** add `${file}` and latest-from-Downloads variants.
  Prompt-for-path configurations already exist.

## 2. Add standby mode

`OWLCMS_STANDBY=true` is not implemented. It will wait for an operator to choose
a local JSON/ZIP before database initialization, then reuse
`Main.initFromImport(Path)`. Do not introduce another importer.

| `OWLCMS_LOADJSON` | Standby page |
| --- | --- |
| File | Show that file with a Load button |
| Folder | List JSON/ZIP candidates newest first, with Load and Refresh |
| Missing setting/path | Fail visibly |

- Start Jetty and open the browser before waiting for selection.
- Reuse `StartupImportSource.candidates()`; refresh the list on each page load.
  An empty folder shows "no checkpoint found yet", not an import attempt.
- Register a plain `/standby` servlet and a filter before Vaadin's readiness
  wait. While waiting, allow `/standby` and `/controlpanel/stop`; redirect `/`
  to `/standby` and reject other requests with 503 (or redirect browser GETs).
- POST accepts only the configured file or a current candidate strictly inside
  the configured folder, from localhost/backdoor access. Never accept arbitrary
  paths. Atomically accept one selection and reject duplicate submissions.
- Redirect to `/` after selection; the existing readiness latch keeps normal
  pages blocked until import and startup finish. Failure is terminal, never
  an empty-competition fallback. Avoid opening the browser a second time.
- In the control panel, label the button **Launch as standby** and carry
  `OWLCMS_STANDBY`/`OWLCMS_LOADJSON` across installed-version upgrades.
  The existing root HTTP readiness probe should reach `/standby`.

## 3. Runtime checks still to record

Existing unit reports pass for source selection (8 tests) and ZIP input
(5 tests). Earlier runtime checks covered V1 imports into memory and H2 file
databases; they are not repeated here as pending work.

- V2, ZIP and an actual tracker checkpoint; valid empty exports; Postgres.
- Missing/empty sources, malformed/truncated/trailing-garbage JSON, bad ZIPs
  and import/final-processing failures: report the cause, never signal ready,
  retry or serve partial data.
- After implementing standby: new candidates on refresh, path confinement,
  duplicate/unauthorized submissions and readiness after selection.
- After implementing the control-panel rule: terminal failure/crash behavior
  on macOS, Windows and Linux. Access-mode acceptance is tracked in
  [005](005-PEM_ACCOUNT_MODE.md).
- Imported locale, resources, timing, accounts, rankings, platforms and tracker
  data match normal startup. Rehearse takeover with the primary inactive;
  only saved competition state returns.
