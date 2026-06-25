<!-- markdownlint-disable -->
# 65 → 66 → 67 Championship Migration — Findings & Plan

**Status:** IMPLEMENTED. `.mv.db` and JSON v1 fixtures inspected and covered by era-specific regression tests. Conclusion: the unified migration funnel covers the semantic 65/66/67 championship migration; the legacy enum storage gate passed without a native storage helper. Two small production fixes were needed: materialize all non-template effective championship names, and refresh the static current `Competition` after JSON v1 restore reconciliation.
**Updated:** 2026‑06‑25 — revised after inspecting the real `.mv.db` fixtures and the v65/v66 JSON v1 exports, then updated after implementation. The original "pre‑66 has blank championship names → add a new Stage 1" premise was **empirically invalidated** (see §2/§4).
**Repo:** `owlcms_67` (release 67). Java sources under `owlcms/src/main/java/app/owlcms/`.
**Owner note:** Written to be fully self‑contained so the work can be resumed even if the chat session is lost.

---

## 1. The three eras (corrected history)

The "championship" concept evolved across three database eras. The distinction is about **how championships are persisted**, *not* about whether age groups carry championship fields:

- **65.x** — `Championship` objects existed **only in memory**, derived from the age groups at runtime. They were never a real table. (Some 65 exports may contain championship data **by accident**, because the in‑memory objects were created from the age groups.) In a 65 `.mv.db` there is **no `CHAMPIONSHIP` table**. Age groups, however, *already* carry `championshipName` and `championshipType`.
- **66.x** — championships were **made persistent**: a `CHAMPIONSHIP` table exists with one row per championship. But the per‑championship defaults are still inherited from the `Competition` object (legacy columns), there is **no `competitionTemplate` column**, and `Competition` has **no `migrated` flag**.
- **67** — **cleanup**: a `COMPETITION_TEMPLATE` championship becomes the single source of truth (`competitionTemplate` column added), `Competition.migrated` is introduced and the legacy competition columns are wiped, and the retired `IWF` championship type is normalized to `U`.

**Key correction:** age groups carry correct `championshipName` + `championshipType` (including the Masters grouping and Youth/Junior/Senior) in **both** 65 and 66. So there is **no "blank championship name → name‑collapse" problem** for real databases. The migration concern is championship **persistence + cleanup**, and that is already handled by one shared funnel (see §3/§4).

---

## 2. Fixture evidence (read‑only H2 inspection)

Fixtures (provided, in place): `owlcms/src/test/resources/testDatabases/v65-h2v2.mv.db`, `…/v66-h2v2.mv.db`.

> **Caveat (from the maintainer):** these are **empty databases as loaded by OWLCMS** — i.e. the default age groups OWLCMS auto‑creates, not real user data. A real database to be migrated may differ substantially, and in particular **may contain an `IWF` championship with no backing age group** (an orphan). The migration must therefore be **table‑driven**, never driven by walking age groups (see §4.2).

**`ChampionshipType` enum ordinals (confirmed from the data):** `0=MASTERS`, `1=U`, `2=IWF`, `3=DEFAULT`. (Declared order in [ChampionshipType.java](../../../../../../main/java/app/owlcms/data/agegroup/ChampionshipType.java): `MASTERS, U, IWF, DEFAULT`.)

| Observation | v65 fixture | v66 fixture |
|---|---|---|
| `CHAMPIONSHIP` table | **absent** (13 tables) | **present**, 12 rows (14 tables) |
| `CHAMPIONSHIP.competitionTemplate` column | n/a | **absent** (67‑only) |
| `COMPETITION.migrated` column | **absent** | **absent** |
| `COMPETITION` legacy scoring/medal/team columns | present | present |
| AgeGroup `championshipName` / `championshipType` | **populated** | **populated** |
| Masters grouping (M30…W85) | all → name `Masters`, type `0` (MASTERS) | same |
| IWF age groups (YTH/JR/SR) | names `Youth`/`Junior`/`Senior`, type `2` (**IWF, not yet U**) | same |
| Other U age groups | U11..U20 own names; U23→`O21`; SF/SM→`Score`; all type `1` (U) | same |
| Open | name `Open`, type `3` (DEFAULT) | same |
| IWF in `CHAMPIONSHIP` rows | n/a | Junior/Senior/Youth rows have `type = IWF` |

