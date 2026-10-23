# Store Panel Decisions

Status: note, not started (2026-10-23).
Related: [seven-person-adjudication.md](seven-person-adjudication.md) (section 10,
"Data and reports").

## Problem

When a lift is reviewed (seven-person Adjudication Panel, or a 3-, 4- or
5-member jury voting alone), owlcms keeps only the outcome with the athlete.
The votes and the circumstances of the review are not stored, so the protocol
and other reports cannot show them, and the panel decision cannot be shown
again after the fact.

### What is stored today

On the athlete, per attempt
([Athlete.java](../owlcms/src/main/java/app/owlcms/data/athlete/Athlete.java)):

- the attempt result (good or failed weight), including a reversal;
- the referees' 3 initial decisions (`snatch1Decisions` … `cleanJerk3Decisions`);
- a jury deliberation result (`setJuryDeliberation`) or a challenge result
  (`setChallenge`), set by `FieldOfPlay.doJuryDecision`.

### What is not stored

- the jury votes on the lift (Phase A);
- the review vote: the referees' new votes and the jury votes;
- how the review started: automatic stop, Challenge, Deliberation;
- the technical rule (reason code) of a No Lift;
- who entered the decision: result of the vote, Jury Chair override ("Jury
  Chair Entered Decision"), Speaker as relayed by the Technical Controller;
- whether a vote result was cancelled and voted again.

### What the audit trail has

[FopAudit.java](../owlcms/src/main/java/app/owlcms/audit/FopAudit.java)
records each event with its station: `referee.vote` (each referee vote,
review votes included), `jury.vote` (each juror), `jury.decision` (decision
and reason code), `jury.summon`, breaks. The panel decision could be rebuilt
from it, but:

- Call for Votes (`PanelVoteCalled`), Cancel vote result (`PanelVoteCancelled`)
  and the No Lift rule request (`JuryRuleRequest`) have no named audit action;
- the tally and the "Jury Chair Entered" / "Speaker entered" markers are not
  recorded;
- the audit trail is not meant to feed the protocol.

## Proposal

One record per reviewed attempt, linked to the athlete and attempt number:

| Field | Content |
|---|---|
| trigger | automatic (panel or `automaticBreakNoVPT`), challenge, deliberation |
| jury size, VPT | as at the time of the review |
| initial referee votes | already on the athlete; repeated for convenience or referenced |
| jury votes on the lift | 3, 4 or 5 values |
| review referee votes | 3 values, seven-person panel only |
| review jury votes | 3, 4 or 5 values |
| result | good / no lift; reversal or confirmation of the initial decision |
| rule | reason code for a No Lift, null otherwise |
| entered by | vote result, Jury Chair override, Speaker |
| cancelled votes | count of Cancel vote result before the final vote |
| times | review start, vote called, decision |

## Open questions

- A separate entity (table) or fields on the athlete? A separate entity keeps
  the athlete table unchanged and allows several reviews of one attempt.
- Export and import (JSON V2 exports, database exports): include the new
  records.
- Reports: which templates show the panel votes and the rule (protocol, jury
  sheet)?
- Audit trail: add named actions for `PanelVoteCalled`, `PanelVoteCancelled`
  and `JuryRuleRequest`, and the markers in `jury.decision`.
- Tracker forwarding: send the stored record with the decision
  ([tracker-jury-votes.md](tracker-jury-votes.md)).
