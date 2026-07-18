<!-- markdownlint-disable -->
# GAMX Normalization Implementation Plan (branch `67_1_gamx`)

This plan is written to be executed step by step by an automated agent. Do the phases
**in order**. After each phase, run the **Verify** commands and do not proceed if they fail.

## Status
- **Phases 0–3: COMPLETE.** Generator built, 16 JSON files produced and parity-checked, `GAMX2.java`
  carries the `Lift` dimension with a JSON loader, and both JUnit test classes are green (25 tests).
- **Phase 4: REMAINING** (tracker-core `Lift` dimension + new filenames in `gamx2.js`).
- **Phase 5: REMAINING** (lazy `gamx_zip` delivery as a per-plugin requirement).
- **Phase 6: REMAINING** (consolidate duplicate `gamx2.js`, delete legacy files, doc updates).

### Known future work (not yet scheduled)
- **`kgTarget` / `kgTargetIterative` are now `Lift`-aware.** Both have a `Lift`-taking overload
  (`kgTarget(..., Variant, Lift)` / `kgTargetIterative(..., Variant, Lift)`) that loads the matching
  parameter set per lift; the prior signatures remain as thin `TOTAL` delegates so existing callers
  are unchanged. Remaining: expose snatch/CJ kg-targets to UI/consumers when needed.

## Delivery decision — lazy `gamx_zip`, a per-plugin requirement (mirrors flags/logos)
GAMX param tables are delivered **on demand** exactly like flags and logos: OWLCMS holds the
canonical files, and a tracker plugin that needs them declares `requires: ['gamx_zip']`. The hub
then requests the zip once (428 handshake), extracts it to its local files dir, and caches it. The
generator may zip the params with gzip-level compression — the transfer is a single zip pulled once.

Why this model (chosen after weighing static bundling):
- **Self-excluding by build.** If a packaged build contains **only** plugins that do not declare
  `gamx_zip` (e.g. an OBS-only package), the requirement is never expressed, the 428 handshake never
  asks for it, and **no GAMX bytes are ever pulled or shipped**. This is the property we want, and
  it falls out of the existing mechanism for free — no special packaging-exclusion logic needed.
- **One copy, pulled once.** Reuses the proven `flags_zip` / `logos_zip` path end to end.
- **Nothing bundled in tracker.** Tracker repos carry no GAMX data; OWLCMS remains the single source.

Plugins that declare `requires: ['gamx_zip']`: the three standard scoreboards, the team scoreboards,
the books that render GAMX, and a future GAMX calculator plugin (see Phase 5e).



## Hard rules learned in Phase 3 (apply to every remaining Java edit/test)
These are the concrete mistakes we hit and the corrections. Do not repeat them.

1. **JUnit 4, never JUnit 5.** OWLCMS uses JUnit 4. Use `org.junit.Test`, `org.junit.BeforeClass`,
   `org.junit.AfterClass`, and `import static org.junit.Assert.*`. Do **not** import anything from
   `org.junit.jupiter.*` (`@BeforeAll`, `@DisplayName`, etc.) — it produces ~120 phantom compile errors.
2. **`new Athlete()` needs JPA up.** The `Athlete` constructor reads `Config.getCurrent()`, which
   throws NPE if JPA is not initialized. Every test class that constructs an `Athlete` must do this
   in `@BeforeClass` and close in `@AfterClass`:
   ```java
   @BeforeClass public static void setUp() {
       Main.injectSuppliers();
       JPAService.init(true, true);
       Config.initConfig();
       TestData.insertInitialData(5, true);
   }
   @AfterClass public static void tearDown() { JPAService.close(); }
   ```
   (Pattern copied from `RobiCategoriesTest`.)
3. **Set age via `setYearOfBirth(LocalDate.now().getYear() - age)`.** There is **no** `setAge(...)`,
   and `setBirthDate(Integer)` is deprecated. Use `setYearOfBirth(int)`.
4. **Realistic GAMX ranges.** GAMX scores land roughly in **640–960** for normal lifts (see
   `GAMX2ComparisonTest` constants), not 900–1500. Range assertions that are too tight will fail on
   correct output. Prefer structural checks (non-zero, finite, monotonic, distinct tables) over
   narrow magnitude windows.
5. **`getGamxA/U/M` require an age-bearing athlete.** Calling an age-variant entry point on an
   athlete with no year-of-birth returns 0/throws. Build the athlete with an age for those cases.
6. **Verify with `get_errors` + `runTests`, never `mvn`.** After each Java edit call `get_errors`;
   run the two test classes with `runTests`.

## Conventions used in this document
- "OWLCMS repo" = `/Users/jflamy/git/owlcms_67`
- "tracker repo" = `/Users/jflamy/git/tracker`
- "tracker-core repo" = `/Users/jflamy/git/tracker-core`
- All three repos are already on branch `67_1_gamx`. Do **not** create or switch branches.
- Do **not** run `mvn`, `git commit`, or `git push`. Only edit files and run the read-only / node / python verify commands shown.
- Shell is `zsh` on macOS. Use plain commands (no heredocs).

