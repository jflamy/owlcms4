#!/usr/bin/env bash
# Send an owlcms jury decision over MQTT (test tool).
# Usage examples:
#   ./mqtt-jury.sh 3 good        # jury member 3 votes good lift on platform A
#   ./mqtt-jury.sh 1 bad -p B    # jury member 1 votes bad lift on platform B
#   ./mqtt-jury.sh --console good
exec python3 "$(dirname "$0")/mqtt-jury.py" "$@"
