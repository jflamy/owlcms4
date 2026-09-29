# Per-Platform Audit Trail — Detailed Design

Status: design for implementation. Open decisions in §12 have defaults to use until confirmed.
Audience: the implementing agent. Read §13 (repository constraints) first.
Related: [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md) (accounts mode supplies the user identity).

Line numbers are approximate; search by method name.

---

## 1. Goals

- A clean, **separate audit file per platform**, plus one file for actions not tied to a platform.
- **Every interactive action** by an official is recorded: clock starts/stops, time resets, breaks, ceremonies, session switches, referee and jury votes, jury decisions, declarations, changes, withdrawals, lift corrections, weigh-in and registration edits.
- **Individual changes only**: one line per changed value. If the marshal changes two values and then the announcer changes one, the file shows exactly 3 lines.
- Each line says **which station did it**:
  - accounts mode: the user name **and** the station;
  - PIN mode: the station, **inferred** from the page (or device) that produced the action — the core of this design (§4).
- Imports, simulations and other bulk loads are **not** recorded (for now).

Non-goals (this iteration): other entities (sessions, platforms, categories, config) — see §12 A4; tamper-proof hashing; viewing the audit in the UI.

---

## 2. Current state (why it is not auditable today)

- Everything goes to one file, `logs/owlcms.log` (daily rolling, `owlcms/src/main/resources/logback.xml`). No MDC, no `SiftingAppender`.
- `FieldOfPlay.handleFOPEvent` (~738) logs `"{}state {}, event received {} from {}"` with the platform prefix (`FieldOfPlay.getLoggingName`) and a stack-derived "where from". It records the **event type**, not the values, not the station in a stable form, and it also logs events that end up refused (`unexpectedEventInState`, ~4543).
- Athlete edits are **not diffed**. For example, `AthleteCardFormFactory.doUpdate()` (~1340) copies the whole edited athlete onto the original (`Athlete.conditionalCopy(...)`), saves it, and posts `FOPEvent.WeightChange`. Nothing records which fields changed or their old and new values.
- Other places that change athletes without any trace: the quick-correction dialog in `AthleteGridContent` (~325–345: *Clear* and *Reverse* lift), `NAthleteRegistrationFormFactory` (~357), team selection/results (`TeamSelectionContent` ~612/630, `TeamResultsContent` ~448/466, `TeamItemSelectionFormFactory` ~111, `TeamItemResultsFormFactory` ~111), and `FieldOfPlay` itself when it records lifts (~2191, ~3527, ~4117).

---

## 3. Architecture overview

```
UI page / dialog / MQTT handler
   │  creates FOPEvent  ──► captures AuditActor (who/where)  (§4.2)
   │  or saves Athlete  ──► AuditContext on the thread         (§5)
   ▼
FieldOfPlay.handleFOPEvent ──► AuditLog.event(...)            (§6.1)
AthleteRepository.save     ──► AthleteDiff ──► AuditLog.change(...) per field (§6.2)
   ▼
AuditLog (logger "owlcms.audit", programmatic SiftingAppender keyed by platform) (§7)
   ▼
logs/audit/<platform>_<date>.log   and   logs/audit/competition_<date>.log
```

New package: `app.owlcms.audit`, containing:
- `AuditActor`, `Station`, `StationResolver`, `AuditContext`, `AthleteDiff`, `AuditLog`, `AuditFormat`.

---

## 4. Who did it: actor and station inference (core)

### 4.1 `AuditActor`
```
record AuditActor(
    AccessModeTag mode,   // PIN | ACCOUNTS
    String user,          // account username; "-" in PIN mode; "backdoor" for backdoor access
    Station station,      // inferred station (always present)
    Integer index,        // referee or jury member number (1..5), null otherwise
    boolean inferred,     // true when the station comes from page/device inference
    String client,        // client IP + short session hash, e.g. "192.168.1.23#4f2a"; "-" for devices/system
    String device)        // MQTT device kind for device actions, null otherwise
```
- `client` is what distinguishes two laptops running the same station in PIN mode.
  - The IP comes from `AccessUtils.getClientIp()`.
  - The session hash is the first 4 hex characters of SHA-256 of the HTTP session id. Never log the raw session id: it is a credential.
