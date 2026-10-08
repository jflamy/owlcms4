# Audit Trail Specification

## 1. Scope and Principles

OWL CMS maintains a human-readable audit trail of competition operations.

- One audit stream exists for each platform, plus a `competition` stream for actions not tied to a platform.
- One changed value produces one `athlete.change` record. Several fields saved together produce consecutive records, not one combined record.
- Accepted and refused field-of-play actions are recorded with stable action names.
- Each record identifies the actor, station, affected athlete and attempt where applicable.
- Imports and other bulk operations do not emit thousands of individual changes; they are suppressed or summarized.
- Opening or viewing a page is not audited. The audit trail records actions and changes, not views.
- Audit records are append-only. Cryptographic sealing, rollover and verification are specified in [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md).

## 2. Audit Streams and Routing

### 2.1 Platform streams

The following records go to the affected platform stream:

- field-of-play actions;
- athlete changes, using the platform of the athlete's session after the save;
- session summaries;
- record challenges, improvements and cancellations;
- authentication records when authentication grants access to that platform.

If an athlete has no session, or the session has no platform, the record goes to `competition`.

### 2.2 Competition stream

The following records go to `competition`:

- application startup, shutdown and restart requests;
- successful and failed logins and logouts;
- database JSON exports;
- changes not associated with a platform;
- bulk-operation summaries;
- athlete changes with no assigned platform.

### 2.3 Stream names

Filesystem stream names are sanitized to `[A-Za-z0-9_-]`, replacing other characters with `_`. The original platform name remains in the full audit record.

## 3. Actor and Station

Each record has an `AuditActor`:

| Field | Meaning |
|---|---|
| `mode` | `PIN`, `ACCOUNTS`, or another explicitly defined access mode. |
| `user` | Account username, or `-` when no account identifies the person. |
| `station` | Station inferred from the page or set explicitly by a device/system action. |
| `index` | Referee or jury-member number when applicable. |
| `inferred` | Whether the station was inferred rather than supplied explicitly. |
| `client` | Client IP plus a short hash of the HTTP session ID; never the raw session ID. |
| `device` | Device kind for MQTT/device actions, otherwise `-`. |

### 3.1 Capture rules

- Capture the actor on the Vaadin UI thread before starting background work.
- Do not call `UI.getCurrent()` or `OwlcmsSession` from a worker to recover actor information.
- Device handlers set their actors explicitly.
- Automatic actions use station `SYSTEM`.
- `AuditContext` carries actor and cause through nested synchronous operations and background save tasks. It is always cleared in `finally`.
- A background save with neither an audit context nor a captured UI actor uses `SYSTEM`.

### 3.2 Station mapping

| Origin | Station |
|---|---|
| Admin page | `ADMIN` |
| Announcer page | `ANNOUNCER` |
| Marshal page | `MARSHAL` |
| Timekeeper or Wodkeeper page | `TIMEKEEPER` |
| Technical-controller page | `TC` |
| Jury console | `JURY_CONSOLE` |
| Jury keypad/mobile | `JURY_MEMBER` |
| Referee page/device | `REFEREE` |
| Weigh-in page | `WEIGHIN` |
| Registration, coach or team-selection page | `REGISTRATION` |
| Session/team/final-results pages and Results navigation | `RESULTS` |
| Medal ceremony page | `MEDALS` |
| Testing page | `TESTING` |
| Other preparation pages | `PREPARATION` |
| Simulator | `SIMULATION` |
| Internal timers, field of play, null system origin | `SYSTEM` |
| Unrecognized interactive origin | `UNKNOWN` |

Dialogs and form factories that retain an origin page are repeatedly unwrapped, with a finite depth limit. Station inference follows the page from which an action was performed, not the type of button pressed.

## 4. Audit Records

### 4.1 Field-of-play actions

| Action | Meaning |
|---|---|
| `clock.start`, `clock.stop`, `clock.set`, `clock.timeover` | Competition-clock operations. |
| `break.start`, `break.pause`, `break.end` | Break operations. |
| `lifting.start` | Lifting begins. |
| `session.load` | A session is loaded on a platform. |
| `ceremony.start`, `ceremony.end` | Ceremony operations. |
| `referee.vote`, `referee.decision`, `referee.down` | Referee input and final panel decision. |
| `jury.vote`, `jury.decision`, `jury.summon` | Jury input and actions. |
| `tc.equipment` | Barbell or plate/equipment change. |

