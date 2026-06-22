# Birth Date Hot Fix Implementation Plan

## Goal

Add a hidden `/admin` action that repairs known-bad athlete birth dates by adding one day to every non-null `Athlete.fullBirthDate` value.

This is an emergency, operator-triggered repair. It must not run automatically, and it must not touch any date field except `Athlete.fullBirthDate`.

## Preconditions

Before implementing or testing the action, verify these existing fixes are present:

- `LocalDateAttributeConverter.convertToDatabaseColumn(LocalDate)` uses `Date.valueOf(locDate)`.
- `LocalDateAttributeConverter.convertToEntityAttribute(Date)` uses `sqlDate.toLocalDate()`.
- `LocalDateAttributeConverter` does not use UTC, system timezone, instants, epoch millis, calendars, or H2 timezone workarounds.
- `UtcNormalizationMigration` normalizes only `LocalDateTime`, not `LocalDate`.
- `Athlete.fullBirthDate` is still a `LocalDate`.

Do not implement a native SQL date repair unless there is a concrete blocker. Use JPA entity updates so the change stays scoped to the domain field and goes through the fixed converter.

## Target Files

Start with these files in the `owlcms_67` workspace:

- `owlcms/src/main/java/app/owlcms/nui/admin/AdminView.java`
- `owlcms/src/main/java/app/owlcms/data/athlete/Athlete.java`
- `owlcms/src/main/java/app/owlcms/data/jpa/JPAService.java`
- `owlcms/src/main/java/app/owlcms/data/jpa/LocalDateAttributeConverter.java`
- `owlcms/src/main/java/app/owlcms/data/jpa/UtcNormalizationMigration.java`

If the admin view becomes too large, add a small helper class near the admin package, for example:

- `owlcms/src/main/java/app/owlcms/nui/admin/BirthDateRepairDialog.java`

Keep the repair UI private to the hidden admin route. Do not add navigation menu entries.

## Implementation Steps

1. Verify the existing `/admin` route behavior.
   - Confirm `AdminView` is routed as `admin`.
   - Keep the current authentication and backdoor checks unchanged.
   - Add the new action to this view only.

2. Add a button labeled:

   ```text
   Repair Birth Dates
   ```

   Show this note next to the button:

   ```text
   Repair dates that were set in the past (previous day or previous year) during the registration process
   ```

   The button should open a confirmation dialog. It must not apply changes directly.

3. Build the preview data when the dialog opens.
   - Query all athletes where `fullBirthDate is not null`.
   - For each athlete, capture:
     - athlete id
       - lot number
     - last name
     - first name
     - current birth date
     - repaired birth date, computed as `current.plusDays(1)`
     - whether the current date is January 1
     - whether the current date is December 31
   - Sort preview rows in a stable, readable order. Prefer last name, first name, id unless there is an existing local convention.

4. Show the warning and summary prominently.
   - Include total athletes with birth dates.
   - Include Jan 1 count.
   - Include Dec 31 count.
   - Show Jan 1 and Dec 31 rows prominently.
   - Add a `Skip` checkbox on each Jan 1 / Dec 31 row so reviewed athletes can be excluded from the repair.
   - Do not show the full affected list.

5. Use this warning text, or a close equivalent:

   ```text
   This emergency action adds one day to every athlete birth date in the database.

   Use it only for databases where athlete birth dates were mass-imported from a registration sheet before the birth-date storage fix and all imported DOB values are known to be one day too early.

   This action cannot distinguish corrupted dates from correct dates. Review Jan 1 and Dec 31 athletes carefully before applying. Some Jan 1 or Dec 31 values may have been manually corrected already and may need separate handling.

   Export or back up the database before continuing.
   ```

6. Use the standard codebase confirmation dialog for the final dangerous action.
   - Do not require typed confirmation.
   - Use the existing `app.owlcms.components.ConfirmationDialog` convention: cancel is the default, and the dangerous action is red.
   - The preview dialog should still have a cancel button.

7. Apply the repair in one transaction.
   - Re-query the athletes inside the transaction instead of trusting stale preview objects.
   - Update only `Athlete.fullBirthDate`.
   - Do not update athletes whose ids were selected with `Skip`.
   - Do not update competition dates, record dates, weigh-in times, start times, lift times, or any other field.

   Core logic:

   ```java
   List<Athlete> athletes = em.createQuery(
       "select a from Athlete a where a.fullBirthDate is not null",
       Athlete.class)
       .getResultList();

   for (Athlete athlete : athletes) {
       athlete.setFullBirthDate(athlete.getFullBirthDate().plusDays(1));
   }
   ```

8. Report the result.
   - After success, show the number of updated athletes.
   - Disable the button for the current UI session, or otherwise make repeat application difficult.
   - Do not store a permanent database flag unless there is an existing admin-action pattern for this.

9. Log the operation at warning level.
   - Include updated athlete count.
   - Include Jan 1 count before repair.
   - Include Dec 31 count before repair.
   - Include the current client IP if available through existing access utilities.

   Suggested message:

   ```text
   Emergency birth-date repair applied: added one day to N Athlete.fullBirthDate values; Jan 1 before repair: X; Dec 31 before repair: Y; clientIp=...
   ```

## Non-Goals

Do not:

- Run the repair automatically.
- Infer corruption from date patterns alone.
- Repair all `LocalDate` fields.
- Repair `LocalDateTime` fields.
- Touch competition dates.
- Touch record event dates.
- Touch weigh-in, start, or lift times.
- Try to distinguish genuine Dec 31 dates from corrupted Jan 1 dates without operator review.
- Add fallback UI messages for missing translation keys.

## Validation Plan

1. Use the Problems panel or `get_errors` on changed Java files after editing.
2. Verify the converter preconditions again after the implementation.
3. If practical, add or run a narrow test for the repair helper logic without creating Vaadin UI objects in the test.
4. Do not run Maven builds or tests in `owlcms_67` without explicit human consent.
5. Manually inspect the final diff to confirm only the intended admin/hotfix files changed.

## Acceptance Criteria

- `/admin` shows the repair action only under the existing admin/backdoor access rules.
- Clicking the repair button opens a warning and preview; it does not immediately update data.
- The preview reports total affected athletes plus Jan 1 and Dec 31 counts.
- The Jan 1 / Dec 31 preview includes ID, lot number, name, current DOB, repaired DOB, and a skip checkbox.
- The final apply action uses the standard confirmation dialog instead of typed confirmation.
- Applying the action updates every non-skipped non-null `Athlete.fullBirthDate` by exactly one day.
- No other date or time field is modified by the repair code.
- The action logs a warning with counts.
- The action is difficult to run twice accidentally in the same UI session.
- Java diagnostics for changed files are clean, or any pre-existing unrelated diagnostics are documented.