- In accounts mode, `user` comes from the principal (ACCESS_CONTROL_DESIGN §4.5). The station is **still inferred from the page**, because one account may hold several roles (for example PLATFORM) and act from different pages.
  - `inferred` is false only when the account holds exactly one role that maps to that station.
  - Before access-control phase 2 exists, `mode` is always PIN.

### 4.2 Where the actor is captured
The actor must be captured **on the thread and in the context where the action happens**: the UI thread for pages, the MQTT thread for devices. `OwlcmsSession` and `UI.getCurrent()` are only valid on the UI thread (see repository instructions).

- **FOP events:** add a field `AuditActor actor` to `FOPEvent`. It is set in the constructor (`FOPEvent(Athlete, Object origin)`, ~762), next to the existing `stackTrace` capture, by calling `AuditActor.capture(origin)`. Add a setter `setAuditActor(AuditActor)` for MQTT and system overrides (§4.4).
  - `fopEventBus` is a synchronous `EventBus` (`FieldOfPlay` ~1296), so `handleFOPEvent` also runs on the posting thread. Capturing at construction is still preferred: it is explicit and doesn't depend on that.
- **Athlete changes:** `AuditContext` (a thread-local, §5) gives the actor at save time. If no context is set, `AuditActor.fromCurrentUi()` is used. It resolves the station from the **active route target**: the first element of `UI.getCurrent().getInternals().getActiveRouterTargetsChain()`, mapped with the table of §4.3.

### 4.3 Station mapping (PIN-mode inference; also used in accounts mode)

`Station` enum values:
- ANNOUNCER, MARSHAL, TIMEKEEPER, TC, JURY_CONSOLE, JURY_MEMBER, REFEREE, REFEREES (the decision panel), WEIGHIN, REGISTRATION, RESULTS, PREPARATION, MEDALS, TESTING, SYSTEM, UNKNOWN.

`StationResolver.resolve(Class<?> originClass)` works on **classes only**, so it can be unit-tested without Vaadin objects. Instance-specific details (referee number, the page that opened a dialog) are extracted by a thin adapter.

| Origin (class or unwrap rule) | Station | Index |
|---|---|---|
| `AnnouncerContent` | ANNOUNCER | |
| `MarshallContent` | MARSHAL | |
| `TimekeeperContent`, `WodkeeperContent` | TIMEKEEPER | |
| `TCContent` | TC | |
| `JuryContent` (jury console) | JURY_CONSOLE | vote events: `refIndex + 1` |
| `JuryKeypadContent`, `JuryMobileContent` | JURY_MEMBER | jury member number from the event (`JuryMemberDecisionUpdate.refIndex + 1`) or from the page's selection |
| `RefContent` | REFEREE | `getRef13ix()` (1..3) |
| `MedalCeremonyContent` | MEDALS | |
| `TestingContent` | TESTING | |
| `WeighinContent` | WEIGHIN | |
| `RegistrationContent`, `NAthleteRegistrationFormFactory`, `CoachContent`, `TeamSelectionContent` | REGISTRATION | |
| `SessionResultsContent`, `TeamResultsContent`, `PackageContent` | RESULTS | |
| other `nui.preparation.*` pages, `PlatformEditingFormFactory` | PREPARATION | |
| **unwrap** `BreakManagement` / `BreakDialog` | resolve their `origin` (the page that opened the dialog) | |
| **unwrap** `AthleteCardFormFactory` | resolve its `origin` (the `IAthleteEditing` page) | |
| **unwrap** other form factories / dialogs holding an origin page | resolve the origin | |
| `MQTTMonitor` or its inner handler classes | set explicitly by the handler (§4.4) | |
| `ProxyAthleteTimer`, `ProxyBreakTimer`, `FieldOfPlay` (internal), `null` | SYSTEM | |
| `FOPSimulator`, `CompetitionSimulator` | not audited (suppressed, §8) | |
| anything else | UNKNOWN (+ one `logger.warn` per class, naming the class so the table can be extended) | |

