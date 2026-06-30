#!/usr/bin/env bash
# clock-log-report.sh — run the consolidated OWLCMS/Playwright diagnostic report
# with the repository's standard log locations and output files.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$REPO_DIR"

if ! command -v python3 >/dev/null 2>&1; then
    echo "python3 not found; install Python 3 or run playwright/scripts/clock-log-report.py with an explicit interpreter" >&2
    exit 1
fi

exec python3 playwright/scripts/clock-log-report.py "$@"