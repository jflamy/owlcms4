# UpdateCheck Attempt Board Monitoring Plan

## Objective

Extend the Playwright `UpdateCheck` harness so it can monitor:

- announcer board only
- attempt board only
- both announcer and attempt board at the same time

The harness should reuse the common MQTT expected-event pipeline and use a shared timer/clock reader where possible.

## Relevant Files

- `playwright/src/main/java/playwright/UpdateCheck.java`
- `playwright/src/main/java/playwright/MonitoredPlatform.java`
- `playwright/src/main/resources/logback.xml`
- `playwright/scripts/run-update-check.sh`
- `playwright/scripts/confirm-clock-logs.py`
- `playwright/scripts/locate-clock-mistakes.py`
- `.vscode/launch.json`
- `owlcms/src/main/java/app/owlcms/monitors/MQTTMonitor.java`
- `owlcms/src/main/java/app/owlcms/displays/attemptboard/AbstractAttemptBoard.java`
- `owlcms/src/main/frontend/components/AttemptBoard.js`
- `owlcms/src/main/frontend/components/TimerElement.js`

## Constraints

- Keep existing announcer-only behavior as the default.
- Prefer environment variables because `.vscode/launch.json` already controls `UpdateCheck` that way.
- Do not add per-second server traffic for the attempt board.
- Keep MQTT input shared per FOP, but each monitored page must have independent `lastSequence` and verification state.
- Do not run Maven/builds without explicit consent. After Java edits, use `get_errors` on touched files.

## Phase 1: Add Board Selection To Config

In `UpdateCheck.java`:

1. Add a board role enum, probably nested:

```java
enum BoardRole {
    ANNOUNCER,
    ATTEMPT
}
```

2. Add defaults:

```java
private static final String DEFAULT_ATTEMPT_BOARD_PATH = "/displays/attemptBoard";
private static final String DEFAULT_BOARDS = "announcer";
```

3. Extend `Config`:

```java
record Config(
    String baseUrl,
    String mqttUri,
    List<String> fops,
    String announcerPath,
    String attemptBoardPath,
    List<BoardRole> boards,
    Duration timeout,
    boolean headless,
    boolean publish,
    boolean publishDown,
    String decisionWord,
    long decisionSpacingMillis
)
```

4. Parse both CLI args and env vars:

```text
--boards=announcer,attempt
--attemptBoardPath=/displays/attemptBoard
OWLCMS_PLAYWRIGHT_BOARDS=announcer,attempt
OWLCMS_ATTEMPT_BOARD_PATH=/displays/attemptBoard
```

Use existing precedence style: CLI arg first, env var second, default third.

5. Log the new values in `main`:

```text
Boards: announcer,attempt
Announcer path: /lifting/announcer
Attempt board path: /displays/attemptBoard
```

## Phase 2: Split Page Monitoring From Platform Monitoring

Currently `MonitoredPlatform.java` is really "one announcer page for one FOP." Refactor it into:

```text
MonitoredPlatform
  fop
  browser
  context
  List<MonitoredPage>

MonitoredPage
  fop
  role
  page
  latestEvent / lastSequence / eventSignal
  watcherThread
  stalled / stallReason
  SnapshotReader
  DisplayMatcher
```

Implementation approach:

1. Keep `MonitoredPlatform.open(...)` as the FOP-level factory.
2. Launch one browser per FOP, one browser context, and one page per selected board role.
3. For each role:
   - `ANNOUNCER` opens `config.announcerPath()`
   - `ATTEMPT` opens `config.attemptBoardPath()`
4. `MonitoredPlatform.acceptEvent(event)` should fan out to every `MonitoredPage`.
5. `MonitoredPlatform.startWatching(...)` starts every page watcher.
6. `MonitoredPlatform.stopWatching(...)` stops every page watcher.
7. `MonitoredPlatform.stalled()` returns true if any page stalled.
8. `MonitoredPlatform.stallReason()` should include role, for example:

```text
RED attempt page stalled after playwright expected display
```

Do not let one page's successful verification advance another page's sequence. That state belongs inside `MonitoredPage`.

## Phase 3: Introduce Snapshot Readers

Create a small interface in the `playwright` package, either standalone or nested if the implementation should stay minimal:

```java
interface SnapshotReader {
    UpdateCheck.SnapshotRead read(MonitoredPage page);
    void waitForReady(Page page, Duration timeout);
}
```

Then implement:

```text
AnnouncerSnapshotReader
AttemptBoardSnapshotReader
```

