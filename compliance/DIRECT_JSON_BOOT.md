# Direct JSON Boot

Status: implementation plan; not implemented or runtime-tested.
Revised 2026-10-17: standby mode, folder resolution and ZIP input.

Related: [SCHEDULED_CHECKPOINTS.md](SCHEDULED_CHECKPOINTS.md) produces the
checkpoint files consumed here;
[EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md](EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md)
protects the secrets they contain.

## Goal and Rules

Start OWLCMS from a JSON export or a tracker checkpoint ZIP without the Vaadin
import workflow and without the restart that follows a UI import (up to a minute
on Windows). The file is imported during startup, before configuration and data
initialization, and startup then completes once with the imported data.

Two use cases share the same mechanism:

1. **Launch with a file** (development and support): the file is named by an
   environment variable, typically prompted for or resolved by VS Code.
2. **Standby** (spare laptop under 5-R03 / 7.3-R03): OWLCMS is already running
   and waits for the operator to select a file.

Rules:

- One ingestion attempt: complete successfully or report the cause and stop.
- No retries, rollback, prevalidation pass, or empty-competition fallback.
- The target database is disposable; the import replaces the competition.
- Checksums play no role in loading. The source filename and the SHA-256 of the
  imported JSON bytes are logged for forensic correlation only.

## Settings

Both variables use the normal `OWLCMS_` conventions
(`StartupUtils.getBooleanParam("standby")`, `StartupUtils.getStringParam("loadJson")`)
and can live in the control panel's `env.properties`, a VS Code launch
configuration, or the process environment.

| `OWLCMS_STANDBY` | `OWLCMS_LOADJSON` | Behavior |
| --- | --- | --- |
| unset | unset | Normal startup |
| unset | file | Import that file immediately |
| unset | folder | Import the latest file in the folder immediately |
| `true` | file | Wait on `/standby`; show that file with a **Load** button |
| `true` | folder | Wait on `/standby`; list all candidate files (newest first), each with a **Load** button |

Resolution rules:

- Candidate files are local `*.json` and `*.zip` files directly in the folder
  (there is no browser file-upload form; files are always picked from the local
  filesystem). Temporary files written by the tracker must not use those
  extensions (for example `*.zip.partial`).
- "Latest" means the most recent modification time. The tracker's
  write-then-rename sets it at publication.
- In standby, the folder contents are read when `/standby` is rendered or
  refreshed, so new checkpoints appear while OWLCMS is waiting.
- A missing path, or `OWLCMS_STANDBY=true` without `OWLCMS_LOADJSON`, or a
  folder without candidates in launch-with-file mode fails immediately. In
  standby, an empty folder displays "no checkpoint found yet" with a **Refresh**
  button.
- If `OWLCMS_LOADJSON` is set, `OWLCMS_INITIALDATA` and `OWLCMS_RESETMODE` are
  irrelevant because the import replaces the competition.

## 1. Fix V2 Error Propagation

In [CompetitionDataV2.java](../owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java):

- `importData()` currently logs and returns `null`; `restore()` then fails on
   `updated.getConfig()`, logs, and returns normally. A broken file therefore
   looks like a successful import.
- Make `importData()` throw with the original cause instead of returning `null`.
- Make `restore()` rethrow caught exceptions; keep validation-flag cleanup in
   `finally`.
- Reuse these fixes for UI and startup imports. Leave V1 and warning-only
   `FormatDetector` post-processing unchanged. Add no separate importer.

## 2. Accept ZIP Input

In [FormatDetector.java](../owlcms/src/main/java/app/owlcms/data/export/FormatDetector.java),
`doImportData()`, after wrapping the stream in a `BufferedInputStream`:

- Peek four bytes. If they are `PK\x03\x04`, open a `ZipInputStream`, advance to
   the first non-directory entry ending in `.json`, and continue the existing
   version detection and restore on that entry.
- A ZIP without a `.json` entry, or a truncated ZIP, throws and follows the
   normal failure path.
- Plain JSON is unaffected. This serves the existing UI import
   ([JsonUploadDialog](../owlcms/src/main/java/app/owlcms/nui/preparation/JsonUploadDialog.java),
   which has no file-type restriction) as well as both startup modes. Tracker
   checkpoint ZIPs and the `database_v2_*.json` inside an Admin Backup ZIP are
   both accepted.