Rules:
- Unwrapping is applied repeatedly (a dialog opened from a dialog) with a depth limit (5).
- Inference follows the **page class**, never the button pressed. Example: a clock start pressed on the announcer page is attributed to ANNOUNCER, even though the timekeeper usually does it. This is the honest answer to "which station did it".
- A `null` origin that comes from an interactive path is a bug to fix. For example, `AnnouncerContent` ~359 posts `StartLifting(null)`: check whether that path is interactive and, if so, pass `this`. Until fixed it is attributed to SYSTEM and logs a warning.

### 4.4 Devices (MQTT) and system actions
`MQTTMonitor` has one handler per topic (~195–215). Each handler sets the actor explicitly on the event before posting:

| Topic / handler | Event(s) | Actor |
|---|---|---|
| decision (and deprecated decision) | `DecisionUpdate` | REFEREE, index = `refIndex + 1`, device = `refbox` |
| downEmitted | `DownSignal` | REFEREE, device = `refbox` (no index unless the payload carries one) |
| clock | `TimeStarted`, `TimeStopped`, `ForceTime` | TIMEKEEPER, device = `clock` |
| jury member decision | `JuryMemberDecisionUpdate` | JURY_MEMBER, index = `refIndex + 1`, device = `jurybox` |
| jury decision | `JuryDecision` | JURY_CONSOLE, device = `jurybox` |
| jury break / summon | `BreakStarted`, `SummonReferee` | JURY_CONSOLE, device = `jurybox` |

For device actions, `client` is `-` and `inferred` is false: the topic identifies the device kind for certain.

Automatic actions get station SYSTEM:
- `TimeOver` from the athlete timer;
- break countdown expiry;
- the lifting order advancing after a decision.

---

## 5. Consequences of an action: `AuditContext`

Some saves are consequences of an event. For example, when the third referee votes, `FieldOfPlay` records the lift on the athlete and saves it. Without care, that change would be attributed to REFEREE #3.

- `AuditContext` is a thread-local stack: `AuditContext.run(AuditActor actor, String cause, Runnable r)`. Nested scopes override outer ones. It is always cleared in `finally`.
- `FieldOfPlay.handleFOPEvent` wraps event processing in `AuditContext.run(e.getAuditActor(), eventName, ...)`.
- Where `FieldOfPlay` records a lift after a **full decision** (~2191, ~4117, etc.), wrap the save in an actor `REFEREES` (panel) with cause `decision`, e.g. "GOOD 2-1".
- Where a **jury decision** changes a lift, the actor is JURY_CONSOLE with cause `jury reversal` / `jury confirmation`.
- A save with no context and no UI (a background thread) is attributed to SYSTEM.

---

## 6. What gets written

### 6.1 FOP event lines (one per accepted event)
Written by `FieldOfPlay.handleFOPEvent`:
- only for events that pass duplicate detection (`prevHash`, ~750);
- only when the event was **accepted**, i.e. `unexpectedEventInState` (~4543) was not called for it. Track this with a per-event outcome flag that `unexpectedEventInState` sets.
- Refused and ignored events stay in `owlcms.log` only (A3).

| Event | Audit `action` | Detail recorded |
|---|---|---|
| `TimeStarted` | `clock.start` | athlete, attempt, time remaining |
| `TimeStopped` | `clock.stop` | athlete, attempt, time remaining |
| `ForceTime` | `clock.set` | new allowed time |
| `TimeOver` | `clock.timeover` | athlete, attempt |
| `BreakStarted` | `break.start` | break type, countdown type, duration or target time |
| `BreakPaused` | `break.pause` | time remaining |
| `BreakDone` | `break.end` | break type |
| `StartLifting` | `lifting.start` | session |
| `SwitchGroup` | `session.load` | session name |
| `CeremonyStarted` / `CeremonyDone` | `ceremony.start` / `ceremony.end` | ceremony type, session / age group / championship |
| `DecisionUpdate` | `referee.vote` | referee number, good/bad, athlete, attempt |
| `DecisionFullUpdate` (final panel result) | `referee.decision` | lights (e.g. `G G B`), result, athlete, attempt |
| `DownSignal` | `referee.down` | athlete, attempt |
| `JuryMemberDecisionUpdate` | `jury.vote` | jury member number, good/bad |
| `JuryDecision` | `jury.decision` | reversal/confirmation, success, reason code, athlete, attempt |
| `SummonReferee` | `jury.summon` | referee number (or all / technical controller) |
| `BarbellOrPlatesChanged` | `tc.equipment` | the changed equipment values, if available from the platform; otherwise the event only |
| `WeightChange` | none | covered by the per-field athlete lines of §6.2 |
| `DecisionReset` | none | internal |
| `ExplicitDecision` | `referee.decision` | if still reachable |

