#!/usr/bin/env bash
# run-theme-compare.sh — capture the same owlcms routes from two servers and diff the screenshots.
#
# Both servers must run against the same database, with the same session/platform selected.
# Defaults: old = http://localhost:8080, new = http://localhost:8083
#
# Uses the locally installed Google Chrome by default; the Chromium bundled with Playwright 1.41
# segfaults on recent macOS. Pass --channel= (empty) to force the bundled build.
#
# Examples:
#   playwright/scripts/run-theme-compare.sh
#   playwright/scripts/run-theme-compare.sh --tiers=1 --fop=A
#   playwright/scripts/run-theme-compare.sh --only=announcer --headed
#   playwright/scripts/run-theme-compare.sh --pin=1234 --width=1920 --height=1080
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$(dirname "$MODULE_DIR")"

CP_FILE="$MODULE_DIR/cp.txt"
if [[ ! -f "$CP_FILE" || ! -d "$MODULE_DIR/target/classes" ]]; then
    echo "playwright module not built." >&2
    echo "Run this first (it invokes Maven, so run it yourself):" >&2
    echo "  mvn -pl playwright compile dependency:build-classpath -Dmdep.outputFile=cp.txt" >&2
    exit 1
fi

CLASSPATH="$MODULE_DIR/target/classes:$(cat "$CP_FILE")"

# The tool drives the locally installed Chrome, so the driver never needs its own browser bundle.
export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

cd "$REPO_DIR"
exec java -cp "$CLASSPATH" playwright.ThemeCompare "$@"
