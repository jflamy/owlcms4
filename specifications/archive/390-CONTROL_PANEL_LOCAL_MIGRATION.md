> Archived historical information. May differ from the actual implementation. Not meant to be updated.

# Control Panel: `local/` Migration and Sparse Overrides

Status: done (2026-11-23, `controlpanel` 3.11.0-rc01).

This specification covers the Control Panel (`controlpanel` repository,
`owlcms/versionlist.go` and `owlcms/actions.go`) and its interaction with
OWLCMS resource resolution ([ResourceWalker.java](../../shared/src/main/java/app/owlcms/utils/ResourceWalker.java)).

## Context

OWLCMS resolves customizable resources (`css/`, `templates/`, `agegroups/`,
`flags/`, `pictures/`, `logos/`, `timing/`, `i18n/`, `sounds/`, `logback.xml`,
etc.) by checking `./local` (or `OWLCMS_LOCALDIR`) first and falling back to
`owlcms.jar` on the classpath.

When **Local templates only** (`Config.isLocalTemplatesOnly()`, which sets
`overridesOnly = true` in `ResourceWalker.getResourceList`) is enabled, OWLCMS
ignores `owlcms.jar` for `templates/` and `agegroups/` and lists only files
present under `local/templates/` and `local/agegroups/`.

### Problems with the prior behavior

1. **Missing `newDir/local` failed migration:**
   `restoreLocalFilesFromPreviousVersion` called `getTopLevelDirs(newLocal)`
   first, failing with `no such file or directory` when the target version had
   no `local/` directory.
2. **Empty `newDir/local` silently skipped everything:**
   An empty `newDir/local` caused `getTopLevelDirs(newLocal)` to return an
   empty list, so the diff saw 0 jar files and 0 local files and dropped real
   customizations such as a modified `local/css/plates.css`.
3. **Root-level files in `local/` were dropped:**
   Because the scan only inspected subdirectories, files directly under
   `local/` (such as `local/logback.xml`) were deleted and not migrated.
4. **Integrated bug fixes stayed pinned forever:**
   The prior logic compared `oldLocal` only against `oldJar`. If a user edited
   a stylesheet to work around a bug and a later release integrated that exact
   fix into `newJar`, the old copy was still copied into `newLocal`.
5. **Default files were always re-extracted into `newLocal`:**
   Even when a user had no customizations (or only a single modified CSS file),
   every update populated a full copy of the distribution defaults in
   `newDir/local`.

## Specification

### Target reset (Update and Import)

Both **Update** (`UpdateRelease`) and **Import** (`ImportDataAndConfig`) treat
`newDir/local` as a clean destination:

- Any pre-existing `newDir/local` is removed first. A missing `newDir/local` is
  normal and must not fail.
- If `oldDir/local` does not exist (or contains no non-metadata files),
  migration succeeds without creating `newDir/local`.
- OS-generated folder metadata files (`.DS_Store`, `Thumbs.db`) are ignored.
- Merging concurrent edits made in both `oldDir/local` and `newDir/local` is
  not performed automatically; the Import confirmation warning continues to
  state that changes in the destination version's `local/` are replaced.

### Two-step rule for standard paths

> Intent: only keep what is not in the new jar.

Applies to every path outside `templates/` and `agegroups/` (including `css/`,
`flags/`, `pictures/`, `logos/`, `timing/`, `i18n/`, `sounds/`, and root-level
files such as `logback.xml`):

1. **Step 1 (`oldLocal` vs `oldJar` -> `newLocal`):**
   - File absent from `oldJar` (user addition, e.g. `flags/CAN.svg`,
     `pictures/4123.jpg`) -> copy from `oldLocal` to `newLocal`.
   - File present in `oldJar` with a different SHA-256 (user modification, e.g.
     `css/plates.css`) -> copy from `oldLocal` to `newLocal`.
   - File present in `oldJar` with the same SHA-256 (unchanged default, e.g.
     `timing/timing.properties`) -> do not copy; OWLCMS reads the new version's
     file directly from `newJar`.
2. **Step 2 (cleanup `newLocal` vs `newJar` and compatibility check):**
   - For each standard-path file written to `newLocal`, if the same relative
     path exists in `newJar` with the identical SHA-256 (for example, a
     stylesheet bug fix now integrated into `newJar`), delete it from
     `newLocal`.
   - For each modified standard-path file that still differs from `newJar`
     (`oldLocal != newJar`), compare `newJar` against `oldJar` in the
     migration report:
     - `newJar == oldJar` -> distribution file unchanged (likely OK).
     - `newJar != oldJar` (or removed from `newJar`) -> distribution file also
       changed (flagged for user review).
   - Prune any parent directories that become empty, including `newLocal`
     itself if no files remain.

### Retrieve-new-version rule for local-only paths (`templates/`, `agegroups/`)

> Intent: keep physical copies, do not add files from the new jar that were not there previously, note files that were identical to the jar and treat them as "standard, must follow the new jar"

`templates/` and `agegroups/` are gated by `Config.isLocalTemplatesOnly()`.
When present in `oldLocal`, their files must remain physically present in
`newLocal`, and they are **not** pruned in Step 2.

