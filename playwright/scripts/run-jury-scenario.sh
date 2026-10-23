#!/usr/bin/env bash
# run-jury-scenario.sh — run the jury review scenario against a running owlcms and write an HTML report with
# screenshots under playwright/reports/. Options (all optional):
#   --base=http://localhost:8080 --mqtt=tcp://localhost:1883 --fop=A --group=M1 --out=<dir> --headed
#   --scenario=<ids, comma-separated; default all>
#     four-member jury with VPT:  1-vpt-reversal-nolift 2-vpt-reversal-goodlift 3-vpt-challenge-success
#                                 4-vpt-challenge-fail 5-vpt-confirmation-nolift
#     three-member jury, no VPT:  6-jury3-reversal-nolift 7-jury3-reversal-goodlift 8-jury3-challenge-success
#                                 9-jury3-challenge-fail 10-jury3-confirmation-nolift
#     four-member jury, no VPT:   11-jury4-reversal-nolift 12-jury4-reversal-goodlift 13-jury4-challenge-success
#                                 14-jury4-challenge-fail 15-jury4-confirmation-nolift
#     four-member jury with VPT, jury box 1 broken: 16-vpt-broken-juror-nolift
#     jury box only, the Speaker enters the decision: 17-vpt-speaker-enters-nolift 18-jury3-speaker-enters-nolift
#     jury box 1 broken, Speaker abandons the vote: 19-vpt-broken-juror-start-anyway
#     (a three-member jury with VPT behaves exactly as without VPT: no separate scenarios)
# Each scenario applies its own settings (7personAdjudication,
# automaticBreakNoVPT, Video Playback Technology, jury size on Jury Control).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"

# Drives the locally installed Chrome, so the driver never needs its own browser bundle.
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

CP_FILE="$MODULE_DIR/cp.txt"
if [[ ! -f "$CP_FILE" ]]; then
    echo "cp.txt not found in $MODULE_DIR; build the playwright module first (mvn -pl playwright compile dependency:build-classpath -Dmdep.outputFile=cp.txt)" >&2
    exit 1
fi

CLASSPATH="$MODULE_DIR/target/classes:$(cat "$CP_FILE")"

exec java -cp "$CLASSPATH" playwright.JuryScenario --out="$MODULE_DIR/reports" "$@"