**Net:** the only structural difference between the two fixtures is the **presence of the persisted `CHAMPIONSHIP` table**. Everything the semantic migration must "fix up" (template creation, `Competition.migrated`, IWF→U) is identical work for both, and is **already implemented** in the 67 funnel. The remaining risk is lower-level storage compatibility: old `AGEGROUP.CHAMPIONSHIPTYPE` values are numeric ordinals, while current JPA mapping expects enum names.

### 2.1 Storage-compatibility gate: `AgeGroup.championshipType`

Both v65 and v66 fixtures store `AGEGROUP.CHAMPIONSHIPTYPE` as `INTEGER`, while current `AgeGroup.championshipType` is mapped as `@Enumerated(EnumType.STRING)`. This is now the first migration risk.

Before assuming `reconcileFromAgeGroups()` can run, tests must prove that:

1. `JPAService.init(true, false)` succeeds on each fixture copy.
2. The current schema can read `AgeGroup` rows through JPA.
3. Old numeric values are converted or readable as `MASTERS` / `U` / `IWF` / `DEFAULT` before `ChampionshipRepository.reconcileFromAgeGroups()` depends on them.

If this gate fails, the required fix is a storage-format compatibility migration, not a championship semantic migration.

### 2.2 JSON v1 fixture evidence

JSON v1 fixtures (provided, in place):

- `owlcms/src/test/resources/testDatabases/65_1_Database_2026-06-24_20h09.json`
- `owlcms/src/test/resources/testDatabases/66_5_Database_2026-06-24_20h12.json`

Both JSON files have **no `formatVersion` field**, so `FormatDetector.importData(...)` must route them through the JSON v1 path (`CompetitionData.restore(InputStream)`). They confirm the v1 import question should be covered by tests, but it is a separate risk from the `.mv.db` numeric enum storage gate:

| Observation | v65 JSON export | v66 JSON export |
|---|---|---|
| Top-level `formatVersion` | absent | absent |
| `championships` array | **absent** | **present**, 12 rows |
| AgeGroup `championshipName` | populated | populated |
| AgeGroup `championshipType` values | string enum names: `DEFAULT`, `IWF`, `MASTERS`, `U` | same |
| AgeGroup `ageDivision` values | string enum names: `DEFAULT`, `IWF`, `MASTERS`, `U` | same |
| Championship `type` values | n/a | string enum names: `DEFAULT`, `IWF`, `MASTERS`, `U` |
| `Competition.migrated` / `competitionTemplate` | absent | absent |

**Net:** JSON v1 exports do **not** carry the H2 ordinal-storage problem; they encode enum values as strings. The JSON v1 regression tests should prove that persistent restore, not just object deserialization, calls the same `reconcileFromAgeGroups()` funnel and normalizes `IWF → U` while preserving the Youth/Junior/Senior grouping.

### Reusable read‑only inspection snippet

```bash
H2JAR=~/.m2/repository/com/h2database/h2/2.1.214/h2-2.1.214.jar
TMP=$(mktemp -d)
cp owlcms/src/test/resources/testDatabases/v65-h2v2.mv.db "$TMP/v65.mv.db"
cp owlcms/src/test/resources/testDatabases/v66-h2v2.mv.db "$TMP/v66.mv.db"
q() { java -cp "$H2JAR" org.h2.tools.Shell \
  -url "jdbc:h2:file:$1;ACCESS_MODE_DATA=r;IFEXISTS=TRUE" \
  -user sa -password "" -sql "$2" 2>&1; }
# e.g. list tables / dump age groups:
q "$TMP/v65" "select table_name from information_schema.tables where table_schema='PUBLIC' order by 1"
q "$TMP/v65" "select CODE, AGEDIVISION, CHAMPIONSHIPNAME, CHAMPIONSHIPTYPE from AGEGROUP order by 1"
```

---

## 3. Current behavior — the single shared funnel

All relevant migration entry points converge on `ChampionshipRepository.reconcileFromAgeGroups()` after persistence is initialized and legacy enum storage is readable. There is **no championship-era branching** — the same semantic funnel handles 65, 66 and 67, and it is idempotent. Tests and implementation work in this plan must target the direct startup/reconcile path; **do not add lazy-bootstrap tests or new lazy-bootstrap behavior**.