- **Absent root:** If `oldLocal/templates/` (or `oldLocal/agegroups/`) does not
  exist (for example, on a sparse install), it is not created in `newLocal`.
- **Existing root:** Apply the same recursive classification to the root and
  every subdirectory present on disk in `oldLocal` (no special case for an
  untouched root). Whether a directory is **touched** is determined by its
  actual direct files on disk versus `oldJar` (a file added, edited, or
  absent); a subfolder that is absent from `oldLocal` (such as
  `templates/registration/`, `templates/scripts/`, or `templates/timing/old/`)
  is not copied and does not mark its parent directory as touched:
  1. **Untouched directory (direct files in `oldLocal/<dir>` == direct files in `oldJar/<dir>`):**
     All direct files are expected to track the new official release (extracted
     from `newJar`, including new direct files added in `newJar`). Recurse into
     any child subdirectories present in `oldLocal/<dir>`.
  2. **Touched directory (direct files in `oldLocal/<dir>` != direct files in `oldJar/<dir>`):**
     1. Prior Additions: Files in `oldLocal` but absent from `oldJar` -> copy from `oldLocal`.
     2. Prior Edits: Difference between `oldLocal` and `oldJar` (modifications) -> copy from
        `oldLocal` 
     3. Unmodified: No difference between the old local and  `oldJar` . The file is expected to track the official release.
        - **Present in `newJar`:** extract the updated version of that path
          from `newJar` so it remains official.
        - **Absent from `newJar`:** copy from `oldLocal` as a legacy file so
          anything still referencing it continues to work; on subsequent
          updates it is absent from the prior jar and is treated as an addition
          (case 1).
     4. Files present in `oldJar` but absent from `oldLocal` (removed by
        the maintainer):
        - **Present in `newJar`:** do not extract from `newJar`; the file stays
          removed in `newLocal` and continues to be treated as removed on
          subsequent updates.
        - **Absent from `newJar`:** nothing to extract; the file is gone.
     5. New Official files:  added in `newJar` directly inside a touched directory -> do
        not inject into `newLocal`. The maintainer can add them to the kit
        explicitly to start tracking them as kept unmodified documents (case 3).
     6. Immediate child subdirectories present in `oldLocal/<dir>` -> recurse
        with the same untouched/touched classification.
  3. **Brand-new subdirectories in `newJar`:**
     When a touched parent directory has untouched sibling subdirectories (a
     full kit layout rather than a sparse single-folder override), brand-new
     subdirectories introduced in `newJar` (absent from both `oldJar` and
     `oldLocal`) are extracted from `newJar` as is.

## Worked Examples

### 1. Touched local-only folder: `local/templates/competitionResults`

A kit maintainer (`localTemplatesOnly = true`) starts from a full `v1` kit and
customizes `local/templates/competitionResults` (`oldLocal <> oldJar` except
for kept documents):

| File in `local/templates/competitionResults/` | State in `v1` (`oldLocal` vs `oldJar`) | Result in `v2` (`newLocal`) | Result in `v3` (`newLocal`) |
| --- | --- | --- | --- |
| `USAW-Custom1.xlsx`, `USAW-Custom2.xlsx` | Added (not in `oldJar`) | Copied from `v1` | Copied from `v2` |
| `USAW_BARS-2026-08_en_US.xlsx` | Modified (`oldLocal != oldJar`); fix integrated into `v2` jar | Copied from `v1` (kept in `newLocal`, not pruned) | Now matches `v2` jar -> updated from `v3` jar |
| `CompetitionResults-LETTER.xlsx`, `_FlatFile.xlsx` | Kept identical (`oldLocal == oldJar`), still in `v2` jar | Updated from `v2` jar | Updated from `v3` jar |
| `USAW-BARS-2025_en_US.xlsx` | Kept identical (`oldLocal == oldJar`), removed from `v2` jar | Copied from `v1` as legacy file | Carried forward from `v2` as an addition |
| `CompetitionResults-A4.xlsx` | Removed (in `oldJar`, absent from `oldLocal`), still in `v2` jar | Stays absent | Stays absent |
| `Score-A4.xlsx` | Removed (in `oldJar`, absent from `oldLocal`), also removed from `v2` jar | Absent (gone from both) | Absent |
| `NewReport-A4.xlsx` (added in `v2` jar) | Not in `v1` | Not injected into touched folder | Stays absent |

### 2. Untouched local-only folder: `local/templates/timing`

In the same kit, the maintainer did not touch `local/templates/timing`
(`oldLocal == oldJar`: `TimingStats.xlsx` and `old/TimingStats_*.xlsx` match
`v1` jar identically):

- In `v1 -> v2`: `templates/timing/` is classified as untouched, so every file
  under `templates/timing/` in `v2` jar (updated `TimingStats.xlsx`, existing
  files under `old/`, and any new file added in `v2` jar) is copied as is into
  `v2/local/templates/timing/`.
- Because `v2/local/templates/timing/` matches `v2` jar identically, on
  `v2 -> v3` it is still untouched and tracks `v3` jar automatically.
