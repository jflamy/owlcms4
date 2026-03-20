## Plan: Mixed Championship Teams

Add a generic mixed-capable championship model in owlcms4 so any championship can optionally define an independent mixed team alongside the existing male and female teams. Introduce a stored Championship object first, because a championship is a real grouping used to compute awards: best athlete M, best athlete F, best athlete Mixed, best team M, best team F, and best team Mixed. A championship is a set of age groups, typically one M and one F for cases like Junior, but potentially many age groups for cases like Masters. Keep the current denormalized age-group input model for import and editing flows, and reconcile it into the stored Championship object using the existing practical rule that the last age group read wins. Use a minimal persistence extension built on the current participation model by keeping the existing teamMember boolean and adding a second mixedTeamMember boolean. When a championship does not define a mixed variant, gender-specific team membership must implicitly count toward the mixed result, so the mixed championship remains the sum of the male and female teams. Move championship/team-definition work to the front of the implementation so naming, enablement, and scoring rules exist before athlete editing and results flows are updated. SBDE import/export remains the final phase.

**Steps**
1. Phase 0: Normalize championships into a stored object. Introduce a persisted Championship entity or equivalent stored model representing a real award-computation grouping of age groups.
2. In Phase 0, define a championship as a set of age groups, typically one M and one F for cases like Junior, but potentially many M and F age groups for cases like Masters.
3. In Phase 0, preserve the current denormalized input shape where championship information is still read from age groups. Reconcile that input into the stored Championship object during load, import, or save operations, and preserve the current practical rule that the last age group read wins when conflicting championship metadata is encountered.
4. In Phase 0, update the current championship discovery flow in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\Championship.java and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroup.java so derived in-memory championships become backed by stored data rather than only by scanning age groups.
5. In Phase 0, update the age-group repository and configuration surfaces so persisted championships and denormalized age-group data stay synchronized, especially in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\agegroup\AgeGroupRepository.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\EditChampionshipsDialog.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupEditingFormFactory.java, and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\preparation\AgeGroupsFileUploadDialog.java.
6. Phase 1: Championship and team definition. Introduce the ability to declare that a stored championship supports an independent mixed team, including the mixed team naming and the mixed scoring/top-N configuration needed to treat it as a first-class team.
7. In Phase 1, anchor the feature on the stored championship object while keeping age groups as the denormalized source used by current editing and upload flows, instead of creating a JR-specific path.
8. In Phase 1, extend c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\competition\Competition.java with mixed-team configuration parallel to the existing mens and womens best-N/team-size settings so mixed scoring rules are configured up front.
9. In Phase 1, identify or add the configuration UI or import surface where championships and age groups are defined so mixed-capable championships and their mixed team names can be created before athlete assignment begins.
10. Phase 2: Participation model extension. Update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\category\Participation.java to keep the current teamMember boolean for gender-specific membership and add a second mixedTeamMember boolean for explicit mixed-team membership when a championship defines a mixed variant.
11. In Phase 2, preserve backward compatibility for existing records by defaulting mixedTeamMember to false. When a championship does not opt into a separate mixed variant, treat gender-specific team membership as implicitly contributing to the mixed result.
12. In Phase 2, extend athlete-side helper methods and serialization-facing accessors so the current team-membership API can expose both membership dimensions cleanly instead of forcing everything through one checkbox set.
13. Phase 3: Team computation and reporting. Update the team rollup pipeline so mixed-capable championships use explicit mixedTeamMember membership, while championships without a mixed variant continue to compute mixed results as the sum of the male and female teams.
14. In Phase 3, implement the mixed-inclusion rule through a predicate-based selection mechanism, with behavior equivalent to "automatically include in mixed if present in gender" when no explicit mixed variant exists. This keeps the fallback rule centralized instead of duplicating special cases throughout team aggregation.
15. In Phase 3, refactor the reporting-bean and team-key generation path in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\competition\Competition.java and the aggregation path in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamResultsTreeData.java so mixed teams can switch cleanly between explicit mixed-team selection and legacy aggregate behavior.
16. Phase 4: Athlete editing UI. Replace the current single binary team-membership control in c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\shared\NAthleteRegistrationFormFactory.java with a UI that presents gender-specific memberships and mixed memberships separately when a championship defines a mixed variant.
17. In Phase 4, the recommended UI direction is two distinct controls or grouped rows/sections: one for gender-specific team membership and one for mixed-team membership. This matches the two-boolean participation model and avoids overloading the existing CheckboxGroup semantics.
18. In Phase 4, when no mixed variant exists for a championship, do not require a separate mixed-membership UI because mixed participation is implied by gender-specific membership.
19. Phase 5: Results and operational flows. Update downstream team-selection and results views so mixed-capable championships expose separate male, female, and mixed teams with independent names and totals, while championships without a mixed variant continue to show the mixed total as the sum of both gender teams.
20. In Phase 5, update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamSelectionTreeData.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\data\team\TeamResultsTreeData.java, and c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\nui\results\TeamResultsContent.java so filters and displays do not collapse explicit mixed teams into legacy aggregate views, while preserving aggregate behavior where no mixed variant exists.
21. Phase 6: SBDE import/export, explicitly last. Once the internal model, team definition, UI, and results behavior are stable, revise spreadsheet import/export to round-trip independent mixed membership and mixed championship configuration.
22. In Phase 6, update c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\NRegistrationFileProcessor.java, c:\Dev\git\owlcms4\owlcms\src\main\java\app\owlcms\spreadsheet\JXLSSBDEExport.java, and related spreadsheet wrapper classes so old files continue to work while a new mixed-aware format is introduced.
23. Parallelism notes. Phase 0 blocks all later work because the championship identity and synchronization rules must exist before mixed-team behavior is layered on top. Phase 1 can proceed once the stored championship shape is settled. Phase 2 can proceed once Phase 1 defines mixed-capable championships and their rules. Phase 3 can start after Phase 2 exposes the second membership dimension. Phase 4 and Phase 5 can overlap after the computation APIs stabilize. Phase 6 should wait until the prior phases are validated.

