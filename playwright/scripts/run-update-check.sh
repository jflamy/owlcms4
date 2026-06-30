#!/usr/bin/env bash
# run-update-check.sh — clear stale playwright.log, then launch UpdateCheck.
# Leaves both owlcms/logs/owlcms.log and playwright/logs/playwright.log in
# place after the run so confirm-clock-logs.py and locate-clock-mistakes.py
# can read them. This script must NOT be called from within the logs scripts.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
LOG_DIR="$MODULE_DIR/logs"
LOG_FILE="$LOG_DIR/playwright.log"

mkdir -p "$LOG_DIR"
rm -f "$LOG_FILE"

export OWLCMS_PLAYWRIGHT_LOG="$LOG_FILE"

CP_FILE="$MODULE_DIR/cp.txt"
if [[ ! -f "$CP_FILE" ]]; then
    echo "cp.txt not found in $MODULE_DIR; build the playwright module first (mvn -pl playwright compile dependency:build-classpath -Dmdep.outputFile=cp.txt)" >&2
    exit 1
fi

CLASSPATH="$MODULE_DIR/target/classes:$(cat "$CP_FILE")"

echo "Starting UpdateCheck — log: $LOG_FILE"
exec java -cp "$CLASSPATH" playwright.UpdateCheck "$@"
