#!/usr/bin/env python3
"""
clock-log-report.py - consolidated Playwright/OWLCMS diagnostics for simulation runs.

Produces:
  1. A compact human-readable summary on stdout, and optionally --summary-file.
  2. A structured JSON evidence file for agent follow-up analysis.

Rolled OWLCMS logs named owlcms_yyyy-mm-dd.log next to --owlcms-log are scanned
in date order before the active owlcms.log tail.

Usage:
    python3 playwright/scripts/clock-log-report.py

or:
    playwright/scripts/clock-log-report.sh
"""
import argparse
import json
import math
import re
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_EVIDENCE_FILE = "playwright/logs/clock-log-evidence.json"
DEFAULT_SUMMARY_FILE = "playwright/logs/clock-log-summary.txt"
TIMING_BUCKETS = (
    ("upTo50ms", 50),
    ("upTo100ms", 100),
    ("upTo150ms", 150),
    ("upTo200ms", 200),
    ("upTo300ms", 300),
)
OVER_300_BUCKET = "moreThan300ms"
MAX_PLAYWRIGHT_CLOCK_DELTA_MS = 15000
CLOCK_RENDER_BEFORE_EXPECT_MS = 500
CLOCK_RENDER_AFTER_EXPECT_MS = 2000

parser = argparse.ArgumentParser(description="Produce OWLCMS/Playwright diagnostic summary and evidence JSON")
parser.add_argument("--owlcms-log", default="owlcms/logs/owlcms.log", metavar="PATH")
parser.add_argument("--playwright-log", default="playwright/logs/playwright.log", metavar="PATH")
parser.add_argument("--evidence-file", default=DEFAULT_EVIDENCE_FILE, metavar="PATH")
parser.add_argument("--summary-file", default=DEFAULT_SUMMARY_FILE, metavar="PATH")
parser.add_argument("--window-seconds", type=float, default=2.0, metavar="S",
                    help="Context window +/- seconds around a failure (default: 2.0)")
parser.add_argument("--stop-grace-millis", type=int, default=750, metavar="MS",
                    help="Ignore tick mismatch evidence within this delay after doStopTimer subscriber fired (default: 750)")
parser.add_argument("--max-context-lines", type=int, default=80, metavar="N",
                    help="Maximum context lines per failure in evidence JSON (default: 80)")
parser.add_argument("--max-summary-failures", type=int, default=25, metavar="N",
                    help="Deprecated; all failures are always printed")
parser.add_argument("--no-fail-on-issues", action="store_true",
                    help="Deprecated; reports now always exit 0 even if issues are found")
args = parser.parse_args()

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
PLAYWRIGHT_MISS = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?\[(.+?)\s+(announcer|attempt)\]\s+MISS\s+(.*)"
)
PLAYWRIGHT_CONFIRMED = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?\[(.+?)\s+(announcer|attempt)\]"
    r"\s+CONFIRMED\s+(HEADER|DISPLAY|GRID)\s+\[(\d+)\s+ms\].*?\bseq=(\d+)"
)
PLAYWRIGHT_EXPECTING = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?\[(.+?)\s+(announcer|attempt)\]\s+EXPECTING\s+(.*?)\s*\bseq=(\d+)"
)
PLAYWRIGHT_SUPERCEDED = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?\[(.+?)\s+(announcer|attempt)\]\s+SUPERCEDED\s+.*?\bseq=(\d+)\s*->"
)
OWLCMS_CLIENT_RENDER = re.compile(
    r"(\d{2}:\d{2}:\d{2}\.\d{3}).*?(\S+)\s+(announcer|attempt) timer "
    r"(start|stop|set) client rendered \+(\d+)ms seq=(\d+) seconds=([^\s]+)"
)
TIMER_DETAIL = re.compile(r"([0-9a-f]+)\{([^}]+)\}")
TS_RE = re.compile(r"^(\d{2}):(\d{2}):(\d{2})\.(\d{3})")

OWLCMS_CONTEXT = re.compile(
    r"uiBus pre-post (StartTime|StopTime)"
    r"|doStartTimer subscriber fired"
    r"|doStartTimer applying start"
    r"|doStartTimer STALE-DROPPED"
    r"|doStopTimer subscriber fired"
    r"|doStopTimer applying stop"
    r"|doStopTimer STALE-DROPPED"
    r"|timer tick mismatch"
    r"|client rendered"
    r"|client acknowledged"
)
PLAYWRIGHT_CONTEXT = re.compile(
    r"\[[^\]]+ (announcer|attempt)\] (EXPECTING|CONFIRMED|MISS|PAUSE|SUPERCEDED|STOP|OPEN|NAV )"
)


