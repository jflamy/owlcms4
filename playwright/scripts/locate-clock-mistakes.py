#!/usr/bin/env python3
"""
locate-clock-mistakes.py — same checks as confirm-clock-logs.py, but for
each failure it also prints correlated context lines from both logs within
+/- 2 seconds of the failure timestamp.

Usage:
    python3 playwright/scripts/locate-clock-mistakes.py \
        --owlcms-log owlcms/logs/owlcms.log \
        --playwright-log playwright/logs/playwright.log

Rolled OWLCMS logs named owlcms_yyyy-mm-dd.log next to --owlcms-log are scanned
in date order before the active owlcms.log tail.
"""
import argparse
from collections import Counter
from pathlib import Path
import re
import sys

parser = argparse.ArgumentParser(description="Locate clock mistakes in owlcms + playwright logs")
parser.add_argument("--owlcms-log", default="owlcms/logs/owlcms.log", metavar="PATH")
parser.add_argument("--playwright-log", default="playwright/logs/playwright.log", metavar="PATH")
parser.add_argument("--window-seconds", type=float, default=2.0, metavar="S",
                    help="Context window +/- seconds around a failure (default: 2.0)")
args = parser.parse_args()


def owlcms_log_paths(active_log: str) -> list[Path]:
    """Return rolled owlcms_yyyy-mm-dd.log files in date order, then active_log."""
    active_path = Path(active_log)
    log_dir = active_path.parent if active_path.parent != Path("") else Path(".")
    date_pattern = re.compile(rf"^{re.escape(active_path.stem)}_\d{{4}}-\d{{2}}-\d{{2}}{re.escape(active_path.suffix)}$")
    rolled = sorted(path for path in log_dir.glob(f"{active_path.stem}_*{active_path.suffix}")
                    if date_pattern.match(path.name))
    return rolled + [active_path]


OWLCMS_LOG_PATHS = owlcms_log_paths(args.owlcms_log)

# ---- patterns ----
PRE_POST = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?(\S+)\s+uiBus pre-post (StartTime|StopTime) seq=(\d+)"
    r".*?athleteTimerSubscribers=(\d+).*?timers=(\[.*\])"
)
START_APPLY = re.compile(r"(\S+)\s+doStartTimer applying start seq=(\d+).*?timer=([0-9a-f]+)\{([^}]+)\}")
STOP_APPLY  = re.compile(r"(\S+)\s+doStopTimer applying stop seq=(\d+).*?timer=([0-9a-f]+)\{([^}]+)\}")
STOP_STALE  = re.compile(r"(\S+)\s+doStopTimer STALE-DROPPED seq=(\d+).*?timer=([0-9a-f]+)\{([^}]+)\}")
STOP_FIRED  = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?(\S+)\s+doStopTimer subscriber fired seq=(\d+)"
    r".*?timer=([0-9a-f]+)\{([^}]+)\}"
)
TICK_MISMATCH = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?(\S+)\s+timer tick mismatch timer=([0-9a-f]+)\{([^}]+)\}"
)
TIMER_DETAIL = re.compile(r"([0-9a-f]+)\{([^}]+)\}")
STOP_GRACE_MILLIS = 750

# context patterns to show around a failure
OWLCMS_CONTEXT = re.compile(
    r"uiBus pre-post (StartTime|StopTime)"
    r"|doStartTimer applying start"
    r"|doStopTimer subscriber fired"
    r"|doStopTimer applying stop"
    r"|doStopTimer STALE-DROPPED"
    r"|timer tick mismatch"
)
PLAYWRIGHT_CONTEXT = re.compile(
    r"\[[^\]]+ (announcer|attempt)\] (EXPECTING|CONFIRMED|MISS|OPEN|NAV )"
)

TS_RE = re.compile(r"^(\d{2}):(\d{2}):(\d{2})\.(\d{3})")


def ts_to_ms(ts: str) -> int | None:
    m = TS_RE.match(ts)
    if not m:
        return None
    h, mi, s, ms = int(m.group(1)), int(m.group(2)), int(m.group(3)), int(m.group(4))
    return ((h * 3600 + mi * 60 + s) * 1000) + ms


def role_from_timer_body(body: str) -> str:
    if m := re.search(r"(?:^|,)role=([^,}]+)", body):
        return m.group(1)
    if "origin=AnnouncerContent" in body or "control=true" in body:
        return "announcer"
    if "AttemptBoard" in body:
        return "attempt"
    return "unknown"


def should_check_timer_ref(body: str) -> bool:
    role = role_from_timer_body(body)
    if role in {"announcer", "control"}:
        return True
    return role == "attempt" and "origin=AttemptBoard" in body


def timer_refs_from_details(details: str) -> set:
    timer_refs = set()
    for timer_id, body in TIMER_DETAIL.findall(details):
        role = role_from_timer_body(body)
        if should_check_timer_ref(body):
            timer_refs.add((timer_id, role))
    return timer_refs or {("", "unknown")}


def log_timer_ref(match) -> tuple:
    return (match.group(1), int(match.group(2)), match.group(3), role_from_timer_body(match.group(4)))


# ---- load both logs indexed by millisecond timestamp for context lookup ----