### 3.1 Entry points (call sites)

- **Startup (non‑empty DB):** `Main.injectData(InitialData, Locale)` → `reconcileFromAgeGroups()`. File: [Main.java](../../../../../../main/java/app/owlcms/Main.java). This is the path real upgraders hit — they just open their old DB.
- **JSON v1 import:** `FormatDetector.importData(InputStream)` routes v1 to `CompetitionData.restore(InputStream)` → `reconcileFromAgeGroups()`, then applies additional normalization. Files: [FormatDetector.java](../../../../../../main/java/app/owlcms/data/export/FormatDetector.java), [CompetitionData.java](../../../../../../main/java/app/owlcms/data/export/CompetitionData.java).
- **JSON v2 import:** `CompetitionDataV2.restore()` → `reconcileFromAgeGroups()`. File: [CompetitionDataV2.java](../../../../../../main/java/app/owlcms/data/export/v2/CompetitionDataV2.java).
- **Fresh/demo data:** `ProdData.insertInitialData(int)` → `reconcileFromAgeGroups()`. File: [ProdData.java](../../../../../../main/java/app/owlcms/data/jpa/ProdData.java).

### 3.2 What `reconcileFromAgeGroups()` does (≈ lines 312‑360)

1. Re‑entrancy guard via `RECONCILING` ThreadLocal.
2. `ensureCompetitionTemplate(em)` — find/create the `COMPETITION_TEMPLATE` championship; calls `migrateCompetitionIfNeeded(em, template)`.
   - For both v65 and v66 the `competitionTemplate` column is freshly added by schema‑update, so **no existing row is the template** → a new `COMPETITION_TEMPLATE` is created.
   - `migrateCompetitionIfNeeded` → `Competition.migrateToChampionship(template)` snapshots the legacy competition defaults into the template, flips `migrated = true`, and wipes the legacy columns. (This is the real 66→67 *Competition* step and must stay unchanged.)
3. `normalizeAgeGroupChampionshipNames(em, ageGroups)` (≈ line 597) — fills any **blank/`COMPETITION_TEMPLATE`** name from the age group **code**. **No‑op for both fixtures** (names already present).
4. `materializeRequiredChampionships(em, …)` — create stored `Championship` rows for age groups that need them. For v65 this creates the rows from the (already correctly named) age groups → all Masters age groups share the single `Masters` championship, etc.
5. Per‑age‑group reconcile of stored championship type/scoring.
6. `normalizeDefaultTypes(em)` → **calls `migrateLegacyIwfRows(em)`** (bulk IWF→U on `Championship` **and** `AgeGroup`), then normalizes the DEFAULT championship.
7. `normalizeCompetitionDefaultFlags(em)` → recomputes each championship's `useCompetitionDefaults` against the template.

---

## 4. Conclusion: no new migration stage is needed

### 4.1 The direct startup/reconcile funnel already covers 65 and 66

- **v65** (no `CHAMPIONSHIP` table): Hibernate schema‑update creates the empty table. The real startup path calls `reconcileFromAgeGroups()` directly. Names are already correct, so `materializeRequiredChampionships` produces the right grouped championships (single `Masters`, Youth/Junior/Senior, U11…U20, O21, Score, Open). Template is created, `Competition.migrated` flips true.
- **v66** (`CHAMPIONSHIP` table present): `reconcileFromAgeGroups()` reconciles the existing rows, adds the `COMPETITION_TEMPLATE`, migrates the Competition defaults, and recomputes `useCompetitionDefaults`.

In both cases the same idempotent funnel produces the 67 shape once old enum storage is readable. The "65 vs 66" semantic detection that the original plan agonized over is **moot** — both paths already converge.

### 4.2 IWF cleanup is already orphan‑safe (the important property)

The maintainer's constraint — *a real DB may have an `IWF` championship with no backing age group* — is exactly why the IWF cleanup must **not** be driven by iterating age groups. The existing `migrateLegacyIwfRows(em)` (≈ line 619) already does the right thing: two **independent bulk JPQL `UPDATE`s**, one over `Championship` and one over `AgeGroup` (plus the legacy `ageDivision` string), each catching every `IWF` row regardless of cross‑references:

```java
update Championship c set c.type = :u where c.type = :iwf
update AgeGroup ag set ag.championshipType = :u where ag.championshipType = :iwf
update AgeGroup ag set ag.ageDivision = :u where lower(ag.ageDivision) = :iwf
```