def owlcms_log_paths(active_log: str) -> list[Path]:
    """Return rolled owlcms_yyyy-mm-dd.log files in date order, then active_log."""
    active_path = Path(active_log)
    log_dir = active_path.parent if active_path.parent != Path("") else Path(".")
    date_pattern = re.compile(rf"^{re.escape(active_path.stem)}_\d{{4}}-\d{{2}}-\d{{2}}{re.escape(active_path.suffix)}$")
    rolled = sorted(path for path in log_dir.glob(f"{active_path.stem}_*{active_path.suffix}")
                    if date_pattern.match(path.name))
    return rolled + [active_path]


def file_info(path: Path) -> dict:
    try:
        stat = path.stat()
        return {"path": str(path), "exists": True, "bytes": stat.st_size}
    except FileNotFoundError:
        return {"path": str(path), "exists": False, "bytes": 0}


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


def timer_refs_from_details(details: str) -> set[tuple[str, str]]:
    """Parse timers=[...] and return (timer_id, role) pairs relevant to athlete clocks."""
    timer_refs = set()
    for timer_id, body in TIMER_DETAIL.findall(details):
        role = role_from_timer_body(body)
        if should_check_timer_ref(body):
            timer_refs.add((timer_id, role))
    return timer_refs or {("", "unknown")}


def log_timer_ref(match) -> tuple[str, int, str, str]:
    return (match.group(1), int(match.group(2)), match.group(3), role_from_timer_body(match.group(4)))


def load_context(path: Path, context_pattern: re.Pattern, source: str) -> list[dict]:
    result = []
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            for line_no, line in enumerate(fh, start=1):
                m = TS_RE.match(line)
                if m and context_pattern.search(line):
                    ts_ms = ts_to_ms(line[:12])
                    if ts_ms is not None:
                        result.append({
                            "source": source,
                            "timestamp": line[:12],
                            "timestampMs": ts_ms,
                            "log": str(path),
                            "line": line_no,
                            "raw": line.rstrip(),
                        })
    except FileNotFoundError:
        pass
    return result


def summarize_counter(counter: Counter) -> dict:
    return {str(key): count for key, count in counter.most_common()}


def failure_key(failure: dict) -> tuple:
    return (failure.get("order", 0), failure.get("log", ""), failure.get("line", 0), failure.get("reason", ""))


def context_for_failure(failure: dict, contexts: list[dict], window_ms: int) -> list[dict]:
    ts_ms = failure.get("timestampMs")
    fop = failure.get("fop", "")
    if ts_ms is None:
        return []
    lo, hi = ts_ms - window_ms, ts_ms + window_ms
    selected = [entry for entry in contexts
                if lo <= entry["timestampMs"] <= hi and (not fop or fop in entry["raw"])]
    selected.sort(key=lambda entry: (entry["timestampMs"], entry["log"], entry["line"]))
    if args.max_context_lines > 0 and len(selected) > args.max_context_lines:
        return selected[:args.max_context_lines]
    return selected


owlcms_paths = owlcms_log_paths(args.owlcms_log)
playwright_path = Path(args.playwright_log)
window_ms = int(args.window_seconds * 1000)

# ---- state ----
events = {}
applied_starts = set()
applied_stops = set()
stale_stops = set()
recent_stop_fired = {}  # (fop, timer_id) -> timestamp ms
failures = []
suppressed_tick_mismatches = []
expected_initial_misses = []
warnings = []
stats = Counter()
confirmed_name_timings_by_role = defaultdict(list)
confirmed_name_timings_by_fop_role = defaultdict(list)
confirmed_name_max_by_role = {}
confirmed_name_max_by_fop_role = {}
missed_display_timings_by_role = defaultdict(list)
missed_display_timings_by_fop_role = defaultdict(list)
clock_render_events = []
clock_render_timings_by_role = defaultdict(list)
clock_render_timings_by_fop_role = defaultdict(list)
clock_render_max_by_role = {}
clock_render_max_by_fop_role = {}
playwright_clock_delta_timings_by_role = defaultdict(list)
playwright_clock_delta_timings_by_fop_role = defaultdict(list)
playwright_clock_delta_max_by_role = {}
playwright_clock_delta_max_by_fop_role = {}
pending_expectation_by_fop_role = {}
scan_order = 0


