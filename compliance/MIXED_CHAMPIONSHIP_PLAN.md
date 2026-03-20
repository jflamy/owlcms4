## Plan: Mixed Championship Teams

Add mixed championship team support to owlcms4 so championships with a `_MIXED` ChampionshipType have independent mixed team membership and scoring alongside the existing male and female teams. Introduce a stored Championship object first, because a championship is a real grouping used to compute awards: best athlete M, best athlete F, best athlete Mixed, best team M, best team F, and best team Mixed. A championship is a set of age groups, typically one M and one F for cases like Junior, but potentially many age groups for cases like Masters. Keep the current denormalized age-group input model for import and editing flows, and reconcile it into the stored Championship object using the existing practical rule that the last age group read wins. Use a minimal persistence extension built on the current participation model by keeping the existing teamMember boolean and adding a second mixedTeamMember boolean. A championship whose type is a `_MIXED` variant (e.g. `U_MIXED`, `MASTERS_MIXED`, `IWF_MIXED`) has independent mixed team membership. A championship without a `_MIXED` type computes its mixed result as the sum of the male and female teams, requiring no extra membership entry. Move championship/team-definition work to the front of the implementation so scoring rules exist before athlete editing and results flows are updated. SBDE import/export remains the final phase.

**Design model:** Each championship is independent with its own type. There is no companion/pairing between championships. A championship with type `MASTERS` and a championship with type `MASTERS_MIXED` are two separate championships. `isMasters()` returns true for both; `isMixed()` distinguishes them. Any championship can be set to any `ChampionshipType`. `AgeGroup.isMixedTeams()` is simply `getChampionshipType().isMixed()` — it reports whether the age group's championship uses mixed team membership.

