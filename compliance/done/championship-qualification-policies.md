# Championship Qualification and Scoring Policies

Status: draft design agreement
Scope: championship configuration, qualification consequences, entry-total enforcement, medals, and team points

## Goal

Move the consequence of not reaching a category qualifying total (QT), the
entry-total rule, and the team-point scale from global/IMWA switches to the
championship configuration.

This allows championships held in the same competition to use different rules.
For example, Masters athletes registered in a Masters championship can use the
IMWA 80% entry-total rule, children registered in a Kids championship can have
no entry-total rule, and athletes registered in an Open championship can use the
20 kg rule.

The existing medal policy and team-points policy remain independent. A
championship can therefore award three medals but award team points for Total
only.

## Proposed Championship Settings

### Medal policy

Existing values:

- `ALL_THREE`: Snatch, Clean and Jerk, and Total medals.
- `TOTAL_ONLY`: Total medal only.
- `LIFTS_ONLY`: Snatch and Clean and Jerk medals only.

### Team-points policy

Existing values:

- `ALL_THREE`: points from Snatch, Clean and Jerk, and Total ranks.
- `TOTAL_ONLY`: points from the Total rank only.
- `LIFTS_ONLY`: points from Snatch and Clean and Jerk ranks only.

### QT not met

Add one championship-level qualification policy:

- `NO_RANKS_NO_MEDALS`: an event result below its enabled qualification
  threshold receives no rank and therefore no medal or team points for that
  event.
- `RANKS_WITHOUT_MEDALS`: preserve the event rank and any team points enabled by
  the team-points policy, but do not award the event medal.
- `RANKS_AND_MEDALS`: qualification thresholds do not restrict ranks, medals, or
  team points.

The policy applies independently to every medal event enabled by the medal
policy. Initially, only Total has a category qualification value. A future
extension can add Snatch Qualification (SQ) and Clean and Jerk Qualification
(CJQ) without adding more policy combinations:

- Snatch is compared with SQ.
- Clean and Jerk is compared with CJQ.
- Total is compared with QT.
- A qualification value of `0` means that the event has no qualification
  threshold.

BECAUSE we can show live rankings, Qualification is evaluated on the already performed lifts and aggregated totals, as if the athlete had withdrawn.

REMINDER: on a ALL_THREE medal event, the QT rules applies independently on the lifts, so bombing out in one lift does not void the medal in the other lift, but voids the Total (since the total becomes 0)

Opening declarations and requested weights do not satisfy a
qualification threshold.

### Entry-total rule

The registration category determines what entry total rule is applied.  In the same competitions, athletes registered as Masters could have different rules than athletes registered in the Open, or from kids where none would be applied.

Add one championship-level entry-total rule:

- `20_KG`
- `IMWA_80_PERCENT`
- `NONE`

Secondary championship participations do not alter the opening declarations.

### Team-point scale

Add a mutually exclusive selector in the championship scoring section:

- `IWF`: use the standard IWF point scale.
- `IMWA`: use the IMWA team-point algorithm, including reduced points for
  one- and two-athlete categories.
- `CUSTOM`: enable the editable first-, second-, and third-place point values.

This should be a radio-button group, segmented control, or select field rather
than independent checkboxes. Only `CUSTOM` enables the three top-value fields.
The control should be named "Team-point scale" or "Team-point rules" to avoid
confusion with score-based team ranking systems.

## Example Configurations

| Ruleset | Medals | Team points | QT not met | Entry-total rule | Team-point scale |
| --- | --- | --- | --- | --- | --- |
| IMWA | Total only | Total only | `RANKS_WITHOUT_MEDALS` | IMWA 80% | IMWA |
| Spanish Masters | Total only | Total only | `NO_RANKS_NO_MEDALS` | IMWA 80% | IMWA |
| USAW | All three | Total only | `NO_RANKS_NO_MEDALS` | 20 kg | IWF |
| PanAm | All three | All three | `NO_RANKS_NO_MEDALS` | 20 kg | IWF |
| Games | Total only | Total only | `NO_RANKS_NO_MEDALS` | 20 kg | IWF |
| Simple | Total only | Total only | `RANKS_AND_MEDALS` | None | Custom |

These are examples, not hard-coded presets. The championship settings remain
independently configurable.

## Registration and Participation Boundary

OWLCMS already distinguishes the registration category from secondary category
participations:

- `Athlete.category` is the persisted registration category.
- `Athlete.mainRankings` is the participation matching that category.
- The registration category currently supplies the championship type used to
  choose the 20 kg or Masters 80% opening rule.
- Final QT evaluation is already category-specific: ranking computation passes
  each participation's category to the eligibility check.

The resulting ownership is:

| Decision | Governing championship |
| --- | --- |
| Weigh-in and entry-total enforcement (QT) | Registration category's championship.  In the future a lift-specific entry requirement (SQ, CJQ) may be added as used in some federations ("base value") |
| QT, (future SQ), and (future) CJQ values | Each participation's age-group category, for the purpose of medals |
| Consequence of not meeting qualification | Each participation's championship |
| Medals and team-point events | Each participation's championship |
| Team-point scale | Each participation's championship |