def record_failure(order, ts, fop, role, reason, log_path, line_no, raw_line, details=None):
    failures.append({
        "order": order,
        "timestamp": ts,
        "timestampMs": ts_to_ms(ts),
        "fop": fop,
        "role": role,
        "reason": reason,
        "log": log_path,
        "line": line_no,
        "raw": raw_line.strip(),
        "details": details or {},
    })


def record_expected_initial_miss(order, ts, fop, role, log_path, line_no, raw_line, message):
    expected_initial_misses.append({
        "order": order,
        "timestamp": ts,
        "timestampMs": ts_to_ms(ts),
        "fop": fop,
        "role": role,
        "reason": "PLAYWRIGHT_INITIAL_MISS",
        "expected": True,
        "classification": "EXPECTED_STARTUP_PROBE",
        "log": log_path,
        "line": line_no,
        "raw": raw_line.strip(),
        "details": {"message": message},
    })


def new_pending_expectation(order, ts, fop, role, seq, display, log_path, line_no, raw_line):
    return {
        "order": order,
        "timestamp": ts,
        "timestampMs": ts_to_ms(ts),
        "fop": fop,
        "role": role,
        "seq": seq,
        "display": display,
        "log": log_path,
        "line": line_no,
        "raw": raw_line.strip(),
        "header": None,
        "grid": None,
        "displayMs": None,
        "superseded": False,
        "confirmed": False,
    }


def pending_fully_confirmed(pending):
    if pending["role"] == "announcer":
        return pending["header"] is not None and pending["grid"] is not None
    return pending["displayMs"] is not None


def record_full_confirmation(pending, ts):
    role = pending["role"]
    if role == "announcer":
        millis = max(pending["header"], pending["grid"])
    else:
        millis = pending["displayMs"]
    event = {
        "order": pending["order"],
        "timestamp": ts,
        "timestampMs": ts_to_ms(ts),
        "fop": pending["fop"],
        "role": role,
        "seq": pending["seq"],
        "kind": "FULL",
        "millis": millis,
        "expectation": pending,
        "log": pending["log"],
        "line": pending["line"],
        "raw": pending["raw"],
    }
    fop_role = f"{pending['fop']} {role}"
    confirmed_name_timings_by_role[role].append(millis)
    confirmed_name_timings_by_fop_role[fop_role].append(millis)
    if role not in confirmed_name_max_by_role or millis > confirmed_name_max_by_role[role]["millis"]:
        confirmed_name_max_by_role[role] = event
    if fop_role not in confirmed_name_max_by_fop_role or millis > confirmed_name_max_by_fop_role[fop_role]["millis"]:
        confirmed_name_max_by_fop_role[fop_role] = event
    stats["playwrightConfirmed.total"] += 1
    stats[f"playwrightConfirmed.{role}"] += 1
    record_playwright_clock_delta(event)


def record_clock_render_timing(order, ts, fop, role, command, millis, sequence, seconds, log_path, line_no, raw_line):
    event = {
        "order": order,
        "timestamp": ts,
        "timestampMs": ts_to_ms(ts),
        "fop": fop,
        "role": role,
        "command": command,
        "millis": millis,
        "sequence": sequence,
        "seconds": seconds,
        "log": log_path,
        "line": line_no,
        "raw": raw_line.strip(),
    }
    clock_render_events.append(event)
    clock_render_timings_by_role[role].append(millis)
    fop_role = f"{fop} {role}"
    clock_render_timings_by_fop_role[fop_role].append(millis)
    if role not in clock_render_max_by_role or millis > clock_render_max_by_role[role]["millis"]:
        clock_render_max_by_role[role] = event
    if fop_role not in clock_render_max_by_fop_role or millis > clock_render_max_by_fop_role[fop_role]["millis"]:
        clock_render_max_by_fop_role[fop_role] = event
    stats["owlcmsClockRender.total"] += 1
    stats[f"owlcmsClockRender.{role}"] += 1


