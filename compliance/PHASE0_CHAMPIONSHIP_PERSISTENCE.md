# Phase 0: Championship Persistence — Detailed Implementation Plan

## Goal

Convert `Championship` from a transient, in-memory-only object into a JPA-persisted entity so that championship identity, metadata, and future mixed-team configuration survive application restart and are the single source of truth for championship definitions.

## Design Constraints (from parent plan)

- **Keep `AgeGroup.championshipName` as a String.** Do NOT change it to a foreign-key reference yet. The string acts as a natural-key lookup into the Championship table. This preserves every existing query and accessor unchanged.
- **Keep the denormalized age-group input model.** Championship information is still read from the age-group spreadsheet columns (column 1 = championshipName, column 2 = ageDivision/type). The stored Championship row is reconciled from that input, not the other way around.
- **Last age group read wins.** When two age groups disagree on type for the same championship name, the last one processed determines the stored championship's type. This preserves the current practical reconciliation rule.
- **Idempotent bootstrap.** Running startup or re-uploading age groups must not duplicate championship rows. Match on canonical name (case-insensitive).
- **Backward-compatible API surface.** Callers that resolve championships via `Championship.of(name)`, `Championship.findAll()`, `Championship.findAllUsed()`, etc. must continue to work without changes. The static API becomes a thin cache over the database, not a replacement of the call sites.

---

## Current State Analysis

### What Championship is today

`Championship.java` is a plain Java class (not a JPA entity) with:

| Field | Type | Purpose |
|-------|------|---------|
| `name` | `String` | Display name, used as map key (lowercased) |
| `type` | `ChampionshipType` | Enum: `MASTERS`, `U`, `IWF`, `DEFAULT` |

All instances live in a static `HashMap<String, Championship> allChampionshipsMap`. There is no database table.

### How the in-memory map is populated

1. **First access via `findAll()`**: Seeds two hard-coded entries (translated "DEFAULT" and "Masters"), then queries `AgeGroupRepository.allChampionshipsForAllAgeGroups()`, which scans every persisted `AgeGroup` row, extracts `computeChampionshipName()` + `ageDivision` as a `¤`-delimited string, deduplicates with a `TreeSet`, and returns the list. `findAll()` then splits each string and calls `addChampionship(name, type)`.

2. **After age-group file upload via `Championship.reset()`**: Called at the end of `AgeGroupDefinitionReader.loadAgeGroupStream()`. Sets the map to null, then calls `findAll()` to rebuild from current AgeGroup state.

3. **After competition data import via `Championship.reset()`**: Called in `CompetitionData.restoreFromJson(...)` after all entities are merged.

### Where championship identity is consumed (32 importing files)

Callers fall into three usage patterns:

| Pattern | Method(s) | Caller count | Phase 0 impact |
|---------|-----------|--------------|----------------|
| Lookup by name | `Championship.of(name)` | ~8 (AgeGroup.getChampionship, AgeGroup.getChampionshipType, ResultsParametersReader, PackageContent, TopTeams*, TopSinclair*) | None — `of()` will load from DB-backed cache |
| List all / list used | `Championship.findAll()`, `Championship.findAllUsed()` | ~12 (IFilterCascade, AgeGroupContent, RegistrationContent, TeamSelectionContent, TeamResultsContent, TopTeams*, TopSinclair*, DemoData) | None — return value shape unchanged |
| Mutate map | `Championship.addChampionship()`, `Championship.remove()`, `Championship.getMap()`, `Championship.reset()` | ~5 (EditChampionshipsDialog, AgeGroupEditingFormFactory, AgeGroupDefinitionReader, CompetitionData) | **Must change** — mutations now go to DB |
| DemoData type lookup | `Championship.ofType()` | 1 (DemoData) | None — can query DB-backed cache |

### Synchronization points

