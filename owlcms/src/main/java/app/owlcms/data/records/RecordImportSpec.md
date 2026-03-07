# Record Import Behavior Specification

## Purpose

This document specifies the intended behavior for importing official record definition files and for displaying provisional records in the edit/export grid.

This is a behavior specification only. It does not itself change runtime behavior.

## Problem Statement

Current official record import replacement is based on uploaded file basename.

This is unsafe in practice because:

1. Re-importing the same official record set under a different filename does not replace the prior official records.
2. Old and new official records can coexist for the same logical record identity.
3. Current-record views then keep the highest value for a key.
4. A downward correction can therefore be hidden by an older, higher official value.

## Definitions

### Official Record

An official record is a record where `groupNameString` is null or empty.

### Provisional Record

A provisional record is a record where `groupNameString` is non-empty.

### Official Logical Key

The logical identity of an official record is defined by:

1. `recordFederation`
2. `recordName`
3. `gender`
4. `recordLift`
5. `ageGrpLower`
6. `ageGrpUpper`
7. `bwCatLower`
8. `bwCatUpper`

`recordValue` is not part of the logical key.

## Approved Official Import Behavior

### Replacement Rule

Official imports must replace existing official records by logical key, not by uploaded file basename.

For each imported official record row:

1. Delete all existing official records matching the same official logical key.
2. Insert the imported official record row.

### Consequences

1. Upward corrections replace prior official values.
2. Downward corrections also replace prior official values.
3. Uploaded filename is treated as provenance metadata only.
4. Uploaded filename must not determine replacement scope.

### Non-Goal

This behavior does not automatically clean up provisional records.

## Approved Provisional Behavior

### Preservation Rule

Official import must not modify provisional records.

Provisional overlaps remain visible for manual cleanup.

### Accepted Round-Trip Exception

An imported official record may replace a matching provisional record created locally.

This applies to the round-trip case where:

1. A local provisional record is exported.
2. The reference site imports and accepts it as official.
3. The local site later reloads records from the reference site.

In that case:

1. The imported official row wins.
2. The otherwise matching local provisional row must be removed.
3. The imported official row is then inserted.
4. The result is a single official row rather than one official row plus one stale provisional row.

This exception is narrow.

It applies only when the imported official matches the same record details, not merely the same logical key.

### Duplicate Cleanup Rule

True duplicate provisional rows may be removed or prevented during import/recompute.

This cleanup applies only to exact duplicates, not to overlapping provisional records with distinct values or provenance.

### True Duplicate Provisional Definition

A provisional row may be considered a true duplicate if all relevant identifying fields match, including:

1. Official logical key fields
2. `recordValue`
3. `athleteName`
4. `recordDate`
5. `event`
6. `eventLocation`
7. `groupNameString`

This definition is intentionally conservative.

### Acceptance Workflow

Accepting provisional records may be done in bulk.

This is the normal administrative workflow.

Typical usage is:

1. Import exported competition records into the reference database.
2. Review the provisional rows.
3. Bulk-accept the set that should become official.
4. Delete obvious void or duplicate rows.

Later external facts such as doping disqualifications may require explicit manual intervention months later.

This later intervention is a separate correction workflow and must not force routine acceptance to be one-record-at-a-time.

### Acceptance Semantics

Bulk acceptance is additive history promotion.

For each accepted provisional row:

1. The row becomes an official historical row.
2. Existing official rows for the same official logical key remain in history.
3. The current view continues to select the highest official value for that logical key.
4. Other provisional overlaps may remain for later manual cleanup unless explicitly deleted.

Example:

1. A record is improved three times during one meet.
2. This produces three provisional rows.
3. When accepted, all three become official historical improvements.
4. All three remain visible in history.

Routine acceptance therefore does not replace prior official history.

### Correction Workflow

Correction workflows are separate from routine acceptance.

Examples include:

1. Later disqualifications.
2. Administrative voiding of an accepted result.
3. Canonical reference corrections where an older current value must stop being authoritative.

