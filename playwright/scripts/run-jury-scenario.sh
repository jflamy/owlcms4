#!/usr/bin/env bash
# run-jury-scenario.sh — run the jury review scenarios against owlcms and write an HTML report with screenshots
# under playwright/reports/. Options (all optional):
#   --server   start a dedicated owlcms for the run and stop it at the end. It runs in memory, loaded from
#              playwright/fixtures/jury-scenarios.json, so every run starts from the same data: session M1, and the
#              single-athlete sessions M2, M4 and M6 (next lift: last snatch), M3, M5 and M7 (next lift: last lift of
#              the session).
#              The server uses its own ports: --server-port=7070 --server-mqtt-port=7071 --server-mqtt-ws-port=7072.
#              Its classes are those of owlcms/target/classes and shared/target/classes; its classpath comes from
#              playwright/owlcms-cp.txt (scripts/build-owlcms-classpath.sh, run once and after a dependency change).
#              Its output goes to playwright/logs/fixture-server.log. Sets --base, --mqtt and --group=M1.
#   --timing=<file>   the product's timing.properties (display durations; default
#              owlcms/src/main/resources/timing/timing.properties from the repository root); the script passes it
#   --base=http://localhost:8080 --mqtt=tcp://localhost:1883 --fop=A --group=M1 --out=<dir> --headed
#              without --server: an owlcms that is already running
#   --scenario=<ids, comma-separated; default all>
#     four-member jury with VPT:  010-vpt-reversal-nolift 020-vpt-reversal-goodlift
#                                 030-vpt-challenge-jury-agreed-nolift 035-vpt-challenge-jury-agreed-goodlift
#                                 040-vpt-challenge-success
#                                 050-vpt-challenge-fail 060-vpt-confirmation-nolift
#     three-member jury, no VPT:  070-jury3-reversal-nolift 080-jury3-reversal-goodlift 090-jury3-challenge-success
#                                 100-jury3-challenge-fail 110-jury3-confirmation-nolift
#     four-member jury, no VPT:   120-jury4-reversal-nolift 130-jury4-reversal-goodlift 140-jury4-challenge-success
#                                 150-jury4-challenge-fail 160-jury4-confirmation-nolift
#     four-member jury with VPT, jury box 1 broken: 170-vpt-broken-juror-nolift
#     jury box only, the Speaker enters the decision: 180-vpt-speaker-enters-nolift 190-jury3-speaker-enters-nolift
#     jury box 1 broken, Speaker abandons the vote: 200-vpt-broken-juror-start-anyway (VPT)
#                                 205-jury4-broken-juror-start-anyway (four-member jury, no VPT)
#     the Jury Chair decides on the jury box (no rule): 210-vpt-jurybox-nolift 220-vpt-jurybox-nolift-against-vote
#                                 230-jury3-jurybox-nolift-no-vote
#     Resume without a jury decision: 240-jury3-deliberation-resume 250-vpt-automatic-review-resume
#     three-member jury, normal Chair decision on the jury box:
#                                 260-jury3-jurybox-confirmation-nolift 270-jury3-jurybox-confirmation-goodlift
#                                 280-jury3-jurybox-reversal-nolift 290-jury3-jurybox-reversal-goodlift
#                                 280 summons the referees for discussion before the new jury vote
#     the jury agrees with the referees, no review; the attempt board stays 10 s on the athlete under review:
#                                 300-jury3-agreed-board-hold 320-vpt-agreed-board-hold
#                                 310-jury3-agreed-clock-start (the Speaker starts the next clock during that time)
#     no jury; the scoreboard decision block stays on the athlete under review for 20 s, or until the next clock:
#                                 330-nojury-decision-block-timeout 340-nojury-decision-block-clock-start
#     the review starts after the Speaker has started the next athlete's clock:
#                                 350-jury3-deliberation-after-next-clock 360-vpt-challenge-after-next-clock
#     the lift ends the snatches or the session (single-athlete sessions M2 to M7 of the fixture: --server):
#                                 370-jury3-last-snatch-board-hold 380-jury3-last-lift-board-hold
#                                 the jury never votes, the Speaker reloads the session:
#                                 390-jury3-last-snatch-silent-jury-reload 400-jury3-last-lift-silent-jury-reload
#                                 no jury: 410-nojury-last-snatch-decision-block 420-nojury-last-lift-decision-block
#     (a three-member jury with VPT behaves exactly as without VPT: no separate scenarios)
# Each scenario applies its own settings (automaticBreakNoVPT,
# Video Playback Technology, jury size on Jury Control).
# After the referees' decision every scenario checks that the Speaker and the scoreboard top line move to the
# next athlete while the attempt board and the decision block stay on the athlete under review for the jury votes.
# Every clock is set to 1:00 before it starts. The running clocks are checked at 0:58 on the Speaker, attempt
# board and scoreboard; the stop is sent two seconds later (0:56), then the MQTT stop acknowledgement and the
# stopped Speaker/board clocks are checked.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$(dirname "$MODULE_DIR")"
TIMING="$REPO_DIR/owlcms/src/main/resources/timing/timing.properties"
# Drives the locally installed Chrome, so the driver never needs its own browser bundle.
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

CP_FILE="$MODULE_DIR/cp.txt"
if [[ ! -f "$CP_FILE" ]]; then
    echo "cp.txt not found in $MODULE_DIR; build the playwright module first (mvn -pl playwright compile dependency:build-classpath -Dmdep.outputFile=cp.txt)" >&2
    exit 1
