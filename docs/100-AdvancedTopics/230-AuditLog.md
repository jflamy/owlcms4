# Audit Log Format and Analysis

OWLCMS writes an audit trail of everything that happens during a competition: clock starts and stops, referee votes, lift results, weight changes, jury decisions, breaks, record attempts, logins. The records are written to text files that can be read by people, and that also contain a JSON copy of each record, so they can be analyzed with tools such as `jq`, Python or a spreadsheet.

This page describes the format so that a person, or an AI agent, can write queries such as:

- referee decisions, and how often each referee is in the minority;
- jury decisions and reversals;
- average time between lifts, excluding breaks;
- whether athletes who change their weight several times, or late, succeed more or less often.

The page has two parts:

- **Audit Log Format**: where the files are, the structure of a line, the JSON record, the actions, stations and causes, and the things to know before analyzing.
- **Example Queries**: `jq` commands that extract the records and answer the questions above. Their output can be saved as CSV for further analysis.

## Audit Log Format

### Where the files are

The files are in the `logs/audit` directory of the OWLCMS installation. There is one stream per platform, plus a `competition` stream for actions not tied to a platform (application start, logins, exports).

```
logs/audit/A_sealed_2026-11-16T23-02-45.786Z.log      platform A, readable columns + JSON, sealed
logs/audit/A_plain_2026-11-16T23-02-45.786Z.log       platform A, readable columns only
logs/audit/competition_sealed_2026-11-16T23-02-29.975Z.log
```

- **Use the `_sealed_` file for analysis.** It is the only one with the JSON. The `_plain_` file has the same records without the JSON, and without the `audit.open` header line, for reading.
- A new file is started at every OWLCMS start, and when a file reaches 10 MB. The time in the file name is UTC and sorts correctly, so `A_sealed_*.log` lists a platform's files in order.
- The sequence number restarts at 1 in every file. Use the timestamp, not the sequence, to order records across files.

### Line format

Each audit record is one line. The sealed file also contains cryptographic control lines that must be skipped.

```
   24 | 18:02:55.199 | REFEREES     | athlete.lift        | LARIVIÈRE Martine    | F1/H1    | SN1 | actualLift - -> 35, clock=0:59 (58128ms) | json={...}
SEAL {"applicationVersion":"69.0.0",...,"marker":"SEAL",...}
```

| Line starts with | Meaning | What to do |
|---|---|---|
| spaces and a number, then ` \| ` | an audit record | keep it, read the JSON after ` \| json=` |
| `FIRST`, `CONTINUE`, `SEAL`, `FINAL` | integrity control line (signature of the preceding block) | skip it |

The readable columns, separated by ` | `, are:

| # | Column | Example | Meaning |
|---|---|---|---|
| 1 | sequence | `24` | position in this file |
| 2 | time | `18:02:55.199` | local time of day |
| 3 | station | `REFEREES`, `REFEREE#2` | who acted (see Stations below); `#n` is the referee or jury member number |
| 4 | action | `athlete.lift` | what happened (see Actions below) |
| 5 | athlete | `LARIVIÈRE Martine` | athlete concerned, `-` if none |
| 6 | session | `F1/H1` | session the record belongs to, `-` if none |
| 7 | attempt | `SN1` … `SN3`, `CJ1` … `CJ3` | attempt concerned, `-` if none |
| 8 | what | `actualLift - -> 35, clock=0:59 (58128ms)` | the change (`before -> after`) and details, for reading |
| 9 | json | `json={...}` | the full record, for programs |

The readable columns escape any `|` in names or values as `\|`, so the first ` | json=` on a line is always the start of the JSON.

### The JSON record

Every record is self-contained:

```json
{
  "schemaVersion": 2,
  "sequence": 24,
  "timestamp": "2026-11-16T18:02:55.199-05:00",
  "platform": "A",
  "actor": {"mode": "PIN", "user": "-", "station": "REFEREES", "index": null,
            "inferred": false, "client": "-", "device": "refbox"},
  "action": "athlete.lift",
  "athlete": {"id": "1881070649266242034", "name": "LARIVIÈRE Martine"},
  "session": "F1/H1",
  "attempt": "SN1",
  "change": {"field": "snatch1.actualLift", "before": "", "after": "35",
             "priorRequested": null, "previousAttemptRequested": null},
  "cause": "decision [true, true, true]",
  "details": {"clockMillis": 58128}
}
```