def record_playwright_clock_delta(playwright_event):
    render_event = nearest_clock_render_event(playwright_event)
    if render_event is None:
        stats["playwrightClockDelta.unmatched"] += 1
        return
    delta_ms = playwright_event["timestampMs"] - render_event["timestampMs"]
    if delta_ms < 0:
        stats["playwrightClockDelta.negative"] += 1
        return
    event = {
        "timestamp": playwright_event["timestamp"],
        "timestampMs": playwright_event["timestampMs"],
        "fop": playwright_event["fop"],
        "role": playwright_event["role"],
        "kind": playwright_event["kind"],
        "millis": delta_ms,
        "playwrightEvent": playwright_event,
        "clockRenderEvent": render_event,
    }
    role = playwright_event["role"]
    fop_role = f"{playwright_event['fop']} {role}"
    playwright_clock_delta_timings_by_role[role].append(delta_ms)
    playwright_clock_delta_timings_by_fop_role[fop_role].append(delta_ms)
    if role not in playwright_clock_delta_max_by_role or delta_ms > playwright_clock_delta_max_by_role[role]["millis"]:
        playwright_clock_delta_max_by_role[role] = event
    if fop_role not in playwright_clock_delta_max_by_fop_role or delta_ms > playwright_clock_delta_max_by_fop_role[fop_role]["millis"]:
        playwright_clock_delta_max_by_fop_role[fop_role] = event
    stats["playwrightClockDelta.total"] += 1
    stats[f"playwrightClockDelta.{role}"] += 1


def nearest_clock_render_event(playwright_event):
    expectation = playwright_event.get("expectation")
    expected_ms = expectation.get("timestampMs") if expectation else None
    if expected_ms is None:
        return None
    lo = expected_ms - CLOCK_RENDER_BEFORE_EXPECT_MS
    hi = expected_ms + CLOCK_RENDER_AFTER_EXPECT_MS
    candidates = [event for event in clock_render_events
                  if event["fop"] == playwright_event["fop"]
                  and event["role"] == playwright_event["role"]
                  and event["command"] == "start"
                  and event["timestampMs"] is not None
                  and lo <= event["timestampMs"] <= hi]
    if not candidates:
        return None
    return min(candidates, key=lambda event: (abs(event["timestampMs"] - expected_ms), event["order"]))


def record_missed_display_timing(fop, role, message):
    if m := re.search(r"after\s+(\d+)ms\s+/\s+\d+\s+polls", message):
        millis = int(m.group(1))
        missed_display_timings_by_role[role].append(millis)
        missed_display_timings_by_fop_role[f"{fop} {role}"].append(millis)


def timing_stats(samples, max_event):
    count = len(samples)
    if count == 0:
        return {"count": 0, "buckets": empty_timing_buckets()}
    average = sum(samples) / count
    variance = sum((sample - average) ** 2 for sample in samples) / count
    return {
        "count": count,
        "averageMs": round(average, 1),
        "maxMs": max(samples),
        "stdDevMs": round(math.sqrt(variance), 1),
        "buckets": timing_buckets(samples),
        "maxEvent": max_event,
    }


def empty_timing_buckets():
    buckets = {name: 0 for name, _limit in TIMING_BUCKETS}
    buckets[OVER_300_BUCKET] = 0
    return buckets


def timing_buckets(samples):
    buckets = empty_timing_buckets()
    for sample in samples:
        for name, limit in TIMING_BUCKETS:
            if sample <= limit:
                buckets[name] += 1
                break
        else:
            buckets[OVER_300_BUCKET] += 1
    return buckets


def format_timing_buckets(buckets):
    return (
        f"<=50={buckets['upTo50ms']} "
        f"<=100={buckets['upTo100ms']} "
        f"<=150={buckets['upTo150ms']} "
        f"<=200={buckets['upTo200ms']} "
        f"<=300={buckets['upTo300ms']} "
        f">300={buckets[OVER_300_BUCKET]}"
    )


def build_timing_stats(timings, max_events):
    return {
        key: timing_stats(samples, max_events.get(key))
        for key, samples in sorted(timings.items())
    }


def is_playwright_initial_miss(message: str) -> bool:
    return message.startswith("no initial ")


def playwright_miss_reason(message: str) -> str:
    if message.startswith("playwright page closed"):
        return "PLAYWRIGHT_PAGE_CLOSED"
    if message.startswith("playwright timed out after"):
        return "PLAYWRIGHT_EXPECTED_MISS"
    return "PLAYWRIGHT_MISS"


def is_recent_stop_in_flight(ts: str, fop: str, timer_id: str) -> bool:
    tick_ms = ts_to_ms(ts)
    stop_ms = recent_stop_fired.get((fop, timer_id))
    if tick_ms is None or stop_ms is None:
        return False
    return 0 <= tick_ms - stop_ms <= args.stop_grace_millis


