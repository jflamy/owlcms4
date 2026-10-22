# Competition Director and Passive Speaker — Remaining Acceptance

Status: implemented; focused Maven verification passes; translation keys are in
`translation4.csv`. The implementation plan is archived in
[done/COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md](done/COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md)
(analysis in
[done/COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md](done/COMPETITION_DIRECTOR_PASSIVE_ANNOUNCER_ANALYSIS.md)).

Still to do:

1. The authenticated browser acceptance checks below. Intended vehicle: a
   check in the `playwright` module (Java Playwright, like `UpdateCheck`)
   against a separately launched owlcms. Blocked on
   [DIRECT_JSON_BOOT.md](DIRECT_JSON_BOOT.md): with `OWLCMS_LOADJSON` pointing
   at a V2 export fixture that holds the access mode, feature switches and the
   announcer / Competition Director accounts, each scenario starts from one
   launch and no database preparation. Until then there is no clean way to
   seed accounts-mode state for an automated run.
2. Partial configuration-tab permissions for the Competition Director (separate
   work, scope not defined).

## Browser acceptance

Pages under test: **PIN speaker** (`lifting/announcer`, PIN mode — active),
**passive speaker** (`lifting/announcer/passive`, accounts-mode default for an
announcer account) and **Competition Director** (`lifting/competition-director`,
accounts mode or PIN with the feature on). Everything that works today on the
PIN speaker must keep working there; the checks below say which page each
applies to.

### Navigation and defaults

| # | Check | Page |
| --- | --- | --- |
| 1 | Run Session and the announcer landing page open the **active** speaker in PIN mode and the **passive** speaker in accounts mode | PIN speaker, passive speaker |
| 2 | Competition Director is off by default in PIN mode, appears when the Features checkbox is ticked (or the startup override is set); in accounts mode it is available to Competition Director and Admin accounts regardless of the toggle | Competition Director |

### Same presentation on all three

| # | Check | Page |
| --- | --- | --- |
| 3 | Athlete header, timer, decision lights, lifting-order grid and notifications are identical on the three pages | all three |
| 7 | Every notification is dismissible and the jury dialogs keep their actions (jury interaction is allowed even on the passive page) | all three |

### Passive speaker is read-only

| # | Check | Page |
| --- | --- | --- |
| 4 | Clock, decision, session selection, introduction, reload, athlete editing and attempt reversal stay hidden or disabled after start, stop, break, session change and lifting-order updates | passive speaker |
| 5 | Red pause indicator is visible but cannot be clicked | passive speaker |
| 6 | Row clicks, attempt clicks and the active-page keyboard shortcuts cannot operate the competition | passive speaker |
| 8 | Live-light cogwheel setting changes only the passive presentation; it does not write the single-referee setting to the platform | passive speaker |

### Active pages keep every control

| # | Check | Page |
| --- | --- | --- |
| 9a | All speaker controls, shortcuts and the pause button work as before | PIN speaker |
| 9b | Cogwheel **switch to passive / switch to active** takes effect at once (full-page navigation, no manual refresh) and preserves platform, session, sound, single-referee, live-light, declaration and notification-position parameters | PIN speaker (both directions), passive speaker |
| 10 | All speaker controls work; the cogwheel has **no** passive-mode command | Competition Director |
