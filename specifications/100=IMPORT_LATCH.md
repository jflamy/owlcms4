# 100 - Import latch during active sessions

Status: proposed (2026-11-24).

## Intent

Prevent destructive imports from changing competition data while lifting is
under way. Registration, SBDE, database, and age-group replacement imports are
blocked for the duration of any active session; files are not filtered or
partially applied.

An active session is a session selected on any field-of-play. This starts when
the session is selected, before the introduction or first clock start, and ends
only when the session is unselected. A platform holds its own athlete copies
and other lifting state during this interval.

The existing `FieldOfPlay.isSessionInProgress()` and
`OwlcmsFactory.getFOPsWithSessionInProgress()` methods already express the
required meaning. The latch must use these definitions; it must not test a
clock state, a break state, or whether an attempt is currently running.

## Decision summary: destructive variants and restart timing

| Import variant | Changes that make it destructive | Active-session policy | Restart after successful import |
| --- | --- | --- | --- |
| Registration: `DELETE_ATHLETES` | Deletes all athletes, recreates imported athletes, rebuilds participations and group links | Block | Delayed restart required |
| Registration: `UPDATE_ADD_ATHLETES` | Updates existing athletes, adds athletes, replaces eligibility/team data, and may reassign sessions | Block | Delayed restart required |
| Registration: `ADD_ATHLETES` | Adds athletes and their participations/session links; existing athletes remain | Block | Delayed restart required if additions are possible |
| Registration: `DELETE_SESSIONS` | Deletes every session and recreates imported sessions/platform links | Block | Delayed restart required |
| Registration: `UPDATE_ADD_SESSIONS` | Creates/fully updates sessions, including platform and times, and rebuilds platform registry | Block | Delayed restart required |
| SBDE with athlete, session, or competition-header changes | Any athlete/session processing, or federation/name/date/venue/organizer header changes | Block | Delayed restart required |
| Registration: `IGNORE_ATHLETES` + `UPDATE_OFFICIALS_AND_DESCRIPTION` + sessions-only file | Updates only matching session descriptions and officials; ignores structure/timing/platform changes | Block under the current global latch | No restart when run with no active session |
| Full database JSON/ZIP | Replaces the complete competition database | Block | **Forced immediate restart; no delayed option** |
| Age-group definitions, uploaded or predefined | Replaces definitions and recomputes every athlete's category eligibility/participations | Block | Delayed restart required |
| Record definitions | Replaces record configuration and platform-visible record caches | Block | Delayed restart required unless complete live refresh is proven |

