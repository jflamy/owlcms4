# Scheduled Checkpoints: Tracker Plugin API vs. Admin Backup (JSON + Logs)

Status: analysis and proposal, 2026-10-14. No implementation changes are authorized by this document.

Related: [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md), [EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md](EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md).

## 1. Purpose and Context

Under the IWF RFP (specifically **5-R03** and **7.3-R03**), OWLCMS does not operate in active-active database replication mode. Instead, a waiting spare laptop (typically the VPT / Interfaces machine running `tracker`) maintains periodic checkpoint copies to meet a defined **5-minute Recovery Point Objective (RPO)**. If the main OWLCMS laptop is lost, the spare starts OWLCMS and imports the latest checkpoint in under a minute, requiring manual re-entry of at most 3–4 lifts.

This document analyzes two ways a `tracker` plugin on the VPT / Interfaces machine could automate 5-minute checkpoints:

1. **Path A — JSON Snapshot via Existing APIs** (WebSocket `428` refresh or HTTP servlet `/competition/export/json/2`).
2. **Path B — Full Backup (JSON + Sealed Audit & Application Logs)** as produced by the Admin Backup interface (`/admin/backup`).

---

## 2. Executive Summary

| Dimension | Path A1: WebSocket `428` (`database_zip`) | Path A2: HTTP Servlet (`/competition/export/json/2`) | Path B: Admin Backup (`JSON + logs` ZIP) |
| --- | --- | --- | --- |
| **Direct URL / API exists today?** | **Yes** (over the already-open WebSocket connection) | **Yes** (`GET /competition/export/json/2` or `/json/1`) | **No** — Vaadin UI component only (`/admin/backup`) |
| **OWLCMS changes required?** | **None** (if raw ZIP/JSON bytes are retained in `tracker-core`) | **None** | **Yes** — new servlet or WebSocket resource type required |
| **Network / Auth constraints** | Uses existing outbound WS connection from OWLCMS to Tracker | Requires Tracker → OWLCMS HTTP reachability + local/backdoor IP | `BackupView` is restricted to **localhost** (or `backdoorList`) + Vaadin session + `ADMIN_PAGES` role |
| **Audit log side-effects** | Logs 1 `export.json` (`channel=websocket`) per refresh | Logs 1 `export.json` (`channel=http`) per request | Logs `export.json` (`channel=audit`) + `audit.snapshot` **and forces `AuditLog.rollover()`** |
| **Suitability for 5-min timer** | **Ideal** — lightweight (~150–250 KB ZIP), skips unchanged checksums | **Good** — exact standalone JSON file, easy to verify SHA-256 | **Unsuitable every 5 min** if using `AuditSnapshot` as-is (creates ~96 rolled log file pairs per stream per 8h day and zips the entire `logs/` tree each time) |

**Key conclusion:**
- **There is currently no direct URL** to obtain the `JSON + logs` admin backup. `/admin/backup` (`BackupView`) uses a Vaadin `LazyDownloadButton` bound to an ephemeral, session-scoped dynamic `DownloadHandler` URL that can only be created by clicking the button in an authenticated browser UI on `localhost` (or a `backdoorList` IP).
- **For the 5-minute RPO checkpoint**, a tracker plugin should capture the **JSON snapshot only** (Path A1 or A2), skipping writes when `databaseChecksum` has not changed.
- **For off-machine log and evidence retention**, either expose `AuditSnapshot` on a dedicated HTTP servlet used only at natural boundaries (`SwitchGroup`, `GroupDone`, manual trigger), or stream/sync closed audit segments without calling `AuditLog.rollover()` every 5 minutes.

---

## 3. Path A: Grabbing a JSON Snapshot Every 5 Minutes

OWLCMS already provides two non-UI ways to obtain a consistent database JSON snapshot. Both pass through `ChecksummedJsonExport`, which streams the JSON, computes its exact byte length and SHA-256 on the fly, and records an audited `export.json` event.

### 3.1 Option A1 — Via the Tracker WebSocket API (`requestDatabaseRefresh`)