These workflows may require explicit replacement, voiding, or deletion logic.

They must not redefine the normal meaning of accepting provisional records.

## Concurrent Competition Scenario

A valid workflow may involve two competitions occurring on the same dates, with athletes at both competitions improving the same logical record.

In this situation:

1. The federation staff may import both competition exports into the same reference database.
2. Unchanged records from the shared reference baseline should behave idempotently across repeated imports.
3. Distinct provisional improvements from the two competitions must remain distinct when their provenance differs.
4. Differences in `event`, `eventLocation`, `recordDate`, athlete identity, or other provenance fields are sufficient reason to keep rows separate in provisional review.

### Review Outcome

After both feeds are imported:

1. The database may contain multiple provisional candidates for the same logical record key.
2. Federation staff may accept some and reject others based on an external criterion.
3. One such criterion may be the actual chronological order of performances.
4. The resulting accepted set defines the proper official picture.

### Duplicate Rule In This Scenario

Rows from different competitions are not duplicates merely because they share the same official logical key.

They are duplicates only if they are true duplicates under the conservative duplicate definition.

Therefore two same-day competitions improving the same record should normally produce multiple provisional candidates for review, not one collapsed row.

## Grid And Export Behavior

The edit grid and export must continue to use the same filtered source data.

## Operator Actions

The records maintenance UI exposes two distinct actions that must remain conceptually separate and complementary.

### Accept Provisional Records

This action applies to provisional rows.

Its purpose is to promote accepted provisional rows into official history.

Behavior:

1. It acts on provisional rows only.
2. It accepts selected or filtered provisional rows in bulk.
3. Accepted rows become official historical rows.
4. Existing official history remains intact.

### Keep Latest Official Record

This action applies to official rows only.

Its purpose is to prune official history so that only the current official row remains for each logical key.

Behavior:

1. It acts on official rows only.
2. It keeps the highest-valued official row for each logical key.
3. It removes older official historical rows for that logical key.
4. It does not modify provisional rows.

### Complementary Nature

These two actions must be complementary rather than overlapping.

1. Accepting provisional records promotes candidate results into official history.
2. Keeping the latest official record prunes official history only.
3. The second action must not accept, reject, hide, or otherwise modify provisional candidates.
4. The first action must not collapse official history.

### Status = PROVISIONAL

If the status filter is `PROVISIONAL`, current/history behavior must be forced to `HISTORY`.

### Reason

When viewing provisional records, users must be able to see all overlaps and duplicates that require manual cleanup.

### Expected UI Behavior

When `Status = PROVISIONAL`:

1. Automatically set `Current/History = HISTORY`.
2. Prevent current-record collapsing from hiding provisional overlaps.
3. Prefer disabling the `Current/History` selector while `PROVISIONAL` is selected.

## Current/History Semantics

### CURRENT

`CURRENT` keeps only the highest-valued record for each logical key.

This is appropriate for official record display, while preserving all accepted improvements in history.

### HISTORY

`HISTORY` shows all matching records without current-record collapsing.

This is required for provisional review and export when the status filter is provisional.

## Summary Of Approved Rules

1. Official import replaces official records by logical key.
2. Official import does not replace by filename.
3. Official import does not touch provisional records.
4. Except for the accepted round-trip case, where a matching official import absorbs the matching provisional.
5. Provisional review must expose overlaps and duplicates.
6. `Status = PROVISIONAL` forces `HISTORY` behavior.
7. Exact duplicate provisional rows may be cleaned up conservatively.
8. Accepting provisional records may be done in bulk.
9. Bulk acceptance is additive: accepted provisional rows become official historical rows.
10. Correction workflows are separate and may require explicit replacement or voiding behavior.
11. Competing provisional improvements from different competitions must remain separately reviewable unless they are true duplicates.

## Explicit Non-Goals

1. Do not automatically reconcile overlapping provisional records into one current record.
2. Do not hide provisional overlaps in the edit/export UI.
3. Do not use uploaded file basename as the authoritative replacement identity for official records.