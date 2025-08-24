#!/bin/bash -
export LC_ALL=C.UTF-8


# Usage: ./restart_8084.sh [port]
REMOTE_HOST="jflamy@143.110.208.71"
REMOTE_PORT="${1:-8084}"
REMOTE_DIR="/home/jflamy/asu"

"$(dirname "$0")/restart_shared.sh" "$REMOTE_HOST" "$REMOTE_PORT" "$REMOTE_DIR"