**Steps**
1. Phase 0: Normalize championships into a stored object. Introduce a persisted Championship entity or equivalent stored model representing a real award-computation grouping of age groups.
2. In Phase 0, define a championship as a set of age groups, typically one M and one F for cases like Junior, but potentially many M and F age groups for cases like Masters.
3. In Phase 0, preserve the current denormalized input shape where championship information is still read from age groups. Reconcile that input into the stored Championship object during load, import, or save operations, and preserve the current practical rule that the last age group read wins when conflicting championship metadata is encountered.
4. In Phase 0, update the current championship discovery flow in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\Championship.java and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroup.java so derived in-memory championships become backed by stored data rather than only by scanning age groups.
5. In Phase 0, update the age-group repository and configuration surfaces so persisted championships and denormalized age-group data stay synchronized, especially in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroupRepository.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\EditChampionshipsDialog.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupEditingFormFactory.java, and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupsFileUploadDialog.java.
6. Phase 1: Championship and team definition. Add `AgeGroup.isMixedTeams()` as the query point for whether an age group's championship uses mixed team membership. Add `mixedBestN` on Competition parallel to `mensBestN`/`womensBestN`. Update the EditChampionshipsDialog so users can set the ChampionshipType when creating or editing a championship.
7. In Phase 1, add a mixed team scoring field (`mixedBestN`) to Competition and to the Competition editing UI, following the existing pattern for mens/womens best-N.
8. In Phase 1, update EditChampionshipsDialog to include a type picker (ComboBox) for creating and editing championships, replacing the current hardcoded `ChampionshipType.U` for new championships.
9. Phase 1b: Legacy fallback cleanup. No databases with partial migration exist — all databases will be clean version 64. Remove the legacy `ageDivision` fallback and enforce that championship identity always comes from the stored Championship object.
10. In Phase 1b, remove the fallback branch in `AgeGroup.computeChampionshipName()` that returns `ageDivision` when `championshipName` is blank. After this change, a blank `championshipName` is an error, not a fallback condition.
11. In Phase 1b, simplify `ChampionshipRepository.reconcileFromAgeGroups()` so it no longer infers championship metadata from legacy `ageDivision` values. Reconciliation should read `championshipName` directly.
12. In Phase 1b, remove `AgeGroupRepository.updateExistingChampionships()` and its startup call. There are no pre-Phase-0 databases to backfill; the method is dead code.
13. In Phase 1b, verify that all age-group creation and reload paths (age-group editor, spreadsheet upload, demo/bootstrap data generators, JSON import) write `championshipName` explicitly so no code path can produce an age group with a blank `championshipName`.
14. Phase 2: Participation model extension. Update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\category\Participation.java to keep the current teamMember boolean for gender-specific membership and add a second mixedTeamMember boolean for explicit mixed-team membership when the participation's championship type is a `_MIXED` variant.
15. In Phase 2, preserve backward compatibility for existing records by defaulting mixedTeamMember to false. When a championship's type is not `_MIXED`, gender-specific team membership implicitly contributes to the mixed result.
16. In Phase 2, extend athlete-side helper methods and serialization-facing accessors so the current team-membership API can expose both membership dimensions cleanly instead of forcing everything through one checkbox set.
17. Phase 3: Team computation and reporting. Update the team rollup pipeline so championships with `_MIXED` types use explicit mixedTeamMember membership, while championships without a `_MIXED` type continue to compute mixed results as the sum of the male and female teams.
18. In Phase 3, implement the mixed-inclusion rule through a predicate-based selection mechanism using `Championship.isMixed()` / `AgeGroup.isMixedTeams()`, with behavior equivalent to "automatically include in mixed if present in gender" when the championship type is not `_MIXED`. This keeps the fallback rule centralized instead of duplicating special cases throughout team aggregation.
19. In Phase 3, refactor the reporting-bean and team-key generation path in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\competition\Competition.java and the aggregation path in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamResultsTreeData.java so mixed teams can switch cleanly between explicit mixed-team membership and legacy aggregate behavior based on `isMixed()`.
20. Phase 4: Athlete editing UI. Replace the current single binary team-membership control in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\shared\NAthleteRegistrationFormFactory.java with a UI that presents gender-specific memberships and mixed memberships separately when the championship type is `_MIXED`.
21. In Phase 4, the recommended UI direction is two distinct controls or grouped rows/sections: one for gender-specific team membership and one for mixed-team membership. This matches the two-boolean participation model and avoids overloading the existing CheckboxGroup semantics.
22. In Phase 4, when the championship type is not `_MIXED`, do not show a separate mixed-membership UI because mixed participation is implied by gender-specific membership.
23. Phase 5: Results and operational flows. Update downstream team-selection and results views so championships with `_MIXED` types expose separate male, female, and mixed teams with independent names and totals, while championships without a `_MIXED` type continue to show the mixed total as the sum of both gender teams.
24. In Phase 5, update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamSelectionTreeData.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamResultsTreeData.java, and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\results\TeamResultsContent.java so filters and displays handle explicit mixed teams distinctly from legacy aggregate views, while preserving aggregate behavior where the championship type is not `_MIXED`.
25. Phase 6: SBDE import/export, explicitly last. Once the internal model, team definition, UI, and results behavior are stable, revise spreadsheet import/export to round-trip independent mixed membership and championship type configuration.
26. In Phase 6, update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\NRegistrationFileProcessor.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\JXLSSBDEExport.java, and related spreadsheet wrapper classes so old files continue to work while a new mixed-aware format is introduced.
27. Parallelism notes. Phase 0 blocks all later work because the championship identity and synchronization rules must exist before mixed-team behavior is layered on top. Phase 1 can proceed once the stored championship shape is settled. Phase 1b can proceed once Phase 1 is complete and all creation paths have been verified. Phase 2 can proceed once Phase 1b removes the legacy fallback, because the participation model extension assumes clean championship identity. Phase 3 can start after Phase 2 exposes the second membership dimension. Phase 4 and Phase 5 can overlap after the computation APIs stabilize. Phase 6 should wait until the prior phases are validated.

**Phase 0 Impact Analysis**
1. Current in-memory championship creation happens in `Championship.findAll()`. That method initializes `Championship.allChampionshipsMap` by calling `AgeGroupRepository.allChampionshipsForAllAgeGroups()`, splitting the returned values into championship name and championship type, and then calling `Championship.addChampionship(...)` for each discovered entry.
2. Current refresh of the in-memory championship map happens in `AgeGroupDefinitionReader.loadAgeGroupStream(...)`, which calls `Championship.reset()` immediately after rebuilding age groups from the workbook. This means championship identity is currently a derived cache, not stored state.
3. Current startup migration logic happens in `Main.injectData(...)`. When the database is not empty, it already runs integrity checks, ensures age groups exist, calls `AgeGroupRepository.updateExistingChampionships()` to backfill `AgeGroup.championshipName` from `ageDivision` when missing, and only then proceeds with the rest of startup.
4. Current repository matching logic already treats `AgeGroup.championshipName` as the effective championship foreign-key-like value, with fallback to `ageDivision` only when `championshipName` is blank. This appears in repository queries such as `AgeGroupRepository.allParticipationsForAgeGroupAgeDivision(...)` and `AgeGroupRepository.findActiveAndUsedAgeGroupNames(...)`.
5. Current admin editing is split: `AgeGroupEditingFormFactory` binds an age group to a `Championship` selected through the age-group editor, while `EditChampionshipsDialog` edits the in-memory `Championship` map directly. That dialog is therefore not a persisted source of truth today and would need to change in Phase 0.