- The same rule applies to untouched `local/agegroups/` in a kit where only
  `local/templates/competitionResults/` was modified.

### 3. Untouched standard folder (`local/timing`) vs. customized standard files

Outside `templates/` and `agegroups/`, OWLCMS always falls back to `owlcms.jar`
for any file absent from `local/`:

| Path | State in `v1` (`oldLocal` vs `oldJar`) | `v2` jar | Result in `v2/local/` |
| --- | --- | --- | --- |
| `local/timing/timing.properties` | Unchanged (`oldLocal == oldJar`) | Present in `v2` jar | **Not copied** (OWLCMS uses `v2` jar directly) |
| `local/css/plates.css` | Modified (`oldLocal != oldJar`) | Differs from `oldLocal` | **Copied** from `v1/local/css/plates.css` |
| `local/css/results.css` | Modified (`oldLocal != oldJar`) | Matches `oldLocal` (fix integrated) | Copied in Step 1, **pruned** in Step 2 (OWLCMS uses `v2` jar) |
| `local/flags/CAN.svg`, `local/pictures/4123.jpg` | Added (not in `oldJar`) | Not in `v2` jar | **Copied** from `v1/local/` |
| `local/logback.xml` | Modified (`oldLocal != oldJar`) | Differs from `oldLocal` | **Copied** from `v1/local/logback.xml` |

### 4. Untouched legacy install (`69.0.0-rc07` -> `69.0.0-rc10`)

When a user upgrades from a legacy release where `local/templates/` and
`local/agegroups/` are physically present and identical to `oldJar`,
`local/timing/` is identical to `oldJar`, and only `local/css/plates.css` was
edited:

- Standard unchanged paths (`timing/`, unmodified `css/`, `i18n/`, `sounds/`,
  `logback.xml`, etc.) are not copied.
- `local/css/plates.css` is copied from `oldLocal`.
- `local/templates/` and `local/agegroups/` are untouched directories present
  in `oldLocal`, so both are extracted fresh from `newJar` (keeping them
  physically present for `localTemplatesOnly` and identical to `newJar` for
  future updates).
- On a fresh sparse install (where `oldLocal/templates/` and
  `oldLocal/agegroups/` do not exist), neither directory is created in
  `newLocal`.

### 5. Sparse single-folder template override

When a user with no full kit manually creates only
`oldDir/local/templates/competitionResults/Custom.xlsx`:

- Only `templates/competitionResults/Custom.xlsx` is copied to `newDir/local/`.
- Untouched folders (`templates/timing/`, `agegroups/`) and brand-new `newJar`
  template categories are not created because they were not present in
  `oldDir/local`.

## Kit Maintainer Workflow

1. Install a new OWLCMS version (clean, without a pre-populated `local/`).
2. Run **Update** or **Import** from the previous kit version.
3. Untouched folders (e.g. `local/templates/timing`, untouched `local/agegroups`)
   receive all files from the new jar as is so `localTemplatesOnly` continues
   to work and those folders remain untouched for future updates.
4. Touched folders (e.g. `local/templates/competitionResults`) preserve
   additions, modifications, and deletions while updating kept distribution
   files from the new jar.
5. Non-template customizations (custom stylesheets, flags, pictures) carry over
   and automatically drop out once integrated identically into a new release.

## Implementation Status and Remaining Work

### Done (`controlpanel`)

- `owlcms/versionlist.go`: two-step `migrateLocalFiles` /
  `restoreLocalFilesFromPreviousVersion`, recursive direct-file
  `classifyRetrieveNewVersionSubtree`, `pruneStandardLocalMatchingJar`,
  per-folder migration reporting, dry-run report checkbox in the Import dialog
  (saved to `$HOME/Downloads` with Open/Folder links), and
  `<version> (extract from distribution)` extraction into `local/` without
  overwriting existing files.
- `owlcms/actions.go` & `shared/downloadutils.go`: `extractLocalOnInitialInstall = false`
  constant with `ExtractZipFiltered` so official `InstallRelease` / `UpdateRelease`
  do not unwrap `local/` while `InstallLocalZipFile` continues extracting `local/`
  from custom ZIP kits; `UpdateRelease` composed directly from `installRelease` +
  `ImportDataAndConfig`.
- `owlcms/versionlist_test.go`: unit tests covering missing `newDir/local`,
  modified `css/plates.css`, flags/pictures, root-level `logback.xml`,
  Step 2 pruning, missing `oldDir/local`, multi-step (`v1 -> v2 -> v3`) kit
  maintainer updates, sparse single-template overrides, dry-run reporting,
  extract-from-distribution, and skipping `local/` on official release install.

### Optional follow-ups

1. **OWLCMS `zip.xml` (optional):**
   Once Control Panel with `extractLocalOnInitialInstall = false` is shipped,
   decide whether to also omit `local/` from OWLCMS's release ZIP assembly.
2. **`AgeGroupContent` and `localTemplatesOnly`:**
   Decide whether [AgeGroupContent.java](../../owlcms/src/main/java/app/owlcms/nui/preparation/AgeGroupContent.java)
   should continue sharing `Config.isLocalTemplatesOnly()` with `templates/` or
   be decoupled.