| Field | Meaning |
|---|---|
| `timestamp` | ISO 8601 with milliseconds and the local UTC offset |
| `platform` | platform name, or `competition` |
| `actor.station` | the station (column 3); `actor.index` is the referee or jury member number |
| `actor.user` | the account name when accounts are used, `-` with PINs |
| `actor.mode` | `ACCOUNTS` when the user signed in with an account or a token, `PIN` otherwise |
| `actor.device` | `refbox`, `jurybox`, `clock` (MQTT devices), `simulator`, `token id=…`, or `-` for a page |
| `actor.client` | for an action on a page, the browser's IP address and a short session tag (`192.168.1.20#6afb`), so actions from the same browser can be grouped; `-` for devices and automatic actions |
| `actor.inferred` | true when the station was deduced from the page or event the action came from |
| `action` | see Actions below |
| `athlete.id` | stable athlete identifier; **use it, not the name, to group by athlete** |
| `session` | the session the record belongs to: the athlete's session, or for a record without an athlete the session loaded on the platform; null for records not tied to a session (logins, exports). **Use it to group by session** |
| `attempt` | `SN1`–`SN3`, `CJ1`–`CJ3`, or null |
| `change` | null, or the field that changed with its `before` and `after` values |
| `cause` | why the record was written (see below); null for direct user actions |
| `details` | action-specific values; times are in milliseconds (`clockMillis`, `remainingMillis`) |

### Actions

#### Lifting

| Action | Station | Meaning |
|---|---|---|
| `athlete.lift` | `REFEREES` (or `ANNOUNCER`); the jury's station for a reversal | result of an attempt: `change.field` is `snatch1.actualLift` … `cleanJerk3.actualLift`; `change.after` is the weight, **positive for a good lift, negative for a no lift**, `"0"` for an attempt not taken |
| `athlete.progression` | same station as the lift result | automatic weight for the next attempt after a result (`snatchN.automatic`, `cleanJerkN.automatic`) |
| `athlete.change` | `MARSHAL`, `ANNOUNCER`, … | any other athlete change. Weight requests have `change.field` ending in `.declaration`, `.change1` or `.change2`; `change.after` is the requested weight and `change.priorRequested` the weight requested before |
| `referee.vote` | `REFEREE` with `index` 1–3 | one referee's vote; `details.decision` is true (good) or false (no lift) |
| `referee.decision` | `REFEREES` | full decision received from a device, `details.lights` |
| `referee.down` | | down signal |
| `clock.start`, `clock.stop` | `TIMEKEEPER` (or `ANNOUNCER`, `SUPERVISOR`) | athlete clock; `details.clockMillis` is the time remaining. `details.override` is true when the clock was started although the jury had not finished voting, and its incomplete votes were abandoned: **Start clock anyway**, or Start pressed again at least one second after the refused start (then `details.repeatedStart` is also true) |
| `clock.set`, `clock.timeover` | | clock forced to a value, clock reached zero |

The `attempt` of an `athlete.lift` is the attempt that was decided. The `attempt` of an `athlete.progression` or weight request is the attempt the weight is for. For clock records and referee votes before the decision, it is the current athlete's attempt. Referee and jury records about a lift that has been decided (jury votes and decisions, votes during a review, a decision applied at once) name the athlete and attempt under review.

#### Jury

| Action | Station | Meaning |
|---|---|---|
| `jury.vote` | `JURY_MEMBER` with `index` | one jury member's vote, `details.decision` |
| `jury.summon` | `JURY_CONTROL` or `JURY_BOX` | the referees are called: `details.referee` is `0` for all of them (during a review with the seven-person panel, or a jury using the automatic stop option, this calls the review vote), `1`–`3` for one referee, `4` for the technical controller |
| `jury.result` | `SYSTEM` | the result of a review vote, written when the last vote is in (see below) |
| `jury.resume` | `JURY_CONTROL` or `JURY_BOX` | Resume was pressed |
| `jury.decision` | who entered it: `JURY_CONTROL` (the Jury Chair on Jury Control), `JURY_BOX` (the jury box buttons) or `ANNOUNCER` (the Speaker) | the jury's decision: `details.decision` true (good lift) or false (no lift); `details.reason` is `0` when the referees' decision stands as called, `1`–`17` for the technical rule given for a No Lift, and null when no rule was given (a Good Lift, or a No Lift from the jury box or the Speaker) |

A lift may get two rounds of `jury.vote` records. The jurors first vote on the lift as it is decided. A review (a `break.start` of type `JURY` or `CHALLENGE`) then opens a review vote: the jurors vote again and, with the seven-person panel, so do the referees (`referee.vote` records during the break, after a `jury.summon` with `referee` 0).