### 6.2 Athlete change lines (one per changed field)
- **Single hook:** `AthleteRepository.save(Athlete)`, before the merge, calls `AthleteDiff.diff(before, after)`.
  - `before` is the row as currently stored: read by id in a separate short read, or from the same transaction before the merge.
  - One `AuditLog.change(...)` line is written per changed audited field.
  - The hook covers every save site listed in §2 without touching each one.
- **Audited fields** are an explicit, reviewed list in `AthleteDiff` (not reflection over all fields):
  - identification: last name, first name, team, gender, birth date/year, membership;
  - session (group), category, eligible categories, participation flags, start number, lot number;
  - entry total, weigh-in body weight;
  - snatch and clean & jerk: each attempt's automatic progression, declaration, change 1, change 2, actual lift;
  - withdrawal / forced-as-current flags.
  - The implementer checks `Athlete` for the exact getter names and includes every field that affects the lifting order, results or rankings.
- **Value formatting:** raw stored values (`85`, `-87` for a failed lift, empty for null), so the file stays unambiguous.
- **No-op saves** (nothing changed) write nothing.
- **Known risk:** `AthleteCardFormFactory` copies the whole form (`Athlete.conditionalCopy`, snapshot taken at dialog open ~396–407) back onto the athlete. If another station changed a field while the card was open, the save may silently revert it. The DB-level diff will show that revert as a change by the saving station. That is truthful, and it makes the lost update visible. Fixing the lost update itself is out of scope; record it as a follow-up.