## Which tool to use for each kind of action
Use the dedicated VS Code agent tools instead of shell equivalents wherever possible — they are
faster and safer. Map of task → tool:

| Task | Tool to use | Notes |
|---|---|---|
| Find a file by name/path | `file_search` (glob, e.g. `**/GAMX2.java`) | Not `find`. |
| Find exact text / a symbol across files | `grep_search` (set `isRegexp` correctly) | Not `grep` in terminal. Use `includePattern` to scope. |
| Understand a concept / "where is X handled" | `semantic_search` | Call it alone, never in parallel. |
| Read a file before editing | `read_file` (read a large range, not many tiny reads) | Always read before you edit. |
| List a directory's contents | `list_dir` | Not `ls`. |
| Create a brand-new file | `create_file` | Never use it to overwrite an existing file. |
| Edit an existing file (one change) | `replace_string_in_file` | Include 3–5 lines of unchanged context before and after. |
| Edit existing files (several changes) | `multi_replace_string_in_file` | Batch independent edits in one call. |
| Check compile/lint errors after a Java/JS edit | `get_errors` (pass the file paths) | This is how you "verify it compiles" — do **not** run `mvn`. |
| Run a specific unit test | `runTests` (pass the test file path) | Preferred over terminal. For OWLCMS Java tests see the `run-java-test` skill. |
| Run a one-off python/node command or a generator script | `run_in_terminal` (mode `sync`) | Use for Phase 0/1 inspection, generator, and `node -e` smoke checks. |
| Rename a code symbol everywhere | `vscode_renameSymbol` | Use for safe renames (e.g. a method signature) instead of text replace when possible. |
| Find all call sites of a function/method | `vscode_listCodeUsages` | Use before changing a public signature. |
| Track multi-step progress | `manage_todo_list` | Create one todo per phase; mark in-progress/completed as you go. |

Rules of thumb:
- Before editing a file you have not read this session, `read_file` it first.
- After every Java edit, call `get_errors` on the changed file(s). After every JS edit, call
  `get_errors` too. Treat a non-empty error list as a blocker.
- Prefer `multi_replace_string_in_file` when you have several edits queued for the same or
  different files.
- Only use `run_in_terminal` for things the editor tools cannot do: running the python generator,
  `node -e` smoke tests, file copies/deletes, and the `grep`/`ls` **only** if a structured tool
  is genuinely insufficient.
- Do not run commands in parallel in the terminal; run one, wait, then the next.

## Canonical naming (target state)
Every parameter table is named:
```
params-{lift}-{variant}-{gender}.json
```
- `{lift}` = `total` | `snatch` | `cj`
- `{variant}` = `sen` | `age` | `u17` | `mas`
- `{gender}` = `men` | `wom`

Old → new mapping for the existing TOTAL tables:
| Old name (CSV in OWLCMS / JSON in tracker) | New name |
|---|---|
| `params_sen_men` / `params-sen-men` | `params-total-sen-men` |
| `params_sen_wom` / `params-sen-wom` | `params-total-sen-wom` |
| `params_iwf_men` / `params-iwf-men` | `params-total-age-men` |
| `params_iwf_wom` / `params-iwf-wom` | `params-total-age-wom` |
| `params_usa_men` / `params-usa-men` | `params-total-u17-men` |
| `params_usa_wom` / `params-usa-wom` | `params-total-u17-wom` |
| `params_mas_men` / `params-mas-men` | `params-total-mas-men` |
| `params_mas_wom` / `params-mas-wom` | `params-total-mas-wom` |

New SNATCH and CJ tables (16 files) come from `GAMX_CJ_Snatch.xlsx`:
```
params-snatch-sen-men.json  params-snatch-sen-wom.json
params-snatch-age-men.json  params-snatch-age-wom.json
params-snatch-u17-men.json  params-snatch-u17-wom.json
params-snatch-mas-men.json  params-snatch-mas-wom.json
params-cj-sen-men.json      params-cj-sen-wom.json
params-cj-age-men.json      params-cj-age-wom.json
params-cj-u17-men.json      params-cj-u17-wom.json
params-cj-mas-men.json      params-cj-mas-wom.json
```

## JSON format (single source of truth)
Array-of-arrays. No object keys. Two shapes by variant:
- `sen` (no age column): `[bodyMass, mu, sigma, nu]`
- `age`, `u17`, `mas` (has age column): `[age, bodyMass, mu, sigma, nu]`

This matches the existing tracker JSON exactly. CSV files will be deleted at the end of Phase 6.

---

# Phase 0 — Inspect the source workbook

**Tools:** `manage_todo_list` (seed one todo per phase) · `run_in_terminal` (python inspection) · record findings with `memory` (session scope) or a scratch file via `create_file`.

Goal: learn the sheet structure of `GAMX_CJ_Snatch.xlsx` so the generator (Phase 1) is correct.