| Trigger | Current behavior | Phase 0 behavior |
|---------|-----------------|-------------------|
| Application startup (`Main.injectData`) | Championship map is lazily built on first `findAll()` call | Bootstrap stored championships from AgeGroup rows if Championship table is empty; then load cache from DB |
| Age-group file upload (`AgeGroupDefinitionReader.loadAgeGroupStream`) | `Championship.reset()` rebuilds in-memory map from AgeGroups | After age groups are created, reconcile Championship table rows, then refresh cache |
| Competition data import (`CompetitionData.restoreFromJson`) | `Championship.reset()` rebuilds in-memory map | After import merges AgeGroups, reconcile Championship table rows, then refresh cache |
| Admin adds/renames/deletes championship (`EditChampionshipsDialog`) | Mutates in-memory map only; lost on restart | Persist changes to Championship table; refresh cache |
| Age-group editor changes championship (`AgeGroupEditingFormFactory`) | Reads `Championship.getMap()` for ComboBox items | Reads from DB-backed `Championship.findAll()` instead |

---

## Target State

### Championship becomes a JPA entity

```
@Entity
@Cacheable
Championship {
    @Id @GeneratedValue
    Long id;

    @Column(unique = true, nullable = false)
    String name;            // canonical name, unique natural key

    @Enumerated(EnumType.STRING)
    ChampionshipType type;  // MASTERS, U, IWF, DEFAULT
}
```

The `name` column is the natural key.  Lookup is always by lowercased name. A unique constraint prevents duplicates. The `@Id` is a surrogate Long for JPA identity.

### AgeGroup.championshipName remains a String

`AgeGroup.championshipName` continues to store the championship name as a plain string. It is NOT converted to a `@ManyToOne` foreign key in this phase. This is the key decision that keeps all existing queries, accessors, and serialization unchanged.

The string relationship is resolved at runtime via `Championship.of(name)`, which now reads from a DB-backed cache instead of a purely in-memory map.

### Static API on Championship becomes a thin cache

The existing static methods (`findAll`, `findAllUsed`, `of`, `getMap`, `addChampionship`, `remove`, `reset`) are preserved but their implementation changes:

- **Read methods** (`findAll`, `findAllUsed`, `of`, `getMap`, `ofType`) → load from an in-memory cache that is populated from the Championship table.
- **Write methods** (`addChampionship`, `remove`, `setName`) → persist to the database AND update the in-memory cache.
- **`reset()`** → clear cache, reload from database.

This means callers see no API change.

---

## Implementation Steps

### Step 0.1 — Add JPA annotations to Championship

Convert `Championship.java` to a JPA entity:

**Changes to `Championship.java`:**
- Add `@Entity`, `@Cacheable` annotations
- Add `@Id @GeneratedValue(strategy = GenerationType.AUTO) Long id` field
- Add `@Column(unique = true, nullable = false) String name` (keep existing field, add annotation)
- Add `@Enumerated(EnumType.STRING)` to the existing `type` field
- Add `equals()` and `hashCode()` based on `name` (lowercased canonical form)
- Add `Serializable` implementation
- Keep the existing `Comparable<Championship>` implementation
- Keep the existing `Comparator<Championship> ct` 

**Do NOT change:**
- The static methods (`findAll`, `of`, `reset`, etc.) — those are updated in Step 0.3
- The two constructors (parameterized and no-arg) — add a no-arg constructor for JPA if one doesn't exist

### Step 0.2 — Register Championship as a JPA entity

**Changes to `JPAService.java` (`entityClassNames()`):**

Add `Championship.class.getName()` to the entity list:

```java
.add(Championship.class.getName())
```

Insert it near `AgeGroup.class.getName()` since they are in the same package and conceptually related.

**Effect:** Hibernate will create the `CHAMPIONSHIP` table automatically on next startup (DDL auto-update).

### Step 0.3 — Create ChampionshipRepository

Create `ChampionshipRepository.java` in `app.owlcms.data.agegroup` with the following methods:

```java
public class ChampionshipRepository {

    // Find by canonical name (case-insensitive)
    public static Championship findByName(String name);

    // Find all stored championships
    public static List<Championship> findAll();

    // Save or update a championship
    public static Championship save(Championship c);

    // Delete a championship by name
    public static void delete(Championship c);

    // Bootstrap: create stored championships from persisted age groups
    // if Championship table is empty.
    public static void bootstrapFromAgeGroups();

    // Reconcile: after age groups are reloaded, ensure stored championships
    // match the current age group state.
    public static void reconcileFromAgeGroups();
}
```

**`bootstrapFromAgeGroups()` algorithm:**

