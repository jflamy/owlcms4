# Mixed Championship Top-N Decision Summary

Date: 2026-04-29

## Purpose

This document captures the high-level decisions for mixed championship team modes, including separate top N men and top N women behavior for score-based mixed championships.

This is an approval summary only. It is not the detailed implementation plan.

## Current State

- The championship model already supports `mixedBestN`, `mixedMensBestN`, and `mixedWomensBestN`.
- The V2 export already carries these fields.
- `tracker-core` already preserves these fields.
- `owlcms-tracker` already has championship-aware logic that can use either a single mixed cap or separate men and women mixed caps.
- The older combined mixed mode remains valid: top N overall among both men and women. This corresponds to `mixedBestN`.
- The current implementation details expose membership and counting settings separately, but the approved summary should describe the feature as three user-facing modes.

## Key Decisions

### 1. Mixed championships are described as three user-facing modes

- Explicit membership.
- Top N mixed-gender (`mixedBestN`).
- Top N + M by gender (`mixedMensBestN` + `mixedWomensBestN`).

These three modes are the approved product description for the feature.

### 2. `mixedBestN` remains the primary mixed counting field

- If `mixedBestN` is positive, mixed scoring uses top N overall among both men and women.
- If `mixedBestN` is `0` or empty, mixed scoring uses the per-gender fields.

### 3. Top N + M by gender uses separate men and women caps

- `mixedMensBestN` defines the men's cap.
- `mixedWomensBestN` defines the women's cap.
- These fields are used when `mixedBestN` is `0` or empty.

### 4. This can likely be implemented primarily as a UI change

- The current backend already supports `mixedBestN`, `mixedMensBestN`, and `mixedWomensBestN`.
- The current tracker stack already supports the same fields.
- Detailed planning should confirm whether any backend cleanup is still needed, but the main intended change is the championship UI behavior.

### 5. The three-mode behavior is championship-specific

- We are not introducing a new stored competition-wide split mixed default.
- The existing older single mixed default does not define the new split-cap behavior.

### 6. Default values for all top-N fields are `0`

- `mensBestN` default = `0`
- `womensBestN` default = `0`
- `mixedBestN` default = `0`
- `mixedMensBestN` default = `0`
- `mixedWomensBestN` default = `0`

No derived category-count default is planned.

### 7. Per-gender top-N labels reuse the gendered championship labels

- The mixed split-cap fields for men and women should use the same labels as the existing gendered championship fields.
- The men label should match the current men top-N/team-size label.
- The women label should match the current women top-N/team-size label.
- We do not want separate mixed-specific wording for the per-gender fields.

### 8. UI enablement should follow the selected mixed mode

- The `mixedBestN` field is always available for the top N mixed-gender mode.
- If `mixedBestN` is positive, the per-gender mixed fields are disabled.
- If `mixedBestN` is `0` or empty, the per-gender mixed fields are enabled.

### 9. Effective precedence and values must be resolved in OWLCMS

- OWLCMS is the source of truth for championship rules.
- The effective precedence between `mixedBestN` and the per-gender mixed caps should remain aligned with OWLCMS behavior.
- Exported championship data should carry the effective values that downstream consumers use.
- `owlcms-tracker` must not independently derive fallback or precedence behavior.

### 10. Tracker impact is expected to be limited

- No new schema concept is required in `tracker-core`.
- No redesign of the championship-aware mixed-team selection algorithm is expected in `owlcms-tracker`.
- Tracker work is expected to be limited to verification, presentation alignment, and any small adjustments needed to use the effective exported values consistently.

## Planning Implications

- Detailed planning should start with OWLCMS championship rule resolution and the championship editor behavior.
- The detailed plan should present the mixed configuration as three modes in the UI and documentation.
- The detailed plan should make `mixedBestN > 0` and `mixedBestN == 0/empty` drive the UI state for the per-gender fields.
- Tracker planning should assume that OWLCMS exports effective mixed men and mixed women caps.
- The detailed plan should include OWLCMS label/translation reuse for the mixed per-gender fields.
- Existing test coverage around `mixedBestN`, `mixedMensBestN`, and `mixedWomensBestN` should be extended rather than replaced.

## Next Step

- After approval of this summary, produce a file-by-file detailed implementation plan for `owlcms4`, `tracker-core` verification points, and `owlcms-tracker`.