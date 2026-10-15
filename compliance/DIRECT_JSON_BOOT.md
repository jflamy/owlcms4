# Direct JSON Boot

Status: implementation plan; not implemented or runtime-tested.

## Goal and Rules

Launch OWLCMS on a backup machine from a clean JSON export, through the control
panel, without the normal Vaadin import workflow or a manual restart. Supply the
JSON as a local file path or through a plain browser upload form.

- One ingestion attempt: complete successfully or report the cause and stop.
- No retries, rollback, prevalidation pass, or empty-competition fallback.
- The backup machine's existing database is disposable.
- Control-panel recovery only; Docker/cloud continue using UI imports.

## 1. Fix V2 Error Propagation

In [CompetitionDataV2.java](../owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java):

- Make `importData()` throw with the original cause instead of returning `null`.
- Make `restore()` rethrow caught exceptions; keep validation-flag cleanup in
   `finally`.
- Reuse these fixes for UI and startup imports. Leave V1 and warning-only
   `FormatDetector` post-processing unchanged. Add no separate importer.

## 2. Add Startup Import

In [Main.java](../owlcms/src/main/java/app/owlcms/Main.java) and
[Config.java](../owlcms/src/main/java/app/owlcms/data/config/Config.java):

- Read `OWLCMS_LOADJSON` directly from the environment, preserving three states:
   unset = normal startup; empty string = wait for browser upload; nonempty =
   import the supplied absolute path. Do not collapse empty into unset.
- Once the file is available, bootstrap the database prerequisites and use the
   same import/startup sequence for both recovery modes.
- Call `FormatDetector.importData()` once. Success means the complete document
   is parsed, persistence commits, and existing final import processing finishes.
- Defer configuration-dependent initialization until after import. In particular,
   `Config.initConfig()` currently loads timing settings too early; move that work
   after imported resources are available, avoiding stale static defaults.
- Initialize audit, accounts, scoring, platforms, MQTT, and forwarding using the
   recovered data. Record recovery with the source filename and checksum.
- Keep readiness closed until startup completes; Jetty already listens during
   initialization. Never expose partial data.

Prefer continuing boot directly. If tests show a clean restart is necessary,
use one explicit success-triggered launch without the import setting, not a retry.
The panel must handle that transition before HTTP readiness.

### Browser Upload

- In empty-variable mode, serve a plain servlet upload page before database
   initialization. No Vaadin session or competition data is needed for the form.
- Use an HTML file input and multipart POST; configure servlet multipart support
   and receive the file with `request.getPart("file")`.
- Save the complete upload to a temporary file, then trigger the shared startup
   sequence in a background worker. Accept only one submission; reject concurrent
   or subsequent uploads and clean up the temporary file when finished.
- Exempt only the recovery upload/status endpoints from the readiness gate;
   keep operational endpoints blocked. Protect upload access against unauthorized
   database replacement and enforce an upload-size limit.
- Poll recovery status from the page and redirect to `/` after normal readiness.
   Failure still exits without retry; retain diagnostics in the panel/logs.

## 3. Stop on Failure

- At the recovery boundary, log the actual cause and exit with a named terminal
   error code (proposed: 78). Do not merely stop Jetty or continue startup.
- Handle that code in OWLCMS control-panel supervision and
   [MainWrapper.java](../owlcms/src/main/java/app/owlcms/MainWrapper.java), which
   currently retries it. Any recovery failure or early crash is terminal.
- Preserve ordinary restart behavior after readiness, without reimporting JSON.
   Leave unrelated applications' restart policies unchanged.

## 4. Add the Control-Panel Action

Use [launch.go](../../controlpanel/owlcms/launch.go) and
[monitor.go](../../controlpanel/owlcms/monitor.go):

- Add "Launch from backup..." for supported OWLCMS versions: select a local JSON
   or open the browser upload page. Confirm database replacement and require the
   target process to be stopped.
- Inject the import setting only into this recovery child, never saved config or
   normal relaunch environments. Preserve the explicit empty value for browser
   mode. A remembered file is only a picker default.
- Reject conflicting reset/demo/in-memory settings. Apply the same one-shot
   behavior inside the Linux wrapper when used.
- Show progress, then the browser link on readiness or an error with diagnostics.
   On cancellation, stop the child; leave no orphan recovery process.
- In browser mode, open the upload URL and show "Waiting for backup upload".
   Wait for the operator to upload the file, then start initialization.
   An HTTP response from the upload page is not OWLCMS readiness.

## 5. Verify

- Valid V2 and supported V1 exports boot on fresh and existing installations;
   valid empty exports also work. No manual restart is needed.
- Verify unset, empty, and path values through the panel and wrapper. Browser
   upload works without database initialization, waits for the operator,
   rejects duplicate submissions, and redirects only after full readiness.
- Missing, malformed, truncated, and invalid trailing JSON fail visibly. V2 parse
   and restore exceptions retain their cause; validation flags are restored.
- Commit or final-processing failure prevents readiness. UI imports show errors,
   not success. Failure never retries or starts an empty competition.
- Terminal failure works on macOS, Windows, and Linux, including the wrapper.
   Normal later launches and crash restarts never import the backup again.
- Imported locale, resources, timing, accounts, rankings, platforms, and tracker
   data match a normal restart. No partial data is served during recovery.
- Rehearse takeover with the primary inactive and the backup machine's access
   mode, network, credentials, and devices prepared; only saved JSON state returns.