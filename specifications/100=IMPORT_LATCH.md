# 100 - Import latch during active sessions

Status: proposed (2026-11-24).

## Intent

Prevent destructive imports from changing competition data while lifting is
under way. Registration and SBDE imports are completely blocked for the
duration of any active session; spreadsheet files are not filtered or partially
applied.

An active session is a session selected on any field-of-play. This starts when
the session is selected, before the introduction or first clock start, and ends
only when the session is unselected. A platform holds its own athlete copies
and other lifting state during this interval.

The existing `FieldOfPlay.isSessionInProgress()` and
`OwlcmsFactory.getFOPsWithSessionInProgress()` methods already express the
required meaning. The latch must use these definitions; it must not test a
clock state, a break state, or whether an attempt is currently running.

## Current import surfaces

### Imports already covered by the import gate

The existing `ImportGate` checks authorization and the administrator's
"Imports are allowed" setting for:

| Surface | File type | Current mutation | Restart today |
| --- | --- | --- | --- |
| Registration upload | XLS/XLSX | Replaces or updates athletes and/or sessions, competition settings, and participations | Refused while any session is selected |
| SBDE upload | XLS/XLSX | Same broad replacement or update path as registration | Refused while any session is selected |
| Full database upload | JSON, or ZIP containing JSON | Replaces the complete competition database | Always calls `RestartUtils.triggerRestart()` after success |

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
| Age-group definitions upload | XLS/XLSX | Replaces every age group and recomputes all athlete participations | Changes categories on which active lifting order and results depend |
| Predefined age-group definitions load | XLSX resource | Same replacement as an uploaded age-group file | Same |
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

The global check must use:

`OwlcmsFactory.getFOPsWithSessionInProgress()`

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

### 7. Explicit restart policy

Every import is classified before it starts:

| Postcondition | Meaning |
| --- | --- |
| `LIVE` | Changes are safe in the current process and refresh all necessary live objects |
| `RESTART_REQUIRED` | The current process must exit after the import commits |

The following are `RESTART_REQUIRED`:

- full database JSON;
- registration imports other than the live-safe processor path;
- all SBDE imports;
- age-group-definition replacement;
- record-definition replacement, unless a complete live cache refresh is
  implemented and tested.

The restart must not depend only on the user closing a confirmation dialog.
After a successful `RESTART_REQUIRED` commit:

1. report that the import succeeded and a restart is taking place;
2. flush/audit the operation;
3. call `RestartUtils.triggerRestart(...)`.

If the OWLCMS process has a restart-capable supervisor, it restarts normally.
If it does not, the process must still exit instead of continuing with a stale
in-memory model. The operator starts it again. Continuing to serve after a
`RESTART_REQUIRED` import is never acceptable.

To support that rule, replace `triggerRestartIfNeeded()` for committed
destructive imports with an unconditional restart/exit path. Availability of
the control panel remains relevant to wording, not to the correctness of the
post-import state.

The following remain `LIVE`:

- the restricted officials-and-description registration update when no
  session is active;
- coaches;
- technical-official rows;
- timetable-row import without assignment generation;
- offline results for sessions that are not active.

## User interface

- Show a translated "session in progress" notice in each affected dialog.
- Disable the import confirmation for globally blocked imports while a session
  is active.
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
- `LIVE` versus `RESTART_REQUIRED` postcondition;
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
- Make the final restart unconditional for `RESTART_REQUIRED` imports.

### Phase 3: protect metadata and results imports

- Guard age-group replacement, predefined age-group loading, and record
  import.
- Guard offline results by selected session ID.
- Add warning-only handling for coach and technical-official imports.
- Split timetable-row import from assignment generation and guard both
  assignment-changing actions.

### Phase 4: UI and verification

- Add shared translated warning/blocked text to the affected dialogs.
- Disable or confirm controls according to the import classification.
- Verify all dialogs still refuse a stale open request after a session starts.
- Verify no destructive import continues after a latch refusal.
- Verify every committed `RESTART_REQUIRED` import exits, even without a
  control panel.

## Acceptance criteria

1. No registration upload, SBDE upload, or JSON import can mutate data while
   any session is selected on a platform.
2. An upload dialog opened before session selection cannot bypass the latch.
3. Offline session results cannot modify an active session, but can modify an
   unrelated inactive session.
4. Coach and technical-official imports show a warning; timetable generation
   is blocked while a session is active.
5. Officials-and-description-only spreadsheet imports are blocked during
   active sessions and remain usable when no session is active.
6. Every committed import classified `RESTART_REQUIRED` causes the OWLCMS
   process to exit or restart; it never silently continues.
7. Refused imports are audited and do not produce success notifications.