**Phase 0 Impact Analysis**
1. Current in-memory championship creation happens in `Championship.findAll()`. That method initializes `Championship.allChampionshipsMap` by calling `AgeGroupRepository.allChampionshipsForAllAgeGroups()`, splitting the returned values into championship name and championship type, and then calling `Championship.addChampionship(...)` for each discovered entry.
2. Current refresh of the in-memory championship map happens in `AgeGroupDefinitionReader.loadAgeGroupStream(...)`, which calls `Championship.reset()` immediately after rebuilding age groups from the workbook. This means championship identity is currently a derived cache, not stored state.
3. Current startup migration logic happens in `Main.injectData(...)`. When the database is not empty, it already runs integrity checks, ensures age groups exist, calls `AgeGroupRepository.updateExistingChampionships()` to backfill `AgeGroup.championshipName` from `ageDivision` when missing, and only then proceeds with the rest of startup.
4. Current repository matching logic already treats `AgeGroup.championshipName` as the effective championship foreign-key-like value, with fallback to `ageDivision` only when `championshipName` is blank. This appears in repository queries such as `AgeGroupRepository.allParticipationsForAgeGroupAgeDivision(...)` and `AgeGroupRepository.findActiveAndUsedAgeGroupNames(...)`.
5. Current admin editing is split: `AgeGroupEditingFormFactory` binds an age group to a `Championship` selected through the age-group editor, while `EditChampionshipsDialog` edits the in-memory `Championship` map directly. That dialog is therefore not a persisted source of truth today and would need to change in Phase 0.

**Phase 0 Bootstrap Logic For Missing Stored Championships**
1. The best migration hook is inside `Main.injectData(...)`, immediately after `AgeGroupRepository.updateExistingChampionships()` and before later computations depend on championship identity. That is the point where persisted age groups are known to exist and their `championshipName` field has already been normalized.
2. At startup, if stored championships do not exist yet, bootstrap them by reading the persisted age groups, not by reading the age-group spreadsheet again. Persisted age groups are the authoritative source for migration because they reflect the actual database state being upgraded.
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
	Championship type.
	Mixed-capable flag defaulting to false.
	Mixed team naming fields defaulting to null or empty.
	Relationship to the age groups that belong to the championship.