# ---- scan OWLCMS logs ----
for owlcms_log_path in owlcms_paths:
    try:
        with open(owlcms_log_path, encoding="utf-8", errors="replace") as fh:
            log_path = str(owlcms_log_path)
            for line_no, line in enumerate(fh, start=1):
                scan_order += 1
                line_order = scan_order
                if m := PRE_POST.search(line):
                    ts, fop, event, seq, timers, details = (
                        m.group(1), m.group(2), m.group(3),
                        int(m.group(4)), int(m.group(5)), m.group(6)
                    )
                    timer_refs = timer_refs_from_details(details)
                    key = (fop, seq, event)
                    if key not in events:
                        events[key] = {
                            "timerCount": timers,
                            "timerRefs": [
                                {"timerId": timer_id, "role": role}
                                for timer_id, role in sorted(timer_refs)
                            ],
                            "log": log_path,
                            "line": line_no,
                            "order": line_order,
                            "timestamp": ts,
                            "raw": line,
                        }
                    stats[f"prePost.{event}"] += 1

                if m := START_APPLY.search(line):
                    timer_ref = log_timer_ref(m)
                    applied_starts.add(timer_ref)
                    stats["startApply"] += 1
                    stats[f"startApply.{timer_ref[3]}"] += 1

                if m := STOP_APPLY.search(line):
                    timer_ref = log_timer_ref(m)
                    applied_stops.add(timer_ref)
                    stats["stopApply"] += 1
                    stats[f"stopApply.{timer_ref[3]}"] += 1

                if m := STOP_STALE.search(line):
                    stale_stops.add(log_timer_ref(m))
                    stats["stopStale"] += 1

                if m := STOP_FIRED.search(line):
                    ts_ms = ts_to_ms(m.group(1))
                    if ts_ms is not None:
                        recent_stop_fired[(m.group(2), m.group(4))] = ts_ms
                    stats["stopFired"] += 1

                if m := TICK_MISMATCH.search(line):
                    stats["tickMismatch.total"] += 1
                    ts, fop, timer_id, body = m.group(1), m.group(2), m.group(3), m.group(4)
                    role = role_from_timer_body(body)
                    if is_recent_stop_in_flight(ts, fop, timer_id):
                        stats["tickMismatch.suppressedInFlightStop"] += 1
                        suppressed_tick_mismatches.append({
                            "order": line_order,
                            "timestamp": ts,
                            "timestampMs": ts_to_ms(ts),
                            "fop": fop,
                            "role": role,
                            "timerId": timer_id,
                            "log": log_path,
                            "line": line_no,
                            "raw": line.strip(),
                            "reason": "IN_FLIGHT_STOP_GRACE",
                        })
                    else:
                        stats["tickMismatch.reported"] += 1
                        record_failure(line_order, ts, fop, role, "CLOCK_KEPT_RUNNING", log_path, line_no, line,
                                       {"timerId": timer_id, "timerBody": body})

                if m := OWLCMS_CLIENT_RENDER.search(line):
                    ts, fop, role, command, millis, sequence, seconds = (
                        m.group(1), m.group(2), m.group(3), m.group(4),
                        int(m.group(5)), int(m.group(6)), m.group(7)
                    )
                    record_clock_render_timing(line_order, ts, fop, role, command, millis, sequence, seconds,
                                               log_path, line_no, line)
    except FileNotFoundError:
        if owlcms_log_path == Path(args.owlcms_log):
            warnings.append(f"OWLCMS log not found: {owlcms_log_path}")

# ---- check apply coverage ----
for (fop, seq, event), event_record in events.items():
    if event_record["timerCount"] == 0:
        continue
    for timer_ref in event_record["timerRefs"]:
        timer_id = timer_ref["timerId"]
        role = timer_ref["role"]
        timer_key = (fop, seq, timer_id, role)
        if event == "StartTime" and timer_key not in applied_starts:
            record_failure(event_record["order"], event_record["timestamp"], fop, role, "MISSED_START",
                           event_record["log"], event_record["line"], event_record["raw"],
                           {"event": event, "sequence": seq, "timerId": timer_id})
        if event == "StopTime" and timer_key not in applied_stops:
            reason = "STALE_DROPPED_STOP" if timer_key in stale_stops else "MISSED_STOP"
            record_failure(event_record["order"], event_record["timestamp"], fop, role, reason,
                           event_record["log"], event_record["line"], event_record["raw"],
                           {"event": event, "sequence": seq, "timerId": timer_id})

