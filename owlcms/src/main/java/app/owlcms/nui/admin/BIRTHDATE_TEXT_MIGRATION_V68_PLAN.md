# Birth-Date Text Migration Plan for Version 68

## Goal

Make a textual ISO birth date the only canonical and persisted athlete birth-date value.

The stored value is one of:

- `null`, when no birth date is known
- `YYYY`, when only the birth year is known
- `YYYY-MM-DD`, when a complete birth date is known

The textual value preserves whether month and day were supplied. Existing `LocalDate` values migrate exactly as complete dates. In particular, an existing January 1 value remains `YYYY-01-01`; the migration must not infer that it originally represented a year-only value.

## Current State

Before version 68, `Athlete` persists:

```java
@Convert(converter = LocalDateAttributeConverter.class)
private LocalDate fullBirthDate;
```

Year-only input is represented as January 1. Existing stored January 1 dates are therefore ambiguous and must all be preserved as complete dates during migration.

There are two competition JSON formats:

- V1 serializes and deserializes `Athlete` entities directly through `CompetitionData`.
- V2 serializes and deserializes `AthleteDTO` objects through `CompetitionDataV2`.

Both formats must remain compatible with older OWLCMS versions.

## Canonical Storage

### Persisted field

Replace the persisted `fullBirthDate` field with:

```java
private String isoBirthDate;
```

The field accepts only `YYYY`, `YYYY-MM-DD`, or `null`. Normalize empty or blank input to `null` if that matches existing null-handling conventions.

Treat `ISO` as the word `Iso` in Java identifiers, following normal camel-case conventions. This also gives JavaBeans the desired `isoBirthDate` property name without acronym-specific decapitalization behavior. Keep the JSON name explicit so it remains stable across mapper configurations:

```java
@JsonGetter("isoBirthDate")
public String getIsoBirthDate()
```

Do NOT use `getISOBirthDate()`: standard JavaBeans introspection derives the property name `ISOBirthDate` when the first two characters after `get` are uppercase. Jackson's legacy non-standard naming can derive `isobirthDate` instead. Neither is the intended Java property name.

### Remove the old Java field

Delete the Java `fullBirthDate` field and its `@Convert` annotation. Do not retain duplicate persisted date and text fields.

`Athlete` uses JPA field access because `@Id` is placed on a field. Deleting the field removes it from the JPA model. The compatibility getters do not become persisted properties.

Hibernate schema update normally adds the new `isoBirthDate` column but does not reliably drop the obsolete physical `fullBirthDate` column. The unused old column may remain in upgraded databases. New databases contain only the new column. Physical removal is optional and requires a separate explicit schema migration.

## Compatibility Accessors

All existing birth-date APIs derive from `isoBirthDate`.

### Canonical accessors

```java
String getIsoBirthDate()
void setIsoBirthDate(String value)
```

`getIsoBirthDate()` returns the canonical stored text unchanged. `setIsoBirthDate()` validates and normalizes before storing.

### Full-date compatibility

Keep:

```java
@Transient
LocalDate getFullBirthDate()

@Transient
void setFullBirthDate(LocalDate value)
```

Behavior:

- Stored `2003-12-02` returns `LocalDate.of(2003, 12, 2)`.
- Stored `2003` returns `LocalDate.of(2003, 1, 1)`.
- Stored `null` returns `null`.
- Setting a `LocalDate` stores its complete ISO representation, including January 1.
- Setting `null` clears `isoBirthDate`.

`@Transient` documents that these are computed compatibility methods. With field-based JPA access, deleting the field is the decisive persistence change.

### Year compatibility

Keep:

```java
Integer getYearOfBirth()
void setYearOfBirth(Integer value)
```

The getter extracts the year from the canonical text. The setter stores only `YYYY`, deliberately recording year-only precision.

Keep deprecated `getBirthDate()` and `setBirthDate(Integer)` delegating to the year methods. Their return and argument types must not change.

## Startup Database Migration

### Why it is required

When version 68 starts an existing database:

1. Hibernate schema update sees the new mapped `isoBirthDate` field and creates its column.
2. The old physical `fullBirthDate` column still contains existing values.
3. The old column is no longer mapped because the Java field was deleted.
4. A startup migration must copy legacy values before normal athlete use begins.

### Migration class

Add:

```text
owlcms/src/main/java/app/owlcms/data/jpa/BirthDateTextMigration.java
```

Provide an entry point such as:

```java
public static void migrate(EntityManager em)
```

### Algorithm

1. Use JDBC `DatabaseMetaData` to determine whether the legacy `Athlete.fullBirthDate` column exists. Compare names case-insensitively for H2 and PostgreSQL.
2. Return immediately if the old column does not exist. This is expected for new or reset databases.
3. Select only rows requiring conversion:

   ```sql
   SELECT id, fullBirthDate
   FROM Athlete
   WHERE isoBirthDate IS NULL
     AND fullBirthDate IS NOT NULL
   ```

4. For each result:
   - Read the athlete ID.
   - Convert the JDBC date to `LocalDate`.
   - Load the mapped `Athlete` with `em.find`.
  - Store `legacyDate.toLocalDate().toString()` through `setIsoBirthDate()`.
5. Log the migrated count.

Use Java date conversion instead of database `CAST`, avoiding H2 and PostgreSQL formatting differences.

The migration is idempotent because it only fills null `isoBirthDate` values. It never overwrites an existing textual value, so no separate completion flag is required.

Iterating over approximately 1,500 athletes is acceptable because this occurs once during database upgrade, not after JSON import. Later startups normally select zero rows.

### Startup location and ordering

Invoke the migration from `Main.initConfig()` after JPA and config initialization and before normal athlete data is loaded or used:

```java
JPAService.init(...);
Config.initConfig();

JPAService.runInTransaction(em -> {
    BirthDateTextMigration.migrate(em);
    UtcNormalizationMigration.normalizeAllToUtc(em);
    return null;
});
```

The sequence matters:

1. `JPAService.init(...)` runs Hibernate schema update and creates `isoBirthDate`.
2. `Config.initConfig()` establishes startup configuration.
3. `BirthDateTextMigration` copies legacy dates.
4. Other migrations and normal startup continue.

The UTC migration must continue ignoring civil `LocalDate` values. It does not perform this textual conversion.

## JSON Export Compatibility

Both V1 and V2 exports retain the existing computed `fullBirthDate` property and add `isoBirthDate`.

For a stored complete date:

```json
{
  "fullBirthDate": [2003, 12, 2],
  "isoBirthDate": "2003-12-02"
}
```

For a stored year only:

```json
{
  "fullBirthDate": [2003, 1, 1],
  "isoBirthDate": "2003"
}
```

The exact representation of `fullBirthDate` remains whatever the existing mapper produces. It is computed by `getFullBirthDate()`.

`isoBirthDate` is always exported exactly as stored. The competition display setting must not modify persisted or exported values.

Older OWLCMS versions ignore unknown `isoBirthDate` and continue importing `fullBirthDate`. New versions preserve year-only precision through `isoBirthDate`.

## Order-Independent V1 Import

V1 has no DTO and Jackson deserializes directly into `Athlete`. Import precedence must not depend on JSON property order.

Add non-persisted import state:

```java
@Transient
@JsonIgnore
private boolean isoBirthDateImported;
```

Use dedicated Jackson setters. Keep ordinary programmatic setters separate where practical so import state cannot affect later application updates.

Required behavior:

- The ISO JSON setter always stores its value and marks ISO as imported.
- The legacy JSON setter stores its value only when ISO has not been imported.
- If legacy appears first, later ISO overwrites it.
- If ISO appears first, later legacy is ignored.
- Legacy-only input becomes a canonical complete date.
- ISO-only input remains canonical unchanged.

Conceptually:

```java
@JsonSetter("isoBirthDate")
void importIsoBirthDate(String value) {
    isoBirthDateImported = true;
    setIsoBirthDate(value);
}

@JsonSetter("fullBirthDate")
void importFullBirthDate(LocalDate value) {
    if (!isoBirthDateImported) {
        setFullBirthDate(value);
    }
}
```

This resolves precedence during Jackson's existing pass without a second athlete iteration or custom deserializer.

Confirm that Jackson does not select ordinary `setFullBirthDate` in addition to the dedicated JSON setter. Use explicit `@JsonGetter`, `@JsonSetter`, or `@JsonIgnore` annotations as needed to expose one unambiguous JSON property while retaining the public Java API.

## V2 DTO

Retain `LocalDate fullBirthDate` in `AthleteDTO` and add `String isoBirthDate`.

During export:

```java
dto.setIsoBirthDate(athlete.getIsoBirthDate());
dto.setFullBirthDate(athlete.getFullBirthDate());
```

During conversion back to an entity, ISO takes precedence after all DTO fields have been populated:

```java
if (isoBirthDate != null) {
    athlete.setIsoBirthDate(isoBirthDate);
} else {
    athlete.setFullBirthDate(fullBirthDate);
}
```

Old V2 exports containing only `fullBirthDate` continue to import. New V2 exports preserve year-only precision.

## Internal Precision

Any operation copying an athlete's canonical birth-date value must copy `isoBirthDate`, not computed `LocalDate`.

Review at least:

- `Athlete.conditionalCopy`
- `AthleteDTO.fromAthlete`
- `AthleteDTO.toAthlete`
- `XAthlete` delegation
- `PAthlete` delegation
- Other copies found by searching for `setFullBirthDate(...getFullBirthDate())`

Copying through the computed full date would silently change stored `2003` into `2003-01-01`.

Code needing `LocalDate` for age calculations, sorting, records, or display may continue using `getFullBirthDate()`. Code storing or copying precision must use `getIsoBirthDate()`.

## Tests

Add focused non-Vaadin tests.

### Canonical model

Cover:

- `YYYY`, `YYYY-MM-DD`, and null storage
- Rejection of malformed or impossible dates
- `getFullBirthDate()` mapping `YYYY` to January 1
- `setFullBirthDate()` preserving a real January 1 as `YYYY-01-01`
- Year and deprecated getter compatibility
- `setYearOfBirth()` producing `YYYY`

### Startup migration

Cover:

- Ordinary dates migrate as complete ISO dates.
- January 1 remains complete `YYYY-01-01`.
- Existing ISO values are not overwritten.
- A second run changes nothing.
- A new database without the old column is accepted.

### JSON

Cover V1 and V2:

- Complete-date and year-only round trips
- Legacy-only input
- ISO-only input
- Both fields in both orders
- Conflicting values where ISO always wins
- Export still includes the legacy computed field

### Internal copies

Verify that copying stored `2003` leaves the destination as `2003`, not `2003-01-01`.

## Validation

After implementation:

1. Check workspace Java diagnostics for every edited Java file.
2. Verify that `translation4.csv` was not modified.
3. Add focused non-Vaadin tests.
4. Run Java tests only after explicit human consent.
5. Do not run Maven or a full build without explicit human consent.
6. Inspect the final diff for unrelated persistence, JSON, or repair changes.

## Acceptance Criteria

- `isoBirthDate` is the only mapped athlete birth-date field.
- The Java `fullBirthDate` field no longer exists.
- Existing dates migrate exactly to `YYYY-MM-DD`, including January 1.
- Year-only values entered afterward remain stored as `YYYY`.
- Computed compatibility accessors continue serving existing Java callers.
- V1 and V2 retain legacy `fullBirthDate` import and export.
- New exports include `isoBirthDate` exactly as stored.
- ISO wins over legacy JSON regardless of property order.
- V1 precedence needs neither a second athlete pass nor custom deserializer.
- Older versions can import new exports through the legacy field.
- New versions can import old exports containing only the legacy field.
- Startup migration is idempotent and harmless on new databases.