## 3. Single-Pass Scorched-Earth Startup Import

In [Main.java](../owlcms/src/main/java/app/owlcms/Main.java),
[EmbeddedJetty.java](../owlcms/src/main/java/app/owlcms/jetty/EmbeddedJetty.java)
and [Config.java](../owlcms/src/main/java/app/owlcms/data/config/Config.java):

- Read `OWLCMS_STANDBY` and `OWLCMS_LOADJSON` in `Main.parseConfig()`.
- Jetty starts and listens before `initConfig` and `initData` run
   (`EmbeddedJetty.run`).
- **Standby wait:** When `OWLCMS_STANDBY=true`, open the browser to the server
   URL (`BrowserUtils.startBrowserIfAppropriate`, which redirects `/` to
   `/standby`) and block the startup thread on a `CompletableFuture<Path>`
   (`standbySelectionFuture.get()`) before initializing the database or
   configuration. With `OWLCMS_LOADJSON` and `OWLCMS_STANDBY` unset/false,
   resolve the target `Path` immediately.
- **Single-pass initialization once the `Path` is chosen:**
  1. Call `JPAService.init(memoryMode, true)` to start with a clean schema.
  2. Set up the startup `OwlcmsSessionThreadLocal` and
     `Gender.initPublicGenderCodeMapString(Locale.ENGLISH)` (required by rank
     and participant-count computations at the end of `restore()`).
  3. Call `FormatDetector.importData()` once on the chosen file. This imports
     `Config`, `Competition`, `Accounts`, `Platforms`, `Athletes`, etc., and
     computes participant counts and athlete ranks.
  4. Run `Config.initConfig()`, `AuditIntegrity.initialize(Config.getCurrent())`,
     `ResourceMonitor.start()`, `AccessStartup.run()`, and
     `AccessStartup.initializeRoleAccountsAfterDataLoad()` once against the
     imported data.
  5. Skip `injectData()` and do not recompute participant counts or athlete
     ranks a second time. Apply `overrideDisplayLanguage()` and
     `overrideTimeZone()`, then run `startMQTT()`,
     `OwlcmsFactory.initDefaultFOP()`, `sendStartupDatabaseToWebSocketTrackers()`,
     and `signalDatabaseReady()` (skipping the duplicate browser launch if it
     was already opened for `/standby`).
- **Access mode and `IWF_COMPLIANCE`:**
  - When a PEM signing key is present (`AuditIntegrity.isInitialized() &&
    AuditIntegrity.current().forcedByKey()`), force `AccessMode.ACCOUNTS` in
    `Config.getAccessMode()` (and lock the access-mode selector in
    `ConfigEditingFormFactory` with the `AuditIntegrity.ForcedByKey` tooltip,
    matching `IWF_COMPLIANCE`). On the spare laptop (which has the PEM key),
    the imported `accounts` and password hashes take effect in `ACCOUNTS` mode
    automatically.
  - When no PEM key is present (development and support), `Config.accessMode`
    remains `@JsonIgnore` and defaults to `PIN`, so the support person is not
    locked out by unknown passwords and can switch to `ACCOUNTS` mode in the UI
    if needed.
- Record the load in the audit log with the source filename and the SHA-256 of
   the JSON bytes (the ZIP entry for a ZIP), for forensics only.
- Keep readiness closed until startup completes (`OwlcmsFactory` latch). Never
   expose partial data.

## 4. Standby Page and Routing

- Because `VaadinServlet` is mapped to `/*` and blocks on
   `OwlcmsFactory.waitDBInitialized()`, register `StandbyServlet` on `/standby`
   and a lightweight `StandbyFilter` on `/*` in
   `EmbeddedJetty.createWebAppContext()`, gated by a `volatile boolean
   standbyWaiting`.
- While `standbyWaiting` is `true`:
  - Allow `/standby` and `/controlpanel/stop` through.
  - Redirect `GET /` (`302 Found`) to `/standby`, and return `503` (or redirect
    browser `GET`s) for any other path.
- `GET /standby` lists the candidate `*.zip` and `*.json` files from
   `OWLCMS_LOADJSON` (newest first, showing filename, modification time and
   age). Each row has its own **Load** button, plus a page **Refresh** button.
