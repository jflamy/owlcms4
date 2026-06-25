<!-- markdownlint-disable -->
# 65 → 66 → 67 Championship Migration — Findings & Plan

**Status:** ANALYSIS COMPLETE, fixtures inspected. **No production code changed.** Conclusion: the existing unified migration funnel already covers all three eras; the deliverable is **fixture‑based regression tests** that prove it (and only add code if a test reveals a real gap).
**Updated:** 2026‑06‑24 — revised after inspecting the real `.mv.db` fixtures. The original "pre‑66 has blank championship names → add a new Stage 1" premise was **empirically invalidated** (see §2/§4).
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

**`ChampionshipType` enum ordinals (confirmed from the data):** `0=MASTERS`, `1=U`, `2=IWF`, `3=DEFAULT`. (Declared order in [ChampionshipType.java](../../owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipType.java): `MASTERS, U, IWF, DEFAULT`.)

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

**Net:** the only structural difference between the two fixtures is the **presence of the persisted `CHAMPIONSHIP` table**. Everything the migration must "fix up" (template creation, `Competition.migrated`, IWF→U) is identical work for both, and is **already implemented** in the 67 funnel.

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

All entry points converge on `ChampionshipRepository.reconcileFromAgeGroups()`. There is **no era branching** — the same funnel handles 65, 66 and 67, and it is idempotent.

### 3.1 Entry points (call sites)

- **Startup (non‑empty DB):** `Main.injectData(InitialData, Locale)` → `reconcileFromAgeGroups()`. File: [Main.java](../../owlcms/src/main/java/app/owlcms/Main.java). This is the path real upgraders hit — they just open their old DB.
- **Lazy bootstrap:** `ChampionshipRepository.bootstrapFromAgeGroups()` (≈ line 294) → `reconcileFromAgeGroups()`. Runs when the `CHAMPIONSHIP` table is empty — i.e. **exactly the v65 case** (table just created by Hibernate schema‑update, zero rows). File: [ChampionshipRepository.java](../../owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipRepository.java).
- **JSON v1 import:** `CompetitionData.restore(InputStream)` → `reconcileFromAgeGroups()`. File: [CompetitionData.java](../../owlcms/src/main/java/app/owlcms/data/export/CompetitionData.java).
- **JSON v2 import:** `CompetitionDataV2.restore()` → `reconcileFromAgeGroups()`. File: [CompetitionDataV2.java](../../owlcms/src/main/java/app/owlcms/data/export/v2/CompetitionDataV2.java).
- **Fresh/demo data:** `ProdData.insertInitialData(int)` → `reconcileFromAgeGroups()`. File: [ProdData.java](../../owlcms/src/main/java/app/owlcms/data/jpa/ProdData.java).

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

### 4.1 The unified funnel already covers 65 and 66

- **v65** (no `CHAMPIONSHIP` table): Hibernate schema‑update creates the empty table → `bootstrapFromAgeGroups()` sees zero rows → `reconcileFromAgeGroups()` runs. Names are already correct, so `materializeRequiredChampionships` produces the right grouped championships (single `Masters`, Youth/Junior/Senior, U11…U20, O21, Score, Open). Template is created, `Competition.migrated` flips true.
- **v66** (`CHAMPIONSHIP` table present): `reconcileFromAgeGroups()` reconciles the existing rows, adds the `COMPETITION_TEMPLATE`, migrates the Competition defaults, and recomputes `useCompetitionDefaults`.

In both cases the same idempotent funnel produces the 67 shape. The "65 vs 66" detection that the original plan agonized over is **moot** — both paths already converge.

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

Because the existing code already appears to cover both eras, the highest‑value, lowest‑risk work is to **prove it with tests** against the two real fixtures, and to lock in the behavior.

### 5.1 Where

Add to `ChampionshipTest` — the designated home for championship regressions; it already has `.mv.db` loading plumbing. File: [ChampionshipTest.java](../../owlcms/src/test/java/app/owlcms/tests/ChampionshipTest.java).

- Existing fixture constant: `FIXTURE_RESOURCE = "/testDatabases/mixedTestsJRSR.mv.db"` (≈ line 105).
- Loader: `loadFixtureIntoMemoryDatabase()` (≈ line 1540) copies the resource, runs H2 `SCRIPT TO` then `RUNSCRIPT FROM` into an in‑memory DB, then `JPAService.init(true,false)`. There is precedent for schema drift (`initFixtureMixedTeamEnabled()` for the missing `mixedTeamEnabled` column) — the same approach covers the missing `competitionTemplate`/`migrated` columns if needed.
- Fixtures already in `owlcms/src/test/resources/testDatabases/`: `v65-h2v2.mv.db`, `v66-h2v2.mv.db`.

> **Test rule (`add-test-case` skill):** tests must **not** create Vaadin UI objects. Drive everything through repositories/domain objects (`ChampionshipTest` already does).

### 5.2 Cases

1. **v65 migration (no `CHAMPIONSHIP` table).** Load `v65-h2v2.mv.db`, run the funnel (mirror `Main.initData` essentials, as `setupTests` already does). Assert:
   - all Masters age groups (M30…W85) resolve to a **single** `Masters` championship (type MASTERS);
   - `Youth`/`Junior`/`Senior` exist with type **U** (IWF migrated away);
   - U11..U20 / O21 / Score / Open championships exist with the expected types;
   - `Competition.isMigrated() == true` and the template holds the former competition defaults;
   - **no** `IllegalStateException` from the "missing championshipName" guard.