**Phase 0 Bootstrap Logic For Missing Stored Championships**
1. The migration hook is inside `Main.injectData(...)`, at startup when the Championship table is empty. No pre-Phase-0 databases exist (all databases are clean version 64), so there is no legacy backfill step.
2. At startup, if stored championships do not exist yet, bootstrap them by reading the persisted age groups. Persisted age groups are the authoritative source because they reflect the actual database state.
3. Bootstrap algorithm:
	Read all persisted age groups.
	Group them by `AgeGroup.computeChampionshipName()`.
	For each championship group, determine the canonical stored championship record.
	If no stored championship exists for that name, create it.
	If one already exists, reuse it.
	Populate its derived metadata from the age groups currently being read.
	Preserve current reconciliation semantics so conflicting denormalized metadata still resolves as “last age group read wins” until a better rule is introduced.
4. Minimum bootstrap fields for a missing stored championship:
	Stable key or database id.
	Championship display name.
	Championship type (any ChampionshipType value; `_MIXED` types indicate mixed team membership).
	Relationship to the age groups that belong to the championship.
5. Type resolution during bootstrap should follow the same practical behavior the current in-memory model already uses. If persisted age groups disagree on type for the same championship name, preserve current behavior by letting the last age group read win rather than inventing a new reconciliation rule during the migration.
6. After bootstrapping stored championships, update the in-memory `Championship` cache to load from stored championships rather than rebuilding solely from age groups. If full cutover is not done immediately, the transition state should at least ensure `Championship.reset()` rebuilds from stored championship rows first and only falls back to age-group derivation if no stored championships exist.
7. The age-group reload path must also participate in synchronization. When `AgeGroupDefinitionReader.loadAgeGroupStream(...)` reloads age groups and currently calls `Championship.reset()`, the new Phase 0 behavior should additionally reconcile stored championships from the newly persisted age groups before the reset exposes data to the rest of the application.
8. The age-group upload path in `AgeGroupsFileUploadDialog` already routes through `AgeGroupRepository.reloadDefinitions(...)`, which eventually reaches `AgeGroupDefinitionReader.loadAgeGroupStream(...)`. That makes the reload path a second required synchronization point after startup migration.
9. JSON import (both legacy and v2 formats) requires a restart. If the imported JSON includes championships, they are already present after restart. If the imported JSON has no championships (e.g. a v64 export from before championships were created), the startup bootstrap in step 1–2 creates them from the imported age groups. No special JSON-import-specific rebuild path is needed.
10. The persistence migration should be idempotent. Running startup twice must not duplicate stored championships, and re-reading age groups must update the existing stored championship rows rather than create new ones for the same championship name.
11. Backward compatibility rule: until all callers are moved, existing code that resolves championships by `championshipName` should keep working. The stored object adds normalization and explicit metadata, but should not require an immediate rewrite of every query that currently uses `AgeGroup.championshipName`.

**Likely Phase 0 Code Changes**
1. Add a stored Championship entity or equivalent persisted model.
2. Add repository access for loading, creating, and reconciling stored championships.
3. Add a startup migration step in `Main.injectData(...)` that bootstraps stored championships from age groups when the Championship table is empty.
4. Update `Championship.findAll()` and `Championship.reset()` so the in-memory map is backed by stored championships, with age-group derivation retained only as a fallback during transition.
5. Update `EditChampionshipsDialog` so edits target persisted championships instead of only mutating the in-memory map.
6. Update age-group reload/import paths (spreadsheet upload) so stored championships are rebuilt whenever age groups are replaced or reloaded. JSON import does not need a special path because it requires a restart, which triggers the startup bootstrap.

