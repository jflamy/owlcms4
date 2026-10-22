# TODO

Work order and decisions on the documents left in `compliance/` (completed and
superseded documents are in [done/](done/)). Confirmed decisions are marked;
the rest await confirmation. 2026-10-22.

## Next up

1. **DIRECT_JSON_BOOT Phase 1 — launch with a file**
   ([DIRECT_JSON_BOOT.md](DIRECT_JSON_BOOT.md), Phasing section).
   `OWLCMS_LOADJSON=<file or folder>` imports a V2/V1 JSON or ZIP at startup:
   §1 V2 error propagation, §2 ZIP input, §3 single-pass import behind the
   `EmbeddedJetty.run` seam (`Main.initFromImport(Path)`, folder helper with
   `candidates`/`latest`), §5 exit code 78 in `MainWrapper` and the control
   panel, §7 VS Code launch configs. Standby (Phase 2) is deliberately excluded.
   Unblocks launching owlcms from a JSON fixture for browser acceptance.
2. [BIRTHDATE_LENIENT_PARSE_SPEC.md](BIRTHDATE_LENIENT_PARSE_SPEC.md) — kept,
   low risk (see below).

## Confirmed

- [x] **Stale-save confirmation default button: Reload card**, not Overwrite.
  ([ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md) §9)
  Follow-up: `ConcurrentEditDialog.confirm` currently gives primary style and
  focus to "Keep editing" (`ConcurrentEdit.Back`); "Reload card" is "Discard my
  changes" (`ConcurrentEdit.Discard`), which should become the default.
- [x] **Medal and best-athlete ranks: current state accepted**; Parts 2 (scoreboard medal columns) and 3 (tracker) will not be done.
  ([done/medal-and-best-athlete-ranks.md](done/medal-and-best-athlete-ranks.md))
- [x] **Resource monitor: accepted as shipped**; no further runtime acceptance, no full-GC escalation.
  ([done/RESOURCE_MONITOR_SPEC.md](done/RESOURCE_MONITOR_SPEC.md))
- [x] **No presence tracking / warning-on-open** for athlete and registration
  cards; the save-time check is the retained protection.
  ([done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](done/ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md))

## Keep or drop (document level)

Each remaining document is either backlog or will never be done.

- [ ] [PLAN-agegroup-edit-confirmation.md](../PLAN-agegroup-edit-confirmation.md) — age-group edit confirmation and safe category save (not started)
- [ ] [PLAN-scoped-dark-dialogs.md](../PLAN-scoped-dark-dialogs.md) — scoped dark theme on display dialogs (not started)
- [x] **Keep** — [BIRTHDATE_LENIENT_PARSE_SPEC.md](BIRTHDATE_LENIENT_PARSE_SPEC.md) — file-wide DMY/MDY detection on registration import (not started). Low risk; constraint: must not break any registration sheet that imports correctly today (ISO and Excel date cells keep their current path).
- [ ] [MEDAL_ELIGIBILITY.md](MEDAL_ELIGIBILITY.md) — per-championship QT-not-met policy, entry-total rule, team-point scale (medal and team-points policies done; the three new settings still global)
- [x] **Keep, Phase 1 first** — [DIRECT_JSON_BOOT.md](DIRECT_JSON_BOOT.md) — `OWLCMS_LOADJSON` / `OWLCMS_STANDBY` startup import (not started). Phase 1 (launch with a file) is next up; Phase 2 (standby) later. Also unblocks automated browser acceptance (launch owlcms from a JSON fixture with accounts and switches), e.g. the Competition Director checks
- [ ] [SCHEDULED_CHECKPOINTS.md](SCHEDULED_CHECKPOINTS.md) — tracker plugin 5-minute checkpoints, Path A1 (decided, not started)
- [ ] [EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md](EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md) — encrypted forwarding keys in exports, export endpoint authorization (proposal, not started)
- [ ] [personal-best-records.md](personal-best-records.md) — Personal records as a record federation in owlcms (tracker side done, owlcms not started)
- [ ] [tracker-jury-votes.md](tracker-jury-votes.md) — seven-person votes in the tracker decision block (proposal, not started)
- [ ] [ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md) — one-off opener gating, break capability, non-Vaadin entry-point review
- [ ] [COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md](COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md) — browser acceptance as a `playwright`-module check (after DIRECT_JSON_BOOT), config-tab permissions
- [ ] [seven-person-adjudication.md](seven-person-adjudication.md) — feature done; left: Jury/Keypads/MQTT docs, `MQTTMonitor` tests, device tests and `panelVoteDeviceIndicators` default, four unused translation keys
- [ ] [ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md](ATHLETE_CARD_CONCURRENT_EDITING_ANALYSIS.md) — §6.4 write-only-changed-fields follow-up, `BreakManagement` unregistration