So an orphan `IWF` championship (no age group) is still migrated to `U`. Read‑time accessors (`ChampionshipType.normalizeOrDefault`, `AgeGroup.getConfiguredChampionshipType`) also fold `IWF → U`, so behavior is correct even before the bulk update persists. **Do not** replace this with age‑group‑driven logic.

### 4.3 One residual edge case (gated on evidence)

`normalizeAgeGroupChampionshipNames` fills a **blank** name from the age group **code** (not from `computeChampionshipName()`, which *throws* on blank). For a *truly ancient* DB whose Masters age groups have a **null** `championshipName`, this would name them per‑code (`M30`, `M35`, …) instead of grouping them under `Masters`. **Neither provided fixture exhibits this** (names are populated). Action: only add a targeted fix (map Masters codes → `Masters` when synthesizing a blank name) **if a real fixture demonstrates the null‑name case**. Do not build speculative machinery for it.

---

## 5. Deliverable: fixture‑based regression tests

Because the existing code covers both eras through the direct startup/reconcile path, the highest‑value, lowest‑risk work was to **prove it with tests** against the two real fixtures, and to lock in the behavior. Do **not** add a separate lazy-bootstrap test; test ordering and shared fixture state make it brittle, and it does not cover the real startup path better than direct reconciliation.

### 5.1 Where

The `.mv.db` migration tests are intentionally split by era so each class has clear persistence initialization expectations:

- [ChampionshipV65MigrationTest.java](ChampionshipV65MigrationTest.java) loads `v65-h2v2.mv.db`, verifies schema update creates an initially empty `CHAMPIONSHIP` table, proves `AgeGroup` rows are readable before reconciliation, and then checks the migrated end state.
- [ChampionshipV66MigrationTest.java](ChampionshipV66MigrationTest.java) loads `v66-h2v2.mv.db`, verifies the existing 12 persisted championship rows reconcile without duplicates, and checks the same migrated end state.
- [ChampionshipLegacyMigrationSupport.java](ChampionshipLegacyMigrationSupport.java) contains shared non-UI fixture loading and assertions. It clears only the championship cache via reflection in fixture setup; it must not call `Championship.reset()` before direct reconciliation because that would trigger lazy bootstrap and mask the startup path being tested.

The JSON v1 persistent-import tests are also split by era:

- [V65JsonV1LegacyImportTest.java](V65JsonV1LegacyImportTest.java) imports the v65 JSON fixture with no `championships` array.
- [V66JsonV1LegacyImportTest.java](V66JsonV1LegacyImportTest.java) imports the v66 JSON fixture with 12 championship rows.

- Existing JSON tests exercise mapper round-trips via `CompetitionData.importDataFromString(...)`; they do **not** exercise persistent restore, database clearing, or `reconcileFromAgeGroups()`.
- JSON fixtures already in `owlcms/src/test/resources/testDatabases/`: `65_1_Database_2026-06-24_20h09.json`, `66_5_Database_2026-06-24_20h12.json`.

> **Test rule (`add-test-case` skill):** tests must **not** create Vaadin UI objects. Drive everything through repositories/domain objects (`ChampionshipTest` already does).

### 5.2 `.mv.db` upgrade cases

0. **Fixture readability / enum storage gate.** Load each fixture, run `JPAService.init(true, false)`, then issue the first JPA read of `AgeGroup`. Assert it succeeds and that stored championship types resolve as expected before calling/relying on championship reconciliation. If this fails, implement the storage migration in §6 before continuing.
1. **v65 migration (no `CHAMPIONSHIP` table).** Load `v65-h2v2.mv.db`, run the funnel (mirror `Main.initData` essentials, as `setupTests` already does). Assert:
   - all Masters age groups (M30…W85) resolve to a **single** `Masters` championship (type MASTERS);
   - `Youth`/`Junior`/`Senior` exist with type **U** (IWF migrated away);
   - U11..U20 / O21 / Score / Open championships exist with the expected types;
   - `Competition.isMigrated() == true` and the template holds the former competition defaults;
   - **no** `IllegalStateException` from the "missing championshipName" guard.