The full option-by-option explanation is in
[What “destructive registration variant” means](#what-destructive-registration-variant-means).

## Current import surfaces

### Imports already covered by the import gate

The existing `ImportGate` checks authorization and the administrator's
"Imports are allowed" setting for:

| Surface | File type | Current mutation | Restart today |
| --- | --- | --- | --- |
| Registration upload | XLS/XLSX | Replaces or updates athletes and/or sessions, competition settings, and participations | Restart later is allowed for destructive variants; the live-safe path needs no restart |
| SBDE upload | XLS/XLSX | Same broad replacement or update path as registration | Restart later is allowed |
| Full database upload | JSON, or ZIP containing JSON | Replaces the complete competition database | Forced immediate restart after success; no later option |

The first two are started from
[NRegistrationFileUploadDialog.java](../owlcms/src/main/java/app/owlcms/nui/preparation/NRegistrationFileUploadDialog.java).
The database import is handled by
[JsonUploadDialog.java](../owlcms/src/main/java/app/owlcms/nui/preparation/JsonUploadDialog.java)
and
[FormatDetector.java](../owlcms/src/main/java/app/owlcms/data/export/FormatDetector.java).

### Other spreadsheet imports

These are not currently covered by `ImportGate`:

| Surface | File type | Current mutation | Active-platform concern |
| --- | --- | --- | --- |
| Age-group definitions upload | XLS/XLSX | Replaces every age group and recomputes all athlete participations | Refused while active; restart later is allowed |
| Predefined age-group definitions load | XLSX resource | Same replacement as an uploaded age-group file | Refused while active; restart later is allowed |
| Record definitions upload | XLS/XLSX | Replaces matching official/provisional record definitions and modifies record configuration | Active platforms cache record lists and challenge state |
| Session timetable upload | XLSX | Replaces the technical-official timetable and regenerates assignments for all sessions | Refreshes safe session fields into active platforms, but clears and regenerates global assignments |
| Technical officials upload | XLS/XLSX | Replaces all technical-official rows | Does not itself regenerate active-session assignments, but a later assignment operation can |
| Coaches upload | XLS/XLSX | Replaces all coach rows | Normally independent of lifting state |
| Offline session-results upload | JSON database export | Copies attempt/results fields into matching athletes for selected sessions | Must never write through a platform's active athlete copies |

The record, technical-official, timetable, age-group, coach, and offline-results
dialogs are in `owlcms.nui.preparation` or `owlcms.nui.results`.

### Not an import latch case

- The audit integrity page reads a JSON export to verify its signature. It
  does not mutate competition data.
- The startup JSON/checkpoint load runs before the HTTP server accepts users
  and therefore cannot race an active session.
- Templates, local override ZIP files, and exports are not part of this
  specification.

## Required policy

### 1. A single backend latch

Add a backend `ImportLatch` utility that has no Vaadin dependencies.

It must answer two questions:

1. Is at least one session active?
2. Does a proposed import target an active session?

Every interactive import kind must route through this global check:

`ImportLatch.requireNoActiveSession(...)`, which uses
`OwlcmsFactory.getFOPsWithSessionInProgress()`.

`ImportGate.requireAllowed(...)` invokes it after permission and administrator
setting checks, without a per-import-kind allow-list. New `ImportGate.Kind`
values inherit the selected-session latch by default.

The targeted check must compare group IDs, not names or object identity. It is
used by the offline session-results importer.

The latch is a live condition, not a persisted flag. The administrator's
existing `Imports are allowed` switch remains independent.

### 2. Recheck at mutation time

A UI check is only advisory. Every import must be checked again immediately
before its first database mutation.

This closes the race where:

1. the dialog opens with no active session;
2. a platform selects a session while the file is being uploaded or previewed;
3. the user confirms the import.

Imports held for a preview or confirmation must therefore be rejected at the
final commit point, not merely when the dialog opens.

Refusals must be visible to the user and audited with the captured
`AuditOrigin`, the import kind, and a distinct `SESSION_IN_PROGRESS` reason.

### 3. Block destructive imports globally

When any session is active, refuse:

- full database JSON;
- every registration import;
- every SBDE import, including sessions-only and officials/description-only
  variants;
- uploaded and predefined age-group definition replacements;
- record-definition replacements.

These imports may not start mutation and may not display a "will take effect
after restart" success result.

Registration and SBDE validation happen before destructive work. A session
started after validation still triggers the final mutation-time latch check.

### 4. No spreadsheet-import exception

Registration and SBDE uploads are both refused while any session is active.
This includes officials-and-description-only, sessions-only, and athlete-only
variants. The live-safe processor path remains available only when no session
is active.

#### What “destructive registration variant” means

The processor combines one athlete option with one session option. A variant is
destructive unless it is the single restricted `LIVE` case below.

| Athlete option | Actual mutation | Assessment |
| --- | --- | --- |
| `DELETE_ATHLETES` | Deletes every existing athlete before importing the spreadsheet athletes; athlete/session links and participations are rebuilt. | Destructive |
| `UPDATE_ADD_ATHLETES` | Updates matching athletes, creates new athletes, changes eligibility/team participations, and can change session assignment. Existing actual-lift values are preserved by the merge logic, but registration and category state are replaced. | Destructive |
| `ADD_ATHLETES` | Existing athletes are left in place and unmatched spreadsheet athletes are added. New athletes and their participations/session assignments enter the database. | Destructive when there are additions; nevertheless classified destructive because impact is not known until the file is parsed |
| `IGNORE_ATHLETES` | The athlete sheets are not written. | Non-destructive by itself; the session option and competition header decide the result |

| Session option | Actual mutation | Assessment |
| --- | --- | --- |
| `DELETE_SESSIONS` | Deletes all sessions, then imports the spreadsheet sessions. This can remove/reassign athletes and recreates platform/session objects. | Destructive |
| `UPDATE_ADD_SESSIONS` | Rebuilds the platform registry and creates or fully updates sessions, including platform, timing, flags, and officials. | Destructive |
| `UPDATE_OFFICIALS_AND_DESCRIPTION` | Updates only description and technical-official fields on matching existing sessions; ignores timing/platform/creation changes and refreshes active-FOP group fields. | Potentially live-safe, but currently blocked while a session is selected by the global latch |
| `IGNORE_SESSIONS` | No session rows are written. | Non-destructive by itself; athlete or competition-header processing may still make the import destructive |

Additional SBDE behavior:

- A non-sessions-only SBDE file also updates the competition header:
  federation/name, dates, address, city, web site, venue, email, and organizer.
- A normal registration upload defaults to `DELETE_ATHLETES` plus
  `DELETE_SESSIONS`.

The only current `LIVE` registration combination is:

- `IGNORE_ATHLETES`;
- `UPDATE_OFFICIALS_AND_DESCRIPTION`;
- a sessions-only file, so the competition header is not imported;
- the file contains no athlete processing;
- no session is active, because the global import latch now applies.

All other combinations are `RESTART_REQUIRED_LATER_ALLOWED`. In particular,
`UPDATE_ADD_ATHLETES`, `ADD_ATHLETES`, `DELETE_ATHLETES`, session creation or
replacement, platform changes, timing changes, and competition-header changes
must not be treated as live updates.

This classification is also the decision list for future policy:

| Proposed treatment during no active session | Current candidates |
| --- | --- |
| Block outright | No special exception is currently proposed after the active-session latch clears. |
| Force immediate restart | Full database JSON/ZIP only. No registration/SBDE spreadsheet variant is forced currently. |
| Allow with delayed restart | Every destructive registration variant and every SBDE import. |
| Allow live, without restart | Only the restricted registration `IGNORE_ATHLETES + UPDATE_OFFICIALS_AND_DESCRIPTION` sessions-only path. |

### 5. Targeted offline-results rule

An offline session-results import is not a full competition import. It may
proceed during another session if:

- none of its selected target sessions is active;
- the import does not write any field to athletes belonging to an active
  session;
- the selected session IDs are rechecked at mutation time.

If one or more selected sessions are active, refuse the complete results
import before applying any athlete. Partial import is not allowed because it
would make the operation ambiguous.

### 6. Warning-only imports

The following imports do not directly replace active-session athlete,
category, timing, platform, or results state:

- coaches;
- technical officials;
- timetable data, provided the assignment generation step can be separated
  from the timetable-file replacement.

They may remain available while lifting is active, but the dialog must show a
warning that a session is currently selected.

Timetable upload currently regenerates all session assignments immediately.
That generation is destructive for active sessions and must be split:

1. Importing the timetable rows is allowed with the warning.
2. Generating assignments for active sessions remains blocked by the latch.
3. The user can generate assignments after active sessions are unselected.

Manual "Generate session assignments" and "Clear session assignments" are not
file imports, but they perform the same protected mutations and should use the
same guard.

### 7. Restart policy

Every import is classified before it starts. Restart timing is separate from
the active-session latch:

| Postcondition | Meaning |
| --- | --- |
| `LIVE` | Changes are safe in the current process and refresh all necessary live objects |
| `RESTART_REQUIRED_LATER_ALLOWED` | Import commits, the operator is told that a restart is required, and may choose to restart after the dialog closes; lifting must not resume on the stale in-memory model |
| `RESTART_FORCED` | After the commit the process requests a restart/exit unconditionally; there is no “restart later” option |

| Import | Postcondition and timing |
| --- | --- |
| Full database JSON or ZIP | `RESTART_FORCED`: the database replacement invalidates the complete process model. Report success, flush/audit, wait for the notification, then call `RestartUtils.triggerRestart(...)` unconditionally. The page cannot cancel or defer the restart. |
| Destructive registration import | `RESTART_REQUIRED_LATER_ALLOWED`: after commit, offer restart now or allow the operator to close the message and restart later. A restart is required before selecting or resuming a session. |
| All SBDE imports | `RESTART_REQUIRED_LATER_ALLOWED`: same timing as destructive registration imports. |
| Uploaded/predefined age-group definition replacement | `RESTART_REQUIRED_LATER_ALLOWED`: the replacement may complete while no session is active, but the application must be restarted before lifting. |
| Record-definition replacement | `RESTART_REQUIRED_LATER_ALLOWED`, unless a complete active-platform cache refresh is implemented and tested; otherwise restart before lifting. |
| Officials-and-description-only registration update | `LIVE` when it uses the restricted synchronization path and no active session; no restart. |
| Coaches, technical officials, timetable rows without assignment generation | `LIVE`; these do not replace active lifting state. |
| Offline session results for inactive target sessions | `LIVE`; selected sessions and group IDs are rechecked at commit. |

For `RESTART_FORCED`, the following sequence is mandatory:

1. report that the import succeeded and a restart is taking place;
2. flush/audit the operation;
3. call `RestartUtils.triggerRestart(...)` unconditionally;
4. exit even when no restart-capable supervisor is present.

For `RESTART_REQUIRED_LATER_ALLOWED`:

1. commit the import and audit it;
2. show a success message that explicitly says a restart is required;
3. provide a restart-now path when available;
4. allow the operator to defer that restart, but never to select or resume a
   lifting session before restarting.

`triggerRestartIfNeeded()` is appropriate only for the restart-now action of a
later-allowed import. It is not appropriate for a database import: JSON must
use `triggerRestart(...)`, not the conditional helper.

The control panel, local process supervisor, or cloud host determines whether
the process restarts automatically after exit and how the operator is informed.
It does not change the `RESTART_FORCED` requirement.

## User interface

- Show a translated "session in progress" notice in each affected dialog.
- Registration and SBDE spreadsheet imports enumerate the active platforms.
  One active platform uses: “A session is active on platform A. You must exit
  it first (or restart the application), then retry.” Multiple platforms use:
  “Sessions are active on platforms: A and B. You must exit them first (or
  restart the application), then retry.”
- Platform enumeration is locale-aware. Java's `ListFormat` supplies the
  separator/conjunction for the current locale; the singular and plural
  sentences are separate translation keys.
- The Preparation navigation registration and SBDE import buttons remain
  enabled, display the standard warning icon while any session is selected, and
  show that message when activated. A stale open upload dialog remains
  protected at upload and mutation time.
- Database imports retain their separate forced-immediate-restart warning and
  do not use the spreadsheet delayed-restart explanation.
- Keep import buttons and upload controls enabled. When a globally blocked
  import is unavailable, decorate only the affected controls with the standard
  Vaadin warning icon and show the standard existing error notification when
  the user activates them.
- For `RESTART_REQUIRED_LATER_ALLOWED`, show that the import succeeded and
  distinguish “restart now” from “restart later”; “later” must remain an
  explicit choice, not a success message that implies no restart is needed.
- For `RESTART_FORCED`, show that a restart is taking place and do not offer a
  later option.
- Age-group import controls use the same singular/plural active-platform
  messages as registration and SBDE. Use the standard centered error
  notification when either control is activated.
- Do not rely on that disabled state for security or consistency; the backend
  latch is authoritative.
- A dialog may be opened before a session starts. If a session starts before
  confirmation, confirmation must fail with the same translated notice.
- For warning-only imports, make the warning part of the confirmation, not a
  transient page-level message.
- Use the existing translation workflow. Do not edit
  `shared/src/main/resources/i18n/translation4.csv` directly.

## Audit

Extend the import audit vocabulary to distinguish:

- import kind;
- acceptance or refusal;
- `SESSION_IN_PROGRESS` refusal;
- names of active platforms and sessions, when present;
- `LIVE`, `RESTART_REQUIRED_LATER_ALLOWED`, or `RESTART_FORCED` postcondition;
- restart requested, including the reason.

Successful imports should keep their existing import-specific audit records.
This specification adds the latch decision; it does not replace file name,
byte count, signature, or checksum auditing.

## Implementation plan

### Phase 1: latch and tests

- Add the non-UI `ImportLatch`.
- Add a focused test helper that installs active fields-of-play.
- Test global active-session detection, targeted group-ID matching, and the
  no-active-session case.
- Add the `SESSION_IN_PROGRESS` refusal reason and audit output.

### Phase 2: protect the existing gated imports

- Apply the global latch to full database JSON.
- Block every registration and SBDE path.
- Preserve the live-safe officials-and-description path for inactive
  competition periods.
- Recheck after upload processing and immediately before mutation.
- Keep JSON restart unconditional; implement restart-now/later messaging for
  `RESTART_REQUIRED_LATER_ALLOWED` imports.

### Phase 3: protect metadata and results imports

- Guard age-group replacement and predefined age-group loading at the
  repository mutation boundary, then guard record import.
- Guard offline results by selected session ID.
- Add warning-only handling for coach and technical-official imports.
- Split timetable-row import from assignment generation and guard both
  assignment-changing actions.

### Phase 4: UI and verification

- Add shared translated warning/blocked text to the affected dialogs.
- Verify warning icons and click-time refusals without disabling affected
  controls.
- Verify all dialogs still refuse a stale open request after a session starts.
- Verify no destructive import continues after a latch refusal.
- Verify every committed `RESTART_FORCED` JSON import exits, even without a
  control panel.
- Verify later-allowed imports present both the restart requirement and the
  restart-now action without incorrectly claiming that a deferred restart is
  safe for lifting.

## Acceptance criteria

1. No registration upload, SBDE upload, or JSON import can mutate data while
   any session is selected on a platform.
2. An upload dialog opened before session selection cannot bypass the latch.
3. Offline session results cannot modify an active session, but can modify an
   unrelated inactive session.
4. Age-group upload and predefined definition loading are blocked; coach and
   technical-official imports show a warning; timetable generation is blocked
   while a session is active.
5. Officials-and-description-only spreadsheet imports are blocked during
   active sessions and remain usable when no session is active.
6. Every committed `RESTART_FORCED` import exits or restarts; JSON has no later
   option. Every later-allowed import prominently requires a restart before
   lifting resumes.
7. Refused imports are audited and do not produce success notifications.
