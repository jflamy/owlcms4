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
#   --scenario=<ids, comma-separated; default all, in this order>
#     seven-person panel (four-member jury with VPT), 1xx:
#                                 100-vpt-reversal-nolift 105-vpt-reversal-goodlift
#                                 110-vpt-challenge-jury-agreed-nolift 115-vpt-challenge-jury-agreed-goodlift
#                                 120-vpt-challenge-success 125-vpt-challenge-fail 130-vpt-confirmation-nolift
#                                 135-vpt-broken-juror-nolift (jury box 1 broken, the Chair decides)
#                                 140-vpt-speaker-enters-nolift (jury box only, the Speaker enters the decision)
#                                 145-vpt-broken-juror-start-anyway (jury box 1 broken, the Speaker starts the clock)
#                                 150-vpt-jurybox-nolift 155-vpt-jurybox-nolift-against-vote (Chair on the jury box)
#                                 160-vpt-automatic-review-resume (ended with Resume, no decision)
#                                 165-vpt-agreed-board-hold (the panel agrees with the referees, no review)
#                                 170-vpt-challenge-after-next-clock
#     four-member jury, no VPT, 2xx:
#                                 200-jury4-reversal-nolift 205-jury4-reversal-goodlift 210-jury4-challenge-success
#                                 215-jury4-challenge-fail 220-jury4-confirmation-nolift
#                                 225-jury4-broken-juror-start-anyway
#     three-member jury, 3xx (a three-member jury with VPT behaves exactly as without VPT: no separate scenarios):
#                                 300-jury3-reversal-nolift 305-jury3-reversal-goodlift 310-jury3-challenge-success
#                                 315-jury3-challenge-fail 320-jury3-confirmation-nolift 325-jury3-goodlift-stands
#                                 (215, 315, 325: the review vote does not reach the jury's rule, the referees'
#                                 decision stands as called)
#                                 330-jury3-speaker-enters-nolift (jury box only, the Speaker enters the decision)
#                                 335-jury3-jurybox-nolift-no-vote 340-jury3-deliberation-resume
#                                 345-jury3-jurybox-confirmation-nolift 350-jury3-jurybox-confirmation-goodlift
#                                 355-jury3-jurybox-reversal-nolift 360-jury3-jurybox-reversal-goodlift
#                                 (355 summons the referees for discussion before the new jury vote)
#                                 365-jury3-agreed-board-hold 370-jury3-agreed-clock-start
#                                 375-jury3-deliberation-after-next-clock
#                                 the lift ends the snatches or the session (single-athlete sessions of the fixture):
#                                 380-jury3-last-snatch-board-hold 385-jury3-last-lift-board-hold
#                                 the jury never votes, the Speaker reloads the session:
#                                 390-jury3-last-snatch-silent-jury-reload 395-jury3-last-lift-silent-jury-reload
#     no jury, 5xx:               500-nojury-decision-block-timeout 505-nojury-decision-block-clock-start
#                                 510-nojury-last-snatch-decision-block 515-nojury-last-lift-decision-block
#     380 to 395, 510 and 515 need the single-athlete sessions M2 to M7 of the fixture (--server).
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
