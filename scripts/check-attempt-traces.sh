#!/bin/bash
# Validates ATTEMPT_TRACES output after a simulation run against the atomic
# attempt-board snapshot invariants (branch atomicAttempts).
# Usage: scripts/check-attempt-traces.sh [logfile...]  (default: owlcms/logs/*.log)

set -u

if [ "$#" -gt 0 ]; then
  logs=("$@")
else
  # rotated owlcms_YYYY-MM-DD.log files sort chronologically; the live file is newest
  logs=()
  for rotated in owlcms/logs/owlcms_*.log; do
    [ -e "$rotated" ] && logs+=("$rotated")
  done
  [ -e owlcms/logs/owlcms.log ] && logs+=(owlcms/logs/owlcms.log)
fi

if [ "${#logs[@]}" -eq 0 ] || [ ! -e "${logs[0]}" ]; then
  echo "FAIL: no log files found (${logs[*]})"
  exit 1
fi

fail=0
inconclusive=0

published=$(grep -h "attemptBoard state published" "${logs[@]}" | wc -l | tr -d ' ')
rendered=$(grep -h "attemptBoard weight rendered" "${logs[@]}" | wc -l | tr -d ' ')
echo "snapshots published: $published, frames rendered: $rendered"
if [ "$published" -eq 0 ]; then
  echo "INCONCLUSIVE: no snapshots published - is ATTEMPT_TRACES enabled and an attempt board attached?"
  inconclusive=1
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
  inconclusive=1
fi

# Correlate each attempt-board browser render to the lifting-order event that
# published its snapshot. Browser trace logs can arrive after a later event.
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
/displayOrder post seq=/ {
  fop = $4
  event = value("seq=")
  if (eventSequence[fop] != "" && event + 0 <= eventSequence[fop] + 0) generation[fop]++
  eventSequence[fop] = event
  events[fop, generation[fop], event] = 1
  next
}
/attemptBoard state published seq=/ {
  fop = $4
  snapshotEvent[fop, generation[fop], "", value("seq=")] = eventSequence[fop]
  next
}
/attemptBoard order published board=/ {
  fop = $4
  snapshotEvent[fop, generation[fop], value("board="), value("snapshotSeq=")] = value("eventSeq=")
  next
}
/scoreboard top rendered/ {
  fop = $4
  event = value("eventSeq=")
  scoreboardCount[fop, generation[fop], event]++
  scoreboardStartNumber[fop, generation[fop], event] = value("startNumber=")
  scoreboardWeight[fop, generation[fop], event] = value("weight=")
  scoreboardMode[fop, generation[fop], event] = value("mode=")
  scoreboardWeightVisible[fop, generation[fop], event] = value("weightVisible=")
  next
}
/attemptBoard weight rendered/ {
  fop = $4
  event = snapshotEvent[fop, generation[fop], value("board="), value("seq=")]
  if (event == "") {
    unmappedAttempt[fop]++
  } else {
    attemptCount[fop, generation[fop], event]++
    snapshotIndex = attemptCount[fop, generation[fop], event]
    attemptStartNumber[fop, generation[fop], event, snapshotIndex] = value("startNumber=")
    attemptWeight[fop, generation[fop], event, snapshotIndex] = value("weight=")
    attemptMode[fop, generation[fop], event, snapshotIndex] = value("mode=")
    attemptWeightVisible[fop, generation[fop], event, snapshotIndex] = value("weightVisible=")
  }
  next
}
END {
  failures = 0
  print "snapshot-to-event render correlation:"
  for (entry in events) {
    split(entry, key, SUBSEP)
    fop = key[1]
    eventGeneration = key[2]
    event = key[3]
    if (scoreboardCount[fop, eventGeneration, event] > 0 && attemptCount[fop, eventGeneration, event] > 0) {
      compared[fop]++
      for (i = 1; i <= attemptCount[fop, eventGeneration, event]; i++) {
        if (scoreboardWeightVisible[fop, eventGeneration, event] == "true" && attemptWeightVisible[fop, eventGeneration, event, i] == "true" &&
            normalizedWeight(attemptWeight[fop, eventGeneration, event, i]) != normalizedWeight(scoreboardWeight[fop, eventGeneration, event])) {
          weightMismatch[fop]++
          failures++
          if (detailsShown++ < 10) printf "FAIL: FOP %s eventSeq=%s weight scoreboard=%s attempt=%s\n", fop, event, scoreboardWeight[fop, eventGeneration, event], attemptWeight[fop, eventGeneration, event, i]
        }
        if (scoreboardMode[fop, eventGeneration, event] == "CURRENT_ATHLETE" && attemptMode[fop, eventGeneration, event, i] == "CURRENT_ATHLETE" &&
            attemptStartNumber[fop, eventGeneration, event, i] != scoreboardStartNumber[fop, eventGeneration, event]) {
          startNumberMismatch[fop]++
          failures++
          if (detailsShown++ < 10) printf "FAIL: FOP %s eventSeq=%s startNumber scoreboard=%s attempt=%s\n", fop, event, scoreboardStartNumber[fop, eventGeneration, event], attemptStartNumber[fop, eventGeneration, event, i]
        }
      }
    }
  }
  for (fop in eventSequence) {
    printf "  %s: compared=%d framesWithoutDirectOrderLink=%d startMismatch=%d weightMismatch=%d\n", \
      fop, compared[fop] + 0, unmappedAttempt[fop] + 0, startNumberMismatch[fop] + 0, weightMismatch[fop] + 0
  }
  exit failures > 0
}
' "${logs[@]}"; then
  fail=1