`jury.result` gives the outcome of the review vote: `decision`, `standsAsCalled` (the vote did not reach the jury's rule, so the referees' decision stands), `procedure` (`PANEL` for the seven-person panel, `JURY` otherwise), the `jury` votes and, for the panel, the `referees` votes and the `whites` and `reds` counts. `vote` is 1 for the first vote on the lift; when the Jury Chair cancels the result and the vote is taken again, the next `jury.result` has `vote` 2, and so on. The vote result is not the decision: the `jury.decision` that follows is.

When the jury **reverses** a decision, the athlete's result is rewritten just before the `jury.decision` record: an `athlete.lift` for the same attempt with `cause` = `"JuryDecision"`, a non-empty `change.before` (the referee result) and the new `change.after`. The next automatic progression is also rewritten (`athlete.progression` with the same cause), except after a third attempt. A jury confirmation does not change the result and writes no `athlete.lift`.

A review ended with Resume has a `jury.resume` and no `jury.decision`.

#### Session flow

| Action | Meaning |
|---|---|
| `session.load` | a session is loaded on the platform, `details.session`; `cause` is `URL` when a page was opened with the session in its address |
| `summary.session`, `summary.athlete` | snapshot of the session and of each athlete (category, body weight, start number, lot, opening weights), written when lifting starts in a session |
| `lifting.start` | lifting begins in the session, or resumes after a break (station `SYSTEM`, for example after a jury decision) |
| `break.start`, `break.pause`, `break.end` | breaks; `details.type` (for example `FIRST_CJ`, `TECHNICAL`, `JURY`, `CHALLENGE`), `details.remainingMillis`. A break ends with `break.end` or with the next `lifting.start` |
| `ceremony.start`, `ceremony.end` | medal and introduction ceremonies |

There is no "session end" record. A session's lifting has two parts:

- the **snatch** part, from the first snatch lift (the first `clock.start` on a snatch attempt) to the last snatch decision;
- the **clean & jerk** part, from the first clean & jerk lift to the last clean & jerk decision, which is the end of the session.

The time between the two parts is the break. A decision is the referees' `athlete.lift`, or a `jury.decision` when the jury ruled on the attempt; when there are several (a challenge, a vote taken again), the last one in sequence ends the part. An OWLCMS restart does not interrupt the session: the lifting time is the time of the lifts. Technical breaks are not deducted; they are rare enough not to matter for statistics.

#### Records and other actions

`record.challenge`, `record.new` and `record.cancelled` concern record attempts (`change.before` is the current record, `change.after` the weight). Other actions include `auth.login`, `auth.logout`, `export.json`, `application.started`, `application.stopping`, `agegroup.*`, `athlete.delete` and `audit.open` (the first line of each file).

An action refused by the field of play (for example a clock start in the wrong state) has `.refused` appended, such as `clock.start.refused`, and `details.state` gives the state it was refused in. Exclude these when measuring timings. A clock start refused because jury votes are missing is followed, when an operator confirms **Start clock anyway** or presses Start again, by a `clock.start` with `details.override`; its station is the console or device that started the clock, which may differ from the one that was refused. A second Start less than one second after the refusal is refused as a reflex.

### Stations

`ANNOUNCER` (active and passive announcer pages), `SUPERVISOR` (the Competition Supervisor page), `MARSHAL`, `TIMEKEEPER`, `TC`, `REFEREE#1`–`#3` (individual votes), `REFEREES` (the decision as a whole), `JURY_MEMBER#n` (jury votes; `JURY_MEMBER` without a number is the jury mobile page), `JURY_CONTROL` (the Jury Control page), `JURY_BOX` (the jury box buttons), `WEIGHIN`, `REGISTRATION`, `RESULTS`, `MEDALS`, `ADMIN`, `PREPARATION` (other preparation pages), `TESTING`, `SIMULATION`, `MQTT` (a rejected device message), `SYSTEM` (automatic actions such as a break started by the program), `LOGIN`, `HTTP`, and `UNKNOWN` when the station cannot be determined.

### Causes

| `cause` | Meaning |
|---|---|
| `decision [true, false, true]` | the record results from the referee decision. The list repeats the referees' votes (the `referee.vote` records written just before) as they stood when the decision was made: referees 1, 2 and 3, `true` white, `false` red, `null` if a light is missing |
| `JuryDecision` | the record results from a jury decision (reversal) |
| `SwitchGroup` | written while loading a session, for example attempts set to 0 for a withdrawn athlete |
| `TimeStarted`, `TimeStopped`, `DecisionUpdate`, `BreakDone`, `WeightChange`, `DecisionReset`, … | the field-of-play event that produced the record |

### Things to know before analyzing