**Relevant files**
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroupRepository.java — repository logic that currently discovers championships from age groups and will need synchronization rules for stored championships.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroup.java — existing championshipName-based grouping and likely anchor for mixed-capable championship definitions.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\Championship.java — current runtime championship model that should become backed by stored championship data.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\competition\Competition.java — mens/womens team-size config and reporting-bean generation; needs mixed config early.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\category\Participation.java — current boolean team-membership storage; extend with mixedTeamMember.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\athlete\Athlete.java — current getAgeGroupTeams / setAgeGroupTeams projection that will need a second mixed-membership projection.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\EditChampionshipsDialog.java — existing championship editing surface that currently edits an in-memory championship map.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupEditingFormFactory.java — age-group editing surface that currently selects championships through age groups.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\CompetitionEditingFormFactory.java — competition settings surface where mixed team-size and mixed scoring fields will likely be added.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupsFileUploadDialog.java — denormalized age-group upload path that must keep working with stored championships.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamSelectionTreeData.java — team selection/display logic.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamResultsTreeData.java — team aggregation and scoring logic.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\shared\NAthleteRegistrationFormFactory.java — athlete editing form.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\results\TeamResultsContent.java — results filtering/presentation.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\NRegistrationFileProcessor.java — SBDE import handling, deferred to final phase.
- c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\JXLSSBDEExport.java — SBDE export handling, deferred to final phase.

**Verification**
1. Verify championships are stored as first-class objects and remain synchronized with denormalized age-group input.
2. Verify a championship can represent one M and one F age group for cases like Junior, and many M and F age groups for cases like Masters.
3. Verify conflicting denormalized championship metadata still resolves predictably using the current "last age group read wins" rule until a stronger reconciliation rule is introduced.
4. Verify a championship with a `_MIXED` type can be created before athlete assignment, with its own mixed team scoring configuration (mixedBestN).
5. Verify an athlete can belong to a gender-specific team (teamMember) and an independent mixed team (mixedTeamMember) simultaneously when the championship type is `_MIXED`.
6. Verify mixed scoring uses its own `mixedBestN` rules and does not implicitly reuse mensBestN or womensBestN when a `_MIXED` championship exists.
7. Verify championships without a `_MIXED` type still produce a mixed result as the sum of the male and female teams, with no extra athlete membership entry required.
8. Verify results show separate male, female, and mixed teams with independent names and totals for a `_MIXED` championship.
9. Verify championships without `_MIXED` types behave exactly as before in athlete editing, results, and reporting.
10. After the core feature is stable, verify SBDE round-trip behavior with both old files and a new mixed-aware format.

**Decisions**
- A championship should become a stored object before mixed-team support is added.
- A championship is a grouping used to compute awards: best athlete M, best athlete F, best athlete Mixed, best team M, best team F, and best team Mixed.
- A championship is a set of age groups, typically one M and one F, but sometimes many more, such as Masters age groups.
- The current denormalized age-group input will remain, and reconciliation into the stored championship object should preserve the current "last age group read wins" behavior unless a better rule is later defined.
- JR X was only an example; the implementation should remain generic.
- Each championship is independent with its own ChampionshipType. There is no companion/pairing between championships. A championship with type `MASTERS` and one with type `MASTERS_MIXED` are separate, unlinked championships. `isMasters()` returns true for both; `isMixed()` distinguishes them. Any championship can be set to any type.
- A minimal persistence extension is acceptable: keep teamMember and add mixedTeamMember on participation.
- When a championship's type is `_MIXED`, mixed membership is independent: an athlete may belong to both the gender-specific team (via teamMember) and the mixed team (via mixedTeamMember).
- When a championship's type is not `_MIXED`, gender-specific membership implicitly contributes to the mixed result, which remains the sum of the male and female teams.
- Championship/team creation must happen early in the plan, before athlete membership UI and results work.
- SBDE import/export is intentionally last so the file format follows the stabilized internal model.

**Further Considerations**
1. Decide whether legacy computed MF summary rows should remain available as an optional report view after independent mixed teams exist. Recommendation: keep only if needed for backward-compatible reports; do not let that legacy summary drive the new model.
4. Expect translation updates for any new configuration labels and separate mixed-team membership UI text once implementation starts.