- Clicking **Load** sends `POST /standby` with the filename. `StandbyServlet`:
  - Verifies the request comes from localhost / backdoor access.
  - Resolves the filename strictly inside the configured `OWLCMS_LOADJSON`
    folder (or matches the configured file); never accepts an arbitrary path.
  - Atomically completes `standbySelectionFuture.complete(chosenPath)` (rejecting
    any duplicate submission), flips `standbyWaiting = false`, and redirects
    (`303 See Other`) to `/`.
- When the browser follows the redirect to `/`, `StandbyFilter` is now a no-op;
    the request enters `VaadinServlet`, waits on
   `OwlcmsFactory.waitDBInitialized()` for the few seconds the import takes, and
   renders the normal home page as soon as the latch opens.

## 5. Stop on Failure

- Log the actual cause and exit with a named terminal code (proposed: 78). Do
   not merely stop Jetty or continue startup with partial state.
- Handle that code in control-panel supervision and
   [MainWrapper.java](../owlcms/src/main/java/app/owlcms/MainWrapper.java), which
   currently restarts every code from 1 to 127.

## 6. Control Panel

Use [launch.go](../../controlpanel/owlcms/launch.go),
[versionlist.go](../../controlpanel/owlcms/versionlist.go) and
[monitor.go](../../controlpanel/owlcms/monitor.go):

- `prepareOwlcmsLaunch` already passes every `OWLCMS_*` property to the child;
   no special file picker or standby state machine is needed in Go.
- Because `GET /` redirects to `/standby` and `/standby` responds with `200 OK`
   immediately, the control panel's existing `CheckPortReady` probe succeeds as
   soon as Jetty is up (~3 seconds) and displays the browser link
   (`http://localhost:<port>`), which opens `/standby`.
- When `OWLCMS_STANDBY=true` in `env.properties`, label the launch button
   **Launch as standby**.
- Treat exit code 78 as non-restartable.
- `env.properties` is per installed version; carry `OWLCMS_STANDBY` and
   `OWLCMS_LOADJSON` over when a new version is installed on the spare.

Typical spare configuration:

```properties
OWLCMS_STANDBY=true
OWLCMS_LOADJSON=/path/to/tracker/checkpoints
```

## 7. Development and Support

In VS Code launch configurations:

- `"OWLCMS_LOADJSON": "${file}"` loads the JSON or ZIP open in the editor.
- `"OWLCMS_LOADJSON": "${input:jsonFile}"` with a `promptString` input asks for
   a path at launch.
- `"OWLCMS_LOADJSON": "${env:HOME}/Downloads"` (`${env:USERPROFILE}` on
   Windows) starts from the most recently downloaded export.
- Combining with `"OWLCMS_MEMORYMODE": "true"` gives a fast, repeatable state.

## 8. Verify

- Valid V2 and supported V1 exports, plain or zipped, load in both modes on fresh
   and existing installations; valid empty exports also work. No restart occurs.
- Folder resolution picks the newest `*.json`/`*.zip` by modification time and
   ignores temporary files. Standby resolves the default at load time.
- Missing paths, empty folders, malformed, truncated or trailing-garbage JSON,
   truncated ZIPs and ZIPs without `.json` fail visibly. V2 parse and restore
   exceptions retain their cause; validation flags are restored.
- Commit or final-processing failure prevents readiness. UI imports show errors,
   not success. Failure never retries or starts an empty competition.
- The standby page lists candidate files with per-row **Load** buttons, rejects
   arbitrary paths, duplicate submissions and unauthorized clients, and
   redirects to `/` which waits on the readiness latch and opens when ready.
- When a PEM signing key is present (`forcedByKey()`), `AccessMode.ACCOUNTS` is
   forced alongside `IWF_COMPLIANCE`; without a PEM key, the imported database
   starts in `PIN` mode and can be switched to `ACCOUNTS` mode in the UI.
- Terminal failure works on macOS, Windows and Linux, including the wrapper.
- Imported locale, resources, timing, accounts, rankings, platforms and tracker
   data match a normal restart. No partial data is served during the load.
- Rehearse takeover with the primary inactive and the spare's access mode,
   network, credentials and devices prepared; only saved JSON state returns.