An athlete may therefore open under a Masters championship's 80% rule while
also participating in another championship with a different QT and a different
QT-not-met policy. This is intentional: the opening process occurs once, while
rankings and awards are computed independently for every participation.

## Behavioral Composition

Medals, team-point events, qualification consequences, entry-total enforcement,
and team-point scale are separate dimensions:

1. The entry-total rule validates the sum of opening declarations against the
   athlete's entry total.
2. Final SQ/CJQ/QT values determine whether each final event result reached its
   qualification threshold.
3. The QT-not-met policy determines whether a failed event keeps its rank and
   whether it remains eligible for a medal.
4. The medal policy determines which events award medals.
5. The team-points policy determines which event ranks contribute team points.
6. The team-point scale converts an included rank into points.

For example, a three-medal championship with Total-only team points can withhold
a Snatch medal below SQ while leaving team points unchanged, because Snatch
points are not enabled. If Total remains ranked under `RANKS_WITHOUT_MEDALS`, its
rank can still earn Total team points even though its Total medal is withheld.

## Implementation Impact

### Championship model and inheritance

Add stored championship fields for:

- qualification policy;
- entry-total rule;
- team-point scale.

Integrate all three with the existing competition-template inheritance model,
including smart getters, copying competition defaults, difference detection,
new-championship materialization, and default normalization.

NOTE on getters: Getters on JPA entities should be accessors because field access is used.  They can protect against nulled values and produce defaults, but nothing more complex.  Fields should not be read directly other than in the getters.  Computed values should be `computedX()` and if they need to be exposed as beans then the getter getX delegates to computedX, with getX marked as JSONignore.

### Ranking and medals

Replace global `Competition.isImwa()` branches in `AthleteSorter` with policy
lookups through the category participation's championship. Keep qualification
checks category-aware so an athlete can have different outcomes in different
championships.

Rank suppression must occur before team points are derived. Medal suppression
must use the same event qualification result but must not clear a rank under
`RANKS_WITHOUT_MEDALS`.

Audit athlete convenience methods that use the main participation directly.
Participation wrappers and championship-specific reports must resolve policy
from their own participation category and must inherit from the participation-specific championship.

### Entry-total validation

Replace global `Competition.isEnforce20kgRule()` and Masters inference with the
registration category championship's entry-total rule. All athlete-card,
weigh-in, lifting, and missing-kilogram notifications should continue to call a
single calculation method so they cannot disagree.

### Team points

Replace global IMWA team-scoring detection with the participation championship's
team-point scale. Preserve the existing distinction between:

- which event ranks count (`TeamPointsPolicy`);
- how included ranks convert to points (the new team-point scale);
- sum-of-points versus score-based team ranking (the existing team scoring
  system).

### User interface

Add the controls to `ChampionshipDetailsForm`. Because the competition defaults
editor uses the same form, the controls will also define defaults inherited by
championships that use competition defaults.

The Custom first/second/third fields are visible or enabled only when the
team-point scale is `CUSTOM`. All visible labels require translation keys through
the approved TSV translation workflow; `translation4.csv` must not be edited
directly.

### Import, export, and documentation

Update:

- the `Championships` worksheet reader and writer;
- JSON V2 `ChampionshipDTO` conversion;
- stored-field JSON export behavior;
- championship normalization and migration;
- age-group definition documentation;
- release notes when implemented.

Suggested worksheet/JSON field names are `qualificationPolicy`,
`entryTotalRule`, and `teamPointScale`.

## Legacy Migration

Suggested migration rules:

Legacy IMWA rules are applied to Masters when the global IMWA flag was present (IMWA_80_PERCENT, RANKS_WITHOUT_MEDALS, IMWA team points). Since there is no per-championship "Masters" marker, we must infer it based on the number of age groups and minimum age of the age groups).  Masters championships that are not IMWA follow the IWF rules (NO_RANKS_NO_MEDALS, 20_kg, IWF scoring).

The default settings for non Masters, non IMWA would be as follows.

| Legacy state | Championship setting |
| --- | --- |
| Entry-total enforcement disabled | `NONE`, otherwise `20_kg` |
| Standard 28/25/23 values | `IWF`, otherwise `CUSTOM` |

Legacy data does not identify Kids championships that should use `NONE`; those
must be configured explicitly after migration or identified by an explicit
migration rule supplied later.

After migration and compatibility support for older imports are complete, the
global IMWA, entry-total enforcement, and hidden Masters-20-kg fields can be
retired as sources of runtime behavior.

## Required Tests

- Each QT-not-met policy above, both below and exactly at QT.
- Qualification is applied on intermediate results -- no medals unless applicable QT is earned
- One athlete participating in championships with different QT values and
  qualification policies (check registration vs additional)
  - Registration championship selects 20 kg, IMWA 80%, or no entry-total rule.
  - Secondary participations do not change entry-total enforcement.
- All medal-policy and team-points-policy combinations, especially all-three
  medals with Total-only points.
- IWF, IMWA one-/two-/three-athlete category scoring, and Custom scales.
- Competition-default inheritance and championship override behavior.
- Legacy database, JSON V1/V2, and age-group workbook migration.