def load_log(path: str, context_pattern: re.Pattern) -> list[tuple[int, int, str, str]]:
    """Return list of (ts_ms, line_no, log_path, raw_line) for lines matching context_pattern."""
    result = []
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            for line_no, line in enumerate(fh, start=1):
                m = TS_RE.match(line)
                if m and context_pattern.search(line):
                    ts_ms = ts_to_ms(line[:12])
                    if ts_ms is not None:
                        result.append((ts_ms, line_no, path, line.rstrip()))
    except FileNotFoundError:
        pass
    return result


owlcms_ctx = [entry for path in OWLCMS_LOG_PATHS for entry in load_log(str(path), OWLCMS_CONTEXT)]
playwright_ctx = load_log(args.playwright_log, PLAYWRIGHT_CONTEXT)


def context_lines_for(failure_ts_ms: int, fop: str, role: str, window_ms: int = None) -> list[str]:
    """Return context lines from both logs near the failure timestamp and FOP."""
    if window_ms is None:
        window_ms = int(args.window_seconds * 1000)
    lo, hi = failure_ts_ms - window_ms, failure_ts_ms + window_ms
    lines = []
    for (ts_ms, line_no, path, raw) in owlcms_ctx + playwright_ctx:
        if lo <= ts_ms <= hi and fop in raw:
            lines.append(f"  context {path}:{line_no} {raw}")
    return lines


# ---- state ----
events = {}
applied_starts = set()
applied_stops  = set()
stale_stops    = set()
recent_stop_fired = {}  # (fop, timer_id) -> timestamp ms
failures = []  # (ts_ms, ts, fop, role, reason, file_path, line_no, raw_line)


def record_failure(ts, fop, role, reason, log_path, line_no, raw_line):
    ts_ms = ts_to_ms(ts) or 0
    failures.append((ts_ms, ts, fop, role, reason, log_path, line_no, raw_line.strip()))


def print_failure_summary():
    print(f"Clock log check found {len(failures)} issue(s):")
    print("Summary by reason:")
    for reason, count in Counter(failure[4] for failure in failures).most_common():
        print(f"  {reason}: {count}")
    print("Summary by FOP/role:")
    for (fop, role), count in Counter((failure[2], failure[3]) for failure in failures).most_common():
        print(f"  {fop} {role}: {count}")
    print("Summary by log:")
    for log_path, count in Counter(failure[5] for failure in failures).most_common():
        print(f"  {log_path}: {count}")
    print()


def is_recent_stop_in_flight(ts: str, fop: str, timer_id: str) -> bool:
    tick_ms = ts_to_ms(ts)
    stop_ms = recent_stop_fired.get((fop, timer_id))
    if tick_ms is None or stop_ms is None:
        return False
    return 0 <= tick_ms - stop_ms <= STOP_GRACE_MILLIS


# ---- scan OWLCMS logs ----
for owlcms_log_path in OWLCMS_LOG_PATHS:
    try:
        with open(owlcms_log_path, encoding="utf-8", errors="replace") as fh:
            log_path = str(owlcms_log_path)
            for line_no, line in enumerate(fh, start=1):
                if m := PRE_POST.search(line):
                    ts, fop, event, seq, timers, details = (
                        m.group(1), m.group(2), m.group(3),
                        int(m.group(4)), int(m.group(5)), m.group(6)
                    )
                    timer_refs = timer_refs_from_details(details)
                    key = (fop, seq, event)
                    if key not in events:
                        events[key] = (timers, timer_refs, log_path, line_no, ts, line)

                if m := START_APPLY.search(line):
                    applied_starts.add(log_timer_ref(m))

                if m := STOP_APPLY.search(line):
                    applied_stops.add(log_timer_ref(m))

                if m := STOP_STALE.search(line):
                    stale_stops.add(log_timer_ref(m))

                if m := STOP_FIRED.search(line):
                    ts_ms = ts_to_ms(m.group(1))
                    if ts_ms is not None:
                        recent_stop_fired[(m.group(2), m.group(4))] = ts_ms

                if m := TICK_MISMATCH.search(line):
                    if not is_recent_stop_in_flight(m.group(1), m.group(2), m.group(3)):
                        record_failure(m.group(1), m.group(2), role_from_timer_body(m.group(4)),
                                       "CLOCK_KEPT_RUNNING", log_path, line_no, line)
    except FileNotFoundError:
        if owlcms_log_path == Path(args.owlcms_log):
            print(f"WARNING: owlcms log not found: {owlcms_log_path}", file=sys.stderr)

# ---- check apply coverage ----
for (fop, seq, event), (timer_count, timer_refs, log_path, line_no, ts, raw_line) in events.items():
    if timer_count == 0:
        continue
    for timer_id, role in timer_refs:
        timer_key = (fop, seq, timer_id, role)
        if event == "StartTime" and timer_key not in applied_starts:
            record_failure(ts, fop, role, "MISSED_START", log_path, line_no, raw_line)
        if event == "StopTime" and timer_key not in applied_stops:
            reason = "STALE_DROPPED_STOP" if timer_key in stale_stops else "MISSED_STOP"
            record_failure(ts, fop, role, reason, log_path, line_no, raw_line)

# ---- report ----
if not failures:
    print("Clock log check OK: no missed start, missed stop, stale stop, "
          "or stuck-running clock found")
    sys.exit(0)

print_failure_summary()
for ts_ms, ts, fop, role, reason, log_path, line_no, raw_line in sorted(failures):
    print(f"FAIL {ts} {fop} {role} {reason} {log_path}:{line_no}")
    print(f"  {raw_line}")
    for ctx in context_lines_for(ts_ms, fop, role):
        print(ctx)
    print()
sys.exit(1)