# ---- scan Playwright log for first-class page-display misses ----
try:
    with open(playwright_path, encoding="utf-8", errors="replace") as fh:
        log_path = str(playwright_path)
        for line_no, line in enumerate(fh, start=1):
            scan_order += 1
            line_order = scan_order
            if m := PLAYWRIGHT_EXPECTING.search(line):
                ts, fop, role, display_text, seq = (
                    m.group(1), m.group(2), m.group(3), m.group(4).strip(), int(m.group(5))
                )
                key = f"{fop} {role}"
                prev = pending_expectation_by_fop_role.get(key)
                if prev is not None and not prev["confirmed"] and not prev["superseded"]:
                    stats["expectingNotConfirmed.total"] += 1
                    stats[f"expectingNotConfirmed.{role}"] += 1
                    record_failure(prev["order"], prev["timestamp"], fop, role, "EXPECTING_NOT_CONFIRMED",
                                   prev["log"], prev["line"], prev["raw"],
                                   {"sequence": prev["seq"], "expected": prev["display"],
                                    "nextSequence": seq, "nextExpectedAt": ts})
                pending_expectation_by_fop_role[key] = new_pending_expectation(
                    line_order, ts, fop, role, seq, f"{display_text} seq={seq}", log_path, line_no, line)

            if m := PLAYWRIGHT_SUPERCEDED.search(line):
                ts, fop, role, from_seq = m.group(1), m.group(2), m.group(3), int(m.group(4))
                prev = pending_expectation_by_fop_role.get(f"{fop} {role}")
                if prev is not None and prev["seq"] == from_seq and not prev["confirmed"]:
                    prev["superseded"] = True
                    stats["expectingSuperceded.total"] += 1
                    stats[f"expectingSuperceded.{role}"] += 1

            if m := PLAYWRIGHT_CONFIRMED.search(line):
                ts, fop, role, kind, millis, seq = (
                    m.group(1), m.group(2), m.group(3), m.group(4), int(m.group(5)), int(m.group(6))
                )
                prev = pending_expectation_by_fop_role.get(f"{fop} {role}")
                if prev is not None and prev["seq"] == seq:
                    if kind == "HEADER":
                        prev["header"] = millis
                    elif kind == "GRID":
                        prev["grid"] = millis
                    elif kind == "DISPLAY":
                        prev["displayMs"] = millis
                    if not prev["confirmed"] and pending_fully_confirmed(prev):
                        prev["confirmed"] = True
                        record_full_confirmation(prev, ts)

            if m := PLAYWRIGHT_MISS.search(line):
                ts, fop, role, message = m.group(1), m.group(2), m.group(3), m.group(4).strip()
                if is_playwright_initial_miss(message):
                    stats["playwrightInitialMiss.expected"] += 1
                    stats[f"playwrightInitialMiss.{role}"] += 1
                    record_expected_initial_miss(line_order, ts, fop, role, log_path, line_no, line, message)
                else:
                    stats["playwrightMiss.total"] += 1
                    stats[f"playwrightMiss.{role}"] += 1
                    record_missed_display_timing(fop, role, message)
                    record_failure(line_order, ts, fop, role, playwright_miss_reason(message), log_path, line_no, line,
                                   {"message": message})
except FileNotFoundError:
    warnings.append(f"Playwright log not found: {playwright_path}")

# ---- attach context evidence ----
all_context = []
for path in owlcms_paths:
    all_context.extend(load_context(path, OWLCMS_CONTEXT, "owlcms"))
all_context.extend(load_context(playwright_path, PLAYWRIGHT_CONTEXT, "playwright"))

for failure in failures:
    failure["context"] = context_for_failure(failure, all_context, window_ms)

failures.sort(key=failure_key)