2. **v66 migration (persisted `CHAMPIONSHIP`).** Load `v66-h2v2.mv.db`. Assert the same end state, and that the 12 pre‑existing championship rows reconcile (no duplicates), with Junior/Senior/Youth ending as type **U**.
3. **Idempotency.** Run the funnel **twice** on each fixture → no duplicate championships, stable types, `migrated` stays true.
4. **No IWF remains.** After migration, assert there are **zero** `Championship` rows and **zero** `AgeGroup` rows with type `IWF` (and no `ageDivision` equal to `IWF`). This broad fixture assertion is sufficient because `migrateLegacyIwfRows(em)` updates all `Championship` rows directly; no separate manufactured orphan-IWF test is required.

### 5.3 JSON v1 persistent-import cases

These tests must import through `FormatDetector.importData(InputStream)`, not through `CompetitionData.importDataFromString(...)`. The point is to cover the production v1 restore path: database clearing, entity merge/persist ordering, `CompetitionData.restore(...)`, `ChampionshipRepository.reconcileFromAgeGroups()`, and the post-import normalizers.

1. **v65 JSON import (no `championships` array).** Import `65_1_Database_2026-06-24_20h09.json` through `FormatDetector.importData(...)`. Assert the same end state as the v65 `.mv.db` test: grouped Masters championship, Youth/Junior/Senior preserved by name and ending as type **U**, expected U/Open/DEFAULT championships, `Competition.isMigrated() == true`, and no IWF remains.
2. **v66 JSON import (persisted `championships` array).** Import `66_5_Database_2026-06-24_20h12.json` through `FormatDetector.importData(...)`. Assert the same end state as the v66 `.mv.db` test: the 12 imported championships reconcile without duplicates, Junior/Senior/Youth become type **U**, template/default migration completes, and no IWF remains.

Do not add synthetic numeric-enum JSON tests unless evidence appears that real v1 JSON exports encoded `ChampionshipType` as ordinals. The provided v65/v66 JSON fixtures encode enum values as strings.

### 5.4 Running

Use the VS Code Java Test Runner per the `run-java-test` skill (JUnit 4 `AllTests` is a `WildcardPatternSuite`). **Do not run `mvn`** or trigger a build without explicit consent; if the JBR runtime is configured, prefer reloading the IDE to refresh the classpath. Validate Java edits with the Problems panel / error check (`verify-java-fix` skill) before declaring done.

---

## 6. Implementation fixes applied

Two fixture-backed failures required small production fixes:

1. **v65 `.mv.db` materialization:** `ChampionshipRepository.requiresMaterializedChampionship(...)` now materializes any nonblank effective championship name except the `COMPETITION_TEMPLATE` sentinel. This allows v65 startup reconciliation to persist self-named `U` championships such as `U11`, `U13`, etc., matching the v66 end state.
2. **JSON v1 current competition state:** `CompetitionData.restore(...)` now refreshes `Competition.getCurrent()` from the persisted row after `ChampionshipRepository.reconcileFromAgeGroups()`. The persisted competition was already migrated; the static current object could remain the deserialized pre-migration instance.

The numeric enum storage gate passed: both `.mv.db` fixtures can be initialized and `AgeGroup` rows can be read through JPA before reconciliation. Therefore **no** `AgeGroupChampionshipTypeStorageMigration` was added.

### 6.1 Deferred fixes only if future fixtures prove them

If a future migration fixture fails, the fix should still be **small and surgical**, e.g.:

- Numeric `AGEGROUP.CHAMPIONSHIPTYPE` storage: add an early native-SQL compatibility migration that runs after `JPAService.init(...)` has applied schema updates but before any `AgeGroup` entity query. It must convert legacy ordinals to enum names:
   - `0 -> 'MASTERS'`
   - `1 -> 'U'`
   - `2 -> 'IWF'` initially, then the existing `migrateLegacyIwfRows` converts it to `U`
   - `3 -> 'DEFAULT'`

   This migration must use native SQL / metadata, not JPQL over `AgeGroup`, because JPQL may already fail while the column is still numeric.

   Recommended package/class:

   ```text
   app.owlcms.data.jpa.migration.AgeGroupChampionshipTypeStorageMigration
   ```

   Recommended startup hook: call it from `Main.initConfig()` immediately after `JPAService.init(...)` and before `Config.initConfig()` / `UtcNormalizationMigration.normalizeAllToUtc(em)`. It belongs under `data.jpa.migration` because this is a persistence-storage compatibility issue, not an age-group domain migration.