### 6.3 Line format
One line per record, fields in fixed order, separated by ` | `. Values are escaped: `\` → `\\`, `|` → `\|`, newline → `\n`. Timestamp is ISO-8601 with milliseconds and offset.

```
<timestamp> | seq=<n> | platform=<name|-> | mode=<PIN|ACCOUNTS> | user=<name|-> | station=<STATION[#index]> | inferred=<true|false> | client=<ip#hash|-> | device=<kind|-> | action=<action> | athlete=<id> "<LASTNAME, Firstname>" | attempt=<SN1..CJ3|-> | field=<name|-> | old=<value|-> | new=<value|-> | cause=<text|-> | detail=<text|->
```

Examples:
```
2026-10-04T14:03:12.345-04:00 | seq=412 | platform=A | mode=PIN | user=- | station=MARSHAL | inferred=true | client=192.168.1.23#4f2a | device=- | action=athlete.change | athlete=123 "DOE, John" | attempt=SN2 | field=change1 | old=- | new=87 | cause=- | detail=-
2026-10-04T14:03:12.346-04:00 | seq=413 | platform=A | mode=PIN | user=- | station=MARSHAL | inferred=true | client=192.168.1.23#4f2a | device=- | action=athlete.change | athlete=123 "DOE, John" | attempt=SN3 | field=declaration | old=- | new=90 | cause=- | detail=-
2026-10-04T14:03:40.001-04:00 | seq=414 | platform=A | mode=PIN | user=- | station=ANNOUNCER | inferred=true | client=192.168.1.10#9b1c | device=- | action=athlete.change | athlete=145 "ROE, Ann" | attempt=CJ1 | field=declaration | old=- | new=110 | cause=- | detail=-
2026-10-04T14:04:05.200-04:00 | seq=415 | platform=A | mode=PIN | user=- | station=TIMEKEEPER | inferred=true | client=192.168.1.31#77e0 | device=- | action=clock.start | athlete=123 "DOE, John" | attempt=SN2 | field=- | old=- | new=- | cause=- | detail=remaining=60000
2026-10-04T14:04:31.870-04:00 | seq=416 | platform=A | mode=PIN | user=- | station=REFEREE#2 | inferred=false | client=- | device=refbox | action=referee.vote | athlete=123 "DOE, John" | attempt=SN2 | field=- | old=- | new=GOOD | cause=- | detail=-
2026-10-04T14:04:33.012-04:00 | seq=419 | platform=A | mode=PIN | user=- | station=REFEREES | inferred=false | client=- | device=- | action=athlete.change | athlete=123 "DOE, John" | attempt=SN2 | field=actualLift | old=- | new=87 | cause=decision G G B | detail=-
```

`seq` is a per-platform counter, monotonic for the life of the server process. At startup, each audit file gets a header line with `action=audit.open`, the server version, the start time and a random run id, so a gap in `seq` or a restart is visible.

---

## 7. Files and logging setup

- Logger name `owlcms.audit`, level INFO, **additivity false** (audit lines do not go to `owlcms.log`).
- The appender is configured **programmatically** by `AuditLog` at startup, with the Logback API, rather than in `logback.xml`. Local overrides of `logback.xml` and `LogbackConfigReloader` reloads must not be able to disable or redirect it.
  - If a reload resets the logger context, re-attach the appender. Listen for context resets, or re-check before each write.
- A `SiftingAppender` uses discriminator key `auditPlatform` (default `competition`). Each child is a `RollingFileAppender`:
  - file `logs/audit/<sanitized platform>.log`;
  - daily rollover to `logs/audit/<sanitized platform>_%d{yyyy-MM-dd}.log`;
  - **no `maxHistory`**: audit files are never deleted automatically;
  - pattern `%msg%n` (the line is fully formatted by `AuditFormat`).
- Platform file names are sanitized: only `[A-Za-z0-9_-]` is kept and everything else becomes `_`. The real platform name is always in the `platform=` field.
- MDC is set only around the single write: `MDC.put("auditPlatform", ...)`, log, then `MDC.remove` in `finally`. This keeps it thread-safe with the async UI event buses.
- The `logs/` directory is resolved the same way as the existing `logs/owlcms.log`.

Which file a line goes to:
- **FOP events:** `event.getFop().getName()`.
- **Athlete changes:** the platform of the athlete's session **after** the save. For an athlete with no session, or a session with no platform, the line goes to `competition`.

---

## 8. Exclusions (imports, simulations, bulk loads)

`AuditContext.suppressed(Runnable)` sets a suppression flag on the thread. `AthleteRepository.save` and `AuditLog` skip everything while it is set. Wrap:
- registration upload processing (`NRegistrationFileProcessor` and its upload dialog);
- competition JSON import (`CompetitionData`, `CompetitionDataV2`, `FormatDetector` import paths);
- SBDE import and records imports (`RecordImportDialog` and the records loaders);
- session import (`SessionImportContent` processing);
- simulation (`CompetitionSimulator`, `FOPSimulator`);
- demo/initial data creation (`InitialData`) and database migrations (for example `UtcNormalizationMigration`).

Interactive **bulk** operations started by a user, such as "clear all lifts", start number assignment or "delete all athletes": see A2. Default: a single summary line (`action=bulk.<name>`, count in `detail`), with per-field lines suppressed.

---

## 9. Accounts mode integration

- When ACCESS_CONTROL_DESIGN phase 2 is implemented, `AuditActor.capture` reads the principal from `OwlcmsSession`:
  - `mode=ACCOUNTS`;
  - `user=<username>`;
  - a backdoor principal gives `user=backdoor`.
- Station inference is unchanged. The audit then shows both **who** (the account) and **where** (the station).
- A login platform is not needed for routing audit lines: the platform always comes from the event or the athlete.

---

## 10. Tests

Follow the `add-test-case` skill: **no Vaadin UI objects**.
- `StationResolverTest`:
  - class-to-station mapping for every class in the table in §4.3;
  - unwrap chains (break dialog → announcer; athlete card → marshal);
  - depth limit;
  - unknown class → UNKNOWN.
- `AthleteDiffTest`: plain `Athlete` instances.
  - one change → one line; three changes → three lines; no change → none;
  - null ↔ value;
  - failed lift sign;
  - non-audited fields ignored.
- `AuditFormatTest`: escaping of `|`, `\`, newlines and quotes in athlete names; field order; timestamp format.
- `AuditContextTest`: nesting, suppression, cleanup after exceptions.
- Coverage test: every class that implements `IAthleteEditing`, and every `@Route` class under `nui.lifting` and `nui.referee`, resolves to a station other than UNKNOWN.
- Running tests needs human consent (repo rule). Use the `run-java-test` or `run-maven-test` skill once authorized.

---

## 11. Phases and acceptance criteria

**Phase 1 — Infrastructure and FOP events**
- Build `AuditActor`, `Station`, `StationResolver`, `AuditContext`, `AuditLog` (programmatic sifting appender) and `AuditFormat`.
- Add the actor to `FOPEvent`, set it explicitly in the MQTT handlers, and write the event lines from `handleFOPEvent`.
- Accept:
  - on a 2-platform competition, each platform has its own audit file;
  - every clock start/stop, break, ceremony, session load, referee vote, panel decision, jury vote/decision and summon appears once, with the right station;
  - device actions show `device=`;
  - refused and duplicate events don't appear;
  - audit lines are absent from `owlcms.log`.

**Phase 2 — Athlete field changes**
- Add the `AthleteDiff` hook in `AthleteRepository.save`, the `AuditContext` wrapping in `handleFOPEvent`, the decision/jury attribution, and the import/simulation suppression.
- Accept:
  - marshal changes two values then the announcer changes one → exactly 3 `athlete.change` lines with the right stations;
  - a referee decision produces one `actualLift` line attributed to REFEREES;
  - a registration upload and a JSON import produce no audit lines;
  - weigh-in body weight edits appear in the file of the athlete's session platform.

**Phase 3 — Accounts**
- Fill `mode` and `user` from the principal.
- Accept: in accounts mode, lines show the username and the inferred station.

---

## 12. Decisions

Resolved: the audit appender is configured in code with the Logback API (§7), not in `logback.xml`.

Still open (implement the default; keep each change local):

| Id | Question | Default until confirmed |
|---|---|---|
| A1 | Line format: readable ` \| `-separated key=value (as above) or JSON Lines. | Readable key=value. |
| A2 | Interactive bulk operations: one summary line, or every field. | One summary line. |
| A3 | Record refused/ignored events (for example a jury decision refused in the current state) in the audit? | No (kept in `owlcms.log`). |
| A4 | Also audit other entities (sessions/groups, categories, platforms, technical officials, config) through `OwlcmsCrudFormFactory.performOperationAndCallback` (~484)? | Not in this iteration. |
| A5 | Pre-competition registration edits: audited (to the session's platform, or `competition`)? | Yes, since they are interactive changes. |
| A6 | Retention: never deleted automatically, under `logs/audit/`. | Yes. |

---

## 13. Repository constraints for the implementer

- Read `.github/copilot-instructions.md` and `AGENTS.md` first.
- No `mvn`, builds or test runs without explicit human consent. No git commits/pushes without consent.
- No fully-qualified class names in Java sources.
- Logging conventions: follow the `owlcms-logging-format` skill for the regular log. The audit line format in §6.3 is separate and must not use the `FOP` prefix convention.
- Vaadin concurrency:
  - capture everything that needs `UI`/`OwlcmsSession` on the UI thread (§4.2);
  - never block inside `ui.access`;
  - audit writes are simple appends and are safe from any thread.
- Never log passwords, PINs or raw session ids.
- After each Java edit, validate with the `verify-java-fix` skill.
- A new `@Subscribe` method or new fields on `FOPEvent` require a JVM restart (DCEVM does not re-register subscribers).