```
1. Count rows in Championship table.
2. If count > 0, return (already bootstrapped).
3. Read all persisted AgeGroups.
4. Group by computeChampionshipName() (case-insensitive).
5. For each group:
   a. Determine canonical name using Championship.canonicalizeChampionshipName().
   b. Determine type: iterate age groups in order, let last non-null
      championshipType win (preserves "last read wins" rule).
      If all null, resolve from ageDivision with fallback to U.
   c. Create Championship entity with (name, type).
   d. Persist.
6. Add the two hard-coded entries (DEFAULT, MASTERS) if not already 
   present after age-group scan. These are always present in the current
   code and must survive even if no age groups reference them.
```

**`reconcileFromAgeGroups()` algorithm:**

```
1. Read all persisted AgeGroups, group by computeChampionshipName().
2. For each group:
   a. Look up existing Championship row by canonical name.
   b. If not found → create and persist new Championship.
   c. If found → update type if age groups disagree (last wins).
3. Ensure DEFAULT and MASTERS entries still exist.
4. Optionally: remove Championship rows that no longer have any age group 
   referencing them, unless they were created manually by the user.
   (Decision: defer removal to avoid losing user-created championships 
   during re-upload. Mark orphan championships rather than deleting.)
```

### Step 0.4 — Rewrite Championship static methods

**`Championship.findAll()`** — New implementation:

```
1. If cache is null or empty:
   a. Load all Championship entities from DB via ChampionshipRepository.findAll().
   b. Populate allChampionshipsMap from the results.
   c. If DB is empty (first run, no age groups yet), seed DEFAULT and MASTERS
      entries into DB and cache, same as today.
2. Return sorted list from cache.
```

The lazy-initialization from AgeGroups is replaced by lazy-initialization from DB. The AgeGroup scan is no longer done here — it was moved to `bootstrapFromAgeGroups()`.

**`Championship.of(String name)`** — Minimal change:

```
1. If cache is null, call findAll() to populate.
2. Look up in cache by canonicalized, lowercased name.
3. If not found in cache, query DB as fallback (handles race where 
   championship was just created by another path).
4. If still not found, return null (current behavior returns null 
   for unknown names, except it returns a new Championship("", U) 
   for null input — preserve that).
```

**`Championship.reset()`** — New implementation:

```
1. Set allChampionshipsMap = null.
2. Call findAll(). (This reloads from DB, not from AgeGroups.)
```

Semantics preserved: cache is invalidated and rebuilt. The difference is the source is now the database.

**`Championship.addChampionship(String name, ChampionshipType type)`** — New implementation:

```
1. Canonicalize name.
2. Check cache for existing entry.
3. If not found:
   a. Create Championship entity.
   b. Persist via ChampionshipRepository.save().
   c. Add to cache.
4. Return the (existing or new) Championship.
```

**`Championship.remove(Championship c)`** — New implementation:

```
1. Delete from DB via ChampionshipRepository.delete().
2. Remove from cache.
```

**`Championship.getMap()`** — Unchanged (returns the cache map). Callers already treat it as read-only except `EditChampionshipsDialog`.

**`Championship.setName(String name)` (instance method)** — New implementation:

```
1. Remove old key from cache.
2. Update this.name.
3. Persist via ChampionshipRepository.save().
4. Add new key to cache.
```

### Step 0.5 — Update startup migration in Main.injectData()

In `Main.injectData()`, after the existing call to `AgeGroupRepository.updateExistingChampionships()` and before `AgeGroupRepository.validateCategoriesConsistency()`, add:

```java
// Phase 0: Bootstrap stored championships from persisted age groups
// if Championship table is empty (first run after upgrade).
ChampionshipRepository.bootstrapFromAgeGroups();
```

This is the migration hook. It runs once on first startup after the code is deployed, creates the Championship rows, and becomes a no-op on subsequent startups because the table is no longer empty.

### Step 0.6 — Update age-group reload path

In `AgeGroupDefinitionReader.loadAgeGroupStream()`, after `createAgeGroups(...)` and before the existing `Championship.reset()`:

```java
// Phase 0: Reconcile stored championships with newly loaded age groups.
ChampionshipRepository.reconcileFromAgeGroups();
Championship.reset(); // existing call — now reloads from DB
```