by_reason = Counter(failure["reason"] for failure in failures)
by_fop_role = Counter(f"{failure['fop']} {failure['role']}" for failure in failures)
by_log = Counter(failure["log"] for failure in failures)
confirmed_name_stats = {
    "byRole": build_timing_stats(confirmed_name_timings_by_role, confirmed_name_max_by_role),
    "byFopRole": build_timing_stats(confirmed_name_timings_by_fop_role, confirmed_name_max_by_fop_role),
    "missesExcluded": {
        "byRole": build_timing_stats(missed_display_timings_by_role, {}),
        "byFopRole": build_timing_stats(missed_display_timings_by_fop_role, {}),
    },
}
clock_render_stats = {
    "byRole": build_timing_stats(clock_render_timings_by_role, clock_render_max_by_role),
    "byFopRole": build_timing_stats(clock_render_timings_by_fop_role, clock_render_max_by_fop_role),
    "playwrightObservationDelta": {
        "byRole": build_timing_stats(playwright_clock_delta_timings_by_role, playwright_clock_delta_max_by_role),
        "byFopRole": build_timing_stats(playwright_clock_delta_timings_by_fop_role,
                                         playwright_clock_delta_max_by_fop_role),
    },
}

summary = {
    "status": "failed" if failures else "ok",
    "failureCount": len(failures),
    "byReason": summarize_counter(by_reason),
    "byFopRole": summarize_counter(by_fop_role),
    "byLog": summarize_counter(by_log),
    "counters": dict(stats),
    "suppressedTickMismatchCount": len(suppressed_tick_mismatches),
    "expectedInitialMissCount": len(expected_initial_misses),
    "confirmedNameTimings": confirmed_name_stats,
    "clockRenderTimings": clock_render_stats,
}

evidence = {
    "schemaVersion": 1,
    "generatedAt": datetime.now(timezone.utc).isoformat(),
    "inputs": {
        "owlcmsLog": args.owlcms_log,
        "playwrightLog": args.playwright_log,
        "windowSeconds": args.window_seconds,
        "stopGraceMillis": args.stop_grace_millis,
        "maxContextLines": args.max_context_lines,
    },
    "scannedLogs": {
        "owlcms": [file_info(path) for path in owlcms_paths],
        "playwright": file_info(playwright_path),
    },
    "warnings": warnings,
    "summary": summary,
    "failures": failures,
    "expectedInitialMisses": expected_initial_misses,
    "suppressedTickMismatches": suppressed_tick_mismatches,
}