Only events accepted after duplicate detection are normally recorded. A refused auditable event is recorded with `.refused` appended to its action and includes the field-of-play state at refusal. Internal reset or propagation events with no audit action are omitted.

### 4.2 Athlete changes

`AthleteRepository.save` compares the persisted athlete with the saved athlete and writes one `athlete.change` record per changed audited field.

Audited fields include:

- name, team, gender, birth date/year and membership;
- session, category, eligible categories and participations;
- individual/team ranking eligibility;
- start number, lot number, entry total and body weight;
- snatch and clean-and-jerk automatic progression, declaration, first change, second change and actual lift;
- snatch or clean-and-jerk withdrawal;
- forced-current status.

Attempt fields use `SN1`–`SN3` and `CJ1`–`CJ3`. The displayed field omits the attempt prefix, for example `change1 - -> 87`.

Automatic progression is annotated as `(+1)` or `(SAME)` when applicable. Athlete deletion uses `athlete.delete`.

### 4.3 Records

| Action | Meaning |
|---|---|
| `record.challenge` | A requested weight challenges a record. |
| `record.new` | A successful lift improves a record. |
| `record.cancelled` | A reversal cancels a previously improved record. |

Record details identify federation, record name, age group, gender, body-weight category and lift. Repeated recomputation does not repeat an unchanged challenge.

### 4.4 Authentication and application lifecycle

| Action | Meaning |
|---|---|
| `auth.login` | Successful login. |
| `auth.login-failed` | Failed login. |
| `auth.logout` | Logout. |
| `application.started` | OWLCMS started. |
| `application.restart-requested` | Restart requested. |
| `application.stopping` | Orderly shutdown initiated. |
| `export.json` | JSON export completed or failed; detail contains format, channel, byte count and SHA-256 on success. |

Login records always go to `competition` and are repeated in a platform stream when the login grants access to that platform.

### 4.5 Session summary

At the start of lifting for a loaded session, write one consecutive summary block:

- `summary.session`: competition and session information;
- `summary.official`: one record per assigned technical-official position;
- `summary.athlete`: one record per eligible athlete in display/protocol order, including start number, lot, birth, membership, team, category, body weight and opening declarations.

The summary is re-armed when a session is loaded again.

### 4.6 Settings and bulk operations

Settings changes use stable action and field names and record old and new values. Bulk operations use `bulk.<operation>` with the affected count instead of individual changes.

## 5. Record Format

### 5.1 Readable form

```text
sequence | time | station | action | athlete | attempt | change and detail
```

Example:

```text
  412 | 14:03:12.345 | MARSHAL      | athlete.change     | DOE, John | SN2 | change1 - -> 87
```

### 5.2 Full form

The full record starts with the exact readable record and appends:

```text
cause=<text|-> | athleteId=<id|-> | platform=<name|-> |
timestamp=<ISO-8601 with offset> | mode=<mode> | user=<name|-> |
client=<ip#hash|-> | device=<kind|->
```

Values escape backslash, `|`, carriage return and newline. Structured details use comma-separated `key=value` pairs and quote values containing whitespace, commas, `=` or quotes.

Sequence numbers reset as specified by the integrity specification. Full records are authoritative; readable records can be regenerated from their leading fields.

## 6. Suppression and Simulation

- Registration/database imports and restore operations run with audit suppression and do not emit individual athlete changes.
- Administrative bulk operations emit one bulk summary.
- Simulators use explicit synthetic actors and are audited so simulation exercises the same audit path.
- Audit suppression is thread-local, nestable and always cleared in `finally`.

## 7. Conformance Requirements

Tests must cover:

- escaping and stable field order;
- station resolution and origin unwrapping without constructing Vaadin UI objects;
- actor propagation into background saves;
- one line per changed athlete field;
- automatic-progression annotations;
- accepted and refused field-of-play events;
- authentication routing;
- platform versus competition stream routing;
- session summaries;
- record challenge de-duplication, improvement and cancellation;
- suppression and bulk summaries.

Cryptographic sealing, rollover, crash handling, key preparation and integrity checking are tested under [AUDIT_INTEGRITY_SPECIFICATION.md](AUDIT_INTEGRITY_SPECIFICATION.md).
