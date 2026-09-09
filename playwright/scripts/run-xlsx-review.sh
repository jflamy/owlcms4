#!/usr/bin/env bash
# Generate jury, competition-results, and session-results XLSX files from a running OWLCMS server.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODULE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$(dirname "$MODULE_DIR")"
CP_FILE="$MODULE_DIR/cp.txt"

if [[ ! -f "$CP_FILE" || ! -f "$MODULE_DIR/target/classes/playwright/XlsxReview.class" ]]; then
    echo "Playwright XLSX review runner is not built." >&2
    echo "Run: mvn -pl playwright compile dependency:build-classpath -Dmdep.outputFile=cp.txt" >&2
    exit 1
fi

export PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
cd "$REPO_DIR"
exec java -cp "$MODULE_DIR/target/classes:$(cat "$CP_FILE")" playwright.XlsxReview "$@"