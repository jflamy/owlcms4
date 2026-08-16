# Birth-Date Repair and Text Migration Plan for Versions 67 and 68

## Version Split

Version 67 implements only a new radical birth-year repair action. It does not change athlete persistence, JSON formats, or the existing `LocalDate fullBirthDate` model.

Version 68 implements the textual `isoBirthDate` storage migration and its associated compatibility work.

The version 67 repair is independent so operators can repair affected databases before upgrading them to version 68.

## Version 67 Scope

Version 67 must:

- Keep the existing **Repair Dates** button and apply its add-one-day correction to every selected athlete with a non-null birth date.
- Add a separate **Repair Years** button to the hidden admin page.
- Show all affected athletes in both repair dialogs and select all of them by default.
- Provide a select-all control in both dialogs so operators can restore the default selection after making changes.
- Keep `Athlete.fullBirthDate` persisted as a `LocalDate`.
- Leave V1 and V2 JSON import/export unchanged.
- Move every selected athlete with a birth date to January 1 of the following year when **Repair Years** is applied.

Version 67 must not:

- Add `isoBirthDate`.
- Delete or change the persisted `fullBirthDate` field.
- Run a birth-date storage migration.
- Change the add-one-day transformation performed by the existing **Repair Dates** action.

## Version 67 Repair Years Action

### Purpose

The new **Repair Years** action repairs databases where all affected athlete birth years are known to be one year too low. Every repaired value becomes January 1 of the following year.

Examples:

```text
2003-12-02 -> 2004-01-01
2003-01-01 -> 2004-01-01
```

This action deliberately and permanently discards the original month and day.

### Admin page

Keep the existing **Repair Dates** button in `AdminView`. Add a separate **Repair Years** button beside it or in the same repair section.

The buttons must open separate dialogs and invoke separate repair logic:

- **Repair Dates** adds exactly one day to every selected athlete with a non-null birth date. It must not restrict updates to January 1, December 31, or any other date pattern.
- **Repair Years** adds one year and forces January 1.

Do not overload one button or infer which repair the operator intends.

### Shared selection behavior

Both dialogs must use the same selection semantics:

- Query and display every athlete with a non-null `fullBirthDate`.
- Include a selection checkbox for every athlete row.
- Select every row when the dialog opens.
- Provide an explicit **Select All** control.
- Permit individual rows to be deselected before confirmation.
- Update the selection summary whenever selection changes.
- Apply the repair only to IDs selected at final confirmation.
- Treat an empty selection as a no-op and disable the apply action or otherwise prevent an empty repair submission.

The full affected list is required in both dialogs. January 1 and December 31 may be visually highlighted in **Repair Dates**, but they must not be the only rows shown.

### Service and dialog structure

Keep the existing `BirthDateRepairService` and `BirthDateRepairDialog` for **Repair Dates**.

Add separate classes for the new behavior, for example:

```text
owlcms/src/main/java/app/owlcms/nui/admin/BirthYearRepairService.java
owlcms/src/main/java/app/owlcms/nui/admin/BirthYearRepairDialog.java
```

Separate classes are preferred because the transformations, previews, warnings, and audit messages are materially different.

### Repair algorithm

For every selected athlete with a non-null `fullBirthDate`:

1. Read the year from `fullBirthDate`.
2. Subtract one.
3. Create January 1 of that following year.
4. Store it through `setFullBirthDate()`.

Core transformation:

```java
LocalDate currentBirthDate = athlete.getFullBirthDate();
LocalDate repairedBirthDate = LocalDate.of(currentBirthDate.getYear() + 1, 1, 1);
athlete.setFullBirthDate(repairedBirthDate);
```

Do not add or subtract days. Do not modify any other athlete field, record date, competition date, or timestamp.

### Preview

Build preview data when the **Repair Years** dialog opens. Query all athletes whose `fullBirthDate` is not null.

Show:

- Athlete ID
- Lot number
- Last name
- First name
- Current full birth date
- Repaired January 1 date in the following year
- Selected checkbox, checked by default

Sort rows by last name, first name, and ID, matching the existing repair preview convention.

The January 1 and December 31 rows remain highlighted in the **Repair Dates** preview because crossing a year boundary deserves operator review. They are not an eligibility filter: all athletes with non-null birth dates are listed and selected by default. This boundary analysis is not needed for **Repair Years**.

### Warning and confirmation

The warning must state clearly that:

- The action subtracts one from every selected athlete's birth year.
- Every repaired value becomes January 1 of the following year.
- Existing month and day information is permanently discarded.
- The action cannot distinguish corrupt values from correct values.
- Applying it twice subtracts two years.
- It must be used only once on a database known to be affected.
- The operator must export or back up the database first.

Keep the existing dangerous-action confirmation pattern. Cancel remains the default, and the destructive action is visually marked as dangerous.

### Transaction

Apply the repair in one transaction:

1. Re-query all athletes with non-null `fullBirthDate` inside the transaction.
2. Do not trust stale preview objects.
3. Update only IDs selected by the operator.
4. Leave unselected athletes unchanged.
5. Apply the previous-year January 1 transformation.
6. Commit all updates together.

Return at least updated and unselected counts.

### Logging

Log the operation at warning level using the repository logging convention. Include:

- Updated count
- Unselected count
- Client IP
- A clear statement that the year was decremented and January 1 was stored

Disable only the **Repair Years** button after successful application for the current UI session. Preserve the current behavior of **Repair Dates**.

Do not add a permanent database flag unless a separate requirement calls for permanently preventing repeat execution.

### UI translations

Do not hard-code new visible UI strings.

Identify existing keys used by `AdminView` and the current repair dialog. Add new translation keys for the distinct **Repair Years** action, warning, confirmation, preview headings, and success message where required.