This ensures that when an admin uploads a new age-group spreadsheet containing a new championship name, a corresponding Championship row is created before the in-memory cache is rebuilt.

### Step 0.7 — Update competition data import path

In `CompetitionData.restoreFromJson()`, before the existing `Championship.reset()`:

```java
// Phase 0: Reconcile stored championships after import.
ChampionshipRepository.reconcileFromAgeGroups();
// Championship.reset() follows — already present
```

### Step 0.8 — Update EditChampionshipsDialog

The dialog currently mutates the in-memory map directly. After Phase 0, the static methods it calls (`addChampionship`, `remove`, `setName`) already persist to the database (Step 0.4). **No changes to the dialog's code are strictly necessary** because the mutations flow through the updated static methods.

However, verify that:
- The `Update` button calls `c.setName(nameField.getValue())`, which now persists.
- The `Delete` button calls `Championship.remove(c)`, which now deletes from DB.
- The `Add` button calls `Championship.addChampionship(...)`, which now persists.

**Optional improvement:** Add a confirmation dialog for delete, since the action is now permanent (persisted) rather than session-scoped.

### Step 0.9 — Update AgeGroupEditingFormFactory championship ComboBox

Currently at line 159:
```java
List<Championship> list = Championship.getMap().values().stream().sorted().toList();
```

This still works because `getMap()` returns the DB-backed cache. However, if the cache hasn't been populated yet, `getMap()` could return null. Change to:

```java
List<Championship> list = Championship.findAll();
```

This guarantees the cache is initialized.

### Step 0.10 — Update DemoData championship references

`DemoData` calls `Championship.ofType()` after calling `addChampionship()`. The `addChampionship()` call now persists to DB. The `ofType()` call reads from DB-backed cache. Verify this sequence works inside the existing `JPAService.runInTransaction()` that DemoData uses. If DemoData creates age groups and championships in the same transaction, the Championship save must happen before the transaction commits. This should work if `ChampionshipRepository.save()` uses `em.persist()` / `em.merge()` within the same EntityManager.

**Likely change:** DemoData may need to either:
- Use the same EntityManager for Championship persistence, OR
- Flush after Championship saves before referencing them

Review `DemoData.insertInitialData()` to confirm transaction boundaries.

---

## Files Changed

| File | Change type | Description |
|------|-------------|-------------|
| `Championship.java` | **Major rewrite** | Add JPA annotations, id field, equals/hashCode; rewrite static methods to use DB-backed cache |
| `JPAService.java` | **One line** | Add `Championship.class.getName()` to entity list |
| `ChampionshipRepository.java` | **New file** | Repository with findByName, findAll, save, delete, bootstrapFromAgeGroups, reconcileFromAgeGroups |
| `Main.java` | **Two lines** | Add `ChampionshipRepository.bootstrapFromAgeGroups()` call in `injectData()` |
| `AgeGroupDefinitionReader.java` | **One line** | Add `ChampionshipRepository.reconcileFromAgeGroups()` before `Championship.reset()` |
| `CompetitionData.java` | **One line** | Add `ChampionshipRepository.reconcileFromAgeGroups()` before `Championship.reset()` |
| `AgeGroupEditingFormFactory.java` | **One line** | Change `Championship.getMap().values()...` to `Championship.findAll()` |
| `DemoData.java` | **Review only** | Verify transaction boundaries for Championship persistence |

**Files NOT changed (verified safe):**
All 32 files that import Championship continue to work because the static API surface is preserved. No import changes, no method signature changes, no return type changes.

---

## Database Schema

### New table: CHAMPIONSHIP

| Column | Type | Constraints | Notes |
|--------|------|-------------|-------|
| `ID` | `BIGINT` | `PRIMARY KEY, AUTO_INCREMENT` | Surrogate key |
| `NAME` | `VARCHAR(255)` | `UNIQUE, NOT NULL` | Canonical championship name |
| `TYPE` | `VARCHAR(31)` | `NOT NULL` | `MASTERS`, `U`, `IWF`, or `DEFAULT` |

Hibernate DDL auto-update creates this table automatically from the `@Entity` annotations. No manual DDL migration script is needed because owlcms uses H2 embedded with `hibernate.hbm2ddl.auto` set to update.