The existing `UpdateCheck.readSnapshotRead(MonitoredPlatform platform)` logic becomes the announcer reader.

For the attempt board reader:

1. Use Playwright `page.evaluate(...)`.
2. Read inside `attempt-board-template` shadow DOM.
3. Extract:
   - last name text
   - first name text
   - start number text
   - attempt text
   - weight text
   - athlete timer snapshot

Suggested JS shape:

```javascript
() => {
  const board = document.querySelector('attempt-board-template');
  const root = board?.shadowRoot;
  if (!root) return null;

  const text = (selector) => root.querySelector(selector)?.innerText?.trim() || '';

  const lastName = text('.lastName, .lastNameWithPicture');
  const firstName = text('.firstName, .firstNameWithPicture, .firstNameWithFlags');
  const startNumber = text('.startNumber');
  const attempt = text('.attempt');
  const weight = text('.weight').replace(/[^0-9]/g, '');

  const timerHost = root.querySelector('timer-element#athleteTimer');
  const timerRoot = timerHost?.shadowRoot;
  const timerDisplay = timerRoot?.querySelector('#timer')?.innerText?.trim().replace(/\s+/g, ' ') || '';

  return {
    athleteName: `${lastName} ${firstName} ${startNumber}`.trim(),
    attempt,
    weight,
    timerDisplay,
    timerRunning: Boolean(timerHost?.running),
    timerCurrentTime: Number(timerHost?.currentTime)
  };
}
```

Be prepared to adjust name formatting. Current MQTT expected `displayName` is `LAST FIRST startNumber`, so the attempt board reader should match that normalization.

## Phase 4: Extend Snapshot Model

In `UpdateCheck.java`, extend `Snapshot` to include optional clock fields:

```java
record Snapshot(
    String platform,
    BoardRole role,
    String athleteName,
    String attempt,
    String weight,
    String gridFirstCell,
    ClockSnapshot clock
)
```

Add:

```java
record ClockSnapshot(String display, boolean running, double currentTime) {
}
```

Keep `gridFirstCell` meaningful only for announcer. For attempt board, return `""`.

If minimal churn is preferred, keep existing `Snapshot` fields and add:

```java
String clockDisplay,
Boolean clockRunning,
Double clockCurrentTime
```

but a nested `ClockSnapshot` is cleaner.

## Phase 5: Split Matchers

The current matcher requires both header and grid confirmation. That should remain announcer-specific.

Introduce:

```java
interface DisplayMatcher {
    boolean expectedDisplayVisible(ExpectedDisplay expected, ExpectationState state, CleanLog log, MonitoredPage page);
    void logExpectedMiss(ExpectedDisplay expected, ExpectationState state, CleanLog log, MonitoredPage page);
}
```

Then:

- `AnnouncerDisplayMatcher`
  - uses current `matchesExpected(...)`
  - uses current `gridConfirmed(...)`
  - logs current announcer messages
- `AttemptBoardDisplayMatcher`
  - compares name, attempt digits, weight digits
  - does not require grid confirmation
  - additionally reports clock fields in logs
  - clock assertion may initially be informational unless Phase 7 is implemented

Important: keep the same supersede/timeout loop from current `MonitoredPlatform.verifyExpectedDisplay(...)`; only the snapshot reader and matcher should vary by role.

## Phase 6: Shared Clock Reader

The app already shares the clock component. Reflect that in the harness.

Add a helper, likely in `UpdateCheck` or a small `TimerSnapshotReader` class:

```java
static ClockSnapshot readTimerSnapshot(Page page, String hostScript)
```

Because the announcer timer and attempt-board timer are found differently, a practical design is:

```java
static ClockSnapshot readClockFromHost(Page page, String jsExpressionReturningTimerHost)
```

Examples:

- attempt board host:

```javascript
document.querySelector('attempt-board-template')?.shadowRoot?.querySelector('timer-element#athleteTimer')
```

- announcer host, if needed later:

```javascript
document.querySelector('.athleteGridTopBar timer-element')
```

The helper should read the common `timer-element` fields:

```text
shadowRoot #timer text
running
currentTime
lastTime
```

For the first implementation, use the shared helper only from the attempt-board reader. Keep announcer timer verification optional unless explicitly needed.

## Phase 7: Add Expected Clock Data

Current MQTT payload in `MQTTMonitor.java` does not include authoritative timer state. It has name, attempt, attempt number, attempts done, requested weight, sequence.

To perform real clock validation, add to `publishMqttPlaywrightLiftingOrder(...)`:

```java
IProxyTimer timer = getFop().getAthleteTimer();
payload.put("timerStateValid", e.isTimerStateValid());
payload.put("timerShouldRun", e.isTimerShouldRun());
payload.put("timerMillisRemaining", e.getTimerMillisRemaining());
payload.put("timerServerMillis", System.currentTimeMillis());
```

Only if those methods are available on `UIEvent.LiftingOrderUpdated`; if not, use the FOP timer directly:

```java
IProxyTimer timer = getFop().getAthleteTimer();
payload.put("timerShouldRun", timer != null && timer.isRunning());
payload.put("timerMillisRemaining", timer == null ? null : timer.liveTimeRemaining());
payload.put("timerStateValid", timer != null);
payload.put("timerServerMillis", System.currentTimeMillis());
```

Then extend `ExpectedDisplay`:

```java
record ExpectedDisplay(
    String displayName,
    String attempt,
    long sequence,
    String weight,
    Boolean timerStateValid,
    Boolean timerShouldRun,
    Long timerMillisRemaining,
    Long timerServerMillis
)
```

Clock validation rule:

- If `timerStateValid` is false/null: skip clock match, but include clock snapshot in confirmation logs.
- If expected stopped:
  - `clock.running == false`
  - display/currentTime within tolerance of `timerMillisRemaining`
- If expected running:
  - `clock.running == true`
  - currentTime approximately equals `timerMillisRemaining - elapsed`
  - use tolerance of about 1.5-2.0 seconds to avoid false misses from render/poll timing

Do not add per-second MQTT or browser callbacks.

## Phase 8: Logging

Update `CleanLog` to include role.

Current logs are:

```text
[RED] EXPECTING ...
[RED] CONFIRMED HEADER ...
[RED] MISS ...
```

Change to:

```text
[RED announcer] EXPECTING ...
[RED attempt] EXPECTING ...
[RED attempt] CONFIRMED DISPLAY [123 ms] name=... attempt#=... weight=... clock=...
[RED attempt] MISS expected clock stopped 57994ms but saw display='0:56' running=true currentTime=56.8
```

Avoid losing FOP-only methods by adding overloads:

```java
void status(String fop, BoardRole role, String label, boolean ok)
```

Keep existing methods as wrappers if needed during migration.

Also keep the OWLCMS server-side timer logs role-identifiable. Both announcer and attempt board clocks are `AthleteTimerElement`, so do not distinguish them by class name alone. Use one of these role markers:

- Playwright harness logs: the explicit `BoardRole` attached to each `MonitoredPage`.
- DOM snapshots: the page role and timer host selector used by that reader.
- OWLCMS logs: `TimerElement.describeTimerForDiagnostics()`, which includes `origin`, `parent`, `fop`, `bus`, `attached`, `running`, and `lastSeq`.

For OWLCMS logs, the important distinction is:

- announcer/control athlete timer: `origin=AnnouncerContent` or another `AthleteGridContent` subclass, and `control=true`
- attempt-board athlete timer: `origin=AbstractAttemptBoard` or `origin=AttemptBoard`, `parent=AttemptBoard`/`AbstractAttemptBoard`, and `control=false`

If this is not already visible enough in the logs produced during a failure run, add one temporary role tag to timer diagnostics rather than adding more class-name parsing in the script:

```java
public String describeTimerForDiagnostics() {
  ...
  return String.format("%s{role=%s,control=%s,origin=%s,fop=%s,bus=%s,parent=%s,attached=%s,running=%s,lastSeq=%s}",
      this.instanceId, timerDiagnosticRole(), isControlAthleteTimer(), originName, fopName, busName,
      parentName, this.getUI().isPresent(), this.elementRunning, this.lastAppliedTimerSeq);
}
```

Suggested role helper:

```java
private String timerDiagnosticRole() {
  Object origin = getOrigin();
  String originName = origin == null ? "" : origin.getClass().getSimpleName();
  String parentName = this.getParent().map(DebugUtils::getOwlcmsParentName).orElse("");
  if ("AnnouncerContent".equals(originName)) {
    return "announcer";
  }
  if (originName.contains("AttemptBoard") || parentName.contains("AttemptBoard")) {
    return "attempt";
  }
  return isControlAthleteTimer() ? "control" : "other";
}
```

## Phase 9: Launch Configs

Update `.vscode/launch.json`.

Keep existing "Playwright Announcer Updates" with default or explicit:

```json
"OWLCMS_PLAYWRIGHT_BOARDS": "announcer"
```