1. Run:
```
cd /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx
python3 -c "import openpyxl,sys; wb=openpyxl.load_workbook('GAMX_CJ_Snatch.xlsx', read_only=True); print('SHEETS:', wb.sheetnames)"
```
If `openpyxl` is not installed, run `pip3 install openpyxl` first (no venv).

2. For each sheet name printed, dump the header row and first 2 data rows:
```
python3 -c "import openpyxl; wb=openpyxl.load_workbook('GAMX_CJ_Snatch.xlsx', read_only=True); s=wb['SHEETNAME']; rows=[[c.value for c in r] for r in s.iter_rows(max_row=3)]; print('SHEETNAME', rows)"
```
Replace `SHEETNAME` for each sheet.

3. Record, in a scratch note, which sheet corresponds to each `{lift}-{variant}-{gender}`
   combination, and which columns hold `age`, `bodyMass`(or `bmass`), `mu`, `sigma`, `nu`.
   The TOTAL sheets should match the existing CSV columns. Snatch and CJ sheets hold the new data.

**Verify Phase 0:** You have a complete mapping table: 24 logical tables
(3 lifts × 4 variants × 2 genders) → (sheet name, column indices). If snatch/CJ only exist
for some variants, record exactly which combinations exist; only generate those.

---

# Phase 1 — Build the generator and produce all JSON files

**Tools:** `create_file` (the generator script — never `create_file` over an existing file) · `run_in_terminal` (run the generator, the parity spot-check, and the copy commands) · `list_dir` to confirm outputs.

Goal: one script writes the canonical JSON set into a staging folder, then copies it to both repos.

1. Create the staging directory:
```
mkdir -p /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/_generated
```

2. Create the generator script at
   `/Users/jflamy/git/owlcms_67/owlcms/scripts/gamx_generate_json.py` with this behavior:
   - **TOTAL tables**: read the 8 existing CSV files in
     `owlcms/src/main/resources/gamx/` (`params_sen_*.csv`, `params_iwf_*.csv`,
     `params_usa_*.csv`, `params_mas_*.csv`). For each row after the header, output a JSON
     array of the numeric columns **in the same column order as the CSV**
     (sen = 4 numbers, others = 5 numbers). Write to
     `_generated/params-total-{variant}-{gender}.json` using the old→new variant map
     (`sen→sen`, `iwf→age`, `usa→u17`, `mas→mas`).
   - **SNATCH and CJ tables**: read `GAMX_CJ_Snatch.xlsx` using the sheet/column mapping
     recorded in Phase 0. Output array-of-arrays with the same column-order rule
     (sen = `[bodyMass,mu,sigma,nu]`, others = `[age,bodyMass,mu,sigma,nu]`). Write to
     `_generated/params-{lift}-{variant}-{gender}.json`.
   - Output format: compact JSON, one row array per line is preferred for diff-ability,
     but a single `JSON.dumps(rows)` is acceptable. Numbers must be plain numbers (not strings).
   - Print a summary: for each file, the row count.

3. Run the generator:
```
cd /Users/jflamy/git/owlcms_67
python3 owlcms/scripts/gamx_generate_json.py
```

4. Sanity-check one generated TOTAL file against the existing tracker JSON (they must be
   numerically identical for `sen`/`age`/`u17`/`mas`). Example for senior men:
```
python3 -c "import json; a=json.load(open('/Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/_generated/params-total-sen-men.json')); b=json.load(open('/Users/jflamy/git/tracker-core/static/gamx/params-sen-men.json')); print('rows', len(a), len(b)); print('row0 equal:', [round(x,6) for x in a[0]]==[round(x,6) for x in b[0]])"
```
Expect equal row counts and `row0 equal: True`.

5. Copy the generated files into both repos:
```
cp /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/_generated/*.json /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/
cp /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/_generated/*.json /Users/jflamy/git/tracker-core/static/gamx/
```
Do **not** delete the old CSV (OWLCMS) or old JSON (tracker-core) yet — they are removed in Phase 6 after parity tests pass.

**Verify Phase 1:**
```
ls /Users/jflamy/git/owlcms_67/owlcms/src/main/resources/gamx/params-total-*.json | wc -l   # expect 8
ls /Users/jflamy/git/tracker-core/static/gamx/params-total-*.json | wc -l                    # expect 8
ls /Users/jflamy/git/tracker-core/static/gamx/params-snatch-*.json /Users/jflamy/git/tracker-core/static/gamx/params-cj-*.json | wc -l   # expect up to 16
```

---

# Phase 2 — OWLCMS: switch GAMX2.java from CSV to JSON + add Lift dimension

**Tools:** `read_file` (read GAMX2.java fully first) · `vscode_listCodeUsages` (find call sites of `getResourcePath`, `loadCsv`, `computeGamx`, `getGamx*` before changing signatures) · `multi_replace_string_in_file` (batch the edits) · `get_errors` on GAMX2.java after editing (this replaces running `mvn`).

File: `owlcms_67/owlcms/src/main/java/app/owlcms/data/scoring/GAMX2.java`

