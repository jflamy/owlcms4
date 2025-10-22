#!/bin/bash -
export LC_ALL=C.UTF-8


# Usage: ./restart_8080.sh [port]
REMOTE_HOST="jflamy@143.110.208.71"
REMOTE_PORT="${1:-8081}"
REMOTE_DIR="/home/jflamy/fhq2"

"$(dirname "$0")/restart_shared.sh" "$REMOTE_HOST" "$REMOTE_PORT" "$REMOTE_DIR"
