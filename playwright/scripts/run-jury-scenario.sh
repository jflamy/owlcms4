#!/usr/bin/env bash
# run-jury-scenario.sh — run the jury review scenario against a running owlcms and write an HTML report with
# screenshots under playwright/reports/. Options (all optional):
#   --base=http://localhost:8080 --mqtt=tcp://localhost:1883 --fop=A --group=M1 --out=<dir> --headed
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
#     jury box 1 broken, Speaker abandons the vote: 200-vpt-broken-juror-start-anyway
#     (a three-member jury with VPT behaves exactly as without VPT: no separate scenarios)
# Each scenario applies its own settings (automaticBreakNoVPT,
# Video Playback Technology, jury size on Jury Control).
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