- Masters null‑name grouping (§4.3): change the blank‑name fallback in `normalizeAgeGroupChampionshipNames` to map known Masters codes → `Masters` (or use a championship‑name computation that preserves grouping) **instead of** the raw code.
- A missing seeded value in the loader: extend the fixture‑prep step (mirror `initFixtureMixedTeamEnabled`). Missing mapped columns should normally be added by Hibernate schema update during `JPAService.init`; `initFixtureMixedTeamEnabled` is only a value-seeding precedent.

Keep `reconcileFromAgeGroups()` semantically the unified funnel. Do **not** reintroduce a separate "pre‑66 → 66" stage unless a fixture proves it is required.

---

## 7. Invariants the migration must keep (must‑holds)

- **Idempotent:** re‑running the funnel changes nothing (IWF bulk update is a no‑op once converted; template already exists; `migrated` already true).
- **Orphan‑safe IWF:** IWF→U is bulk/table‑driven, independent of age‑group references (§4.2).
- **Competition defaults preserved:** snapshotted into the template by `migrateToChampionship` exactly once; assert `migrated == true`.
- **Re‑entrancy:** all work stays inside the `RECONCILING` ThreadLocal guard so nested championship reads during migration cannot recurse.
- **No Masters fragmentation:** all Masters age groups share one `Masters` championship.

---

## 8. Key code anchors (for fast resumption)

| Symbol / location | File | ≈ line | Role |
|---|---|---|---|
| `reconcileFromAgeGroups()` | `owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipRepository.java` | 312 | The shared funnel (handles all eras). |
| `ensureCompetitionTemplate(em)` | same | 103 | Find/create template; calls `migrateCompetitionIfNeeded`. |
| `migrateCompetitionIfNeeded(em, template)` | same | 145 | Competition legacy defaults → template (66→67 Competition step). |
| `normalizeAgeGroupChampionshipNames(em, ags)` | same | 597 | Blank/`COMPETITION_TEMPLATE` name → canonicalize(**code**). No‑op for fixtures. (§4.3 residual risk.) |
| `materializeRequiredChampionships(…)` / `materializeChampionship(…)` | same | 505 / 523 | Create stored `Championship` rows from age groups. |
| `normalizeDefaultTypes(em)` | same | 455 (calls `migrateLegacyIwfRows`) | Bulk IWF→U + DEFAULT normalization. |
| `migrateLegacyIwfRows(em)` | same | 619 | **Orphan‑safe** bulk IWF→U (Championship + AgeGroup + ageDivision). |
| `normalizeCompetitionDefaultFlags(em)` | same | ~430 | Recompute `useCompetitionDefaults` vs template. |
| `ChampionshipType` (enum order) | `…/agegroup/ChampionshipType.java` | 11 | `MASTERS,U,IWF,DEFAULT` → ordinals 0/1/2/3. `normalizeLegacy`/`normalizeOrDefault` fold IWF→U. |
| `AgeGroup.getStoredChampionshipType()` | `…/agegroup/AgeGroup.java` | 299 | Raw field (package‑private). |
| `AgeGroup.getConfiguredChampionshipType()` | same | 302 | `normalizeOrDefault` (null/IWF → U). |
| `AgeGroup.computeChampionshipName()` | same | 227 | **Throws** if blank (does *not* derive from code). |
| `Competition.migrated` / `isMigrated()` | `…/competition/Competition.java` | 402 / 1465 | 67 signal. |
| `Competition.migrateToChampionship(template)` | same | 1484 | Validates + snapshots legacy defaults; wipes legacy columns. |
| `ChampionshipLegacyMigrationSupport` | `…/tests/migration/ChampionshipLegacyMigrationSupport.java` | — | Shared non-UI fixture loader/assertions for legacy `.mv.db` and JSON tests; avoids lazy bootstrap before direct reconcile. |
| `ChampionshipV65MigrationTest` | `…/tests/migration/ChampionshipV65MigrationTest.java` | — | v65 `.mv.db` startup/reconcile coverage; no pre-existing `CHAMPIONSHIP` table. |
| `ChampionshipV66MigrationTest` | `…/tests/migration/ChampionshipV66MigrationTest.java` | — | v66 `.mv.db` startup/reconcile coverage; pre-existing 12-row `CHAMPIONSHIP` table. |
| JSON v1 import routing | `…/data/export/FormatDetector.java` | — | Routes no-`formatVersion` v1 JSON to `CompetitionData.restore(...)`; then runs normalization. |
| JSON v1 restore | `…/data/export/CompetitionData.java` | — | Persistent v1 import; clears DB, persists imported entities, calls `reconcileFromAgeGroups()`, then refreshes static current `Competition`. |
| `V65JsonV1LegacyImportTest` | `…/tests/migration/V65JsonV1LegacyImportTest.java` | — | Persistent JSON v1 import coverage for v65 export with no `championships` array. |
| `V66JsonV1LegacyImportTest` | `…/tests/migration/V66JsonV1LegacyImportTest.java` | — | Persistent JSON v1 import coverage for v66 export with imported championship rows. |
| Fixtures | `owlcms/src/test/resources/testDatabases/` | — | `v65-h2v2.mv.db`, `v66-h2v2.mv.db`, `65_1_Database_2026-06-24_20h09.json`, `66_5_Database_2026-06-24_20h12.json`. |
| Deferred storage helper | `owlcms/src/main/java/app/owlcms/data/jpa/migration/AgeGroupChampionshipTypeStorageMigration.java` | — | **Not implemented** because the fixture gate passed. Only add if a future fixture proves numeric enum storage cannot be read through JPA. |