Follow the OWLCMS repo rule: do not use fully-qualified class names; add imports instead.
Use `com.fasterxml.jackson.databind.ObjectMapper` (already a dependency).

1. Add a `Lift` enum next to the existing `Variant` enum:
```java
public enum Lift {
    TOTAL, SNATCH, CJ
}
```

2. Change the cache key to include lift. Replace the field
   `parameterCache` (currently `Map<Variant, Map<Gender, ArrayList<ParamRow>>>`) with a
   nested map keyed first by `Lift`, then `Variant`, then `Gender`. A simple approach:
   `Map<Lift, Map<Variant, Map<Gender, ArrayList<ParamRow>>>>`.

3. Replace `getResourcePath(Variant, Gender)` with `getResourcePath(Lift, Variant, Gender)`:
   - lift segment: `TOTAL -> "total"`, `SNATCH -> "snatch"`, `CJ -> "cj"`
   - variant segment: `SENIOR -> "sen"`, `AGE_ADJUSTED -> "age"`, `U17 -> "u17"`, `MASTERS -> "mas"`
   - gender segment: `M -> "men"`, `F -> "wom"`
   - return `"/gamx/params-" + lift + "-" + variant + "-" + gender + ".json"`

4. Replace `loadCsv(String)` with `loadJson(String resourcePath, boolean hasAge)`:
   - Get the stream via the existing `ResourceWalker.getResourceAsStream(resourcePath)`.
   - Parse with `new ObjectMapper().readValue(stream, double[][].class)`.
   - For each `double[] row`: if `hasAge` is false build `new ParamRow(SENIOR_AGE, row[0], row[1], row[2], row[3])`; if true build `new ParamRow(row[0], row[1], row[2], row[3], row[4])`.
   - Keep the capacity-estimate optimization if convenient, but it is optional now.
   - `hasAge` is `false` only for `Variant.SENIOR`; `true` for `AGE_ADJUSTED`, `U17`, `MASTERS`.

5. Update `loadParameters(...)` to take `(Lift, Variant)` (or iterate lifts) and populate the
   3-level cache by calling `loadJson(getResourcePath(lift, variant, M), hasAge)` and the F variant.

6. Thread a `Lift` parameter through `computeGamx(...)` and the cache lookups. Default existing
   public entry points (`getGamx`, `getGamxA`, `getGamxU`, `getGamxM`, the `computeGamx`
   overloads) to `Lift.TOTAL` so current callers keep computing the **total** score unchanged.

7. Add new public entry points for snatch and clean&jerk, mirroring the existing ones but
   passing `Lift.SNATCH` / `Lift.CJ`. Suggested signatures (keep style consistent with file):
```java
public static double getGamxSnatch(Athlete a, Integer snatchWeight, Variant variant) { ... Lift.SNATCH ... }
public static double getGamxCJ(Athlete a, Integer cjWeight, Variant variant) { ... Lift.CJ ... }
```

8. Update the class Javadoc block (lines ~33-36) to describe lift + variant naming and remove
   the `params_iwf`/`params_usa` references.

**Verify Phase 2:** Call `get_errors` on `GAMX2.java` and confirm it returns an empty list
(zero compile errors). This is the substitute for running `mvn`. If the workspace uses a bundled
JBR runtime and stale errors appear, reload the window to refresh the classpath — do not run Maven.

---

# Phase 3 — OWLCMS: unit tests for the conversion AND the new values  ✅ DONE

> **Outcome:** both classes are green (25 tests). They live under
> `owlcms/src/test/java/app/owlcms/data/scoring/` (next to the existing `GAMX2ComparisonTest`),
> **not** under `.../tests/`. They are JUnit 4 and initialize JPA in `@BeforeClass` per the
> "Hard rules learned in Phase 3" box above. The notes below are kept for traceability.

**Tools:** `create_file` (the new test classes) · `read_file` (check for an existing GAMX test to
reuse expected values) · `grep_search` (find any existing `Gamx`/`GAMX` test) · `get_errors` on
each test file · `runTests` with the test file paths (or the `run-java-test` skill for the JUnit
runner). Do **not** run `mvn`.

Goal: two distinct guarantees must be tested:
- **(A) Conversion is lossless** — the new JSON-backed loader produces the **same TOTAL scores**
  as the old CSV-backed code (this guards the Phase 6 deletion of the CSVs).
- **(B) New lift values are correct** — the new **SNATCH** and **CJ** tables produce the expected
  scores from the source workbook.

First check whether a GAMX test already exists and reuse its expected values:
```
grep_search  query="GAMX2|computeGamx|getGamx"  includePattern="owlcms_67/owlcms/src/test/**/*.java"  isRegexp=true
```
(An existing `GAMX2ComparisonTest` was found; its `MEN_TEST_CASES`/`WOMEN_TEST_CASES` give
known-good TOTAL values and the realistic 640–960 score range.)