5. Type resolution during bootstrap should follow the same practical behavior the current in-memory model already uses. If persisted age groups disagree on type for the same championship name, preserve current behavior by letting the last age group read win rather than inventing a new reconciliation rule during the migration.
6. After bootstrapping stored championships, update the in-memory `Championship` cache to load from stored championships rather than rebuilding solely from age groups. If full cutover is not done immediately, the transition state should at least ensure `Championship.reset()` rebuilds from stored championship rows first and only falls back to age-group derivation if no stored championships exist.
7. The age-group reload path must also participate in synchronization. When `AgeGroupDefinitionReader.loadAgeGroupStream(...)` reloads age groups and currently calls `Championship.reset()`, the new Phase 0 behavior should additionally reconcile stored championships from the newly persisted age groups before the reset exposes data to the rest of the application.
8. The age-group upload path in `AgeGroupsFileUploadDialog` already routes through `AgeGroupRepository.reloadDefinitions(...)`, which eventually reaches `AgeGroupDefinitionReader.loadAgeGroupStream(...)`. That makes the reload path a second required synchronization point after startup migration.
9. The persistence migration should be idempotent. Running startup twice must not duplicate stored championships, and re-reading age groups must update the existing stored championship rows rather than create new ones for the same championship name.
10. Backward compatibility rule: until all callers are moved, existing code that resolves championships by `championshipName` should keep working. The stored object adds normalization and explicit metadata, but should not require an immediate rewrite of every query that currently uses `AgeGroup.championshipName`.

**Likely Phase 0 Code Changes**
1. Add a stored Championship entity or equivalent persisted model.
2. Add repository access for loading, creating, and reconciling stored championships.
3. Add a startup migration step in `Main.injectData(...)` after `AgeGroupRepository.updateExistingChampionships()`.
4. Update `Championship.findAll()` and `Championship.reset()` so the in-memory map is backed by stored championships, with age-group derivation retained only as a fallback during transition.
5. Update `EditChampionshipsDialog` so edits target persisted championships instead of only mutating the in-memory map.
6. Update age-group reload/import paths so stored championships are re-synchronized whenever age groups are replaced or reloaded.

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
4. Verify a mixed-capable championship can be defined before athlete assignment, with its own mixed team name and mixed top-N settings.
5. Verify an athlete can belong to a gender-specific team and an independent mixed team simultaneously when the championship defines a mixed variant.
6. Verify mixed scoring uses its own rules and does not implicitly reuse mensBestN or womensBestN when a mixed variant exists.
7. Verify championships without a mixed variant still produce a mixed result as the sum of the male and female teams, with no extra athlete membership entry required.
8. Verify results show separate male, female, and mixed teams with independent names and totals for a mixed-capable championship.
9. Verify championships without mixed support behave exactly as before in athlete editing, results, and reporting.
10. After the core feature is stable, verify SBDE round-trip behavior with both old files and a new mixed-aware format.

**Decisions**
- A championship should become a stored object before mixed-team support is added.
- A championship is a grouping used to compute awards: best athlete M, best athlete F, best athlete Mixed, best team M, best team F, and best team Mixed.
- A championship is a set of age groups, typically one M and one F, but sometimes many more, such as Masters age groups.
- The current denormalized age-group input will remain, and reconciliation into the stored championship object should preserve the current "last age group read wins" behavior unless a better rule is later defined.
- JR X was only an example; the implementation should remain generic.
- A minimal persistence extension is acceptable: keep teamMember and add mixedTeamMember on participation.
- When a championship defines a mixed variant, mixed membership is independent: an athlete may belong to both the gender-specific team and the mixed team for the same championship family.
- When no mixed variant exists for a championship, gender-specific membership implicitly contributes to the mixed result, which remains the sum of the male and female teams.
- Championship/team creation must happen early in the plan, before athlete membership UI and results work.
- SBDE import/export is intentionally last so the file format follows the stabilized internal model.

**Further Considerations**
1. Decide the minimum stored Championship shape needed in Phase 0: likely stable identity, display name, championship type, mixed-capable flag, mixed team naming, and linkage to participating age groups.
2. Decide whether mixed team naming should live on championship definitions, age-group metadata, or competition-level overrides. Recommendation: championship-level first, with competition-level override only if real use cases demand it.
3. Decide whether legacy computed MF summary rows should remain available as an optional report view after independent mixed teams exist. Recommendation: keep only if needed for backward-compatible reports; do not let that legacy summary drive the new model.
4. Expect translation updates for any new configuration labels and separate mixed-team membership UI text once implementation starts.