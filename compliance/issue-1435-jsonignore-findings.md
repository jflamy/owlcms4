# Issue 1435: JsonIgnore and field-access persistence

Generated from JavaParser analysis of field-access JPA types. The complete declaration-level inventory is in `issue-1435-jsonignore-persistence-inventory.tsv`.

## What JsonIgnore does

`@JsonIgnore` controls Jackson JSON serialization. It has no effect on JPA persistence.

With field access, JPA inspects fields and ignores bean getters/setters. A getter such as `getAnnouncerAsTO()` does not create a database property when there is no `announcerAsTO` field.

## Computed object properties

There are 55 formerly `@Transient`, `@JsonIgnore` object-valued properties with no matching field. They are computed JavaBean properties and need no field-level JPA annotation.

This includes all 25 `Group.get...AsTO()` methods. Each resolves a `TechnicalOfficial` on demand from a persisted official-name `String`; no `TechnicalOfficial` object is stored by these methods.

Other examples include computed collections and views such as:

- `Athlete.getEligibleCategories()`
- `Athlete.getCleanParticipations()`
- `Athlete.getRunningLiftOrderInfo()`
- `Competition.getGlobalRanking()`
- `Group.getAthletes()` and `Group.getRecords()`
- `Platform.getSessions()`
- `RecordConfig.getLoadedFiles()`

## Former method Transient annotations with matching fields

Eleven unique fields correspond to formerly `@Transient`, `@JsonIgnore` accessors and are currently persisted because JPA uses field access.

### Explicit or apparently intentional persistence

- `Athlete.gamxRank`, `gamxMRank`, `gamxURank`, `gamxARank`: explicit rank columns.
- `Athlete.smhfRank`: explicit rank column.
- `Participation.categoryScoreRank`: explicit rank column.
- `Competition.useRegistrationCategory`: explicit deprecated compatibility column.
- `Config.clearZip`: explicit column used to carry the clear request through the repository save/merge flow, then reset.
- `Athlete.ageAdjustedTotalRank`: implicit column, but copied and exported alongside the other persisted ranking values.

Moving `@Transient` to these fields would change the database schema/behavior and is not merely annotation cleanup.

### Restored field-level Transient

- `Config.traceMemory`: initialized lazily from the startup parameter, has no setter, and has no use outside `Config`. Its ineffective method annotation was moved to the field.

### Candidate requiring a later decision

- `Athlete.presumedBodyWeight`: runtime fallback derived from category/body weight, but it is also explicitly included in the V2 athlete DTO import/export. Decide whether DB persistence and export persistence should intentionally differ before annotating the field.

## Other JsonIgnore persistence

Nine fields directly carry `@JsonIgnore` while remaining persisted. They are IDs, scalar rank/configuration fields, or an explicitly enumerated role; none is an accidental arbitrary Java-object mapping.

Five ignored accessors match object-typed persisted fields, but all five are explicit mappings: `Category.participations` is `@OneToMany`, and the `TechnicalOfficial`/timetable role fields are `@Enumerated` values.