fi

CLASSPATH="$MODULE_DIR/target/classes:$(cat "$CP_FILE")"

SERVER=false
SERVER_PORT=7070
SERVER_MQTT_PORT=7071
SERVER_MQTT_WS_PORT=7072
ARGS=()
for arg in "$@"; do
    case "$arg" in
        --server) SERVER=true ;;
        --server-port=*) SERVER_PORT="${arg#*=}" ;;
        --server-mqtt-port=*) SERVER_MQTT_PORT="${arg#*=}" ;;
        --server-mqtt-ws-port=*) SERVER_MQTT_WS_PORT="${arg#*=}" ;;
        *) ARGS+=("$arg") ;;
    esac
done

if [[ "$SERVER" != true ]]; then
    exec java -cp "$CLASSPATH" playwright.JuryScenario --out="$MODULE_DIR/reports" --timing="$TIMING" ${ARGS[@]+"${ARGS[@]}"}
fi

# ---------------------------------------------------------------- dedicated owlcms for the run

FIXTURE="$MODULE_DIR/fixtures/jury-scenarios.json"
OWLCMS_CP_FILE="$MODULE_DIR/owlcms-cp.txt"
SERVER_LOG="$MODULE_DIR/logs/fixture-server.log"
# its own working directory: logs, audit trail and MQTT data stay apart from those of the development server
SERVER_DIR="$MODULE_DIR/target/fixture-server"
SERVER_PID=""

if [[ ! -f "$OWLCMS_CP_FILE" ]]; then
    echo "owlcms-cp.txt not found in $MODULE_DIR; run $SCRIPT_DIR/build-owlcms-classpath.sh once" >&2
    exit 1
fi
if [[ ! -f "$FIXTURE" ]]; then
    echo "fixture not found: $FIXTURE" >&2
    exit 1
fi
for port in "$SERVER_PORT" "$SERVER_MQTT_PORT" "$SERVER_MQTT_WS_PORT"; do
    if lsof -nP -iTCP:"$port" -sTCP:LISTEN > /dev/null 2>&1; then
        echo "port $port is already in use; choose others with --server-port, --server-mqtt-port, --server-mqtt-ws-port" >&2
        exit 1
    fi
done

stop_server() {
    if [[ -n "$SERVER_PID" ]] && kill -0 "$SERVER_PID" 2> /dev/null; then
        echo "stopping the owlcms server (pid $SERVER_PID)"
        # MainWrapper stops owlcms when it is told to stop, as it does on Control-C; owlcms is told as well so
        # that it shuts down at once
        for child in $(pgrep -P "$SERVER_PID" || true); do
            kill -TERM "$child" 2> /dev/null || true
        done
        kill -TERM "$SERVER_PID" 2> /dev/null || true
        wait "$SERVER_PID" 2> /dev/null || true
    fi
    SERVER_PID=""
}
trap stop_server EXIT
trap 'exit 130' INT TERM

mkdir -p "$SERVER_DIR" "$(dirname "$SERVER_LOG")"
echo "starting owlcms on port $SERVER_PORT (MQTT $SERVER_MQTT_PORT) from $FIXTURE; log: $SERVER_LOG"
(
    cd "$SERVER_DIR"
    export OWLCMS_MEMORYMODE=true
    export OWLCMS_LOADJSON="$FIXTURE"
    export OWLCMS_PORT="$SERVER_PORT"
    export OWLCMS_ENABLEEMBEDDEDMQTT=true
    export OWLCMS_MQTTPORT="$SERVER_MQTT_PORT"
    export OWLCMS_MQTTWSPORT="$SERVER_MQTT_WS_PORT"
    export OWLCMS_ENABLEEVENTFORWARDING=false
    exec java -ea -Dvaadin.devmode.devTools.enabled=false -Dlogback.configurationFile=logback-console.xml \
        --add-opens=jdk.unsupported/sun.misc=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
        --enable-native-access=ALL-UNNAMED \
        -cp "$REPO_DIR/owlcms/target/classes:$(cat "$OWLCMS_CP_FILE")" app.owlcms.MainWrapper
) > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!

# the server answers before the fixture is loaded: wait for the end of its startup. The first start after a
# frontend change rebuilds the Vaadin development bundle, which takes a few minutes.
for ((i = 0; i < 600; i++)); do
    if grep -q "OWLCMS Ready" "$SERVER_LOG" 2> /dev/null; then
        break
    fi
    if ! kill -0 "$SERVER_PID" 2> /dev/null; then
        echo "the owlcms server stopped during startup; see $SERVER_LOG" >&2
        tail -n 20 "$SERVER_LOG" >&2
        exit 1
    fi
    sleep 1
done
if ! grep -q "OWLCMS Ready" "$SERVER_LOG" 2> /dev/null; then
    echo "the owlcms server did not finish starting within 10 minutes; see $SERVER_LOG" >&2
    exit 1
fi
echo "owlcms is ready"

STATUS=0
java -cp "$CLASSPATH" playwright.JuryScenario --out="$MODULE_DIR/reports" --timing="$TIMING" \
    --base="http://localhost:$SERVER_PORT" --mqtt="tcp://localhost:$SERVER_MQTT_PORT" --group=M1 \
    ${ARGS[@]+"${ARGS[@]}"} || STATUS=$?
exit "$STATUS"
