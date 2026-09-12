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

# Correlate browser renders within each FOP's interval between consecutive
# lifting-order updates. The final open interval is intentionally excluded.
if ! awk '
function value(prefix,   i, result) {
  for (i = 1; i <= NF; i++) {
    if (index($i, prefix) == 1) {
      result = $i
      sub(prefix, "", result)
      return result
    }
  }
  return ""
}
function normalizedWeight(result) {
  gsub(/[^0-9.-]/, "", result)
  return result
}
function finish(fop,   i, valid) {
  if (!active[fop]) return

  closed[fop]++
  valid = 1
  if (scoreboardCount[fop] != 1) {
    if (scoreboardCount[fop] == 0) missingScoreboard[fop]++
    else multipleScoreboards[fop]++
    valid = 0
  }
  if (attemptCount[fop] < 1) {
    missingAttempt[fop]++
    valid = 0
  }

  if (scoreboardCount[fop] == 1) {
    for (i = 1; i <= attemptCount[fop]; i++) {
      if (scoreboardWeightVisible[fop] == "true" && attemptWeightVisible[fop, i] == "true" &&
          normalizedWeight(attemptWeight[fop, i]) != normalizedWeight(scoreboardWeight[fop])) {
        weightMismatch[fop]++
        valid = 0
      }
      if (scoreboardMode[fop] == "CURRENT_ATHLETE" && attemptMode[fop, i] == "CURRENT_ATHLETE" &&
          attemptStartNumber[fop, i] != scoreboardStartNumber[fop]) {
        startNumberMismatch[fop]++
        valid = 0
      }
    }
  }

  if (valid) {
    passed[fop]++
  } else if (detailsShown < 10) {
    printf "FAIL: FOP %s eventSeq=%s scoreboardRenders=%d attemptRenders=%d\n", \
      fop, eventSequence[fop], scoreboardCount[fop], attemptCount[fop]
    detailsShown++
  }
}
/displayOrder post seq=/ {
  fop = $4
  finish(fop)
  active[fop] = 1
  eventSequence[fop] = value("seq=")
  scoreboardCount[fop] = 0
  attemptCount[fop] = 0
  next
}
/scoreboard top rendered/ {
  fop = $4
  if (active[fop]) {
    scoreboardCount[fop]++
    scoreboardStartNumber[fop] = value("startNumber=")
    scoreboardWeight[fop] = value("weight=")
    scoreboardMode[fop] = value("mode=")
    scoreboardWeightVisible[fop] = value("weightVisible=")
  }
  next
}
/attemptBoard weight rendered/ {
  fop = $4
  if (active[fop]) {
    attemptCount[fop]++
    attemptStartNumber[fop, attemptCount[fop]] = value("startNumber=")
    attemptWeight[fop, attemptCount[fop]] = value("weight=")
    attemptMode[fop, attemptCount[fop]] = value("mode=")
    attemptWeightVisible[fop, attemptCount[fop]] = value("weightVisible=")
  }
  next
}
END {
  failures = 0
  print "lifting-order render correlation:"
  for (fop in active) {
    failures += missingScoreboard[fop] + multipleScoreboards[fop] + missingAttempt[fop] + \
      startNumberMismatch[fop] + weightMismatch[fop]
    printf "  %s: closed=%d pass=%d missingScoreboard=%d multipleScoreboards=%d missingAttempt=%d startMismatch=%d weightMismatch=%d openExcluded=1\n", \
      fop, closed[fop] + 0, passed[fop] + 0, missingScoreboard[fop] + 0, \
      multipleScoreboards[fop] + 0, missingAttempt[fop] + 0, \
      startNumberMismatch[fop] + 0, weightMismatch[fop] + 0
  }
  exit failures > 0
}
' "${logs[@]}"; then
  fail=1
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

# The combined log interleaves independent attempt-board clients and FOPs, so
# sequence changes in this global stream cannot prove a stale client render.
nonmono=$(grep -h "attemptBoard weight rendered" "${logs[@]}" \
  | sed -n 's/.*rendered seq=\([0-9][0-9]*\).*/\1/p' \
  | awk 'NR>1 && $1<=prev {print "  seq " $1 " after " prev} {prev=$1}')
if [ -n "$nonmono" ]; then
  echo "INFO: combined render stream is non-monotonic across independent clients:"
  echo "$nonmono" | head -10
else
  echo "OK: combined rendered sequences monotonic"
fi

if [ "$fail" -eq 0 ]; then
  echo "PASS"
else
  echo "FAILURES DETECTED"
fi
exit "$fail"