> Line numbers drift with edits — prefer locating by method name.

---

## 9. Implementation sequence (checklist)

1. [x] Inspect both `.mv.db` fixtures (§2). → real signal is `CHAMPIONSHIP` table presence; names already correct; IWF still type 2 in both.
2. [x] Inspect both JSON v1 fixtures (§2.2). → v65 has no `championships` array, v66 has 12 championship rows, both encode enum values as strings including `IWF`.
3. [x] Resolve IWF grouping question → already `Youth`/`Junior`/`Senior` in the data; migration just converts type `IWF → U` while keeping those names.
4. [x] Confirm the existing unified funnel + orphan‑safe `migrateLegacyIwfRows` already cover both eras semantically (§3/§4). → **no new championship semantic stage needed.**
5. [x] Add the fixture readability / enum storage gate test (§5.2 case 0). Do not add lazy-bootstrap ordering tests. → gate passed, no storage helper needed.
6. [x] Split `.mv.db` tests into [ChampionshipV65MigrationTest.java](ChampionshipV65MigrationTest.java) and [ChampionshipV66MigrationTest.java](ChampionshipV66MigrationTest.java).
7. [x] Add shared non-UI support in [ChampionshipLegacyMigrationSupport.java](ChampionshipLegacyMigrationSupport.java), using cache-only reset instead of lazy bootstrap.
8. [x] Fix v65 materialization by updating `ChampionshipRepository.requiresMaterializedChampionship(...)` to materialize all non-template effective championship names.
9. [x] Split JSON v1 persistent-import tests into [V65JsonV1LegacyImportTest.java](V65JsonV1LegacyImportTest.java) and [V66JsonV1LegacyImportTest.java](V66JsonV1LegacyImportTest.java), both using `FormatDetector.importData(...)`.
10. [x] Fix JSON v1 current competition state by refreshing `Competition.getCurrent()` from the persisted row after restore reconciliation.
11. [x] Run the focused tests via the VS Code Java Test Runner (`run-java-test` skill). **No `mvn`/build without explicit consent.**
12. [x] Update release notes (`update-release-notes` skill / canonical [ReleaseNotes.md](../../../../../../../../src/main/markdown/ReleaseNotes.md)) once behavior is confirmed.

---

## 10. Repo constraints to honor

- **Do NOT run `mvn`/builds** or deploy without explicit human consent. If the workspace uses a JBR runtime, prefer reloading the IDE window over compiling.
- Use **bash** for shell commands.
- **Never** edit `shared/src/main/resources/i18n/translation4.csv`. New keys go via a TSV file per the copilot‑instructions process.
- Temporary debug logs: `logger.warn(...)` (optionally with `LoggerUtils.whereFrom()`).
- Avoid fully‑qualified class names in new/edited Java — add imports.
- No `git commit`/`push` without explicit authorization.