- **Lift results**: keep `athlete.lift` records whose `cause` starts with `decision ` (the referees' result) or equals `JuryDecision` (a jury reversal). This excludes withdrawals (`"0"` with cause `SwitchGroup`). The last one for an athlete and attempt is the final result.
- **Good or no lift**: the sign of `change.after`. Weights are strings; convert with `tonumber`.
- **Weight requests** for an attempt are the `athlete.change` records whose `change.field` ends in `.declaration`, `.change1` or `.change2`, grouped by `athlete.id` and `attempt`. Their timestamps give the time of each request. An athlete has at most one declaration and two changes per attempt.
- **`details.clockMillis` on a weight request is the platform clock** when the change was saved. It belongs to the athlete who is currently lifting, which may be another athlete. A value of 60000 or 120000 usually means the clock was not running.
- **Timestamps**: compute differences in milliseconds. `jq`'s `fromdateiso8601` does not accept milliseconds or offsets, so cut the string as in the `ms` helper below.
- **Sessions**: group by the `session` field; never deduce the session from the order of the records. A jury decision on the last lift of a session can come after the next session was loaded, and it still carries its own session. See Session flow for where the snatch part, the break and the clean & jerk part start and end.
- **Older files**: records with `schemaVersion` 1 have no `session` field and no session column.

## Example Queries

The examples use [jq](https://jqlang.org). The first two steps prepare the data; the other examples use the result.

### Extracting the records

Convert a sealed file into one JSON record per line (JSON Lines). All the following examples read `A.jsonl`:

```bash
jq -cR 'select(test("^ *[0-9]+ \\| ")) | sub("^.*? \\| json="; "") | fromjson' A_sealed_*.log > A.jsonl
```

- `-R` reads each line as a string; `select(test(...))` keeps the record lines and skips the control lines;
- `sub(...)` removes everything up to ` | json=`; `fromjson` parses the rest.

A platform's log is usually split into several files: a new one is started at every OWLCMS start and when a file reaches 10 MB. Give them all at once, as above: the shell expands `A_sealed_*.log` in name order, which is time order, so `A.jsonl` holds the whole competition in order. The queries below do not use `sequence`, which restarts in every file.

### Helper functions

Save this as `audit.jq` in the current directory, and add `-L . 'include "audit"; ...'` to use it:

```jq
# milliseconds since epoch, from the local wall-clock time; correct for differences within one day
def ms: (.timestamp[0:19] + "Z" | fromdateiso8601) * 1000 + (.timestamp[20:23] | tonumber);
# athlete and attempt, to group or join records about the same attempt
def key: "\(.athlete.id)/\(.attempt)";
# result given by the referees on the platform
def decided: .action == "athlete.lift" and (.cause // "" | startswith("decision "));
# final result, including jury reversals
def result: .action == "athlete.lift" and ((.cause // "" | startswith("decision ")) or .cause == "JuryDecision");
# referee lights of a decided lift, e.g. [true,false,true]
def lights: .cause | ltrimstr("decision ") | fromjson;
def weight: .change.after | tonumber | fabs;
def good: (.change.after | tonumber) > 0;
# declaration or weight change
def request: .action == "athlete.change" and (.change.field | test("\\.(declaration|change1|change2)$"));
```

### Split decisions

Lifts where the referees did not agree, for a referee or jury review. `lights` are the votes of referees 1, 2 and 3 as they stood at the decision, `true` for white and `false` for red.

```bash
jq -c -L . 'include "audit"; select(decided) | select(lights | unique | length > 1)
  | {athlete: .athlete.name, attempt, weight: weight, good: good, lights: lights, at: .timestamp[11:19]}' A.jsonl
```
```
{"athlete":"HARVEY Christine","attempt":"SN1","weight":36,"good":true,"lights":[true,false,true],"at":"18:03:06"}
{"athlete":"LARIVIÈRE Martine","attempt":"SN2","weight":38,"good":false,"lights":[false,true,false],"at":"18:03:32"}
```

### Referee voting time

Seconds between the timekeeper stopping the clock and each referee's first vote, averaged per referee. A referee who changes a vote has several `referee.vote` records for the same attempt; the first one is used.

```bash
jq -s -L . 'include "audit";
  [ map(select(.action == "clock.stop" or .action == "referee.vote")) | group_by(key)[]
    | sort_by(ms)
    | (map(select(.action == "referee.vote")) | group_by(.actor.index) | map(.[0])) as $first
    | select($first | length > 0)
    | (($first | map(ms) | min) as $v | map(select(.action == "clock.stop" and ms < $v)) | last) as $stop
    | select($stop)
    | $first[] | {referee: .actor.index, seconds: ((ms - ($stop | ms)) / 1000)} ]
  | group_by(.referee)
  | map({referee: .[0].referee, votes: length, avgSeconds: (map(.seconds) | add / length), max: (map(.seconds) | max)})' A.jsonl
```

### How often each referee is in the minority

```bash
jq -s -L . 'include "audit";
  [ .[] | select(decided) | good as $g | lights | to_entries[] | select(.value != null)
    | {referee: (.key + 1), minority: (.value != $g)} ]
  | group_by(.referee)
  | map({referee: .[0].referee, votes: length, minority: map(select(.minority)) | length})' A.jsonl
```

### Jury decisions and reversals

```bash
# every jury decision, with the reason code
jq -c 'select(.action == "jury.decision")
  | {athlete: .athlete.name, attempt, good: .details.decision, reason: .details.reason, at: .timestamp}' A.jsonl

# reversals: the result before and after the jury
jq -c 'select(.action == "athlete.lift" and .cause == "JuryDecision")
  | {athlete: .athlete.name, attempt, referees: .change.before, jury: .change.after}' A.jsonl
```

### Time per lift, by session

For each session, the duration of the snatch part and of the clean & jerk part, the number of lifts and the average seconds per lift, and the length of the break between the parts. Save this as `sessions.jq` next to `audit.jq`:

```jq
include "audit";
# a decision on an attempt: the referees' result or the jury's decision
def decision: decided or .action == "jury.decision";
def part($rows; $prefix):
  ($rows | map(select(.action == "clock.start" and (.attempt // "" | startswith($prefix)))) | map(ms) | min) as $from
  | ($rows | map(select(decision and (.attempt // "" | startswith($prefix)))) | map(ms) | max) as $end
  | ($rows | map(select(decided and (.attempt | startswith($prefix)))) | length) as $lifts
  | if $from == null or $end == null or $lifts == 0 then null
    else {from: $from, end: $end, lifts: $lifts, minutes: (($end - $from) / 60000),
          secondsPerLift: (($end - $from) / 1000 / $lifts)} end;
map(select(.session != null)) | group_by(.session)
| map(part(.; "SN") as $sn | part(.; "CJ") as $cj
    | {session: .[0].session,
       snatch: ($sn | if . then del(.from, .end) else null end),
       cleanJerk: ($cj | if . then del(.from, .end) else null end),
       breakMinutes: (if $sn and $cj then ($cj.from - $sn.end) / 60000 else null end)})
```

```bash
jq -s -L . -f sessions.jq A.jsonl
```
```
[
  {
    "session": "F1/H1",
    "snatch": {"lifts": 33, "minutes": 6.97, "secondsPerLift": 12.68},
    "cleanJerk": {"lifts": 33, "minutes": 6.85, "secondsPerLift": 12.45},
    "breakMinutes": 0.24
  },
  ...
]
```

Records are grouped by their `session` field, so a jury decision on the last lift is counted in its own session even if the next session was already loaded. Each part ends at its last decision in sequence, which takes challenges and votes taken again into account. A part with no decision in the log (for example a session started directly in the clean & jerk) is null. Files written before schema version 2 have no `session` field and are not covered by this query.

### Weight changes per attempt, with the outcome

One row per attempt that had requests: number of requests, seconds between the first and the last, and whether the lift was good. This is the starting point for questions such as "does a longer delay between the first and last change give more good or bad lifts":

```bash
jq -s -c -L . 'include "audit";
  (map(select(result)) | INDEX(key)) as $result
  | map(select(request)) | group_by(key)
  | map({athlete: .[0].athlete.name, attempt: .[0].attempt, changes: length,
         spanSeconds: ((.[-1] | ms) - (.[0] | ms)) / 1000,
         good: ($result[.[0] | key] | if . then good else null end)})
  | .[]' A.jsonl
```

`INDEX(key)` keeps the last result for each attempt, so jury reversals are taken into account. `good` is null when the attempt was not taken.

Success rate by number of changes:

```bash
jq -s -L . 'include "audit";
  (map(select(result)) | INDEX(key)) as $result
  | map(select(request)) | group_by(key)
  | map({changes: length, good: ($result[.[0] | key] | if . then good else null end)})
  | map(select(.good != null)) | group_by(.changes)
  | map({changes: .[0].changes, attempts: length, goodRate: (map(select(.good)) | length) / length})' A.jsonl
```

### Exporting to CSV for further analysis

Any of the row-producing queries can end with `@csv` to load the data in a spreadsheet, Python or R:

```bash
jq -r -L . 'include "audit"; select(decided)
  | [.athlete.id, .athlete.name, .attempt, weight, good, .timestamp] | @csv' A.jsonl > lifts.csv
```