## 3.1 Conversion parity test (Test A) — `GamxJsonParityTest`
Add `owlcms_67/owlcms/src/test/java/app/owlcms/data/scoring/GamxJsonParityTest.java` that:
- For a fixed grid of inputs (gender M/F; a spread of bodyweights; for age variants a spread of
  ages), computes the GAMX **TOTAL** score via the new JSON-backed `GAMX2`.
- Compares against **hard-coded expected values captured from the pre-change CSV behavior**.
  Capture these once *before* editing `GAMX2.java` (Phase 2): temporarily log current outputs for
  the chosen inputs, or copy them from an existing GAMX test if one exists. Paste the captured
  numbers into the test as constants.
- Asserts equality at 2-decimal precision.
- Includes cases for every variant: `Variant.SENIOR`, `AGE_ADJUSTED`, `U17`, `MASTERS`.

> Note (cross-check, optional but recommended): the existing tracker test
> `tracker/src/lib/gamx2.test.js` already contains known-good GAMX TOTAL values. Those same
> numbers can be reused as the expected constants here, giving an OWLCMS↔tracker cross-engine
> check for free.

## 3.2 New SNATCH/CJ values test (Test B) — `GamxLiftValuesTest`
Add `owlcms_67/owlcms/src/test/java/app/owlcms/data/scoring/GamxLiftValuesTest.java` that:
- Calls the new `getGamxSnatch(...)` / `getGamxCJ(...)` (or the `computeGamx(..., Lift.SNATCH)` /
  `Lift.CJ` overloads) for a fixed grid of inputs across all four variants
  (`SENIOR/age, U17, MASTERS`) and both genders, for whichever lift×variant combinations actually
  exist in `GAMX_CJ_Snatch.xlsx` (per the Phase 0 mapping).
- Compares against **expected values derived independently from the source workbook**. Produce
  these expected numbers by computing GAMX directly from the workbook's mu/sigma/nu rows for the
  chosen inputs — e.g. a small throwaway python snippet using the BCCG formula
  `GAMX = qnorm(pBCCG(lift, mu, sigma, nu)) * 100 + 1000` against the exact row in the relevant
  sheet (no interpolation if you pick body masses that land on a table row). Paste the results in
  as constants.
- Asserts equality at 2-decimal precision.
- Add at least one **sanity assertion** that a heavier snatch yields a higher snatch score for the
  same athlete (monotonicity), and that snatch and CJ scores for the same lifted kg differ
  (i.e. the tables are genuinely distinct, not accidentally pointing at the TOTAL files).

## 3.3 Generated-data structural test (cheap guard on the generator)
Add a small test (can live in `GamxLiftValuesTest`) that loads each generated JSON resource and
asserts: non-empty; column count is 4 for `sen` files and 5 for the age-bearing files; values are
finite numbers. This catches a mis-generated or mis-named file before it reaches production.

## 3.4 Run them
Run **only** these test classes with the `runTests` tool (pass the two file paths), or via the
VS Code Java Test Runner per the `run-java-test` skill. Do **not** run the whole suite, do **not**
run mvn.

**Verify Phase 3:** `runTests` reports both `GamxJsonParityTest` (Test A) and `GamxLiftValuesTest`
(Tests B + structural) passing (green). Do not proceed to Phase 6 deletion unless Test A passes.

---

## Hard rules for the remaining phases (4–6)
Same spirit as the Phase 3 box, but for the JS edits and the Phase 5 Java glue:

- **Line numbers in Phases 4–6 are approximate and will have drifted.** Never edit by line number.
  Anchor every edit with `grep_search`/`vscode_listCodeUsages` on the named symbol
  (`getGamxBasePath`, `loadParams`, `computeGamx`, `checkPluginPreconditions`, `handleLogosMessage`,
  `sendFlags`, `setMissingDataCallback`, etc.) and `read_file` the surrounding block first.
- **Confirm the hub's local files dir before adding the `local/gamx` candidate (Phase 4 step 5).**
  Read `competition-hub.js` `getLocalFilesDir()` / `resolveLocalDir(...)` and match the **exact**
  prefix the logos/flags handlers use; do not guess `process.cwd()/local/gamx` if the real code
  differs.
- **Mirror the existing flags/logos implementation verbatim.** `gamx_zip` is a third copy of an
  established pattern (`flags_zip`, `logos_zip`). Read those reference sites and follow them exactly
  rather than inventing new shapes — name, event string, and reset all have to line up.
- **OWLCMS Java rule still applies (Phase 5a/5b):** no fully-qualified class names — add imports.
  After each Java edit call `get_errors`; never run `mvn`.
- **No new JUnit tests are required for 4–6.** If you add any Java test, it must follow the Phase 3
  box (JUnit 4 + JPA init + `setYearOfBirth`). For tracker-core, use the existing `node -e` smoke
  test / Vitest, not a Java runner.

# Phase 4 — tracker-core: add Lift dimension to gamx2.js and prefer extracted dir

**Tools:** `read_file` (read gamx2.js first) · `multi_replace_string_in_file` (batch edits) · `get_errors` on gamx2.js · `run_in_terminal` for the `node -e` smoke test.