### Existing table: AGEGROUP — unchanged

`AGEGROUP.CHAMPIONSHIP_NAME` remains a `VARCHAR` column. It is NOT converted to a foreign key. The relationship is a soft reference resolved by name lookup.

---

## Data Migration Flow (first startup after upgrade)

```
Application starts
  → Main.injectData()
    → (existing) CategoryRepository.fixCategories()
    → (existing) AthleteRepository.removeBrokenParticipationsAndCategories()
    → (existing) AgeGroupRepository.updateExistingChampionships()
        ↳ backfills championshipName from ageDivision where blank
    → (NEW) ChampionshipRepository.bootstrapFromAgeGroups()
        ↳ Championship table is empty → scan AgeGroups
        ↳ Group by computeChampionshipName()
        ↳ Create Championship row per group
        ↳ Ensure DEFAULT and MASTERS entries exist
    → (existing) AgeGroupRepository.validateCategoriesConsistency()
    → ...rest of startup
```

On second and subsequent startups, `bootstrapFromAgeGroups()` finds the Championship table non-empty and returns immediately.

---

## Reconciliation Flow (age-group re-upload)

```
Admin uploads new age-group Excel file
  → AgeGroupsFileUploadDialog
    → AgeGroupRepository.reloadDefinitions()
      → AgeGroupDefinitionReader.doInsertRobiAndAgeGroups()
        → loadAgeGroupStream()
          → createAgeGroups(workbook, ...)
              ↳ Deletes old AgeGroups, creates new ones
              ↳ Each new AgeGroup has championshipName from column 1
          → (NEW) ChampionshipRepository.reconcileFromAgeGroups()
              ↳ Scan new AgeGroups
              ↳ Create missing Championship rows
              ↳ Update type for existing rows if changed
          → Championship.reset()
              ↳ Clears cache, reloads from DB
```

---

## Edge Cases and Decisions

### 1. Orphan championships after age-group re-upload

If an admin re-uploads age groups and the new spreadsheet no longer references championship "FooChamp", should the stored Championship row be deleted?

**Decision:** Do NOT auto-delete orphan championships in Phase 0. An admin may have manually created a championship via EditChampionshipsDialog with the intent to assign age groups to it later. Deleting it on re-upload would be surprising. Orphan championships are harmless — they show up in the ComboBox but have no age groups.

Future phases may add a "cleanup unused championships" admin action.

### 2. Case sensitivity

Championship names are stored with their original case (`"Junior"`, `"Masters"`) but lookups use lowercased keys (`"junior"`, `"masters"`). The unique constraint in the database should be case-insensitive.

**Implementation:** Use `@Column(unique = true)` on `name`. For H2, add a functional index or use `LOWER(name)` in queries. Alternatively, always store the lowercased form in a separate indexed column and keep the display form in `name`.

Simpler approach: store names as-is, and enforce uniqueness in application code via `canonicalizeChampionshipName()` before persist. The unique DB constraint catches any races.

### 3. Hard-coded DEFAULT and MASTERS entries

Currently, `findAll()` always seeds DEFAULT and MASTERS. After Phase 0:
- `bootstrapFromAgeGroups()` ensures they exist on first run.
- `reconcileFromAgeGroups()` ensures they exist after re-upload.
- `findAll()` no longer seeds them — it just reads from DB. If they were deleted via the admin dialog, they stay deleted until the next reconciliation.

**Decision:** This matches current behavior closely enough. If the admin explicitly deletes "Masters", it stays deleted until they re-upload age groups.

### 4. Translation of DEFAULT championship name

Currently, the DEFAULT championship is created with `Translator.translate("Division.DEFAULT")`, which means its name varies by locale. This is fragile for persistence — the stored name depends on which locale was active at creation time.

**Decision for Phase 0:** Store the English name as the canonical persistent name. Translation for display remains via `Championship.translate()` at render time. This is a behavioral change from the current code, where the translated name IS the map key. If this causes issues, the alternative is to use the `ChampionshipType` enum value (e.g., `"DEFAULT"`) as the stored name and translate only for display.

### 5. Transaction boundaries for DemoData

