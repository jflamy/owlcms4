# Role-Based Access Control — Remaining Work

Status: the design is implemented (roles, `AccessPolicy`, `AccessControlListener`,
PIN and accounts modes, platform lock, account administration, navigation
hiding, the four required test classes, translations imported). The full design
is archived in [done/ACCESS_CONTROL_DESIGN.md](done/ACCESS_CONTROL_DESIGN.md);
its §18 repository constraints still apply.

Verified 2026-10-22, still open:

- §10.2 one-off openers: only the two `NavigationPage` interfaces,
  `OwlcmsLayout`, `HomeNavigationContent` and `LiftingNavigationContent` call
  `AccessUi.canOpen(...)`. None of the one-off openers listed below is gated.
- §10.3: `Capability.BREAK_MANAGEMENT` exists but
  `AthleteGridContent.breakButtons()` does not use it.
- §13.1: the non-Vaadin entry points have not been reviewed;
  `MoquetteAuthenticator` still logs the client password at debug level
  (`init/MoquetteAuthenticator.java`, `checkValid`).
- §19 Phase 4 acceptance has not been run.

---

## 10. Hiding UI

### 10.2 Other navigation sites
- `OwlcmsLayout` drawer tabs (`createTab`, ~368–436): skip tabs whose target cannot be opened (Home, Preparation, Lifting, Displays, Results, Records, Info…).
- `HomeNavigationContent` buttons using `UI.navigate` (~149–151 and following).
- `RefereeNavigationContent`, `JuryNavigationContent`, `MobileScoreboardsNavigationContent` navigation buttons.
- One-off openers — gate each on its target route class:
  - `AnnouncerContent` (~792) → `MedalCeremonyContent`
  - `BreakManagement` (~504) → `MedalCeremonyContent`
  - `CompetitionEditingFormFactory` (~603), `EditChampionshipsPanel` (~329), `AgeGroupActionsMenu` (~112) → `AgeGroupContent`
  - `AgeGroupContent` (~194) → `ChampionshipsContent`
  - `RecordConfigEditingFormFactory` (~312), `RecordsConfigContent` (~184) → `RecordContent`
  - `PlatformContent` (~174) → `TCContent` for that platform
  - `SessionContent` (~288) → `RegistrationContent` ("Edit athletes")
  - `PublicRecordsContent` (~59) → `preparation/records`
  - `getPage().open(url, "_blank")` sites (`AgeGroupActionsMenu` ~120, `RecordContent` ~303, `TechnicalOfficialContent` ~133): check whether the URL is a route or a download; gate only routes.

### 10.3 In-page capabilities
- Break dialog: `AthleteGridContent.breakButtons()` (~1221) returns `null` (callers already null-check `this.breaks`) unless `hasCapability(BREAK_MANAGEMENT, getFop())`. This covers every `AthleteGridContent` subclass using the base top bar (TC, Jury, Testing, SessionResults…). The marshal page keeps its Pause button (it starts a `BreakType.MARSHAL` break). `AnnouncerContent.createInitialBar` (~723) and `TimekeeperContent.createInitialBar` (~184) need no gate (page roles already imply the capability).
- Medal ceremony links: §10.2.
- "Edit athletes" in `SessionContent`: §10.2.

---


## 13. Out of scope but must keep working

### 13.1 TODO — review the entry points that are not Vaadin routes

Routes are covered by the annotation check (§6). The entry points below are not, and are reviewed separately after phase 1. Each is a plain servlet, filter or WebSocket endpoint, so none goes through `AccessControlListener`.

| URL | Class | Protection today | To check |
|---|---|---|---|
| `/competition/export`, `/competition/export/json/1`, `/competition/export/json/2`, `/competition/export/sbde` | `CompetitionExport` | client IP on the local network, or in the backdoor list; no login | Known not covered by roles. Decide whether roles apply. |
| `/competition/h2` | `H2BackupServlet` | same as the exports (full database backup) | Same decision as the exports. |
| `/simulation/*` (GET and POST) | `SimulationServlet` | localhost or backdoor list | Starts and stops the simulator. Confirm the IP rule is enough. |
| `/controlpanel/stop` (POST) | `ControlPanelServlet` (registered in `EmbeddedJetty`) | localhost socket address, or backdoor list | Confirm the IP rule is enough. |
| `/local/*` | `FileServlet` (shared module) | none | Believed innocuous if it is correctly confined to the `./local` directory and classpath resources, read-only. Verify the confinement (`resolvePath`, URL-decoding, `..`, encoded separators, symlinks) and that nothing sensitive can be reached. |
| `/mqtt` (WebSocket proxy) and the broker ports | `MqttWebSocketProxyEndpoint`, `MoquetteAuthenticator` | MQTT user name and password from `Config`; anonymous when none is configured | Unchanged, but see the logging item below. |
| `/VAADIN/dynamic/...` downloads and upload handlers | Vaadin | bound to the Vaadin session | Confirm they are only created by pages the principal may open. |

Cross-cutting items to review with the servlets:
- **Client IP.** `AccessUtils.getClientIp()` and `ProxyUtils.getClientIp()` use the `X-Forwarded-For` header when it is present, whoever sent it. The backdoor list, the officials whitelist and every "localhost" or "local network" rule depend on it. Decide when the header may be trusted (behind a known proxy) and when only the socket address counts.
- **CORS.** `CorsFilter` (`/*`) allows every origin with credentials and rewrites the session cookie to `SameSite=None`, but only for Vaadin UIDL and heartbeat requests and for `/VAADIN/build/` and `/web-component/`. The servlets above are not affected. Review whether a session cookie usable cross-site is acceptable once accounts exist.
- **Password in logs.** `MoquetteAuthenticator` logs the client-supplied MQTT password at debug level. Remove it (§14: never log passwords).
- **Audit.** Audit requirements are defined in [AUDIT_TRAIL_SPECIFICATION.md](done/AUDIT_TRAIL_SPECIFICATION.md) and [AUDIT_INTEGRITY_SPECIFICATION.md](done/AUDIT_INTEGRITY_SPECIFICATION.md).

---


## 19. Acceptance still to verify

**Phase 4 — Hiding**
- §10.1–10.3.
- Accept: a PLATFORM(A) account sees only lifting buttons for A plus displays; a REGISTRATION+WEIGHIN+RESULTS account sees preparation (athletes, coaches, teams, documents, weigh-in), results and displays; empty groups and drawer tabs are hidden; the Pause button appears only for announcer, timekeeper and marshal.