File: `tracker-core/src/scoring/gamx2.js`

1. Add a `Lift` export:
```js
export const Lift = { TOTAL: 'TOTAL', SNATCH: 'SNATCH', CJ: 'CJ' };
```

2. Replace the flat `PARAM_FILES` (variant → {M,F}) with a lift-aware structure
   `PARAM_FILES[lift][variant][gender]` using the new file names, e.g.:
```js
const VARIANT_SEG = { SENIOR: 'sen', AGE_ADJUSTED: 'age', U17: 'u17', MASTERS: 'mas' };
const LIFT_SEG = { TOTAL: 'total', SNATCH: 'snatch', CJ: 'cj' };
function paramFileName(lift, variant, gender) {
    const g = gender === 'M' ? 'men' : 'wom';
    return `params-${LIFT_SEG[lift]}-${VARIANT_SEG[variant]}-${g}.json`;
}
```
   You can drop the literal `PARAM_FILES` map and build the name with `paramFileName(...)`.

3. Update `loadParams(...)` to `loadParams(lift, variant, gender)`:
   - `cacheKey = \`${lift}-${variant}-${gender}\``
   - `fileName = paramFileName(lift, variant, gender)`

4. Update `computeGamx(...)` (find via `grep_search`, not by line number) to accept a
   `lift = Lift.TOTAL` parameter and pass it to `loadParams`. Keep `lift` defaulted to `TOTAL` so
   all existing callers are unchanged. Add `computeGamxSnatch(...)` / `computeGamxCJ(...)` thin
   wrappers if helpers will need them.

5. In `getGamxBasePath()`, source the param tables **solely** from the OWLCMS-delivered extracted
   directory. No param tables are bundled (`static/gamx` is removed from both repos — see Phase 6),
   so the function returns just:
```js
return path.join(process.cwd(), 'local', 'gamx');   // OWLCMS-delivered gamx_zip (lazy); sole source
```
   With no static fallback, an offline build has no GAMX data and `computeGamx` returns `0`
   gracefully until a `gamx_zip` pull populates `local/gamx`. (Confirm the exact local dir prefix
   from competition-hub `getLocalFilesDir()`; it defaults to `<cwd>/local`.)

**Verify Phase 4:**
```
cd /Users/jflamy/git/tracker-core && node -e "import('./src/scoring/gamx2.js').then(m=>{const v=m.computeGamx('M',73,300);console.log('total senior 73/300 =',v);if(!v||isNaN(v))process.exit(1)})"
```
Expect a finite number printed (uses the new `params-total-sen-men.json`).

---

# Phase 5 — Lazy delivery as a per-plugin `gamx_zip` requirement

**Tools:** `read_file` the reference implementations first — `FlagsZipHelper.java`, the `sendFlags`/`setMissingDataCallback` sites in `WebSocketEventForwarder.java`, `handleLogosMessage` in `binary-handler.js`, and the `logos`/`flags` sites in `competition-hub.js`. Then `create_file` (GamxZipHelper.java) and `multi_replace_string_in_file` for the rest. `get_errors` on every changed Java/JS file. Use `grep_search` for the Verify checks (not terminal `grep`).

The requirement is **per-plugin**: only plugins that compute GAMX declare it — the standard
scoreboards, the team scoreboards, the GAMX-rendering books, and a future GAMX calculator. Video
overlays, attempt boards, timers, jury/replays, OBS, documents do **not**. This is what makes an
OBS-only (or any GAMX-free) package automatically skip the pull entirely.

## 5a. OWLCMS: create GamxZipHelper (mirror FlagsZipHelper)
New file `shared/src/main/java/app/owlcms/utils/GamxZipHelper.java`, modeled on
`shared/src/main/java/app/owlcms/utils/FlagsZipHelper.java`:
- `getGamxDirectory()` → `ResourceWalker.getFileOrResourcePath("gamx")`.
- `hasGamxAvailable()` → directory exists.
- `createGamxZipBytes()` → zip **only** the `params-*.json` files in that directory
  (skip `.csv`, `.xlsx`, and the `_generated` folder). Return `byte[]`. (zip/deflate gives the
  ~2.4× compression measured; the transfer is a single zip pulled once.)

## 5b. OWLCMS: wire sendGamx + missing-data callback
File `owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java`:
- Add a private `sendGamx()` method modeled on `sendFlags()` (around line 2065). It calls
  `GamxZipHelper.createGamxZipBytes()` and `sender.sendBinary("gamx_zip", bytes)`.
- Register the callback wherever the other `setMissingDataCallback("flags", ...)` registrations
  are (there are two sites: around line 2173 and around line 2424). Add:
```java
sender.setMissingDataCallback("gamx", () -> sendGamx());
```
  At the startup site (~2424) the equivalent inline form is acceptable, matching neighbors.

