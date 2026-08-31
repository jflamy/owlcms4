#!/bin/bash
# Validates ATTEMPT_TRACES output after a simulation run against the atomic
# attempt-board snapshot invariants (branch atomicAttempts).
# Usage: scripts/check-attempt-traces.sh [logfile...]  (default: owlcms/logs/*.log)

set -u

if [ "$#" -gt 0 ]; then
  logs=("$@")
else
  logs=(owlcms/logs/*.log)
fi

if [ ! -e "${logs[0]}" ]; then
  echo "FAIL: no log files found (${logs[*]})"
  exit 1
fi

fail=0

published=$(grep -h "attemptBoard state published" "${logs[@]}" | wc -l | tr -d ' ')
rendered=$(grep -h "attemptBoard weight rendered" "${logs[@]}" | wc -l | tr -d ' ')
echo "snapshots published: $published, frames rendered: $rendered"
if [ "$published" -eq 0 ]; then
  echo "FAIL: no snapshots published - is ATTEMPT_TRACES enabled and an attempt board attached?"
  fail=1
fi

# A hidden weight node still has textContent "kg"; only a visible row is a failure.
barekg=$(grep -h "attemptBoard weight rendered" "${logs[@]}" | grep -c "rendered=kg .*weightVisible=true")
if [ "$barekg" -gt 0 ]; then
  echo "FAIL: $barekg bare-kg frame(s):"
  grep -h "attemptBoard weight rendered" "${logs[@]}" | grep "rendered=kg .*weightVisible=true" | head -10
  fail=1
else
  echo "OK: no bare-kg frames"
fi

legacyRendered=$(grep -h "attemptBoard weight rendered" "${logs[@]}" | grep -vc "weightVisible=")
if [ "$legacyRendered" -gt 0 ]; then
  echo "WARN: $legacyRendered legacy frame trace(s) omit weightVisible and cannot be checked for bare kg"
fi

invariants=$(grep -hc "attemptBoard cannot publish" "${logs[@]}" | awk '{s+=$1} END {print s}')
if [ "$invariants" -gt 0 ]; then
  echo "FAIL: $invariants backend invariant violation(s):"
  grep -h "attemptBoard cannot publish" "${logs[@]}" | head -10
  fail=1
else
  echo "OK: no invariant violations"
fi

mismatches=$(grep -hc "MISMATCH" "${logs[@]}" | awk '{s+=$1} END {print s}')
if [ "$mismatches" -gt 0 ]; then
  # leads, not proof: async delivery can lag the FOP state
  echo "WARN: $mismatches decisionVisible/FOP-state mismatch(es) (investigate whereFrom):"
  grep -h "MISMATCH" "${logs[@]}" | head -10
else
  echo "OK: no decision-visibility mismatches"
fi

# rendered sequences must be strictly increasing (client guard drops stale snapshots)
nonmono=$(grep -h "attemptBoard weight rendered" "${logs[@]}" \
  | sed -n 's/.*rendered seq=\([0-9][0-9]*\).*/\1/p' \
  | awk 'NR>1 && $1<=prev {print "  seq " $1 " after " prev} {prev=$1}')
if [ -n "$nonmono" ]; then
  echo "FAIL: non-monotonic rendered sequences:"
  echo "$nonmono" | head -10
  fail=1
else
  echo "OK: rendered sequences monotonic"
fi

if [ "$fail" -eq 0 ]; then
  echo "PASS"
else
  echo "FAILURES DETECTED"
fi
exit "$fail"