#### How it works today
1. When `scoreboardRegistry.initialize()` runs at Tracker startup (`src/hooks.server.js`), it imports every plugin's `helpers.data.js`. A plugin (like `OBS/ledwall`) can start a background singleton timer on module load.
2. `@owlcms/tracker-core` already exports `requestDatabaseRefresh()` (and injects `competitionHub._requestResourcesCallback`), which sends over the active OWLCMS WebSocket:
   ```json
   {
     "status": 428,
     "message": "Precondition Required: Fresh database requested",
     "reason": "database_refresh",
     "refresh": true,
     "missing": ["database"]
   }
   ```
3. In OWLCMS, `WebSocketEventSender.handleServerMessage()` receives the `428`, sees `"database"` in `missing`, and invokes `WebSocketEventForwarder`'s registered callback:
   - `ForwarderPayloadBuilder.exportCompetitionData()` runs `new CompetitionDataV2().exportData(AuditActor.system(), ExportAudit.CHANNEL_WEBSOCKET)`.
   - OWLCMS sends a `database_metadata` frame (`{ "databaseChecksum": "<sha256-Without-ExportDate-And-UpdateKey>" }`) followed by a binary `database_zip` frame containing `competition.json`.
4. In `tracker-core`, `handleDatabaseZipMessage()` (`src/websocket/binary-handler.js`) extracts `competition.json`, parses it, and calls `competitionHub.handleFullCompetitionData(database)`.

#### Subtlety in `tracker-core` today
`handleDatabaseZipMessage()` currently:
- Unzips `competition.json` into a local string `jsonText`, parses it into an object, and **discards both the raw `zipBuffer` and the raw `jsonText`** (unless `LEARNING_MODE=true`).
- Mutates the parsed object if `metadata.databaseChecksum` was sent separately (`database.databaseChecksum = metadata.databaseChecksum`).
- Normalizes `this.databaseState` via `parseV2Database()` (`parseFullCompetitionData()`).

If a tracker plugin merely calls `JSON.stringify(competitionHub.getDatabaseState())`:
- It will **not** match the exact bytes or SHA-256 logged by OWLCMS in `export.json` (`channel=websocket`).
- Any fields transformed or stripped by `parseV2Database()` will differ from a raw OWLCMS V2 export.

#### What is required for Option A1
1. **`tracker-core` (`src/websocket/binary-handler.js` & `src/competition-hub.js`, ~15 lines):**
   - Retain the last raw `zipBuffer` and/or exact `jsonText` bytes on `competitionHub` (e.g., `hub.getLastRawDatabaseJson()` / `hub.getLastDatabaseZipBuffer()`), alongside `lastDatabaseChecksum` and `lastDatabaseLoad`.
   - Note: if `incomingChecksum === this.lastDatabaseChecksum`, `handleFullCompetitionData()` short-circuits (`duplicate_checksum`), which is desirable—the plugin knows no competition data has changed since the last checkpoint and does not need to write a duplicate file.
2. **Tracker plugin (`src/plugins/<category>/checkpoints/helpers.data.js`, ~80 lines):**
   - Register a `globalThis` singleton `setInterval(..., 5 * 60 * 1000)` on server startup (plus an optional listener on `competitionHub.on('fop_update')` for `GroupDone` / `SwitchGroup`).
   - On each tick: call `competitionHub._requestResourcesCallback(['database'], { refresh: true, reason: 'scheduled_checkpoint' })`.
   - Wait for `database:ready` (or `lastDatabaseLoad` advancing) and write `competition_v2_<timestamp>_<shortChecksum>.json` (or `.zip`) to the configured checkpoint directory **only if `lastDatabaseChecksum` changed** since the last saved file.
   - Apply a retention policy (e.g., keep all session-boundary snapshots + the last $N$ 5-minute snapshots).

### 3.2 Option A2 — Via OWLCMS HTTP Servlet (`/competition/export/json/2`)

#### How it works today
OWLCMS already exposes `CompetitionExport` (`@WebServlet`), a plain `HttpServlet` (not a Vaadin UI route) at:
- `/competition/export` and `/competition/export/json/1` — V1 JSON export (`CompetitionData`)
- `/competition/export/json/2` — V2 JSON export (`CompetitionDataV2`)
- `/competition/export/sbde` — Excel SBDE export