Do not edit `shared/src/main/resources/i18n/translation4.csv`. Create a focused TSV proposal under `shared/src/main/resources/i18n/` using its exact header and the repository translation workflow.

### Version 67 tests

Add focused non-Vaadin tests for the service or an extracted pure transformation helper. Do not instantiate dialogs, grids, views, or other Vaadin objects.

Cover:

- `2003-12-02` becomes `2004-01-01`.
- `2003-01-01` becomes `2004-01-01`.
- Null birth dates remain untouched.
- Unselected athletes remain unchanged.
- Only `fullBirthDate` changes.
- All rows are selected by default.
- Select All restores all rows after individual deselection.
- Updated and unselected counts are correct.
- The **Repair Dates** transformation remains exactly `currentBirthDate.plusDays(1)` while adopting the shared full-list selection behavior.
- Both repair services update only the supplied selected IDs.
- Both repair services treat an empty selected-ID set as a no-op.

## Version 68 Goal

Make a textual ISO birth date the only canonical and persisted athlete birth-date value.

The stored value is one of:

- `null`, when no birth date is known
- `YYYY`, when only the birth year is known
- `YYYY-MM-DD`, when a complete birth date is known

The textual value preserves whether month and day were supplied. Existing `LocalDate` values migrate exactly as complete dates. In particular, an existing January 1 value remains `YYYY-01-01`; the migration must not infer that it originally represented a year-only value.

## Version 68 Current State

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

## Version 68 Canonical Storage

### Persisted field

Replace the persisted `fullBirthDate` field with:

```java
private String isoBirthDate;
```

The field accepts only `YYYY`, `YYYY-MM-DD`, or `null`. Normalize empty or blank input to `null` if that matches existing null-handling conventions.

Use an explicit Jackson property name because JavaBeans handling of the capitalized acronym in `getISOBirthDate()` may not consistently produce the intended name:

```java
@JsonProperty("isoBirthDate")
public String getISOBirthDate()
```

### Remove the old Java field

Delete the Java `fullBirthDate` field and its `@Convert` annotation. Do not retain duplicate persisted date and text fields.

`Athlete` uses JPA field access because `@Id` is placed on a field. Deleting the field removes it from the JPA model. The compatibility getters do not become persisted properties.

Hibernate schema update normally adds the new `isoBirthDate` column but does not reliably drop the obsolete physical `fullBirthDate` column. The unused old column may remain in upgraded databases. New databases contain only the new column. Physical removal is optional and requires a separate explicit schema migration.

## Version 68 Compatibility Accessors

All existing birth-date APIs derive from `isoBirthDate`.

### Canonical accessors

```java
String getISOBirthDate()
void setISOBirthDate(String value)
```

`getISOBirthDate()` returns the canonical stored text unchanged. `setISOBirthDate()` validates and normalizes before storing.

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

## Version 68 Startup Database Migration

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
   - Store `legacyDate.toLocalDate().toString()` through `setISOBirthDate()`.
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

## Version 68 JSON Export Compatibility

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

## Version 68 Order-Independent V1 Import

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
void importISOBirthDate(String value) {
    isoBirthDateImported = true;
    setISOBirthDate(value);
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

## Version 68 V2 DTO

Retain `LocalDate fullBirthDate` in `AthleteDTO` and add `String isoBirthDate`.

During export:

```java
dto.setISOBirthDate(athlete.getISOBirthDate());
dto.setFullBirthDate(athlete.getFullBirthDate());
```

During conversion back to an entity, ISO takes precedence after all DTO fields have been populated:

```java
if (isoBirthDate != null) {
    athlete.setISOBirthDate(isoBirthDate);
} else {
    athlete.setFullBirthDate(fullBirthDate);
}
```

Old V2 exports containing only `fullBirthDate` continue to import. New V2 exports preserve year-only precision.

## Version 68 Internal Precision

Any operation copying an athlete's canonical birth-date value must copy `isoBirthDate`, not computed `LocalDate`.

Review at least:

- `Athlete.conditionalCopy`
- `AthleteDTO.fromAthlete`
- `AthleteDTO.toAthlete`
- `XAthlete` delegation
- `PAthlete` delegation
- Other copies found by searching for `setFullBirthDate(...getFullBirthDate())`

Copying through the computed full date would silently change stored `2003` into `2003-01-01`.

Code needing `LocalDate` for age calculations, sorting, records, or display may continue using `getFullBirthDate()`. Code storing or copying precision must use `getISOBirthDate()`.

## Version 68 Tests

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

After either implementation phase:

1. Check workspace Java diagnostics for every edited Java file.
2. Verify that `translation4.csv` was not modified.
3. Add focused non-Vaadin tests.
4. Run Java tests only after explicit human consent.
5. Do not run Maven or a full build without explicit human consent.
6. Inspect the final diff for unrelated persistence, JSON, or repair changes.

## Version 67 Acceptance Criteria

- Both repair dialogs show every athlete with a non-null birth date.
- Both dialogs select every athlete by default and provide Select All.
- Operators can deselect individual athletes, and only selected IDs are updated.
- **Repair Dates** adds one day to every selected athlete, not only January 1 or December 31 values.
- Its January 1 and December 31 display is boundary highlighting within the full athlete list.
- A separate **Repair Years** button is present on the hidden admin page.
- The year repair maps every selected date to January 1 of the following year.
- The preview shows current and repaired full dates and supports deselecting athletes.
- The warning clearly describes year subtraction, January 1 replacement, precision loss, and repeat-run risk.
- The repair runs in one transaction and updates only `Athlete.fullBirthDate`.
- Each operation logs updated and unselected counts and client IP.
- Version 67 does not add `isoBirthDate`, change persistence, or change JSON formats.

## Version 68 Acceptance Criteria

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