# ---- write evidence ----
evidence_path = Path(args.evidence_file)
evidence_path.parent.mkdir(parents=True, exist_ok=True)
evidence_path.write_text(json.dumps(evidence, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

# ---- human summary ----
summary_lines = []
summary_lines.append(f"Simulation log report {summary['status'].upper()}: {len(failures)} issue(s)")
summary_lines.append(f"Evidence file: {evidence_path}")
summary_lines.append("OWLCMS logs scanned:")
for info in evidence["scannedLogs"]["owlcms"]:
    marker = "ok" if info["exists"] else "missing"
    summary_lines.append(f"  {marker} {info['path']} ({info['bytes']} bytes)")
playwright_info = evidence["scannedLogs"]["playwright"]
summary_lines.append(f"Playwright log: {'ok' if playwright_info['exists'] else 'missing'} {playwright_info['path']} ({playwright_info['bytes']} bytes)")
if warnings:
    summary_lines.append("Warnings:")
    summary_lines.extend(f"  {warning}" for warning in warnings)
summary_lines.append("Summary by reason:")
if by_reason:
    summary_lines.extend(f"  {reason}: {count}" for reason, count in by_reason.most_common())
else:
    summary_lines.append("  none")
summary_lines.append("Summary by FOP/role:")
if by_fop_role:
    summary_lines.extend(f"  {fop_role}: {count}" for fop_role, count in by_fop_role.most_common())
else:
    summary_lines.append("  none")
summary_lines.append("Key counters:")
summary_lines.append(f"  tick mismatches total: {stats.get('tickMismatch.total', 0)}")
summary_lines.append(f"  tick mismatches suppressed as in-flight stop: {len(suppressed_tick_mismatches)}")
summary_lines.append(f"  tick mismatches reported: {stats.get('tickMismatch.reported', 0)}")
summary_lines.append(f"  playwright update misses total: {stats.get('playwrightMiss.total', 0)}")
summary_lines.append(f"  playwright announcer update misses: {stats.get('playwrightMiss.announcer', 0)}")
summary_lines.append(f"  playwright attempt update misses: {stats.get('playwrightMiss.attempt', 0)}")
summary_lines.append(f"  expected startup initial misses: {len(expected_initial_misses)}")
summary_lines.append(f"  expecting not confirmed before next expecting: {stats.get('expectingNotConfirmed.total', 0)} "
                     f"(announcer={stats.get('expectingNotConfirmed.announcer', 0)} attempt={stats.get('expectingNotConfirmed.attempt', 0)})")
summary_lines.append(f"  expecting superseded (excused): {stats.get('expectingSuperceded.total', 0)} "
                     f"(announcer={stats.get('expectingSuperceded.announcer', 0)} attempt={stats.get('expectingSuperceded.attempt', 0)})")
summary_lines.append(f"  full confirmations by seq: {stats.get('playwrightConfirmed.total', 0)} "
                     f"(announcer={stats.get('playwrightConfirmed.announcer', 0)} attempt={stats.get('playwrightConfirmed.attempt', 0)})")
summary_lines.append("Playwright full-confirmation times by seq (announcer=HEADER+GRID, attempt=DISPLAY; misses excluded):")
role_timings = confirmed_name_stats["byRole"]
if role_timings:
    for role in ("announcer", "attempt"):
        if role in role_timings:
            timing = role_timings[role]
            max_event = timing.get("maxEvent") or {}
            summary_lines.append(
                f"  {role}: count={timing['count']} avg={timing['averageMs']:.1f}ms "
                f"max={timing['maxMs']}ms stdDev={timing['stdDevMs']:.1f}ms "
                f"maxAt={max_event.get('timestamp', '?')} {max_event.get('fop', '?')}:{max_event.get('line', '?')} "
                f"buckets[{format_timing_buckets(timing['buckets'])}]"
            )
else:
    summary_lines.append("  none")
summary_lines.append("OWLCMS display timer client-render clock delays:")
clock_role_timings = clock_render_stats["byRole"]
for role in ("announcer", "attempt"):
    if role in clock_role_timings:
        timing = clock_role_timings[role]
        max_event = timing.get("maxEvent") or {}
        summary_lines.append(
            f"  {role}: count={timing['count']} avg={timing['averageMs']:.1f}ms "
            f"max={timing['maxMs']}ms stdDev={timing['stdDevMs']:.1f}ms "
            f"maxAt={max_event.get('timestamp', '?')} {max_event.get('fop', '?')}:{max_event.get('line', '?')} "
            f"buckets[{format_timing_buckets(timing['buckets'])}]"
        )
    elif stats.get(f"startApply.{role}", 0):
        summary_lines.append(
            f"  {role}: no client-render samples found "
            f"({stats.get(f'startApply.{role}', 0)} OWLCMS start applications were present)"
        )
    else:
        summary_lines.append(f"  {role}: no samples")
summary_lines.append("Playwright confirmation delta after OWLCMS clock render:")
delta_role_timings = clock_render_stats["playwrightObservationDelta"]["byRole"]
if delta_role_timings:
    for role in ("announcer", "attempt"):
        if role in delta_role_timings:
            timing = delta_role_timings[role]
            max_event = timing.get("maxEvent") or {}
            clock_event = max_event.get("clockRenderEvent") or {}
            summary_lines.append(
                f"  {role}: count={timing['count']} avg={timing['averageMs']:.1f}ms "
                f"max={timing['maxMs']}ms stdDev={timing['stdDevMs']:.1f}ms "
                f"maxAt={max_event.get('timestamp', '?')} {max_event.get('fop', '?')}:{clock_event.get('line', '?')} "
                f"buckets[{format_timing_buckets(timing['buckets'])}]"
            )
else:
    summary_lines.append("  none")
excluded_timings = confirmed_name_stats["missesExcluded"]["byRole"]
if excluded_timings:
    summary_lines.append("Playwright missed display times excluded from max:")
    for role in ("announcer", "attempt"):
        if role in excluded_timings:
            timing = excluded_timings[role]
            summary_lines.append(
                f"  {role}: count={timing['count']} avg={timing['averageMs']:.1f}ms "
                f"max={timing['maxMs']}ms stdDev={timing['stdDevMs']:.1f}ms"
            )

if failures:
    summary_lines.append(f"Issues ({len(failures)}):")
    for failure in failures:
        summary_lines.append(
            f"  FAIL {failure['timestamp']} {failure['fop']} {failure['role']} {failure['reason']} "
            f"{failure['log']}:{failure['line']}"
        )
else:
    summary_lines.append("No failures found.")

summary_text = "\n".join(summary_lines) + "\n"
print(summary_text, end="")
if args.summary_file:
    summary_path = Path(args.summary_file)
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.write_text(summary_text, encoding="utf-8")