Add:

```json
{
  "type": "java",
  "name": "♦️ Playwright Attempt Board Updates",
  "request": "launch",
  "mainClass": "playwright.UpdateCheck",
  "projectName": "playwright",
  "cwd": "${workspaceFolder}/playwright",
  "vmArgs": "",
  "console": "integratedTerminal",
  "env": {
    "OWLCMS_FOPS": "RED,WHITE,BLUE",
    "OWLCMS_BASE_URL": "http://127.0.0.1:8080",
    "OWLCMS_MQTT_URI": "tcp://127.0.0.1:1883",
    "OWLCMS_PLAYWRIGHT_LOG": "logs/playwright.log",
    "OWLCMS_PLAYWRIGHT_HEADLESS": "false",
    "OWLCMS_PLAYWRIGHT_BOARDS": "attempt",
    "PLAYWRIGHT_BROWSER_CHANNEL": "chrome",
    "PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD": "1"
  }
}
```

Add combined:

```json
"OWLCMS_PLAYWRIGHT_BOARDS": "announcer,attempt"
```

Optionally rename the existing compound or add a new compound for combined monitoring.

## Phase 10: File-Backed Playwright Logs

The Playwright checker currently writes to the integrated terminal only. That is not enough for confirmation scripts, and it gives us nothing durable to correlate with `owlcms.log` after a run. Add a parseable Playwright log file and make every launch/script use it.

The purpose of `playwright.log` is correlation, not direct control. OWLCMS can remotely stop the simulation when a scoreboard is stuck, but runaway/stuck clocks are harder to stop safely from the outside. For clocks, the Playwright log should provide a durable, timestamp-aligned record that the scripts can correlate with OWLCMS server logs to locate the mistake.

Suggested default path, relative to the `playwright` module working directory:

```text
playwright/logs/playwright.log
```

Add `playwright/logs/` to `playwright/.gitignore`:

```gitignore
/logs/
```

Add `playwright/src/main/resources/logback.xml` with both console and file appenders:

```xml
<configuration>
  <property name="PLAYWRIGHT_LOG" value="${OWLCMS_PLAYWRIGHT_LOG:-logs/playwright.log}" />

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} %highlight(%-5level) %-75msg [%logger{100}:%L %M]%n</pattern>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>${PLAYWRIGHT_LOG}</file>
    <append>false</append>
    <encoder>
      <pattern>%d{HH:mm:ss.SSS} %-5level %-75msg [%logger{100}:%L %M]%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
  </root>
</configuration>
```

Use the same pattern as OWLCMS' detailed file log so line-by-line correlation is mechanical:

```text
%d{HH:mm:ss.SSS} %-5level %-75msg [%logger{100}:%L %M]%n
```

Do not use a Playwright-only date/time format unless OWLCMS is changed to match it too; the scripts should be able to parse both logs with the same timestamp regex and then report both `file:line` and timestamp.

If logback environment-variable substitution is unreliable in the current runtime, use a system property instead and set it in launch configs:

```json
"vmArgs": "-DOWLCMS_PLAYWRIGHT_LOG=logs/playwright.log"
```

Then use this property in `logback.xml`:

```xml
<property name="PLAYWRIGHT_LOG" value="${OWLCMS_PLAYWRIGHT_LOG:-logs/playwright.log}" />
```

At startup, `UpdateCheck` should log the resolved file path near the config section:

```text
Playwright log: logs/playwright.log
```

Add a run wrapper script so stale failures from a previous run cannot contaminate confirmation:

```text
playwright/scripts/run-update-check.sh
```

The wrapper should:

1. Create `playwright/logs/` if missing.
2. Remove or truncate `playwright/logs/playwright.log` before launching `UpdateCheck`.
3. Set `OWLCMS_PLAYWRIGHT_LOG=logs/playwright.log` for the Java process.
4. Leave both `owlcms/logs/owlcms.log` and `playwright/logs/playwright.log` in place for the confirmation and mistake-location scripts after the run.

The confirmation and mistake-location scripts must not clear logs; they only read completed run logs.

## Phase 11: Validation

After Java edits:

1. Run `get_errors` on:
   - `playwright/src/main/java/playwright/UpdateCheck.java`
   - `playwright/src/main/java/playwright/MonitoredPlatform.java`
   - any new Java files under `playwright/src/main/java/playwright/`
   - `owlcms/src/main/java/app/owlcms/monitors/MQTTMonitor.java`, if timer payload fields were added

2. Do not run Maven unless the user explicitly consents.