## 5c. tracker-core: hub state for gamx
File `tracker-core/src/competition-hub.js`:
- Constructor (~line 132, next to `this.flagsLoaded = false;`): add `this.gamxLoaded = false;`
- Add `setGamxReady(v)` and `markGamxLoaded()` mirroring `setLogosReady` / `markLogosLoaded`
  (~lines 2563 and 2679). `markGamxLoaded()` sets the flag and emits `'gamx_loaded'`.
- In `checkPluginPreconditions(requires)` (~line 1019) add a case:
```js
case 'gamx_zip':
  if (!this.gamxLoaded) missing.push('gamx_zip');
  break;
```
- In the `resourceToEvent` map inside `requestPluginPreconditions` (~line 1059) add:
```js
'gamx_zip': 'gamx_loaded',
```
- In `reset()` (~line 2742, next to `this.flagsLoaded = false;`) add `this.gamxLoaded = false;`

## 5d. tracker-core: binary handler extracts gamx_zip
File `tracker-core/src/websocket/binary-handler.js`:
- In the dispatch chain (~line 261, near the `else if (messageType === 'logos_zip')` branch) add:
```js
} else if (messageType === 'gamx_zip') {
  await handleGamxMessage(payload, hub);
```
- Add `handleGamxMessage(zipBuffer, hub)` modeled exactly on `handleLogosMessage` (~line 443):
  use `AdmZip`, `resolveLocalDir('gamx', hub)`, `resetDirectory`, extract all entries,
  then `hub.setGamxReady(true)` and `hub.emit('gamx_loaded', { count, timestamp: Date.now() })`.
  Files extract to `<localFilesDir>/gamx/params-*.json`, which Phase 4 made the first base-path
  candidate.

## 5e. Declare the requirement on the plugins that COMPUTE GAMX (only those)
The dependency is required only by plugins that compute a GAMX score **locally** (they import
`calculateGamx` from `@owlcms/tracker-core/scoring` and therefore need the param tables). Plugins
that merely display a precomputed score sent by OWLCMS in the `update` message do NOT compute GAMX
and must NOT declare `gamx_zip`. Verified by `grep_search "calculateGamx"` across `tracker/src`:
the only GAMX-computing files are the three below.

Add `'gamx_zip'` to the `requires` array in `config.js` for these plugins (append, keeping existing
entries like `'flags_zip'`/`'logos_zip'`):

Team scoreboards (render GAMX team scoring):
- `tracker/src/plugins/teams/team-rankings/config.js`   (no prior `requires` → `requires: ['gamx_zip']`) — DONE
- `tracker/src/plugins/teams/team-scoreboard/config.js` (`['flags_zip']` → `['flags_zip','gamx_zip']`) — DONE

Result book (computes GAMX team scoring via `iwf-helpers/team-results-data.js`):
- `tracker/src/plugins/books/iwf-results/config.js`     (`['logos_zip']` → `['logos_zip','gamx_zip']`) — DONE

Do NOT add `gamx_zip` to:
- Standard scoreboards (`scoreboards/lifting-order`, `scoreboards/rankings`, `scoreboards/start-order`)
  — they display OWLCMS-precomputed scores and never call `calculateGamx`.
- `books/iwf-startbook` — start book, no scores, no GAMX computation.
- video overlays, attempt boards, timers, jury/replays, OBS, documents.

The per-plugin mechanism stays intact: when the future **GAMX calculator** plugin lands, it simply
declares `requires: ['gamx_zip']` in its `config.js` and the request/wait/extract is automatic — no
other wiring needed.

The registry already calls `competitionHub.ensurePluginPreconditions(config.requires)` in
`processData()` (`tracker/src/lib/server/scoreboard-registry.js` ~line 650), so declaring the
requirement is sufficient — request/wait/extract is automatic.

**Verify Phase 5 (static checks):**
```
grep_search  query="gamx_zip"  includePattern="tracker/src/plugins/scoreboards"   # 0 hits (no local GAMX)
grep_search  query="gamx_zip"  includePattern="tracker/src/plugins/teams"          # 2 hits (team-rankings, team-scoreboard)
grep_search  query="gamx_zip"  includePattern="tracker/src/plugins/books"          # 1 hit  (iwf-results only)
grep_search  query="gamx_zip"  includePattern="tracker/src/plugins/OBS"            # 0 hits
grep_search  query="gamxLoaded|gamx_loaded|setGamxReady|markGamxLoaded"  includePattern="tracker-core/src/competition-hub.js"
grep_search  query="gamx_zip|handleGamxMessage"  includePattern="tracker-core/src/websocket/binary-handler.js"
```

---

# Phase 6 — Consolidate duplicate, delete legacy files, final checks

**Tools:** `grep_search` (find importers and old-name stragglers) · `read_file` before any edit · `multi_replace_string_in_file` (doc/name updates) · `run_in_terminal` only for deleting the legacy `.csv`/old `.json` files and removing `_generated` · `get_errors` on edited files.