2. **v66 migration (persisted `CHAMPIONSHIP`).** Load `v66-h2v2.mv.db`. Assert the same end state, and that the 12 pre‑existing championship rows reconcile (no duplicates), with Junior/Senior/Youth ending as type **U**.
3. **IWF orphan safety.** Construct (or, if present, exploit) an `IWF` championship with **no** age group referencing it; assert it becomes **U** after the funnel. (Covers the maintainer's "no age group associated with an IWF championship" case via the bulk path.)
4. **Idempotency.** Run the funnel **twice** on each fixture → no duplicate championships, stable types, `migrated` stays true.
5. **No IWF remains.** After migration, assert there are **zero** `Championship` rows and **zero** `AgeGroup` rows with type `IWF` (and no `ageDivision` equal to `IWF`).

### 5.3 Running

Use the VS Code Java Test Runner per the `run-java-test` skill (JUnit 4 `AllTests` is a `WildcardPatternSuite`). **Do not run `mvn`** or trigger a build without explicit consent; if the JBR runtime is configured, prefer reloading the IDE to refresh the classpath. Validate Java edits with the Problems panel / error check (`verify-java-fix` skill) before declaring done.

---

## 6. If a test fails (only then, add code)

If any case in §5.2 fails, the fix is expected to be **small and surgical**, e.g.:
- Masters null‑name grouping (§4.3): change the blank‑name fallback in `normalizeAgeGroupChampionshipNames` to map known Masters codes → `Masters` (or use a championship‑name computation that preserves grouping) **instead of** the raw code.
- A missing column the loader doesn't seed: extend the fixture‑prep step (mirror `initFixtureMixedTeamEnabled`).

Keep `reconcileFromAgeGroups()` semantically the unified funnel. Do **not** reintroduce a separate "pre‑66 → 66" stage unless a fixture proves it is required.

---

## 7. Invariants the migration must keep (must‑holds)

- **Idempotent:** re‑running the funnel changes nothing (IWF bulk update is a no‑op once converted; template already exists; `migrated` already true).
- **Orphan‑safe IWF:** IWF→U is bulk/table‑driven, independent of age‑group references (§4.2).
- **Competition defaults preserved:** snapshotted into the template by `migrateToChampionship` exactly once; assert `migrated == true`.
- **Re‑entrancy:** all work stays inside the `RECONCILING` ThreadLocal guard so a lazy `Championship.findAll()` mid‑migration cannot recurse.
- **No Masters fragmentation:** all Masters age groups share one `Masters` championship.

---

## 8. Key code anchors (for fast resumption)

| Symbol / location | File | ≈ line | Role |
|---|---|---|---|
| `reconcileFromAgeGroups()` | `owlcms/src/main/java/app/owlcms/data/agegroup/ChampionshipRepository.java` | 312 | The shared funnel (handles all eras). |
| `bootstrapFromAgeGroups()` | same | 294 | Runs when `CHAMPIONSHIP` empty (the v65 path) → reconcile. |
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
| `ChampionshipTest` fixture + loader | `…/tests/ChampionshipTest.java` | 105 / 1540 | `.mv.db` test harness; `initFixtureMixedTeamEnabled` precedent for schema drift. |
| Fixtures | `owlcms/src/test/resources/testDatabases/` | — | `v65-h2v2.mv.db`, `v66-h2v2.mv.db`. |

> Line numbers drift with edits — prefer locating by method name.

---

## 9. Implementation sequence (checklist)

1. [x] Inspect both `.mv.db` fixtures (§2). → real signal is `CHAMPIONSHIP` table presence; names already correct; IWF still type 2 in both.
2. [x] Resolve IWF grouping question → already `Youth`/`Junior`/`Senior` in the data; migration just converts type `IWF → U` while keeping those names.
3. [x] Confirm the existing unified funnel + orphan‑safe `migrateLegacyIwfRows` already cover both eras (§3/§4). → **no new migration stage needed.**
4. [ ] Add `ChampionshipTest` cases (§5.2) loading `v65-h2v2.mv.db` and `v66-h2v2.mv.db`; seed any missing columns via the loader precedent.
5. [ ] Run the championship tests via the VS Code Java Test Runner (`run-java-test` skill). **No `mvn`/build without explicit consent.**
6. [ ] Only if a test fails: apply the smallest surgical fix (§6) and re‑run.
7. [ ] Update release notes (`update-release-notes` skill / `ReleaseNotes.md`) once behavior is confirmed.

---

## 10. Repo constraints to honor

- **Do NOT run `mvn`/builds** or deploy without explicit human consent. If the workspace uses a JBR runtime, prefer reloading the IDE window over compiling.
- Use **bash** for shell commands.
- **Never** edit `shared/src/main/resources/i18n/translation4.csv`. New keys go via a TSV file per the copilot‑instructions process.
- Temporary debug logs: `logger.warn(...)` (optionally with `LoggerUtils.whereFrom()`).
- Avoid fully‑qualified class names in new/edited Java — add imports.
- No `git commit`/`push` without explicit authorization.