`DemoData.insertInitialData()` creates age groups and then looks up championships via `Championship.ofType()`. After Phase 0, championship creation happens via `addChampionship()` which persists to DB. This must happen within the same JPA transaction as age-group creation, or a separate transaction that completes before the lookup.

**Decision:** Review DemoData transaction boundaries and ensure championships are flushed before `ofType()` is called.

---

## Verification Criteria

1. **Startup with empty DB:** Championships are created in the database from the default age-group definitions. `Championship.findAll()` returns the same entries as before.

2. **Startup with existing DB (upgrade):** Championship table is bootstrapped from existing AgeGroup rows. All previously visible championships are present. No data loss.

3. **Startup with existing DB (already bootstrapped):** `bootstrapFromAgeGroups()` is a no-op. Startup time is not affected.

4. **Age-group file upload:** New championships from the spreadsheet are created in the DB. The in-memory cache is refreshed. ComboBox in age-group editor shows the new entries.

5. **EditChampionshipsDialog — Add:** New championship is persisted. Survives restart.

6. **EditChampionshipsDialog — Rename:** Championship is renamed in DB. AgeGroup.championshipName values that reference the old name are NOT automatically updated (they are separate strings). This is acceptable because the age-group editor allows reassignment, and the reconciliation path can handle the mismatch on next upload.

7. **EditChampionshipsDialog — Delete:** Championship is deleted from DB. AgeGroup rows that reference the deleted name become orphaned (no matching Championship). This is the same state as before Phase 0 when championships were ephemeral.

8. **Competition data import:** Championships in the imported data are reconciled into the DB. The cache is refreshed.

9. **DemoData path:** Demo competitions create championships in DB correctly.

10. **All 32 importing files:** No compilation errors. No behavioral changes in nominal paths.

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| `findAll()` performance regression (DB query instead of memory-only) | Low | Low | Cache is populated once per startup/reset; subsequent calls are pure memory reads. Same as before. |
| Rename in dialog leaves orphaned championshipName on AgeGroup rows | Medium | Low | This is an existing problem (rename in dialog today doesn't update AgeGroup.championshipName either, since the dialog only edits the in-memory map). Phase 0 does not make it worse. Can be addressed later. |
| Translation-dependent championship name changes persist differently per locale | Medium | Medium | Seed DEFAULT with the English name and use `translate()` for display. Document this decision. |
| DemoData transaction boundary issues | Medium | Low | Test DemoData path explicitly. Add flush if needed. |
| H2 case-insensitive unique constraint | Low | Low | Enforce uniqueness in application code via canonicalization. DB constraint is a safety net. |

---

## Sequencing Summary

```
Step 0.1  Add JPA annotations to Championship ← foundational, do first
Step 0.2  Register in JPAService               ← immediately after 0.1
Step 0.3  Create ChampionshipRepository         ← can start in parallel with 0.1/0.2
Step 0.4  Rewrite Championship static methods   ← depends on 0.3
Step 0.5  Startup migration in Main.injectData  ← depends on 0.3
Step 0.6  Update age-group reload path          ← depends on 0.3
Step 0.7  Update competition data import path   ← depends on 0.3
Step 0.8  Verify EditChampionshipsDialog        ← depends on 0.4
Step 0.9  Update AgeGroupEditingFormFactory      ← trivial, depends on 0.4
Step 0.10 Verify DemoData                       ← depends on 0.4
```

Steps 0.1 + 0.2 are the foundation. Step 0.3 can be developed in parallel. Steps 0.4–0.10 depend on 0.3 and are mostly small, targeted changes.

---

## Future Phase 0 Extensions (deferred to Phase 1+)

These fields will be added to the Championship entity in later phases but are mentioned here so the Phase 0 schema design can accommodate them without a second migration:

| Future field | Type | Default | Phase |
|-------------|------|---------|-------|
| `mixedCapable` | `Boolean` | `false` | Phase 1 |
| `mixedTeamName` | `String` | `null` | Phase 1 |
| `mixedBestN` | `Integer` | `null` | Phase 1 |

**Recommendation:** Do NOT add these columns in Phase 0. Hibernate DDL auto-update will add them when the fields are introduced in Phase 1. Adding them prematurely creates unused nullable columns with no consumer.