1. **Duplicate gamx2.js**: `tracker/src/lib/gamx2.js` is a near-copy of
   `tracker-core/src/scoring/gamx2.js`. Apply the **same** Phase 4 edits to it, OR (preferred)
   change tracker imports to use the tracker-core version and delete `tracker/src/lib/gamx2.js`.
   First check importers:
```
grep -rn "lib/gamx2" /Users/jflamy/git/tracker/src
```
   Only consolidate if every importer can resolve the tracker-core export; otherwise just mirror
   the edits in place. Update `tracker/src/lib/gamx2.test.js` accordingly.

2. **Delete legacy data files only after Phase 3 parity passes:**
   - OWLCMS CSVs: `owlcms/src/main/resources/gamx/params_*.csv` (8 files)
   - tracker-core: **delete the entire `tracker-core/static/gamx/` directory** (~46 MB). The
     library ships no param tables; OWLCMS is the sole source via `gamx_zip` extracted to
     `local/gamx` at runtime. Also delete the dead `tracker/src/lib/gamx2.js` + `gamx2.test.js`
     (no importers — every caller uses `@owlcms/tracker-core/scoring`).
   - tracker: **delete the entire `tracker/static/gamx/` directory** (~46 MB) for the same reason
     (no offline-dev fallback retained).
   - Remove the `_generated` staging folder.
   Use `git rm` is **not** required; deleting the files is enough (do not commit).

3. **Update references to old names** in docs/specs:
   - `owlcms/src/main/java/app/owlcms/data/scoring/GAMX2_SPEC.md` (table at lines ~42-45)
   - `owlcms/src/main/java/app/owlcms/data/scoring/gamx_routes.R` (lines ~17-18 use
     `params_sen_men.json`; update to `params-total-sen-men.json` if that script is still used)
   - `tracker-core/src/scoring/gamx2.js` header comment block (lines ~9-12)
   Search for any stragglers:
```
grep -rn "params_iwf\|params_usa\|params_sen\|params_mas\|params-iwf\|params-usa\|params-sen\|params-mas" /Users/jflamy/git/owlcms_67/owlcms/src /Users/jflamy/git/tracker/src /Users/jflamy/git/tracker-core/src | grep -v node_modules
```
   Expect **no** matches except intentional ones (none should remain).

**Verify Phase 6 (end to end, manual):**
- OWLCMS: parity test still passes; no compile errors in `GAMX2.java`, `GamxZipHelper.java`,
  `WebSocketEventForwarder.java`.
- tracker-core: `node -e` smoke from Phase 4 still returns a finite number.
- Behavior check (described, run by the user): open an **OBS-only** package → confirm OWLCMS
  never sends `gamx_zip` (no `gamx` request in logs). Open a **standard scoreboard**, a **team
  scoreboard**, or a **book** → confirm a `gamx` request is sent, `gamx_zip` is received and
  extracted to `<localFilesDir>/gamx`, and GAMX scores render.

---

# Summary of files touched
OWLCMS:
- `owlcms/scripts/gamx_generate_json.py` (new)
- `owlcms/src/main/resources/gamx/params-*-*-*.json` (new, generated)
- `owlcms/src/main/resources/gamx/params_*.csv` (deleted in Phase 6)
- `owlcms/src/main/java/app/owlcms/data/scoring/GAMX2.java` (Lift dimension + JSON loader)
- `owlcms/src/test/java/app/owlcms/data/scoring/GamxJsonParityTest.java` (new — Test A: conversion parity)
- `owlcms/src/test/java/app/owlcms/data/scoring/GamxLiftValuesTest.java` (new — Test B: snatch/CJ values + structural)
- `shared/src/main/java/app/owlcms/utils/GamxZipHelper.java` (new)
- `owlcms/src/main/java/app/owlcms/monitors/WebSocketEventForwarder.java` (sendGamx + callback)
- `owlcms/src/main/java/app/owlcms/data/scoring/GAMX2_SPEC.md`, `gamx_routes.R` (doc updates)

tracker-core:
- `src/scoring/gamx2.js` (Lift dimension + new filenames; loads solely from lazy `local/gamx`)
- `src/scoring/index.js` (export `computeGamxSnatch`, `computeGamxCJ`, `Lift`)
- `src/competition-hub.js` (gamxLoaded state, preconditions, event map, reset)
- `src/websocket/binary-handler.js` (gamx_zip dispatch + handleGamxMessage)
- `static/gamx/` directory **deleted entirely** (no bundled param tables); dead
  `src/lib/gamx2.js`/`gamx2.test.js` in the **tracker** repo deleted

tracker:
- `src/plugins/teams/team-rankings/config.js` (+gamx_zip)
- `src/plugins/teams/team-scoreboard/config.js` (+gamx_zip)
- `src/plugins/books/iwf-results/config.js` (+gamx_zip)
- `static/gamx/` directory **deleted entirely**
- (future) `src/plugins/gamx/calculator/config.js` declares `requires: ['gamx_zip']`
- `src/lib/gamx2.js` + `src/lib/gamx2.test.js` (consolidate or mirror edits)