## Decisions inside the documents

### Age-group editing ([PLAN-agegroup-edit-confirmation.md](../PLAN-agegroup-edit-confirmation.md) §8)

- [ ] When an athlete gains eligibility, preserve a valid current registration or re-infer it?
- [ ] How do missing age/bodyweight and manual registration choices affect reassignment?
- [ ] "Reassign later": persist/show pending-reassignment indicators, or just keep the per-group and global actions available?
- [ ] Confirmation Cancel: return to the populated editor, or discard and close?
- [ ] During save: which controls/dialogs stay visible and disabled; how are failure and retry presented?
- [ ] Active checkbox: show the proposed or the original value while a decision is pending?

### Scoped dark dialogs ([PLAN-scoped-dark-dialogs.md](../PLAN-scoped-dark-dialogs.md))

- [ ] Give display-page `Notification`s component-level dark styling, or accept that they render light?
- [ ] Delete the unused `.referee-jury-home-displays` CSS block?

### Birth-date lenient parsing ([BIRTHDATE_LENIENT_PARSE_SPEC.md](BIRTHDATE_LENIENT_PARSE_SPEC.md))

- [ ] Invalid calendar dates (month 15, Feb 30): reject the row or the whole sheet?

### Championship qualification policies ([MEDAL_ELIGIBILITY.md](MEDAL_ELIGIBILITY.md), Legacy Migration)

- [ ] How to identify Masters championships in legacy data (inferred from age-group count and minimum age, as suggested)?
- [ ] How to identify Kids championships that should get entry rule `NONE` (explicit configuration after migration, or a migration rule)?
- [ ] Confirm the non-Masters, non-IMWA defaults: entry-total enforcement off → `NONE` else `20_kg`; standard 28/25/23 → `IWF` else `CUSTOM`.

### Direct JSON boot ([DIRECT_JSON_BOOT.md](DIRECT_JSON_BOOT.md) §5)

- [ ] Terminal exit code on import failure: 78 as proposed? (`MainWrapper` and the control panel must stop restarting on that code.)

### Export protection ([EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md](EXTERNAL_CONNECTION_EXPORT_PROTECTION_PROPOSAL.md) §1)

- [ ] Adopt the four recommendations (keep connection info in exports; encrypt newly entered forwarding keys on save; always protect keys at export; accept unmarked plaintext on import)?
- [ ] Apply ECX-20 (socket-peer / trusted-proxy authorization on `/competition/export*` and `/competition/h2`)? This is also the open question of ACCESS_CONTROL §13.1.

### Access control ([ACCESS_CONTROL_DESIGN.md](ACCESS_CONTROL_DESIGN.md) §13.1)

- [ ] Do roles apply to the export and H2 backup servlets, or does the IP/backdoor rule stay?
- [ ] When may `X-Forwarded-For` be trusted (behind a known proxy only)?
- [ ] Is the `CorsFilter` cross-site session cookie acceptable now that accounts exist?

### Tracker jury votes ([tracker-jury-votes.md](tracker-jury-votes.md) §0.9, §9)

- [ ] Option A (raw events) or Option B (forward rendered `DecisionBlockState`)?
- [ ] Deliberation without a panel pattern: no jury circles, or honour `DECISION_SECTION_SHOW_BOTH_JURY_VOTES`? (proposed: honour the switch)
- [ ] Athlete under review: replace the break message with `fullName`, or keep the break message and add the name to the decision label? (proposed: keep the break message)
- [ ] Include the HTTP forwarder / publicresults payload in the same change? (proposed: yes)
- [ ] Forwarder keeps a `lastCells` snapshot for `JURY_DECISION`? (proposed: yes)
- [ ] Clear revealed cells at `RESET` or keep until `StartLifting`? (proposed: clear at `RESET`)
- [ ] Option B only: drop `JURY_UPDATE` / `PANEL_VOTE` in favour of one `DECISION_BLOCK` type; add the athlete key to `scoreboardDecision`; confirm canonical attempt-board defaults.

### Competition Director ([COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md](COMPETITION_DIRECTOR_PAGE_IMPLEMENTATION_PLAN.md))

- [ ] Scope of the partial configuration-tab permissions for the Competition Director.