#### Access control and audit behavior
- **No Vaadin session or login is required.**
- `CompetitionExport.processRequest()` checks:
  ```java
  String host = ProxyUtils.getClientIp(request);
  boolean allowed = AccessUtils.isLocalNetwork(host) || AccessUtils.checkBackdoor(host);
  ```
  where `AccessUtils.isLocalNetwork(host)` permits any loopback (`127.0.0.1`, `::1`), RFC 1918 private LAN IP (`10.x`, `172.16-31.x`, `192.168.x`), link-local, or IPv6 ULA (`fc00::/7`) address.
- Because the VPT / Interfaces laptop and the OWLCMS laptop are on the same platform switch (RFC 1918 LAN), **Tracker can `fetch('http://<owlcms-host>:8080/competition/export/json/2')` directly with zero OWLCMS code changes**.
- Every HTTP pull streams through `ChecksummedJsonExport` and writes an audit record:
  ```text
  SYSTEM/HTTP | export.json | format=2,channel=http,bytes=...,sha256=...
  ```
  The bytes saved by Tracker are **bit-for-bit identical** to the SHA-256 recorded in OWLCMS's sealed audit log.

#### What is required for Option A2
1. **No changes to `owlcms_69` or `tracker-core`.**
2. **Tracker plugin (~80 lines):**
   - Needs the OWLCMS HTTP base URL (either configured as a plugin option `owlcmsUrl = http://192.168.1.x:8080` or derived from the incoming WebSocket peer IP on `activeConnection._socket.remoteAddress`).
   - Every 5 minutes, optionally check whether any FOP activity occurred since the last pull (using `competitionHub.fopUpdates[fop]?.lastDataUpdate`) so idle periods do not trigger unnecessary exports and `export.json` audit lines.
   - Perform `fetch(`${owlcmsUrl}/competition/export/json/2`)`, compute SHA-256 locally, and atomically write `.tmp` → `.json` to the checkpoint folder.

---

## 4. Path B: Grabbing `JSON + Logs` via the Admin Interface

### 4.1 Why there is no URL way to get it today

The `JSON + logs` package is generated by `AuditSnapshot.writeTo(OutputStream, Scope, AuditActor)` and exposed in the UI through `BackupView` (`@Route("admin/backup")`):

1. **Vaadin Dynamic Resource (`LazyDownloadButton`):**
   - `BackupView` constructs a `LazyDownloadButton` via `DownloadButtonFactory.createDynamicZipDownloadButton(...)`.
   - The download URL is **not a static path**. When a user clicks the button in the browser, Vaadin's client-side engine fires a server RPC event on the UI thread. Only inside that click listener does `LazyDownloadButton` register a `DownloadHandler` on a hidden `<a>` element (`vaadin://...` or `/VAADIN/dynamic/resource/<uiId>/<sessionToken>/<resourceId>`) and invoke `anchor.getElement().callJsFunction("click")`.
   - Without an active Vaadin `UI`, `VaadinSession`, CSRF token, and simulated DOM click, that resource URL does not exist.
2. **Role and Authentication Gate (`@RequiresRole(Role.ADMIN_PAGES)`):**
   - Reaching `/admin/backup` in accounts mode requires logging in as an `admin` user.
3. **Strict Localhost IP Gate (`AdminView.checkLocalAccess()`):**
   - Unlike `CompetitionExport` (which calls `AccessUtils.isLocalNetwork(host)`), `BackupView.beforeEnter()` calls `AdminView.checkLocalAccess()`, which enforces:
     ```java
     if (!AccessUtils.isLocalhost(clientIp) && !AccessUtils.checkBackdoor(clientIp)) {
         throw new AccessDeniedException();
     }
     ```
   - Even if a LAN machine used a headless browser (Playwright/Puppeteer) to log in and click the button, **`AdminView.checkLocalAccess()` rejects LAN IPs with `AccessDeniedException`** unless the Tracker machine's IP is explicitly added to `backdoorList`.

### 4.2 Operational and Audit Side-Effects of `AuditSnapshot` Every 5 Minutes

Even if `AuditSnapshot.stream(Scope.LOGS_AND_EXPORT, actor)` were exposed on a plain `HttpServlet` (like `CompetitionExport`), calling it on a 5-minute timer has major architectural side-effects because `AuditSnapshot` was designed to be *"taken at a quiet point, for the competition director to copy off the machine"*:

1. **Forced Sealed Audit Log Rollover (`AuditLog.rollover()`):**
   - Every call to `AuditSnapshot.writeTo()` writes an `audit.snapshot` record and immediately calls `AuditLog.rollover()` (`AuditFileStore.rollAll()`).
   - `rollAll()` closes the active block on **every** platform/competition stream, writes a signed `FINAL` marker, closes the `.log` file, and opens a brand-new `*_sealed_<timestamp>.log` and `*_plain_<timestamp>.log` pair with a `CONTINUE` marker.
   - Running this every 5 minutes over an 8-hour competition day would force **96 file rollovers per stream** (hundreds of tiny `.log` files across `competition`, `A`, `B`, etc.), defeating the normal 5-minute idle / 500-record block sealing cadence in `AuditSealLimits.DEFAULT`.
2. **Cumulative Zip Growth (`zipLogs`):**
   - `AuditSnapshot.zipLogs()` walks the entire `logs/` directory and packages **all** historical application logs (`owlcms.log`, etc.) and **all** previously rolled audit log files since the competition began, plus a fresh `database_v2_<stamp>.json`.
   - Pulling this full archive every 5 minutes over the network and storing each copy on disk writes $O(N^2)$ cumulative log bytes over the course of a multi-day event.

### 4.3 What Would Be Required to Support Path B via URL / WebSocket

If automated remote collection of `JSON + logs` is desired, OWLCMS needs a small server-side addition:

1. **Expose a Non-UI Endpoint in OWLCMS (`owlcms_69`):**
   - **Option B1 (HTTP Servlet):** Add `/competition/export/backup` (or `/competition/backup`) to `CompetitionExport` (or a new `BackupExportServlet`).
     - Authenticate via `AccessUtils.isLocalNetwork(host)` plus an `X-OWLCMS-Update-Key` header (or HMAC token) matching a configured `ForwardingConnection.updateKey`, rather than unauthenticated LAN access, because `logs/` and `local/` may contain diagnostic details.
   - **Option B2 (WebSocket Precondition):** Register a `"backup"` / `"backup_zip"` callback in `WebSocketEventForwarder` (alongside `"database"`, `"flags"`, etc.) so Tracker can request `missing: ["backup"]` over the existing authenticated WebSocket connection.
2. **Separate "Periodic Checkpoint" Mode from "Full Rollover Snapshot" Mode:**
   - Add a non-rolling mode (or `rollover=false` parameter) for frequent automated pulls:
     - Call `AuditLog.closeBlock(...)` (or rely on `AuditSealLimits.DEFAULT`, which already seals active blocks after 5 minutes of age or 5 minutes of idle time) **without** rolling to a new file.
     - Include `database_v2_<stamp>.json` and the current `logs/audit/` files (or only audit files modified since a `?since=<timestamp>` parameter).
   - Reserve the full `AuditSnapshot.writeTo()` (with `AuditLog.rollover()` and full `logs/` tree) for **session boundaries** (`GroupDone`) or explicit operator backups.

---

## 5. Recommended Architecture for 5-Minute Checkpoints

To satisfy **5-R03**, **7.3-R03**, and **AUDIT_INTEGRITY_SPECIFICATION.md** cleanly:

1. **Every 5 Minutes (and on `SwitchGroup` / `GroupDone`): JSON Snapshot via Path A**
   - Implement a `checkpoints` plugin (or background service) in `tracker` that uses **Path A1** (WebSocket `database_zip` refresh, keeping the raw ZIP/JSON bytes in `tracker-core`) or **Path A2** (`GET /competition/export/json/2`).
   - Only write a new file to disk when `databaseChecksum` (or the FOP `lastDataUpdate` timestamp) has changed since the previous checkpoint.
   - Keep a rolling window of 5-minute files (e.g., last 24 checkpoints = 2 hours) plus permanent copies at `SwitchGroup` and `GroupDone`.
   - Because each export is hashed by `ChecksummedJsonExport`, its exact SHA-256 is recorded in OWLCMS's `competition_sealed_*.log` under `export.json`.

2. **At Session Boundaries (`GroupDone`) or On Demand: Sealed Audit Log Backup**
   - Trigger the full `AuditSnapshot` (`JSON + sealed logs`) at the end of each session (or via a lightweight `/competition/export/backup` servlet / WebSocket message), when rolling the sealed log file is natural and desirable.