3. If the debug launch is used manually, test in three modes:
   - `OWLCMS_PLAYWRIGHT_BOARDS=announcer`
   - `OWLCMS_PLAYWRIGHT_BOARDS=attempt`
   - `OWLCMS_PLAYWRIGHT_BOARDS=announcer,attempt`

Expected behavior:

- announcer mode behaves as before
- attempt mode opens only attempt-board pages
- combined mode opens both pages for each FOP
- any failed page identifies both FOP and role
- no per-second server load is introduced

## Phase 12: Log Confirmation And Mistake Location Scripts

Add log parsers so a run can be confirmed clean after the fact, without relying on manual grep or terminal scrollback. They should be scripts in the Playwright module, not ad hoc one-liners.

Suggested paths:

```text
playwright/scripts/confirm-clock-logs.py
playwright/scripts/locate-clock-mistakes.py
```

The confirmation script should accept both log paths and fail with a non-zero exit code if it sees evidence of either clock mishap:

```bash
python3 playwright/scripts/confirm-clock-logs.py \
  --owlcms-log owlcms/logs/owlcms.log \
  --playwright-log playwright/logs/playwright.log
```

The mistake locator should print actionable findings with source log and line number:

```bash
python3 playwright/scripts/locate-clock-mistakes.py \
  --owlcms-log owlcms/logs/owlcms.log \
  --playwright-log playwright/logs/playwright.log
```

Example output:

```text
FAIL 17:44:12.481 RED announcer CLOCK_KEPT_RUNNING owlcms/logs/owlcms.log:1832 announcer timer tick correction display='0:56' ...
FAIL 17:44:15.012 RED attempt MISSED_STOP owlcms/logs/owlcms.log:2144 uiBus pre-post StopTime seq=91 ...
FAIL 17:44:15.884 WHITE attempt CLOCK_KEPT_RUNNING playwright/logs/playwright.log:441 [WHITE attempt] MISS expected clock stopped ... running=true
```

Correlation rules:

- Parse the common `HH:mm:ss.SSS` prefix from both files.
- Report `timestamp`, `fop`, `role`, `reason`, `file:line`, and the original line.
- For a Playwright clock failure, show the nearest OWLCMS timer lines for the same FOP within a small time window, for example +/- 2 seconds:
  - `uiBus pre-post StartTime/StopTime`
  - `doStartTimer applying start`
  - `doStopTimer applying stop`
  - `doStopTimer STALE-DROPPED`
  - `announcer timer tick correction`
- For an OWLCMS timer failure, show the nearest Playwright lines for the same FOP/role in the same window:
  - `[FOP role] EXPECTING ...`
  - `[FOP role] CONFIRMED ...`
  - `[FOP role] MISS ...`
  - page open/navigation lines

Minimum checks:

1. Detect clocks that did not start:
   - For each `uiBus pre-post StartTime seq=...` with at least one relevant timer subscriber, require a later matching `doStartTimer applying start seq=...` for that FOP/role.
   - If a `StartTime` has `athleteTimerSubscribers=0`, report it separately as `NO_TIMER_REGISTERED`, not as a missed start. Startup stop/start events before pages are open should not fail the run unless they occur during a monitored Playwright window.

2. Detect clocks that did not stop:
   - For each `uiBus pre-post StopTime seq=...` with at least one relevant timer subscriber, require a later matching `doStopTimer applying stop seq=...` for that FOP/role.
   - If a later `doStopTimer STALE-DROPPED seq=...` appears instead, report `STALE_DROPPED_STOP`.
   - If no stop application appears and no newer `StartTime` superseded it, report `MISSED_STOP`.

3. Detect clocks that kept running after stop:
   - Any `announcer timer tick correction ...` is a failure for announcer/control clocks.
  - Any Playwright log line of the form `[FOP attempt] MISS expected clock stopped ... running=true` or `[FOP attempt] MISS expected clock stopped ... decreasing=true` in `playwright.log` is a failure for attempt-board clocks.

4. Confirm client application when available:
   - For announcer/control timers, if `doStopTimer applying stop seq=N` is logged, treat `announcer timer stop client acknowledged seq=... running=false` as additional confirmation that the browser executed the pause command.
   - Do not require this acknowledgement for attempt boards unless equivalent acknowledgement logging is added; display boards should not introduce per-second callbacks.

Role classification rules:

- Use explicit Playwright role prefixes first: `[RED announcer]`, `[RED attempt]`.
- Use timer diagnostic `role=...` if the temporary helper above is added.
- Otherwise parse `registeredTimerDetails(...)` entries:
  - `origin=AnnouncerContent` or `control=true` means announcer/control.
  - `origin` or `parent` containing `AttemptBoard` means attempt board.
- Never classify solely from `class=AthleteTimerElement`; both clocks use that class.

Suggested script structure:

```python
#!/usr/bin/env python3
import argparse
import re
import sys

parser = argparse.ArgumentParser()
parser.add_argument("--owlcms-log", default="owlcms/logs/owlcms.log")
parser.add_argument("--playwright-log", default="playwright/logs/playwright.log")
args = parser.parse_args()

PRE_POST = re.compile(r"(?P<fop>\S+).*uiBus pre-post (?P<event>StartTime|StopTime) seq=(?P<seq>\d+).*athleteTimerSubscribers=(?P<timers>\d+).*timers=(?P<details>\[.*\])")
START_APPLY = re.compile(r"(?P<fop>\S+).*doStartTimer applying start seq=(?P<seq>\d+)")
STOP_APPLY = re.compile(r"(?P<fop>\S+).*doStopTimer applying stop seq=(?P<seq>\d+)")
STOP_STALE = re.compile(r"(?P<fop>\S+).*doStopTimer STALE-DROPPED seq=(?P<seq>\d+)")
TICK_CORRECTION = re.compile(r"(?P<fop>\S+).*announcer timer tick correction")
ATTEMPT_CLOCK_MISS = re.compile(r"\[(?P<fop>[^\]]+) attempt\] MISS expected clock stopped.*(running=true|decreasing=true)")

events = {}
applied_starts = set()
applied_stops = set()
stale_stops = set()
failures = []

def failure(log_path, line_number, fop, role, reason, line):
  failures.append((log_path, line_number, fop, role, reason, line.strip()))

def roles_from_details(details):
  roles = set()
  for item in re.findall(r"[0-9a-f]+\{[^}]+\}", details):
    if "role=announcer" in item or "origin=AnnouncerContent" in item or "control=true" in item:
      roles.add("announcer")
    if "role=attempt" in item or "AttemptBoard" in item:
      roles.add("attempt")
  return roles or {"unknown"}

with open(args.owlcms_log, encoding="utf-8", errors="replace") as log:
  for line_number, line in enumerate(log, start=1):
    if match := PRE_POST.search(line):
      fop = match.group("fop")
      seq = int(match.group("seq"))
      timers = int(match.group("timers"))
      roles = roles_from_details(match.group("details"))
      events[(fop, seq, match.group("event"))] = (timers, roles, args.owlcms_log, line_number, line.strip())
    if match := START_APPLY.search(line):
      applied_starts.add((match.group("fop"), int(match.group("seq"))))
    if match := STOP_APPLY.search(line):
      applied_stops.add((match.group("fop"), int(match.group("seq"))))
    if match := STOP_STALE.search(line):
      stale_stops.add((match.group("fop"), int(match.group("seq"))))
    if match := TICK_CORRECTION.search(line):
      failure(args.owlcms_log, line_number, match.group("fop"), "announcer", "CLOCK_KEPT_RUNNING", line)

with open(args.playwright_log, encoding="utf-8", errors="replace") as log:
  for line_number, line in enumerate(log, start=1):
    if match := ATTEMPT_CLOCK_MISS.search(line):
      failure(args.playwright_log, line_number, match.group("fop"), "attempt", "CLOCK_KEPT_RUNNING", line)

for (fop, seq, event), (timer_count, roles, log_path, line_number, source) in events.items():
  if timer_count == 0:
    continue
  if event == "StartTime" and (fop, seq) not in applied_starts:
    failure(log_path, line_number, fop, ",".join(sorted(roles)), "MISSED_START", source)
  if event == "StopTime" and (fop, seq) not in applied_stops:
    reason = "STALE_DROPPED_STOP" if (fop, seq) in stale_stops else "MISSED_STOP"
    failure(log_path, line_number, fop, ",".join(sorted(roles)), reason, source)

if failures:
  print("Clock log confirmation FAILED")
  for log_path, line_number, fop, role, reason, line in failures:
    print(f"FAIL {fop} {role} {reason} {log_path}:{line_number} {line}")
  sys.exit(1)

print("Clock log confirmation OK: no missed start, missed stop, stale stop, or stuck-running clock evidence found")
```

This script is intentionally conservative. It should report unknown or ambiguous role classification as `unknown` instead of guessing, because `AthleteTimerElement` alone does not identify the board.