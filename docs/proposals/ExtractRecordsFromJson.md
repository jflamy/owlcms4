# Extract Records From JSON Exports

## Purpose

Allow a Record Repository installation to identify records broken in a completed
competition JSON export, even when that export contains no record definitions or
record results.

Conceptually, this is equivalent to working in the target competition's OWLCMS
instance: remove the records from the imported database, load the reference
records, then run **Extract New Records**. This feature performs that workflow
without manually altering the imported competition database.

It reconstructs each session's lifting order using OWLCMS rules and creates
provisional records against the reference record definitions.

## Open Issue: Record Eligibility

The repository may contain record definitions for ten federations while the
imported competition database applies to only one or two federations. The
extractor must not silently restrict candidate evaluation to the federations
present in the imported competition configuration.

Define how each imported athlete's federation is matched against every active
repository record definition, including whether a repository record definition
can apply when its federation is not configured by the imported competition.
This rule must be resolved before implementation so that the staging import
does not omit otherwise eligible record candidates.

## Open Issue: Candidate Cleanup

During provisional review, an administrator may determine that extracted
candidates are ineligible and must be deleted. Define a direct list-level
deletion workflow, including multi-select or bulk deletion, so these candidates
can be removed without opening each record's editor dialog.

## Availability

- Available only when the `recordRepository` feature toggle is enabled.
- The action is exposed from Records management as **Extract Records From JSON**.
- The normal JSON restore action remains unchanged and must not be used for this
  workflow.

## Accepted Inputs

The extractor accepts both OWLCMS competition JSON formats:

- Legacy V1 exports, identified by `groups` and legacy athlete objects.
- V2 exports, identified by `formatVersion` or `sessions` and athlete DTOs.

Format detection may reuse the existing version-detection rules, but must not
call the existing destructive JSON import routine.

## Staged Competition Import

The selected export becomes the temporary competition data in the Record
Repository database. This gives the extractor the complete source model needed
for OWLCMS lifting-order reconstruction.

The staging import replaces source competition data only:

- Competition, age groups, categories, sessions/groups, platforms, athletes,
  participations, and related source competition data are loaded.
- The source competition becomes the current competition while extraction runs.
- Source `records`, `recordConfig`, and `config` are ignored.

The staging import must preserve repository data:

- Existing `RecordEvent` rows remain unchanged before extraction.
- Existing `RecordConfig` remains unchanged.
- The repository `Config`, including `recordRepository`, remains unchanged.
- Existing provisional records are not cleared.

The existing full JSON restore routines cannot be used because their cleanup
deletes records and configuration.

## Record Baseline

The repository's active records are the only baseline for determining whether a
lift breaks a record.

This is required for the pathological but valid case where the source
competition export contains no records at all.

During an extraction run, newly identified records must also update the
in-memory baseline. A later lift can therefore break the record set earlier in
the same source competition only when it exceeds that new value.

## Lifting Order

The extractor must not order lifts by their recorded timestamps.

For each imported session, it must use the same rules-based reconstruction as
OWLCMS, represented by `LiftOrderReconstruction` and `LiftOrderInfo`:

- Snatches precede clean and jerks.
- Attempts are ordered by weight, attempt number, progression, cumulative
  progression, start number, and lot number.
- Failed lifts remain in the reconstruction where relevant, but only successful
  lifts can create records.

Each reconstructed lift is replayed in sequence against the current in-memory
record baseline.

## Record Eligibility And Values

For every replayed lift, evaluate the repository's active record definitions
using the imported athlete's:

- gender
- age under the source competition's normal OWLCMS rules
- weighed body weight
- record-eligibility federation codes

Successful snatches may improve Snatch records. Successful clean and jerks may
improve Clean and Jerk and Total records. The values follow the normal OWLCMS
best-snatch, best-clean-and-jerk, and total rules at that point in reconstructed
order.

## Extracted Record Fields

Each identified record is saved as a new `RecordEvent` with:

- the matching repository record definition fields
- athlete identity, birth data, gender, body weight, and nation from the source
  athlete
- `event` from the source competition name
- `eventLocation` from the source competition city
- `groupNameString` from the source session/group name

`groupNameString` must be nonblank, so every extracted record is provisional
regardless of the source export's contents.

## Date Policy

The record date comes from the recorded successful-lift timestamp:

- Do not use the scheduled session time.
- Do not infer a date from the source competition date.

Set `recordDate` to the date portion of the lift timestamp and set `recordYear`
from that date. The timestamp must not be used to reconstruct lifting order.

If a successful lift has no recorded timestamp, leave `recordDate` unset and do
not synthesize a `recordYear`. A records administrator can add or correct this
information during provisional review.

## Persistence And Review

After reconstruction, present the extracted candidates for review before
persisting them. Persist confirmed candidates through the repository's normal
save path so its duplicate handling remains effective.

The resulting entries appear as provisional records in Records management. The
existing acceptance workflow promotes selected provisional records to official
records; no extracted record becomes official automatically.

## Safety Requirements

- Never call the standard JSON restore path from this feature.
- Never delete imported or existing `RecordEvent` rows as part of extraction.
- Never overwrite repository configuration with source configuration.
- Never use lift timestamps as a substitute for lifting-order reconstruction.
- Report parsing, staging, reconstruction, and persistence failures without
  partially replacing repository records.