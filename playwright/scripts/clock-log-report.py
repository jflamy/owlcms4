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
import re
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

DEFAULT_EVIDENCE_FILE = "playwright/logs/clock-log-evidence.json"
DEFAULT_SUMMARY_FILE = "playwright/logs/clock-log-summary.txt"

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
                    help="Exit 0 even if clock issues are found")
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
                    applied_starts.add(log_timer_ref(m))
                    stats["startApply"] += 1

                if m := STOP_APPLY.search(line):
                    applied_stops.add(log_timer_ref(m))
                    stats["stopApply"] += 1

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
            if m := PLAYWRIGHT_MISS.search(line):
                ts, fop, role, message = m.group(1), m.group(2), m.group(3), m.group(4).strip()
                if is_playwright_initial_miss(message):
                    stats["playwrightInitialMiss.expected"] += 1
                    stats[f"playwrightInitialMiss.{role}"] += 1
                    record_expected_initial_miss(line_order, ts, fop, role, log_path, line_no, line, message)
                else:
                    stats["playwrightMiss.total"] += 1
                    stats[f"playwrightMiss.{role}"] += 1
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

summary = {
    "status": "failed" if failures else "ok",
    "failureCount": len(failures),
    "byReason": summarize_counter(by_reason),
    "byFopRole": summarize_counter(by_fop_role),
    "byLog": summarize_counter(by_log),
    "counters": dict(stats),
    "suppressedTickMismatchCount": len(suppressed_tick_mismatches),
    "expectedInitialMissCount": len(expected_initial_misses),
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

if failures and not args.no_fail_on_issues:
    sys.exit(1)