fi

instrumented=$(grep -hc "attemptBoard order received board=" "${logs[@]}" | awk '{s+=$1} END {print s}')
if [ "$instrumented" -gt 0 ]; then
  if awk '
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
function millis(clock, parts) {
  split(clock, parts, ":")
  return ((parts[1] * 60 + parts[2]) * 60 + parts[3]) * 1000
}
function elapsed(start, finish) {
  return (finish - start + 86400000) % 86400000
}
function numericWeight(weight) {
  gsub(/[^0-9.-]/, "", weight)
  return weight
}
function detail(message) {
  if (detailsShown++ < 20) print message
}
/entering BREAK.FIRST_CJ / {
  cjBreak[$4, generation[$4], lastEvent[$4]] = 1
  next
}
/state INACTIVE, event received StartLifting / {
  groupStartFrom[$4, generation[$4]] = lastEvent[$4]
  next
}
/displayOrder post seq=/ {
  fop = $4
  event = value("seq=")
  if (lastEvent[fop] != "" && event + 0 <= lastEvent[fop] + 0) generation[fop]++
  lastEvent[fop] = event
  eventKey = fop SUBSEP generation[fop] SUBSEP event
  posted[eventKey] = 1
  postedAt[eventKey] = millis($1)
  expectedStart[eventKey] = value("startNumber=")
  expectedWeight[eventKey] = value("weight=")
  expectedAthlete[eventKey] = value("athlete=")
  eventState[eventKey] = value("state=")
  previousGroupOrder = groupStartFrom[fop, generation[fop]]
  if (previousGroupOrder != "") {
    if (eventState[eventKey] == "CURRENT_ATHLETE_DISPLAYED") groupStartTo[fop, generation[fop], previousGroupOrder] = event
    delete groupStartFrom[fop, generation[fop]]
  }
  currentDisplay[eventKey] = value("state=") == "CURRENT_ATHLETE_DISPLAYED" || value("state=") == "TIME_RUNNING" || value("state=") == "TIME_STOPPED"
  affected[eventKey] = value("displayAffected=") == "true"
  next
}
/attemptBoard order received board=/ {
  board = $4 SUBSEP generation[$4] SUBSEP value("board=")
  event = value("seq=")
  if (value("attached=") == "true") {
    received[board, event] = 1
    boards[board] = 1
    if (!(board in firstReceived) || event + 0 < firstReceived[board]) firstReceived[board] = event + 0
    if (event + 0 > lastReceived[board]) lastReceived[board] = event + 0
  }
  next
}
/attemptBoard state published board=/ {
  boardId = value("board=")
  snapshot = value("seq=") + 0
  board = $4 SUBSEP generation[$4] SUBSEP boardId
  snapshotBoard[$4, boardId, snapshot] = board
  publicationAt[board, snapshot] = millis($1)
  if (!(board in firstSnapshot) || snapshot < firstSnapshot[board]) firstSnapshot[board] = snapshot
  if (snapshot > lastSnapshot[board]) lastSnapshot[board] = snapshot
  next
}
/attemptBoard order published board=/ {
  board = $4 SUBSEP generation[$4] SUBSEP value("board=")
  event = value("eventSeq=")
  snapshot = value("snapshotSeq=") + 0
  published[board, event] = snapshot
  snapshotOrder[board, snapshot] = event
  next
}
/attemptBoard order superseded board=/ {
  superseded[$4, generation[$4], value("board="), value("eventSeq=")] = value("byEventSeq=")
  next
}
/attemptBoard order deferred board=/ {
  deferred[$4, generation[$4], value("board="), value("eventSeq=")] = 1
  next
}
/dropping out-of-order LiftingOrderUpdated board=/ {
  stale[$4, generation[$4], value("board="), value("seq=")] = 1
  next
}
/attemptBoard weight rendered board=/ {
  boardId = value("board=")
  snapshot = value("seq=") + 0
  board = snapshotBoard[$4, boardId, snapshot]
  if (board == "") board = $4 SUBSEP generation[$4] SUBSEP boardId
  acknowledged[board, snapshot] = 1
  if (!((board SUBSEP snapshot) in acknowledgedAt)) {
    acknowledgedAt[board, snapshot] = millis($1)
    if ((board SUBSEP snapshot) in publicationAt) {
      ackDelay = elapsed(publicationAt[board, snapshot], acknowledgedAt[board, snapshot])
      ackDelaySum[board] += ackDelay
      ackDelayCount[board]++
      if (ackDelay > maxAckObserved[board]) maxAckObserved[board] = ackDelay
    }
  }
  if (snapshot > latestRendered[board]) latestRendered[board] = snapshot
  frame = ++frameCount[board, snapshot]
  frameStart[board, snapshot, frame] = value("startNumber=")
  frameWeight[board, snapshot, frame] = value("weight=")
  frameText[board, snapshot, frame] = value("rendered=")
  frameMode[board, snapshot, frame] = value("mode=")
  frameVisible[board, snapshot, frame] = value("weightVisible=")
  if (value("clientTime=") != "") {
    if (!(board in clientOrigin)) clientOrigin[board] = millis(value("clientTime="))
    clientTime = millis(value("clientTime=")) - clientOrigin[board]
    if (clientTime < -43200000) clientTime += 86400000
    if (clientTime > 43200000) clientTime -= 86400000
    if (!((board SUBSEP snapshot) in firstClient) || clientTime < firstClient[board, snapshot]) firstClient[board, snapshot] = clientTime
    if (!((board SUBSEP snapshot) in lastClient) || clientTime > lastClient[board, snapshot]) lastClient[board, snapshot] = clientTime
  } else {
    missingClientTime[board]++
    incomplete = 1
  }
  next
}
END {
  print "order-event lifecycle coverage:"
  for (entry in received) {
    split(entry, key, SUBSEP)
    board = key[1] SUBSEP key[2] SUBSEP key[3]
    event = key[4]
    label = key[1] " " key[3] " eventSeq=" event
    if (published[entry] != "") {
      snapshot = published[entry] + 0
      if (acknowledged[board, snapshot]) {
        renderedCount[board]++
        if ((board SUBSEP snapshot) in publicationAt) {
          delay = elapsed(publicationAt[board, snapshot], acknowledgedAt[board, snapshot])
          if (delay > maxAckDelay[board]) maxAckDelay[board] = delay
        }
        eventKey = key[1] SUBSEP key[2] SUBSEP event
        if (eventKey in postedAt) {
          delay = elapsed(postedAt[eventKey], acknowledgedAt[board, snapshot])
          if (delay > maxEventAckDelay[board]) {
            maxEventAckDelay[board] = delay
            slowestEvent[board] = event
          }
        }
      } else {
        pendingRender[board]++
        eventKey = key[1] SUBSEP key[2] SUBSEP event
        successor = 0
        for (candidate = snapshot + 1; candidate <= lastSnapshot[board]; candidate++) {
          if (acknowledged[board, candidate]) { successor = candidate; break }
        }
        avgAck = ackDelayCount[board] ? ackDelaySum[board] / ackDelayCount[board] : 0
        if (successor) {
          interval = elapsed(publicationAt[board, snapshot], publicationAt[board, successor])
          mode = frameMode[board, successor, 1]
          wantStart = mode == "LIFT_COUNTDOWN" ? "0" : expectedStart[eventKey]
          sameValues = frameVisible[board, successor, 1] == "true" && frameStart[board, successor, 1] == wantStart && numericWeight(frameText[board, successor, 1]) == numericWeight(expectedWeight[eventKey]) && numericWeight(frameWeight[board, successor, 1]) == numericWeight(expectedWeight[eventKey])
          nextOrder = snapshotOrder[board, successor]
          nextEventKey = key[1] SUBSEP key[2] SUBSEP nextOrder
          transition = ""
          if (sameValues && mode == "LIFT_COUNTDOWN" && cjBreak[eventKey] && nextOrder == "") {
            sameCycle = 1
            for (candidate = snapshot + 1; candidate <= successor; candidate++) {
              if (snapshotOrder[board, candidate] != "") sameCycle = 0
            }
            if (sameCycle) transition = "cjBreak"
          }
          if (sameValues && mode == "CURRENT_ATHLETE" && eventState[eventKey] == "INACTIVE" &&
              nextOrder != "" && nextOrder == groupStartTo[eventKey] &&
              expectedAthlete[nextEventKey] == expectedAthlete[eventKey] &&
              expectedStart[nextEventKey] == expectedStart[eventKey] &&
              expectedWeight[nextEventKey] == expectedWeight[eventKey]) transition = "groupStart"
          if (transition != "") {
            expectedTransitions[board, transition]++
            replacedSnapshot[board, snapshot] = 1
            detail("EXPECTED: " label " snapshotSeq=" snapshot " transition=" transition " successorSeq=" successor " mode=" mode " weight=" frameText[board, successor, 1] "; initial snapshot not individually acknowledged")
          } else {
            unexplainedMissing[board]++
            incomplete = 1
            detail("SUSPECT: " label " snapshotSeq=" snapshot " never rendered; next rendered snapshotSeq=" successor " after " interval "ms (avgAck=" int(avgAck) "ms maxAck=" maxAckObserved[board] "ms) mode=" mode " start=" frameStart[board, successor, 1] " weight=" frameText[board, successor, 1] " expected " expectedStart[eventKey] "/" expectedWeight[eventKey])
          }
        } else {
          unexplainedMissing[board]++
          incomplete = 1
          detail("UNCONFIRMED: " label " snapshotSeq=" snapshot " never rendered and no later snapshot rendered on this board")
        }
      }
    } else if (superseded[entry]) {
      resolvedSuperseded[board]++
    } else if (stale[entry]) {
      resolvedStale[board]++
    } else if (deferred[entry]) {
      pendingDeferred[board]++
      incomplete = 1
    } else {
      missingOutcome[board]++
      incomplete = 1
      detail("PENDING: " label " has no recorded outcome yet")
    }
  }
  for (board in boards) {
    boardCount++
    split(board, key, SUBSEP)
    label = key[1] " " key[3] " generation=" key[2]
    for (eventKey in posted) {
      split(eventKey, eventParts, SUBSEP)
      event = eventParts[3]
      if (eventParts[1] != key[1] || eventParts[2] != key[2] || !affected[eventKey]) continue
      if (event + 0 > firstReceived[board] && event + 0 < lastReceived[board] && !received[board, event]) {
        receiptGaps[board]++
        incomplete = 1
        detail("UNCONFIRMED: " label " eventSeq=" event " posted but not received between observed receipts; attachment continuity unknown")
      }
    }
    cycle = ""
    previousClient = ""
    previousSnapshot = ""
    for (snapshot = firstSnapshot[board]; snapshot <= lastSnapshot[board]; snapshot++) {
      if (snapshotOrder[board, snapshot] != "") {
        cycle = snapshotOrder[board, snapshot]
        cycleSnapshot = snapshot
        eventKey = key[1] SUBSEP key[2] SUBSEP cycle
        if (lastOrder != "" && cycle + 0 < lastOrder + 0) {
          orderRegressions[board]++
          failures++
          detail("FAIL: " label " eventSeq=" cycle " published after eventSeq=" lastOrder)
        }
        lastOrder = cycle
      }
      if ((board SUBSEP snapshot) in firstClient) {
        if (previousClient != "" && firstClient[board, snapshot] < previousClient) {
          regressions[board]++
          incomplete = 1
          detail("SUSPECT: " label " older snapshotSeq=" previousSnapshot " rendered after newer snapshotSeq=" snapshot " by client timestamps; verify client clock stability")
        }
        if (previousClient == "" || lastClient[board, snapshot] > previousClient) {
          previousClient = lastClient[board, snapshot]
          previousSnapshot = snapshot
        }
      }
      for (frame = 1; frame <= frameCount[board, snapshot]; frame++) {
        if ((frameMode[board, snapshot, frame] != "CURRENT_ATHLETE" && frameMode[board, snapshot, frame] != "LIFT_COUNTDOWN") || frameVisible[board, snapshot, frame] != "true") {
          nonAthleteFrames[board]++
          cycle = ""
          continue
        }
        if (cycle == "" || !currentDisplay[eventKey] || deferred[board, cycle]) {
          # published by syncWithFOP/StartLifting rather than a LiftingOrderUpdated; no cycle to compare against
          unclassifiedFrames[board]++
          continue
        }
        cycleFrames[board]++
        expectedFrameStart = frameMode[board, snapshot, frame] == "LIFT_COUNTDOWN" ? "0" : expectedStart[eventKey]
        if (frameStart[board, snapshot, frame] != expectedFrameStart ||
            numericWeight(frameWeight[board, snapshot, frame]) != numericWeight(expectedWeight[eventKey]) ||
            numericWeight(frameText[board, snapshot, frame]) != numericWeight(expectedWeight[eventKey])) {
          cycleMismatch[board]++
          failures++
          detail("FAIL: " label " eventSeq=" cycle " snapshotSeq=" snapshot " mode=" frameMode[board, snapshot, frame] " expectedStart=" expectedFrameStart " expectedWeight=" expectedWeight[eventKey] " renderedStart=" frameStart[board, snapshot, frame] " renderedWeight=" frameText[board, snapshot, frame])
        }
        if (snapshot != cycleSnapshot && !acknowledged[board, cycleSnapshot] && !replacedSnapshot[board, cycleSnapshot] && !lateCycle[board, cycleSnapshot]) {
          lateCycle[board, cycleSnapshot] = 1
          successorWithoutStart[board]++
          incomplete = 1
          detail("UNCONFIRMED: " label " eventSeq=" cycle " successor snapshotSeq=" snapshot " rendered without cycle-start snapshotSeq=" cycleSnapshot " acknowledgement; not assumed coalesced")
        }
      }
    }
    lastOrder = ""
    printf "  %s: exactOrderAcks=%d superseded=%d stale=%d pendingDeferred=%d missingExactAck=%d missingOutcome=%d receiptGaps=%d\n", \
      label, renderedCount[board] + 0, resolvedSuperseded[board] + 0, resolvedStale[board] + 0, \
      pendingDeferred[board] + 0, pendingRender[board] + 0, missingOutcome[board] + 0, receiptGaps[board] + 0
    printf "    cycleFrames=%d cycleMismatch=%d orderRegressions=%d clientOrderSuspects=%d successorWithoutStartAck=%d nonAthleteFrames=%d framesOutsideOrderCycle=%d missingClientTime=%d\n", \
      cycleFrames[board] + 0, cycleMismatch[board] + 0, orderRegressions[board] + 0, regressions[board] + 0, successorWithoutStart[board] + 0, \
      nonAthleteFrames[board] + 0, unclassifiedFrames[board] + 0, missingClientTime[board] + 0
    printf "    avgPublishToAckMs=%d maxPublishToAckMs=%d maxEventToAckMs=%d slowestEventSeq=%s\n", \
      ackDelayCount[board] ? ackDelaySum[board] / ackDelayCount[board] : 0, maxAckDelay[board] + 0, maxEventAckDelay[board] + 0, slowestEvent[board]
    printf "    missingExactAck: expectedCJBreak=%d expectedGroupStart=%d unexplained=%d\n", \
      expectedTransitions[board, "cjBreak"] + 0, expectedTransitions[board, "groupStart"] + 0, unexplainedMissing[board] + 0
  }
  print "INFO: acknowledgement latency includes transport/callback delay, not just rendering; equal client timestamps cannot establish render order"
  if (failures) exit 1
  if (incomplete || boardCount == 0) {
    print "INCONCLUSIVE: unresolved delivery, ordering, or cycle evidence; see counters above"
    exit 3
  }
  print "INFO: superseded events are not counted as rendered; no coalescing is inferred"
  exit 0
}
' "${logs[@]}"; then
    :
  else
    audit_result=$?
    if [ "$audit_result" -eq 3 ]; then
      inconclusive=1
    else
      fail=1
    fi
  fi
else
  echo "INCONCLUSIVE: no explicit order-event lifecycle traces; end-to-end coverage cannot be established"
  inconclusive=1
fi

renderFailures=$(grep -hcE "SIMULATION_RENDER_(MISMATCH|TIMEOUT)" "${logs[@]}" | awk '{s+=$1} END {print s}')
if [ "$renderFailures" -gt 0 ]; then
  echo "FAIL: $renderFailures simulation render mismatch/timeout record(s)"
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
  | sed -n 's/.*rendered .*seq=\([0-9][0-9]*\).*/\1/p' \
  | awk 'NR>1 && $1<=prev {print "  seq " $1 " after " prev} {prev=$1}')
if [ -n "$nonmono" ]; then
  echo "INFO: combined render stream is non-monotonic across independent clients:"
  echo "$nonmono" | head -10
else
  echo "OK: combined rendered sequences monotonic"
fi

if [ "$fail" -ne 0 ]; then
  echo "FAILURES DETECTED"
elif [ "$inconclusive" -ne 0 ]; then
  echo "INCONCLUSIVE: no confirmed failures, but coverage is incomplete"
else
  echo "PASS"
fi
exit "$fail"
